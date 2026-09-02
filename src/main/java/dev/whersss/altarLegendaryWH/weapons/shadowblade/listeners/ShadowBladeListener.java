package dev.whersss.altarLegendaryWH.weapons.shadowblade.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.weapons.shadowblade.managers.ShadowBladeManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ShadowBladeListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final ShadowBladeManager manager;

    public ShadowBladeListener(AltarLegendaryWH plugin, ShadowBladeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public static boolean isShadowBlade(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        return item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 10;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (manager.activeLeaps.contains(player.getUniqueId())) {
                if (System.currentTimeMillis() - manager.getLeapStartTime(player) > 300) {
                    manager.activeLeaps.remove(player.getUniqueId());
                }
                return;
            }

            if (!isShadowBlade(item)) return;
            if (player.isSneaking()) {
                if (plugin.isAboveLegendaryHeight(player)) return;
                manager.castShadowLeap(player);
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isShadowBlade(item)) {
            event.setCancelled(true);
            if (plugin.isAboveLegendaryHeight(player)) return;
            manager.castShadowDaggers(player, item);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!CombatUtils.isDirectMeleeHit(event)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (plugin.isAboveLegendaryHeight(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (isShadowBlade(item)) {
            playShadowSweep(player, victim);

            Vector victimDir = victim.getLocation().getDirection().setY(0).normalize();
            Vector attackerDir = player.getLocation().getDirection().setY(0).normalize();
            if (victimDir.dot(attackerDir) > 0.5) {
                manager.triggerBackstab(player, victim);
            }
        }
    }

    private void playShadowSweep(Player player, LivingEntity victim) {
        Vector dir = player.getLocation().getDirection().normalize();
        Location center = victim.getLocation().add(0, 1, 0).subtract(dir.clone().multiply(0.8));

        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.2f);

        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.clone().crossProduct(dir).normalize();

        double rotAngle = Math.random() * Math.PI;
        double cos = Math.cos(rotAngle);
        double sin = Math.sin(rotAngle);

        Object blockData = Material.BLACK_CONCRETE.createBlockData();
        int step = 0;

        for (double i = -1.4; i <= 1.4; i += 0.05) {
            double x = i * cos;
            double y = i * sin;
            double z = -(i * i) * 0.25;

            Vector point = right.clone().multiply(x).add(up.clone().multiply(y)).add(dir.clone().multiply(z));
            point.multiply(1.15);
            Location particleLoc = center.clone().add(point);
            center.getWorld().spawnParticle(Particle.BLOCK, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, blockData);

            if (step % 2 == 0) {
                center.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);
            }
            step++;
        }
    }
}
