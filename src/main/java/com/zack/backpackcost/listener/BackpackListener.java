package com.zack.backpackcost.listener;

import com.zack.backpackcost.BackpackPlugin;
import com.zack.backpackcost.model.BackpackHolder;
import com.zack.backpackcost.util.BackpackItemUtil;
import com.zack.backpackcost.util.BackpackTier;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BackpackListener implements Listener {

    private final BackpackPlugin plugin;

    public BackpackListener(BackpackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        plugin.getBackpackManager().fromHolder(event.getInventory().getHolder())
                .ifPresent(uuid -> {
                    BackpackTier tier = plugin.getBackpackManager().getTier(uuid, BackpackTier.LEATHER);
                    plugin.getBackpackManager().saveContents(uuid, event.getInventory(), tier);
                    plugin.getWeightManager().updatePlayer((Player) event.getPlayer());
                });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isBackpackTop(event.getView().getTopInventory())) {
            return;
        }
        if (plugin.getConfig().getBoolean("backpack.allow-nesting", false)) {
            return;
        }
        int usable = getTier(event.getView().getTopInventory()).getUsableSlots();
        if (BackpackItemUtil.isBackpackItem(plugin, event.getCurrentItem()) && event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory())
                && (BackpackItemUtil.isBackpackItem(plugin, event.getCursor()) || event.getSlot() >= usable)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (plugin.getConfig().getBoolean("backpack.allow-nesting", false)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!isBackpackTop(top)) {
            return;
        }
        int usable = getTier(top).getUsableSlots();
        ItemStack cursor = event.getOldCursor();
        if (!BackpackItemUtil.isBackpackItem(plugin, cursor)) {
            return;
        }
        boolean affectsTop = event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize());
        if (affectsTop) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // avoid double firing from offhand
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!BackpackItemUtil.isBackpackItem(plugin, item)) {
            return;
        }
        BackpackItemUtil.ensureModelData(plugin, item);
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            event.setCancelled(true);
            boolean enabled = BackpackItemUtil.toggleAutoPickup(plugin, item);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, enabled ? 1.4f : 0.8f);
            Component msg = Component.text("Backpack item pickup ", NamedTextColor.GRAY)
                    .append(Component.text(enabled ? "enabled" : "disabled", enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            player.sendActionBar(msg);
            return;
        }
        event.setCancelled(true); // prevent placing/using as chest
        BackpackTier tier = BackpackItemUtil.getTier(plugin, item);
        if (tier == null) {
            tier = BackpackTier.LEATHER;
        }
        var id = BackpackItemUtil.getOrCreateId(plugin, item);
        plugin.getBackpackManager().openBackpack(player, id, tier);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // No-op; backpacks save on close and upgrades.
    }

    private boolean isBackpackTop(Inventory top) {
        return plugin.getBackpackManager().fromHolder(top.getHolder()).isPresent();
    }

    private BackpackTier getTier(Inventory top) {
        if (top.getHolder() instanceof BackpackHolder holder) {
            return plugin.getBackpackManager().getTier(holder.getBackpackId(), holder.getTier());
        }
        return BackpackTier.LEATHER;
    }
}
