package dev.whersss.altarLegendaryWH.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.items.copperarmor.utils.CopperArmorFactory;
import dev.whersss.altarLegendaryWH.items.copperpickaxe.CopperPickaxeItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemProtectionListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Long> weaponLimitTimers = new HashMap<>();

    public ItemProtectionListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        startInventoryCheckTask();
    }

    private void startInventoryCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isLimitsEnabled()) {
                    weaponLimitTimers.clear();
                    return;
                }

                int maxWeapons = plugin.getConfig().getInt("limits.max-legendary-weapons", 4);
                int maxArmor = plugin.getConfig().getInt("limits.max-copper-armor", 2);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerInventory inventory = player.getInventory();
                    UUID uuid = player.getUniqueId();

                    ItemStack[] armorContents = inventory.getArmorContents();
                    int equippedArmorCount = 0;

                    for (int i = 0; i < armorContents.length; i++) {
                        ItemStack armorPiece = armorContents[i];
                        if (!isCopperArmor(armorPiece)) continue;

                        equippedArmorCount++;
                        if (equippedArmorCount > maxArmor) {
                            armorContents[i] = null;
                            inventory.setArmorContents(armorContents);

                            Map<Integer, ItemStack> leftovers = inventory.addItem(armorPiece);
                            for (ItemStack leftover : leftovers.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                            }

                            player.sendMessage(TextUtils.legacy("§c§l[!] §f" + plugin.tr(
                                    "Вы не можете носить на себе больше " + maxArmor + " элементов медной брони одновременно! Лишнее было снято.",
                                    "You cannot wear more than " + maxArmor + " copper armor pieces at the same time. Extra armor was removed."
                            )));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        }
                    }

                    int weaponCount = countLegendaryWeapons(inventory);
                    if (weaponCount > maxWeapons) {
                        if (!weaponLimitTimers.containsKey(uuid)) {
                            weaponLimitTimers.put(uuid, System.currentTimeMillis() + 30000L);
                            player.sendMessage(TextUtils.legacy("§c§l[!] §f" + plugin.tr(
                                    "Внимание! У вас слишком много легендарного оружия (" + weaponCount + "/" + maxWeapons + ").",
                                    "Warning! You have too many legendary weapons (" + weaponCount + "/" + maxWeapons + ")."
                            )));
                            player.sendMessage(TextUtils.legacy("§c" + plugin.tr(
                                    "У вас есть 30 секунд, чтобы убрать лишнее, иначе оно будет выброшено!",
                                    "You have 30 seconds to remove the extra items, or they will be dropped!"
                            )));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        } else if (System.currentTimeMillis() >= weaponLimitTimers.get(uuid)) {
                            int kept = 0;
                            for (int i = 0; i < inventory.getSize(); i++) {
                                ItemStack item = inventory.getItem(i);
                                if (!isLegendaryWeapon(item)) continue;

                                kept++;
                                if (kept > maxWeapons) {
                                    inventory.setItem(i, null);
                                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                                }
                            }

                            player.sendMessage(TextUtils.legacy("§4§l[!] §c" + plugin.tr(
                                    "Время вышло! Лишнее легендарное оружие было выброшено из инвентаря.",
                                    "Time is up! Extra legendary weapons were dropped from your inventory."
                            )));
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                            weaponLimitTimers.remove(uuid);
                        }
                    } else if (weaponLimitTimers.containsKey(uuid)) {
                        weaponLimitTimers.remove(uuid);
                        player.sendMessage(TextUtils.legacy("§a§l[!] §f" + plugin.tr(
                                "Количество легендарного оружия снова в норме. Спасибо!",
                                "Your legendary weapon count is back within the limit. Thank you!"
                        )));
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    @EventHandler
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item droppedItem)) return;

        ItemStack item = droppedItem.getItemStack();
        if (!isLegendary(item)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.CONTACT
                || cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setCancelled(true);
            droppedItem.setFireTicks(0);
        }
    }

    private int countLegendaryWeapons(PlayerInventory inventory) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (isLegendaryWeapon(item)) {
                count++;
            }
        }
        return count;
    }

    private boolean isCopperArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(CopperArmorFactory.getArmorKey(), PersistentDataType.STRING);
    }

    private boolean isLegendaryWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        if (meta.getPersistentDataContainer().has(plugin.getKillsKey(), PersistentDataType.INTEGER)) return true;
        if (meta.getPersistentDataContainer().has(plugin.getVulcanKey(), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(plugin.getPaleGunKey(), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(plugin.getWitherKey(), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(plugin.getHyperionKey(), PersistentDataType.BYTE)) return true;
        if (plugin.getPaladinsBattleAxeKey() != null
                && meta.getPersistentDataContainer().has(plugin.getPaladinsBattleAxeKey(), PersistentDataType.BYTE)) {
            return true;
        }
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "bone_blade"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "bloodlust"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "nightpiercer"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "pure_blade"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shadow_blade"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "windweaver"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "earth_gauntlet"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "cutlass"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "frost_scythe"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "crazyslots"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "knightfall"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(plugin.getKnightfallTierKey(), PersistentDataType.INTEGER)) return true;

        if (plugin.getCrazySlotsManager() != null && plugin.getCrazySlotsManager().isCrazySlots(item)) {
            return true;
        }
        if (plugin.getCrazySlotsManager() != null && plugin.getCrazySlotsManager().isTransformedItem(item)) {
            return true;
        }

        if (meta.hasCustomModelData()) {
            int customModelData = meta.getCustomModelData();
            if (customModelData >= 3000 && customModelData <= 3009) return true;
            Material type = item.getType();

            if (type == Material.NETHERITE_SWORD) {
                return customModelData == 1
                        || customModelData == 5
                        || customModelData == 7
                        || customModelData == 9
                        || customModelData == 10
                        || customModelData == 6
                        || customModelData == 8
                        || customModelData == 11
                        || customModelData == 4;
            }
            if (type == Material.NETHERITE_AXE && customModelData == 2) return true;
            if (type == Material.TRIDENT && customModelData == 4) return true;
            if ((type == Material.CROSSBOW || type == Material.BOW) && customModelData == 1) return true;
            if ((type == Material.CROSSBOW || type == Material.BOW) && customModelData == 2) return true;
            if (type == Material.MACE && (customModelData == 1 || customModelData == 2 || customModelData == 3 || (customModelData >= 3001 && customModelData <= 3003))) return true;
        }
        return false;
    }

    private boolean isLegendary(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (isLegendaryWeapon(item)) return true;
        if (isCopperArmor(item)) return true;
        if (CopperPickaxeItem.isCopperPickaxe(item)) return true;

        ItemMeta meta = item.getItemMeta();
        if (meta.hasCustomModelData()) {
            int customModelData = meta.getCustomModelData();
            Material type = item.getType();

            if (type == Material.FEATHER && customModelData == 1) return true;
            if (type == Material.CLAY_BALL && customModelData == 3) return true;
            if (type == Material.PAPER && (customModelData == 1 || customModelData == 2 || customModelData == 3 || customModelData == 7 || customModelData == 9)) return true;
            if (type == Material.MACE && (customModelData == 1 || customModelData == 2 || customModelData == 3 || (customModelData >= 3001 && customModelData <= 3003))) return true;
        }

        return false;
    }
}
