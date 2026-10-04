package dev.whersss.altarLegendaryWH.weapons.hyperion.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.hyperion.tasks.HyperionTasks;
import dev.whersss.altarLegendaryWH.weapons.hyperion.managers.HyperionCooldownManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class HyperionListener implements Listener {

    private boolean isHyperion(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(AltarLegendaryWH.getInstance().getHyperionKey(), PersistentDataType.BYTE);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (isHyperion(p.getInventory().getItemInMainHand())) {
            EntityDamageEvent.DamageCause c = e.getCause();
            if (c == EntityDamageEvent.DamageCause.FIRE || c == EntityDamageEvent.DamageCause.FIRE_TICK || c == EntityDamageEvent.DamageCause.LAVA || c == EntityDamageEvent.DamageCause.HOT_FLOOR) {
                e.setCancelled(true);
                p.setFireTicks(0);
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

        if (e.getDamager() instanceof Player p && e.getEntity() instanceof LivingEntity victim) {
            if (isHyperion(p.getInventory().getItemInMainHand())) {
                playHyperionSweep(p, victim);
            }
        }
    }

    private void playHyperionSweep(Player p, LivingEntity victim) {
        Vector dir = p.getLocation().getDirection().normalize();
        Location center = victim.getLocation().add(0, 1, 0).subtract(dir.clone().multiply(0.8));
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.6f);

        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (right.lengthSquared() == 0) right.copy(new Vector(1, 0, 0));
        Vector up = right.clone().crossProduct(dir).normalize();

        double rotAngle = Math.random() * Math.PI;
        double cos = Math.cos(rotAngle);
        double sin = Math.sin(rotAngle);

        for (double i = -1.4; i <= 1.4; i += 0.05) {
            double x = i * cos;
            double y = i * sin;
            double z = -(i * i) * 0.25;

            Vector point = right.clone().multiply(x).add(up.clone().multiply(y)).add(dir.clone().multiply(z));
            point.multiply(1.15);

            Location pLoc = center.clone().add(point);
            if (Math.random() > 0.5) center.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0.0, 0.0, 0.0, 0.0);
            center.getWorld().spawnParticle(Particle.CRIT, pLoc, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (isHyperion(p.getInventory().getItemInMainHand())) {
            e.setCancelled(true);


            if (p.isSneaking()) {
                if (HyperionCooldownManager.isOnScorchingCooldown(p) || HyperionTasks.ScorchingChargeTask.isCharging(p)) return;
                new HyperionTasks.ScorchingChargeTask(p).runTaskTimer(AltarLegendaryWH.getInstance(), 0, 1);
            } else {
                if (HyperionCooldownManager.isOnHolyLanceCooldown(p)) return;

                HyperionCooldownManager.setHolyLanceCooldown(p);
                new HyperionTasks.HolyLanceTask(p).runTaskTimer(AltarLegendaryWH.getInstance(), 0, 1);
            }
        }
    }
}