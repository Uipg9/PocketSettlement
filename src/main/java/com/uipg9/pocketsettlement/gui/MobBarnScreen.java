package com.uipg9.pocketsettlement.gui;

import com.uipg9.pocketsettlement.data.*;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

/**
 * Mob Barn production screen - animal husbandry.
 */
public class MobBarnScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private final int gridX;
    private final int gridZ;
    private final Building building;
    
    public MobBarnScreen(ServerPlayer player, int gridX, int gridZ) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.building = state.getBuilding(gridX, gridZ);
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player, int gridX, int gridZ) {
        MobBarnScreen screen = new MobBarnScreen(player, gridX, gridZ);
        screen.setTitle(Component.literal("§e§l🐄 Mob Barn"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background with yellow theme
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.YELLOW_STAINED_GLASS_PANE)
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
            .setItem(Items.WHEAT)
            .setName(Component.literal("§e§lMob Barn §7Lv" + building.getLevel()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Worker: §f" + workerName))
            .addLoreLine(Component.literal("§7Efficiency: §f" + building.getEfficiency(state) + "%"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Progress display
        int progress = building.getProgress();
        String progressBar = createProgressBar(progress);
        
        this.setSlot(13, new GuiElementBuilder()
            .setItem(progress >= 100 ? Items.COOKED_BEEF : Items.BEEF)
            .setName(Component.literal("§eRanching Progress"))
            .addLoreLine(Component.literal("§7" + progressBar + " §f" + progress + "%"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(building.hasWorker() ? 
                Component.literal("§eRancher is working...") : 
                Component.literal("§cAssign a worker to ranch!"))
        );
        
        // Animal pens (visual representation)
        this.setSlot(10, new GuiElementBuilder()
            .setItem(Items.COW_SPAWN_EGG)
            .setName(Component.literal("§6Cattle"))
            .addLoreLine(Component.literal("§7Produces: Leather, Beef"))
            .addLoreLine(Component.literal("§a✓ Available"))
        );
        
        this.setSlot(11, new GuiElementBuilder()
            .setItem(Items.PIG_SPAWN_EGG)
            .setName(Component.literal("§dPigs"))
            .addLoreLine(Component.literal("§7Produces: Porkchop"))
            .addLoreLine(Component.literal("§a✓ Available"))
        );
        
        boolean hasAdvanced = state.getTechTree().isUnlocked(TechTree.TechNode.RANCHING_II);
        
        this.setSlot(14, new GuiElementBuilder()
            .setItem(Items.SHEEP_SPAWN_EGG)
            .setName(Component.literal("§fSheep"))
            .addLoreLine(Component.literal("§7Produces: Wool, Mutton"))
            .addLoreLine(hasAdvanced ? Component.literal("§a✓ Available") : Component.literal("§c✗ Ranching II"))
        );
        
        this.setSlot(15, new GuiElementBuilder()
            .setItem(Items.CHICKEN_SPAWN_EGG)
            .setName(Component.literal("§eChickens"))
            .addLoreLine(Component.literal("§7Produces: Eggs, Feathers"))
            .addLoreLine(hasAdvanced ? Component.literal("§a✓ Available") : Component.literal("§c✗ Ranching II"))
        );
        
        // Stockpile preview
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.CHEST)
            .setName(Component.literal("§6Quick Stockpile View"))
            .addLoreLine(Component.literal("§7Leather: §f" + state.getStockpile().getResourceCount(Items.LEATHER)))
            .addLoreLine(Component.literal("§7Beef: §f" + state.getStockpile().getResourceCount(Items.BEEF)))
            .addLoreLine(Component.literal("§7Porkchop: §f" + state.getStockpile().getResourceCount(Items.PORKCHOP)))
            .addLoreLine(Component.literal("§7Wool: §f" + state.getStockpile().getResourceCount(Items.WHITE_WOOL)))
            .addLoreLine(Component.literal("§7Eggs: §f" + state.getStockpile().getResourceCount(Items.EGG)))
        );
    }
    
    private String createProgressBar(int progress) {
        int filled = progress / 10;
        StringBuilder bar = new StringBuilder("§e");
        for (int i = 0; i < filled; i++) bar.append("█");
        bar.append("§8");
        for (int i = filled; i < 10; i++) bar.append("█");
        return bar.toString();
    }
}
