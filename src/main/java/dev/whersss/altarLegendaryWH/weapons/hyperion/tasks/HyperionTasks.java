package dev.whersss.altarLegendaryWH.weapons.hyperion.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.weapons.hyperion.managers.HyperionCooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HyperionTasks {

    public static final Set<UUID> hallowedVictims = new HashSet<>();

    public static class HallowedFlamesTask extends BukkitRunnable {
        private final LivingEntity target;
        private final Player attacker;
        private int ticks = 0;
        private BossBar bar;

        public HallowedFlamesTask(LivingEntity target) {
            this(target, null);
        }

        public HallowedFlamesTask(LivingEntity target, Player attacker) {
            this.target = target;
            this.attacker = attacker;
            hallowedVictims.add(target.getUniqueId());

            if (target instanceof Player p) {
                bar = TextUtils.bossBar(
                        AltarLegendaryWH.getInstance().tr("§6§l!! §c§lСВЯТОЕ ПЛАМЯ §6§l!!", "§6§l!! §c§lHOLY FLAME §6§l!!"),
                        BarColor.RED,
                        BarStyle.SOLID
                );
                bar.addPlayer(p);
            }
        }

        @Override
        public void run() {
            if (ticks >= 100 || target.isDead() || !target.isValid()) {
                if (bar != null) bar.removeAll();
                target.setFireTicks(0);
                hallowedVictims.remove(target.getUniqueId());
                this.cancel();
                return;
            }
            target.setFireTicks(20);

            if (ticks % 20 == 0) {
                double reducedFireDamage = 1.35;
                if (attacker != null && attacker.isValid()) {
                    AltarLegendaryWH.getInstance().getCleanDamageManager().apply(target, attacker, reducedFireDamage);
                } else {
                    CombatUtils.runSyntheticDamage(() -> target.damage(reducedFireDamage));
                }
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 0.6f);
            }

            if (bar != null) bar.setProgress(Math.max(0.0, 1.0 - (ticks / 100.0)));

            if (ticks % 4 == 0) {
                target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 4, 0.3, 0.4, 0.3, 0.02);
                target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 3, 0.3, 0.4, 0.3, 0.02);
            }
            ticks++;
        }
    }

    public static class ScorchingChargeTask extends BukkitRunnable {
        private static final Set<Player> chargingPlayers = new HashSet<>();
        private final Player p;
        private final BossBar bar;
        private final Location startLoc;
        private int ticks = 0;

        public static boolean isCharging(Player p) {
            return chargingPlayers.contains(p);
        }

        public ScorchingChargeTask(Player p) {
            this.p = p;
            chargingPlayers.add(p);
            this.bar = TextUtils.bossBar(
                    AltarLegendaryWH.getInstance().tr("§6!! §e§lОБЖИГАНИЕ §6!!", "§6!! §e§lSCORCHING §6!!"),
                    BarColor.YELLOW,
                    BarStyle.SOLID
            );
            this.bar.addPlayer(p);
            this.bar.setProgress(1.0);

            this.startLoc = p.getLocation().clone();
            p.getWorld().playSound(startLoc, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
        }

        @Override
        public void run() {
            if (!p.isOnline() || p.isDead() || ticks >= 20) {
                bar.removeAll();
                chargingPlayers.remove(p);
                if (ticks >= 20) {
                    p.swingMainHand();
                    try {
                        p.resetCooldown();
                    } catch (Throwable ignored) {}
                    Location eyeLoc = p.getEyeLocation();
                    Location sweepLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(1.5));
                    p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, sweepLoc, 1, 0.0, 0.0, 0.0, 0.0);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.0f);
                    HyperionCooldownManager.setScorchingCooldown(p);
                    new ScorchingBladeTask(p).runTaskTimer(AltarLegendaryWH.getInstance(), 0, 1);
                }
                this.cancel();
                return;
            }
            if (ticks == 0) {
                Location center = startLoc.clone().add(0, 0.30, 0);
                int rays = 64;
                for (int i = 0; i < rays; i++) {
                    double angle = i * (2.0 * Math.PI) / rays;
                    double dx = Math.cos(angle);
                    double dz = Math.sin(angle);
                    Location spawnPoint = center.clone().add(dx * 0.4, 0.0, dz * 0.4);
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, spawnPoint, 0, dx, 0.02, dz, 0.15);
                    center.getWorld().spawnParticle(Particle.FLAME, spawnPoint, 0, dx, 0.02, dz, 0.25);
                    center.getWorld().spawnParticle(Particle.FLAME, spawnPoint, 0, dx, 0.02, dz, 0.35);
                }
            }
            ticks++;
        }
    }

    public static class ScorchingBladeTask extends BukkitRunnable {
        private static final int DISPLAY_COUNT = 45;
        private final Player shooter;
        private final BlockDisplay[] blocks = new BlockDisplay[DISPLAY_COUNT];
        private final Location currentLoc;
        private final Vector dir;
        private final Vector tiltRight;
        private int ticks = 0;
        private double totalWidth = 1.25;
        private boolean isLavaPhase = false;

        private final Set<UUID> hitVictims = new HashSet<>();

        public ScorchingBladeTask(Player shooter) {
            this.shooter = shooter;

            this.currentLoc = shooter.getEyeLocation().clone();
            this.dir = currentLoc.getDirection().normalize();

            if (dir.lengthSquared() == 0) dir.copy(new Vector(1, 0, 0));

            Vector up = new Vector(0, 1, 0);
            if (Math.abs(dir.getY()) > 0.99) {
                up = new Vector(1, 0, 0);
            }
            Vector right = dir.clone().crossProduct(up).normalize();

            double radAngle = Math.toRadians(22.5);
            double cosAngle = Math.cos(radAngle);
            double sinAngle = Math.sin(radAngle);
            this.tiltRight = right.clone().multiply(cosAngle).add(up.clone().multiply(sinAngle)).normalize();

            shooter.getWorld().playSound(currentLoc, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.8f);

            for (int i = 0; i < DISPLAY_COUNT; i++) {
                BlockDisplay bd = shooter.getWorld().spawn(currentLoc, BlockDisplay.class);
                AltarLegendaryWH.getInstance().getVisualCleanupManager().track(bd);
                bd.setBlock(Material.FIRE.createBlockData());
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(1);

                Transformation t = bd.getTransformation();
                t.getScale().set(1.375f, 2.2f, 1.375f);
                t.getTranslation().set(-0.6875f, -1.1f, -0.6875f);

                float yaw = (float) Math.toRadians(currentLoc.getYaw());
                float pitch = (float) Math.toRadians(currentLoc.getPitch());
                t.getLeftRotation().rotationY(-yaw).rotationX(-pitch);
                bd.setTransformation(t);

                blocks[i] = bd;
            }
        }

        @Override
        public void run() {
            int maxTicks = 32;
            int lavaPhaseTick = 24;

            if (ticks >= maxTicks) {
                for (int i = 0; i < DISPLAY_COUNT; i++) {
                    if (blocks[i] != null && blocks[i].isValid()) {
                        if (isLavaPhase) {
                            spawnRealLavaColumn(blocks[i].getLocation(), i, DISPLAY_COUNT - 1);
                        }
                        AltarLegendaryWH.getInstance().getVisualCleanupManager().untrack(blocks[i]);
                        blocks[i].remove();
                    }
                }
                this.cancel();
                return;
            }
            if (ticks >= lavaPhaseTick && !isLavaPhase) {
                isLavaPhase = true;
                currentLoc.getWorld().playSound(currentLoc, Sound.ITEM_BUCKET_EMPTY_LAVA, 1.2f, 0.8f);
                for (int i = 0; i < DISPLAY_COUNT; i++) {
                    if (blocks[i] != null && blocks[i].isValid()) {
                        blocks[i].setBlock(Material.LAVA.createBlockData());
                    }
                }
            }

            currentLoc.add(dir.clone().multiply(1.2));
            totalWidth += 0.95;

            float lengthShrink = 1.0f;
            if (ticks >= maxTicks - 4) {
                lengthShrink = (float) (maxTicks - ticks) / 4.0f;
                if (lengthShrink < 0.05f) lengthShrink = 0.05f;
            }

            double pureDamage = AltarLegendaryWH.getInstance().getWeaponsConfig().getDouble("hyperion.scorching-blade.pure-damage", 6.0);
            double curveDepth = 1.3;

            for (int i = 0; i < DISPLAY_COUNT; i++) {
                if (blocks[i] == null || !blocks[i].isValid()) continue;

                double norm = -1.0 + (2.0 * i / (DISPLAY_COUNT - 1.0));
                double transverseOffset = norm * (totalWidth / 2.0);
                double depthOffset = -(norm * norm - 1.0) * curveDepth;

                Location bLoc = currentLoc.clone()
                        .add(tiltRight.clone().multiply(transverseOffset))
                        .add(dir.clone().multiply(depthOffset));

                blocks[i].setInterpolationDelay(0);
                blocks[i].setInterpolationDuration(1);

                Transformation t = blocks[i].getTransformation();
                t.getScale().set(1.375f, 2.2f, 1.375f * lengthShrink);
                blocks[i].setTransformation(t);

                blocks[i].teleport(bLoc);

                RayTraceResult ray = bLoc.getWorld().rayTrace(bLoc, dir, 1.3, FluidCollisionMode.NEVER, true, 0.5, e -> e != shooter && e instanceof LivingEntity);

                if (ray != null && ray.getHitEntity() instanceof LivingEntity victim) {
                    if (!hitVictims.contains(victim.getUniqueId())) {
                        boolean isFriend = false;
                        if (victim instanceof Player targetPlayer) {
                            if (AltarLegendaryWH.getInstance().getFriendManager().isFriend(shooter.getUniqueId(), targetPlayer.getUniqueId())) {
                                isFriend = true;
                            }
                        }

                        if (!isFriend) {
                            hitVictims.add(victim.getUniqueId());
                            AltarLegendaryWH.getInstance().getCleanDamageManager().apply(victim, shooter, pureDamage);
                            new HallowedFlamesTask(victim, shooter).runTaskTimer(AltarLegendaryWH.getInstance(), 0, 1);
                        }
                    }
                    if (isLavaPhase) {
                        spawnRealLavaColumn(bLoc, i, DISPLAY_COUNT - 1);
                    }
                }
            }
            if (isLavaPhase) {
                if (ticks % 2 == 0) {
                    for (int i = 0; i < DISPLAY_COUNT; i += 4) {
                        if (blocks[i] != null && blocks[i].isValid()) {
                            spawnRealLavaColumn(blocks[i].getLocation(), i, DISPLAY_COUNT - 1);
                        }
                    }
                }
                currentLoc.getWorld().spawnParticle(Particle.DRIPPING_LAVA, currentLoc, 25, totalWidth / 2, 0.8, totalWidth / 2, 0.1);
                currentLoc.getWorld().spawnParticle(Particle.FALLING_LAVA, currentLoc, 15, totalWidth / 2, 0.8, totalWidth / 2, 0.1);
            } else {
                currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 30, totalWidth / 2, 0.8, totalWidth / 2, 0.1);
                currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 25, totalWidth / 2, 0.8, totalWidth / 2, 0.1);
            }

            ticks++;
        }

        private void spawnRealLavaColumn(Location loc, int index, int maxIndex) {
            Block baseBlock = loc.getBlock();
            int edgeMargin = 4;
            boolean isEdge = (index < edgeMargin) || (index > maxIndex - edgeMargin);

            loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.2f);
            loc.getWorld().playSound(loc, Sound.ENTITY_MAGMA_CUBE_HURT, 0.5f, 0.8f);

            setTempLava(baseBlock);

            if (!isEdge) {
                Block topBlock = baseBlock.getRelative(org.bukkit.block.BlockFace.UP);
                setTempLava(topBlock);
            }
        }

        private void setTempLava(Block block) {
            if (block.getType().isAir() || block.getType() == Material.SHORT_GRASS || block.getType() == Material.TALL_GRASS) {
                org.bukkit.block.data.BlockData oldData = block.getBlockData().clone();
                block.setType(Material.LAVA);

                Bukkit.getScheduler().runTaskLater(AltarLegendaryWH.getInstance(), () -> {
                    if (block.getType() == Material.LAVA) {
                        block.setBlockData(oldData);
                    }
                }, 12L);
            }
        }
    }

    public static class HolyLanceTask extends BukkitRunnable {
        private final Player shooter;
        private final Location targetLoc;
        private int ticks = 0;
        private ItemDisplay lance;

        public HolyLanceTask(Player shooter) {
            this.shooter = shooter;

            org.bukkit.entity.Entity targetEntity = shooter.getTargetEntity(30);
            Block targetBlock = shooter.getTargetBlockExact(30, org.bukkit.FluidCollisionMode.NEVER);

            if (targetEntity != null) {
                this.targetLoc = targetEntity.getLocation().clone().add(0, targetEntity.getHeight() / 2.0, 0);
            } else if (targetBlock != null) {
                this.targetLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
            } else {
                this.targetLoc = shooter.getEyeLocation().add(shooter.getLocation().getDirection().multiply(15));
            }

            this.targetLoc.setYaw(0);
            this.targetLoc.setPitch(0);

            targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1f);

            double radius = 3.3;
            for (int i = 0; i < 360; i += 12) {
                double rad = Math.toRadians(i);
                targetLoc.getWorld().spawnParticle(Particle.FIREWORK, targetLoc.clone().add(Math.cos(rad)*radius, 0.2, Math.sin(rad)*radius), 1, 0,0,0,0);
            }
            targetLoc.getWorld().spawnParticle(Particle.FIREWORK, targetLoc.clone().add(0, 0.2, 0), 15, 0.2, 0.2, 0.2, 0.05);
        }

        @Override
        public void run() {
            if (ticks == 30) {
                AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
                double radDmg = plugin.getWeaponsConfig().getDouble("hyperion.holy-lance.radius-damage", 10.0);
                double centerDmg = plugin.getWeaponsConfig().getDouble("hyperion.holy-lance.center-damage", 14.0);

                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_CONDUIT_DEACTIVATE, 1.0f, 1.0f);

                Block hitBlock = targetLoc.clone().subtract(0, 1, 0).getBlock();
                Location effectLoc = targetLoc.clone().add(0, 0.5, 0);

                try {
                    targetLoc.getWorld().spawnParticle(Particle.valueOf("BLOCK_CRACK"), effectLoc, 150, 1.2, 0.5, 1.2, 0.15, hitBlock.getBlockData());
                } catch (Exception e) {
                    targetLoc.getWorld().spawnParticle(Particle.valueOf("BLOCK"), effectLoc, 150, 1.2, 0.5, 1.2, 0.15, hitBlock.getBlockData());
                }

                Particle.DustOptions yellowDust = new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f);
                try {
                    targetLoc.getWorld().spawnParticle(Particle.DUST, effectLoc, 100, 1.5, 1.0, 1.5, 0.1, yellowDust);
                } catch (IllegalArgumentException e) {
                    targetLoc.getWorld().spawnParticle(Particle.valueOf("REDSTONE"), effectLoc, 100, 1.5, 1.0, 1.5, 0.1, yellowDust);
                }

                for (org.bukkit.entity.Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, 3.3, 3.5, 3.3)) {
                    if (e instanceof LivingEntity victim && e != shooter) {
                        if (victim instanceof Player targetPlayer) {
                            if (plugin.getFriendManager().isFriend(shooter.getUniqueId(), targetPlayer.getUniqueId())) {
                                continue;
                            }
                        }

                        double dist = victim.getLocation().distance(targetLoc);
                        double pureDmg = dist <= 1.5 ? centerDmg : radDmg;

                        AltarLegendaryWH.getInstance().getCleanDamageManager().apply(victim, shooter, pureDmg);
                        new HallowedFlamesTask(victim, shooter).runTaskTimer(AltarLegendaryWH.getInstance(), 0, 1);
                    }
                }

                lance = targetLoc.getWorld().spawn(targetLoc, ItemDisplay.class);
                AltarLegendaryWH.getInstance().getVisualCleanupManager().track(lance);

                ItemStack customLanceItem = new ItemStack(Material.FEATHER);
                ItemMeta meta = customLanceItem.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(3);
                    customLanceItem.setItemMeta(meta);
                }

                lance.setItemStack(customLanceItem);
                lance.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);

                Transformation tStart = lance.getTransformation();
                tStart.getScale().set(0.01f, 0.01f, 0.01f);
                tStart.getTranslation().set(0f, 0f, 0f);
                lance.setTransformation(tStart);

                final float lanceScale = 16.0f;
                final float yOffset = lanceScale / 2f;

                Bukkit.getScheduler().runTaskLater(AltarLegendaryWH.getInstance(), () -> {
                    if (lance != null && lance.isValid()) {
                        lance.setInterpolationDelay(0);
                        lance.setInterpolationDuration(8);
                        Transformation t = lance.getTransformation();
                        t.getScale().set(lanceScale, lanceScale, lanceScale);
                        t.getTranslation().set(0f, yOffset, 0f);
                        t.getLeftRotation().rotationY((float) Math.toRadians(90));
                        lance.setTransformation(t);
                    }
                }, 3L);

                Bukkit.getScheduler().runTaskLater(AltarLegendaryWH.getInstance(), () -> {
                    if (lance != null && lance.isValid()) {
                        lance.setInterpolationDelay(0);
                        lance.setInterpolationDuration(10);
                        Transformation t = lance.getTransformation();
                        t.getScale().set(lanceScale, lanceScale, lanceScale);
                        t.getTranslation().set(0f, yOffset, 0f);
                        t.getLeftRotation().rotationY((float) Math.toRadians(180));
                        lance.setTransformation(t);
                    }
                }, 11L);

                Bukkit.getScheduler().runTaskLater(AltarLegendaryWH.getInstance(), () -> {
                    if (lance != null && lance.isValid()) {
                        lance.setInterpolationDelay(0);
                        lance.setInterpolationDuration(8);
                        Transformation t = lance.getTransformation();
                        t.getScale().set(0.01f, 0.01f, 0.01f);
                        t.getTranslation().set(0f, 0f, 0f);
                        t.getLeftRotation().rotationY((float) Math.toRadians(270));
                        lance.setTransformation(t);
                    }
                }, 21L);

                Bukkit.getScheduler().runTaskLater(AltarLegendaryWH.getInstance(), () -> {
                    if (lance != null) lance.remove();
                }, 30L);
            }

            if (ticks >= 60) {
                this.cancel();
            }
            ticks++;
        }
    }
}

