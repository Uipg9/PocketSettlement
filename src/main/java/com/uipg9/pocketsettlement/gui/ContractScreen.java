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

import java.util.List;

/**
 * Daily contracts screen - fulfill orders to earn coins.
 */
public class ContractScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    
    public ContractScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player) {
        ContractScreen screen = new ContractScreen(player);
        screen.setTitle(Component.literal("§d§l📜 Daily Contracts"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.PURPLE_STAINED_GLASS_PANE)
            .setName(Component.literal(""));
        
        for (int i = 0; i < 27; i++) {
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
        
        // Contract info
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.WRITABLE_BOOK)
            .setName(Component.literal("§d§lMerchant Contracts"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Fulfill orders to earn coins!"))
            .addLoreLine(Component.literal("§7Contracts refresh each morning."))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Display contracts
        List<Contract> contracts = state.getActiveContracts();
        
        if (contracts.isEmpty()) {
            this.setSlot(13, new GuiElementBuilder()
                .setItem(Items.CLOCK)
                .setName(Component.literal("§eNo Active Contracts"))
                .addLoreLine(Component.literal("§7Wait until morning for"))
                .addLoreLine(Component.literal("§7new contracts to appear!"))
            );
        } else {
            int slot = 10;
            for (int i = 0; i < contracts.size() && slot <= 16; i++) {
                Contract contract = contracts.get(i);
                this.setSlot(slot, createContractElement(contract));
                slot++;
            }
        }
        
        // Coins display
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.GOLD_INGOT)
            .setName(Component.literal("§6Coins: §e" + state.getCoins()))
        );
    }
    
    private GuiElementBuilder createContractElement(Contract contract) {
        Item requiredItem = contract.getRequiredItem();
        boolean isCompleted = contract.isCompleted();
        int stockpileAmount = state.getStockpile().getResourceCount(requiredItem);
        int needed = contract.getRemainingAmount();
        boolean canFulfill = stockpileAmount >= needed && !isCompleted;
        
        String progressBar = createProgressBar(contract.getProgress());
        
        GuiElementBuilder builder = new GuiElementBuilder()
            .setItem(isCompleted ? Items.EMERALD : requiredItem)
            .setName(Component.literal((isCompleted ? "§a✓ " : "§f") + requiredItem.getName().getString() + " Contract"));
        
        builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
        
        if (isCompleted) {
            builder.addLoreLine(Component.literal("§a§lCOMPLETED!"));
            builder.addLoreLine(Component.literal("§7Reward collected: §e" + contract.getReward() + " coins"));
        } else {
            builder.addLoreLine(Component.literal("§7Request: §f" + contract.getRequiredAmount() + "x " + requiredItem.getName().getString()));
            builder.addLoreLine(Component.literal("§7Delivered: §f" + contract.getDelivered() + "/" + contract.getRequiredAmount()));
            builder.addLoreLine(Component.literal("§7" + progressBar));
            builder.addLoreLine(Component.literal("§7Reward: §e" + contract.getReward() + " coins"));
            builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
            builder.addLoreLine(Component.literal("§7In Stockpile: §f" + stockpileAmount));
            
            if (canFulfill) {
                builder.addLoreLine(Component.literal("§a✓ Click to fulfill!"));
            } else {
                builder.addLoreLine(Component.literal("§cNeed " + needed + " more in stockpile"));
            }
        }
        
        builder.setCallback((index, type, action) -> {
            if (isCompleted) return;
            
            // Try to fulfill from stockpile
            int available = state.getStockpile().getResourceCount(requiredItem);
            int toDeliver = Math.min(available, contract.getRemainingAmount());
            
            if (toDeliver > 0 && state.getStockpile().removeResource(requiredItem, toDeliver)) {
                int accepted = contract.deliver(toDeliver);
                
                if (contract.isCompleted()) {
                    // Award coins
                    state.addCoins(contract.getReward());
                    GuiHelper.playSound(player, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.sendSystemMessage(Component.literal("§a§l✓ CONTRACT COMPLETE! §e+" + contract.getReward() + " coins"));
                } else {
                    GuiHelper.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.sendSystemMessage(Component.literal("§a✓ Delivered " + accepted + "x " + requiredItem.getName().getString()));
                }
                
                state.setDirty();
                ContractScreen.open(player);  // Refresh
            } else {
                GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
                player.sendSystemMessage(Component.literal("§cNot enough items in stockpile!"));
            }
        });
        
        return builder;
    }
    
    private String createProgressBar(float progress) {
        int filled = (int) (progress * 10);
        StringBuilder bar = new StringBuilder("§d");
        for (int i = 0; i < filled; i++) bar.append("█");
        bar.append("§8");
        for (int i = filled; i < 10; i++) bar.append("█");
        bar.append(" §f" + (int)(progress * 100) + "%");
        return bar.toString();
    }
}
