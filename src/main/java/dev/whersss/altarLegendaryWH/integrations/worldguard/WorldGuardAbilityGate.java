package dev.whersss.altarLegendaryWH.integrations.worldguard;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class WorldGuardAbilityGate implements Listener {

    private final AltarLegendaryWH plugin;
    private final WorldGuardManager worldGuardManager;

    public WorldGuardAbilityGate(AltarLegendaryWH plugin, WorldGuardManager worldGuardManager) {
        this.plugin = plugin;
        this.worldGuardManager = worldGuardManager;
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
            if ((type == Material.CROSSBOW || type == Material.BOW) && (customModelData == 1 || customModelData == 2)) return true;
            if (type == Material.MACE && (customModelData == 1 || customModelData == 2 || customModelData == 3 || (customModelData >= 3001 && customModelData <= 3003))) return true;
        }
        return false;
    }

    private boolean isPaladinsBattleAxe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (plugin.getPaladinsBattleAxeKey() != null
                && meta.getPersistentDataContainer().has(plugin.getPaladinsBattleAxeKey(), PersistentDataType.BYTE)) {
            return true;
        }
        return item.getType() == Material.NETHERITE_AXE
                && meta.hasCustomModelData()
                && meta.getCustomModelData() == 2;
    }

    private void notifyDenied(Player player, ItemStack item) {
        if (isPaladinsBattleAxe(item)) {
            worldGuardManager.notifyDeniedChat(player);
        } else {
            worldGuardManager.notifyDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!isLegendaryWeapon(event.getMainHandItem()) && !isLegendaryWeapon(event.getOffHandItem())) {
            return;
        }

        if (!worldGuardManager.canUseAbilities(player)) {
            event.setCancelled(true);
            ItemStack item = isLegendaryWeapon(event.getMainHandItem())
                    ? event.getMainHandItem()
                    : event.getOffHandItem();
            notifyDenied(player, item);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isLegendaryWeapon(item)) return;

        Player player = event.getPlayer();
        if (!worldGuardManager.canUseAbilities(player)) {
            event.setCancelled(true);
            notifyDenied(player, item);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLegendaryWeapon(event.getBow())) return;

        if (!worldGuardManager.canUseAbilities(player)) {
            event.setCancelled(true);
            worldGuardManager.notifyDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            if (!worldGuardManager.canUseAbilities(player)) {
                ItemStack main = player.getInventory().getItemInMainHand();
                ItemStack off = player.getInventory().getItemInOffHand();
                if (isLegendaryWeapon(main) || isLegendaryWeapon(off)) {
                    event.setCancelled(true);
                    notifyDenied(player, isLegendaryWeapon(main) ? main : off);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTemporaryDisplayTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Display)) {
            return;
        }
        if (event.getTo() == null || worldGuardManager.isLocationAllowed(event.getTo(), null)) {
            return;
        }
        event.setCancelled(true);
    }
}
