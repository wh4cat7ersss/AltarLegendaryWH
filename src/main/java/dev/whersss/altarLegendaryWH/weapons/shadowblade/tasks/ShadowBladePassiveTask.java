package dev.whersss.altarLegendaryWH.weapons.shadowblade.tasks;

import dev.whersss.altarLegendaryWH.weapons.shadowblade.listeners.ShadowBladeListener;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShadowBladePassiveTask extends BukkitRunnable {

    private final Map<UUID, PotionEffect> savedSpeed = new HashMap<>();
    private final Map<UUID, Long> savedSpeedTime = new HashMap<>();
    private final Map<UUID, Boolean> wasHolding = new HashMap<>();
    private final Particle.DustOptions subtleBlackDust = new Particle.DustOptions(Color.fromRGB(15, 15, 20), 1.35f);

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack item = p.getInventory().getItemInMainHand();
            boolean isHolding = ShadowBladeListener.isShadowBlade(item);

            PotionEffect curSpeed = p.getPotionEffect(PotionEffectType.SPEED);
            boolean isWeaponSpeed = curSpeed != null && curSpeed.getAmplifier() == 1 && curSpeed.getDuration() <= 80;

            if (isHolding) {
                if (!wasHolding.getOrDefault(p.getUniqueId(), false) && curSpeed != null && !isWeaponSpeed) {
                    if (!dev.whersss.altarLegendaryWH.utils.SpeedBuffUtils.isWearingCopperBoots(p)) {
                        savedSpeed.put(p.getUniqueId(), curSpeed);
                        savedSpeedTime.put(p.getUniqueId(), System.currentTimeMillis());
                    }
                }

                if (curSpeed == null || isWeaponSpeed || curSpeed.getAmplifier() < 1) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true));
                }

                wasHolding.put(p.getUniqueId(), true);

                Location playerLoc = p.getLocation();
                Vector facing = playerLoc.getDirection().setY(0);
                if (facing.lengthSquared() < 0.001) {
                    facing = new Vector(0, 0, 1);
                } else {
                    facing.normalize();
                }

                Location trailLoc = playerLoc.clone()
                        .add(0.0, 0.12, 0.0)
                        .subtract(facing.clone().multiply(0.4));
                p.getWorld().spawnParticle(
                        Particle.DUST,
                        trailLoc,
                        8,
                        0.22, 0.10, 0.22,
                        0.0,
                        subtleBlackDust
                );

            } else {
                boolean heldBefore = wasHolding.getOrDefault(p.getUniqueId(), false);

                if (heldBefore) {
                    if (isWeaponSpeed) {
                        p.removePotionEffect(PotionEffectType.SPEED);
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

                    wasHolding.put(p.getUniqueId(), false);
                }
            }
        }
    }
}
