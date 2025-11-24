package com.zack.backpackcost.listener;

import com.zack.backpackcost.BackpackPlugin;
import com.zack.backpackcost.model.BackpackManager;
import com.zack.backpackcost.util.BackpackItemUtil;
import com.zack.backpackcost.util.BackpackTier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.UUID;

public class BackpackCraftListener implements Listener {

    private final BackpackPlugin plugin;

    public BackpackCraftListener(BackpackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        if (matrix.length < 9) {
            return;
        }

        // Base leather backpack
        if (isPattern(matrix, Material.LEATHER, Material.CHEST, null)) {
            UUID id = UUID.randomUUID();
            inv.setResult(BackpackItemUtil.createItem(plugin, BackpackTier.LEATHER, id, false));
            return;
        }

        // Upgrades
        if (upgradePattern(matrix, BackpackTier.LEATHER, BackpackTier.IRON, Material.IRON_INGOT, inv)) return;
        if (upgradePattern(matrix, BackpackTier.IRON, BackpackTier.GOLD, Material.GOLD_INGOT, inv)) return;
        if (upgradePattern(matrix, BackpackTier.GOLD, BackpackTier.DIAMOND, Material.DIAMOND, inv)) return;
        if (upgradePattern(matrix, BackpackTier.DIAMOND, BackpackTier.NETHERITE, Material.NETHERITE_INGOT, inv)) return;
    }

    private boolean upgradePattern(ItemStack[] matrix, BackpackTier from, BackpackTier to, Material surround, CraftingInventory inv) {
        if (!isPattern(matrix, surround, null, from)) return false;
        ItemStack center = matrix[4];
        BackpackTier tier = BackpackItemUtil.getTier(plugin, center);
        if (tier != from) return false;
        UUID id = BackpackItemUtil.getOrCreateId(plugin, center);
        BackpackManager manager = plugin.getBackpackManager();
        // Persist contents before changing size/tier so items transfer.
        manager.saveContents(id, manager.getBackpack(id, from), from);
        manager.upgradeTier(id, to);
        boolean auto = BackpackItemUtil.isAutoPickupEnabled(plugin, center);
        inv.setResult(BackpackItemUtil.createItem(plugin, to, id, auto));
        return true;
    }

    private boolean isPattern(ItemStack[] matrix, Material surround, Material centerOverride, BackpackTier centerTier) {
        if (matrix[4] == null) return false;
        if (centerOverride != null && (matrix[4].getType() != centerOverride)) return false;
        if (centerTier != null && !BackpackItemUtil.isBackpackItem(plugin, matrix[4])) return false;
        for (int i = 0; i < matrix.length; i++) {
            if (i == 4) continue;
            if (matrix[i] == null || matrix[i].getType() != surround) return false;
        }
        return true;
    }
}
