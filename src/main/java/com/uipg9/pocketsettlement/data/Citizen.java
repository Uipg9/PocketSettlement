package com.uipg9.pocketsettlement.data;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/**
 * Represents a citizen in the settlement.
 * Citizens have names, jobs, levels, and XP progression.
 */
public class Citizen {
    private String id;
    private String name;
    private CitizenJob job;
    private int level;    // 1-5
    private int xp;       // XP towards next level
    private int happiness; // 0-100
    
    // XP required per level: 100, 250, 500, 1000 (to reach levels 2, 3, 4, 5)
    private static final int[] XP_REQUIREMENTS = {0, 100, 250, 500, 1000};
    
    // Random names for citizens
    private static final String[] FIRST_NAMES = {
        "Aldric", "Barnaby", "Cedric", "Damian", "Edmund",
        "Fletcher", "Gareth", "Harold", "Isaac", "Jasper",
        "Alaric", "Beatrice", "Clara", "Diana", "Eleanor",
        "Fiona", "Gwendolyn", "Helena", "Iris", "Juliana",
        "Magnus", "Neville", "Oliver", "Percy", "Quincy",
        "Roland", "Sebastian", "Theodore", "Ulric", "Victor"
    };
    
    private static final String[] LAST_NAMES = {
        "Baker", "Cooper", "Fletcher", "Gardner", "Hunter",
        "Mason", "Miller", "Potter", "Smith", "Tanner",
        "Weaver", "Wright", "Thatcher", "Cartwright", "Shepherd",
        "Fisher", "Brewer", "Sawyer", "Chandler", "Dyer"
    };
    
    public Citizen() {
        this.id = UUID.randomUUID().toString();
        this.name = generateRandomName();
        this.job = CitizenJob.NONE;
        this.level = 1;
        this.xp = 0;
        this.happiness = 50;
    }
    
    public Citizen(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.job = CitizenJob.NONE;
        this.level = 1;
        this.xp = 0;
        this.happiness = 50;
    }
    
    private static String generateRandomName() {
        String firstName = FIRST_NAMES[(int) (Math.random() * FIRST_NAMES.length)];
        String lastName = LAST_NAMES[(int) (Math.random() * LAST_NAMES.length)];
        return firstName + " " + lastName;
    }
    
    // === Getters & Setters ===
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public CitizenJob getJob() {
        return job;
    }
    
    public void setJob(CitizenJob job) {
        this.job = job;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(5, level));
    }
    
    public int getXp() {
        return xp;
    }
    
    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }
    
    public int getHappiness() {
        return happiness;
    }
    
    public void setHappiness(int happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness));
    }
    
    public void adjustHappiness(int amount) {
        this.happiness = Math.max(0, Math.min(100, this.happiness + amount));
    }
    
    // === Level & XP System ===
    
    /**
     * Add XP and check for level up.
     * @return true if leveled up
     */
    public boolean addXp(int amount) {
        this.xp += amount;
        
        if (level < 5 && xp >= getXpForNextLevel()) {
            xp -= getXpForNextLevel();
            level++;
            return true;
        }
        
        return false;
    }
    
    public int getXpForNextLevel() {
        if (level >= 5) return Integer.MAX_VALUE;
        return XP_REQUIREMENTS[level];
    }
    
    public float getXpProgress() {
        if (level >= 5) return 1.0f;
        return (float) xp / getXpForNextLevel();
    }
    
    /**
     * Get efficiency multiplier based on level.
     * Level 1: 100%, Level 2: 120%, Level 3: 150%, Level 4: 200%, Level 5: 300%
     */
    public int getEfficiencyPercent() {
        return switch (level) {
            case 1 -> 100;
            case 2 -> 120;
            case 3 -> 150;
            case 4 -> 200;
            case 5 -> 300;
            default -> 100;
        };
    }
    
    /**
     * Get training cost to level up at academy.
     */
    public int getTrainingCost() {
        if (level >= 5) return Integer.MAX_VALUE;
        return switch (level) {
            case 1 -> 50;
            case 2 -> 100;
            case 3 -> 200;
            case 4 -> 500;
            default -> 1000;
        };
    }
    
    public boolean isEmployed() {
        return job != CitizenJob.NONE;
    }
    
    // === NBT Serialization ===
    
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", id);
        nbt.putString("name", name);
        nbt.putString("job", job.name());
        nbt.putInt("level", level);
        nbt.putInt("xp", xp);
        nbt.putInt("happiness", happiness);
        return nbt;
    }
    
    public static Citizen fromNBT(CompoundTag nbt) {
        Citizen citizen = new Citizen();
        citizen.id = nbt.getStringOr("id", "");
        if (citizen.id.isEmpty()) {
            citizen.id = UUID.randomUUID().toString();
        }
        citizen.name = nbt.getStringOr("name", "");
        if (citizen.name.isEmpty()) {
            citizen.name = generateRandomName();
        }
        try {
            citizen.job = CitizenJob.valueOf(nbt.getStringOr("job", "NONE"));
        } catch (IllegalArgumentException e) {
            citizen.job = CitizenJob.NONE;
        }
        citizen.level = Math.max(1, nbt.getIntOr("level", 1));
        citizen.xp = nbt.getIntOr("xp", 0);
        citizen.happiness = nbt.getIntOr("happiness", 50);
        if (citizen.happiness == 0) citizen.happiness = 50;
        return citizen;
    }
    
    @Override
    public String toString() {
        return String.format("%s (Lv%d %s)", name, level, job.getDisplayName());
    }
}
