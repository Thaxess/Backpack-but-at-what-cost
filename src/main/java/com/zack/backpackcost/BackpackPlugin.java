package com.zack.backpackcost;

import com.zack.backpackcost.command.BackpackCommand;
import com.zack.backpackcost.listener.BackpackCraftListener;
import com.zack.backpackcost.listener.BackpackListener;
import com.zack.backpackcost.listener.WeightListener;
import com.zack.backpackcost.model.BackpackManager;
import com.zack.backpackcost.util.WeightManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class BackpackPlugin extends JavaPlugin {

    private BackpackManager backpackManager;
    private WeightManager weightManager;
    private String resourcePackUrl;
    private byte[] resourcePackHash;
    private boolean resourcePackRequired;
    private Component resourcePackPrompt = Component.empty();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadResourcePackConfig();
        this.weightManager = new WeightManager(this);
        this.backpackManager = new BackpackManager(this);
        // Refresh model data for online players on reload.
        Bukkit.getOnlinePlayers().forEach(this::refreshBackpackModels);

        BackpackCommand backpackCommand = new BackpackCommand(this);
        if (getCommand("backpack") != null) {
            getCommand("backpack").setExecutor(backpackCommand);
            getCommand("backpack").setTabCompleter(backpackCommand);
        }

        Bukkit.getPluginManager().registerEvents(new BackpackListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WeightListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BackpackCraftListener(this), this);

        // Start weight checks for online players so penalties stay in sync.
        long interval = getConfig().getLong("penalties.check-interval-ticks", 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> weightManager.applyPenalties(Bukkit.getOnlinePlayers()), interval, interval);
        sendResourcePack(Bukkit.getOnlinePlayers().toArray(Player[]::new));
        getLogger().info("Backpack, but at what cost? enabled.");
    }

    @Override
    public void onDisable() {
        if (backpackManager != null) {
            backpackManager.saveAll();
        }
    }

    public BackpackManager getBackpackManager() {
        return backpackManager;
    }

    public WeightManager getWeightManager() {
        return weightManager;
    }

    public void reloadPlugin() {
        reloadConfig();
        loadResourcePackConfig();
        weightManager.reload();
        backpackManager.reload();
        getLogger().info("Config reloaded.");
        weightManager.applyPenalties(Bukkit.getOnlinePlayers());
        sendResourcePack(Bukkit.getOnlinePlayers().toArray(Player[]::new));
    }

    public FileConfiguration getMessages() {
        return getConfig();
    }

    public File getStorageFile() {
        String name = getConfig().getString("storage.file", "backpacks.yml");
        return new File(getDataFolder(), name);
    }

    public void sendResourcePack(Player... players) {
        if (resourcePackUrl == null || resourcePackUrl.isEmpty()) {
            return;
        }
        for (Player player : players) {
            if (resourcePackHash != null) {
                // Signature: setResourcePack(String url, byte[] hash, Component prompt, boolean required)
                player.setResourcePack(resourcePackUrl, resourcePackHash, resourcePackPrompt, resourcePackRequired);
            } else {
                player.setResourcePack(resourcePackUrl);
            }
        }
    }

    public void refreshBackpackModels(Player player) {
        com.zack.backpackcost.util.BackpackItemUtil.refreshInventoryModels(this, player.getInventory());
    }

    private void loadResourcePackConfig() {
        var cfg = getConfig().getConfigurationSection("resource-pack");
        if (cfg == null || !cfg.getBoolean("enabled", false)) {
            resourcePackUrl = null;
            resourcePackHash = null;
            return;
        }
        String url = cfg.getString("url", "");
        if (url != null) {
            url = url.trim();
        }
        resourcePackUrl = (url == null || url.isEmpty()) ? null : url;
        resourcePackRequired = cfg.getBoolean("required", true);
        String promptRaw = cfg.getString("prompt", "");
        resourcePackPrompt = promptRaw == null ? Component.empty() : LegacyComponentSerializer.legacyAmpersand().deserialize(promptRaw);
        String sha1 = cfg.getString("sha1", "");
        resourcePackHash = parseSha1(sha1);
    }

    private byte[] parseSha1(String hex) {
        if (hex == null) return null;
        hex = hex.trim().replaceAll("\\s+", "");
        if (hex.isEmpty()) return null;
        if (hex.length() != 40) {
            getLogger().warning("resource-pack.sha1 must be 40 hex chars");
            return null;
        }
        byte[] out = new byte[20];
        for (int i = 0; i < 20; i++) {
            String part = hex.substring(i * 2, i * 2 + 2);
            try {
                out[i] = (byte) Integer.parseInt(part, 16);
            } catch (NumberFormatException ex) {
                getLogger().warning("Invalid sha1 hex in resource-pack.sha1");
                return null;
            }
        }
        return out;
    }
}
