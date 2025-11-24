package com.zack.backpackcost.util;

import com.zack.backpackcost.BackpackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BackpackItemUtil {

    private static final String KEY_ITEM = "backpack-item";
    private static final String KEY_ID = "backpack-id";
    private static final String KEY_TIER = "backpack-tier";
    private static final String KEY_FILLER = "backpack-filler";
    private static final String KEY_AUTOPICK = "backpack-autopick";

    private BackpackItemUtil() {}

    public static ItemStack createItem(BackpackPlugin plugin, BackpackTier tier, UUID id, boolean autoPickup) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(color("&6" + tier.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(color("&7" + tier.getDefaultLore()));
        if (tier == BackpackTier.NETHERITE) {
            lore.add(color("&7Despite being made out of netherite, it doesn't weigh that much"));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setCustomModelData(tier.getCustomModelData());
        meta.getPersistentDataContainer().set(getKey(plugin, KEY_ITEM), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(getKey(plugin, KEY_ID), PersistentDataType.STRING, id.toString());
        meta.getPersistentDataContainer().set(getKey(plugin, KEY_TIER), PersistentDataType.STRING, tier.name());
        meta.getPersistentDataContainer().set(getKey(plugin, KEY_AUTOPICK), PersistentDataType.BYTE, (byte) (autoPickup ? 1 : 0));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBackpackItem(BackpackPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte marker = meta.getPersistentDataContainer().get(getKey(plugin, KEY_ITEM), PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public static boolean isAutoPickupEnabled(BackpackPlugin plugin, ItemStack stack) {
        if (!isBackpackItem(plugin, stack)) return false;
        ItemMeta meta = stack.getItemMeta();
        Byte val = meta.getPersistentDataContainer().get(getKey(plugin, KEY_AUTOPICK), PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public static boolean toggleAutoPickup(BackpackPlugin plugin, ItemStack stack) {
        if (!isBackpackItem(plugin, stack)) return false;
        ItemMeta meta = stack.getItemMeta();
        byte next = (byte) (isAutoPickupEnabled(plugin, stack) ? 0 : 1);
        meta.getPersistentDataContainer().set(getKey(plugin, KEY_AUTOPICK), PersistentDataType.BYTE, next);
        stack.setItemMeta(meta);
        return next == (byte) 1;
    }

    public static void ensureModelData(BackpackPlugin plugin, ItemStack stack) {
        if (!isBackpackItem(plugin, stack)) return;
        ItemMeta meta = stack.getItemMeta();
        BackpackTier tier = getTier(plugin, stack);
        if (tier == null || meta == null) return;
        Integer cmd = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        if (cmd == null || cmd != tier.getCustomModelData()) {
            meta.setCustomModelData(tier.getCustomModelData());
            stack.setItemMeta(meta);
        }
    }

    public static void refreshInventoryModels(BackpackPlugin plugin, org.bukkit.inventory.Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            ensureModelData(plugin, stack);
        }
    }

    public static BackpackTier getTier(BackpackPlugin plugin, ItemStack stack) {
        if (!isBackpackItem(plugin, stack)) return null;
        ItemMeta meta = stack.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(getKey(plugin, KEY_TIER), PersistentDataType.STRING);
        return raw == null ? BackpackTier.LEATHER : BackpackTier.fromString(raw);
    }

    public static UUID getOrCreateId(BackpackPlugin plugin, ItemStack stack) {
        if (!isBackpackItem(plugin, stack)) return UUID.randomUUID();
        ItemMeta meta = stack.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(getKey(plugin, KEY_ID), PersistentDataType.STRING);
        if (raw != null) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        UUID id = UUID.randomUUID();
        meta.getPersistentDataContainer().set(getKey(plugin, KEY_ID), PersistentDataType.STRING, id.toString());
        meta.getPersistentDataContainer().set(getKey(plugin, KEY_ITEM), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return id;
    }

    public static ItemStack createFiller(BackpackPlugin plugin) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.getPersistentDataContainer().set(getKey(plugin, KEY_FILLER), PersistentDataType.BYTE, (byte) 1);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public static boolean isFiller(BackpackPlugin plugin, ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        Byte marker = meta.getPersistentDataContainer().get(getKey(plugin, KEY_FILLER), PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private static NamespacedKey getKey(BackpackPlugin plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
