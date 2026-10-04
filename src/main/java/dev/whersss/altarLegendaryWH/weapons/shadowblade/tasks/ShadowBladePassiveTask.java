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
                if (!isWeaponSpeed && curSpeed != null) {
                    savedSpeed.put(p.getUniqueId(), curSpeed);
                }

                if (curSpeed == null || isWeaponSpeed || curSpeed.getAmplifier() < 1) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false, true));
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
                        p.addPotionEffect(savedSpeed.remove(p.getUniqueId()));
                    }

                    wasHolding.put(p.getUniqueId(), false);
                }
            }
        }
    }
}
