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
        return dev.whersss.altarLegendaryWH.utils.WeaponFactory.isAnyAltarWeapon(item);
    }

    private boolean isPaladinsBattleAxe(ItemStack item) {
        return dev.whersss.altarLegendaryWH.utils.WeaponFactory.isAltarWeapon(item, "paladins_battle_axe");
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
