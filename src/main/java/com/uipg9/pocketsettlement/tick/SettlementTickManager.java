package com.uipg9.pocketsettlement.tick;

import com.uipg9.pocketsettlement.PocketSettlement;
import com.uipg9.pocketsettlement.data.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Random;

/**
 * Manages the settlement simulation tick.
 * Called every world tick, but only processes logic every 20 ticks (1 second).
 */
public class SettlementTickManager {
    
    private static int tickCounter = 0;
    private static final int PROCESS_INTERVAL = 20;  // Process every 1 second
    private static final Random random = new Random();
    
    /**
     * Called every world tick.
     */
    public static void tick(ServerLevel level) {
        tickCounter++;
        
        if (tickCounter >= PROCESS_INTERVAL) {
            tickCounter = 0;
            processSettlement(level);
        }
    }
    
    /**
     * Main settlement processing (runs once per second).
     */
    private static void processSettlement(ServerLevel level) {
        SettlementState state = SettlementState.getOrCreate(level);
        
        // Process production buildings
        processProduction(state, level);
        
        // Check for daily reset (new contracts)
        checkDailyReset(state, level);
        
        // Process happiness changes
        processHappiness(state);
        
        // Mark state as dirty to save
        state.setDirty();
    }
    
    /**
     * Process all production buildings (Greenhouse, Quarry, Lumber Yard, Mob Barn).
     */
    private static void processProduction(SettlementState state, ServerLevel level) {
        for (int x = 0; x < SettlementState.GRID_SIZE; x++) {
            for (int z = 0; z < SettlementState.GRID_SIZE; z++) {
                Building building = state.getBuilding(x, z);
                
                if (building == null || !building.getType().isProducer()) {
                    continue;
                }
                
                // Only produce if building has a worker
                if (!building.hasWorker()) {
                    continue;
                }
                
                // Calculate progress to add based on efficiency
                int efficiency = building.getEfficiency(state);
                int baseTime = building.getType().getBaseProductionTime();
                
                // Progress per second = 100 / (baseTime / 20) * (efficiency / 100)
                // Simplified: Progress = 2000 * efficiency / baseTime
                float progressPerSecond = (2000f * efficiency) / (baseTime * 100f);
                int progressToAdd = Math.max(1, (int) progressPerSecond);
                
                building.addProgress(progressToAdd);
                
                // Check if production is complete
                if (building.isProductionReady()) {
                    produceOutput(state, building);
                    building.resetProgress();
                    
                    // Award XP to worker
                    Citizen worker = state.getCitizen(building.getAssignedCitizenId());
                    if (worker != null) {
                        worker.addXp(5);  // 5 XP per production cycle
                    }
                }
            }
        }
    }
    
    /**
     * Produce output based on building type.
     */
    private static void produceOutput(SettlementState state, Building building) {
        Stockpile stockpile = state.getStockpile();
        int level = building.getLevel();
        int baseAmount = level;  // Higher level = more output
        
        switch (building.getType()) {
            case GREENHOUSE -> {
                // Produce random crop
                Item[] crops = {Items.WHEAT, Items.CARROT, Items.POTATO};
                if (state.getTechTree().isUnlocked(TechTree.TechNode.FARMING_II)) {
                    crops = new Item[]{Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.MELON_SLICE};
                }
                if (state.getTechTree().isUnlocked(TechTree.TechNode.FARMING_III)) {
                    crops = new Item[]{Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.MELON_SLICE, Items.PUMPKIN};
                }
                Item crop = crops[random.nextInt(crops.length)];
                int amount = baseAmount * (2 + random.nextInt(3));  // 2-4 per level
                stockpile.addResource(crop, amount);
                state.addItemsProduced(amount);
            }
            
            case QUARRY -> {
                // Produce based on level (deeper = rarer ores)
                Item ore;
                int amount;
                float roll = random.nextFloat();
                
                if (level >= 5 && roll < 0.05f && state.getTechTree().isUnlocked(TechTree.TechNode.MINING_III)) {
                    ore = Items.DIAMOND;
                    amount = 1;
                } else if (level >= 4 && roll < 0.15f && state.getTechTree().isUnlocked(TechTree.TechNode.MINING_II)) {
                    ore = Items.RAW_GOLD;
                    amount = 1 + random.nextInt(2);
                } else if (level >= 3 && roll < 0.30f && state.getTechTree().isUnlocked(TechTree.TechNode.MINING_II)) {
                    ore = Items.RAW_IRON;
                    amount = 1 + random.nextInt(3);
                } else if (level >= 2 && roll < 0.50f) {
                    ore = Items.COAL;
                    amount = 2 + random.nextInt(3);
                } else {
                    ore = Items.COBBLESTONE;
                    amount = 3 + random.nextInt(5);
                }
                
                stockpile.addResource(ore, amount * baseAmount);
                state.addItemsProduced(amount * baseAmount);
            }
            
            case LUMBER_YARD -> {
                // Produce logs and occasionally planks
                int logAmount = baseAmount * (3 + random.nextInt(3));
                stockpile.addResource(Items.OAK_LOG, logAmount);
                
                // Chance for bonus planks
                if (random.nextFloat() < 0.3f) {
                    int plankAmount = logAmount / 2;
                    stockpile.addResource(Items.OAK_PLANKS, plankAmount);
                    state.addItemsProduced(plankAmount);
                }
                
                state.addItemsProduced(logAmount);
            }
            
            case MOB_BARN -> {
                // Produce animal products
                Item[] products = {Items.LEATHER, Items.BEEF, Items.PORKCHOP};
                if (state.getTechTree().isUnlocked(TechTree.TechNode.RANCHING_II)) {
                    products = new Item[]{Items.LEATHER, Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.WHITE_WOOL, Items.EGG, Items.FEATHER};
                }
                
                // Produce 1-2 different products
                int productCount = 1 + random.nextInt(2);
                for (int i = 0; i < productCount; i++) {
                    Item product = products[random.nextInt(products.length)];
                    int amount = baseAmount * (1 + random.nextInt(3));
                    stockpile.addResource(product, amount);
                    state.addItemsProduced(amount);
                }
            }
            
            default -> {}
        }
    }
    
    /**
     * Check if a new Minecraft day has started and refresh contracts.
     */
    private static void checkDailyReset(SettlementState state, ServerLevel level) {
        long currentTime = level.getDayTime();
        long dayTime = currentTime % 24000L;
        
        // Check if it's morning (time 0-100) and contracts haven't been refreshed today
        if (dayTime < 100) {
            long currentDay = currentTime / 24000L;
            long lastRefreshDay = state.getLastContractRefresh() / 24000L;
            
            if (currentDay > lastRefreshDay || state.getLastContractRefresh() < 0) {
                PocketSettlement.LOGGER.info("[Pocket Settlement] New day! Generating contracts...");
                state.generateNewContracts(currentTime);
                state.incrementDaysPlayed();
                state.generateDailyInfluence();
                
                // Daily Market income
                int marketCount = state.getBuildingCount(BuildingType.MARKET);
                if (marketCount > 0) {
                    int income = marketCount * 50;  // 50 coins per market per day
                    state.addCoins(income);
                    PocketSettlement.LOGGER.info("[Pocket Settlement] Market income: {} coins", income);
                }
                
                // Daily Bank interest
                if (state.getTechTree().isUnlocked(TechTree.TechNode.COMMERCE_II)) {
                    int bankCount = state.getBuildingCount(BuildingType.BANK);
                    if (bankCount > 0) {
                        int interest = (int) (state.getCoins() * 0.10f * bankCount);  // 10% per bank
                        state.addCoins(interest);
                        PocketSettlement.LOGGER.info("[Pocket Settlement] Bank interest: {} coins", interest);
                    }
                }
            }
        }
    }
    
    /**
     * Process happiness changes based on adjacency and conditions.
     */
    private static void processHappiness(SettlementState state) {
        // Calculate settlement-wide happiness modifiers
        int happinessModifier = 0;
        
        // Check for positive/negative adjacencies
        for (int x = 0; x < SettlementState.GRID_SIZE; x++) {
            for (int z = 0; z < SettlementState.GRID_SIZE; z++) {
                Building building = state.getBuilding(x, z);
                
                if (building.getType() == BuildingType.HOUSE) {
                    // Check neighbors
                    for (Building neighbor : state.getAdjacentBuildings(x, z)) {
                        switch (neighbor.getType().getAdjacencyBonus()) {
                            case NATURE -> happinessModifier += 2;      // Farms nearby = good
                            case POLLUTION -> happinessModifier -= 3;   // Quarries nearby = bad
                            case COMMERCE -> happinessModifier += 1;    // Markets nearby = good
                            case SECURITY -> happinessModifier += 2;    // Guard towers = good
                            default -> {}
                        }
                    }
                }
            }
        }
        
        // Apply happiness changes to all citizens (very slowly - 1 point per minute max)
        if (random.nextInt(60) == 0) {  // Once per minute on average
            for (Citizen citizen : state.getAllCitizens()) {
                int targetHappiness = 50 + happinessModifier;
                int currentHappiness = citizen.getHappiness();
                
                if (currentHappiness < targetHappiness) {
                    citizen.adjustHappiness(1);
                } else if (currentHappiness > targetHappiness) {
                    citizen.adjustHappiness(-1);
                }
            }
        }
    }
}
