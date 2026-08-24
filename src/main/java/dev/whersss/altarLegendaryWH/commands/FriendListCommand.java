package dev.whersss.altarLegendaryWH.commands;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.managers.FriendManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class FriendListCommand implements CommandExecutor {

    private final AltarLegendaryWH plugin;

    public FriendListCommand(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(TextUtils.legacy("Только для игроков."));
            return true;
        }

        FriendManager fm = plugin.getFriendManager();

        if (args.length == 0) {
            p.sendMessage(TextUtils.legacy("§eПомощь по друзьям:"));
            p.sendMessage(TextUtils.legacy("§f/friendlist add <ник> §7- Добавить друга (макс 2)"));
            p.sendMessage(TextUtils.legacy("§f/friendlist remove <ник> §7- Удалить друга"));
            p.sendMessage(TextUtils.legacy("§f/friendlist list §7- Список друзей"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length < 2) {
                    p.sendMessage(TextUtils.legacy("§cУкажите ник игрока."));
                    return true;
                }
                Player targetAdd = Bukkit.getPlayer(args[1]);
                if (targetAdd == null) {
                    p.sendMessage(TextUtils.legacy("§cИгрок не найден (он должен быть онлайн)."));
                    return true;
                }
                if (targetAdd.equals(p)) {
                    p.sendMessage(TextUtils.legacy("§cВы не можете добавить самого себя."));
                    return true;
                }

                if (fm.isFriend(p.getUniqueId(), targetAdd.getUniqueId())) {
                    p.sendMessage(TextUtils.legacy("§cЭтот игрок уже в вашем френдлисте."));
                    return true;
                }

                if (fm.addFriend(p.getUniqueId(), targetAdd.getUniqueId())) {
                    p.sendMessage(TextUtils.legacy("§aВы добавили §e" + targetAdd.getName() + " §aв френдлист! Легендарные оружия его больше не заденут."));
                } else {
                    p.sendMessage(TextUtils.legacy("§cУ вас уже максимум друзей (2/2). Удалите кого-то сначала."));
                }
                break;

            case "remove":
                if (args.length < 2) {
                    p.sendMessage(TextUtils.legacy("§cУкажите ник игрока."));
                    return true;
                }
                OfflinePlayer targetRemove = Bukkit.getOfflinePlayer(args[1]);
                if (!fm.isFriend(p.getUniqueId(), targetRemove.getUniqueId())) {
                    p.sendMessage(TextUtils.legacy("§cЭтого игрока нет в вашем френдлисте."));
                    return true;
                }
                fm.removeFriend(p.getUniqueId(), targetRemove.getUniqueId());
                p.sendMessage(TextUtils.legacy("§aИгрок §e" + targetRemove.getName() + " §aудален из френдлиста."));
                break;

            case "list":
                Set<UUID> friends = fm.getFriends(p.getUniqueId());
                if (friends.isEmpty()) {
                    p.sendMessage(TextUtils.legacy("§cВаш френдлист пуст."));
                    return true;
                }
                p.sendMessage(TextUtils.legacy("§aВаши друзья:"));
                for (UUID uuid : friends) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    p.sendMessage(TextUtils.legacy("§f- §e" + op.getName()));
                }
                break;

            default:
                p.sendMessage(TextUtils.legacy("§cНеизвестный аргумент. Пишите /friendlist"));
                break;
        }
        return true;
    }
}