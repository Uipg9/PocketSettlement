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

/**
 * Build menu - select what to construct on an empty plot.
 */
public class BuildMenuScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private final int gridX;
    private final int gridZ;
    
    public BuildMenuScreen(ServerPlayer player, int gridX, int gridZ) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        this.gridX = gridX;
        this.gridZ = gridZ;
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player, int gridX, int gridZ) {
        BuildMenuScreen screen = new BuildMenuScreen(player, gridX, gridZ);
        screen.setTitle(Component.literal("§2§lSelect Building [" + gridX + ", " + gridZ + "]"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.GRAY_STAINED_GLASS_PANE)
            .setName(Component.literal(""));
        
        for (int i = 0; i < 27; i++) {
            this.setSlot(i, bg);
        }
        
        // Back button
        this.setSlot(0, new GuiElementBuilder()
            .setItem(Items.ARROW)
            .setName(Component.literal("§e← Back"))
            .setCallback((index, type, action) -> {
                GridScreen.open(player);
            })
        );
        
        // Coins display
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.GOLD_INGOT)
            .setName(Component.literal("§6Coins: §e" + state.getCoins()))
        );
        
        // Building options
        int slot = 10;
        for (BuildingType type : BuildingType.values()) {
            if (type == BuildingType.EMPTY || type == BuildingType.TOWN_HALL) continue;
            if (slot > 16) break;  // Only show 7 options
            
            // Check if tech is unlocked for this building
            if (!canBuildType(type)) {
                this.setSlot(slot, createLockedBuildingButton(type));
            } else {
                this.setSlot(slot, createBuildingButton(type));
            }
            slot++;
        }
    }
    
    private boolean canBuildType(BuildingType type) {
        TechTree techTree = state.getTechTree();
        return switch (type) {
            case GREENHOUSE -> techTree.isUnlocked(TechTree.TechNode.FARMING_I);
            case QUARRY -> techTree.isUnlocked(TechTree.TechNode.MINING_I);
            case LUMBER_YARD -> techTree.isUnlocked(TechTree.TechNode.FORESTRY_I);
            case MOB_BARN -> techTree.isUnlocked(TechTree.TechNode.RANCHING_I);
            case HOUSE -> techTree.isUnlocked(TechTree.TechNode.HOUSING_I);
            case MARKET -> techTree.isUnlocked(TechTree.TechNode.COMMERCE_I);
            case BANK -> techTree.isUnlocked(TechTree.TechNode.COMMERCE_II);
            case ACADEMY -> techTree.isUnlocked(TechTree.TechNode.EDUCATION_I);
            case GUARD_TOWER -> techTree.isUnlocked(TechTree.TechNode.DEFENSE_I);
            default -> true;
        };
    }
    
    private GuiElementBuilder createBuildingButton(BuildingType type) {
        int cost = type.getBaseCost();
        boolean canAfford = state.getCoins() >= cost;
        
        GuiElementBuilder builder = new GuiElementBuilder()
            .setItem(type.getIconItem())
            .setName(Component.literal((canAfford ? "§a" : "§c") + type.getDisplayName()));
        
        builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
        builder.addLoreLine(Component.literal("§7Cost: " + (canAfford ? "§e" : "§c") + cost + " coins"));
        
        if (type.isProducer()) {
            builder.addLoreLine(Component.literal("§7Type: §bProduction"));
            builder.addLoreLine(Component.literal("§7Requires worker"));
        } else {
            builder.addLoreLine(Component.literal("§7Type: §dUtility"));
        }
        
        builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
        
        if (canAfford) {
            builder.addLoreLine(Component.literal("§aClick to build!"));
            builder.setCallback((index, clickType, action) -> {
                if (state.constructBuilding(gridX, gridZ, type)) {
                    GuiHelper.playSound(player, SoundEvents.ANVIL_USE, 1.0f, 1.2f);
                    player.sendSystemMessage(Component.literal("§a✓ Built " + type.getDisplayName() + "!"));
                    GridScreen.open(player);
                }
            });
        } else {
            builder.addLoreLine(Component.literal("§cNot enough coins!"));
            builder.setCallback((index, clickType, action) -> {
                GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
            });
        }
        
        return builder;
    }
    
    private GuiElementBuilder createLockedBuildingButton(BuildingType type) {
        return new GuiElementBuilder()
            .setItem(Items.BARRIER)
            .setName(Component.literal("§8" + type.getDisplayName() + " §c[LOCKED]"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§cResearch required!"))
            .addLoreLine(Component.literal("§7Visit the University"))
            .addLoreLine(Component.literal("§7to unlock this building."))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .setCallback((index, type2, action) -> {
                GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
            });
    }
}
