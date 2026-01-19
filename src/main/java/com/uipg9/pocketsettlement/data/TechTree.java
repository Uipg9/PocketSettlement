package com.uipg9.pocketsettlement.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.*;

/**
 * Technology tree for unlocking new buildings, features, and upgrades.
 * Organized into three branches: Industry, Civics, and Logistics.
 */
public class TechTree {
    private Set<String> unlockedTechs;
    
    public TechTree() {
        this.unlockedTechs = new HashSet<>();
        // Start with basic techs
        unlockedTechs.add(TechNode.SETTLEMENT_BASICS.getId());
    }
    
    // === Tech Management ===
    
    public boolean isUnlocked(TechNode tech) {
        return unlockedTechs.contains(tech.getId());
    }
    
    public boolean canUnlock(TechNode tech, int coins, int influence) {
        // Check if already unlocked
        if (isUnlocked(tech)) return false;
        
        // Check prerequisites
        for (TechNode prereq : tech.getPrerequisites()) {
            if (!isUnlocked(prereq)) return false;
        }
        
        // Check cost
        return coins >= tech.getCoinCost() && influence >= tech.getInfluenceCost();
    }
    
    public boolean unlock(TechNode tech) {
        if (unlockedTechs.contains(tech.getId())) return false;
        unlockedTechs.add(tech.getId());
        return true;
    }
    
    public Set<TechNode> getAvailableTechs(int coins, int influence) {
        Set<TechNode> available = new HashSet<>();
        for (TechNode tech : TechNode.values()) {
            if (canUnlock(tech, coins, influence)) {
                available.add(tech);
            }
        }
        return available;
    }
    
    public int getUnlockedCount() {
        return unlockedTechs.size();
    }
    
    // === NBT Serialization ===
    
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag techList = new ListTag();
        for (String techId : unlockedTechs) {
            CompoundTag techNBT = new CompoundTag();
            techNBT.putString("id", techId);
            techList.add(techNBT);
        }
        nbt.put("techs", techList);
        return nbt;
    }
    
    public static TechTree fromNBT(CompoundTag nbt) {
        TechTree tree = new TechTree();
        tree.unlockedTechs.clear();
        
        ListTag techList = nbt.getListOrEmpty("techs");
        for (int i = 0; i < techList.size(); i++) {
            CompoundTag techNBT = techList.getCompoundOrEmpty(i);
            String techId = techNBT.getStringOr("id", "");
            if (!techId.isEmpty()) {
                tree.unlockedTechs.add(techId);
            }
        }
        
        // Ensure basics are always unlocked
        tree.unlockedTechs.add(TechNode.SETTLEMENT_BASICS.getId());
        
        return tree;
    }
    
    // === Tech Nodes ===
    
    public enum TechNode {
        // === Starting Tech ===
        SETTLEMENT_BASICS("settlement_basics", "Settlement Basics", TechBranch.NONE,
            "The foundation of your colony.", 0, 0),
        
        // === INDUSTRY BRANCH ===
        FARMING_I("farming_1", "Basic Farming", TechBranch.INDUSTRY,
            "Unlock the Greenhouse. Grow wheat and carrots.", 100, 0,
            SETTLEMENT_BASICS),
        
        FARMING_II("farming_2", "Advanced Crops", TechBranch.INDUSTRY,
            "Grow potatoes, beetroot, and melons.", 250, 10,
            FARMING_I),
        
        FARMING_III("farming_3", "Master Agriculture", TechBranch.INDUSTRY,
            "Grow pumpkins and nether wart. +50% farm speed.", 500, 25,
            FARMING_II),
        
        MINING_I("mining_1", "Basic Mining", TechBranch.INDUSTRY,
            "Unlock the Quarry. Extract stone and coal.", 150, 0,
            SETTLEMENT_BASICS),
        
        MINING_II("mining_2", "Deep Mining", TechBranch.INDUSTRY,
            "Access deeper layers. Extract iron and gold.", 350, 15,
            MINING_I),
        
        MINING_III("mining_3", "Gem Extraction", TechBranch.INDUSTRY,
            "Extract diamonds and emeralds. +50% mining speed.", 600, 30,
            MINING_II),
        
        FORESTRY_I("forestry_1", "Forestry", TechBranch.INDUSTRY,
            "Unlock the Lumber Yard. Harvest oak wood.", 120, 0,
            SETTLEMENT_BASICS),
        
        FORESTRY_II("forestry_2", "Advanced Forestry", TechBranch.INDUSTRY,
            "Harvest all wood types. +25% lumber speed.", 300, 10,
            FORESTRY_I),
        
        RANCHING_I("ranching_1", "Animal Husbandry", TechBranch.INDUSTRY,
            "Unlock the Mob Barn. Raise cows and pigs.", 200, 5,
            FARMING_I),
        
        RANCHING_II("ranching_2", "Advanced Ranching", TechBranch.INDUSTRY,
            "Raise sheep and chickens. +25% barn output.", 400, 20,
            RANCHING_I),
        
        // === CIVICS BRANCH ===
        HOUSING_I("housing_1", "Basic Housing", TechBranch.CIVICS,
            "Unlock Houses. Increase population cap.", 100, 5,
            SETTLEMENT_BASICS),
        
        HOUSING_II("housing_2", "Comfortable Homes", TechBranch.CIVICS,
            "+20% citizen happiness. Houses hold 2 citizens.", 250, 15,
            HOUSING_I),
        
        COMMERCE_I("commerce_1", "Marketplace", TechBranch.CIVICS,
            "Unlock the Market. Generate passive income.", 200, 10,
            HOUSING_I),
        
        COMMERCE_II("commerce_2", "Banking", TechBranch.CIVICS,
            "Unlock the Bank. +10% daily interest on savings.", 500, 25,
            COMMERCE_I),
        
        EDUCATION_I("education_1", "Academy", TechBranch.CIVICS,
            "Unlock the Academy. Train citizens to level up.", 300, 15,
            HOUSING_I),
        
        EDUCATION_II("education_2", "Advanced Studies", TechBranch.CIVICS,
            "Citizens can reach Level 5. -25% training cost.", 600, 35,
            EDUCATION_I),
        
        DEFENSE_I("defense_1", "Guard Tower", TechBranch.CIVICS,
            "Unlock Guard Towers. Protect against raids.", 350, 20,
            HOUSING_I),
        
        DEFENSE_II("defense_2", "Militia", TechBranch.CIVICS,
            "Guards are more effective. -50% raid damage.", 700, 40,
            DEFENSE_I),
        
        // === LOGISTICS BRANCH ===
        STORAGE_I("storage_1", "Expanded Storage", TechBranch.LOGISTICS,
            "+500 stockpile capacity.", 150, 5,
            SETTLEMENT_BASICS),
        
        STORAGE_II("storage_2", "Warehouses", TechBranch.LOGISTICS,
            "+1000 stockpile capacity. Organize by category.", 350, 15,
            STORAGE_I),
        
        STORAGE_III("storage_3", "Mass Storage", TechBranch.LOGISTICS,
            "+2500 stockpile capacity.", 600, 30,
            STORAGE_II),
        
        AUTOMATION_I("automation_1", "Auto-Collection", TechBranch.LOGISTICS,
            "Production buildings auto-deposit to stockpile.", 250, 10,
            STORAGE_I),
        
        AUTOMATION_II("automation_2", "Auto-Feed", TechBranch.LOGISTICS,
            "Mob Barn auto-uses bait from stockpile.", 450, 20,
            AUTOMATION_I),
        
        CONTRACTS_I("contracts_1", "Trade Routes", TechBranch.LOGISTICS,
            "4 daily contracts instead of 3.", 200, 10,
            COMMERCE_I),
        
        CONTRACTS_II("contracts_2", "Merchant Guild", TechBranch.LOGISTICS,
            "5 daily contracts. Better rewards.", 500, 25,
            CONTRACTS_I);
        
        private final String id;
        private final String displayName;
        private final TechBranch branch;
        private final String description;
        private final int coinCost;
        private final int influenceCost;
        private final TechNode[] prerequisites;
        
        TechNode(String id, String displayName, TechBranch branch, String description,
                 int coinCost, int influenceCost, TechNode... prerequisites) {
            this.id = id;
            this.displayName = displayName;
            this.branch = branch;
            this.description = description;
            this.coinCost = coinCost;
            this.influenceCost = influenceCost;
            this.prerequisites = prerequisites;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public TechBranch getBranch() { return branch; }
        public String getDescription() { return description; }
        public int getCoinCost() { return coinCost; }
        public int getInfluenceCost() { return influenceCost; }
        public TechNode[] getPrerequisites() { return prerequisites; }
    }
    
    public enum TechBranch {
        NONE("None", "§7"),
        INDUSTRY("Industry", "§6"),      // Orange/Gold
        CIVICS("Civics", "§b"),          // Aqua
        LOGISTICS("Logistics", "§a");    // Green
        
        private final String displayName;
        private final String colorCode;
        
        TechBranch(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColorCode() { return colorCode; }
    }
}
