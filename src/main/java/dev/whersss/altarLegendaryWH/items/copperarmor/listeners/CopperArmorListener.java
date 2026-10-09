package dev.whersss.altarLegendaryWH.items.copperarmor.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.items.copperarmor.utils.CopperArmorFactory;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CopperArmorListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Integer> chestplateHits = new HashMap<>();
    private final Random random = new Random();
    private boolean isLightningStriking = false;

    public CopperArmorListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    private boolean isCopperArmor(ItemStack item, String type) {
        if (item == null || !item.hasItemMeta()) return false;
        String val = item.getItemMeta().getPersistentDataContainer().get(CopperArmorFactory.getArmorKey(), PersistentDataType.STRING);
        return type.equals(val);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (isCopperArmor(p.getInventory().getLeggings(), "leggings")) {
                e.setCancelled(true);

                double fallDist = p.getFallDistance();
                double minFall = plugin.getItemsConfig().getDouble("copper-armor.leggings.min-fall-distance", 3.0);

                if (fallDist >= minFall) {
                    double maxFall = plugin.getItemsConfig().getDouble("copper-armor.leggings.max-fall-distance", 20.0);
                    double minPow = plugin.getItemsConfig().getDouble("copper-armor.leggings.min-explosion-power", 0.2);
                    double maxPow = plugin.getItemsConfig().getDouble("copper-armor.leggings.max-explosion-power", 3.5);

                    double power = minPow + ((fallDist - minFall) / (maxFall - minFall)) * (maxPow - minPow);
                    power = Math.min(power, maxPow);

                    Location loc = p.getLocation();

                    p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

                    p.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
                    p.getWorld().spawnParticle(Particle.GUST_EMITTER_LARGE, loc, 1, 0, 0, 0, 0);

                    int cloudCount = (int) (power * 30);
                    for (int i = 0; i < cloudCount; i++) {
                        double vx = (random.nextDouble() - 0.5);
                        double vy = (random.nextDouble() - 0.5);
                        double vz = (random.nextDouble() - 0.5);
                        p.getWorld().spawnParticle(Particle.CLOUD, loc, 0, vx, vy, vz, 0.2);
                    }

                    double damageMult = plugin.getItemsConfig().getDouble("copper-armor.leggings.damage-multiplier", 6.0);
                    double radiusMult = plugin.getItemsConfig().getDouble("copper-armor.leggings.radius-multiplier", 2.0);
                    double knockbackMult = plugin.getItemsConfig().getDouble("copper-armor.leggings.knockback-multiplier", 1.0);

                    double explosionRadius = power * radiusMult;
                    for (Entity ent : loc.getWorld().getNearbyEntities(loc, explosionRadius, explosionRadius, explosionRadius)) {
                        if (ent instanceof LivingEntity victim && !victim.equals(p)) {
                            if (victim instanceof Player vp && plugin.getFriendManager().isFriend(p.getUniqueId(), vp.getUniqueId())) continue;
                            double dist = victim.getLocation().distance(loc);
                            if (dist > explosionRadius) continue;

                            double damageFactor = 1.0 - (dist / explosionRadius);
                            final double finalDamage = power * damageMult * damageFactor;
                            victim.setNoDamageTicks(0);
                            CombatUtils.runSyntheticDamage(() -> victim.damage(finalDamage, p));

                            Vector knockback = victim.getLocation().toVector().subtract(loc.toVector()).normalize();
                            knockback.multiply(power * 0.8 * knockbackMult * damageFactor).setY(power * 0.4);
                            victim.setVelocity(victim.getVelocity().add(knockback));
                        }
                    }
                }
            }
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            if (isCopperArmor(p.getInventory().getChestplate(), "chestplate")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (isLightningStriking) return;

        Player chestplateUser = null;
        LivingEntity guaranteedTarget = null;

        if (e.getDamager() instanceof Player attacker && isCopperArmor(attacker.getInventory().getChestplate(), "chestplate")) {
            if (attacker.getCooledAttackStrength(0) >= 1.0f) {
                chestplateUser = attacker;
                if (e.getEntity() instanceof LivingEntity target) {
                    guaranteedTarget = target;
                }
            }
        }
        else if (e.getEntity() instanceof Player victim && isCopperArmor(victim.getInventory().getChestplate(), "chestplate")) {
            chestplateUser = victim;
            if (e.getDamager() instanceof LivingEntity attacker) {
                guaranteedTarget = attacker;
            }
        }

        if (chestplateUser != null) {
            int hits = chestplateHits.getOrDefault(chestplateUser.getUniqueId(), 0) + 1;
            int targetHits = plugin.getItemsConfig().getInt("copper-armor.chestplate.hits-for-lightning", 7);

            if (hits >= targetHits) {
                hits = 0;

                double radius = plugin.getItemsConfig().getDouble("copper-armor.chestplate.ring-radius", 10.0);
                int count = plugin.getItemsConfig().getInt("copper-armor.chestplate.lightning-count", 18);
                double lightningDmg = plugin.getItemsConfig().getDouble("copper-armor.chestplate.lightning-damage", 6.0);
                Location center = chestplateUser.getLocation();

                isLightningStriking = true;
                try {
                    if (guaranteedTarget != null && guaranteedTarget.isValid()) {
                        spawnCustomLightning(guaranteedTarget.getLocation().clone().add(0, 0.2, 0), lightningDmg, chestplateUser, guaranteedTarget);
                    }

                    for (int i = 0; i < count; i++) {
                        double angle = 2 * Math.PI * i / count;
                        double dx = radius * Math.cos(angle);
                        double dz = radius * Math.sin(angle);
                        Location strikeLoc = center.clone().add(dx, 0, dz);
                        spawnCustomLightning(strikeLoc, lightningDmg, chestplateUser, null);
                    }

                    int innerLightnings = count / 3;
                    for (int i = 0; i < innerLightnings; i++) {
                        double randomRadius = random.nextDouble() * radius;
                        double randomAngle = random.nextDouble() * 2 * Math.PI;
                        Location strikeLoc = center.clone().add(randomRadius * Math.cos(randomAngle), 0, randomRadius * Math.sin(randomAngle));
                        spawnCustomLightning(strikeLoc, lightningDmg, chestplateUser, null);
                    }
                } finally {
                    isLightningStriking = false;
                }
            }
            chestplateHits.put(chestplateUser.getUniqueId(), hits);
        }
    }

    private void spawnCustomLightning(Location loc, double damage, Player owner, LivingEntity guaranteedTarget) {
        loc.getWorld().strikeLightningEffect(loc);
        if (guaranteedTarget != null && guaranteedTarget.isValid()) {
            if (!(guaranteedTarget instanceof Player gp && plugin.getFriendManager().isFriend(owner.getUniqueId(), gp.getUniqueId()))) {
                guaranteedTarget.setNoDamageTicks(0);
                CombatUtils.runSyntheticDamage(() -> guaranteedTarget.damage(damage, owner));
            }
        }
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 1.5, 2.0, 1.5)) {
            if (ent instanceof LivingEntity victim && !victim.equals(owner)) {
                if (victim.equals(guaranteedTarget)) continue;
                if (victim instanceof Player vp && plugin.getFriendManager().isFriend(owner.getUniqueId(), vp.getUniqueId())) continue;
                victim.setNoDamageTicks(0);
                CombatUtils.runSyntheticDamage(() -> victim.damage(damage, owner));
            }
        }
    }
}
