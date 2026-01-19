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
 * Technology tree screen - research new technologies.
 */
public class TechScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private TechTree.TechBranch currentBranch = TechTree.TechBranch.INDUSTRY;
    
    public TechScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player) {
        TechScreen screen = new TechScreen(player);
        screen.setTitle(Component.literal("§5§l🎓 University - Technology"));
        screen.open();
    }
    
    private void setupScreen() {
        // Fill background
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.PURPLE_STAINED_GLASS_PANE)
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
        
        // Title and resources
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.ENCHANTING_TABLE)
            .setName(Component.literal("§5§lUniversity"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§7Research technologies to"))
            .addLoreLine(Component.literal("§7unlock buildings and upgrades!"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Resources display
        this.setSlot(7, new GuiElementBuilder()
            .setItem(Items.GOLD_INGOT)
            .setName(Component.literal("§6Coins: §e" + state.getCoins()))
        );
        
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.NETHER_STAR)
            .setName(Component.literal("§bInfluence: §3" + state.getInfluence()))
        );
        
        // Branch tabs (row 1)
        setupBranchTabs();
        
        // Tech nodes (rows 2-5)
        setupTechNodes();
    }
    
    private void setupBranchTabs() {
        // Industry (slot 10)
        this.setSlot(10, new GuiElementBuilder()
            .setItem(currentBranch == TechTree.TechBranch.INDUSTRY ? Items.GOLDEN_PICKAXE : Items.IRON_PICKAXE)
            .setName(Component.literal("§6§lIndustry Branch"))
            .addLoreLine(Component.literal("§7Farming, Mining, Forestry, Ranching"))
            .addLoreLine(currentBranch == TechTree.TechBranch.INDUSTRY ? Component.literal("§a▶ Selected") : Component.literal("§eClick to view"))
            .setCallback((index, type, action) -> {
                currentBranch = TechTree.TechBranch.INDUSTRY;
                setupScreen();
            })
        );
        
        // Civics (slot 13)
        this.setSlot(13, new GuiElementBuilder()
            .setItem(currentBranch == TechTree.TechBranch.CIVICS ? Items.GOLDEN_SWORD : Items.IRON_SWORD)
            .setName(Component.literal("§b§lCivics Branch"))
            .addLoreLine(Component.literal("§7Housing, Commerce, Education, Defense"))
            .addLoreLine(currentBranch == TechTree.TechBranch.CIVICS ? Component.literal("§a▶ Selected") : Component.literal("§eClick to view"))
            .setCallback((index, type, action) -> {
                currentBranch = TechTree.TechBranch.CIVICS;
                setupScreen();
            })
        );
        
        // Logistics (slot 16)
        this.setSlot(16, new GuiElementBuilder()
            .setItem(currentBranch == TechTree.TechBranch.LOGISTICS ? Items.GOLDEN_SHOVEL : Items.IRON_SHOVEL)
            .setName(Component.literal("§a§lLogistics Branch"))
            .addLoreLine(Component.literal("§7Storage, Automation, Contracts"))
            .addLoreLine(currentBranch == TechTree.TechBranch.LOGISTICS ? Component.literal("§a▶ Selected") : Component.literal("§eClick to view"))
            .setCallback((index, type, action) -> {
                currentBranch = TechTree.TechBranch.LOGISTICS;
                setupScreen();
            })
        );
    }
    
    private void setupTechNodes() {
        int slot = 19;  // Start at row 2
        
        for (TechTree.TechNode tech : TechTree.TechNode.values()) {
            if (tech.getBranch() != currentBranch) continue;
            if (slot > 43) break;  // Don't overflow
            
            // Skip edge slots
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }
            
            this.setSlot(slot, createTechElement(tech));
            slot++;
        }
    }
    
    private GuiElementBuilder createTechElement(TechTree.TechNode tech) {
        TechTree techTree = state.getTechTree();
        boolean isUnlocked = techTree.isUnlocked(tech);
        boolean canUnlock = techTree.canUnlock(tech, state.getCoins(), state.getInfluence());
        
        // Determine icon
        net.minecraft.world.item.Item icon;
        String statusColor;
        if (isUnlocked) {
            icon = Items.ENCHANTED_BOOK;
            statusColor = "§a";
        } else if (canUnlock) {
            icon = Items.BOOK;
            statusColor = "§e";
        } else {
            icon = Items.BARRIER;
            statusColor = "§c";
        }
        
        GuiElementBuilder builder = new GuiElementBuilder()
            .setItem(icon)
            .setName(Component.literal(tech.getBranch().getColorCode() + tech.getDisplayName()))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
        
        // Description
        builder.addLoreLine(Component.literal("§7" + tech.getDescription()));
        builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
        
        if (isUnlocked) {
            builder.addLoreLine(Component.literal("§a✓ RESEARCHED"));
        } else {
            builder.addLoreLine(Component.literal("§7Cost:"));
            builder.addLoreLine(Component.literal("  §6" + tech.getCoinCost() + " coins"));
            if (tech.getInfluenceCost() > 0) {
                builder.addLoreLine(Component.literal("  §b" + tech.getInfluenceCost() + " influence"));
            }
            
            // Prerequisites
            if (tech.getPrerequisites().length > 0) {
                builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
                builder.addLoreLine(Component.literal("§7Requires:"));
                for (TechTree.TechNode prereq : tech.getPrerequisites()) {
                    boolean hasPrereq = techTree.isUnlocked(prereq);
                    builder.addLoreLine(Component.literal("  " + (hasPrereq ? "§a✓ " : "§c✗ ") + prereq.getDisplayName()));
                }
            }
            
            builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
            builder.addLoreLine(canUnlock ? Component.literal("§aClick to research!") : Component.literal("§cRequirements not met"));
        }
        
        builder.setCallback((index, type, action) -> {
            if (!isUnlocked && canUnlock) {
                if (state.unlockTech(tech)) {
                    GuiHelper.playSound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                    player.sendSystemMessage(Component.literal("§5§l✓ Researched: " + tech.getDisplayName() + "!"));
                    TechScreen.open(player);  // Refresh
                }
            } else if (!isUnlocked) {
                GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
            }
        });
        
        return builder;
    }
}
