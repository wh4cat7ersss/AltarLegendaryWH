package dev.whersss.altarLegendaryWH.commands;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FriendListTabCompleter implements TabCompleter {

    private final AltarLegendaryWH plugin;

    public FriendListTabCompleter(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player p)) return completions;

        if (args.length == 1) {
            completions.addAll(Arrays.asList("add", "remove", "list"));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(p)) {
                    completions.add(online.getName());
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(p) && !plugin.getFriendManager().isFriend(p.getUniqueId(), online.getUniqueId())) {
                        completions.add(online.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                for (UUID uuid : plugin.getFriendManager().getFriends(p.getUniqueId())) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    if (op.getName() != null) completions.add(op.getName());
                }
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}