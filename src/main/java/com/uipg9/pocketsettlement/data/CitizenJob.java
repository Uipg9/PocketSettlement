package com.uipg9.pocketsettlement.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Enum representing all citizen job types.
 */
public enum CitizenJob {
    NONE("Unemployed", Items.VILLAGER_SPAWN_EGG),
    FARMER("Farmer", Items.GOLDEN_HOE),
    MINER("Miner", Items.IRON_PICKAXE),
    LUMBERJACK("Lumberjack", Items.IRON_AXE),
    RANCHER("Rancher", Items.LEAD),
    MERCHANT("Merchant", Items.EMERALD),
    SCHOLAR("Scholar", Items.BOOK),
    GUARD("Guard", Items.IRON_SWORD);
    
    private final String displayName;
    private final Item iconItem;
    
    CitizenJob(String displayName, Item iconItem) {
        this.displayName = displayName;
        this.iconItem = iconItem;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Item getIconItem() {
        return iconItem;
    }
    
    /**
     * Get the building type this job works at.
     */
    public BuildingType getWorkplace() {
        return switch (this) {
            case FARMER -> BuildingType.GREENHOUSE;
            case MINER -> BuildingType.QUARRY;
            case LUMBERJACK -> BuildingType.LUMBER_YARD;
            case RANCHER -> BuildingType.MOB_BARN;
            case MERCHANT -> BuildingType.MARKET;
            case SCHOLAR -> BuildingType.ACADEMY;
            case GUARD -> BuildingType.GUARD_TOWER;
            default -> null;
        };
    }
}
