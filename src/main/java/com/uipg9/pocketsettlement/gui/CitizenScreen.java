package com.uipg9.pocketsettlement.gui;

import com.uipg9.pocketsettlement.data.*;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Citizen management screen - recruit, view, and train citizens.
 */
public class CitizenScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    
    public CitizenScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player) {
        CitizenScreen screen = new CitizenScreen(player);
        screen.setTitle(Component.literal("§b§l☻ Citizenry"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.CYAN_STAINED_GLASS_PANE)
            .setName(Component.literal(""));
        
        for (int i = 0; i < 54; i++) {
            this.setSlot(i, bg);
        }
        
        // Back button
        this.setSlot(0, new GuiElementBuilder()
            .setItem(Items.ARROW)
            .setName(Component.literal("§e← Back to Desk"))
            .setCallback((index, type, action) -> {
                DeskScreen.open(player);
            })
        );
        
        // Population info
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.PLAYER_HEAD)
            .setName(Component.literal("§b§lCitizenry"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Population: §f" + state.getCitizenCount() + "/" + state.getMaxCitizens()))
            .addLoreLine(Component.literal("§7Avg Happiness: §f" + state.getAverageHappiness() + "%"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Coins display
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.GOLD_INGOT)
            .setName(Component.literal("§6Coins: §e" + state.getCoins()))
        );
        
        // Recruit button (slot 45)
        int recruitCost = state.getRecruitmentCost();
        boolean canRecruit = state.canRecruitCitizen();
        
        this.setSlot(45, new GuiElementBuilder()
            .setItem(canRecruit ? Items.VILLAGER_SPAWN_EGG : Items.BARRIER)
            .setName(Component.literal((canRecruit ? "§a" : "§c") + "✚ Recruit Citizen"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Cost: " + (canRecruit ? "§e" : "§c") + recruitCost + " coins"))
            .addLoreLine(Component.literal("§7Population: " + state.getCitizenCount() + "/" + state.getMaxCitizens()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(canRecruit ? Component.literal("§aClick to recruit!") : Component.literal("§cMax population or insufficient coins"))
            .setCallback((index, type, action) -> {
                if (canRecruit) {
                    Citizen newCitizen = state.recruitCitizen();
                    if (newCitizen != null) {
                        GuiHelper.playSound(player, SoundEvents.VILLAGER_YES, 1.0f, 1.0f);
                        player.sendSystemMessage(Component.literal("§a✓ Welcome, " + newCitizen.getName() + "!"));
                        CitizenScreen.open(player);  // Refresh
                    }
                } else {
                    GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
                }
            })
        );
        
        // Display citizens (rows 1-4)
        List<Citizen> citizens = new ArrayList<>(state.getAllCitizens());
        int slot = 10;
        for (int i = 0; i < citizens.size() && slot < 44; i++) {
            Citizen citizen = citizens.get(i);
            
            // Skip edge slots
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }
            
            this.setSlot(slot, createCitizenElement(citizen));
            slot++;
        }
    }
    
    private GuiElementBuilder createCitizenElement(Citizen citizen) {
        String happinessColor = citizen.getHappiness() >= 70 ? "§a" : (citizen.getHappiness() >= 40 ? "§e" : "§c");
        
        GuiElementBuilder builder = new GuiElementBuilder()
            .setItem(citizen.getJob().getIconItem())
            .setName(Component.literal("§b" + citizen.getName()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Job: §f" + citizen.getJob().getDisplayName()))
            .addLoreLine(Component.literal("§7Level: §f" + citizen.getLevel() + "/5"))
            .addLoreLine(Component.literal("§7XP: §f" + citizen.getXp() + "/" + citizen.getXpForNextLevel()))
            .addLoreLine(Component.literal("§7Efficiency: §f" + citizen.getEfficiencyPercent() + "%"))
            .addLoreLine(Component.literal("§7Happiness: " + happinessColor + citizen.getHappiness() + "%"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
        
        // Training option if academy is unlocked
        if (state.getTechTree().isUnlocked(TechTree.TechNode.EDUCATION_I) && citizen.getLevel() < 5) {
            int trainingCost = citizen.getTrainingCost();
            boolean canTrain = state.getCoins() >= trainingCost && state.getBuildingCount(BuildingType.ACADEMY) > 0;
            
            builder.addLoreLine(Component.literal("§7Training: " + (canTrain ? "§e" : "§c") + trainingCost + " coins"));
            builder.addLoreLine(Component.literal(canTrain ? "§eClick to train!" : "§cNeed academy & coins"));
            
            builder.setCallback((index, type, action) -> {
                if (canTrain && state.spendCoins(trainingCost)) {
                    boolean leveledUp = citizen.addXp(citizen.getXpForNextLevel());  // Instantly level up
                    state.setDirty();
                    
                    if (leveledUp) {
                        GuiHelper.playSound(player, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
                        player.sendSystemMessage(Component.literal("§a✓ " + citizen.getName() + " leveled up to Level " + citizen.getLevel() + "!"));
                    } else {
                        GuiHelper.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        player.sendSystemMessage(Component.literal("§a✓ " + citizen.getName() + " trained! XP gained."));
                    }
                    CitizenScreen.open(player);
                } else {
                    GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
                }
            });
        }
        
        return builder;
    }
}
