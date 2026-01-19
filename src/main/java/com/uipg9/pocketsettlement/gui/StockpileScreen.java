package com.uipg9.pocketsettlement.gui;

import com.uipg9.pocketsettlement.data.*;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stockpile view and withdrawal screen.
 */
public class StockpileScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 21;
    
    public StockpileScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player) {
        StockpileScreen screen = new StockpileScreen(player);
        screen.setTitle(Component.literal("§6§l📦 Stockpile"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.ORANGE_STAINED_GLASS_PANE)
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
        
        // Stockpile info
        Stockpile stockpile = state.getStockpile();
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.CHEST)
            .setName(Component.literal("§6§lStockpile"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Capacity: §f" + stockpile.getTotalItems() + "/" + stockpile.getMaxCapacity()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§eClick items to withdraw"))
        );
        
        // Display stored resources (rows 1-4)
        Map<Item, Integer> resources = stockpile.getAllResources();
        List<Map.Entry<Item, Integer>> resourceList = new ArrayList<>(resources.entrySet());
        
        // Content slots (7 columns x 4 rows = 28, but we use 21 per page)
        int[][] contentSlots = {
            {10, 11, 12, 13, 14, 15, 16},
            {19, 20, 21, 22, 23, 24, 25},
            {28, 29, 30, 31, 32, 33, 34},
        };
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int itemIndex = 0;
        
        for (int[] row : contentSlots) {
            for (int slot : row) {
                int globalIndex = startIndex + itemIndex;
                
                if (globalIndex < resourceList.size()) {
                    Map.Entry<Item, Integer> entry = resourceList.get(globalIndex);
                    this.setSlot(slot, createResourceElement(entry.getKey(), entry.getValue()));
                }
                itemIndex++;
            }
        }
        
        // Pagination controls (bottom row)
        int totalPages = Math.max(1, (int) Math.ceil((double) resourceList.size() / ITEMS_PER_PAGE));
        
        // Previous page
        this.setSlot(45, new GuiElementBuilder()
            .setItem(currentPage > 0 ? Items.ARROW : Items.GRAY_DYE)
            .setName(Component.literal(currentPage > 0 ? "§e← Previous Page" : "§8No Previous Page"))
            .setCallback((index, type, action) -> {
                if (currentPage > 0) {
                    currentPage--;
                    setupScreen();
                }
            })
        );
        
        // Page indicator
        this.setSlot(49, new GuiElementBuilder()
            .setItem(Items.PAPER)
            .setName(Component.literal("§ePage " + (currentPage + 1) + "/" + totalPages))
        );
        
        // Next page
        this.setSlot(53, new GuiElementBuilder()
            .setItem(currentPage < totalPages - 1 ? Items.ARROW : Items.GRAY_DYE)
            .setName(Component.literal(currentPage < totalPages - 1 ? "§eNext Page →" : "§8No Next Page"))
            .setCallback((index, type, action) -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    setupScreen();
                }
            })
        );
    }
    
    private GuiElementBuilder createResourceElement(Item item, int count) {
        int displayCount = Math.min(64, count);
        
        return new GuiElementBuilder()
            .setItem(item)
            .setCount(displayCount)
            .setName(Component.literal("§f" + item.getName().getString()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Stored: §e" + count))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§aLeft-click: Withdraw 1 stack"))
            .addLoreLine(Component.literal("§aShift-click: Withdraw all"))
            .setCallback((index, type, action) -> {
                int toWithdraw = type.shift ? count : Math.min(item.getDefaultMaxStackSize(), count);
                int withdrawn = state.getStockpile().withdrawToPlayer(player, item, toWithdraw);
                
                if (withdrawn > 0) {
                    GuiHelper.playSound(player, SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);
                    player.sendSystemMessage(Component.literal("§a✓ Withdrew " + withdrawn + "x " + item.getName().getString()));
                    state.setDirty();
                    StockpileScreen.open(player);  // Refresh
                }
            });
    }
}
