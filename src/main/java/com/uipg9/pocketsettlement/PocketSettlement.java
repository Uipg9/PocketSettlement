package com.uipg9.pocketsettlement;

import com.uipg9.pocketsettlement.commands.SettlementCommand;
import com.uipg9.pocketsettlement.data.SettlementState;
import com.uipg9.pocketsettlement.tick.SettlementTickManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pocket Settlement: The Governor's Desk
 * 
 * A cozy, "Vanilla Plus" colony simulator that plays entirely within a beautiful,
 * code-rendered GUI. No placing blocks in the world—you manage the settlement from your desk.
 * 
 * @author Uipg9
 * @version 1.0.0
 * @since Minecraft 1.21.11
 */
public class PocketSettlement implements ModInitializer {
    public static final String MOD_ID = "pocketsettlement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // UI Color Constants (Parchment/Desk Aesthetic)
    public static final int COLOR_PARCHMENT = 0xFFF2EAD3;      // Cream parchment background
    public static final int COLOR_LEATHER_BORDER = 0xFF8B4513; // Leather brown border
    public static final int COLOR_INK_BLACK = 0xFF1A1A1A;      // Ink black text
    public static final int COLOR_INK_RED = 0xFF8B0000;        // Dark red accent
    public static final int COLOR_INK_GREEN = 0xFF006400;      // Dark green accent
    public static final int COLOR_GOLD_ACCENT = 0xFFDAA520;    // Gold for coins/rewards
    public static final int COLOR_VIGNETTE = 0x40000000;       // Semi-transparent vignette
    
    @Override
    public void onInitialize() {
        LOGGER.info("═══════════════════════════════════════════════════════");
        LOGGER.info("   Pocket Settlement: The Governor's Desk");
        LOGGER.info("   Version 1.0.0 for Minecraft 1.21.11");
        LOGGER.info("   Author: Uipg9");
        LOGGER.info("═══════════════════════════════════════════════════════");
        
        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            SettlementCommand.register(dispatcher);
            LOGGER.info("[Pocket Settlement] Commands registered");
        });
        
        // Register server tick event for simulation processing
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                SettlementTickManager.tick(world);
            }
        });
        
        // Initialize settlement data when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SettlementState.getOrCreate(server.overworld());
            LOGGER.info("[Pocket Settlement] Settlement state initialized");
        });
        
        // Save settlement data when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            SettlementState state = SettlementState.getOrCreate(server.overworld());
            state.setDirty();
            LOGGER.info("[Pocket Settlement] Settlement state saved");
        });
        
        LOGGER.info("[Pocket Settlement] Initialization complete!");
    }
}
