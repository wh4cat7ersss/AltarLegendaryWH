package dev.whersss.altarLegendaryWH.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AltarLegendaryTabCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "show", "reload", "cooldownreset"));
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("show")) {
            completions.addAll(Arrays.asList("weapons", "items"));
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(Arrays.asList(
                    "boneblade", "bloodlust", "nightpiercer", "vulcan", "frost_scythe",
                    "pale_gun", "pure_blade", "knightfall", "shadow_blade", "windweaver", "hyperion",
                    "wither_blade", "earth_gauntlet", "cutlass",
                    "warden_heart", "vulcan_skull", "weapons_handle", "illusion_core",
                    "copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots",
                    "copper_pickaxe"
            ));
        }
        else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("cooldownreset")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            String w = args[1].toLowerCase();
            if (w.equals("bloodlust") || w.equals("knightfall")) {
                completions.addAll(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}
