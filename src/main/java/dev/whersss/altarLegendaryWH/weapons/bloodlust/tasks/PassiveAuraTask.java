package dev.whersss.altarLegendaryWH.weapons.bloodlust.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.bloodlust.listeners.BloodLustListener;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PassiveAuraTask extends BukkitRunnable {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Integer> trackerCooldowns = new HashMap<>();

    private final Map<UUID, PotionEffect> savedSpeed = new HashMap<>();
    private final Map<UUID, Long> savedSpeedTime = new HashMap<>();
    private final Map<UUID, PotionEffect> savedStrength = new HashMap<>();
    private final Map<UUID, Long> savedStrengthTime = new HashMap<>();
    private final Map<UUID, Boolean> wasHolding = new HashMap<>();

    public PassiveAuraTask(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getBloodAbilityManager().updateHandCheck(p);
            ItemStack item = p.getInventory().getItemInMainHand();

            boolean isHoldingBloodlust = BloodLustListener.isBloodLust(item);
            boolean heldBefore = wasHolding.getOrDefault(p.getUniqueId(), false);

            if (isHoldingBloodlust) {
                int kills = BloodLustListener.getKills(item);

                int speedAmp = plugin.getWeaponsConfig().getInt("bloodlust.passive.speed-amplifier", 1);
                int strAmp = plugin.getWeaponsConfig().getInt("bloodlust.passive.strength-amplifier", 0);

                if (!heldBefore) {
                    PotionEffect curSpeed = p.getPotionEffect(PotionEffectType.SPEED);
                    if (curSpeed != null && !isBloodlustPassiveEffect(curSpeed, speedAmp) && !dev.whersss.altarLegendaryWH.utils.SpeedBuffUtils.isWearingCopperBoots(p)) {
                        savedSpeed.put(p.getUniqueId(), curSpeed);
                        savedSpeedTime.put(p.getUniqueId(), System.currentTimeMillis());
                    }

                    PotionEffect curStr = p.getPotionEffect(PotionEffectType.STRENGTH);
                    if (curStr != null && !isBloodlustPassiveEffect(curStr, strAmp)) {
                        savedStrength.put(p.getUniqueId(), curStr);
                        savedStrengthTime.put(p.getUniqueId(), System.currentTimeMillis());
                    }
                }

                if (kills >= 1) {
                    PotionEffect curSpeed = p.getPotionEffect(PotionEffectType.SPEED);
                    if (curSpeed == null || isBloodlustPassiveEffect(curSpeed, speedAmp) || curSpeed.getAmplifier() < speedAmp) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, speedAmp, false, false, true));
                    }
                }
                if (kills >= 4) {
                    PotionEffect curStr = p.getPotionEffect(PotionEffectType.STRENGTH);
                    if (curStr == null || isBloodlustPassiveEffect(curStr, strAmp) || curStr.getAmplifier() < strAmp) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, strAmp, false, false, true));
                    }
                }

                wasHolding.put(p.getUniqueId(), true);

                if (kills >= 2) {
                    int cooldown = trackerCooldowns.getOrDefault(p.getUniqueId(), 0);
                    if (cooldown > 0) {
                        trackerCooldowns.put(p.getUniqueId(), cooldown - 1);
                    } else {
                        Player target = findNearestTarget(p);
                        if (target != null) {
                            plugin.getBloodAbilityManager().launchTrackerProjectile(p, target);
                            trackerCooldowns.put(p.getUniqueId(), 30);
                        }
                    }
                }
            } else {
                if (heldBefore) {
                    PotionEffect activeSpeed = p.getPotionEffect(PotionEffectType.SPEED);
                    PotionEffect activeStrength = p.getPotionEffect(PotionEffectType.STRENGTH);
                    int speedAmp = plugin.getWeaponsConfig().getInt("bloodlust.passive.speed-amplifier", 1);
                    int strAmp = plugin.getWeaponsConfig().getInt("bloodlust.passive.strength-amplifier", 0);

                    if (isBloodlustPassiveEffect(activeSpeed, speedAmp)) {
                        p.removePotionEffect(PotionEffectType.SPEED);
                    }
                    if (isBloodlustPassiveEffect(activeStrength, strAmp)) {
                        p.removePotionEffect(PotionEffectType.STRENGTH);
                    }

                    if (savedSpeed.containsKey(p.getUniqueId())) {
                        PotionEffect old = savedSpeed.remove(p.getUniqueId());
                        Long startTime = savedSpeedTime.remove(p.getUniqueId());
                        if (old != null && startTime != null && !dev.whersss.altarLegendaryWH.utils.SpeedBuffUtils.isWearingCopperBoots(p)) {
                            if (old.getDuration() == PotionEffect.INFINITE_DURATION) {
                                p.addPotionEffect(old);
                            } else {
                                long elapsed = (System.currentTimeMillis() - startTime) / 50L;
                                int rem = old.getDuration() - (int) elapsed;
                                if (rem > 0) {
                                    p.addPotionEffect(new PotionEffect(old.getType(), rem, old.getAmplifier(), old.isAmbient(), old.hasParticles(), old.hasIcon()));
                                }
                            }
                        }
                    }

                    if (savedStrength.containsKey(p.getUniqueId())) {
                        PotionEffect old = savedStrength.remove(p.getUniqueId());
                        Long startTime = savedStrengthTime.remove(p.getUniqueId());
                        if (old != null && startTime != null) {
                            if (old.getDuration() == PotionEffect.INFINITE_DURATION) {
                                p.addPotionEffect(old);
                            } else {
                                long elapsed = (System.currentTimeMillis() - startTime) / 50L;
                                int rem = old.getDuration() - (int) elapsed;
                                if (rem > 0) {
                                    p.addPotionEffect(new PotionEffect(old.getType(), rem, old.getAmplifier(), old.isAmbient(), old.hasParticles(), old.hasIcon()));
                                }
                            }
                        }
                    }

                    wasHolding.put(p.getUniqueId(), false);
                }
            }
        }
    }

    private boolean isBloodlustPassiveEffect(PotionEffect effect, int amplifier) {
        return effect != null
                && effect.getAmplifier() == amplifier
                && effect.getDuration() <= 80;
    }

    private Player findNearestTarget(Player p) {
        Player nearest = null;
        double minDist = 80.0;
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p) || other.getGameMode() == GameMode.SPECTATOR) continue;
            if (BloodLustListener.isBloodLust(other.getInventory().getItemInMainHand())) continue;
            if (!other.getWorld().equals(p.getWorld())) continue;

            double dist = p.getLocation().distance(other.getLocation());
            if (dist < minDist) {
                minDist = dist;
                nearest = other;
            }
        }
        return nearest;
    }
}

