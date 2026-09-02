package dev.whersss.altarLegendaryWH.weapons.palegun.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.palegun.managers.PaleGunAbilityManager;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PaleProjectileTask extends BukkitRunnable {

    private final Player shooter;
    private final Vector velocity;
    private final BlockDisplay display;
    private final AltarLegendaryWH plugin;
    private final PaleGunAbilityManager abilityManager;

    private final BlockData resinData = Material.RESIN_BLOCK.createBlockData();
    private final BlockData paleOakData = Material.PALE_OAK_LOG.createBlockData();

    private Location currentLoc;
    private int ticks;
    private boolean cleaned;

    public PaleProjectileTask(Player shooter, Location start, Vector velocity, AltarLegendaryWH plugin, PaleGunAbilityManager abilityManager) {
        this.shooter = shooter;
        this.currentLoc = start.clone();
        this.velocity = velocity.clone();
        this.plugin = plugin;
        this.abilityManager = abilityManager;

        this.display = (BlockDisplay) start.getWorld().spawnEntity(start, EntityType.BLOCK_DISPLAY);
        display.setBlock(resinData);
        display.setInterpolationDuration(1);
        display.setTeleportDuration(1);
        display.setInterpolationDelay(0);
        Matrix4f transform = new Matrix4f().translate(-0.5f, -0.5f, -0.5f);
        display.setTransformationMatrix(transform);
    }

    @Override
    public void run() {
        if (cleaned || !shooter.isOnline() || shooter.isDead() || display.isDead()) {
            forceCleanup();
            return;
        }

        if (ticks++ > plugin.getWeaponsConfig().getInt("pale-gun.shoot.lifetime-ticks", 150)) {
            forceCleanup();
            return;
        }

        double gravity = plugin.getWeaponsConfig().getDouble("pale-gun.shoot.gravity-per-tick", 0.05);
        velocity.setY(velocity.getY() - gravity);

        RayTraceResult hit = currentLoc.getWorld().rayTrace(
                currentLoc,
                velocity.clone().normalize(),
                velocity.length() + 0.1,
                FluidCollisionMode.NEVER,
                true,
                0.5,
                entity -> entity != shooter && entity != display
        );

        if (hit != null) {
            impact(hit.getHitPosition().toLocation(currentLoc.getWorld()));
            return;
        }
        Location previousLoc = currentLoc.clone();

        currentLoc.add(velocity);
        display.teleport(currentLoc);
        double distance = velocity.length();
        double step = 0.2;

        if (distance > 0) {
            Vector direction = velocity.clone().normalize();
            for (double d = 0; d < distance; d += step) {
                Location trailLoc = previousLoc.clone().add(direction.clone().multiply(d));
                currentLoc.getWorld().spawnParticle(
                        Particle.BLOCK_CRUMBLE,
                        trailLoc,
                        3,
                        0.15, 0.15, 0.15,
                        0.0,
                        resinData
                );
            }
        }
    }

    public void forceCleanup() {
        if (cleaned) {
            return;
        }

        cleaned = true;
        display.remove();
        abilityManager.untrackProjectile(this);
        cancel();
    }

    private void impact(Location loc) {
        if (cleaned) {
            return;
        }

        World world = loc.getWorld();
        if (world == null) {
            forceCleanup();
            return;
        }

        float power = (float) plugin.getWeaponsConfig().getDouble("pale-gun.shoot.explosion-power", 2.0);
        world.createExplosion(loc, power, false, false, shooter);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        world.playSound(loc, Sound.BLOCK_RESIN_BREAK, 1.0f, 0.8f);
        world.playSound(loc, Sound.BLOCK_WOOD_BREAK, 1.0f, 0.5f);

        List<Vector> velocities = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            double u = ThreadLocalRandom.current().nextDouble();
            double v = ThreadLocalRandom.current().nextDouble();
            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);

            double speed = ThreadLocalRandom.current().nextDouble() * 0.8 + 0.2;

            double dx = Math.sin(phi) * Math.cos(theta) * speed * 1.6;
            double dy = Math.abs(Math.cos(phi)) * speed + 0.3;
            double dz = Math.sin(phi) * Math.sin(theta) * speed * 1.6;

            velocities.add(new Vector(dx, dy, dz));
        }

        new BukkitRunnable() {
            int tick = 0;
            final int maxTicks = 5;

            @Override
            public void run() {
                if (tick > maxTicks) {
                    cancel();
                    return;
                }

                for (int i = 0; i < velocities.size(); i++) {
                    Vector vel = velocities.get(i);
                    Location particleLoc = loc.clone().add(
                            vel.getX() * tick,
                            (vel.getY() * tick) - (0.05 * tick * tick),
                            vel.getZ() * tick
                    );

                    BlockData data = (i % 6 == 0) ? paleOakData : resinData;

                    loc.getWorld().spawnParticle(
                            Particle.BLOCK_CRUMBLE,
                            particleLoc,
                            1,
                            0.0, 0.0, 0.0,
                            0.0,
                            data
                    );
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0, 1);

        createMistCloud(loc);
        forceCleanup();
    }

    private void createMistCloud(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        int duration = plugin.getWeaponsConfig().getInt("pale-gun.shoot.cloud-duration-ticks", 600);
        double radius = plugin.getWeaponsConfig().getDouble("pale-gun.shoot.cloud-radius", 3.0);
        int effectDuration = plugin.getWeaponsConfig().getInt("pale-gun.shoot.effect-duration-ticks", 12000);
        int effectAmp = plugin.getWeaponsConfig().getInt("pale-gun.shoot.effect-amplifier", 1);

        AreaEffectCloud cloud = (AreaEffectCloud) world.spawnEntity(loc.clone().add(0, 0.1, 0), EntityType.AREA_EFFECT_CLOUD);
        cloud.setDuration(duration);
        cloud.setWaitTime(0);
        cloud.setRadius((float) radius);

        cloud.setRadiusPerTick(-((float) radius / duration));
        cloud.setRadiusOnUse(-0.5f);

        cloud.setBasePotionType(PotionType.AWKWARD);
        cloud.setColor(Color.fromRGB(132, 155, 153));

        PotionEffect luckEffect = new PotionEffect(PotionEffectType.LUCK, effectDuration, effectAmp, false, true, true);
        cloud.addCustomEffect(luckEffect, true);
    }
}
