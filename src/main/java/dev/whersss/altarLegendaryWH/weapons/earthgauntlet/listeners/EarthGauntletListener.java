package dev.whersss.altarLegendaryWH.weapons.earthgauntlet.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.earthgauntlet.managers.EarthGauntletManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class EarthGauntletListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final EarthGauntletManager manager;
    private final NamespacedKey gauntletKey;

    public EarthGauntletListener(AltarLegendaryWH plugin, EarthGauntletManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.gauntletKey = new NamespacedKey(plugin, "earth_gauntlet");
    }

    public boolean isEarthGauntlet(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(gauntletKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();

        ItemStack item = e.getOffHandItem();
        if (!isEarthGauntlet(item)) {
            item = e.getMainHandItem();
            if (!isEarthGauntlet(item)) return;
        }

        e.setCancelled(true);

        if (plugin.isAboveLegendaryHeight(p)) return;

        if (p.isSneaking()) {
            manager.castMudslide(p);
        } else {
            if (manager.hasMuddiedTarget(p)) {
                manager.pullTarget(p);
            } else {
                manager.activateMeteorStrike(p);
            }
        }
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        if (isEarthGauntlet(item)) {
            if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                manager.playPassiveHitSound(p);
                manager.triggerMeteorStrikeHit(p, victim);
            }
        }
    }
}
