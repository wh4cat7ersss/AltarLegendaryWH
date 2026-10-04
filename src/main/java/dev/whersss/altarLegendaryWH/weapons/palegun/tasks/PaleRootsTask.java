package dev.whersss.altarLegendaryWH.weapons.palegun.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.ParticleUtils;
import dev.whersss.altarLegendaryWH.weapons.palegun.managers.PaleGunAbilityManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PaleRootsTask extends BukkitRunnable {

    private static final Vector[] OFFSETS = {
            new Vector(-1.0, 0.0, 0.0),
            new Vector(-0.7, 0.7, 0.0),
            new Vector(0.0, 1.0, 0.0),
            new Vector(0.7, 0.7, 0.0),
            new Vector(1.0, 0.0, 0.0)
    };

    private static final float[] ROLLS = {
            0f, -45f, 90f, 45f, 0f
    };

    private final Player owner;
    private final int lifetimeTicks;
    private final double damage;
    private final AltarLegendaryWH plugin;
    private final PaleGunAbilityManager abilityManager;

    private final List<BlockDisplay> spawnedDisplays = new ArrayList<>();
    private final List<Location> archCenters = new ArrayList<>();
    private final Map<UUID, Integer> hitCooldowns = new HashMap<>();

    private Location currentArchBase;
    private Vector currentForward;
    private Vector currentRight;

    private int ageTicks = 0;
    private boolean cleaned = false;
    private boolean cleaningUp = false;

    public PaleRootsTask(Player owner, int durationSeconds, double damage, AltarLegendaryWH plugin, PaleGunAbilityManager abilityManager) {
        this.owner = owner;
        this.lifetimeTicks = durationSeconds * 20;
        this.damage = damage;
        this.plugin = plugin;
        this.abilityManager = abilityManager;

        this.currentArchBase = owner.getLocation().clone();
        this.currentArchBase.setPitch(0);
        updateDirection();

        while (currentArchBase.getY() > owner.getWorld().getMinHeight() && !currentArchBase.getBlock().getType().isSolid()) {
            currentArchBase.subtract(0, 1.0, 0);
        }
        currentArchBase.add(0, 1.0, 0);
    }

    @Override
    public void run() {
        if (cleaned || cleaningUp || !owner.isOnline() || owner.isDead() || owner.getWorld() == null) {
            forceCleanup();
            return;
        }

        hitCooldowns.replaceAll((uuid, ticks) -> ticks - 1);
        hitCooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);

        if (ageTicks < 60) {
            int archIndex = ageTicks / 5;
            int partIndex = ageTicks % 5;

            if (partIndex == 0) {
                updateDirection();
                if (archIndex > 0) {
                    currentArchBase.add(currentForward.clone().multiply(3.10));

                    currentArchBase.setY(currentArchBase.getY() + 2.0);
                    while (currentArchBase.getY() > owner.getWorld().getMinHeight() && !currentArchBase.getBlock().getType().isSolid()) {
                        currentArchBase.subtract(0, 1.0, 0);
                    }
                    if (currentArchBase.getBlock().getType().isSolid()) {
                        currentArchBase.add(0, 1.0, 0);
                    }
                }
                archCenters.add(currentArchBase.clone());
                currentArchBase.getWorld().playSound(currentArchBase, Sound.ENTITY_CREAKING_ATTACK, 1.0f, 1.0f);
            }

            spawnArchPart(partIndex);
        }

        applyContacts();
        ageTicks++;

        if (ageTicks >= lifetimeTicks) {
            forceCleanup();
        }
    }

    public void forceCleanup() {
        if (cleaned || cleaningUp) {
            return;
        }

        cleaningUp = true;
        abilityManager.untrackRootTask(this);
        cancel();

        BlockData paleOakData = Material.PALE_OAK_WOOD.createBlockData();

        if (!spawnedDisplays.isEmpty()) {
            Location soundLoc = spawnedDisplays.get(0).getLocation();
            soundLoc.getWorld().playSound(soundLoc, Sound.ENTITY_CREAKING_ATTACK, 1.0f, 0.8f);
        }
        for (BlockDisplay display : spawnedDisplays) {
            if (display != null && display.isValid()) {
                Location loc = display.getLocation();
                ParticleUtils.spawnBlockDispersion(plugin, loc, paleOakData, 2.0, 36);
                display.remove();
            }
        }

        cleaned = true;
        spawnedDisplays.clear();
        archCenters.clear();
        hitCooldowns.clear();
    }

    private void updateDirection() {
        Vector dir = owner.getEyeLocation().getDirection().setY(0);
        if (dir.lengthSquared() > 0.0001) {
            currentForward = dir.normalize();
            currentRight = currentForward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        } else if (currentForward == null) {
            currentForward = new Vector(1, 0, 0);
            currentRight = new Vector(0, 0, 1);
        }
    }

    private void spawnArchPart(int partIndex) {
        Vector offset = OFFSETS[partIndex];
        float roll = ROLLS[partIndex];

        Location targetLoc = currentArchBase.clone()
                .add(currentForward.clone().multiply(offset.getX()))
                .add(0, offset.getY(), 0);

        BlockData paleOakData = Material.PALE_OAK_WOOD.createBlockData();
        Location particleLoc = targetLoc.clone().add(currentForward.clone().multiply(1.2));
        ParticleUtils.spawnBlockDispersion(plugin, particleLoc, paleOakData, 0.5, 4);

        Location dirLoc = currentArchBase.clone();
        dirLoc.setDirection(currentForward);
        float dynamicYaw = dirLoc.getYaw();

        float yawRad = (float) Math.toRadians(-dynamicYaw);

        Location initialLoc;
        if (partIndex == 0) {
            initialLoc = targetLoc.clone().subtract(0, 1.0, 0);
        } else {
            Vector prevOffset = OFFSETS[partIndex - 1];
            initialLoc = currentArchBase.clone()
                    .add(currentForward.clone().multiply(prevOffset.getX()))
                    .add(0, prevOffset.getY(), 0);
        }

        initialLoc.setYaw(0);
        initialLoc.setPitch(0);
        targetLoc.setYaw(0);
        targetLoc.setPitch(0);

        Quaternionf targetRot = new Quaternionf()
                .rotateY(yawRad)
                .rotateX((float) Math.toRadians(roll));

        Vector3f uniformScale = new Vector3f(1.0f, 1.0f, 1.0f);
        Vector3f targetTransVec = new Vector3f(-0.5f, -0.5f, -0.5f).rotate(targetRot);
        Transformation targetTrans = new Transformation(
                targetTransVec, targetRot, uniformScale, new Quaternionf()
        );

        Transformation initialTrans;
        if (partIndex == 0) {
            initialTrans = targetTrans;
        } else {
            float prevRoll = ROLLS[partIndex - 1];

            Quaternionf prevRot = new Quaternionf()
                    .rotateY(yawRad)
                    .rotateX((float) Math.toRadians(prevRoll));

            Vector3f prevTransVec = new Vector3f(-0.5f, -0.5f, -0.5f).rotate(prevRot);
            initialTrans = new Transformation(
                    prevTransVec, prevRot, uniformScale, new Quaternionf()
            );
        }

        BlockDisplay display = (BlockDisplay) targetLoc.getWorld().spawnEntity(initialLoc, EntityType.BLOCK_DISPLAY);
        display.setBlock(paleOakData);
        display.setTransformation(initialTrans);
        plugin.getVisualCleanupManager().track(display);
        spawnedDisplays.add(display);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (display.isValid()) {
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(4);
                display.setTeleportDuration(4);
                display.setTransformation(targetTrans);
                display.teleport(targetLoc);
            }
        }, 1L);
    }

    private void applyContacts() {
        for (Location center : archCenters) {
            for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, 1.0, 1.0, 1.0)) {
                if (!(entity instanceof LivingEntity target) || target.equals(owner)) {
                    continue;
                }
                if (hitCooldowns.containsKey(target.getUniqueId())) {
                    continue;
                }
                if (target instanceof Player targetPlayer) {
                    if (targetPlayer.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }
                    if (plugin.getFriendManager().isFriend(owner.getUniqueId(), targetPlayer.getUniqueId())) {
                        continue;
                    }
                }

                target.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 600, 1, false, false, true));
                target.getWorld().playSound(target.getLocation(), Sound.ENCHANT_THORNS_HIT, 1.0f, 1.0f);

                Vector knockback = target.getLocation().toVector().subtract(center.toVector()).setY(0);
                if (knockback.lengthSquared() < 0.0001) {
                    knockback = currentForward.clone();
                }
                target.setVelocity(knockback.normalize().multiply(1.0).setY(0.25));

                plugin.getCleanDamageManager().apply(target, owner, damage);

                hitCooldowns.put(target.getUniqueId(), 10);
            }
        }
    }

    public void breakDisplay(BlockDisplay display, Location hitLocation) {
        if (display == null || !spawnedDisplays.remove(display)) {
            return;
        }

        Location loc = hitLocation != null ? hitLocation : display.getLocation();
        BlockData paleOakData = Material.PALE_OAK_WOOD.createBlockData();
        ParticleUtils.spawnBlockDispersion(plugin, loc, paleOakData, 3.0);
        abilityManager.untrackRootDisplay(display);
        plugin.getVisualCleanupManager().untrack(display);
        display.remove();
    }
}