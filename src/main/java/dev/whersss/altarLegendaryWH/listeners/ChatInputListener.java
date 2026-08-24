package dev.whersss.altarLegendaryWH.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.InventoryUtils;
import dev.whersss.altarLegendaryWH.commands.AltarLegendaryCommand;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class ChatInputListener implements Listener {

    private final AltarLegendaryWH plugin;
    public static final Map<UUID, String> awaitingWeapon = new ConcurrentHashMap<>();

    public ChatInputListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingWeapon.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String msg = event.getMessage().trim();
        String weaponType = awaitingWeapon.get(player.getUniqueId());

        if (msg.equalsIgnoreCase("отмена") || msg.equalsIgnoreCase("cancel")) {
            awaitingWeapon.remove(player.getUniqueId());
            player.sendMessage(TextUtils.legacy("§c" + plugin.tr("Выдача отменена.", "Issuing cancelled.")));
            return;
        }

        try {
            int kills = Integer.parseInt(msg);
            if (kills < 0) throw new NumberFormatException();
            awaitingWeapon.remove(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if ("bloodlust".equals(weaponType)) {
                    InventoryUtils.giveOrDrop(player, WeaponFactory.getBloodLust(kills));
                    player.sendMessage(TextUtils.legacy("§a" + plugin.tr("Вы получили §cЖажду крови §aс §c", "You received §cBloodlust §awith §c") + kills + " " + plugin.tr("§aубийствами.", "§akills.")));
                } else if ("knightfall".equals(weaponType)) {
                    InventoryUtils.giveOrDrop(player, WeaponFactory.getKnightfall(kills));
                    player.sendMessage(TextUtils.legacy("§a" + plugin.tr("Вы получили §fПадшего рыцаря §aс §c", "You received §fKnightfall §awith §c") + kills + " " + plugin.tr("§aубийствами.", "§akills.")));
                }

                AltarLegendaryCommand.openLegendaryMenu(player);
            });
        } catch (NumberFormatException ex) {
            player.sendMessage(TextUtils.legacy("§c" + plugin.tr("Ошибка! Введите целое число (например, 5) или напишите 'отмена'.", "Error! Enter a whole number (for example, 5) or type 'cancel'.")));
        }
    }
}
