package dev.whersss.altarLegendaryWH.weapons.witherblade.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.witherblade.managers.WitherManager;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class WitherTasks {

    private static final Random random = new Random();
    private static final Particle.DustOptions BLACK_DUST = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f);
    public static final Set<UUID> activeSlimeTraps = new HashSet<>();

    public static void castDash(AltarLegendaryWH plugin, Player p, WitherManager manager) {
        if (manager.getDashCharges(p) <= 0) return;
        manager.useDashCharge(p);

        p.swingMainHand();
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f);
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SKELETON_HURT, 1.0f, 1.0f);

        double velocity = plugin.getWeaponsConfig().getDouble("wither-blade.dash.velocity", 1.5);
        p.setVelocity(p.getLocation().getDirection().normalize().multiply(velocity));

        int duration = plugin.getWeaponsConfig().getInt("wither-blade.dash.speed-duration", 4) * 20;
        int amplifier = plugin.getWeaponsConfig().getInt("wither-blade.dash.speed-amplifier", 1);

        dev.whersss.altarLegendaryWH.utils.SpeedBuffUtils.applyBurstSpeed(p, duration, amplifier, plugin);

        new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> hitDuringDash = new HashSet<>();

            @Override
            public void run() {
                if (ticks > 15 || !p.isOnline() || (ticks > 5 && ((Entity) p).isOnGround())) {
                    cancel(); return;
                }
                Location loc = p.getLocation().add(0, 0.8, 0);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.3, 0.3, 0.3, 0.1, BLACK_DUST);

                for (Entity entity : p.getWorld().getNearbyEntities(p.getLocation(), 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && target != p && !hitDuringDash.contains(target.getUniqueId())) {
                        if (target instanceof Player targetPlayer) {
                            if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                            if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) continue;
                        }
                        hitDuringDash.add(target.getUniqueId());
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 3, false, false));
                    }
                }

                ItemDisplay drop = loc.getWorld().spawn(loc, ItemDisplay.class);
                plugin.getVisualCleanupManager().track(drop);
                drop.setItemStack(new ItemStack(Material.BONE));
                drop.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);

                drop.setInterpolationDuration(1);
                drop.setTeleportDuration(1);

                Transformation t = drop.getTransformation();
                t.getScale().set(0.5f, 0.5f, 0.5f);
                drop.setTransformation(t);

                Vector3f rot = new Vector3f(random.nextFloat() * 0.3f, random.nextFloat() * 0.3f, random.nextFloat() * 0.3f);
                Vector v = new Vector((random.nextDouble() - 0.5) * 0.25, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.25);

                new BukkitRunnable() {
                    int life = 0;
                    @Override
                    public void run() {
                        Location c = drop.getLocation();
                        v.add(new Vector(0, -0.02, 0));
                        c.add(v);
                        Transformation trans = drop.getTransformation();
                        trans.getLeftRotation().rotateXYZ(rot.x, rot.y, rot.z);
                        drop.setTransformation(trans);
                        drop.teleport(c);
                        if (c.getY() < -64 || life++ > 40) { drop.remove(); cancel(); }
                    }
                }.runTaskTimer(plugin, 0, 1);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public static void castAura(AltarLegendaryWH plugin, Player p, WitherManager manager) {
        int charge = manager.getAttackCharge(p);
        if (charge <= 0) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.7f, 1.2f);
            return;
        }

        p.swingMainHand();

        manager.setAuraCooldown(p);
        manager.resetAttackCharge(p);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 0.5f);

        double maxDamage = plugin.getWeaponsConfig().getDouble("wither-blade.aura.max-damage", 10.0);
        double range = Math.max(1.0, charge);

        new BukkitRunnable() {
            double currentRadius = 0;
            @Override
            public void run() {
                if (currentRadius >= range) { this.cancel(); return; }
                currentRadius += 1.5;

                int particles = (int) (currentRadius * 40);
                for (int i = 0; i < particles; i++) {
                    double r = currentRadius - (random.nextDouble() * 1.5);
                    if (r < 0) r = 0;

                    double angle = random.nextDouble() * 2 * Math.PI;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    double y = random.nextDouble() * 2.0;

                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(x, y, z), 2, 0.4, 0.4, 0.4, 0.0, BLACK_DUST);
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity victim && e != p) {
                if (victim instanceof Player targetPlayer) {
                    if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                    if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) continue;
                }

                double dist = p.getLocation().distance(victim.getLocation());
                if (dist > range) continue;

                double distFactor = Math.max(0.1, 1.0 - (dist / range));
                if (dist < 2.0) distFactor = 1.0;

                double chargeFactor = charge / 30.0;
                double finalDamage = maxDamage * chargeFactor * distFactor;
                int witherTicks = (int) (400 * chargeFactor * distFactor);

                plugin.getCleanDamageManager().apply(victim, p, finalDamage);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherTicks, 1, false, false));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, witherTicks, 1, false, false));
                victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1, 0), 1);
            }
        }

        int baseSlimeCount = Math.min(35, charge + 15);
        int slimeCount = (int) (baseSlimeCount * 1.4);

        for (int i = 0; i < slimeCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            boolean isFlowing = random.nextDouble() < 0.4;
            Vector dir;
            Location startLoc = p.getLocation().add(0, 0.3 + (random.nextDouble() * 1.2), 0);

            if (isFlowing) {
                double speed = 0.1 + random.nextDouble() * 0.15;
                dir = new Vector(Math.cos(angle) * speed, -0.1 - random.nextDouble() * 0.2, Math.sin(angle) * speed);
            } else {
                double speed = 0.4 + random.nextDouble() * 0.4;
                dir = new Vector(Math.cos(angle) * speed, 0.2 + random.nextDouble() * 0.6, Math.sin(angle) * speed);
            }

            launchSlimeProjectile(plugin, p, startLoc, dir, range, false);
        }
    }

    public static void launchSlimeProjectile(AltarLegendaryWH plugin, Player shooter, Location start, Vector dir, double maxDist, boolean isDrip) {
        ItemDisplay slime = start.getWorld().spawn(start, ItemDisplay.class);
        plugin.getVisualCleanupManager().track(slime);
        ItemStack slimeItem = new ItemStack(Material.FEATHER);
        ItemMeta meta = slimeItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(4);
            slimeItem.setItemMeta(meta);
        }
        slime.setItemStack(slimeItem);
        slime.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);

        slime.setTeleportDuration(1);
        slime.setInterpolationDuration(1);

        Transformation t = slime.getTransformation();
        t.getScale().set(0.3f, 0.3f, 0.3f);
        t.getTranslation().set(-0.15f, -0.15f, -0.15f);
        slime.setTransformation(t);

        new BukkitRunnable() {
            double traveled = 0;
            Location curr = start.clone();

            @Override
            public void run() {
                if (!shooter.isOnline() || traveled >= maxDist || !slime.isValid() || curr.getY() < -64) {
                    slime.remove();
                    this.cancel();
                    return;
                }

                Location prevLoc = curr.clone();
                dir.setY(dir.getY() - 0.05);
                curr.add(dir);
                traveled += dir.length();

                slime.teleport(curr);

                Transformation flyTransform = slime.getTransformation();
                flyTransform.getLeftRotation().rotateXYZ(0.1f, 0.2f, 0.15f);
                slime.setTransformation(flyTransform);

                if (traveled % 1.0 < dir.length()) {
                    curr.getWorld().spawnParticle(Particle.DUST, curr, 1, 0.1, 0.1, 0.1, 0.0, BLACK_DUST);
                }

                RayTraceResult ray = curr.getWorld().rayTraceBlocks(prevLoc, dir.clone().normalize(), dir.length(), FluidCollisionMode.NEVER, true);
                if (ray != null && ray.getHitBlock() != null) {
                    createSlimeTrap(plugin, shooter, slime, ray.getHitPosition().toLocation(curr.getWorld()), ray.getHitBlockFace(), isDrip);
                    this.cancel();
                    return;
                }

                for (Entity e : curr.getWorld().getNearbyEntities(curr, 0.6, 0.6, 0.6)) {
                    if (e instanceof LivingEntity victim && !victim.equals(shooter)) {
                        if (victim instanceof Player p) {
                            if (p.getGameMode() == GameMode.SPECTATOR) continue;
                            if (plugin.getFriendManager().isFriend(shooter.getUniqueId(), p.getUniqueId())) continue;
                        }

                        stickSlimeToEntity(plugin, shooter, slime, victim);
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private static void createSlimeTrap(AltarLegendaryWH plugin, Player shooter, ItemDisplay slime, Location loc, BlockFace face, boolean isDrip) {
        Location offsetLoc = loc.clone().add(face.getDirection().multiply(0.01));
        slime.teleport(offsetLoc);

        slime.getWorld().playSound(offsetLoc, Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.8f);
        slime.getWorld().spawnParticle(Particle.DUST, offsetLoc, 10, 0.15, 0.15, 0.15, 0.0, BLACK_DUST);

        slime.setInterpolationDelay(0);
        slime.setInterpolationDuration(6);
        Transformation t = slime.getTransformation();

        float thick = 0.03f + (random.nextFloat() * 0.03f);
        float stretch1 = 0.5f + (random.nextFloat() * 0.3f);
        float stretch2 = 0.5f + (random.nextFloat() * 0.3f);

        t.getLeftRotation().identity();
        t.getRightRotation().identity();

        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            t.getScale().set(stretch1, thick, stretch2);
        } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
            t.getScale().set(stretch1, stretch2, thick);
        } else {
            t.getScale().set(thick, stretch1, stretch2);
        }

        t.getTranslation().set(-t.getScale().x() / 2, -t.getScale().y() / 2, -t.getScale().z() / 2);
        slime.setTransformation(t);

        if (!isDrip) {
            Location checkLoc = offsetLoc.clone().add(0, -0.6, 0);
            if (!checkLoc.getBlock().getType().isSolid()) {
                Vector dripDir = new Vector(0, -0.15, 0);
                launchSlimeProjectile(plugin, shooter, checkLoc, dripDir, 8.0, true);
            }
        }

        UUID trapId = slime.getUniqueId();
        activeSlimeTraps.add(trapId);

        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life >= 200 || !slime.isValid()) {
                    activeSlimeTraps.remove(trapId);
                    slime.remove();
                    this.cancel();
                    return;
                }

                if (life % 10 == 0) offsetLoc.getWorld().spawnParticle(Particle.DUST, offsetLoc, 1, 0.2, 0.2, 0.2, 0.0, BLACK_DUST);

                for (Entity e : offsetLoc.getWorld().getNearbyEntities(offsetLoc, 0.7, 0.7, 0.7)) {
                    if (e instanceof LivingEntity victim && !victim.equals(shooter)) {
                        if (victim instanceof Player targetPlayer) {
                            if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                            if (plugin.getFriendManager().isFriend(shooter.getUniqueId(), targetPlayer.getUniqueId())) continue;
                        }

                        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1, false, false));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, false));
                        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_SLIME_HURT, 1.0f, 1.0f);

                        activeSlimeTraps.remove(trapId);
                        slime.remove();
                        this.cancel();
                        return;
                    }
                }
                life++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private static void stickSlimeToEntity(AltarLegendaryWH plugin, Player shooter, ItemDisplay slime, LivingEntity victim) {
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_SLIME_ATTACK, 1.0f, 1.0f);

        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life >= 100 || !victim.isValid() || victim.isDead() || !slime.isValid()) {
                    slime.remove();
                    this.cancel();
                    return;
                }

                slime.teleport(victim.getLocation().add(0, victim.getHeight() / 2, 0));

                if (life % 20 == 0) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1, false, false));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.5f, 1.5f);
                    victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0, BLACK_DUST);
                }

                life++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
