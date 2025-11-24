package com.zack.backpackcost.util;

import com.zack.backpackcost.BackpackPlugin;
import com.zack.backpackcost.model.BackpackManager;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class WeightManager {

    private final BackpackPlugin plugin;
    private final Map<Material, Double> materialWeights = new EnumMap<>(Material.class);
    private double defaultWeight;
    private double slownessOne;
    private double slownessTwo;
    private double slownessThree;
    private int potionDuration;

    public WeightManager(BackpackPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        materialWeights.clear();
        ConfigurationSection weights = plugin.getConfig().getConfigurationSection("weight.material-weights");
        this.defaultWeight = plugin.getConfig().getDouble("weight.default-per-item", 1.0);
        if (weights == null) {
            weights = plugin.getConfig().createSection("weight.material-weights");
        }
        // Load existing overrides.
        for (String key : weights.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material != null) {
                materialWeights.put(material, weights.getDouble(key));
            }
        }
        // Ensure every item has an entry (lighter defaults) and persist to config.
        for (Material material : Material.values()) {
            if (!material.isItem()) {
                continue;
            }
            if (!weights.isSet(material.name())) {
                weights.set(material.name(), defaultWeight);
            }
            materialWeights.putIfAbsent(material, defaultWeight);
        }
        plugin.saveConfig();
        ConfigurationSection thresholds = plugin.getConfig().getConfigurationSection("weight.thresholds");
        if (thresholds != null) {
            slownessOne = thresholds.getDouble("slowness-1", 60.0);
            slownessTwo = thresholds.getDouble("slowness-2", 90.0);
            slownessThree = thresholds.getDouble("slowness-3", 120.0);
        } else {
            slownessOne = 60.0;
            slownessTwo = 90.0;
            slownessThree = 120.0;
        }
        potionDuration = plugin.getConfig().getInt("penalties.potion-duration-ticks", 60);
    }

    public double getItemWeight(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return 0.0;
        }
        double perItem = materialWeights.getOrDefault(stack.getType(), defaultWeight);
        return perItem * stack.getAmount();
    }

    public double getInventoryWeight(Inventory inventory) {
        double total = 0.0;
        for (ItemStack stack : inventory.getContents()) {
            if (BackpackItemUtil.isFiller(plugin, stack)) {
                continue;
            }
            total += getItemWeight(stack);
        }
        return total;
    }

    public double getTotalWeight(Player player) {
        BackpackManager manager = plugin.getBackpackManager();
        double total = 0.0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (!BackpackItemUtil.isBackpackItem(plugin, stack)) {
                continue;
            }
            BackpackTier tier = BackpackItemUtil.getTier(plugin, stack);
            if (tier == null) tier = BackpackTier.LEATHER;
            int amount = Math.max(1, stack.getAmount());
            // per-backpack item weight
            total += tier.getItemWeight() * amount;
            for (int i = 0; i < amount; i++) {
                var id = BackpackItemUtil.getOrCreateId(plugin, stack);
                total += getBackpackWeight(manager, id, tier);
            }
        }
        return total;
    }

    public double getBackpackWeight(Player player, ItemStack stack, BackpackTier tier) {
        BackpackManager manager = plugin.getBackpackManager();
        var id = BackpackItemUtil.getOrCreateId(plugin, stack);
        return tier.getItemWeight() + getBackpackWeight(manager, id, tier);
    }

    private double getBackpackWeight(BackpackManager manager, java.util.UUID id, BackpackTier tier) {
        Inventory inv = manager.getBackpack(id, tier);
        return getInventoryWeight(inv) * tier.getWeightMultiplier();
    }

    public double getMaxThreshold() {
        return slownessThree;
    }

    public void applyPenalties(Collection<? extends Player> players) {
        for (Player player : players) {
            double weight = getTotalWeight(player);
            applyPenalty(player, weight);
        }
    }

    public void updatePlayer(Player player) {
        applyPenalty(player, getTotalWeight(player));
    }

    public void applyPenalty(Player player, double weight) {
        int amplifier = -1;
        if (weight >= slownessThree) {
            amplifier = 2; // Slowness III
        } else if (weight >= slownessTwo) {
            amplifier = 1; // Slowness II
        } else if (weight >= slownessOne) {
            amplifier = 0; // Slowness I
        }

        if (amplifier >= 0) {
            PotionEffect effect = new PotionEffect(PotionEffectType.SLOWNESS, potionDuration, amplifier, true, false, false);
            player.addPotionEffect(effect);
        } else {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }

        // Optional: nudge max health? Not needed; reset to default.
        var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            double baseHealth = attribute.getDefaultValue();
            if (player.getHealth() > baseHealth) {
                player.setHealth(baseHealth);
            }
        }
    }

    public String format(double value) {
        return String.format("%.1f", value);
    }
}
