package dev.whersss.altarLegendaryWH.weapons.shadowblade.tasks;

import dev.whersss.altarLegendaryWH.weapons.shadowblade.listeners.ShadowBladeListener;
import org.bukkit.Color;
import org.bukkit.Bukkit;
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

                if (p.getVelocity().lengthSquared() > 0.01) {
                    spawnHoldDust(p);
                }

                wasHolding.put(p.getUniqueId(), true);
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

    private void spawnHoldDust(Player p) {
        Location base = p.getLocation().clone().add(0, 0.1, 0);
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.7f);

        for (int i = 0; i < 4; i++) {
            double y = i == 0 ? -0.15 : 0.15 + (i * 0.05);
            double spread = i == 0 ? 0.18 : 0.32;
            base.getWorld().spawnParticle(Particle.DUST, base.clone().add((Math.random() - 0.5) * spread, y, (Math.random() - 0.5) * spread), 1, 0, 0, 0, 0, dust);
        }
    }
}
