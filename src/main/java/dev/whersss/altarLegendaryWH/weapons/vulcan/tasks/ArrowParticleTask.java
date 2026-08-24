package dev.whersss.altarLegendaryWH.weapons.vulcan.tasks;

import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.scheduler.BukkitRunnable;

public class ArrowParticleTask extends BukkitRunnable {

    private final Arrow arrow;
    private final boolean isCentral;

    public ArrowParticleTask(Arrow arrow, boolean isCentral) {
        this.arrow = arrow;
        this.isCentral = isCentral;
    }

    @Override
    public void run() {
        if (arrow.isDead() || arrow.isInBlock() || arrow.isOnGround()) {
            this.cancel();
            return;
        }

        if (isCentral) {
            arrow.getWorld().spawnParticle(Particle.FLAME, arrow.getLocation(), 2, 0.05, 0.05, 0.05, 0.01);
            if (Math.random() > 0.6) {
                arrow.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, arrow.getLocation(), 1, 0.05, 0.05, 0.05, 0.01);
            }
        } else {
            arrow.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, arrow.getLocation(), 2, 0.05, 0.05, 0.05, 0.01);
            if (Math.random() > 0.6) {
                arrow.getWorld().spawnParticle(Particle.FLAME, arrow.getLocation(), 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }
}