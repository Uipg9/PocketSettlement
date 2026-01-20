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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stockpile view with withdraw, deposit, sell, and contract tracking.
 */
public class StockpileScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 14;
    private ViewMode mode = ViewMode.RESOURCES;
    
    private enum ViewMode {
        RESOURCES, CONTRACTS
    }
    
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
        
        // Mode toggle button
        this.setSlot(2, new GuiElementBuilder()
            .setItem(mode == ViewMode.RESOURCES ? Items.CHEST : Items.PAPER)
            .setName(Component.literal(mode == ViewMode.RESOURCES ? "§6§lResources" : "§d§lContracts"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Current view: §f" + (mode == ViewMode.RESOURCES ? "Resources" : "Contracts")))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§eClick to switch!"))
            .setCallback((index, type, action) -> {
                mode = (mode == ViewMode.RESOURCES) ? ViewMode.CONTRACTS : ViewMode.RESOURCES;
                currentPage = 0;
                GuiHelper.playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
                setupScreen();
            })
        );
        
        // Stockpile info
        Stockpile stockpile = state.getStockpile();
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.ENDER_CHEST)
            .setName(Component.literal("§6§lStockpile & Trading"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Capacity: §f" + stockpile.getTotalItems() + "/" + stockpile.getMaxCapacity()))
            .addLoreLine(Component.literal("§7Coins: §6" + state.getCoins()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§fWithdraw: §aLeft-click items"))
            .addLoreLine(Component.literal("§fDeposit: §aRight-click items"))
            .addLoreLine(Component.literal("§fSell: §eShift + Right-click"))
        );
        
        // Deposit all button
        this.setSlot(6, new GuiElementBuilder()
            .setItem(Items.HOPPER)
            .setName(Component.literal("§a§lDeposit All"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Deposit all items from"))
            .addLoreLine(Component.literal("§7your inventory into"))
            .addLoreLine(Component.literal("§7the stockpile."))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§aClick to deposit!"))
            .setCallback((index, type, action) -> {
                int deposited = depositAllFromPlayer();
                if (deposited > 0) {
                    GuiHelper.playSound(player, SoundEvents.ITEM_PICKUP, 1.0f, 0.8f);
                    player.sendSystemMessage(Component.literal("§a✓ Deposited " + deposited + " items!"));
                    state.setDirty();
                    setupScreen();
                } else {
                    GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
                    player.sendSystemMessage(Component.literal("§7No items to deposit"));
                }
            })
        );
        
        if (mode == ViewMode.RESOURCES) {
            setupResourcesView();
        } else {
            setupContractsView();
        }
        
        setupPagination();
    }
    
    private void setupResourcesView() {
        Map<Item, Integer> resources = state.getStockpile().getAllResources();
        List<Map.Entry<Item, Integer>> resourceList = new ArrayList<>(resources.entrySet());
        
        // Content slots (7 columns x 2 rows = 14 items per page)
        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        
        for (int i = 0; i < contentSlots.length; i++) {
            int globalIndex = startIndex + i;
            
            if (globalIndex < resourceList.size()) {
                Map.Entry<Item, Integer> entry = resourceList.get(globalIndex);
                this.setSlot(contentSlots[i], createResourceElement(entry.getKey(), entry.getValue()));
            }
        }
    }
    
    private void setupContractsView() {
        List<Contract> contracts = state.getActiveContracts();
        
        // Content slots (7 columns x 2 rows = 14 items per page)
        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        
        for (int i = 0; i < contentSlots.length; i++) {
            int globalIndex = startIndex + i;
            
            if (globalIndex < contracts.size()) {
                Contract contract = contracts.get(globalIndex);
                this.setSlot(contentSlots[i], createContractElement(contract, globalIndex));
            }
        }
    }
    
    private void setupPagination() {
        int totalItems = (mode == ViewMode.RESOURCES) 
            ? state.getStockpile().getAllResources().size()
            : state.getActiveContracts().size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
        
        // Previous page
        this.setSlot(45, new GuiElementBuilder()
            .setItem(currentPage > 0 ? Items.ARROW : Items.GRAY_DYE)
            .setName(Component.literal(currentPage > 0 ? "§e← Previous Page" : "§8No Previous Page"))
            .setCallback((index, type, action) -> {
                if (currentPage > 0) {
                    currentPage--;
                    GuiHelper.playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
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
                    GuiHelper.playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
                    setupScreen();
                }
            })
        );
    }
    
    private GuiElementBuilder createContractElement(Contract contract, int index) {
        Item requestedItem = contract.getRequiredItem();
        int currentAmount = state.getStockpile().getResourceCount(requestedItem);
        int requiredAmount = contract.getRequiredAmount();
        int progress = Math.min(100, (currentAmount * 100) / requiredAmount);
        boolean canComplete = currentAmount >= requiredAmount;
        
        GuiElementBuilder builder = new GuiElementBuilder()
            .setItem(requestedItem)
            .setName(Component.literal((canComplete ? "§a" : "§e") + "Contract #" + (index + 1)));
        
        builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
        builder.addLoreLine(Component.literal("§7Request: §f" + requiredAmount + "x " + requestedItem.getName().getString()));
        builder.addLoreLine(Component.literal("§7Progress: §f" + currentAmount + "/" + requiredAmount + " §7(" + progress + "%)"));
        builder.addLoreLine(Component.literal("§7Reward: §6" + contract.getReward() + " coins"));
        builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
        
        if (canComplete) {
            builder.addLoreLine(Component.literal("§a✓ Ready to complete!"));
            builder.addLoreLine(Component.literal("§aClick to fulfill!"));
            builder.setCallback((i, type, action) -> {
                if (state.getStockpile().removeResource(requestedItem, requiredAmount)) {
                    contract.deliver(requiredAmount);
                    state.addCoins(contract.getReward());
                    state.getActiveContracts().remove(contract);
                    GuiHelper.playSound(player, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.2f);
                    player.sendSystemMessage(Component.literal("§a§l✓ Contract completed! +" + contract.getReward() + " coins"));
                    state.setDirty();
                    setupScreen();
                }
            });
        } else {
            builder.addLoreLine(Component.literal("§c✗ Insufficient resources"));
        }
        
        return builder;
    }
    
    private int depositAllFromPlayer() {
        int totalDeposited = 0;
        
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                int amount = stack.getCount();
                int added = state.getStockpile().addResource(stack.getItem(), amount);
                if (added > 0) {
                    player.getInventory().removeItem(slot, amount);
                    totalDeposited += amount;
                }
            }
        }
        
        return totalDeposited;
    }
    
    private int getSellPrice(Item item) {
        // Base prices for common items
        if (item == Items.WHEAT || item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT) {
            return 2;  // 2 coins per crop
        } else if (item == Items.APPLE || item == Items.MELON_SLICE || item == Items.SWEET_BERRIES) {
            return 3;  // 3 coins per fruit
        } else if (item == Items.COBBLESTONE || item == Items.STONE) {
            return 1;  // 1 coin per stone
        } else if (item == Items.IRON_ORE || item == Items.COPPER_ORE || item == Items.COAL_ORE) {
            return 5;  // 5 coins per raw ore
        } else if (item == Items.GOLD_ORE || item == Items.DIAMOND_ORE) {
            return 20;  // 20 coins per valuable ore
        } else if (item == Items.OAK_LOG || item == Items.BIRCH_LOG || item == Items.SPRUCE_LOG) {
            return 2;  // 2 coins per log
        } else if (item == Items.BEEF || item == Items.PORKCHOP || item == Items.CHICKEN) {
            return 4;  // 4 coins per meat
        } else if (item == Items.LEATHER) {
            return 6;  // 6 coins per leather
        } else if (item == Items.WHITE_WOOL) {
            return 3;  // 3 coins per wool
        }
        return 1;  // Default 1 coin for misc items
    }
    
    private GuiElementBuilder createResourceElement(Item item, int count) {
        int displayCount = Math.min(64, count);
        int sellPrice = getSellPrice(item);
        int totalValue = sellPrice * count;
        
        return new GuiElementBuilder()
            .setItem(item)
            .setCount(displayCount)
            .setName(Component.literal("§f" + item.getName().getString()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Stored: §e" + count))
            .addLoreLine(Component.literal("§7Value: §6" + totalValue + " coins §7(" + sellPrice + " ea)"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§aLeft-click: Withdraw 1 stack"))
            .addLoreLine(Component.literal("§aShift-left: Withdraw all"))
            .addLoreLine(Component.literal("§eRight-click: Deposit from inv"))
            .addLoreLine(Component.literal("§6Drop (Q): Sell all"))
            .setCallback((index, type, action) -> {
                if (type == eu.pb4.sgui.api.ClickType.DROP) {
                    // Drop key (Q): Sell all
                    int coinsEarned = count * sellPrice;
                    state.getStockpile().removeResource(item, count);
                    state.addCoins(coinsEarned);
                    GuiHelper.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.sendSystemMessage(Component.literal("§6✓ Sold " + count + "x " + item.getName().getString() + " for " + coinsEarned + " coins!"));
                    state.setDirty();
                    setupScreen();
                } else if (type.isRight) {
                    // Right-click: Deposit from inventory
                    int deposited = depositItemFromPlayer(item);
                    if (deposited > 0) {
                        GuiHelper.playSound(player, SoundEvents.ITEM_PICKUP, 1.0f, 0.8f);
                        player.sendSystemMessage(Component.literal("§a✓ Deposited " + deposited + "x " + item.getName().getString()));
                        state.setDirty();
                        setupScreen();
                    } else {
                        GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
                        player.sendSystemMessage(Component.literal("§7No " + item.getName().getString() + " in inventory"));
                    }
                } else {
                    // Left-click actions: Withdraw
                    int toWithdraw = type.shift ? count : Math.min(item.getDefaultMaxStackSize(), count);
                    int withdrawn = state.getStockpile().withdrawToPlayer(player, item, toWithdraw);
                    
                    if (withdrawn > 0) {
                        GuiHelper.playSound(player, SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);
                        player.sendSystemMessage(Component.literal("§a✓ Withdrew " + withdrawn + "x " + item.getName().getString()));
                        state.setDirty();
                        setupScreen();
                    }
                }
            });
    }
    
    private int depositItemFromPlayer(Item item) {
        int totalDeposited = 0;
        
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int amount = stack.getCount();
                int added = state.getStockpile().addResource(item, amount);
                if (added > 0) {
                    player.getInventory().removeItem(slot, amount);
                    totalDeposited += amount;
                }
            }
        }
        
        return totalDeposited;
    }
}
