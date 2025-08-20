package com.example.energybridge.content;

import com.example.energybridge.registry.ModRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * =====================================
 * EnergyBridgeBlock.java
 * -------------------------------------
 * This class represents the actual BLOCK placed in the world.
 * It "has" a BlockEntity (EnergyBridgeBE) which stores the energy and does logic.
 *
 * Key ideas:
 *  - Blocks are the physical thing you see and interact with.
 *  - BlockEntities add per-block data/logic (like a battery).
 *  - We also store some "state" on the block itself, like which way it's facing.
 * =====================================
 */
public class EnergyBridgeBlock extends BaseEntityBlock {
    // "FACING" is which horizontal direction the block is pointing (N, S, E, W).
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // "LIT" is a simple on/off visual we toggle when energy flows this tick.
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /**
     * Constructor: sets up basic physical properties.
     * @param props The base properties we pass to the Block superclass.
     */
    public EnergyBridgeBlock(Properties props) {
        // strength(hardness, blastResistance), metal sound.
        // We intentionally *don't* require a specific tool tier so it drops reliably.
        super(props.strength(2.0F, 6.0F).sound(SoundType.METAL));

        // This line sets the default state the block starts with (facing north, not lit).
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, Boolean.FALSE)
        );
    }

    /**
     * Tell Minecraft which block state properties exist for this block.
     * Without this, you can't set/get FACING or LIT.
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /**
     * Called when the player places the block.
     * We set the FACING to "look at the player" (so the front faces the player).
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(LIT, Boolean.FALSE);
    }

    /**
     * How the block is rendered. "MODEL" means use the JSON block model.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /**
     * Create a new BlockEntity instance whenever this block is placed/loaded.
     */
    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyBridgeBE(pos, state);
    }

    /**
     * Hook up the server tick function of our block entity.
     * This runs once per tick (20 times/sec) on the server and performs energy IO.
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // No ticking on the client (visuals only there). Server runs the logic.
        return level.isClientSide ? null : createTickerHelper(type, ModRegistries.ENERGY_BRIDGE_BE.get(), EnergyBridgeBE::serverTick);
    }

    /**
     * Called by Minecraft when a neighboring block changes (placed/broken/moved).
     * We forward this to our BE so it can immediately stop feeding energy into a removed neighbor.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyBridgeBE ebb) {
            ebb.onNeighborChanged(fromPos);
        }
        // Also tell the world around us something changed (helps redstone/other systems).
        level.updateNeighborsAt(pos, this);
    }

    /**
     * Force the block to always drop itself when broken.
     * Why? You reported inconsistent drops (likely loot table/tool tier issues).
     * Returning a single ItemStack here bypasses all that and guarantees a drop.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Testing Tip: Break this block in survival mode. It should always drop itself now.
        return java.util.Collections.singletonList(new ItemStack(ModRegistries.ENERGY_BRIDGE_ITEM.get()));
    }

    /**
     * Boilerplate required by modern block classes to support serialization of block definitions.
     */
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(EnergyBridgeBlock::new);
    }
}