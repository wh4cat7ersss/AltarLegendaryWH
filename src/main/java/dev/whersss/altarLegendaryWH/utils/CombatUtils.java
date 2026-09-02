package dev.whersss.altarLegendaryWH.utils;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class CombatUtils {

    private static final ThreadLocal<Integer> SYNTHETIC_DAMAGE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private CombatUtils() {
    }

    public static boolean isDirectMeleeHit(EntityDamageByEntityEvent event) {
        return !isSyntheticDamage()
                && (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK);
    }

    public static void runSyntheticDamage(Runnable action) {
        SYNTHETIC_DAMAGE_DEPTH.set(SYNTHETIC_DAMAGE_DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            int next = SYNTHETIC_DAMAGE_DEPTH.get() - 1;
            if (next <= 0) {
                SYNTHETIC_DAMAGE_DEPTH.remove();
            } else {
                SYNTHETIC_DAMAGE_DEPTH.set(next);
            }
        }
    }

    public static boolean isSyntheticDamage() {
        return SYNTHETIC_DAMAGE_DEPTH.get() > 0;
    }
}
