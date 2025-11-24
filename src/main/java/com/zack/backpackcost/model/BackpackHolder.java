package com.zack.backpackcost.model;

import com.zack.backpackcost.util.BackpackTier;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class BackpackHolder implements InventoryHolder {

    private final UUID backpackId;
    private final BackpackTier tier;

    public BackpackHolder(UUID backpackId, BackpackTier tier) {
        this.backpackId = backpackId;
        this.tier = tier;
    }

    public UUID getBackpackId() {
        return backpackId;
    }

    public BackpackTier getTier() {
        return tier;
    }

    @Override
    public Inventory getInventory() {
        return null; // Bukkit fills this when creating the inventory.
    }
}
