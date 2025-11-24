package com.zack.backpackcost.command;

import com.zack.backpackcost.BackpackPlugin;
import com.zack.backpackcost.util.BackpackItemUtil;
import com.zack.backpackcost.util.BackpackTier;
import com.zack.backpackcost.util.WeightManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BackpackCommand implements CommandExecutor, TabCompleter {

    private final BackpackPlugin plugin;

    public BackpackCommand(BackpackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("BackpackNesting")) {
            if (!sender.hasPermission("backpack.reload")) {
                sender.sendMessage(color(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /" + label + " BackpackNesting <true|false>"));
                return true;
            }
            Boolean value = parseBoolean(args[1]);
            if (value == null) {
                sender.sendMessage(color(plugin.getConfig().getString("messages.invalid-number", "&cInvalid value.")));
                return true;
            }
            plugin.getConfig().set("backpack.allow-nesting", value);
            plugin.saveConfig();
            sender.sendMessage(color("&aBackpack nesting set to &e" + value));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("weight")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can run this.");
                return true;
            }
            Player player = (Player) sender;
            var held = player.getInventory().getItemInMainHand();
            if (!BackpackItemUtil.isBackpackItem(plugin, held)) {
                player.sendMessage(color("&cHold a backpack to check its weight."));
                return true;
            }
            BackpackTier tier = BackpackItemUtil.getTier(plugin, held);
            if (tier == null) tier = BackpackTier.LEATHER;
            double weight = plugin.getWeightManager().getBackpackWeight(player, held, tier);
            player.sendMessage(color("&eBackpack weight: &f" + plugin.getWeightManager().format(weight)));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("backpack.reload")) {
                sender.sendMessage(color(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(color(plugin.getConfig().getString("messages.reloaded")));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open backpacks.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("backpack.use")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        var held = player.getInventory().getItemInMainHand();
        if (!BackpackItemUtil.isBackpackItem(plugin, held)) {
            player.sendMessage(color("&cHold a backpack to open it."));
            return true;
        }

        BackpackTier tier = BackpackItemUtil.getTier(plugin, held);
        if (tier == null) tier = BackpackTier.LEATHER;
        var id = BackpackItemUtil.getOrCreateId(plugin, held);

        plugin.getBackpackManager().openBackpack(player, id, tier);
        sendWeightMessage(player, tier);
        return true;
    }

    private void sendWeightMessage(Player player, BackpackTier tier) {
        WeightManager weights = plugin.getWeightManager();
        double current = weights.getTotalWeight(player);
        double threshold = weights.getMaxThreshold();
        String raw = plugin.getConfig().getString("messages.opened", "Backpack opened.");
        raw = raw.replace("%weight%", weights.format(current)).replace("%threshold%", weights.format(threshold));
        player.sendMessage(color(raw));
    }

    private Boolean parseBoolean(String input) {
        if (input == null) return null;
        String v = input.toLowerCase();
        if (v.equals("true") || v.equals("yes") || v.equals("on")) return true;
        if (v.equals("false") || v.equals("no") || v.equals("off")) return false;
        return null;
    }

    private String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("backpack.reload")) {
                completions.add("reload");
                completions.add("BackpackNesting");
            }
            completions.add("weight");
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("BackpackNesting") && sender.hasPermission("backpack.reload")) {
            completions.add("true");
            completions.add("false");
        }
        return completions.stream()
                .filter(name -> name.toLowerCase().startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
                .collect(Collectors.toList());
    }
}
