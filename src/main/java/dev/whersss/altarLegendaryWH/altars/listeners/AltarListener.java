package dev.whersss.altarLegendaryWH.altars.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.altars.Altar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AltarListener implements Listener {

    private final AltarLegendaryWH plugin;

    public AltarListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (event.getBlockAgainst().getType() == Material.BARRIER) {
            Altar altar = plugin.getAltarManager().getAltarAt(event.getBlockAgainst().getLocation());
            if (altar != null) {
                event.setCancelled(true);
                return;
            }
        }

        if (item.getType() != Material.STRUCTURE_BLOCK || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getAltarManager().getAltarKey(), PersistentDataType.BYTE)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            event.getBlockPlaced().setType(Material.BARRIER, false);
            Altar altar = plugin.getAltarManager().createAltar(event.getBlockPlaced().getLocation());
            altar.spawnEntities();
            event.getPlayer().sendMessage(plugin.getAltarLanguageManager().component(
                    "altar.placed", "id", String.valueOf(altar.getId())));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARRIER) return;

        Altar altar = plugin.getAltarManager().getAltarAt(block.getLocation());
        if (altar == null || event.getHand() == EquipmentSlot.OFF_HAND) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        Player player = event.getPlayer();
        if ((player.isOp() || player.hasPermission("altarlegendary.admin")) && player.isSneaking()) {
            String uses = altar.getMaxUses() == -1
                    ? plugin.getAltarLanguageManager().plain("altar.infinite_uses")
                    : altar.getCurrentUses() + "/" + altar.getMaxUses();
            player.sendMessage(plugin.getAltarLanguageManager().component("altar.admin_info",
                    "id", String.valueOf(altar.getId()), "uses", uses));
            return;
        }

        if (altar.getCooldownRemaining() > 0) {
            player.sendMessage(plugin.getAltarLanguageManager().component("altar.still_on_cooldown"));
            return;
        }

        if (altar.isCrafted() && altar.getMaxUses() != -1 && altar.getCurrentUses() <= 0) return;
        if (altar.getResultItem() == null) return;
        processCraft(player, altar);
    }

    private void processCraft(Player player, Altar altar) {
        List<ItemStack> recipe = altar.getRecipe();
        if (recipe.isEmpty()) return;

        List<Component> missingLines = new ArrayList<>();

        for (ItemStack required : recipe) {
            int needed = required.getAmount();
            int available = 0;

            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && matchesRequirement(invItem, required)) {
                    available += invItem.getAmount();
                }
            }

            if (available < needed) {
                missingLines.add(plugin.getAltarLanguageManager().component("craft.missing_prefix")
                        .append(Altar.getItemDisplayName(required))
                        .append(plugin.getAltarLanguageManager().component("craft.missing_suffix",
                                "amount", String.valueOf(needed - available))));
            }
        }

        if (!missingLines.isEmpty()) {
            player.sendMessage(plugin.getAltarLanguageManager().component("craft.missing_header"));
            for (Component line : missingLines) player.sendMessage(line);
            return;
        }

        for (ItemStack required : recipe) {
            int toRemove = required.getAmount();
            ItemStack[] contents = player.getInventory().getContents();
            for (int slot = 0; slot < contents.length; slot++) {
                if (contents[slot] == null || !matchesRequirement(contents[slot], required)) continue;

                int amount = contents[slot].getAmount();
                if (amount > toRemove) {
                    contents[slot].setAmount(amount - toRemove);
                    break;
                }

                toRemove -= amount;
                contents[slot] = null;
            }
            player.getInventory().setContents(contents);
        }

        ItemStack craftedResult = altar.getResultItem().clone();
        Location location = altar.getLocation().clone().add(0.5, 1.0, 0.5);
        location.getWorld().strikeLightningEffect(location);

        altar.setLastUsedTime(System.currentTimeMillis());
        if (altar.getMaxUses() != -1) {
            altar.setCurrentUses(altar.getCurrentUses() - 1);
            if (altar.getCurrentUses() <= 0) {
                altar.setCrafted(true);
                altar.removeHolograms();
            }
        }

        if (altar.getCurrentUses() > 0 || altar.getMaxUses() == -1) altar.spawnEntities();
        plugin.getAltarManager().saveAltars();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(craftedResult);
            if (!leftover.isEmpty()) {
                Location dropLocation = location.clone().add(0, 1.5, 0);
                for (ItemStack dropItem : leftover.values()) {
                    Item drop = dropLocation.getWorld().dropItem(dropLocation, dropItem);
                    drop.setPickupDelay(0);
                    drop.setVelocity(new org.bukkit.util.Vector(0, 0.1, 0));
                }
            }

            Component playerName = MiniMessage.miniMessage()
                    .deserialize("<gradient:#FFAA00:#FFFFFF:#FFAA00>" + player.getName() + "</gradient>")
                    .decoration(TextDecoration.ITALIC, false);

            Component itemHoverComp = Component.text("[")
                    .append(Altar.getItemDisplayName(craftedResult))
                    .append(Component.text("]"))
                    .color(NamedTextColor.GOLD)
                    .hoverEvent(craftedResult.asHoverEvent());

            Component broadcastMessage = playerName
                    .append(plugin.getAltarLanguageManager().component("craft.broadcast_prefix"))
                    .append(itemHoverComp)
                    .append(plugin.getAltarLanguageManager().component("craft.broadcast_suffix"));

            Bukkit.broadcast(broadcastMessage);

            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "adminshop addcraft " + player.getName());
            } catch (Throwable ignored) {
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(onlinePlayer.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }, 20L);
    }

    private boolean matchesRequirement(ItemStack item, ItemStack requirement) {
        if (requirement.getType() == Material.PLAYER_HEAD && item.getType() == Material.PLAYER_HEAD) {
            SkullMeta reqMeta = (SkullMeta) requirement.getItemMeta();
            SkullMeta itemMeta = (SkullMeta) item.getItemMeta();

            if (reqMeta != null && reqMeta.hasOwner()) {
                if (itemMeta != null && itemMeta.hasOwner()) {
                    OfflinePlayer reqOwner = reqMeta.getOwningPlayer();
                    OfflinePlayer itemOwner = itemMeta.getOwningPlayer();

                    return reqOwner != null && itemOwner != null &&
                            reqOwner.getUniqueId().equals(itemOwner.getUniqueId());
                }
                return false;
            }
            return true;
        }
        return item.isSimilar(requirement);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        boolean enabled = plugin.getConfig().getBoolean("player-heads.enabled",
                plugin.getConfig().getBoolean("player_heads.enabled",
                        plugin.getAltarsConfig().getBoolean("player_heads.enabled", true)));
        if (!enabled) return;

        Player victim = event.getEntity();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(victim);

            String headName = plugin.getAltarLanguageManager().plain("head.name", "player", victim.getName());
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(headName));

            Component deathComponent = event.deathMessage();
            String deathCause = deathComponent != null ?
                    PlainTextComponentSerializer.plainText().serialize(deathComponent) :
                    "Умер";

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getAltarLanguageManager().component("head.lore_cause", "cause", deathCause));
            meta.lore(lore);

            head.setItemMeta(meta);
        }

        event.getDrops().add(head);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BARRIER) return;

        Altar altar = plugin.getAltarManager().getAltarAt(block.getLocation());
        if (altar == null) return;

        altar.removeAllEntities();
        plugin.getAltarManager().removeAltar(altar.getId());
        event.getPlayer().sendMessage(plugin.getAltarLanguageManager().component(
                "altar.removed", "id", String.valueOf(altar.getId())));
    }
}
