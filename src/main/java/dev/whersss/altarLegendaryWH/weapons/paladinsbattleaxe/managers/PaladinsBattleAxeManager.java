package dev.whersss.altarLegendaryWH.weapons.paladinsbattleaxe.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.ParticleUtils;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PaladinsBattleAxeManager {

    private final AltarLegendaryWH plugin;
    private final NamespacedKey weaponKey;

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, BossBar> cooldownBars = new ConcurrentHashMap<>();
    private final Set<UUID> absorbingPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Double> absorbedDamage = new ConcurrentHashMap<>();

    public PaladinsBattleAxeManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        this.weaponKey = new NamespacedKey(plugin, "paladins_battle_axe");
    }

    public NamespacedKey getWeaponKey() {
        return weaponKey;
    }

    public boolean isPaladinsBattleAxe(ItemStack item) {
        return WeaponFactory.isAltarWeapon(item, "paladins_battle_axe");
    }

    public void setPaladinsBattleAxeAttributes(ItemStack item, double damage, double attackSpeed) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();

        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        NamespacedKey dmgKey = new NamespacedKey(plugin, "paladin_damage");
        NamespacedKey spdKey = new NamespacedKey(plugin, "paladin_speed");

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(dmgKey, damage - 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                new AttributeModifier(spdKey, attackSpeed - 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
    }

    public boolean isOnCooldown(Player player, String ability) {
        String key = player.getUniqueId() + ":" + ability;
        return cooldowns.getOrDefault(key, 0L) > System.currentTimeMillis();
    }

    public void applyCooldown(Player player, String ability, int seconds, String title) {
        String key = player.getUniqueId() + ":" + ability;
        long expireTime = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.put(key, expireTime);

        BossBar oldBar = cooldownBars.remove(key);
        if (oldBar != null) {
            oldBar.removeAll();
        }

        BossBar bar = TextUtils.bossBar(
                ChatColor.YELLOW + ChatColor.stripColor(title),
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bar.addPlayer(player);
        cooldownBars.put(key, bar);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !cooldownBars.containsKey(key) || !cooldownBars.get(key).equals(bar)) {
                    bar.removeAll();
                    cooldownBars.remove(key);
                    cancel();
                    return;
                }

                long timeLeft = expireTime - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    bar.removeAll();
                    cooldownBars.remove(key);
                    cancel();
                    return;
                }

                double progress = (double) timeLeft / (seconds * 1000.0);
                bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void resetCooldowns(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.keySet().removeIf(k -> k.startsWith(uuid.toString() + ":"));
        cooldownBars.keySet().removeIf(k -> {
            if (k.startsWith(uuid.toString() + ":")) {
                BossBar bar = cooldownBars.get(k);
                if (bar != null) bar.removeAll();
                return true;
            }
            return false;
        });
        absorbingPlayers.remove(uuid);
        absorbedDamage.remove(uuid);
    }

    public void clearAllBars() {
        cooldowns.clear();
        for (BossBar bar : cooldownBars.values()) {
            bar.removeAll();
        }
        cooldownBars.clear();
        absorbingPlayers.clear();
        absorbedDamage.clear();
    }

    public boolean isAbsorbing(UUID uuid) {
        return absorbingPlayers.contains(uuid);
    }

    public void recordAbsorbedDamage(UUID uuid, double damage) {
        double cap = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.damage-cap", 30.0);
        double ratio = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.absorption-ratio", 1.0);
        absorbedDamage.compute(uuid, (k, current) -> {
            double newVal = (current == null ? 0.0 : current) + (damage * ratio);
            return Math.min(cap, newVal);
        });
    }

    /**
     * Ability 1: Earthshatter (ᴇᴀʀᴛʜsʜᴀᴛᴛᴇʀ)
     */
    public void useEarthShatter(Player player) {
        int cdSec = plugin.getWeaponsConfig().getInt("paladins-battle-axe.earth-shatter.cooldown", 45);
        if (isOnCooldown(player, "earth_shatter")) {
            return;
        }

        if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().canUseAbilities(player)) {
            plugin.getWorldGuardManager().notifyDeniedChat(player);
            return;
        }

        Location resolvedTarget = null;

        // Check if player is directly targeting a mob or player (like Hyperion Holy Lance)
        RayTraceResult entityTrace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                60.0,
                0.6,
                e -> e != player && e instanceof LivingEntity
        );

        if (entityTrace != null && entityTrace.getHitEntity() instanceof LivingEntity living) {
            Block ground = getSurfaceBlock(living.getWorld(), living.getLocation().getBlockX(), living.getLocation().getBlockY(), living.getLocation().getBlockZ());
            if (ground != null) {
                resolvedTarget = ground.getLocation().add(0.5, 1.0, 0.5);
            } else {
                resolvedTarget = living.getLocation().clone();
            }
        } else {
            RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 60.0);
            if (rayTrace != null && rayTrace.getHitBlock() != null) {
                resolvedTarget = rayTrace.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
            }
        }

        if (resolvedTarget == null) {
            return;
        }

        final Location targetLoc = resolvedTarget;

        if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().isLocationAllowed(targetLoc, player)) {
            plugin.getWorldGuardManager().notifyDeniedChat(player);
            return;
        }

        applyCooldown(player, "earth_shatter", cdSec, plugin.tr("§6§lᴄоᴋрушᴇниᴇ зᴇмли", "§6§lᴇᴀʀᴛʜsʜᴀᴛᴛᴇʀ"));
        player.swingMainHand();

        ItemStack clayBall = new ItemStack(Material.CLAY_BALL);
        ItemMeta meta = clayBall.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(8);
            clayBall.setItemMeta(meta);
        }

        final double baseY = 4.0;
        Location spawnLoc = targetLoc.clone().add(0, baseY, 0);
        spawnLoc.setYaw(0f);
        spawnLoc.setPitch(0f);
        World world = targetLoc.getWorld();
        if (world == null) return;
        final double groundY = targetLoc.getY();

        // Initial cast sounds (both at target location and directly to the casting player)
        world.playSound(targetLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.5f);
        world.playSound(targetLoc, Sound.ENTITY_BREEZE_CHARGE, 1.0f, 0.1f);
        world.playSound(targetLoc, Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.6f);

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1.0f, 0.1f);
        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.6f);

        Vector dirToOwner = player.getLocation().toVector().subtract(targetLoc.toVector()).setY(0);
        if (dirToOwner.lengthSquared() < 0.001) dirToOwner = new Vector(0, 0, 1);
        float yawToOwner = (float) Math.toDegrees(Math.atan2(-dirToOwner.getX(), dirToOwner.getZ()));

        ItemDisplay display = world.spawn(spawnLoc, ItemDisplay.class, ent -> {
            ent.setItemStack(clayBall);
            ent.setViewRange(4.0f);
            ent.setTeleportDuration(0); // entity never moves; all motion is done via translation
            applyHammerTransform(ent, 6.0f, yawToOwner, 0, 0f);
        });

        plugin.getVisualCleanupManager().track(display);

        // Clean vertical target indicator pillar of 10 spark particles at impact point
        spawnTargetPillar(world, targetLoc);

        final double riseHeight = 10.5;
        // Fall stages (start tick offset from t2, duration, target height) -> accelerating drop
        final int[] fallStart = {0, 6, 10};
        final int[] fallDur = {6, 4, 2};
        final float[] fallTarget = {8.5f, 4.0f, 0f};

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                int curTick = tick++; // Always increment to prevent freezing on impact!

                if (!display.isValid() || display.isDead()) {
                    cancel();
                    return;
                }

                int t1 = 18;
                int t2 = 24;
                int t3 = 36;
                int t4 = 61;

                try {
                    if (curTick < t1) {
                        double progress = curTick / 18.0;
                        double ease = 1.0 - Math.pow(1.0 - progress, 2.0);
                        float h = (float) (riseHeight * ease);

                        float rotY = yawToOwner + (float) ((1.0 - progress) * 360.0);
                        applyHammerTransform(display, 6.0f, rotY, 2, h);

                        spawnTargetPillar(world, targetLoc);
                        spawnAxeDust(world, targetLoc, groundY + baseY + h);
                    } else if (curTick < t2) {
                        float hoverBob = (float) (Math.sin((curTick - t1) * 0.8) * 0.35);
                        float h = (float) riseHeight + hoverBob;
                        // last hover tick snaps exactly to the top so the fall starts cleanly
                        if (curTick == t2 - 1) h = (float) riseHeight;
                        applyHammerTransform(display, 6.0f, yawToOwner, 2, h);

                        spawnTargetPillar(world, targetLoc);
                        spawnAxeDust(world, targetLoc, groundY + baseY + h);
                    } else if (curTick < t3) {
                        int ft = curTick - t2;
                        // Send one interpolated segment per stage: the client animates it smoothly
                        for (int s = 0; s < fallStart.length; s++) {
                            if (ft == fallStart[s]) {
                                applyHammerTransform(display, 6.0f, yawToOwner, fallDur[s], fallTarget[s]);
                            }
                        }

                        spawnTargetPillar(world, targetLoc);

                        double progress = ft / 12.0;
                        double h = riseHeight * (1.0 - progress * progress);
                        spawnAxeDust(world, targetLoc, groundY + baseY + h);
                    } else if (curTick == t3) {
                        // Lock exactly at the final resting pose (no re-interpolation -> no shake)
                        applyHammerTransform(display, 6.0f, yawToOwner, 0, 0f);

                        // ONLY the heavy Totem boom with pitch 0.1!
                        world.playSound(targetLoc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 3.5f, 0.1f);

                        // 1) Direct hit: ONLY targets standing right under the axe take one big hit (no knock-up)
                        Set<UUID> directHit = new HashSet<>();
                        try {
                            double directRadius = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.earth-shatter.direct-radius", 1.6);
                            double directDamage = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.earth-shatter.direct-damage", 12.0);
                            double topY = groundY + baseY + 3.0;
                            for (Entity ent : world.getNearbyEntities(targetLoc, directRadius + 0.5, baseY + 3.0, directRadius + 0.5)) {
                                if (!(ent instanceof LivingEntity target) || target.equals(player)) continue;
                                if (plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) continue;
                                if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().canAffectTarget(player, target)) continue;

                                Location tl = target.getLocation();
                                double dx = tl.getX() - targetLoc.getX();
                                double dz = tl.getZ() - targetLoc.getZ();
                                if (dx * dx + dz * dz > directRadius * directRadius) continue;
                                if (tl.getY() < groundY - 1.0 || tl.getY() > topY) continue;

                                plugin.getCleanDamageManager().apply(target, player, directDamage);
                                directHit.add(target.getUniqueId());
                            }
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Earthshatter damage error: " + t);
                        }

                        // 2) Rising block wave: hits everyone else ONCE when the ring reaches them
                        try {
                            int radius = plugin.getWeaponsConfig().getInt("paladins-battle-axe.earth-shatter.radius", 8);
                            int maxR = Math.min(8, Math.max(6, radius));
                            createRisingBlockWave(targetLoc, maxR, player, directHit);
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Earthshatter wave error: " + t);
                        }

                        // Ground block data
                        Block centerGround = world.getBlockAt(targetLoc.getBlockX(), targetLoc.getBlockY() - 1, targetLoc.getBlockZ());
                        BlockData hitBData = (centerGround.getType().isAir()) ? Material.COBBLESTONE.createBlockData() : centerGround.getBlockData();

                        try {
                            // Big impact flash (Pure Blade style 3x)
                            spawnFlash(world, targetLoc, player);
                            // Slow beautiful 3D dispersion of end rod, spark, firework (including downwards)
                            spawnHolyParticleDispersion(world, targetLoc, player);
                            ParticleUtils.spawnBlockDispersion(plugin, targetLoc, hitBData, 6.0, 150);
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Earthshatter particles error: " + t);
                        }
                    } else if (curTick >= t4) {
                        if (display.isValid()) {
                            new BukkitRunnable() {
                                float scale = 6.0f;
                                int count = 0;

                                @Override
                                public void run() {
                                    if (count >= 10 || !display.isValid()) {
                                        if (display.isValid()) {
                                            plugin.getVisualCleanupManager().untrack(display);
                                            display.remove();
                                        }
                                        cancel();
                                        return;
                                    }
                                    scale -= 0.6f;
                                    if (scale < 0.0f) scale = 0.0f;

                                    Transformation trans = display.getTransformation();
                                    trans.getScale().set(scale, scale, scale);
                                    // keep the bottom of the hammer planted while it shrinks
                                    trans.getTranslation().set(0f, -HAMMER_HALF_HEIGHT * (6.0f - scale), 0f);
                                    display.setInterpolationDuration(2);
                                    display.setInterpolationDelay(0);
                                    display.setTransformation(trans);
                                    count++;
                                }
                            }.runTaskTimer(plugin, 0L, 1L);
                        }
                        cancel();
                        return;
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Error in Earthshatter tick " + curTick + ": " + t.getMessage());
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnFlash(World world, Location loc, Player player) {
        Location flashLoc = loc.clone().add(0, 1.5, 0);
        Color whiteColor = Color.fromRGB(255, 255, 255);
        try {
            // Exactly like Pure Blade, 3 times larger (Pure Blade: count=3, 0.2, 0.5, 0.2, 0, whiteColor)
            world.spawnParticle(Particle.FLASH, flashLoc, 9, 0.6, 1.5, 0.6, 0, whiteColor);
            if (player != null && player.isOnline()) {
                player.spawnParticle(Particle.FLASH, flashLoc, 9, 0.6, 1.5, 0.6, 0, whiteColor);
            }
        } catch (Throwable t) {
            try {
                world.spawnParticle(Particle.FLASH, flashLoc, 9, 0.6, 1.5, 0.6, 0);
                if (player != null && player.isOnline()) {
                    player.spawnParticle(Particle.FLASH, flashLoc, 9, 0.6, 1.5, 0.6, 0);
                }
            } catch (Throwable ignored) {}
        }
    }

    public void spawnHolyParticleDispersion(World world, Location centerLoc, Player player) {
        if (world == null || centerLoc == null) return;
        Location origin = centerLoc.clone().add(0, 1.2, 0);

        int count = 72;
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i / count) + (Math.random() - 0.5) * 0.25;
            double h = 0.18 + Math.random() * 0.28; // slow horizontal speed (медленно и красиво)
            double vx = Math.cos(angle) * h;
            double vz = Math.sin(angle) * h;

            // Full 3D vertical dispersion including downwards:
            // 40% upwards, 30% horizontal, 30% downwards
            double vy;
            double r = Math.random();
            if (r < 0.40) {
                vy = 0.25 + Math.random() * 0.35; // Upward arc
            } else if (r < 0.70) {
                vy = -0.05 + Math.random() * 0.15; // Horizontal spray
            } else {
                vy = -0.15 - Math.random() * 0.30; // Downward spray
            }

            world.spawnParticle(Particle.FIREWORK, origin, 0, vx, vy, vz, 1.0, null, true);
            world.spawnParticle(Particle.END_ROD, origin, 0, vx * 0.85, vy * 0.85, vz * 0.85, 1.0, null, true);
            world.spawnParticle(Particle.ELECTRIC_SPARK, origin, 0, vx * 1.15, vy * 1.15, vz * 1.15, 1.0, null, true);

            if (player != null && player.isOnline()) {
                player.spawnParticle(Particle.FIREWORK, origin, 0, vx, vy, vz, 1.0);
                player.spawnParticle(Particle.END_ROD, origin, 0, vx * 0.85, vy * 0.85, vz * 0.85, 1.0);
                player.spawnParticle(Particle.ELECTRIC_SPARK, origin, 0, vx * 1.15, vy * 1.15, vz * 1.15, 1.0);
            }
        }
    }

    /** Yellow dust right under the bottom of the flying axe. axeCenterY = world Y of the axe model center. */
    private void spawnAxeDust(World world, Location targetLoc, double axeCenterY) {
        double bottomY = axeCenterY - HAMMER_HALF_HEIGHT * 6.0;
        Location fx = new Location(world, targetLoc.getX(), bottomY, targetLoc.getZ());
        world.spawnParticle(Particle.DUST, fx, 6, 0.35, 0.15, 0.35, 0.0,
                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.4f));
    }

    private void spawnTargetPillar(World world, Location targetLoc) {
        for (int i = 0; i < 10; i++) {
            double sy = 0.1 + i * 0.22; // 10 spark particles tightly packed vertically (slitno)
            world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc.clone().add(0, sy, 0), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Half of the hammer model height in model units (item models span -0.5..0.5). Used to keep the bottom planted when scaling. */
    private static final float HAMMER_HALF_HEIGHT = 0.5f;

    public void applyHammerTransform(ItemDisplay display, float scale, float rotYDegrees) {
        applyHammerTransform(display, scale, rotYDegrees, 1, 0f);
    }

    public void applyHammerTransform(ItemDisplay display, float scale, float rotYDegrees, int interpDuration) {
        applyHammerTransform(display, scale, rotYDegrees, interpDuration, 0f);
    }

    public void applyHammerTransform(ItemDisplay display, float scale, float rotYDegrees, int interpDuration, float translationY) {
        Transformation trans = display.getTransformation();
        trans.getScale().set(scale, scale, scale);
        trans.getTranslation().set(0f, translationY, 0f);
        Quaternionf q = new Quaternionf();
        q.rotateY((float) Math.toRadians(-rotYDegrees + 180.0));
        q.rotateX((float) Math.PI);
        trans.getRightRotation().set(q);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(interpDuration);
        display.setTransformation(trans);
    }

    private void circleWaveParticle(Location center, int r, World world) {
        Block groundBlock = center.clone().add(0, -1, 0).getBlock();
        BlockData blockData = groundBlock.getType().isAir() ? Material.COBBLESTONE.createBlockData() : groundBlock.getBlockData();
        Particle.DustOptions dustOpt = new Particle.DustOptions(Color.fromRGB(220, 180, 80), 1.2f);

        for (double angle = 0.0; angle < 2 * Math.PI; angle += 0.2) {
            double deg = Math.toDegrees(angle) % 360.0;
            boolean skip = false;
            for (int i = 0; i < 8; i++) {
                double targetDeg = i * 45.0;
                double diff = Math.abs(deg - targetDeg);
                if (diff > 180.0) diff = 360.0 - diff;
                if (diff < 10.0) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;

            double px = center.getX() + r * Math.cos(angle);
            double pz = center.getZ() + r * Math.sin(angle);
            Location pLoc = new Location(world, px, center.getY() + 0.2, pz);

            world.spawnParticle(Particle.DUST, pLoc, 2, 0.0, 0.1, 0.0, 0.0, dustOpt);
        }
    }

    public void createRisingBlockWave(Location center, int maxRadius, Player player) {
        createRisingBlockWave(center, maxRadius, player, Collections.emptySet());
    }

    public void createRisingBlockWave(Location center, int maxRadius, Player player, Set<UUID> directHit) {
        World world = center.getWorld();
        if (world == null) return;

        Map<Integer, List<Block>> blocksByDistance = new HashMap<>();
        int centerBX = center.getBlockX();
        int centerBZ = center.getBlockZ();
        int baseY = center.getBlockY() - 1;

        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                if (dx * dx + dz * dz > maxRadius * maxRadius) continue;
                if (dx == 0 && dz == 0) continue; // CENTER BLOCK NEVER MOVES!

                int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
                if (dist < 1) continue; // CENTER BLOCK NEVER MOVES!

                int x = centerBX + dx;
                int z = centerBZ + dz;

                Block surface = getSurfaceBlock(world, x, baseY + 3, z);
                if (surface != null) {
                    blocksByDistance.computeIfAbsent(dist, k -> new ArrayList<>()).add(surface);
                }
            }
        }

        double shatterDamage = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.earth-shatter.damage", 7.0);
        double shatterKb = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.earth-shatter.knockback", 0.85);
        Set<UUID> hitByWave = new HashSet<>();

        for (Map.Entry<Integer, List<Block>> entry : blocksByDistance.entrySet()) {
            int dist = entry.getKey();
            List<Block> blocks = entry.getValue();

            long delay = (dist - 1) * 2L;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    for (Block block : blocks) {
                        Material mat = block.getType();
                        if (mat.isAir() || mat == Material.WATER || mat == Material.LAVA) continue;

                        BlockData bData = block.getBlockData();
                        Location loc = block.getLocation();

                        int dx = block.getX() - centerBX;
                        int dz = block.getZ() - centerBZ;
                        boolean isPlus = (Math.abs(dx) <= 1 || Math.abs(dz) <= 1);
                        boolean isCross = (Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1);

                        double jumpH;
                        if (isPlus) {
                            jumpH = Math.max(1.3, 2.0 - (dist * 0.08));
                        } else if (isCross) {
                            jumpH = Math.max(1.0, 1.6 - (dist * 0.08));
                        } else {
                            jumpH = Math.max(0.6, 1.1 - (dist * 0.08));
                        }
                        final double finalJumpH = jumpH + (Math.random() - 0.5) * 0.35;

                        Location blockCenter = loc.clone().add(0.5, 0.5, 0.5);
                        Material itemMat = mat.isItem() ? mat : Material.STONE;
                        ItemDisplay blockDisplay = world.spawn(blockCenter, ItemDisplay.class, ent -> {
                            ent.setItemStack(new ItemStack(itemMat));
                            ent.setBrightness(new Display.Brightness(15, 15));
                            ent.setTeleportDuration(5);
                        });
                        plugin.getVisualCleanupManager().track(blockDisplay);

                        Location targetUp = blockCenter.clone().add(0, finalJumpH, 0);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (blockDisplay.isValid()) {
                                blockDisplay.teleport(targetUp);
                                world.spawnParticle(Particle.ELECTRIC_SPARK, targetUp.clone().add(0, 0.4, 0), 1, 0.05, 0.05, 0.05, 0.01);
                            }
                        }, 2L);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (blockDisplay.isValid()) {
                                blockDisplay.teleport(blockCenter);
                            }
                        }, 9L);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (blockDisplay.isValid()) {
                                plugin.getVisualCleanupManager().untrack(blockDisplay);
                                blockDisplay.remove();
                            }
                        }, 15L);
                    }

                    // Damage and knockup to targets as the wave gradually reaches them
                    for (Entity ent : world.getNearbyEntities(center, dist + 1.2, 4.0, dist + 1.2)) {
                        if (ent instanceof LivingEntity victim && !victim.equals(player)) {
                            // If directly crushed by the axe, DO NOT apply wave damage or knockup!
                            if (directHit != null && directHit.contains(victim.getUniqueId())) continue;
                            if (hitByWave.contains(victim.getUniqueId())) continue;

                            if (victim instanceof Player pVictim) {
                                if (plugin.getFriendManager().isFriend(player.getUniqueId(), pVictim.getUniqueId())) continue;
                            }
                            if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().canAffectTarget(player, victim)) continue;

                            double dX = victim.getLocation().getX() - center.getX();
                            double dZ = victim.getLocation().getZ() - center.getZ();
                            double d2 = dX * dX + dZ * dZ;
                            if (d2 >= (dist - 1.2) * (dist - 1.2) && d2 <= (dist + 1.2) * (dist + 1.2)) {
                                hitByWave.add(victim.getUniqueId());
                                plugin.getCleanDamageManager().apply(victim, player, shatterDamage);
                                victim.setVelocity(new Vector(0, shatterKb, 0));
                            }
                        }
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Error in block wave: " + t.getMessage());
                }
            }, delay);
        }
    }

    public void spawnMaceSmashBlockDispersal(Location center, BlockData blockData) {
        ParticleUtils.spawnBlockDispersion(plugin, center, blockData, 4.5);
    }

    private Block getSurfaceBlock(World world, int x, int startY, int z) {
        for (int y = startY; y >= startY - 20; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material mat = block.getType();
            if (!mat.isAir() && mat != Material.SHORT_GRASS && mat != Material.TALL_GRASS
                    && mat != Material.FERN && mat != Material.LARGE_FERN
                    && mat != Material.WATER && mat != Material.LAVA) {
                return block;
            }
        }
        return null;
    }

    /**
     * Ability 2: Stalwart Absorption (sᴛᴀʟᴡᴀʀᴛ ᴀʙsᴏʀᴘᴛɪᴏɴ)
     */
    public void useStalwartAbsorption(Player player) {
        int cdSec = plugin.getWeaponsConfig().getInt("paladins-battle-axe.stalwart-absorption.cooldown", 45);
        if (isOnCooldown(player, "stalwart_absorption")) {
            return;
        }

        if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().canUseAbilities(player)) {
            plugin.getWorldGuardManager().notifyDeniedChat(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        if (absorbingPlayers.contains(uuid)) {
            return;
        }

        applyCooldown(player, "stalwart_absorption", cdSec, plugin.tr("§6§lᴄᴛойᴋоᴇ поглощᴇниᴇ", "§6§lsᴛᴀʟᴡᴀʀᴛ ᴀʙsᴏʀᴘᴛɪᴏɴ"));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.5f, 2.0f);
        absorbingPlayers.add(uuid);
        absorbedDamage.put(uuid, 0.0);

        int durationSec = plugin.getWeaponsConfig().getInt("paladins-battle-axe.stalwart-absorption.duration-seconds", 5);
        int durationTicks = durationSec * 20;

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0, false, true, true));

        new BukkitRunnable() {
            int a = 0;

            @Override
            public void run() {
                if (a >= durationTicks || !player.isOnline() || player.isDead() || !absorbingPlayers.contains(uuid)) {
                    releaseAbsorbedDamage(player);
                    cancel();
                    return;
                }

                Location pLoc = player.getLocation();
                for (int i = 0; i < 4; i++) {
                    double ox = (Math.random() - 0.5) * 0.85;
                    double oy = Math.random() * 1.85;
                    double oz = (Math.random() - 0.5) * 0.85;
                    Location auraLoc = pLoc.clone().add(ox, oy, oz);
                    player.getWorld().spawnParticle(Particle.DUST, auraLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f));
                }

                double absorbed = absorbedDamage.getOrDefault(uuid, 0.0);
                String valStr = (absorbed == Math.floor(absorbed)) ? String.valueOf((int) absorbed) : String.format(Locale.US, "%.1f", absorbed);
                player.sendActionBar(TextUtils.shadow(TextUtils.legacy(
                        plugin.tr("&6поглощᴇно уронᴀ &e" + valStr, "&6ᴅᴀᴍᴀɢᴇ ᴀʙsᴏʀʙᴇᴅ &e" + valStr)
                )));

                a++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void releaseAbsorbedDamage(Player player) {
        UUID uuid = player.getUniqueId();
        if (!absorbingPlayers.remove(uuid)) return;

        double cap = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.damage-cap", 15.0);
        double absorbed = Math.min(cap, absorbedDamage.getOrDefault(uuid, 0.0));
        absorbedDamage.remove(uuid);

        if (!player.isOnline()) return;

        String valStr = (absorbed == Math.floor(absorbed)) ? String.valueOf((int) absorbed) : String.format(Locale.US, "%.1f", absorbed);
        // Underline on release as requested!
        player.sendActionBar(TextUtils.shadow(TextUtils.legacy(
                plugin.tr("&6&nпоглощᴇно уронᴀ &e&n" + valStr, "&6&nᴅᴀᴍᴀɢᴇ ᴀʙsᴏʀʙᴇᴅ &e&n" + valStr)
        )));

        double knockback = 0.65;
        double minDmg = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.min-shockwave-damage", 2.0);
        double releaseDmg = Math.max(minDmg, Math.min(10.0, absorbed * 0.65));

        Location loc = player.getLocation();
        World world = player.getWorld();

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
        world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.8f);

        world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 2, 0.8, 0.5, 0.8, 0.1);
        world.spawnParticle(Particle.DUST, loc.clone().add(0, 0.5, 0), 70, 2.0, 0.5, 2.0, 0.1,
                new Particle.DustOptions(Color.fromRGB(255, 220, 50), 2.0f));

        double radius = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.release-radius", 6.0);
        for (Entity ent : world.getNearbyEntities(loc, radius, radius, radius)) {
            if (!(ent instanceof LivingEntity target)) continue;
            if (target.getUniqueId().equals(player.getUniqueId())) continue;

            if (plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) continue;
            if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().canAffectTarget(player, target)) continue;

            plugin.getCleanDamageManager().apply(target, player, releaseDmg);

            Vector kb = target.getLocation().toVector().subtract(loc.toVector());
            if (kb.lengthSquared() > 0.001) {
                kb.normalize().multiply(knockback).setY(0.45);
            } else {
                kb = new Vector(0, 0.5, 0);
            }
            target.setVelocity(kb);
        }
    }
}
