package com.uipg9.pocketsettlement.gui;

import com.uipg9.pocketsettlement.data.*;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

/**
 * Lumber Yard production screen.
 */
public class LumberYardScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private final int gridX;
    private final int gridZ;
    private final Building building;
    
    public LumberYardScreen(ServerPlayer player, int gridX, int gridZ) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.building = state.getBuilding(gridX, gridZ);
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player, int gridX, int gridZ) {
        LumberYardScreen screen = new LumberYardScreen(player, gridX, gridZ);
        screen.setTitle(Component.literal("§6§l🌲 Lumber Yard"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background with brown theme
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.BROWN_STAINED_GLASS_PANE)
            .setName(Component.literal(""));
        
        for (int i = 0; i < 27; i++) {
            this.setSlot(i, bg);
        }
        
        // Back button
        this.setSlot(0, new GuiElementBuilder()
            .setItem(Items.ARROW)
            .setName(Component.literal("§e← Back to Grid"))
            .setCallback((index, type, action) -> {
                GridScreen.open(player);
            })
        );
        
        // Building info
        Citizen worker = building.hasWorker() ? state.getCitizen(building.getAssignedCitizenId()) : null;
        String workerName = worker != null ? worker.getName() : "None";
        
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.IRON_AXE)
            .setName(Component.literal("§6§lLumber Yard §7Lv" + building.getLevel()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Worker: §f" + workerName))
            .addLoreLine(Component.literal("§7Efficiency: §f" + building.getEfficiency(state) + "%"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Progress display
        int progress = building.getProgress();
        String progressBar = createProgressBar(progress);
        
        this.setSlot(13, new GuiElementBuilder()
            .setItem(progress >= 100 ? Items.OAK_LOG : Items.OAK_SAPLING)
            .setName(Component.literal("§6Harvesting Progress"))
            .addLoreLine(Component.literal("§7" + progressBar + " §f" + progress + "%"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(building.hasWorker() ? 
                Component.literal("§6Lumberjack is chopping...") : 
                Component.literal("§cAssign a worker to chop!"))
        );
        
        // Tree types (visual)
        this.setSlot(10, new GuiElementBuilder()
            .setItem(Items.OAK_LOG)
            .setName(Component.literal("§6Oak Trees"))
            .addLoreLine(Component.literal("§7Standard production"))
        );
        
        this.setSlot(11, new GuiElementBuilder()
            .setItem(Items.OAK_PLANKS)
            .setName(Component.literal("§6Processed Planks"))
            .addLoreLine(Component.literal("§7Bonus output (30% chance)"))
        );
        
        this.setSlot(12, new GuiElementBuilder()
            .setItem(Items.STICK)
            .setName(Component.literal("§6Sticks"))
            .addLoreLine(Component.literal("§7From leftover branches"))
        );
        
        // Stockpile preview
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.CHEST)
            .setName(Component.literal("§6Quick Stockpile View"))
            .addLoreLine(Component.literal("§7Logs: §f" + state.getStockpile().getResourceCount(Items.OAK_LOG)))
            .addLoreLine(Component.literal("§7Planks: §f" + state.getStockpile().getResourceCount(Items.OAK_PLANKS)))
            .addLoreLine(Component.literal("§7Sticks: §f" + state.getStockpile().getResourceCount(Items.STICK)))
        );
    }
    
    private String createProgressBar(int progress) {
        int filled = progress / 10;
        StringBuilder bar = new StringBuilder("§6");
        for (int i = 0; i < filled; i++) bar.append("█");
        bar.append("§8");
        for (int i = filled; i < 10; i++) bar.append("█");
        return bar.toString();
    }
}
