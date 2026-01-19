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
 * Assign a worker to a building.
 */
public class AssignWorkerScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private final int gridX;
    private final int gridZ;
    
    public AssignWorkerScreen(ServerPlayer player, int gridX, int gridZ) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        this.gridX = gridX;
        this.gridZ = gridZ;
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player, int gridX, int gridZ) {
        AssignWorkerScreen screen = new AssignWorkerScreen(player, gridX, gridZ);
        screen.setTitle(Component.literal("§b§lAssign Worker"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.CYAN_STAINED_GLASS_PANE)
            .setName(Component.literal(""));
        
        for (int i = 0; i < 27; i++) {
            this.setSlot(i, bg);
        }
        
        // Back button
        this.setSlot(0, new GuiElementBuilder()
            .setItem(Items.ARROW)
            .setName(Component.literal("§e← Back"))
            .setCallback((index, type, action) -> {
                ManageBuildingScreen.open(player, gridX, gridZ);
            })
        );
        
        // Building info
        Building building = state.getBuilding(gridX, gridZ);
        this.setSlot(4, new GuiElementBuilder()
            .setItem(building.getType().getIconItem())
            .setName(Component.literal("§6Assign to: §f" + building.getType().getDisplayName()))
            .addLoreLine(Component.literal("§7Select an unemployed citizen"))
        );
        
        // List unemployed citizens
        List<Citizen> unemployed = new ArrayList<>();
        for (Citizen citizen : state.getAllCitizens()) {
            if (!citizen.isEmployed()) {
                unemployed.add(citizen);
            }
        }
        
        if (unemployed.isEmpty()) {
            this.setSlot(13, new GuiElementBuilder()
                .setItem(Items.BARRIER)
                .setName(Component.literal("§cNo Available Workers"))
                .addLoreLine(Component.literal("§7All citizens are employed"))
                .addLoreLine(Component.literal("§7or you have no citizens."))
                .addLoreLine(Component.literal(""))
                .addLoreLine(Component.literal("§eRecruit more citizens!"))
            );
        } else {
            int slot = 10;
            for (Citizen citizen : unemployed) {
                if (slot > 16) break;
                
                this.setSlot(slot, new GuiElementBuilder()
                    .setItem(Items.PLAYER_HEAD)
                    .setName(Component.literal("§b" + citizen.getName()))
                    .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
                    .addLoreLine(Component.literal("§7Level: §f" + citizen.getLevel()))
                    .addLoreLine(Component.literal("§7Efficiency: §f" + citizen.getEfficiencyPercent() + "%"))
                    .addLoreLine(Component.literal("§7Happiness: §f" + citizen.getHappiness() + "%"))
                    .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
                    .addLoreLine(Component.literal("§aClick to assign!"))
                    .setCallback((index, type, action) -> {
                        if (state.assignCitizen(citizen.getId(), gridX, gridZ)) {
                            GuiHelper.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            player.sendSystemMessage(Component.literal("§a✓ " + citizen.getName() + " assigned!"));
                            ManageBuildingScreen.open(player, gridX, gridZ);
                        }
                    })
                );
                slot++;
            }
        }
    }
}
