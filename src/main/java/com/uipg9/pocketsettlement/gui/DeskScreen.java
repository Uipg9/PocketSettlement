package com.uipg9.pocketsettlement.gui;

import com.uipg9.pocketsettlement.data.*;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

/**
 * Main settlement GUI - The Governor's Desk
 * 
 * Layout (9x6 = 54 slots):
 * Row 0: Header bar (Title, Stats)
 * Row 1-5: Settlement grid (7x7 centered) with navigation
 */
public class DeskScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    
    public DeskScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        
        // Play open sound
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.MASTER, 1.0f, 1.0f);
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player) {
        DeskScreen screen = new DeskScreen(player);
        screen.setTitle(Component.literal("§6§l✦ Governor's Desk ✦"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background with parchment-colored glass
        fillBackground();
        
        // Header row
        setupHeaderBar();
        
        // Main navigation buttons
        setupNavigationButtons();
        
        // Settlement grid preview (mini-map style)
        setupGridPreview();
    }
    
    private void fillBackground() {
        GuiElementBuilder parchment = new GuiElementBuilder()
            .setItem(Items.YELLOW_STAINED_GLASS_PANE)
            .setName(Component.literal(""));
        
        // Fill all slots
        for (int i = 0; i < 54; i++) {
            this.setSlot(i, parchment);
        }
    }
    
    private void setupHeaderBar() {
        // Title/Info (slot 4 - center)
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.BELL)
            .setName(Component.literal("§6§lPocket Settlement"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§eCoins: §6" + formatNumber(state.getCoins())))
            .addLoreLine(Component.literal("§bInfluence: §3" + state.getInfluence()))
            .addLoreLine(Component.literal("§aCitizens: §2" + state.getCitizenCount() + "/" + state.getMaxCitizens()))
            .addLoreLine(Component.literal("§7Day " + state.getDaysPlayed()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Close button (slot 8)
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.BARRIER)
            .setName(Component.literal("§c✖ Close"))
            .setCallback((index, type, action) -> {
                this.close();
            })
        );
        
        // Help button (slot 0)
        this.setSlot(0, new GuiElementBuilder()
            .setItem(Items.BOOK)
            .setName(Component.literal("§e? Help"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§fWelcome, Governor!"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Manage your settlement from"))
            .addLoreLine(Component.literal("§7this desk. Build structures,"))
            .addLoreLine(Component.literal("§7hire citizens, and complete"))
            .addLoreLine(Component.literal("§7contracts to grow your colony!"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
    }
    
    private void setupNavigationButtons() {
        // Row 2: Main navigation buttons
        
        // Settlement Grid (slot 10)
        this.setSlot(10, new GuiElementBuilder()
            .setItem(Items.GRASS_BLOCK)
            .setName(Component.literal("§a§l⬛ Settlement Grid"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§fView and manage your"))
            .addLoreLine(Component.literal("§f7×7 settlement grid."))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Buildings: " + countBuildings()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§eClick to open!"))
            .setCallback((index, type, action) -> {
                playClickSound();
                GridScreen.open(player);
            })
        );
        
        // Citizens (slot 12)
        this.setSlot(12, new GuiElementBuilder()
            .setItem(Items.PLAYER_HEAD)
            .setName(Component.literal("§b§l☻ Citizenry"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§fRecruit and manage"))
            .addLoreLine(Component.literal("§fyour citizens."))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Population: §f" + state.getCitizenCount() + "/" + state.getMaxCitizens()))
            .addLoreLine(Component.literal("§7Happiness: §f" + state.getAverageHappiness() + "%"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§eClick to open!"))
            .setCallback((index, type, action) -> {
                playClickSound();
                CitizenScreen.open(player);
            })
        );
        
        // Stockpile (slot 14)
        this.setSlot(14, new GuiElementBuilder()
            .setItem(Items.CHEST)
            .setName(Component.literal("§6§l📦 Stockpile"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§fView stored resources"))
            .addLoreLine(Component.literal("§fand withdraw items."))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Items: §f" + state.getStockpile().getTotalItems() + "/" + state.getStockpile().getMaxCapacity()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§eClick to open!"))
            .setCallback((index, type, action) -> {
                playClickSound();
                StockpileScreen.open(player);
            })
        );
        
        // Contracts (slot 16)
        this.setSlot(16, new GuiElementBuilder()
            .setItem(Items.PAPER)
            .setName(Component.literal("§d§l📜 Contracts"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§fComplete contracts"))
            .addLoreLine(Component.literal("§fto earn coins!"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Active: §f" + state.getActiveContracts().size()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§eClick to open!"))
            .setCallback((index, type, action) -> {
                playClickSound();
                ContractScreen.open(player);
            })
        );
        
        // Row 3: Secondary navigation
        
        // Tech Tree (slot 22 - center)
        this.setSlot(22, new GuiElementBuilder()
            .setItem(Items.ENCHANTING_TABLE)
            .setName(Component.literal("§5§l🎓 University"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§fResearch new technologies"))
            .addLoreLine(Component.literal("§fto unlock buildings and"))
            .addLoreLine(Component.literal("§fupgrades!"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Unlocked: §f" + state.getTechTree().getUnlockedCount() + "/" + TechTree.TechNode.values().length))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§eClick to open!"))
            .setCallback((index, type, action) -> {
                playClickSound();
                TechScreen.open(player);
            })
        );
    }
    
    private void setupGridPreview() {
        // Bottom section: Mini grid preview (3x3 in center)
        // Shows a simplified view of the settlement
        
        // Decorative frame
        int[] frameSlots = {36, 37, 38, 39, 41, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};
        GuiElementBuilder frame = new GuiElementBuilder()
            .setItem(Items.BROWN_STAINED_GLASS_PANE)
            .setName(Component.literal(""));
        
        for (int slot : frameSlots) {
            this.setSlot(slot, frame);
        }
        
        // Center slot - settlement summary
        this.setSlot(49, new GuiElementBuilder()
            .setItem(Items.BELL)
            .setName(Component.literal("§6Settlement Overview"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Houses: §f" + state.getBuildingCount(BuildingType.HOUSE)))
            .addLoreLine(Component.literal("§7Farms: §f" + state.getBuildingCount(BuildingType.GREENHOUSE)))
            .addLoreLine(Component.literal("§7Quarries: §f" + state.getBuildingCount(BuildingType.QUARRY)))
            .addLoreLine(Component.literal("§7Lumber Yards: §f" + state.getBuildingCount(BuildingType.LUMBER_YARD)))
            .addLoreLine(Component.literal("§7Barns: §f" + state.getBuildingCount(BuildingType.MOB_BARN)))
            .addLoreLine(Component.literal("§7Markets: §f" + state.getBuildingCount(BuildingType.MARKET)))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Stats in corner slots
        this.setSlot(40, new GuiElementBuilder()
            .setItem(Items.GOLD_INGOT)
            .setName(Component.literal("§6Total Earned: §e" + formatNumber(state.getTotalCoinsEarned()) + " coins"))
            .addLoreLine(Component.literal("§7Items Produced: §f" + formatNumber(state.getTotalItemsProduced())))
        );
    }
    
    private int countBuildings() {
        int count = 0;
        for (int x = 0; x < SettlementState.GRID_SIZE; x++) {
            for (int z = 0; z < SettlementState.GRID_SIZE; z++) {
                if (!state.getBuilding(x, z).isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    private void playClickSound() {
        GuiHelper.playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
}
