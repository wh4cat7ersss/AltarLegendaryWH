package dev.whersss.altarLegendaryWH.utils;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CleanDamageManager implements Listener {

    private final Map<UUID, Double> pendingDamage = new ConcurrentHashMap<>();

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
        CombatUtils.runSyntheticDamage(() -> {
            if (attacker != null) {
                target.damage(triggerDamage, attacker);
            } else {
                target.damage(triggerDamage);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCleanDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        Double damage = pendingDamage.remove(target.getUniqueId());
        if (damage == null) {
            return;
        }

        if (target instanceof Player player && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)) {
            return;
        }
        if (target.isInvulnerable()) {
            return;
        }

        if (event.isCancelled()) {
            event.setCancelled(false);
        }

        // 1. Golden hearts (Absorption) handling:
        // Absorption takes damage first!
        double currentAbsorption = Math.max(0.0, target.getAbsorptionAmount());
        double damageToAbsorb = Math.min(currentAbsorption, damage);
        double remainingDamage = Math.max(0.0, damage - damageToAbsorb);

        if (damageToAbsorb > 0.0) {
            target.setAbsorptionAmount(Math.max(0.0, currentAbsorption - damageToAbsorb));
        }

        // 2. Totem of Undying handling:
        // If remainingDamage is lethal (would reduce target's health to 0 or below)
        if (remainingDamage >= target.getHealth()) {
            EquipmentSlot totemSlot = findTotemSlot(target);
            if (totemSlot != null) {
                EntityResurrectEvent resurrectEvent = new EntityResurrectEvent(target, totemSlot);
                Bukkit.getPluginManager().callEvent(resurrectEvent);
                if (!resurrectEvent.isCancelled()) {
                    consumeTotem(target, totemSlot);

                    target.setHealth(1.0);
                    for (PotionEffect effect : target.getActivePotionEffects()) {
                        target.removePotionEffect(effect.getType());
                    }
                    target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));

                    target.playEffect(EntityEffect.TOTEM_RESURRECT);
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                    target.setNoDamageTicks(target.getMaximumNoDamageTicks());

                    if (target instanceof Player p) {
                        try {
                            p.incrementStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING);
                        } catch (Throwable ignored) {
                        }
                    }

                    event.setDamage(0.0);
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 3. Apply clean damage directly to health:
        // If all damage was absorbed by golden hearts:
        if (remainingDamage <= 0.0) {
            try {
                target.playHurtAnimation(0.0f);
                Sound hurtSound = target.getHurtSound();
                if (hurtSound != null) {
                    target.getWorld().playSound(target.getLocation(), hurtSound, 1.0f, 1.0f);
                }
            } catch (Throwable ignored) {
            }
            event.setDamage(0.0);
            zeroAllModifiers(event);
            return;
        }

        // Pure damage bypasses armor, resistance, magic and blocking
        event.setDamage(remainingDamage);
        zeroAllModifiers(event);
    }

    private void zeroAllModifiers(EntityDamageEvent event) {
        zeroModifier(event, EntityDamageEvent.DamageModifier.ARMOR);
        zeroModifier(event, EntityDamageEvent.DamageModifier.RESISTANCE);
        zeroModifier(event, EntityDamageEvent.DamageModifier.MAGIC);
        zeroModifier(event, EntityDamageEvent.DamageModifier.HARD_HAT);
        zeroModifier(event, EntityDamageEvent.DamageModifier.BLOCKING);
        zeroModifier(event, EntityDamageEvent.DamageModifier.ABSORPTION);
    }

    private EquipmentSlot findTotemSlot(LivingEntity target) {
        if (target instanceof Player player) {
            if (isTotem(player.getInventory().getItemInOffHand())) {
                return EquipmentSlot.OFF_HAND;
            }
            if (isTotem(player.getInventory().getItemInMainHand())) {
                return EquipmentSlot.HAND;
            }
        } else if (target.getEquipment() != null) {
            if (isTotem(target.getEquipment().getItemInOffHand())) {
                return EquipmentSlot.OFF_HAND;
            }
            if (isTotem(target.getEquipment().getItemInMainHand())) {
                return EquipmentSlot.HAND;
            }
        }
        return null;
    }

    private boolean isTotem(ItemStack item) {
        return item != null && item.getType() == Material.TOTEM_OF_UNDYING;
    }

    private void consumeTotem(LivingEntity target, EquipmentSlot slot) {
        if (target instanceof Player player) {
            ItemStack item = slot == EquipmentSlot.HAND
                    ? player.getInventory().getItemInMainHand()
                    : player.getInventory().getItemInOffHand();
            if (isTotem(item)) {
                item.setAmount(item.getAmount() - 1);
                if (slot == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(item.getAmount() > 0 ? item : null);
                } else {
                    player.getInventory().setItemInOffHand(item.getAmount() > 0 ? item : null);
                }
            }
        } else if (target.getEquipment() != null) {
            ItemStack item = slot == EquipmentSlot.HAND
                    ? target.getEquipment().getItemInMainHand()
                    : target.getEquipment().getItemInOffHand();
            if (isTotem(item)) {
                item.setAmount(item.getAmount() - 1);
                if (slot == EquipmentSlot.HAND) {
                    target.getEquipment().setItemInMainHand(item.getAmount() > 0 ? item : null);
                } else {
                    target.getEquipment().setItemInOffHand(item.getAmount() > 0 ? item : null);
                }
            }
        }
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
