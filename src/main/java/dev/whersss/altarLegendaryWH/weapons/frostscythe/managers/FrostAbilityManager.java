package dev.whersss.altarLegendaryWH.weapons.frostscythe.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.ParticleUtils;
import dev.whersss.altarLegendaryWH.weapons.frostscythe.tasks.FrostScytheTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("deprecation")
public class FrostAbilityManager {
    private final AltarLegendaryWH plugin;
    private final FrostBossBarManager bossBarManager;

    private final BlockData blueIceData = Bukkit.createBlockData(Material.PACKED_ICE);
    private final BlockData packedIceData = Bukkit.createBlockData(Material.PACKED_ICE);

    public FrostAbilityManager(AltarLegendaryWH plugin, FrostBossBarManager bossBarManager) {
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
    }

    public AltarLegendaryWH getPlugin() {
        return plugin;
    }

    public boolean isOnCooldown(Player p, String ability) {
        return bossBarManager.isOnCooldown(p, ability);
    }

    public void playSweepEffect(Location center, Vector dir) {
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.2f);
        Vector forward = dir.clone().normalize();
        if (forward.lengthSquared() == 0) forward = new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (right.lengthSquared() == 0) right = new Vector(1, 0, 0);
        Vector up = right.clone().crossProduct(forward).normalize();

        double rotAngle = Math.random() * Math.PI;
        double cos = Math.cos(rotAngle);
        double sin = Math.sin(rotAngle);

        for (double i = -1.2; i <= 1.2; i += 0.05) {
            double x = i * cos;
            double y = i * sin;
            double z = -(i * i) * 0.25;
            Vector point = right.clone().multiply(x).add(up.clone().multiply(y)).add(forward.clone().multiply(z));
            Location pLoc = center.clone().add(point);
            center.getWorld().spawnParticle(Particle.BLOCK, pLoc, 1, 0.0, 0.0, 0.0, 0.0, blueIceData);
        }
    }
    public void playMassiveDustSweep(Location center, Vector dir) {
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THROW, 1.1f, 0.55f);

        Vector forward = dir.clone().setY(0).normalize();
        if (forward.lengthSquared() == 0) forward = new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        for (double i = -2.5; i <= 2.5; i += 0.05) {
            double z = -(i * i) * 0.25;

            Vector point = right.clone().multiply(i).add(forward.clone().multiply(z));
            Location spawnLoc = center.clone().add(point);
            center.getWorld().spawnParticle(Particle.SNOWFLAKE, spawnLoc, 1, 0.0, 0.0, 0.0, 0.0);
            center.getWorld().spawnParticle(Particle.ENCHANTED_HIT, spawnLoc, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public void playImpactBurst(Location center, Vector dir) {
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THROW, 1.15f, 0.4f);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.0f);

        Location baseLoc = center.clone().add(0, 0.2, 0);
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, baseLoc, 30, 0.4, 0.4, 0.4, 0.08);
        ParticleUtils.spawnBlockDispersion(plugin, baseLoc, blueIceData, 4.2, 85);
    }

    public void executeScytheThrow(Player p, ItemStack scytheItem, EquipmentSlot hand) {
        if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().canUseAbilities(p)) {
            plugin.getWorldGuardManager().notifyDenied(p);
            return;
        }

        int throwCd = plugin.getWeaponsConfig().getInt("frost-scythe.throw.cooldown", 45);
        bossBarManager.setCooldown(p, "ScytheThrow", "§b§lsᴄʏᴛʜᴇ ᴛʜʀᴏᴡ", throwCd);

        ItemStack throwItem = scytheItem.clone();

        throwItem.setType(Material.NETHERITE_SWORD);
        dev.whersss.altarLegendaryWH.weapons.frostscythe.listeners.FrostListener.setScytheModel(throwItem, false);

        if (plugin.getCrazySlotsManager() != null && plugin.getCrazySlotsManager().isTransformedItem(throwItem)) {
            java.util.UUID instId = plugin.getCrazySlotsManager().getTransformedInstanceId(throwItem);
            plugin.getCrazySlotsManager().markInFlight(instId);
        }

        if (hand == EquipmentSlot.OFF_HAND) p.getInventory().setItemInOffHand(null);
        else p.getInventory().setItemInMainHand(null);

        Location start = p.getEyeLocation();
        Vector dir = start.getDirection().normalize().multiply(plugin.getWeaponsConfig().getDouble("frost-scythe.throw.flight-speed", 1.65));

        Location sweepCenter = start.clone().add(dir.clone().normalize().multiply(1.5));
        playMassiveDustSweep(sweepCenter, dir);

        new FrostScytheTask(plugin, p, this, throwItem).runTaskTimer(plugin, 0, 1);
    }

    public void castCommandOfIce(Player p) {
        if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().canUseAbilities(p)) {
            plugin.getWorldGuardManager().notifyDenied(p);
            return;
        }
        if (isOnCooldown(p, "CommandOfIce")) return;
        int cd = plugin.getWeaponsConfig().getInt("frost-scythe.ice-command.cooldown", 45);
        bossBarManager.setCooldown(p, "CommandOfIce", "§b§lᴄᴏᴍᴍᴀɴᴅ ᴏғ ɪᴄᴇ", cd);

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM, 1.0f, 1.0f);

        List<BlockDisplay> iceBlocks = new ArrayList<>();

        Vector[] targetOffsets = {
                new Vector(1.2, 3.0, 1.2),
                new Vector(0.0, 3.6, 1.5),
                new Vector(-1.2, 3.0, 1.2)
        };

        new BukkitRunnable() {
            int spawnTick = 0;
            int launchTick = 0;
            boolean allSpawned = false;

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead()) {
                    iceBlocks.forEach(Entity::remove);
                    this.cancel();
                    return;
                }

                if (!allSpawned) {
                    if (spawnTick % 10 == 0 && iceBlocks.size() < 3) {
                        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_SPAWN_ITEM, 1.0f, 1.0f);

                        BlockDisplay block = p.getWorld().spawn(p.getLocation().add(0, 1.0, 0), BlockDisplay.class, ent -> {
                            ent.setBlock(packedIceData);
                            Transformation t = ent.getTransformation();
                            t.getScale().set(0.0f, 0.0f, 0.0f);
                            t.getTranslation().set(-0.265f, -0.265f, -0.265f);
                            ent.setTransformation(t);
                            ent.setTeleportDuration(3);
                        });
                        plugin.getVisualCleanupManager().track(block);

                        block.setInterpolationDelay(0);
                        block.setInterpolationDuration(36);
                        Transformation t = block.getTransformation();
                        t.getScale().set(0.53f, 0.53f, 0.53f);
                        t.getLeftRotation().rotationXYZ(0.785f, 0.785f, 0.0f);

                        Vector3f center = new Vector3f(0.265f, 0.265f, 0.265f);
                        Vector3f trans = new Vector3f(center);
                        t.getLeftRotation().transform(trans);
                        trans.mul(-1.0f);
                        t.getTranslation().set(trans);

                        block.setTransformation(t);

                        iceBlocks.add(block);
                    }

                    Vector pDir = p.getLocation().getDirection().setY(0);
                    if (pDir.lengthSquared() < 1e-4 || Double.isNaN(pDir.getX())) {
                        pDir = new Vector(1, 0, 0);
                    } else {
                        pDir.normalize();
                    }
                    Vector pRight = pDir.clone().crossProduct(new Vector(0, 1, 0));
                    if (pRight.lengthSquared() < 1e-4 || Double.isNaN(pRight.getX())) {
                        pRight = new Vector(0, 0, 1);
                    } else {
                        pRight.normalize();
                    }
                    Location pLoc = p.getLocation();

                    for (int i = 0; i < iceBlocks.size(); i++) {
                        BlockDisplay b = iceBlocks.get(i);
                        Vector offset = targetOffsets[i];

                        double bobbing = Math.sin((spawnTick + i * 30) * 0.05) * 0.25;

                        Location targetLoc = pLoc.clone()
                                .add(pRight.clone().multiply(offset.getX()))
                                .add(0, offset.getY() + bobbing, 0)
                                .add(pDir.clone().multiply(offset.getZ()));

                        b.teleport(targetLoc);

                        if (spawnTick % 5 == 0) {
                            b.getWorld().spawnParticle(Particle.SNOWFLAKE, targetLoc, 2, 0.3, 0.3, 0.3, 0.0);
                            b.getWorld().spawnParticle(Particle.ENCHANTED_HIT, targetLoc, 6, 0.9, 0.9, 0.9, 0.0);
                            b.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, targetLoc, 2, 0.3, 0.3, 0.3, 0.0);
                        }
                    }

                    if (iceBlocks.size() == 3 && spawnTick >= 60) {
                        allSpawned = true;
                    }
                    spawnTick++;
                } else {
                    if (launchTick % 3 == 0) {
                        int indexToLaunch = launchTick / 3;
                        if (indexToLaunch < 3) {
                            BlockDisplay blockToLaunch = iceBlocks.get(indexToLaunch);
                            if (blockToLaunch.isValid()) {
                                launchIceMissile(p, blockToLaunch, targetOffsets[indexToLaunch]);
                            }
                        } else {
                            this.cancel();
                        }
                    }
                    launchTick++;
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void launchIceMissile(Player p, BlockDisplay block, Vector offset) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITCH_THROW, 1.0f, 1.0f);

        final float rotX = (ThreadLocalRandom.current().nextFloat() * 0.6f + 0.2f) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
        final float rotY = (ThreadLocalRandom.current().nextFloat() * 0.6f + 0.2f) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
        final float rotZ = (ThreadLocalRandom.current().nextFloat() * 0.6f + 0.2f) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);

        final double speed = plugin.getWeaponsConfig().getDouble("frost-scythe.ice-command.speed", 1.25);

        new BukkitRunnable() {
            int life = 0;
            double distanceTraveled = 0;

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead() || life > 200 || !block.isValid() || distanceTraveled >= 50.0) {
                    breakIce(block.getLocation());
                    block.remove();
                    this.cancel();
                    return;
                }

                distanceTraveled += speed;

                Vector dynamicDir = p.getEyeLocation().getDirection();
                if (dynamicDir.lengthSquared() < 1e-4 || Double.isNaN(dynamicDir.getX())) {
                    dynamicDir = new Vector(0, 0, 1);
                } else {
                    dynamicDir.normalize();
                }

                Vector dynamicRight = dynamicDir.clone().crossProduct(new Vector(0, 1, 0));
                if (dynamicRight.lengthSquared() < 1e-4 || Double.isNaN(dynamicRight.getX())) {
                    dynamicRight = new Vector(1, 0, 0);
                } else {
                    dynamicRight.normalize();
                }

                Location current = p.getLocation().clone()
                        .add(dynamicRight.clone().multiply(offset.getX()))
                        .add(0, offset.getY(), 0)
                        .add(dynamicDir.clone().multiply(offset.getZ() + distanceTraveled));

                block.setInterpolationDelay(0);
                block.setInterpolationDuration(1);
                block.teleport(current);

                Transformation t = block.getTransformation();
                t.getLeftRotation().rotateXYZ(rotX, rotY, rotZ);

                Vector3f center = new Vector3f(0.265f, 0.265f, 0.265f);
                Vector3f trans = new Vector3f(center);
                t.getLeftRotation().transform(trans);
                trans.mul(-1.0f);
                t.getTranslation().set(trans);

                block.setTransformation(t);

                current.getWorld().spawnParticle(Particle.SNOWFLAKE, current, 10, 0.4, 0.4, 0.4, 0.05);

                if (current.getBlock().getType().isSolid()
                        || (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().isLocationAllowed(current, p))) {
                    breakIce(current);
                    block.remove();
                    this.cancel();
                    return;
                }

                for (Entity e : current.getWorld().getNearbyEntities(current, 1.0, 1.0, 1.0)) {
                    if (e instanceof LivingEntity victim && e != p) {

                        if (victim instanceof Player targetPlayer) {
                            if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                            if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) continue;
                        }

                        if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().canAffectTarget(p, victim)) {
                            continue;
                        }

                        breakIce(current);
                        CombatUtils.runSyntheticDamage(() -> victim.damage(plugin.getWeaponsConfig().getDouble("frost-scythe.ice-command.damage", 15.0), p));
                        applyFreeze(p, victim, 5, 2);
                        block.remove();
                        this.cancel();
                        return;
                    }
                }
                life++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void breakIce(Location loc) {
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.0f);
        Location baseLoc = loc.clone().add(0, 0.2, 0);
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, baseLoc, 25, 0.35, 0.35, 0.35, 0.06);
        ParticleUtils.spawnBlockDispersion(plugin, baseLoc, blueIceData, 4.2, 85);
    }

    public void applyFreeze(Player p, LivingEntity victim, int seconds, int slownessAmp) {
        breakIce(victim.getLocation().add(0, 1, 0));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20, slownessAmp, false, false));
        victim.setFreezeTicks(victim.getMaxFreezeTicks() + (seconds * 20));

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= (seconds * 20) || victim.isDead()) {
                    this.cancel();
                    return;
                }

                if (ticks > 0 && ticks % 60 == 0) {
                    CombatUtils.runSyntheticDamage(() -> victim.damage(plugin.getWeaponsConfig().getDouble("frost-scythe.freeze.damage", 2.0), p));
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}

