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

/**
 * The 7x7 Settlement Grid view.
 * Players can view/build/upgrade buildings here.
 */
public class GridScreen extends SimpleGui {
    
    private final ServerPlayer player;
    private final SettlementState state;
    private int selectedX = -1;
    private int selectedZ = -1;
    
    public GridScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.player = player;
        this.state = SettlementState.getOrCreate(player.level());
        
        setupScreen();
    }
    
    public static void open(ServerPlayer player) {
        GridScreen screen = new GridScreen(player);
        screen.setTitle(Component.literal("§2§l⬛ Settlement Grid"));
        screen.open();
    }
    
    private void setupScreen() {
        fillBackground();
        setupHeader();
        setupGrid();
        setupFooter();
    }
    
    private void fillBackground() {
        GuiElementBuilder bg = new GuiElementBuilder()
            .setItem(Items.LIME_STAINED_GLASS_PANE)
            .setName(Component.literal(""));
        
        for (int i = 0; i < 54; i++) {
            this.setSlot(i, bg);
        }
    }
    
    private void setupHeader() {
        // Back button
        this.setSlot(0, new GuiElementBuilder()
            .setItem(Items.ARROW)
            .setName(Component.literal("§e← Back to Desk"))
            .setCallback((index, type, action) -> {
                DeskScreen.open(player);
            })
        );
        
        // Title
        this.setSlot(4, new GuiElementBuilder()
            .setItem(Items.MAP)
            .setName(Component.literal("§a§lSettlement Grid"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
            .addLoreLine(Component.literal("§fLeft-click: View building"))
            .addLoreLine(Component.literal("§fRight-click: Build/Upgrade"))
            .addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"))
        );
        
        // Coins display
        this.setSlot(8, new GuiElementBuilder()
            .setItem(Items.GOLD_INGOT)
            .setName(Component.literal("§6Coins: §e" + state.getCoins()))
        );
    }
    
    private void setupGrid() {
        // The 7x7 grid fits in rows 1-5 with some offset
        // Slots: 9-17, 18-26, 27-35, 36-44, 45-53
        // We'll use slots 10-16 for each row (7 columns)
        
        for (int gridZ = 0; gridZ < 7; gridZ++) {
            for (int gridX = 0; gridX < 7; gridX++) {
                int slot = (gridZ + 1) * 9 + (gridX + 1);  // Offset by 1 for border
                
                Building building = state.getBuilding(gridX, gridZ);
                GuiElementBuilder element = createBuildingElement(gridX, gridZ, building);
                
                final int x = gridX;
                final int z = gridZ;
                
                element.setCallback((index, clickType, action) -> {
                    handleGridClick(x, z, clickType);
                });
                
                this.setSlot(slot, element);
            }
        }
    }
    
    private GuiElementBuilder createBuildingElement(int x, int z, Building building) {
        BuildingType type = building.getType();
        Item icon = type.getIconItem();
        
        GuiElementBuilder builder = new GuiElementBuilder()
            .setItem(icon)
            .setName(Component.literal(getColorForBuilding(type) + type.getDisplayName()));
        
        if (type != BuildingType.EMPTY) {
            builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
            builder.addLoreLine(Component.literal("§7Level: §f" + building.getLevel()));
            
            if (type.isProducer()) {
                builder.addLoreLine(Component.literal("§7Progress: §f" + building.getProgress() + "%"));
                builder.addLoreLine(Component.literal("§7Efficiency: §f" + building.getEfficiency(state) + "%"));
            }
            
            if (building.hasWorker()) {
                Citizen worker = state.getCitizen(building.getAssignedCitizenId());
                if (worker != null) {
                    builder.addLoreLine(Component.literal("§7Worker: §f" + worker.getName()));
                }
            } else if (type.isProducer()) {
                builder.addLoreLine(Component.literal("§c⚠ No worker assigned!"));
            }
            
            builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
            builder.addLoreLine(Component.literal("§eLeft-click: Open"));
            builder.addLoreLine(Component.literal("§eRight-click: Manage"));
        } else {
            builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
            builder.addLoreLine(Component.literal("§7An empty plot ready"));
            builder.addLoreLine(Component.literal("§7for construction."));
            builder.addLoreLine(Component.literal("§7━━━━━━━━━━━━━━━━━"));
            builder.addLoreLine(Component.literal("§aRight-click to build!"));
        }
        
        builder.addLoreLine(Component.literal("§8[" + x + ", " + z + "]"));
        
        return builder;
    }
    
    private String getColorForBuilding(BuildingType type) {
        return switch (type) {
            case EMPTY -> "§7";
            case HOUSE -> "§c";
            case GREENHOUSE -> "§a";
            case QUARRY -> "§8";
            case LUMBER_YARD -> "§6";
            case MOB_BARN -> "§e";
            case MARKET -> "§b";
            case BANK -> "§6";
            case ACADEMY -> "§d";
            case GUARD_TOWER -> "§c";
            case TOWN_HALL -> "§5";
        };
    }
    
    private void handleGridClick(int x, int z, eu.pb4.sgui.api.ClickType clickType) {
        Building building = state.getBuilding(x, z);
        
        if (clickType.isLeft) {
            // Left click - open building details or production screen
            if (!building.isEmpty() && building.getType() != BuildingType.TOWN_HALL) {
                openBuildingScreen(x, z, building);
            }
        } else if (clickType.isRight) {
            // Right click - build or manage
            if (building.isEmpty()) {
                // Open build menu
                BuildMenuScreen.open(player, x, z);
            } else if (building.getType() != BuildingType.TOWN_HALL) {
                // Open manage menu (upgrade/demolish)
                ManageBuildingScreen.open(player, x, z);
            }
        }
    }
    
    private void openBuildingScreen(int x, int z, Building building) {
        switch (building.getType()) {
            case GREENHOUSE -> GreenhouseScreen.open(player, x, z);
            case QUARRY -> QuarryScreen.open(player, x, z);
            case LUMBER_YARD -> LumberYardScreen.open(player, x, z);
            case MOB_BARN -> MobBarnScreen.open(player, x, z);
            default -> {
                GuiHelper.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
                player.sendSystemMessage(Component.literal("§7This building has no special interface."));
            }
        }
    }
    
    private void setupFooter() {
        // Legend in bottom corners
        this.setSlot(45, new GuiElementBuilder()
            .setItem(Items.OAK_SIGN)
            .setName(Component.literal("§eLegend"))
            .addLoreLine(Component.literal("§a■ §fFarm §8(Production)"))
            .addLoreLine(Component.literal("§8■ §fQuarry §8(Production)"))
            .addLoreLine(Component.literal("§6■ §fLumber Yard §8(Production)"))
            .addLoreLine(Component.literal("§e■ §fMob Barn §8(Production)"))
            .addLoreLine(Component.literal("§c■ §fHouse §8(Housing)"))
            .addLoreLine(Component.literal("§b■ §fMarket §8(Economy)"))
        );
    }
}
