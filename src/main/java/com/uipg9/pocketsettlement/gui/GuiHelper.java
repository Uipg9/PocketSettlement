package com.uipg9.pocketsettlement.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Helper utilities for GUI operations.
 */
public class GuiHelper {
    
    /**
     * Play a sound to a player.
     * Uses the world's playSound method which is the correct 1.21.11 API.
     */
    public static void playSound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.MASTER, volume, pitch);
    }
}
