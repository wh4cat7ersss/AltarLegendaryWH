package dev.whersss.altarLegendaryWH.weapons.knightfall.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import java.util.UUID;
import dev.whersss.altarLegendaryWH.weapons.knightfall.managers.KnightfallManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class KnightfallListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final KnightfallManager manager;
    private final NamespacedKey huntKey;

    public KnightfallListener(AltarLegendaryWH plugin, KnightfallManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.huntKey = new NamespacedKey("ffashopwh", "hunt_item");
    }

    private boolean isKnightfall(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "knightfall"), PersistentDataType.BYTE)) return true;
        if (meta.getPersistentDataContainer().has(plugin.getKnightfallTierKey(), PersistentDataType.INTEGER)) return true;
        return item.getType() == Material.MACE && meta.getPersistentDataContainer().has(plugin.getKillsKey(), PersistentDataType.INTEGER);
    }

    private int getKills(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(plugin.getKillsKey(), PersistentDataType.INTEGER, 0);
    }

    private ItemStack applyHuntTagIfNecessary(ItemStack oldItem, ItemStack newItem) {
        if (oldItem.hasItemMeta() && oldItem.getItemMeta().getPersistentDataContainer().has(huntKey, PersistentDataType.BYTE)) {
            ItemMeta newMeta = newItem.getItemMeta();
            if (newMeta != null) {
                newMeta.getPersistentDataContainer().set(huntKey, PersistentDataType.BYTE, (byte) 1);
                newItem.setItemMeta(newMeta);
            }
        }
        return newItem;
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        if (isKnightfall(e.getInventory().getItem(0)) || isKnightfall(e.getInventory().getItem(1))) {
            e.setResult(null);
        }
    }

    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent e) {
        if (isKnightfall(e.getItem())) e.setCancelled(true);
    }

    private int getTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(plugin.getKnightfallTierKey(), PersistentDataType.INTEGER);
        if (tier != null) return tier;
        int cmd = item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 1;
        boolean hasDensity = item.getItemMeta().hasEnchant(Enchantment.DENSITY);
        if (cmd >= 3003 || cmd == 3) return 3;
        if (hasDensity) return 2;
        if (cmd >= 3002 || cmd == 2) return 1;
        return 0;
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() != null) {
            Player p = e.getEntity().getKiller();
            ItemStack item = p.getInventory().getItemInMainHand();

            if (isKnightfall(item)) {
                if (e.getEntity() instanceof Player) {
                    manager.playKillEffect(e.getEntity().getLocation());

                    int newKills = getKills(item) + 1;
                    int currentTier = getTier(item);
                    int currentCmd = (currentTier >= 3) ? 3 : (currentTier >= 1 ? 2 : 1);
                    boolean currentDensity = (currentTier >= 2);

                    ItemStack newWeapon = WeaponFactory.getKnightfallCustom(newKills, currentCmd, currentDensity);
                    newWeapon = applyHuntTagIfNecessary(item, newWeapon);
                    if (plugin.getCrazySlotsManager() != null && plugin.getCrazySlotsManager().isTransformedItem(item)) {
                        UUID csId = plugin.getCrazySlotsManager().getTransformedInstanceId(item);
                        if (csId != null) {
                            ItemMeta nm = newWeapon.getItemMeta();
                            if (nm != null) {
                                nm.getPersistentDataContainer().set(plugin.getCrazySlotsManager().getUniqueKey(), PersistentDataType.STRING, csId.toString());
                                newWeapon.setItemMeta(nm);
                            }
                        }
                    }
                    p.getInventory().setItemInMainHand(newWeapon);

                    p.sendMessage(TextUtils.legacy(plugin.tr("§cПадший Рыцарь теперь имеет " + newKills + " убийств(а/ов).", "§cknightfall now has " + newKills + " kills.")));

                    if (newKills == 2) p.sendMessage(TextUtils.legacy(plugin.tr("§eПадший рыцарь теперь имеет чары Порыв Ветра I.", "§eknightfall now has Wind Burst I.")));

                    boolean needsUpgrade = (newKills >= 4 && currentTier < 1) ||
                            (newKills >= 8 && currentTier < 2) ||
                            (newKills >= 10 && currentTier < 3);

                    if (needsUpgrade) {
                        p.sendMessage(TextUtils.legacy(plugin.tr("§aНажмите §2[Смена руки] §aчтобы улучшить оружие.", "§aPress §2[OffHand] §ato upgrade the weapon.")));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!isKnightfall(item)) return;

        e.setCancelled(true);

        int kills = getKills(item);
        int currentTier = getTier(item);

        if (p.isSneaking()) {
            if (kills >= 10 && currentTier >= 3) {
                manager.throwHammer(p, item);
            }
        } else {
            int targetTier = currentTier;
            if (kills >= 10 && currentTier < 3) {
                targetTier = 3;
            } else if (kills >= 8 && currentTier < 2) {
                targetTier = 2;
            } else if (kills >= 4 && currentTier < 1) {
                targetTier = 1;
            }

            if (targetTier > currentTier) {
                int newCmd = (targetTier >= 3) ? 3 : (targetTier >= 1 ? 2 : 1);
                boolean newDensity = (targetTier >= 2);

                ItemStack newWeapon = WeaponFactory.getKnightfallCustom(kills, newCmd, newDensity);
                newWeapon = applyHuntTagIfNecessary(item, newWeapon);
                if (plugin.getCrazySlotsManager() != null && plugin.getCrazySlotsManager().isTransformedItem(item)) {
                    UUID csId = plugin.getCrazySlotsManager().getTransformedInstanceId(item);
                    if (csId != null) {
                        ItemMeta nm = newWeapon.getItemMeta();
                        if (nm != null) {
                            nm.getPersistentDataContainer().set(plugin.getCrazySlotsManager().getUniqueKey(), PersistentDataType.STRING, csId.toString());
                            newWeapon.setItemMeta(nm);
                        }
                    }
                }
                p.getInventory().setItemInMainHand(newWeapon);
                p.updateInventory();

                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1f, 0.8f);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0f);

                p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, p.getLocation().add(0, 1, 0), 100, 0.5, 1.0, 0.5);

                p.sendActionBar(TextUtils.legacy(plugin.tr("&6Оружие успешно улучшено.", "&6Weapon upgraded successfully.")));
                p.sendMessage(TextUtils.legacy(plugin.tr("§6Оружие успешно улучшено.", "§6Weapon upgraded successfully.")));

                if (targetTier == 1) p.sendMessage(TextUtils.legacy(plugin.tr("§eТеперь вы можете использовать Абордажный Крюк Падшего Рыцаря.", "§eYou can now use knightfall's grappling hook.")));
                if (targetTier == 2) p.sendMessage(TextUtils.legacy(plugin.tr("§eПадший рыцарь теперь имеет чары Плотность II.", "§eknightfall now has Density II.")));
                if (targetTier == 3) p.sendMessage(TextUtils.legacy(plugin.tr("§eТеперь вы можете использовать Бросок Молота Падшего Рыцаря.", "§eYou can now use knightfall's hammer throw.")));

                return;
            }

            if (kills >= 4 && currentTier >= 1) {
                manager.useHook(p);
            }
        }
    }

    @EventHandler
    public void onSmashAttack(EntityDamageByEntityEvent e) {
        if (!CombatUtils.isDirectMeleeHit(e)) return;
        if (!(e.getDamager() instanceof Player p)) return;
        ItemStack item = p.getInventory().getItemInMainHand();

        if (isKnightfall(item)) {
            if (p.getFallDistance() > 0.0 && !p.isOnGround()) {
                if (Math.random() <= plugin.getWeaponsConfig().getDouble("knightfall.cloak.chance", 0.10)) {
                    if (manager.canUseCloak(p)) {
                        manager.triggerCloak(p);
                    }
                }
            }
        }
    }
}

