package dev.whersss.altarLegendaryWH.weapons.palegun.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.ParticleUtils;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
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
        plugin.getVisualCleanupManager().track(display);
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

        Vector traceDirection = velocity.clone().normalize();
        double traceDistance = velocity.length() + 0.1;
        RayTraceResult blockHit = currentLoc.getWorld().rayTraceBlocks(
                currentLoc, traceDirection, traceDistance, FluidCollisionMode.NEVER, true
        );
        RayTraceResult entityHit = currentLoc.getWorld().rayTraceEntities(
                currentLoc, traceDirection, traceDistance, 0.35,
                entity -> entity != shooter
                        && entity != display
                        && (entity instanceof LivingEntity || abilityManager.isPaleRootDisplay(entity))
        );
        RayTraceResult hit = nearestHit(currentLoc, blockHit, entityHit);

        if (hit != null) {
            Location hitLocation = hit.getHitPosition().toLocation(currentLoc.getWorld());
            if (hit.getHitEntity() != null) {
                abilityManager.handleRootDisplayHit(hit.getHitEntity(), hitLocation);
            }
            impact(hitLocation);
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
                        2,
                        0.05, 0.05, 0.05,
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

        Location baseLoc = loc.clone().add(0, 0.2, 0);
        // 300 particles with wide horizontal spread
        ParticleUtils.spawnBlockDispersion(plugin, baseLoc, resinData, 8.0, 200);
        ParticleUtils.spawnBlockDispersion(plugin, baseLoc, paleOakData, 8.0, 100);

        createMistCloud(loc);
        forceCleanup();
    }

    private RayTraceResult nearestHit(Location origin, RayTraceResult first, RayTraceResult second) {
        if (first == null) return second;
        if (second == null) return first;
        double firstDistance = first.getHitPosition().distanceSquared(origin.toVector());
        double secondDistance = second.getHitPosition().distanceSquared(origin.toVector());
        return firstDistance <= secondDistance ? first : second;
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
