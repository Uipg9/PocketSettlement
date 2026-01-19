package com.uipg9.pocketsettlement.data;

import com.uipg9.pocketsettlement.PocketSettlement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Main persistent state for the settlement simulation.
 * Saves to the world's data folder and persists across game sessions.
 * 
 * Uses file-based NBT storage for simplicity and compatibility.
 */
public class SettlementState extends SavedData {
    
    private static final String DATA_NAME = PocketSettlement.MOD_ID + "_settlement";
    private static final Map<ServerLevel, SettlementState> INSTANCES = new HashMap<>();
    
    // Settlement Grid (7x7 = 49 plots)
    public static final int GRID_SIZE = 7;
    private Building[][] grid;
    
    // Citizens
    private Map<String, Citizen> citizens;
    private int maxCitizens;
    
    // Economy
    private int coins;
    private int influence;
    private Stockpile stockpile;
    private List<Contract> activeContracts;
    private long lastContractRefresh;  // World time of last contract generation
    
    // Tech Tree
    private TechTree techTree;
    
    // Stats
    private long totalCoinsEarned;
    private long totalItemsProduced;
    private int daysPlayed;
    
    // Reference to level for saving
    private transient ServerLevel level;
    
    // === Constructor ===
    
    public SettlementState() {
        this.grid = new Building[GRID_SIZE][GRID_SIZE];
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                grid[x][z] = new Building();
            }
        }
        
        // Place Town Hall in center
        grid[3][3] = new Building(BuildingType.TOWN_HALL);
        
        this.citizens = new HashMap<>();
        this.maxCitizens = 5;
        
        this.coins = 500;  // Starting coins
        this.influence = 0;
        this.stockpile = new Stockpile();
        this.activeContracts = new ArrayList<>();
        this.lastContractRefresh = -1;
        
        this.techTree = new TechTree();
        
        this.totalCoinsEarned = 0;
        this.totalItemsProduced = 0;
        this.daysPlayed = 0;
    }
    
    // === Static Factory with File-Based Storage ===
    
    private static File getDataFile(ServerLevel level) {
        File worldDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File modDir = new File(worldDir, "data");
        if (!modDir.exists()) {
            modDir.mkdirs();
        }
        return new File(modDir, DATA_NAME + ".dat");
    }
    
    public static SettlementState getOrCreate(ServerLevel level) {
        // Check cache first
        if (INSTANCES.containsKey(level)) {
            return INSTANCES.get(level);
        }
        
        // Try to load from file
        SettlementState state;
        File dataFile = getDataFile(level);
        
        if (dataFile.exists()) {
            try {
                CompoundTag nbt = NbtIo.readCompressed(dataFile.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                state = load(nbt);
            } catch (IOException e) {
                PocketSettlement.LOGGER.error("Failed to load settlement data, creating new", e);
                state = new SettlementState();
            }
        } else {
            state = new SettlementState();
        }
        
        state.level = level;
        INSTANCES.put(level, state);
        return state;
    }
    
    public void saveToFile() {
        if (level == null) return;
        
        try {
            File dataFile = getDataFile(level);
            CompoundTag nbt = save(new CompoundTag());
            NbtIo.writeCompressed(nbt, dataFile.toPath());
        } catch (IOException e) {
            PocketSettlement.LOGGER.error("Failed to save settlement data", e);
        }
    }
    
    @Override
    public void setDirty() {
        super.setDirty();
        saveToFile();
    }
    
    public static SettlementState load(CompoundTag nbt) {
        SettlementState state = new SettlementState();
        
        // Load grid
        ListTag gridList = nbt.getListOrEmpty("grid");
        for (int i = 0; i < gridList.size() && i < GRID_SIZE * GRID_SIZE; i++) {
            int x = i % GRID_SIZE;
            int z = i / GRID_SIZE;
            state.grid[x][z] = Building.fromNBT(gridList.getCompoundOrEmpty(i));
        }
        
        // Load citizens
        ListTag citizenList = nbt.getListOrEmpty("citizens");
        for (int i = 0; i < citizenList.size(); i++) {
            Citizen citizen = Citizen.fromNBT(citizenList.getCompoundOrEmpty(i));
            state.citizens.put(citizen.getId(), citizen);
        }
        state.maxCitizens = nbt.getIntOr("maxCitizens", 5);
        if (state.maxCitizens < 5) state.maxCitizens = 5;
        
        // Load economy
        state.coins = nbt.getIntOr("coins", 500);
        state.influence = nbt.getIntOr("influence", 0);
        
        if (nbt.contains("stockpile")) {
            state.stockpile = Stockpile.fromNBT(nbt.getCompoundOrEmpty("stockpile"));
        }
        
        ListTag contractList = nbt.getListOrEmpty("contracts");
        for (int i = 0; i < contractList.size(); i++) {
            state.activeContracts.add(Contract.fromNBT(contractList.getCompoundOrEmpty(i)));
        }
        state.lastContractRefresh = nbt.getLongOr("lastContractRefresh", -1L);
        
        // Load tech tree
        if (nbt.contains("techTree")) {
            state.techTree = TechTree.fromNBT(nbt.getCompoundOrEmpty("techTree"));
        }
        
        // Load stats
        state.totalCoinsEarned = nbt.getLongOr("totalCoinsEarned", 0L);
        state.totalItemsProduced = nbt.getLongOr("totalItemsProduced", 0L);
        state.daysPlayed = nbt.getIntOr("daysPlayed", 0);
        
        return state;
    }
    
    public CompoundTag save(CompoundTag nbt) {
        // Save grid
        ListTag gridList = new ListTag();
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                gridList.add(grid[x][z].toNBT());
            }
        }
        nbt.put("grid", gridList);
        
        // Save citizens
        ListTag citizenList = new ListTag();
        for (Citizen citizen : citizens.values()) {
            citizenList.add(citizen.toNBT());
        }
        nbt.put("citizens", citizenList);
        nbt.putInt("maxCitizens", maxCitizens);
        
        // Save economy
        nbt.putInt("coins", coins);
        nbt.putInt("influence", influence);
        nbt.put("stockpile", stockpile.toNBT());
        
        ListTag contractList = new ListTag();
        for (Contract contract : activeContracts) {
            contractList.add(contract.toNBT());
        }
        nbt.put("contracts", contractList);
        nbt.putLong("lastContractRefresh", lastContractRefresh);
        
        // Save tech tree
        nbt.put("techTree", techTree.toNBT());
        
        // Save stats
        nbt.putLong("totalCoinsEarned", totalCoinsEarned);
        nbt.putLong("totalItemsProduced", totalItemsProduced);
        nbt.putInt("daysPlayed", daysPlayed);
        
        return nbt;
    }
    
    // === Grid Access ===
    
    public Building getBuilding(int x, int z) {
        if (x < 0 || x >= GRID_SIZE || z < 0 || z >= GRID_SIZE) {
            return null;
        }
        return grid[x][z];
    }
    
    public void setBuilding(int x, int z, Building building) {
        if (x < 0 || x >= GRID_SIZE || z < 0 || z >= GRID_SIZE) {
            return;
        }
        grid[x][z] = building;
        setDirty();
    }
    
    public boolean constructBuilding(int x, int z, BuildingType type) {
        Building current = getBuilding(x, z);
        if (current == null || current.getType() != BuildingType.EMPTY) {
            return false;
        }
        
        int cost = type.getBaseCost();
        if (coins < cost) {
            return false;
        }
        
        coins -= cost;
        current.setType(type);
        setDirty();
        return true;
    }
    
    public boolean demolishBuilding(int x, int z) {
        Building building = getBuilding(x, z);
        if (building == null || building.isEmpty() || building.getType() == BuildingType.TOWN_HALL) {
            return false;
        }
        
        // Refund 50% of base cost
        int refund = building.getType().getBaseCost() / 2;
        coins += refund;
        
        // Unassign any citizen
        if (building.hasWorker()) {
            Citizen citizen = citizens.get(building.getAssignedCitizenId());
            if (citizen != null) {
                citizen.setJob(CitizenJob.NONE);
            }
            building.setAssignedCitizenId("");
        }
        
        building.setType(BuildingType.EMPTY);
        building.setLevel(1);
        building.setProgress(0);
        setDirty();
        return true;
    }
    
    public List<Building> getAdjacentBuildings(int x, int z) {
        List<Building> adjacent = new ArrayList<>();
        
        // North
        if (z > 0) adjacent.add(grid[x][z - 1]);
        // South
        if (z < GRID_SIZE - 1) adjacent.add(grid[x][z + 1]);
        // East
        if (x < GRID_SIZE - 1) adjacent.add(grid[x + 1][z]);
        // West
        if (x > 0) adjacent.add(grid[x - 1][z]);
        
        return adjacent;
    }
    
    public int getBuildingCount(BuildingType type) {
        int count = 0;
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                if (grid[x][z].getType() == type) count++;
            }
        }
        return count;
    }
    
    // === Citizen Management ===
    
    public Citizen getCitizen(String id) {
        return citizens.get(id);
    }
    
    public Collection<Citizen> getAllCitizens() {
        return citizens.values();
    }
    
    public int getCitizenCount() {
        return citizens.size();
    }
    
    public boolean canRecruitCitizen() {
        return citizens.size() < maxCitizens && coins >= getRecruitmentCost();
    }
    
    public int getRecruitmentCost() {
        return 50 + (citizens.size() * 25);  // Cost increases with population
    }
    
    public Citizen recruitCitizen() {
        if (!canRecruitCitizen()) return null;
        
        coins -= getRecruitmentCost();
        Citizen citizen = new Citizen();
        citizens.put(citizen.getId(), citizen);
        setDirty();
        return citizen;
    }
    
    public boolean assignCitizen(String citizenId, int gridX, int gridZ) {
        Citizen citizen = citizens.get(citizenId);
        Building building = getBuilding(gridX, gridZ);
        
        if (citizen == null || building == null || building.isEmpty()) {
            return false;
        }
        
        // Unassign from previous building if any
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                Building b = grid[x][z];
                if (b.getAssignedCitizenId().equals(citizenId)) {
                    b.setAssignedCitizenId("");
                }
            }
        }
        
        // Assign to new building
        building.setAssignedCitizenId(citizenId);
        citizen.setJob(building.getType().getPreferredJob());
        setDirty();
        return true;
    }
    
    public void increaseMaxCitizens(int amount) {
        maxCitizens += amount;
        setDirty();
    }
    
    // === Economy ===
    
    public int getCoins() {
        return coins;
    }
    
    public void addCoins(int amount) {
        coins += amount;
        totalCoinsEarned += amount;
        setDirty();
    }
    
    public boolean spendCoins(int amount) {
        if (coins < amount) return false;
        coins -= amount;
        setDirty();
        return true;
    }
    
    public int getInfluence() {
        return influence;
    }
    
    public void addInfluence(int amount) {
        influence += amount;
        setDirty();
    }
    
    public boolean spendInfluence(int amount) {
        if (influence < amount) return false;
        influence -= amount;
        setDirty();
        return true;
    }
    
    public Stockpile getStockpile() {
        return stockpile;
    }
    
    public List<Contract> getActiveContracts() {
        return activeContracts;
    }
    
    public void generateNewContracts(long worldTime) {
        // Clear old contracts
        activeContracts.clear();
        
        // Generate 3 new contracts (or more based on tech)
        int contractCount = 3;
        if (techTree.isUnlocked(TechTree.TechNode.CONTRACTS_I)) contractCount = 4;
        if (techTree.isUnlocked(TechTree.TechNode.CONTRACTS_II)) contractCount = 5;
        
        for (int i = 0; i < contractCount; i++) {
            activeContracts.add(Contract.generateRandom(worldTime));
        }
        
        lastContractRefresh = worldTime;
        setDirty();
    }
    
    public long getLastContractRefresh() {
        return lastContractRefresh;
    }
    
    // === Tech Tree ===
    
    public TechTree getTechTree() {
        return techTree;
    }
    
    public boolean unlockTech(TechTree.TechNode tech) {
        if (!techTree.canUnlock(tech, coins, influence)) {
            return false;
        }
        
        spendCoins(tech.getCoinCost());
        spendInfluence(tech.getInfluenceCost());
        techTree.unlock(tech);
        setDirty();
        return true;
    }
    
    // === Stats ===
    
    public long getTotalCoinsEarned() {
        return totalCoinsEarned;
    }
    
    public long getTotalItemsProduced() {
        return totalItemsProduced;
    }
    
    public void addItemsProduced(int count) {
        totalItemsProduced += count;
        setDirty();
    }
    
    public int getDaysPlayed() {
        return daysPlayed;
    }
    
    public void incrementDaysPlayed() {
        daysPlayed++;
        setDirty();
    }
    
    public int getMaxCitizens() {
        return maxCitizens;
    }
    
    // === Happiness Calculation ===
    
    public int getAverageHappiness() {
        if (citizens.isEmpty()) return 50;
        
        int total = 0;
        for (Citizen citizen : citizens.values()) {
            total += citizen.getHappiness();
        }
        return total / citizens.size();
    }
    
    /**
     * Generate influence from happy citizens.
     * Called daily.
     */
    public void generateDailyInfluence() {
        int happiness = getAverageHappiness();
        if (happiness >= 60) {
            int influenceGain = (happiness - 50) / 10;  // 1 influence per 10 happiness above 50
            influence += influenceGain;
            setDirty();
        }
    }
    
    /**
     * Clear all settlement data and reset to initial state.
     */
    public void clearAll() {
        // Reset grid
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                grid[x][z] = new Building();
            }
        }
        // Place Town Hall in center
        grid[3][3] = new Building(BuildingType.TOWN_HALL);
        
        // Reset citizens
        citizens.clear();
        maxCitizens = 5;
        
        // Reset economy
        coins = 500;
        influence = 0;
        stockpile = new Stockpile();
        activeContracts.clear();
        lastContractRefresh = -1;
        
        // Reset tech tree
        techTree = new TechTree();
        
        // Reset stats
        totalCoinsEarned = 0;
        totalItemsProduced = 0;
        daysPlayed = 0;
        
        setDirty();
    }
}
