package com.example.energybridge;

import com.example.energybridge.content.EnergyBridgeBE;
import com.example.energybridge.registry.ModRegistries;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * =====================================
 * ModCapabilities.java
 * -------------------------------------
 * "Capabilities" are a flexible way for mods to expose features to each other.
 * Forge Energy (FE) is exposed via a standard capability so that cables and
 * machines from different mods can "speak energy" to each other.
 *
 * Here we register that our Energy Bridge BlockEntity PROVIDES Forge Energy
 * on all sides. Consumers will call "getCapability" to obtain an IEnergyStorage.
 *
 * NOTE: In NeoForge 1.21.1, the correct constant is Capabilities.EnergyStorage.BLOCK
 * (older examples use Capabilities.Energy, which will not compile).
 * =====================================
 */
@EventBusSubscriber(modid = EnergyBridgeMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {
    private ModCapabilities() {}

    /**
     * Called by NeoForge when capability registration happens.
     * We tell it: "For our block entity type, if someone asks for a BLOCK energy capability,
     * give them our per-side handler."
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,       // Which capability we provide (FE on blocks)
                ModRegistries.ENERGY_BRIDGE_BE.get(),   // For which BlockEntityType
                (EnergyBridgeBE be, Direction side) ->  // How to provide it (lambda called with our BE and the side)
                        be.getEnergy(side)              // Return the energy handler for that side (or null on client)
        );
    }
}