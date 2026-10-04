package dev.whersss.altarLegendaryWH.altars.commands;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.altars.Altar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EditAltarCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "give", "set", "remove", "save", "load", "delete", "create",
            "reload", "restart", "teleport", "teleporthere");

    private final AltarLegendaryWH plugin;

    public EditAltarCommand(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("altarlegendary.admin") || sender.hasPermission("altar.admin");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        return executeSubcommand(sender, args);
    }

    public boolean executeSubcommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!hasAdminPermission(sender)) {
            send(sender, "command.no_permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            plugin.reloadAltarsConfig();
            plugin.getAltarLanguageManager().reload();
            for (Integer id : plugin.getAltarManager().getAltarsIds()) {
                Altar altar = plugin.getAltarManager().getAltar(id);
                if (altar != null) altar.spawnEntities();
            }
            send(sender, "command.reload_success");
            return true;
        }

        if (subCommand.equals("restart")) {
            plugin.getAltarManager().cleanupEntities();
            for (Integer id : plugin.getAltarManager().getAltarsIds()) {
                Altar altar = plugin.getAltarManager().getAltar(id);
                if (altar != null) altar.spawnEntities();
            }
            send(sender, "command.restart_success");
            return true;
        }

        if (subCommand.equals("give")) {
            if (args.length < 2) {
                send(sender, "command.give_usage");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                send(sender, "command.player_not_found", "player", args[1]);
                return true;
            }

            ItemStack altarItem = plugin.getAltarManager().createAltarItem();
            target.getInventory().addItem(altarItem);
            send(sender, "command.give_success", "player", target.getName());
            return true;
        }

        if (!(sender instanceof Player player)) {
            send(sender, "command.player_only");
            return true;
        }

        if (subCommand.equals("teleport") || subCommand.equals("tp")) {
            if (args.length < 2) {
                send(player, "command.teleport_usage");
                return true;
            }
            int id = tryParse(args[1]);
            Altar altar = getAltar(player, id);
            if (altar == null) return true;

            player.teleport(altar.getLocation().clone().add(0.5, 1, 0.5));
            send(player, "command.teleport_success", "id", String.valueOf(id));
            return true;
        }

        if (subCommand.equals("teleporthere") || subCommand.equals("tphere")) {
            if (args.length < 2) {
                send(player, "command.teleporthere_usage");
                return true;
            }
            int id = tryParse(args[1]);
            Altar altar = getAltar(player, id);
            if (altar == null) return true;

            Location newLocation = player.getLocation().getBlock().getLocation();
            if (plugin.getAltarManager().getAltarAt(newLocation) != null) {
                send(player, "error.altar_already_here");
                return true;
            }

            altar.getLocation().getBlock().setType(Material.AIR);
            altar.removeAllEntities();
            altar.setLocation(newLocation);
            newLocation.getBlock().setType(Material.BARRIER);
            altar.spawnEntities();
            plugin.getAltarManager().saveAltars();

            send(player, "command.teleporthere_success", "id", String.valueOf(id));
            return true;
        }

        if (subCommand.equals("create")) {
            Location location = player.getLocation().getBlock().getLocation();
            if (plugin.getAltarManager().getAltarAt(location) != null) {
                send(player, "error.altar_already_here");
                return true;
            }

            location.getBlock().setType(Material.BARRIER);
            Altar altar = plugin.getAltarManager().createAltar(location);
            altar.spawnEntities();
            send(player, "command.create_success", "id", String.valueOf(altar.getId()));
            return true;
        }

        if (subCommand.equals("delete")) {
            handleDelete(player, args);
            return true;
        }

        if (subCommand.equals("remove")) {
            handleRemove(player, args);
            return true;
        }

        if (subCommand.equals("set")) {
            handleSet(player, args);
            return true;
        }

        if (subCommand.equals("save")) {
            handleSave(player, args);
            return true;
        }

        if (subCommand.equals("load")) {
            handleLoad(player, args);
            return true;
        }

        send(sender, "command.unknown");
        return true;
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            send(player, "command.delete_usage");
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            List<Integer> ids = new ArrayList<>(plugin.getAltarManager().getAltarsIds());
            if (ids.isEmpty()) {
                send(player, "command.delete_none");
                return;
            }
            for (int id : ids) {
                Altar altar = plugin.getAltarManager().getAltar(id);
                if (altar != null) {
                    altar.getLocation().getBlock().setType(Material.AIR);
                    plugin.getAltarManager().removeAltar(id);
                }
            }
            send(player, "command.delete_all_success", "amount", String.valueOf(ids.size()));
            return;
        }

        int id = tryParse(args[1]);
        Altar altar = getAltar(player, id);
        if (altar == null) return;

        altar.getLocation().getBlock().setType(Material.AIR);
        plugin.getAltarManager().removeAltar(id);
        send(player, "command.delete_success", "id", String.valueOf(id));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            send(player, "command.remove_usage");
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("item")) {
            if (args.length < 3) {
                send(player, "command.remove_item_usage");
                return;
            }

            int id = tryParse(args[2]);
            Altar altar = getAltar(player, id);
            if (altar == null) return;

            altar.setResultItem(null);
            altar.setRecipe(new ArrayList<>());
            altar.setCrafted(false);
            altar.spawnEntities();
            plugin.getAltarManager().saveAltars();
            send(player, "command.remove_item_success", "id", String.valueOf(id));
            return;
        }

        if (action.equals("recipe")) {
            if (args.length < 4) {
                send(player, "command.remove_recipe_usage");
                return;
            }

            int id = tryParse(args[2]);
            Altar altar = getAltar(player, id);
            if (altar == null) return;

            int recipeSize = altar.getRecipe().size();
            if (recipeSize == 0) {
                send(player, "error.recipe_empty", "id", String.valueOf(id));
                return;
            }

            int position = tryParse(args[3]);
            if (position < 1 || position > recipeSize) {
                send(player, "error.recipe_position", "max", String.valueOf(recipeSize));
                return;
            }

            altar.getRecipe().remove(position - 1);
            altar.spawnEntities();
            plugin.getAltarManager().saveAltars();
            send(player, "command.remove_recipe_success",
                    "position", String.valueOf(position), "id", String.valueOf(id));
            return;
        }

        send(player, "command.remove_usage");
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 3) {
            send(player, "command.set_usage");
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("use")) {
            if (args.length < 4) {
                send(player, "command.set_use_usage");
                return;
            }
            int id = tryParse(args[2]);
            Altar altar = getAltar(player, id);
            if (altar == null) return;

            if (args[3].equalsIgnoreCase("inf")) {
                altar.setMaxUses(-1);
                send(player, "command.set_use_infinite", "id", String.valueOf(id));
            } else {
                int uses = tryParse(args[3]);
                if (uses <= 0) {
                    send(player, "error.positive_integer");
                    return;
                }
                altar.setMaxUses(uses);
                send(player, "command.set_use_success",
                        "id", String.valueOf(id), "amount", String.valueOf(uses));
            }
            altar.spawnEntities();
            plugin.getAltarManager().saveAltars();
            return;
        }

        if (action.equals("cooldown")) {
            if (args.length < 4) {
                send(player, "command.set_cooldown_usage");
                return;
            }
            int id = tryParse(args[2]);
            Altar altar = getAltar(player, id);
            if (altar == null) return;

            int cooldown = tryParse(args[3]);
            if (cooldown < 0) {
                send(player, "error.non_negative_integer");
                return;
            }
            altar.setCooldownSeconds(cooldown);
            plugin.getAltarManager().saveAltars();
            send(player, "command.set_cooldown_success",
                    "id", String.valueOf(id), "seconds", String.valueOf(cooldown));
            return;
        }

        if (action.equals("item") || action.equals("recipe")) {
            if (args.length >= 5 && action.equals("recipe") && args[3].equalsIgnoreCase("head")) {
                int id = tryParse(args[2]);
                Altar altar = getAltar(player, id);
                if (altar == null) return;

                int amount = tryParse(args[4]);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD, amount <= 0 ? 1 : amount);
                SkullMeta meta = (SkullMeta) head.getItemMeta();

                if (meta != null) {
                    if (args.length >= 6) {
                        String playerName = args[5];
                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
                        meta.setOwningPlayer(targetPlayer);
                        meta.displayName(Component.text(playerName + "'s Head"));
                    } else {
                        meta.displayName(Component.text("Player's Head"));
                    }
                    head.setItemMeta(meta);
                }

                altar.addRecipeItem(head);
                altar.spawnEntities();
                plugin.getAltarManager().saveAltars();
                send(player, "command.set_recipe_success",
                        "position", String.valueOf(altar.getRecipe().size()), "id", String.valueOf(id));
                return;
            }

            int id = tryParse(args[2]);
            Altar altar = getAltar(player, id);
            if (altar == null) return;

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                send(player, "error.empty_hand");
                return;
            }

            if (action.equals("item")) {
                ItemStack result = hand.clone();
                result.setAmount(1);
                altar.setResultItem(result);
                altar.getRecipe().clear();
                altar.setCrafted(false);
                altar.spawnEntities();
                plugin.getAltarManager().saveAltars();
                send(player, "command.set_item_success", "id", String.valueOf(id));
            } else {
                altar.addRecipeItem(hand.clone());
                altar.spawnEntities();
                plugin.getAltarManager().saveAltars();
                send(player, "command.set_recipe_success",
                        "position", String.valueOf(altar.getRecipe().size()), "id", String.valueOf(id));
            }
            return;
        }

        if (action.equals("scale")) {
            handleDisplaySetting(player, args);
            return;
        }

        send(player, "command.set_usage");
    }

    private void handleDisplaySetting(Player player, String[] args) {
        if (args.length < 5) {
            send(player, "command.set_display_usage", "action", "scale");
            return;
        }

        String type = args[2].toLowerCase();
        if (!List.of("title", "recipe", "item").contains(type)) {
            send(player, "command.set_display_invalid_type");
            return;
        }

        double value;
        try {
            value = Double.parseDouble(args[4]);
        } catch (NumberFormatException exception) {
            send(player, "error.number");
            return;
        }

        String target = args[3].toLowerCase();
        String localizedType = plugin.getAltarLanguageManager().plain("display_type." + type);

        if (target.equals("all") || target.equals("def")) {
            setDisplayConfigDefault(type, value);
            send(player, "command.set_default_scale_success",
                    "type", localizedType, "value", String.valueOf(value));

            if (target.equals("all")) {
                for (Integer id : plugin.getAltarManager().getAltarsIds()) {
                    Altar a = plugin.getAltarManager().getAltar(id);
                    if (a != null) {
                        a.setCustomScale(type, "x", (float) value);
                        a.setCustomScale(type, "y", (float) value);
                        a.setCustomScale(type, "z", (float) value);
                    }
                }
                plugin.getAltarManager().saveAltars();
                send(player, "command.set_applied_all");
            }
        } else {
            int id = tryParse(target);
            Altar altar = plugin.getAltarManager().getAltar(id);
            if (altar == null) {
                send(player, "error.altar_id_not_found", "id", String.valueOf(id));
                return;
            }

            altar.setCustomScale(type, "x", (float) value);
            altar.setCustomScale(type, "y", (float) value);
            altar.setCustomScale(type, "z", (float) value);
            plugin.getAltarManager().saveAltars();

            send(player, "command.set_scale_success",
                    "type", localizedType, "id", String.valueOf(id), "value", String.valueOf(value));
        }

        for (Integer id : plugin.getAltarManager().getAltarsIds()) {
            Altar altar = plugin.getAltarManager().getAltar(id);
            if (altar != null) altar.spawnEntities();
        }
    }

    private void setDisplayConfigDefault(String type, double value) {
        String path = "settings.display." + type;
        plugin.getAltarsConfig().set(path + ".scale.x", value);
        plugin.getAltarsConfig().set(path + ".scale.y", value);
        plugin.getAltarsConfig().set(path + ".scale.z", value);
        plugin.saveAltarsConfig();
    }

    private void handleSave(Player player, String[] args) {
        if (args.length < 3) {
            send(player, "command.save_usage");
            return;
        }

        int id = tryParse(args[1]);
        String name = args[2].toLowerCase();
        Altar altar = getAltar(player, id);
        if (altar == null) return;

        String basePath = "templates." + name;
        plugin.getAltarManager().getDataConfig().set(basePath + ".result", altar.getResultItem());
        plugin.getAltarManager().getDataConfig().set(basePath + ".recipe", altar.getRecipe());

        for (String type : List.of("title", "recipe", "item")) {
            double defaultY = type.equals("title") ? 3.2 : (type.equals("recipe") ? 3.5 : 1.5);
            double yOffset = altar.getOffset(type, "y", defaultY);
            plugin.getAltarManager().getDataConfig().set(basePath + ".display." + type + ".offset.y", yOffset);

            float scale = altar.getScale(type, "x", 1.0f);
            plugin.getAltarManager().getDataConfig().set(basePath + ".display." + type + ".scale", scale);
        }
        plugin.getAltarManager().saveDataConfig();
        send(player, "command.save_success", "id", String.valueOf(id), "name", name);
    }

    @SuppressWarnings("unchecked")
    private void handleLoad(Player player, String[] args) {
        if (args.length < 2) {
            send(player, "command.load_usage");
            return;
        }

        String name = args[1].toLowerCase();
        String basePath = "templates." + name;
        if (!plugin.getAltarManager().getDataConfig().contains(basePath)) {
            send(player, "template_not_found", "name", name);
            return;
        }

        Location location = player.getLocation().getBlock().getLocation();
        if (plugin.getAltarManager().getAltarAt(location) != null) {
            send(player, "error.altar_already_here");
            return;
        }
        location.getBlock().setType(Material.BARRIER);

        Altar altar = plugin.getAltarManager().createAltar(location);
        int newId = altar.getId();
        altar.setResultItem(plugin.getAltarManager().getDataConfig().getItemStack(basePath + ".result"));
        altar.setRecipe((List<ItemStack>) plugin.getAltarManager().getDataConfig().getList(basePath + ".recipe"));

        for (String type : List.of("title", "recipe", "item")) {
            if (plugin.getAltarManager().getDataConfig().contains(basePath + ".display." + type + ".offset.y")) {
                double offset = plugin.getAltarManager().getDataConfig().getDouble(basePath + ".display." + type + ".offset.y");
                altar.setCustomOffset(type, "y", offset);
            }
            if (plugin.getAltarManager().getDataConfig().contains(basePath + ".display." + type + ".scale")) {
                float scale = (float) plugin.getAltarManager().getDataConfig().getDouble(basePath + ".display." + type + ".scale");
                altar.setCustomScale(type, "x", scale);
                altar.setCustomScale(type, "y", scale);
                altar.setCustomScale(type, "z", scale);
            }
        }
        altar.spawnEntities();
        plugin.getAltarManager().saveAltars();
        send(player, "command.load_success", "name", name, "id", String.valueOf(newId));
    }

    private void showHelp(CommandSender sender) {
        for (Component line : plugin.getAltarLanguageManager().components("help.lines")) {
            sender.sendMessage(line);
        }
    }

    private Altar getAltar(CommandSender sender, int id) {
        Altar altar = plugin.getAltarManager().getAltar(id);
        if (altar == null) send(sender, "error.altar_not_found");
        return altar;
    }

    private void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(plugin.getAltarLanguageManager().component(key, replacements));
    }

    private int tryParse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        return completeSubcommand(sender, args);
    }

    public @NotNull List<String> completeSubcommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!hasAdminPermission(sender)) return List.of();

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(SUBCOMMANDS);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            } else if (subCommand.equals("set")) {
                completions.addAll(List.of("item", "recipe", "scale", "use", "cooldown"));
            } else if (subCommand.equals("remove")) {
                completions.addAll(List.of("recipe", "item"));
            } else if (List.of("save", "teleport", "teleporthere", "tp", "tphere").contains(subCommand)) {
                completions.addAll(altarIds());
            } else if (subCommand.equals("delete")) {
                completions.add("all");
                completions.addAll(altarIds());
            } else if (subCommand.equals("load") && plugin.getAltarManager().getDataConfig().getConfigurationSection("templates") != null) {
                completions.addAll(plugin.getAltarManager().getDataConfig().getConfigurationSection("templates").getKeys(false));
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            if (subCommand.equals("set")) {
                if (List.of("item", "recipe", "use", "cooldown").contains(action)) {
                    completions.addAll(altarIds());
                } else if (action.equals("scale")) {
                    completions.addAll(List.of("title", "recipe", "item"));
                }
            } else if (subCommand.equals("remove") && List.of("recipe", "item").contains(action)) {
                completions.addAll(altarIds());
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            if (subCommand.equals("set")) {
                if (action.equals("scale")) {
                    completions.addAll(List.of("all", "def"));
                    completions.addAll(altarIds());
                } else if (action.equals("use")) {
                    completions.addAll(List.of("inf", "1", "5", "10"));
                } else if (action.equals("cooldown")) {
                    completions.addAll(List.of("0", "10", "30", "60"));
                } else if (action.equals("recipe")) {
                    completions.add("head");
                }
            } else if (subCommand.equals("remove") && action.equals("recipe")) {
                Altar altar = plugin.getAltarManager().getAltar(tryParse(args[2]));
                if (altar != null) {
                    completions.addAll(IntStream.rangeClosed(1, altar.getRecipe().size())
                            .mapToObj(String::valueOf).toList());
                }
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("recipe") && args[3].equalsIgnoreCase("head")) {
                completions.addAll(List.of("1", "2", "4", "8", "16", "32", "64"));
            } else if (args[0].equalsIgnoreCase("set") && (args[1].equalsIgnoreCase("scale"))) {
                completions.addAll(List.of("0.5", "1.0", "1.5", "2.0", "2.4", "3.0", "3.2", "3.5"));
            }
        } else if (args.length == 6) {
            if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("recipe") && args[3].equalsIgnoreCase("head")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }
        }

        String lastWord = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(value -> value.toLowerCase().startsWith(lastWord))
                .collect(Collectors.toList());
    }

    private List<String> altarIds() {
        return plugin.getAltarManager().getAltarsIds().stream()
                .sorted()
                .map(String::valueOf)
                .toList();
    }
}
