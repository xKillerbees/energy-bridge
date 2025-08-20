package com.example.energybridge.registry;

import com.example.energybridge.EnergyBridgeMod;
import com.example.energybridge.content.EnergyBridgeBE;
import com.example.energybridge.content.EnergyBridgeBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * =================================
 * ModRegistries.java
 * ---------------------------------
 * This file centralizes registration of game objects:
 *  - The Block you place in the world
 *  - The Item you hold/place (BlockItem wraps the Block)
 *  - The BlockEntityType (the logic container for the block)
 *
 * "DeferredRegister" means: register things at the right time during startup.
 * We create the registers here and then "attach" them to the mod event bus
 * from our mod's entry point, so everything gets registered properly.
 * =================================
 */
public final class ModRegistries {
    // Utility class pattern: private constructor prevents instantiation.
    private ModRegistries() {}

    // Create a "register" for each vanilla registry we need to add things to.
    // The second argument is our mod id so objects are namespaced as "energybridge:..."
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, EnergyBridgeMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, EnergyBridgeMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyBridgeMod.MODID);

    // Register our Energy Bridge block. The string is its registry name ("energybridge:energy_bridge").
    public static final DeferredHolder<Block, EnergyBridgeBlock> ENERGY_BRIDGE =
            BLOCKS.register("energy_bridge", () -> new EnergyBridgeBlock(Block.Properties.of()));

    // Register the item form of the block so you can hold/place it.
    public static final DeferredHolder<Item, BlockItem> ENERGY_BRIDGE_ITEM = ITEMS.register(
            "energy_bridge", () -> new BlockItem(ENERGY_BRIDGE.get(), new Item.Properties())
    );

    // Register the "type" of block entity. This tells Minecraft which block uses which logic class (EnergyBridgeBE).
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyBridgeBE>> ENERGY_BRIDGE_BE =
            BLOCK_ENTITIES.register("energy_bridge",
                    () -> BlockEntityType.Builder.of(EnergyBridgeBE::new, ENERGY_BRIDGE.get()).build(null));

    /**
     * Hook all of our registers into the mod event bus.
     * @param bus Provided by the mod entrypoint.
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}