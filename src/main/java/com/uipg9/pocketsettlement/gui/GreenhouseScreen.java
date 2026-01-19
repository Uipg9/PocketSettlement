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
 * Greenhouse production screen - farming interface.
 */
public class GreenhouseScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private final int gridX;
    private final int gridZ;
    private final Building building;
    
    public GreenhouseScreen(ServerPlayer player, int gridX, int gridZ) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.building = state.getBuilding(gridX, gridZ);
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player, int gridX, int gridZ) {
        GreenhouseScreen screen = new GreenhouseScreen(player, gridX, gridZ);
        screen.setTitle(Component.literal("§a§l🌿 Greenhouse"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background with green theme
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.LIME_STAINED_GLASS_PANE)
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
        
        // Building info (center)
        Citizen worker = building.hasWorker() ? state.getCitizen(building.getAssignedCitizenId()) : null;
        String workerName = worker != null ? worker.getName() : "None";
        
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.HAY_BLOCK)
            .setName(Component.literal("§a§lGreenhouse §7Lv" + building.getLevel()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Worker: §f" + workerName))
            .addLoreLine(Component.literal("§7Efficiency: §f" + building.getEfficiency(state) + "%"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Progress display (slot 13 - center bottom area)
        int progress = building.getProgress();
        String progressBar = createProgressBar(progress);
        
        this.setSlot(13, new GuiElementBuilder()
            .setItem(progress >= 100 ? Items.GOLDEN_CARROT : Items.CARROT)
            .setName(Component.literal("§eProduction Progress"))
            .addLoreLine(Component.literal("§7" + progressBar + " §f" + progress + "%"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(building.hasWorker() ? 
                Component.literal("§aWorker is farming...") : 
                Component.literal("§cAssign a worker to produce!"))
        );
        
        // Output display - show what can be produced
        this.setSlot(10, createCropDisplay(Items.WHEAT, "Wheat"));
        this.setSlot(11, createCropDisplay(Items.CARROT, "Carrot"));
        this.setSlot(12, createCropDisplay(Items.POTATO, "Potato"));
        
        // Advanced crops (if unlocked)
        if (state.getTechTree().isUnlocked(TechTree.TechNode.FARMING_II)) {
            this.setSlot(14, createCropDisplay(Items.BEETROOT, "Beetroot"));
            this.setSlot(15, createCropDisplay(Items.MELON_SLICE, "Melon"));
        }
        if (state.getTechTree().isUnlocked(TechTree.TechNode.FARMING_III)) {
            this.setSlot(16, createCropDisplay(Items.PUMPKIN, "Pumpkin"));
        }
        
        // Stockpile preview
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.CHEST)
            .setName(Component.literal("§6Quick Stockpile View"))
            .addLoreLine(Component.literal("§7Wheat: §f" + state.getStockpile().getResourceCount(Items.WHEAT)))
            .addLoreLine(Component.literal("§7Carrots: §f" + state.getStockpile().getResourceCount(Items.CARROT)))
            .addLoreLine(Component.literal("§7Potatoes: §f" + state.getStockpile().getResourceCount(Items.POTATO)))
        );
    }
    
    private GuiElementBuilder createCropDisplay(net.minecraft.world.item.Item item, String name) {
        int stock = state.getStockpile().getResourceCount(item);
        return new GuiElementBuilder()
            .setItem(item)
            .setName(Component.literal("§a" + name))
            .addLoreLine(Component.literal("§7In stockpile: §f" + stock));
    }
    
    private String createProgressBar(int progress) {
        int filled = progress / 10;
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < filled; i++) bar.append("█");
        bar.append("§8");
        for (int i = filled; i < 10; i++) bar.append("█");
        return bar.toString();
    }
}
