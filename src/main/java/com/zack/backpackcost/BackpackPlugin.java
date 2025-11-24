package com.zack.backpackcost;

import com.zack.backpackcost.command.BackpackCommand;
import com.zack.backpackcost.listener.BackpackCraftListener;
import com.zack.backpackcost.listener.BackpackListener;
import com.zack.backpackcost.listener.WeightListener;
import com.zack.backpackcost.model.BackpackManager;
import com.zack.backpackcost.util.WeightManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class BackpackPlugin extends JavaPlugin {

    private BackpackManager backpackManager;
    private WeightManager weightManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.weightManager = new WeightManager(this);
        this.backpackManager = new BackpackManager(this);

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
        weightManager.reload();
        backpackManager.reload();
        getLogger().info("Config reloaded.");
        weightManager.applyPenalties(Bukkit.getOnlinePlayers());
    }

    public FileConfiguration getMessages() {
        return getConfig();
    }

    public File getStorageFile() {
        String name = getConfig().getString("storage.file", "backpacks.yml");
        return new File(getDataFolder(), name);
    }
}
