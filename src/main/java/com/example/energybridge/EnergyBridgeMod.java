package com.example.energybridge;

import com.example.energybridge.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * ================================
 * EnergyBridgeMod.java
 * -------------------------------
 * This is the "entry point" of the mod.
 * NeoForge (the modding library) looks for classes annotated with @Mod
 * to know which mod to start. The string you see here must match your
 * mod id (also declared in neoforge.mods.toml).
 *
 * When Minecraft loads, NeoForge creates an instance of this class.
 * The constructor is passed an "event bus", which is like a message
 * system where we can register things (blocks, items, etc.) at the
 * correct time during startup.
 * ================================
 */
@Mod(EnergyBridgeMod.MODID) // Tell NeoForge: this class is our mod.
public class EnergyBridgeMod {
    // This needs to match the "modId" field in your META-INF/neoforge.mods.toml file.
    public static final String MODID = "energybridge";

    /**
     * Constructor called by NeoForge when the game starts.
     * @param modBus The mod-specific event bus. We use it to "hook in" our registrations.
     */
    public EnergyBridgeMod(IEventBus modBus) {
        // Register our blocks/items/block-entities so Minecraft knows about them.
        ModRegistries.register(modBus);
    }
}