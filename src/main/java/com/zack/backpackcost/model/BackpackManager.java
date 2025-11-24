package com.zack.backpackcost.model;

import com.zack.backpackcost.BackpackPlugin;
import com.zack.backpackcost.util.BackpackItemUtil;
import com.zack.backpackcost.util.BackpackTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BackpackManager {

    private final BackpackPlugin plugin;
    private final Map<UUID, Inventory> backpacks = new HashMap<>();
    private final Map<UUID, BackpackTier> backpackTiers = new HashMap<>();
    private File storageFile;
    private YamlConfiguration storageConfig;

    public BackpackManager(BackpackPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (storageConfig != null) {
            closeOpenBackpacks();
            saveAll();
            backpacks.clear();
        }
        this.storageFile = plugin.getStorageFile();
        if (!storageFile.exists()) {
            try {
                storageFile.getParentFile().mkdirs();
                storageFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not create storage file", e);
            }
        }
        this.storageConfig = new YamlConfiguration();
        try {
            storageConfig.load(storageFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not load storage file", e);
        }
    }

    private String getTitle(BackpackTier tier) {
        String title = plugin.getConfig().getString("backpack.title", "Backpack");
        return ChatColor.translateAlternateColorCodes('&', title + " - " + tier.getDisplayName());
    }

    public Inventory getBackpack(UUID backpackId, BackpackTier tier) {
        backpackTiers.put(backpackId, tier);
        Inventory existing = backpacks.get(backpackId);
        if (existing != null && existing.getSize() == tier.getRows() * 9) {
            applyLayout(existing, tier);
            return existing;
        }
        Inventory inventory = Bukkit.createInventory(new BackpackHolder(backpackId, tier), tier.getRows() * 9, getTitle(tier));
        loadContents(backpackId, inventory, tier);
        applyLayout(inventory, tier);
        backpacks.put(backpackId, inventory);
        return inventory;
    }

    public void openBackpack(Player viewer, UUID backpackId, BackpackTier tier) {
        viewer.openInventory(getBackpack(backpackId, tier));
    }

    public void saveAll() {
        backpacks.forEach((id, inv) -> saveContents(id, inv, backpackTiers.getOrDefault(id, BackpackTier.LEATHER)));
        if (storageConfig != null) {
            try {
                storageConfig.save(storageFile);
            } catch (IOException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save backpacks", e);
            }
        }
    }

    public void saveContents(UUID backpackId, Inventory inventory, BackpackTier tier) {
        if (storageConfig == null) return;
        ItemStack[] contents = inventory.getContents().clone();
        for (int i = tier.getUsableSlots(); i < contents.length; i++) {
            contents[i] = null; // strip filler/blocked slots
        }
        String base = backpackId.toString();
        storageConfig.set(base + ".tier", tier.name());
        storageConfig.set(base + ".contents", contents);
        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save backpack for " + backpackId, e);
        }
    }

    private void loadContents(UUID backpackId, Inventory inventory, BackpackTier tier) {
        if (storageConfig == null) {
            return;
        }
        ConfigurationSection section = storageConfig.getConfigurationSection(backpackId.toString());
        if (section != null) {
            BackpackTier storedTier = BackpackTier.fromString(section.getString("tier"));
            if (storedTier != null) {
                backpackTiers.put(backpackId, storedTier);
            }
            Object raw = section.get("contents");
            if (raw instanceof java.util.List<?>) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> list = (java.util.List<Object>) raw;
                ItemStack[] items = list.toArray(new ItemStack[0]);
                inventory.setContents(items);
            }
        }
        if (inventory.getSize() > tier.getUsableSlots()) {
            for (int i = tier.getUsableSlots(); i < inventory.getSize(); i++) {
                inventory.setItem(i, BackpackItemUtil.createFiller(plugin));
            }
        }
    }

    public Optional<UUID> fromHolder(Object holder) {
        if (holder instanceof BackpackHolder) {
            return Optional.of(((BackpackHolder) holder).getBackpackId());
        }
        return Optional.empty();
    }

    private void closeOpenBackpacks() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (fromHolder(top.getHolder()).isPresent()) {
                player.closeInventory();
            }
        });
    }

    public BackpackTier getTier(UUID backpackId, BackpackTier fallback) {
        return backpackTiers.getOrDefault(backpackId, fallback);
    }

    public void upgradeTier(UUID backpackId, BackpackTier newTier) {
        BackpackTier oldTier = backpackTiers.getOrDefault(backpackId, newTier);
        Inventory inv = getBackpack(backpackId, newTier);
        if (inv.getSize() != newTier.getRows() * 9) {
            ItemStack[] oldContents = inv.getContents();
            Inventory newInv = Bukkit.createInventory(new BackpackHolder(backpackId, newTier), newTier.getRows() * 9, getTitle(newTier));
            for (int i = 0; i < Math.min(oldContents.length, newTier.getUsableSlots()); i++) {
                newInv.setItem(i, oldContents[i]);
            }
            backpacks.put(backpackId, newInv);
            applyLayout(newInv, newTier);
            inv = newInv;
        } else {
            applyLayout(inv, newTier);
        }
        backpackTiers.put(backpackId, newTier);
        saveContents(backpackId, inv, newTier);
    }

    private void applyLayout(Inventory inv, BackpackTier tier) {
        if (tier.getUsableSlots() >= inv.getSize()) return;
        for (int i = tier.getUsableSlots(); i < inv.getSize(); i++) {
            inv.setItem(i, BackpackItemUtil.createFiller(plugin));
        }
    }
}
