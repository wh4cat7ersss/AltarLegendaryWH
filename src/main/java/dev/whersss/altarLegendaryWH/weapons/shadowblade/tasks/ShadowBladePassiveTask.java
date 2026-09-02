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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShadowBladePassiveTask extends BukkitRunnable {

    private final Map<UUID, PotionEffect> savedSpeed = new HashMap<>();
    private final Map<UUID, Boolean> wasHolding = new HashMap<>();
    private final Particle.DustOptions bigBlackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.8f);

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

                Location feetLoc = p.getLocation().add(0, 0.15, 0);
                p.getWorld().spawnParticle(
                        Particle.DUST,
                        feetLoc,
                        6,
                        0.35, 0.15, 0.35,
                        0.0,
                        bigBlackDust
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