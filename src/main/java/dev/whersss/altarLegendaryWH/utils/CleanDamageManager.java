package dev.whersss.altarLegendaryWH.utils;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CleanDamageManager implements Listener {

    private final Map<UUID, Double> pendingDamage = new HashMap<>();

    public CleanDamageManager(AltarLegendaryWH plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void apply(LivingEntity target, double amount) {
        apply(target, null, amount);
    }

    public void apply(LivingEntity target, Entity attacker, double amount) {
        if (target == null || amount <= 0 || target.isDead() || !target.isValid()) {
            return;
        }

        pendingDamage.put(target.getUniqueId(), amount);
        target.setNoDamageTicks(0);

        double triggerDamage = Math.max(0.001, amount);
        if (attacker != null) {
            target.damage(triggerDamage, attacker);
        } else {
            target.damage(triggerDamage);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCleanDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Double damage = pendingDamage.remove(event.getEntity().getUniqueId());
        if (damage == null) {
            return;
        }

        if (event.isCancelled()) {
            event.setCancelled(false);
        }

        event.setDamage(damage);
        zeroModifier(event, EntityDamageEvent.DamageModifier.ARMOR);
        zeroModifier(event, EntityDamageEvent.DamageModifier.RESISTANCE);
        zeroModifier(event, EntityDamageEvent.DamageModifier.MAGIC);
        zeroModifier(event, EntityDamageEvent.DamageModifier.HARD_HAT);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void cleanupPending(EntityDamageEvent event) {
        pendingDamage.remove(event.getEntity().getUniqueId());
    }

    public void clear() {
        pendingDamage.clear();
    }

    private void zeroModifier(EntityDamageEvent event, EntityDamageEvent.DamageModifier modifier) {
        if (event.isApplicable(modifier)) {
            try {
            event.setDamage(modifier, 0.0);
            } catch (UnsupportedOperationException ignored) {
            }
        }
    }
}
