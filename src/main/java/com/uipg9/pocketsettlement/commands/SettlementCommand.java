package com.uipg9.pocketsettlement.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.uipg9.pocketsettlement.data.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * Admin/debug commands for the settlement.
 */
public class SettlementCommand {
    
    /**
     * Helper method to check if a command source has operator permissions (level 2+).
     */
    private static boolean hasOpPermission(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("settlement")
            .then(Commands.literal("info")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    showInfo(player);
                    return 1;
                })
            )
            .then(Commands.literal("give")
                .requires(SettlementCommand::hasOpPermission)
                .then(Commands.literal("coins")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            giveCoins(player, amount);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("influence")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            giveInfluence(player, amount);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("citizens")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    showCitizens(player);
                    return 1;
                })
            )
            .then(Commands.literal("buildings")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    showBuildings(player);
                    return 1;
                })
            )
            .then(Commands.literal("tech")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    showTech(player);
                    return 1;
                })
            )
            .then(Commands.literal("stockpile")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    showStockpile(player);
                    return 1;
                })
            )
            .then(Commands.literal("reset")
                .requires(SettlementCommand::hasOpPermission)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    resetSettlement(player);
                    return 1;
                })
            )
            .then(Commands.literal("unlock")
                .requires(SettlementCommand::hasOpPermission)
                .then(Commands.argument("tech", StringArgumentType.word())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String techName = StringArgumentType.getString(ctx, "tech");
                        unlockTech(player, techName);
                        return 1;
                    })
                )
            )
        );
    }
    
    private static void showInfo(ServerPlayer player) {
        SettlementState state = SettlementState.getOrCreate(player.level());
        
        player.sendSystemMessage(Component.literal("§6§l━━━━ Settlement Overview ━━━━"));
        player.sendSystemMessage(Component.literal("§7Day: §f" + state.getDaysPlayed()));
        player.sendSystemMessage(Component.literal("§6Coins: §e" + state.getCoins()));
        player.sendSystemMessage(Component.literal("§bInfluence: §3" + state.getInfluence()));
        player.sendSystemMessage(Component.literal("§7Population: §f" + state.getCitizenCount() + "/" + state.getMaxCitizens()));
        player.sendSystemMessage(Component.literal("§7Buildings: §f" + state.getBuildingCount(null)));
        player.sendSystemMessage(Component.literal("§7Happiness: §f" + state.getAverageHappiness() + "%"));
        player.sendSystemMessage(Component.literal("§7Stockpile: §f" + state.getStockpile().getTotalItems() + " items"));
        player.sendSystemMessage(Component.literal("§6§l━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private static void giveCoins(ServerPlayer player, int amount) {
        SettlementState state = SettlementState.getOrCreate(player.level());
        state.addCoins(amount);
        player.sendSystemMessage(Component.literal("§a✓ Added §e" + amount + " coins§a. Total: §e" + state.getCoins()));
    }
    
    private static void giveInfluence(ServerPlayer player, int amount) {
        SettlementState state = SettlementState.getOrCreate(player.level());
        state.addInfluence(amount);
        player.sendSystemMessage(Component.literal("§a✓ Added §b" + amount + " influence§a. Total: §b" + state.getInfluence()));
    }
    
    private static void showCitizens(ServerPlayer player) {
        SettlementState state = SettlementState.getOrCreate(player.level());
        
        player.sendSystemMessage(Component.literal("§b§l━━━━ Citizens (" + state.getCitizenCount() + ") ━━━━"));
        
        for (Citizen citizen : state.getAllCitizens()) {
            player.sendSystemMessage(Component.literal(
                "§f" + citizen.getName() + " §7| " + 
                citizen.getJob().getDisplayName() + " §7| Lvl " + 
                citizen.getLevel() + " §7| HP " + citizen.getHappiness() + "%"
            ));
        }
        
        if (state.getCitizenCount() == 0) {
            player.sendSystemMessage(Component.literal("§7No citizens yet. Recruit some!"));
        }
        player.sendSystemMessage(Component.literal("§b§l━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private static void showBuildings(ServerPlayer player) {
        SettlementState state = SettlementState.getOrCreate(player.level());
        
        player.sendSystemMessage(Component.literal("§6§l━━━━ Buildings ━━━━"));
        
        for (int y = 0; y < 7; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < 7; x++) {
                Building building = state.getBuilding(x, y);
                if (building == null || building.getType() == BuildingType.EMPTY) {
                    row.append("§8□ ");
                } else {
                    // Use first char of display name as icon
                    row.append("§a").append(building.getType().getDisplayName().charAt(0)).append(" ");
                }
            }
            player.sendSystemMessage(Component.literal(row.toString()));
        }
        
        player.sendSystemMessage(Component.literal("§6§l━━━━━━━━━━━━━━━━━━"));
    }
    
    private static void showTech(ServerPlayer player) {
        SettlementState state = SettlementState.getOrCreate(player.level());
        TechTree tree = state.getTechTree();
        
        player.sendSystemMessage(Component.literal("§5§l━━━━ Technology ━━━━"));
        
        int unlocked = 0;
        for (TechTree.TechNode node : TechTree.TechNode.values()) {
            if (tree.isUnlocked(node)) {
                unlocked++;
            }
        }
        
        player.sendSystemMessage(Component.literal("§7Unlocked: §f" + unlocked + "/" + TechTree.TechNode.values().length));
        player.sendSystemMessage(Component.literal(""));
        
        for (TechTree.TechBranch branch : TechTree.TechBranch.values()) {
            player.sendSystemMessage(Component.literal(branch.getColorCode() + "§l" + branch.getDisplayName() + ":"));
            for (TechTree.TechNode node : TechTree.TechNode.values()) {
                if (node.getBranch() == branch) {
                    boolean isUnlocked = tree.isUnlocked(node);
                    player.sendSystemMessage(Component.literal(
                        "  " + (isUnlocked ? "§a✓ " : "§8✗ ") + node.getDisplayName()
                    ));
                }
            }
        }
        
        player.sendSystemMessage(Component.literal("§5§l━━━━━━━━━━━━━━━━━━"));
    }
    
    private static void showStockpile(ServerPlayer player) {
        SettlementState state = SettlementState.getOrCreate(player.level());
        Stockpile stockpile = state.getStockpile();
        
        player.sendSystemMessage(Component.literal("§e§l━━━━ Stockpile ━━━━"));
        player.sendSystemMessage(Component.literal("§7Capacity: §f" + stockpile.getTotalItems() + "/" + stockpile.getMaxCapacity()));
        player.sendSystemMessage(Component.literal(""));
        
        var resources = stockpile.getAllResources();
        if (resources.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Stockpile is empty."));
        } else {
            for (var entry : resources.entrySet()) {
                player.sendSystemMessage(Component.literal(
                    "§7- §f" + entry.getValue() + "x " + entry.getKey().getName().getString()
                ));
            }
        }
        
        player.sendSystemMessage(Component.literal("§e§l━━━━━━━━━━━━━━━━━━"));
    }
    
    private static void resetSettlement(ServerPlayer player) {
        // Create a fresh settlement state
        SettlementState state = SettlementState.getOrCreate(player.level());
        // Clear all data by reinitializing
        state.clearAll();
        state.setDirty();
        player.sendSystemMessage(Component.literal("§c⚠ Settlement has been reset!"));
    }
    
    private static void unlockTech(ServerPlayer player, String techName) {
        SettlementState state = SettlementState.getOrCreate(player.level());
        
        try {
            TechTree.TechNode node = TechTree.TechNode.valueOf(techName.toUpperCase());
            state.getTechTree().unlock(node);
            state.setDirty();
            player.sendSystemMessage(Component.literal("§a✓ Unlocked: " + node.getDisplayName()));
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("§cUnknown tech: " + techName));
            player.sendSystemMessage(Component.literal("§7Available: FARMING_I, MINING_I, FORESTRY_I, etc."));
        }
    }
}
