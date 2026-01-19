package com.uipg9.pocketsettlement.network;

import com.uipg9.pocketsettlement.PocketSettlement;
import com.uipg9.pocketsettlement.gui.DeskScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles networking between client and server for the settlement GUI.
 */
public class SettlementNetworking {
    
    // Packet IDs
    public static final Identifier OPEN_DESK_ID = Identifier.fromNamespaceAndPath(PocketSettlement.MOD_ID, "open_desk");
    public static final Identifier SYNC_DATA_ID = Identifier.fromNamespaceAndPath(PocketSettlement.MOD_ID, "sync_data");
    
    // === Packet Records ===
    
    /**
     * Client -> Server: Request to open the desk GUI
     */
    public record OpenDeskPacket() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenDeskPacket> TYPE = 
            new CustomPacketPayload.Type<>(OPEN_DESK_ID);
        
        public static final StreamCodec<FriendlyByteBuf, OpenDeskPacket> CODEC = 
            StreamCodec.unit(new OpenDeskPacket());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // === Registration ===
    
    /**
     * Register packet types. Call this from both client and server init.
     */
    public static void registerPackets() {
        // Register C2S packets
        PayloadTypeRegistry.playC2S().register(OpenDeskPacket.TYPE, OpenDeskPacket.CODEC);
    }
    
    /**
     * Register server-side packet handlers.
     * Call from ModInitializer.
     */
    public static void registerServerReceivers() {
        registerPackets();
        
        // Handle open desk request
        ServerPlayNetworking.registerGlobalReceiver(OpenDeskPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                // Open the desk GUI for the player
                DeskScreen.open(player);
            });
        });
        
        PocketSettlement.LOGGER.info("[Pocket Settlement] Server networking registered");
    }
    
    /**
     * Register client-side packet handlers.
     * Call from ClientModInitializer.
     */
    @Environment(EnvType.CLIENT)
    public static void registerClientReceivers() {
        registerPackets();
        
        PocketSettlement.LOGGER.info("[Pocket Settlement] Client networking registered");
    }
    
    // === Client-side sending ===
    
    /**
     * Send request to open desk GUI.
     */
    @Environment(EnvType.CLIENT)
    public static void sendOpenDeskPacket() {
        ClientPlayNetworking.send(new OpenDeskPacket());
    }
}
