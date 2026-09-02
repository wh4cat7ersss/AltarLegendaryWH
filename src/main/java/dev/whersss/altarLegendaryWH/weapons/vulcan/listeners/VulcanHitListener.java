package dev.whersss.altarLegendaryWH.weapons.vulcan.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

public class VulcanHitListener implements Listener {

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        if (!arrow.getPersistentDataContainer().has(plugin.getArrowKey(), PersistentDataType.BYTE)) return;

        Location hitLoc = event.getHitEntity() != null ? event.getHitEntity().getLocation() : (event.getHitBlock() != null ? event.getHitBlock().getLocation() : null);

        if (hitLoc == null) return;

        if (event.getHitEntity() instanceof LivingEntity target) {

            if (arrow.getShooter() instanceof Player shooter && target instanceof Player targetPlayer) {
                if (plugin.getFriendManager().isFriend(shooter.getUniqueId(), targetPlayer.getUniqueId())) {
                    event.setCancelled(true);
                    arrow.remove();
                    return;
                }
            }

            hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 30, 0.3, 0.3, 0.3, 0.1);

            double extraDmg = plugin.getWeaponsConfig().getDouble("vulcan_crossbow.damage", 18.0);
            LivingEntity shooter = arrow.getShooter() instanceof LivingEntity ? (LivingEntity) arrow.getShooter() : null;
            CombatUtils.runSyntheticDamage(() -> target.damage(extraDmg, shooter));
            target.setFireTicks(60);

            hitLoc.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 60, 0.6, 0.8, 0.6, 0.1);

            arrow.remove();
        } else {
            hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 30, 0.3, 0.3, 0.3, 0.1);
        }
    }
}

