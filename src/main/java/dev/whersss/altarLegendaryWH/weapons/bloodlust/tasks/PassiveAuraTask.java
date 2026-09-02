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
    private final Map<UUID, PotionEffect> savedStrength = new HashMap<>();
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

                if (!heldBefore) {
                    PotionEffect curSpeed = p.getPotionEffect(PotionEffectType.SPEED);
                    if (curSpeed != null) savedSpeed.put(p.getUniqueId(), curSpeed);

                    PotionEffect curStr = p.getPotionEffect(PotionEffectType.STRENGTH);
                    if (curStr != null) savedStrength.put(p.getUniqueId(), curStr);

                    wasHolding.put(p.getUniqueId(), true);
                }

                int speedAmp = plugin.getWeaponsConfig().getInt("bloodlust.passive.speed-amplifier", 1);
                int strAmp = plugin.getWeaponsConfig().getInt("bloodlust.passive.strength-amplifier", 0);

                if (kills >= 1) p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, speedAmp, false, false, true));
                if (kills >= 4) p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, strAmp, false, false, true));

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
                        p.addPotionEffect(savedSpeed.remove(p.getUniqueId()));
                    }
                    if (savedStrength.containsKey(p.getUniqueId())) {
                        p.addPotionEffect(savedStrength.remove(p.getUniqueId()));
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

