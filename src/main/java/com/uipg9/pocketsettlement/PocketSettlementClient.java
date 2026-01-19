package com.uipg9.pocketsettlement;

import com.uipg9.pocketsettlement.network.SettlementNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side initialization for Pocket Settlement.
 * Handles keybindings and client-specific features.
 */
@Environment(EnvType.CLIENT)
public class PocketSettlementClient implements ClientModInitializer {
    
    // Keybinding: G for Governor's Desk
    private static KeyMapping openDeskKey;
    
    // Category for keybindings - registered on first use
    public static final KeyMapping.Category KEYBIND_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("pocketsettlement", "main")
    );
    
    @Override
    public void onInitializeClient() {
        PocketSettlement.LOGGER.info("[Pocket Settlement] Initializing client...");
        
        // Register keybinding
        openDeskKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.pocketsettlement.open_desk",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEYBIND_CATEGORY
        ));
        
        // Handle keybinding presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDeskKey.consumeClick()) {
                if (client.player != null && client.level != null) {
                    // Send packet to server to open GUI
                    SettlementNetworking.sendOpenDeskPacket();
                }
            }
        });
        
        // Register client-side networking
        SettlementNetworking.registerClientReceivers();
        
        PocketSettlement.LOGGER.info("[Pocket Settlement] Client initialization complete!");
    }
}
