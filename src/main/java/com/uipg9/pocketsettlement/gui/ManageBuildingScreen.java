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
 * Manage an existing building - upgrade or demolish.
 */
public class ManageBuildingScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private final int gridX;
    private final int gridZ;
    private final Building building;
    
    public ManageBuildingScreen(ServerPlayer player, int gridX, int gridZ) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.building = state.getBuilding(gridX, gridZ);
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player, int gridX, int gridZ) {
        ManageBuildingScreen screen = new ManageBuildingScreen(player, gridX, gridZ);
        screen.setTitle(Component.literal("§6§lManage Building"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.ORANGE_STAINED_GLASS_PANE)
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
        
        // Building info (center top)
        this.setSlot(4, new GuiElementBuilder()
            .setItem(building.getType().getIconItem())
            .setName(Component.literal("§6§l" + building.getType().getDisplayName()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Level: §f" + building.getLevel() + "/5"))
            .addLoreLine(Component.literal("§7Efficiency: §f" + building.getEfficiency(state) + "%"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Upgrade button (slot 11)
        if (building.getLevel() < 5) {
            int upgradeCost = building.getType().getUpgradeCost(building.getLevel() + 1);
            boolean canAfford = state.getCoins() >= upgradeCost;
            
            this.setSlot(11, new GuiElementBuilder()
                .setItem(canAfford ? Items.EXPERIENCE_BOTTLE : Items.GLASS_BOTTLE)
                .setName(Component.literal((canAfford ? "§a" : "§c") + "⬆ Upgrade to Level " + (building.getLevel() + 1)))
                .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
                .addLoreLine(Component.literal("§7Cost: " + (canAfford ? "§e" : "§c") + upgradeCost + " coins"))
                .addLoreLine(Component.literal("§7Current: §fLevel " + building.getLevel()))
                .addLoreLine(Component.literal("§7After: §aLevel " + (building.getLevel() + 1)))
                .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
                .addLoreLine(Component.literal(canAfford ? "§aClick to upgrade!" : "§cNot enough coins!"))
                .setCallback((index, type, action) -> {
                    if (canAfford && state.spendCoins(upgradeCost)) {
                        building.upgrade();
                        state.setDirty();
                        GuiHelper.playSound(player, SoundEvents.ANVIL_USE, 1.0f, 1.5f);
                        player.sendSystemMessage(Component.literal("§a✓ Upgraded to Level " + building.getLevel() + "!"));
                        ManageBuildingScreen.open(player, gridX, gridZ);  // Refresh
                    } else {
                        GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
                    }
                })
            );
        } else {
            this.setSlot(11, new GuiElementBuilder()
                .setItem(Items.NETHER_STAR)
                .setName(Component.literal("§b§l★ MAX LEVEL ★"))
                .addLoreLine(Component.literal("§7This building is fully"))
                .addLoreLine(Component.literal("§7upgraded. Nice work!"))
            );
        }
        
        // Assign worker button (slot 13)
        if (building.getType().isProducer()) {
            if (building.hasWorker()) {
                Citizen worker = state.getCitizen(building.getAssignedCitizenId());
                String workerName = worker != null ? worker.getName() : "Unknown";
                
                this.setSlot(13, new GuiElementBuilder()
                    .setItem(Items.PLAYER_HEAD)
                    .setName(Component.literal("§b☻ Worker: §f" + workerName))
                    .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
                    .addLoreLine(worker != null ? Component.literal("§7Level: §f" + worker.getLevel()) : Component.literal(""))
                    .addLoreLine(worker != null ? Component.literal("§7Job: §f" + worker.getJob().getDisplayName()) : Component.literal(""))
                    .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
                    .addLoreLine(Component.literal("§eClick to unassign"))
                    .setCallback((index, type, action) -> {
                        building.setAssignedCitizenId("");
                        if (worker != null) worker.setJob(CitizenJob.NONE);
                        state.setDirty();
                        player.sendSystemMessage(Component.literal("§e✓ Worker unassigned."));
                        ManageBuildingScreen.open(player, gridX, gridZ);
                    })
                );
            } else {
                this.setSlot(13, new GuiElementBuilder()
                    .setItem(Items.ARMOR_STAND)
                    .setName(Component.literal("§c⚠ No Worker"))
                    .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
                    .addLoreLine(Component.literal("§7This building needs"))
                    .addLoreLine(Component.literal("§7a worker to produce!"))
                    .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
                    .addLoreLine(Component.literal("§eClick to assign"))
                    .setCallback((index, type, action) -> {
                        AssignWorkerScreen.open(player, gridX, gridZ);
                    })
                );
            }
        }
        
        // Demolish button (slot 15)
        int refund = building.getType().getBaseCost() / 2;
        
        this.setSlot(15, new GuiElementBuilder()
            .setItem(Items.TNT)
            .setName(Component.literal("§c§l⚠ Demolish"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Destroy this building"))
            .addLoreLine(Component.literal("§7and reclaim the plot."))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Refund: §e" + refund + " coins §7(50%)"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§c⚠ This cannot be undone!"))
            .addLoreLine(Component.literal("§cShift-click to confirm"))
            .setCallback((index, type, action) -> {
                if (type.shift) {
                    if (state.demolishBuilding(gridX, gridZ)) {
                        GuiHelper.playSound(player, SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.0f);
                        player.sendSystemMessage(Component.literal("§c✓ Building demolished. Refunded: §e" + refund + " coins"));
                        GridScreen.open(player);
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§eShift-click to confirm demolition!"));
                }
            })
        );
    }
}
