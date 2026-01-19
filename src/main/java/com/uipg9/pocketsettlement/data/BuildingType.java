package com.uipg9.pocketsettlement.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Enum representing all building types available in the settlement.
 * Each building has a display name, icon item, and production info.
 */
public enum BuildingType {
    // Empty plot
    EMPTY("Empty Plot", Items.GRASS_BLOCK, false, CitizenJob.NONE),
    
    // Housing
    HOUSE("House", Items.RED_BED, false, CitizenJob.NONE),
    
    // Production Buildings
    GREENHOUSE("Greenhouse", Items.HAY_BLOCK, true, CitizenJob.FARMER),
    QUARRY("Quarry", Items.IRON_PICKAXE, true, CitizenJob.MINER),
    LUMBER_YARD("Lumber Yard", Items.IRON_AXE, true, CitizenJob.LUMBERJACK),
    MOB_BARN("Mob Barn", Items.WHEAT, true, CitizenJob.RANCHER),
    
    // Economy Buildings
    MARKET("Market", Items.EMERALD, false, CitizenJob.MERCHANT),
    BANK("Bank", Items.GOLD_INGOT, false, CitizenJob.NONE),
    
    // Civic Buildings
    ACADEMY("Academy", Items.BOOK, false, CitizenJob.SCHOLAR),
    GUARD_TOWER("Guard Tower", Items.BOW, false, CitizenJob.GUARD),
    
    // Special Buildings
    TOWN_HALL("Town Hall", Items.BELL, false, CitizenJob.NONE);
    
    private final String displayName;
    private final Item iconItem;
    private final boolean isProducer;
    private final CitizenJob preferredJob;
    
    BuildingType(String displayName, Item iconItem, boolean isProducer, CitizenJob preferredJob) {
        this.displayName = displayName;
        this.iconItem = iconItem;
        this.isProducer = isProducer;
        this.preferredJob = preferredJob;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Item getIconItem() {
        return iconItem;
    }
    
    public boolean isProducer() {
        return isProducer;
    }
    
    public CitizenJob getPreferredJob() {
        return preferredJob;
    }
    
    /**
     * Get the base construction cost in coins.
     */
    public int getBaseCost() {
        return switch (this) {
            case EMPTY -> 0;
            case HOUSE -> 100;
            case GREENHOUSE -> 250;
            case QUARRY -> 300;
            case LUMBER_YARD -> 200;
            case MOB_BARN -> 350;
            case MARKET -> 500;
            case BANK -> 1000;
            case ACADEMY -> 750;
            case GUARD_TOWER -> 400;
            case TOWN_HALL -> 2000;
        };
    }
    
    /**
     * Get upgrade cost for a specific level.
     */
    public int getUpgradeCost(int toLevel) {
        return (int) (getBaseCost() * 0.5 * toLevel);
    }
    
    /**
     * Get base production time in ticks (20 ticks = 1 second).
     */
    public int getBaseProductionTime() {
        return switch (this) {
            case GREENHOUSE -> 200;    // 10 seconds
            case QUARRY -> 300;        // 15 seconds
            case LUMBER_YARD -> 250;   // 12.5 seconds
            case MOB_BARN -> 400;      // 20 seconds
            default -> 0;
        };
    }
    
    /**
     * Get adjacency synergy bonus type.
     */
    public AdjacencyBonus getAdjacencyBonus() {
        return switch (this) {
            case HOUSE -> AdjacencyBonus.HOUSING;      // Near markets = tax bonus
            case MARKET -> AdjacencyBonus.COMMERCE;    // Near houses = more coins
            case QUARRY -> AdjacencyBonus.POLLUTION;   // Near houses = happiness penalty
            case GREENHOUSE -> AdjacencyBonus.NATURE;  // Near houses = happiness bonus
            case GUARD_TOWER -> AdjacencyBonus.SECURITY; // Near anything = protection bonus
            default -> AdjacencyBonus.NONE;
        };
    }
    
    public enum AdjacencyBonus {
        NONE,
        HOUSING,
        COMMERCE,
        POLLUTION,
        NATURE,
        SECURITY
    }
}
