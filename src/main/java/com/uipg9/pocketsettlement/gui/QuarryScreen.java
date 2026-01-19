package com.uipg9.pocketsettlement.gui;

import com.uipg9.pocketsettlement.data.*;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

/**
 * Quarry production screen - mining interface.
 */
public class QuarryScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private final int gridX;
    private final int gridZ;
    private final Building building;
    
    public QuarryScreen(ServerPlayer player, int gridX, int gridZ) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.building = state.getBuilding(gridX, gridZ);
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player, int gridX, int gridZ) {
        QuarryScreen screen = new QuarryScreen(player, gridX, gridZ);
        screen.setTitle(Component.literal("§8§l⛏ Quarry"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background with gray theme
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.GRAY_STAINED_GLASS_PANE)
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
            .setItem(Items.IRON_PICKAXE)
            .setName(Component.literal("§8§lQuarry §7Lv" + building.getLevel()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Worker: §f" + workerName))
            .addLoreLine(Component.literal("§7Efficiency: §f" + building.getEfficiency(state) + "%"))
            .addLoreLine(Component.literal("§7Depth: §f" + getDepthString(building.getLevel())))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Progress display
        int progress = building.getProgress();
        String progressBar = createProgressBar(progress);
        
        this.setSlot(13, new GuiElementBuilder()
            .setItem(progress >= 100 ? Items.DIAMOND : Items.COBBLESTONE)
            .setName(Component.literal("§7Mining Progress"))
            .addLoreLine(Component.literal("§7" + progressBar + " §f" + progress + "%"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(building.hasWorker() ? 
                Component.literal("§8Miners are excavating...") : 
                Component.literal("§cAssign a worker to mine!"))
        );
        
        // Depth layers visualization
        this.setSlot(10, new GuiElementBuilder()
            .setItem(Items.COBBLESTONE)
            .setName(Component.literal("§7Layer 1: Surface"))
            .addLoreLine(Component.literal("§fProduces: Stone, Coal"))
            .addLoreLine(building.getLevel() >= 1 ? Component.literal("§a✓ Accessible") : Component.literal("§c✗ Locked"))
        );
        
        this.setSlot(11, new GuiElementBuilder()
            .setItem(Items.IRON_ORE)
            .setName(Component.literal("§8Layer 2: Deep Stone"))
            .addLoreLine(Component.literal("§fProduces: Iron, Gold"))
            .addLoreLine(building.getLevel() >= 3 && state.getTechTree().isUnlocked(TechTree.TechNode.MINING_II) ? 
                Component.literal("§a✓ Accessible") : Component.literal("§c✗ Level 3 + Mining II"))
        );
        
        this.setSlot(12, new GuiElementBuilder()
            .setItem(Items.DIAMOND_ORE)
            .setName(Component.literal("§bLayer 3: Bedrock"))
            .addLoreLine(Component.literal("§fProduces: Diamonds"))
            .addLoreLine(building.getLevel() >= 5 && state.getTechTree().isUnlocked(TechTree.TechNode.MINING_III) ? 
                Component.literal("§a✓ Accessible") : Component.literal("§c✗ Level 5 + Mining III"))
        );
        
        // Stockpile preview
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.CHEST)
            .setName(Component.literal("§6Quick Stockpile View"))
            .addLoreLine(Component.literal("§7Stone: §f" + state.getStockpile().getResourceCount(Items.COBBLESTONE)))
            .addLoreLine(Component.literal("§7Coal: §f" + state.getStockpile().getResourceCount(Items.COAL)))
            .addLoreLine(Component.literal("§7Iron: §f" + state.getStockpile().getResourceCount(Items.RAW_IRON)))
            .addLoreLine(Component.literal("§7Gold: §f" + state.getStockpile().getResourceCount(Items.RAW_GOLD)))
            .addLoreLine(Component.literal("§7Diamonds: §f" + state.getStockpile().getResourceCount(Items.DIAMOND)))
        );
    }
    
    private String getDepthString(int level) {
        return switch (level) {
            case 1 -> "Surface (0-16)";
            case 2 -> "Shallow (0-32)";
            case 3 -> "Medium (0-48)";
            case 4 -> "Deep (0-64)";
            case 5 -> "Bedrock (0-Bedrock)";
            default -> "Unknown";
        };
    }
    
    private String createProgressBar(int progress) {
        int filled = progress / 10;
        StringBuilder bar = new StringBuilder("§8");
        for (int i = 0; i < filled; i++) bar.append("█");
        bar.append("§0");
        for (int i = filled; i < 10; i++) bar.append("█");
        return bar.toString();
    }
}
