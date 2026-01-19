package com.uipg9.pocketsettlement.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * The Stockpile stores virtual resources produced by buildings.
 * Uses integers instead of ItemStacks for efficient large-scale storage.
 */
public class Stockpile {
    private Map<String, Integer> resources;  // Item ID -> Count
    private int maxCapacity;
    
    // Default resource limits
    private static final int DEFAULT_CAPACITY = 1000;
    
    public Stockpile() {
        this.resources = new HashMap<>();
        this.maxCapacity = DEFAULT_CAPACITY;
    }
    
    // === Resource Management ===
    
    /**
     * Add resources to the stockpile.
     * @return The amount actually added (may be less if capacity reached)
     */
    public int addResource(Item item, int amount) {
        String itemId = getItemId(item);
        int current = resources.getOrDefault(itemId, 0);
        int toAdd = Math.min(amount, maxCapacity - current);
        
        if (toAdd > 0) {
            resources.put(itemId, current + toAdd);
        }
        
        return toAdd;
    }
    
    /**
     * Remove resources from the stockpile.
     * @return true if successful, false if not enough resources
     */
    public boolean removeResource(Item item, int amount) {
        String itemId = getItemId(item);
        int current = resources.getOrDefault(itemId, 0);
        
        if (current >= amount) {
            int newAmount = current - amount;
            if (newAmount <= 0) {
                resources.remove(itemId);
            } else {
                resources.put(itemId, newAmount);
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Get the count of a specific resource.
     */
    public int getResourceCount(Item item) {
        return resources.getOrDefault(getItemId(item), 0);
    }
    
    /**
     * Check if stockpile has enough of a resource.
     */
    public boolean hasResource(Item item, int amount) {
        return getResourceCount(item) >= amount;
    }
    
    /**
     * Get total number of items stored.
     */
    public int getTotalItems() {
        return resources.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get all resources as a map.
     */
    public Map<Item, Integer> getAllResources() {
        Map<Item, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            Item item = getItemFromId(entry.getKey());
            if (item != null && item != Items.AIR) {
                result.put(item, entry.getValue());
            }
        }
        return result;
    }
    
    // === Capacity ===
    
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    public void setMaxCapacity(int capacity) {
        this.maxCapacity = Math.max(100, capacity);
    }
    
    public void upgradeCapacity(int increase) {
        this.maxCapacity += increase;
    }
    
    public boolean isFull() {
        return getTotalItems() >= maxCapacity;
    }
    
    public int getRemainingCapacity() {
        return Math.max(0, maxCapacity - getTotalItems());
    }
    
    // === Withdrawal to Player ===
    
    /**
     * Withdraw items from stockpile to player's inventory.
     * @return Amount actually withdrawn
     */
    public int withdrawToPlayer(ServerPlayer player, Item item, int amount) {
        int available = getResourceCount(item);
        int toWithdraw = Math.min(amount, available);
        
        if (toWithdraw <= 0) return 0;
        
        int withdrawn = 0;
        while (withdrawn < toWithdraw) {
            int stackSize = Math.min(item.getDefaultMaxStackSize(), toWithdraw - withdrawn);
            ItemStack stack = new ItemStack(item, stackSize);
            
            if (player.getInventory().add(stack)) {
                withdrawn += stackSize;
            } else {
                // Inventory full - drop remaining items at player's feet
                int remaining = toWithdraw - withdrawn;
                ItemStack dropStack = new ItemStack(item, remaining);
                player.drop(dropStack, false);
                withdrawn = toWithdraw;
                break;
            }
        }
        
        removeResource(item, withdrawn);
        return withdrawn;
    }
    
    // === Helper Methods ===
    
    private String getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
    
    private Item getItemFromId(String id) {
        try {
            Identifier loc = Identifier.parse(id);
            return BuiltInRegistries.ITEM.getValue(loc);
        } catch (Exception e) {
            return Items.AIR;
        }
    }
    
    // === NBT Serialization ===
    
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("maxCapacity", maxCapacity);
        
        CompoundTag resourcesNBT = new CompoundTag();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            resourcesNBT.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("resources", resourcesNBT);
        
        return nbt;
    }
    
    public static Stockpile fromNBT(CompoundTag nbt) {
        Stockpile stockpile = new Stockpile();
        stockpile.maxCapacity = nbt.getIntOr("maxCapacity", DEFAULT_CAPACITY);
        if (stockpile.maxCapacity < 100) stockpile.maxCapacity = DEFAULT_CAPACITY;
        
        CompoundTag resourcesNBT = nbt.getCompoundOrEmpty("resources");
        for (String key : resourcesNBT.keySet()) {
            stockpile.resources.put(key, resourcesNBT.getIntOr(key, 0));
        }
        
        return stockpile;
    }
    
    // === Common Resource Presets ===
    
    public static class Resources {
        // Farming outputs
        public static final Item WHEAT = Items.WHEAT;
        public static final Item CARROT = Items.CARROT;
        public static final Item POTATO = Items.POTATO;
        public static final Item BEETROOT = Items.BEETROOT;
        public static final Item MELON = Items.MELON_SLICE;
        public static final Item PUMPKIN = Items.PUMPKIN;
        
        // Mining outputs
        public static final Item STONE = Items.COBBLESTONE;
        public static final Item COAL = Items.COAL;
        public static final Item IRON = Items.RAW_IRON;
        public static final Item GOLD = Items.RAW_GOLD;
        public static final Item DIAMOND = Items.DIAMOND;
        
        // Lumber outputs
        public static final Item LOG = Items.OAK_LOG;
        public static final Item PLANKS = Items.OAK_PLANKS;
        public static final Item STICK = Items.STICK;
        
        // Ranching outputs
        public static final Item LEATHER = Items.LEATHER;
        public static final Item BEEF = Items.BEEF;
        public static final Item PORKCHOP = Items.PORKCHOP;
        public static final Item MUTTON = Items.MUTTON;
        public static final Item WOOL = Items.WHITE_WOOL;
        public static final Item FEATHER = Items.FEATHER;
        public static final Item EGG = Items.EGG;
    }
}
