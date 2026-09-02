package dev.whersss.altarLegendaryWH.weapons.boneblade.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class BladeListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final Set<UUID> stunnedEntities = new HashSet<>();
    private final Random random = new Random();

    private final Particle.DustOptions BONE_COLOR = new Particle.DustOptions(Color.fromRGB(183, 178, 157), 1.5f);
    private final Particle.DustOptions RING_BONE_COLOR = new Particle.DustOptions(Color.fromRGB(183, 178, 157), 0.75f);

    public BladeListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return;
        if (!item.getItemMeta().hasCustomModelData() || item.getItemMeta().getCustomModelData() != 1) return;

        event.setCancelled(true);

        if (plugin.isAboveLegendaryHeight(player)) return;

        if (player.isSneaking()) {
            if (plugin.getBoneCooldownManager().isOnCageCooldown(player)) return;
            player.swingMainHand();
            castBoneCage(player);
        } else {
            if (plugin.getBoneCooldownManager().isOnDashCooldown(player)) return;
            player.swingMainHand();
            castSkeletalLeap(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (stunnedEntities.contains(event.getPlayer().getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                Location newLoc = from.clone();
                newLoc.setPitch(to.getPitch());
                newLoc.setYaw(to.getYaw());
                event.setTo(newLoc);
            }
        }
    }

    private void castSkeletalLeap(Player player) {
        plugin.getBoneCooldownManager().setDashCooldown(player, plugin.getWeaponsConfig().getInt("bone-blade.dash.cooldown", 15));
        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0f, 0.8f);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0f, 0.8f);
                }
            }
        }.runTaskLater(plugin, 2L);
        double velocity = plugin.getWeaponsConfig().getDouble("bone-blade.dash.velocity", 1.5);
        player.setVelocity(player.getLocation().getDirection().normalize().multiply(velocity));

        int duration = plugin.getWeaponsConfig().getInt("bone-blade.dash.speed-duration", 3) * 20;
        int amplifier = plugin.getWeaponsConfig().getInt("bone-blade.dash.speed-amplifier", 2) - 1;

        PotionEffect oldSpeed = player.getPotionEffect(PotionEffectType.SPEED);
        boolean hadSpeed = oldSpeed != null && oldSpeed.getAmplifier() < amplifier;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, false, false));

        if (hadSpeed) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.addPotionEffect(oldSpeed);
                    }
                }
            }.runTaskLater(plugin, duration);
        }

        new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> hitDuringDash = new HashSet<>();
            final double dashDamage = plugin.getWeaponsConfig().getDouble("bone-blade.dash.damage", 4.0);
            final double knockbackY = plugin.getWeaponsConfig().getDouble("bone-blade.dash.knockback-y", 1.1);

            @Override
            public void run() {
                if (ticks > 100 || !player.isOnline() || (ticks > 5 && ((Entity) player).isOnGround())) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation().add(0, 0.8, 0);
                spawnSmallFallingBone(loc.clone().add(random.nextDouble() - 0.5, -0.2, random.nextDouble() - 0.5), true);

                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && target != player && !hitDuringDash.contains(target.getUniqueId())) {
                        if (target instanceof Player targetPlayer) {
                            if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                            if (plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) continue;
                        }
                        hitDuringDash.add(target.getUniqueId());
                        CombatUtils.runSyntheticDamage(() -> target.damage(dashDamage, player));
                        target.setVelocity(target.getVelocity().add(new Vector(0, knockbackY, 0)));

                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SKELETON_HURT, 1.0f, 0.8f);

                        new BukkitRunnable() {
                            int kTicks = 0;
                            @Override
                            public void run() {
                                if (kTicks > 15 || !target.isValid()) {
                                    cancel();
                                    return;
                                }
                                Location tLoc = target.getLocation().add(0, 1, 0);
                                target.getWorld().spawnParticle(Particle.DUST, tLoc, 8, 0.4, 0.4, 0.4, 0.1, BONE_COLOR);

                                if (kTicks % 3 == 0) {
                                    spawnSmallFallingBone(tLoc.clone().add(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5), true);
                                }
                                kTicks++;
                            }
                        }.runTaskTimer(plugin, 0, 1);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void castBoneCage(Player player) {
        int fullCd = plugin.getWeaponsConfig().getInt("bone-blade.cage.cooldown", 45);
        plugin.getBoneCooldownManager().setCageCooldown(player, fullCd);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0f, 0.8f);

        Location startLoc = player.getEyeLocation();
        Vector dir = startLoc.getDirection().normalize().multiply(1.2);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (ticks++ > 15) {
                    cancel();
                    return;
                }

                Location previousLoc = currentLoc.clone();
                currentLoc.add(dir);

                for (int i = 0; i < 3; i++) {
                    spawnSmallFallingBone(currentLoc.clone().add(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5), true);
                }

                RayTraceResult ray = currentLoc.getWorld().rayTraceBlocks(previousLoc, dir, dir.length() + 0.5, FluidCollisionMode.NEVER, true);

                if (ray != null && ray.getHitBlock() != null) {
                    playImpactExplosion(ray.getHitPosition().toLocation(currentLoc.getWorld()));
                    cancel();
                    return;
                }

                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.9, 0.9, 0.9)) {
                    if (entity instanceof LivingEntity target && target != player && !stunnedEntities.contains(target.getUniqueId())) {
                        if (target instanceof Player targetPlayer) {
                            if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                            if (plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) continue;
                        }

                        playImpactExplosion(currentLoc);
                        applyBoneStun(target);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void applyBoneStun(LivingEntity target) {
        stunnedEntities.add(target.getUniqueId());
        int stunTicks = plugin.getWeaponsConfig().getInt("bone-blade.cage.stun-duration", 4) * 20;

        if (target instanceof Player p) {
            Component titleText = TextUtils.legacy(plugin.tr("&eОглушен!", "&eStunned!"));
            Title title = Title.title(titleText, TextUtils.empty(), Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(stunTicks * 50L / 2), Duration.ofMillis(250)));
            p.showTitle(title);
        } else {
            target.setAI(false);
        }

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.0f);

        BlockDisplay[] blocks = new BlockDisplay[3];
        Vector3f[] rotationSpeeds = new Vector3f[3];

        for (int i = 0; i < 3; i++) {
            blocks[i] = target.getWorld().spawn(target.getLocation().add(0, 1.0, 0), BlockDisplay.class);
            blocks[i].setBlock(Material.BONE_BLOCK.createBlockData());
            blocks[i].setTeleportDuration(1);
            rotationSpeeds[i] = new Vector3f((random.nextFloat() * 0.25f) - 0.125f, (random.nextFloat() * 0.25f) - 0.125f, (random.nextFloat() * 0.25f) - 0.125f);

            Transformation trans = new Transformation(new Vector3f(-0.15f, 0, -0.15f), new Quaternionf(), new Vector3f(0.0f, 0.0f, 0.0f), new Quaternionf());
            blocks[i].setTransformation(trans);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                boolean isSpectator = (target instanceof Player p && p.getGameMode() == GameMode.SPECTATOR);

                if (!target.isValid() || isSpectator || (target instanceof Player && !((Player) target).isOnline()) || ticks >= stunTicks) {
                    for (BlockDisplay b : blocks) b.remove();
                    stunnedEntities.remove(target.getUniqueId());
                    if (!(target instanceof Player)) {
                        target.setAI(true);
                    }
                    cancel();
                    return;
                }

                double currentScale = 0.35;
                double targetRadius = 1.2;
                double heightY = 1.0;

                if (ticks < 10) {
                    currentScale = (ticks / 10.0) * 0.35;
                } else if (ticks > stunTicks - 10) {
                    currentScale = ((stunTicks - ticks) / 10.0) * 0.35;
                }

                double radius = (currentScale / 0.35) * targetRadius;

                if (ticks >= 10 && ticks <= stunTicks - 10) {
                    double wave = Math.sin(ticks * 0.2) * 0.22;
                    radius += wave;
                    currentScale += wave * 0.15;
                }

                double angle = ticks * 0.25;
                Location center = target.getLocation();

                if (radius > 0.05) {
                    for (int i = 0; i < 15; i++) {
                        double pAngle = (2 * Math.PI / 15) * i;
                        double px = Math.cos(pAngle) * radius;
                        double pz = Math.sin(pAngle) * radius;
                        target.getWorld().spawnParticle(Particle.DUST, center.clone().add(px, heightY, pz), 1, 0, 0, 0, 0, RING_BONE_COLOR);
                    }
                }

                for (int i = 0; i < 3; i++) {
                    double offset = (2.0 * Math.PI / 3.0) * i;

                    blocks[i].setInterpolationDelay(0);
                    blocks[i].setInterpolationDuration(1);

                    Transformation t = blocks[i].getTransformation();
                    t.getScale().set((float) Math.max(0.01, currentScale), (float) Math.max(0.01, currentScale), (float) Math.max(0.01, currentScale));
                    t.getLeftRotation().rotateXYZ(rotationSpeeds[i].x, rotationSpeeds[i].y, rotationSpeeds[i].z);
                    blocks[i].setTransformation(t);

                    blocks[i].teleport(center.clone().add(Math.cos(angle + offset) * radius, heightY, Math.sin(angle + offset) * radius));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void playImpactExplosion(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_SKELETON_STEP, 1.0f, 0.5f);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 15, 0.4, 0.4, 0.4, 0.1, BONE_COLOR);

        for (int i = 0; i < 45; i++) {
            ItemDisplay bone = loc.getWorld().spawn(loc, ItemDisplay.class);
            bone.setItemStack(new ItemStack(Material.BONE));
            bone.setInterpolationDuration(1);
            bone.setTeleportDuration(1);
            Transformation t = bone.getTransformation();
            t.getScale().set(0.46f, 0.46f, 0.46f);
            bone.setTransformation(t);

            Vector v = new Vector((random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.3);
            Vector3f rot = new Vector3f(random.nextFloat() * 0.4f, random.nextFloat() * 0.4f, random.nextFloat() * 0.4f);

            new BukkitRunnable() {
                final Vector vel = v.clone();
                int life = 0;

                @Override
                public void run() {
                    Location c = bone.getLocation();
                    vel.add(new Vector(0, -0.025, 0));
                    c.add(vel);
                    Transformation trans = bone.getTransformation();
                    trans.getLeftRotation().rotateXYZ(rot.x, rot.y, rot.z);
                    bone.setTransformation(trans);
                    bone.teleport(c);

                    if (c.getY() < -64 || life++ > 60) {
                        bone.remove();
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    private void spawnSmallFallingBone(Location loc, boolean hasTrail) {
        ItemDisplay bone = loc.getWorld().spawn(loc, ItemDisplay.class);
        bone.setItemStack(new ItemStack(Material.BONE));
        bone.setInterpolationDuration(1);
        bone.setTeleportDuration(1);
        Transformation t = bone.getTransformation();
        t.getScale().set(0.4025f, 0.4025f, 0.4025f);
        bone.setTransformation(t);

        Vector3f rot = new Vector3f(random.nextFloat() * 0.3f, random.nextFloat() * 0.3f, random.nextFloat() * 0.3f);

        new BukkitRunnable() {
            int life = 0;
            final Vector v = new Vector((random.nextDouble() - 0.5) * 0.25, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.25);

            @Override
            public void run() {
                Location c = bone.getLocation();
                v.add(new Vector(0, -0.02, 0));
                c.add(v);
                Transformation trans = bone.getTransformation();
                trans.getLeftRotation().rotateXYZ(rot.x, rot.y, rot.z);
                bone.setTransformation(trans);
                bone.teleport(c);

                if (hasTrail) {
                    c.getWorld().spawnParticle(Particle.DUST, c, 1, 0.05, 0.05, 0.05, 0.0, BONE_COLOR);
                }

                if (c.getY() < -64 || life++ > 40) {
                    bone.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.getKiller() == null) return;
        Player killer = victim.getKiller();
        ItemStack item = killer.getInventory().getItemInMainHand();

        if (item.getType() == Material.NETHERITE_SWORD && item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 1) {
            Location loc = victim.getLocation().add(0, 1, 0);
            victim.getWorld().playSound(loc, Sound.ENTITY_SKELETON_DEATH, 1.2f, 0.8f);
            victim.getWorld().spawnParticle(Particle.DUST, loc, 20, 0.4, 0.4, 0.4, 0.1, BONE_COLOR);
            for (int i = 0; i < 8; i++) spawnSmallFallingBone(loc.clone().add(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5), false);
        }
    }
}

