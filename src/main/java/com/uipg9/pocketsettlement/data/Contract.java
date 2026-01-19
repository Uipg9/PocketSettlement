package com.uipg9.pocketsettlement.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Represents a daily contract from the Merchant.
 * Contracts request resources in exchange for coins.
 */
public class Contract {
    private String id;
    private String requiredItemId;
    private int requiredAmount;
    private int reward;
    private int delivered;     // How much has been delivered
    private boolean completed;
    private long expiresAt;    // World time when this contract expires
    
    // Contract generation parameters
    private static final ContractTemplate[] TEMPLATES = {
        // Farming contracts
        new ContractTemplate(Items.WHEAT, 50, 200, 50, 200),
        new ContractTemplate(Items.CARROT, 40, 150, 60, 180),
        new ContractTemplate(Items.POTATO, 40, 150, 55, 170),
        new ContractTemplate(Items.BEETROOT, 30, 100, 75, 200),
        
        // Mining contracts
        new ContractTemplate(Items.COBBLESTONE, 100, 500, 30, 150),
        new ContractTemplate(Items.COAL, 30, 100, 60, 180),
        new ContractTemplate(Items.RAW_IRON, 20, 80, 100, 300),
        new ContractTemplate(Items.RAW_GOLD, 10, 40, 150, 400),
        new ContractTemplate(Items.DIAMOND, 5, 20, 300, 800),
        
        // Lumber contracts
        new ContractTemplate(Items.OAK_LOG, 50, 200, 40, 160),
        new ContractTemplate(Items.OAK_PLANKS, 100, 400, 25, 100),
        new ContractTemplate(Items.STICK, 150, 500, 15, 60),
        
        // Ranching contracts
        new ContractTemplate(Items.LEATHER, 20, 80, 80, 250),
        new ContractTemplate(Items.BEEF, 30, 100, 70, 200),
        new ContractTemplate(Items.WHITE_WOOL, 40, 120, 50, 150),
        new ContractTemplate(Items.EGG, 30, 100, 40, 120)
    };
    
    public Contract() {
        this.id = UUID.randomUUID().toString();
    }
    
    /**
     * Generate a random contract.
     */
    public static Contract generateRandom(long currentWorldTime) {
        Contract contract = new Contract();
        
        // Pick random template
        ContractTemplate template = TEMPLATES[(int) (Math.random() * TEMPLATES.length)];
        
        contract.requiredItemId = BuiltInRegistries.ITEM.getKey(template.item).toString();
        contract.requiredAmount = template.minAmount + (int) (Math.random() * (template.maxAmount - template.minAmount + 1));
        contract.reward = template.minReward + (int) (Math.random() * (template.maxReward - template.minReward + 1));
        
        // Scale reward based on amount
        float scale = (float) contract.requiredAmount / template.minAmount;
        contract.reward = (int) (contract.reward * scale);
        
        contract.delivered = 0;
        contract.completed = false;
        contract.expiresAt = currentWorldTime + 24000L; // Expires in 1 Minecraft day
        
        return contract;
    }
    
    // === Getters ===
    
    public String getId() {
        return id;
    }
    
    public Item getRequiredItem() {
        try {
            Identifier loc = Identifier.parse(requiredItemId);
            return BuiltInRegistries.ITEM.getValue(loc);
        } catch (Exception e) {
            return Items.AIR;
        }
    }
    
    public int getRequiredAmount() {
        return requiredAmount;
    }
    
    public int getReward() {
        return reward;
    }
    
    public int getDelivered() {
        return delivered;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public boolean isExpired(long currentWorldTime) {
        return currentWorldTime >= expiresAt;
    }
    
    public int getRemainingAmount() {
        return Math.max(0, requiredAmount - delivered);
    }
    
    public float getProgress() {
        return (float) delivered / requiredAmount;
    }
    
    // === Contract Actions ===
    
    /**
     * Deliver items to this contract.
     * @return The amount actually accepted
     */
    public int deliver(int amount) {
        if (completed) return 0;
        
        int remaining = requiredAmount - delivered;
        int toAccept = Math.min(amount, remaining);
        
        delivered += toAccept;
        
        if (delivered >= requiredAmount) {
            completed = true;
        }
        
        return toAccept;
    }
    
    /**
     * Get a display string for this contract.
     */
    public String getDisplayString() {
        Item item = getRequiredItem();
        String itemName = item.getName().getString();
        return String.format("Need %d %s - Reward: %d coins", requiredAmount, itemName, reward);
    }
    
    // === NBT Serialization ===
    
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", id);
        nbt.putString("itemId", requiredItemId);
        nbt.putInt("amount", requiredAmount);
        nbt.putInt("reward", reward);
        nbt.putInt("delivered", delivered);
        nbt.putBoolean("completed", completed);
        nbt.putLong("expires", expiresAt);
        return nbt;
    }
    
    public static Contract fromNBT(CompoundTag nbt) {
        Contract contract = new Contract();
        contract.id = nbt.getStringOr("id", UUID.randomUUID().toString());
        contract.requiredItemId = nbt.getStringOr("itemId", "minecraft:air");
        contract.requiredAmount = nbt.getIntOr("amount", 1);
        contract.reward = nbt.getIntOr("reward", 10);
        contract.delivered = nbt.getIntOr("delivered", 0);
        contract.completed = nbt.getBooleanOr("completed", false);
        contract.expiresAt = nbt.getLongOr("expires", 0L);
        return contract;
    }
    
    // === Contract Template ===
    
    private record ContractTemplate(Item item, int minAmount, int maxAmount, int minReward, int maxReward) {}
}
