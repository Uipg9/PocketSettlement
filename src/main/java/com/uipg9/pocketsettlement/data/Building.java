package com.uipg9.pocketsettlement.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a single building in the settlement grid.
 * Buildings have types, levels, workers, and production progress.
 */
public class Building {
    private BuildingType type;
    private int level;
    private int progress;       // 0-100, production progress
    private String assignedCitizenId;  // UUID string of assigned citizen
    
    public Building() {
        this.type = BuildingType.EMPTY;
        this.level = 1;
        this.progress = 0;
        this.assignedCitizenId = "";
    }
    
    public Building(BuildingType type) {
        this.type = type;
        this.level = 1;
        this.progress = 0;
        this.assignedCitizenId = "";
    }
    
    // === Getters & Setters ===
    
    public BuildingType getType() {
        return type;
    }
    
    public void setType(BuildingType type) {
        this.type = type;
        this.progress = 0;  // Reset progress when changing type
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(5, level));
    }
    
    public void upgrade() {
        if (level < 5) {
            level++;
        }
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }
    
    public void addProgress(int amount) {
        this.progress = Math.min(100, this.progress + amount);
    }
    
    public boolean isProductionReady() {
        return progress >= 100;
    }
    
    public void resetProgress() {
        this.progress = 0;
    }
    
    public String getAssignedCitizenId() {
        return assignedCitizenId;
    }
    
    public void setAssignedCitizenId(String citizenId) {
        this.assignedCitizenId = citizenId != null ? citizenId : "";
    }
    
    public boolean hasWorker() {
        return !assignedCitizenId.isEmpty();
    }
    
    public boolean isEmpty() {
        return type == BuildingType.EMPTY;
    }
    
    // === Efficiency Calculation ===
    
    /**
     * Calculate production efficiency based on level and worker.
     * Base efficiency is 100% at level 1, increases by 25% per level.
     * Worker adds additional bonus based on their level.
     */
    public int getEfficiency(SettlementState state) {
        int baseEfficiency = 75 + (level * 25);  // 100%, 125%, 150%, 175%, 200%
        
        if (hasWorker() && state != null) {
            Citizen worker = state.getCitizen(assignedCitizenId);
            if (worker != null) {
                // Worker level adds 20% per level
                baseEfficiency += worker.getLevel() * 20;
            }
        }
        
        return baseEfficiency;
    }
    
    // === NBT Serialization ===
    
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", type.name());
        nbt.putInt("level", level);
        nbt.putInt("progress", progress);
        nbt.putString("citizen", assignedCitizenId);
        return nbt;
    }
    
    public static Building fromNBT(CompoundTag nbt) {
        Building building = new Building();
        try {
            building.type = BuildingType.valueOf(nbt.getStringOr("type", "EMPTY"));
        } catch (IllegalArgumentException e) {
            building.type = BuildingType.EMPTY;
        }
        building.level = nbt.getIntOr("level", 1);
        if (building.level < 1) building.level = 1;
        building.progress = nbt.getIntOr("progress", 0);
        building.assignedCitizenId = nbt.getStringOr("citizen", "");
        return building;
    }
    
    @Override
    public String toString() {
        return String.format("%s Lv%d (%d%%)", type.getDisplayName(), level, progress);
    }
}
