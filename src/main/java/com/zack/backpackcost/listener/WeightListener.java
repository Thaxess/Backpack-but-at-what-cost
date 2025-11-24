package com.zack.backpackcost.listener;

import com.zack.backpackcost.BackpackPlugin;
import com.zack.backpackcost.model.BackpackManager;
import com.zack.backpackcost.util.BackpackItemUtil;
import com.zack.backpackcost.util.BackpackTier;
import org.bukkit.entity.Item;
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
import org.bukkit.inventory.ItemStack;

public class WeightListener implements Listener {

    private final BackpackPlugin plugin;

    public WeightListener(BackpackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getWeightManager().updatePlayer(event.getPlayer());
        plugin.sendResourcePack(event.getPlayer());
        plugin.refreshBackpackModels(event.getPlayer());
    }

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        // Refresh penalties when sprint state changes but do not block sprinting.
        plugin.getWeightManager().updatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItem();
        ItemStack stack = item.getItemStack();
        if (BackpackItemUtil.isBackpackItem(plugin, stack) && !plugin.getConfig().getBoolean("backpack.allow-nesting", false)) {
            scheduleUpdate(player);
            return;
        }
        if (player.getInventory().firstEmpty() != -1) {
            scheduleUpdate(player);
            return;
        }

        // Inventory is full; try auto-pick into an enabled backpack.
        ItemStack remaining = stack.clone();
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (!BackpackItemUtil.isBackpackItem(plugin, invItem)) continue;
            if (!BackpackItemUtil.isAutoPickupEnabled(plugin, invItem)) continue;
            BackpackTier tier = BackpackItemUtil.getTier(plugin, invItem);
            if (tier == null) tier = BackpackTier.LEATHER;
            var id = BackpackItemUtil.getOrCreateId(plugin, invItem);
            BackpackManager manager = plugin.getBackpackManager();
            remaining = manager.depositItem(id, tier, remaining);
            if (remaining == null || remaining.getAmount() <= 0) {
                break;
            }
        }

        if (remaining == null || remaining.getAmount() <= 0) {
            event.setCancelled(true);
            item.remove();
        } else {
            item.setItemStack(remaining);
        }
        scheduleUpdate(player);
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
