package dev.whersss.altarLegendaryWH.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.InventoryUtils;
import dev.whersss.altarLegendaryWH.commands.AltarLegendaryCommand;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import dev.whersss.altarLegendaryWH.weapons.bloodlust.listeners.BloodLustListener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    private final AltarLegendaryWH plugin;

    public MenuListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String expectedTitle = plugin.tr("Легендарные оружия", "Legendary weapons");

        if (!plainTitle.equalsIgnoreCase(expectedTitle) || event.getInventory().getSize() != 27) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.WHITE_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (isBloodlustMenuItem(item)) {
            player.closeInventory();
            ChatInputListener.awaitingWeapon.put(player.getUniqueId(), "bloodlust");
            sendPrompt(player);
            return;
        }

        if (isKnightfallMenuItem(item)) {
            player.closeInventory();
            ChatInputListener.awaitingWeapon.put(player.getUniqueId(), "knightfall");
            sendPrompt(player);
            return;
        }

        ItemStack itemToGive = item.clone();
        itemToGive.setAmount(1);
        InventoryUtils.giveOrDrop(player, itemToGive);

        String plainName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                : plugin.tr("предмет", "item");
        player.sendMessage(TextUtils.legacy("§2" + plugin.tr("Вам было выдано §6", "You received §6") + plainName));
    }

    private boolean isKnightfallMenuItem(ItemStack item) {
        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(plugin.getKillsKey(), PersistentDataType.INTEGER);
    }

    private boolean isBloodlustMenuItem(ItemStack item) {
        return BloodLustListener.isBloodLust(item);
    }

    private void sendPrompt(Player player) {
        player.sendMessage(TextUtils.legacy(""));
        player.sendMessage(TextUtils.legacy("§6§l[!] §f" + plugin.tr("Введите в чат количество §cубийств §fдля оружия.", "Enter the number of §ckills §ffor the weapon in chat.")));
        player.sendMessage(TextUtils.legacy("§7" + plugin.tr("Напишите §nотмена§7 для выхода.", "Type §ncancel§7 to exit.")));
    }
}
