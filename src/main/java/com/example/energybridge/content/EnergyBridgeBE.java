package com.example.energybridge.content;

import com.example.energybridge.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * =====================================
 * EnergyBridgeBE.java (Block Entity)
 * -------------------------------------
 * BlockEntities are where we store *data* and *logic* for individual block
 * instances in the world. Think of this like the "battery + controller"
 * for one placed Energy Bridge.
 *
 * Responsibilities here:
 *  - Own an internal Forge Energy buffer (like a battery)
 *  - Every tick: pull energy from neighbors (if we can) and push energy to neighbors (if we can)
 *  - Toggle the block's "LIT" state if any energy moved this tick
 *  - Expose per-side energy capabilities so other mods can connect
 *  - Save/load energy to NBT so it persists across world saves
 * =====================================
 */
public class EnergyBridgeBE extends BlockEntity {
    // ======= Tunable settings =======
    // Total energy the block can store internally.
    private static final int CAPACITY = 100000;
        // TODO: Change this number if you want a different max buffer size (in FE).
        // Testing Tip: Place the block, use any FE generator next to it, and see how much it stores.
    // Maximum FE we can pull from neighbors each tick.
    private static final int MAX_IN = 2000;
        // TODO: Change this to set how fast energy can be pulled in from neighbors per tick.
    // Maximum FE we can push to neighbors each tick.
    private static final int MAX_OUT = 2000;
        // TODO: Change this to set how fast energy can be sent out per tick.

    // The actual internal battery. EnergyStorage is a helper provided by NeoForge.
    private final EnergyStorage buffer = new EnergyStorage(CAPACITY, MAX_IN, MAX_OUT);

    // We expose a handler per side (NORTH, SOUTH, EAST, WEST, UP, DOWN).
    // For simplicity, each side connects to the same internal buffer and respects IO limits.
    private final Map<Direction, IEnergyStorage> sideHandlers = new EnumMap<>(Direction.class);

    // Used to note that a neighbor changed recently (not strictly required with our no-cache approach).
    private boolean neighborsDirty = false;
    // Used for client sync; we keep it even though Jade is disabled so it's easy to re-enable later.
    private int lastSyncedEnergy = -1;

    /**
     * Constructor: called whenever this block entity is created (block placed, chunk loaded, etc.)
     * @param pos  Block position in the world (x,y,z).
     * @param state The current block state (things like FACING/LIT).
     */
    public EnergyBridgeBE(BlockPos pos, BlockState state) {
        // Super constructor tells Minecraft which "type" of BE this is.
        super(ModRegistries.ENERGY_BRIDGE_BE.get(), pos, state);

        // Build a small IEnergyStorage wrapper for each side that talks to our internal buffer.
        for (Direction d : Direction.values()) {
            sideHandlers.put(d, new IEnergyStorage() {
                /**
                 * Try to insert energy (neighbor -> us).
                 * @param maxReceive how much the neighbor wants to send
                 * @param simulate if true, don't actually change energy; just report what would happen
                 */
                @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                    // Respect our MAX_IN as a hard limit per call.
                    return buffer.receiveEnergy(Math.min(maxReceive, MAX_IN), simulate);
                }
                /**
                 * Try to extract energy (us -> neighbor).
                 * @param maxExtract how much the neighbor wants from us
                 * @param simulate if true, don't actually change energy; just report what would happen
                 */
                @Override public int extractEnergy(int maxExtract, boolean simulate) {
                    // Respect our MAX_OUT as a hard limit per call.
                    return buffer.extractEnergy(Math.min(maxExtract, MAX_OUT), simulate);
                }
                /** @return current energy stored in our internal buffer. */
                @Override public int getEnergyStored() { return buffer.getEnergyStored(); }
                /** @return maximum capacity of our internal buffer. */
                @Override public int getMaxEnergyStored() { return buffer.getMaxEnergyStored(); }
                /** We allow extraction by neighbors. */
                @Override public boolean canExtract() { return true; }
                /** We allow reception from neighbors. */
                @Override public boolean canReceive() { return true; }
            });
        }
    }

    /**
     * The main logic tick for the block entity. Runs ONLY on the server.
     * @param level The world the block is in.
     * @param pos   The block's position.
     * @param state The block's current blockstate (FACING/LIT).
     * @param be    The block entity instance (this object).
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyBridgeBE be) {
        // Client worlds are for rendering only; never run game logic there.
        if (level == null || level.isClientSide) return;

        boolean pulled = false; // Did we pull energy from any side this tick?
        boolean pushed = false; // Did we push energy to any side this tick?

        // ======= PULL phase: try to fill our buffer by asking all neighbors for energy =======
        for (Direction dir : Direction.values()) {
            // If we're already full, stop pulling.
            if (be.buffer.getEnergyStored() >= be.buffer.getMaxEnergyStored()) break;

            // Look up the neighbor's energy capability on the opposite face (facing us).
            IEnergyStorage neighbor = getNeighborEnergy(level, pos.relative(dir), dir.getOpposite());
            if (neighbor != null && neighbor.canExtract()) {
                // We can only receive up to MAX_IN, and not exceed our capacity.
                int toPull = Math.min(MAX_IN, be.buffer.getMaxEnergyStored() - be.buffer.getEnergyStored());
                int extracted = neighbor.extractEnergy(toPull, false); // false = actually extract (not simulate)
                if (extracted > 0) {
                    be.buffer.receiveEnergy(extracted, false); // Store it in our buffer.
                    pulled = true;
                }
            }
        }

        // ======= PUSH phase: try to empty our buffer by sending energy to neighbors =======
        for (Direction dir : Direction.values()) {
            // If we're empty, stop pushing.
            if (be.buffer.getEnergyStored() <= 0) break;

            IEnergyStorage neighbor = getNeighborEnergy(level, pos.relative(dir), dir.getOpposite());
            if (neighbor != null && neighbor.canReceive()) {
                // Send at most MAX_OUT, or however much we have stored.
                int toSend = Math.min(MAX_OUT, be.buffer.getEnergyStored());
                int sent = neighbor.receiveEnergy(toSend, false);
                if (sent > 0) {
                    be.buffer.extractEnergy(sent, false); // Remove from our buffer.
                    pushed = true;
                }
            }
        }

        // ======= Visual feedback: turn the ring light on ONLY if there was IO this tick =======
        boolean shouldBeLit = pulled || pushed;
        if (state.hasProperty(EnergyBridgeBlock.LIT) && state.getValue(EnergyBridgeBlock.LIT) != shouldBeLit) {
            level.setBlockAndUpdate(pos, state.setValue(EnergyBridgeBlock.LIT, shouldBeLit));
        }

        // ======= Client sync (optional): update clients when energy changes =======
        // Keeping this code means it's easy to re-enable overlays later.
        int now = be.buffer.getEnergyStored();
        if (now != be.lastSyncedEnergy) {
            be.lastSyncedEnergy = now;
            be.setChanged(); // Mark data as changed for saving
            level.sendBlockUpdated(pos, state, state, 3); // Ask clients to refresh the block
        }

        // Neighbor info flag not used for logic (left for future expansion).
        if (be.neighborsDirty) be.neighborsDirty = false;
    }

    /**
     * Helper to ask a neighbor for its energy handler on a specific face.
     * @param level The world
     * @param pos   The neighbor's position
     * @param face  Which side of the neighbor we are interacting with
     * @return IEnergyStorage for that neighbor face, or null if none
     */
    @Nullable
    private static IEnergyStorage getNeighborEnergy(Level level, BlockPos pos, Direction face) {
        // In NeoForge 1.21.1 the correct capability is EnergyStorage.BLOCK.
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, face);
    }

    /**
     * Called by the block when something next to us changed (e.g., a block was broken).
     * We mark as dirty and trigger a client update so visuals can reflect changes promptly.
     */
    public void onNeighborChanged(BlockPos fromPos) {
        this.neighborsDirty = true;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Provide our per-side energy handler to other mods/cables.
     * Returning null on the CLIENT side is a simple way to "hide" from overlays like Jade.
     * Servers (the source of truth) still expose capabilities normally.
     * @param side Which side is being queried (may be null in some contexts)
     * @return an IEnergyStorage or null
     */
    @Nullable
    public IEnergyStorage getEnergy(Direction side) {
        if (this.level != null && this.level.isClientSide) return null; // Hide on client
        return sideHandlers.get(side == null ? Direction.NORTH : side);
    }

    // ======= Saving and Loading data (so energy persists) =======

    /**
     * Save extra data to the chunk. Called when the chunk or world saves.
     * @param tag NBT tag to write into
     * @param registries Registry access (unused here)
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", buffer.getEnergyStored());
    }

    /**
     * Load extra data from the chunk. Called when the chunk is loaded.
     * @param tag NBT tag to read from
     * @param registries Registry access (unused here)
     */
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int e = tag.getInt("Energy");
        if (e > 0) buffer.receiveEnergy(e, false);
    }

    // ======= Networking helpers (safe to remove if you don't need client sync) =======

    /**
     * Create a tag that represents the current state, sent to the client when needed.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    /**
     * Receive and apply the tag sent by the server (client side).
     */
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        loadAdditional(tag, lookup);
    }

    /**
     * Ask NeoForge to create a packet to sync this BE to the client.
     * Returning non-null means "yes, please send an update packet."
     */
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Handle an incoming update packet on the client.
     * We read the tag and update our local data.
     */
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookup) {
        if (pkt != null) {
            handleUpdateTag(pkt.getTag(), lookup);
        }
    }
}