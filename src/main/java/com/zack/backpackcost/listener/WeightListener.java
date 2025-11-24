package com.zack.backpackcost.listener;

import com.zack.backpackcost.BackpackPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class WeightListener implements Listener {

    private final BackpackPlugin plugin;

    public WeightListener(BackpackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getWeightManager().updatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        // Refresh penalties when sprint state changes but do not block sprinting.
        plugin.getWeightManager().updatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        scheduleUpdate(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        scheduleUpdate(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleUpdate(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleUpdate(player);
        }
    }

    private void scheduleUpdate(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getWeightManager().updatePlayer(player);
            }
        }.runTask(plugin);
    }
}
