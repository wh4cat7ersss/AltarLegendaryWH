package dev.whersss.altarLegendaryWH.weapons.pureblade.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.*;

public class PureBladeManager {
    private final AltarLegendaryWH plugin;
    private final PureBossBarManager bossBarManager;
    private final Random random = new Random();

    public PureBladeManager(AltarLegendaryWH plugin, PureBossBarManager bossBarManager) {
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
    }

    private void applyPureDamage(LivingEntity victim, Player attacker, double damage) {
        plugin.getCleanDamageManager().apply(victim, attacker, damage);
    }

    public void playMassiveSweep(Player p, LivingEntity victim) {
        if (victim instanceof Player targetPlayer) {
            if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                return;
            }
        }

        Vector dir = p.getLocation().getDirection().normalize();
        Location center = victim.getLocation().add(0, 1, 0).subtract(dir.clone().multiply(0.8));

        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);

        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.clone().crossProduct(dir).normalize();

        double rotAngle = Math.random() * Math.PI;
        double cos = Math.cos(rotAngle);
        double sin = Math.sin(rotAngle);

        Particle.DustOptions miniWhiteDust = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.5f);

        for (double i = -1.4; i <= 1.4; i += 0.05) {
            double x = i * cos;
            double y = i * sin;
            double z = -(i * i) * 0.25;

            Vector point = right.clone().multiply(x).add(up.clone().multiply(y)).add(dir.clone().multiply(z));
            Location sparkLoc = center.clone().add(point);

            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 1, 0, 0, 0, 0);
            center.getWorld().spawnParticle(Particle.DUST, sparkLoc, 1, 0, 0, 0, 0, miniWhiteDust);
        }
    }

    public void castShadeSoul(Player p) {
        if (bossBarManager.isOnCooldown(p, "ShadeSoul")) return;
        bossBarManager.setCooldown(
                p,
                "ShadeSoul",
                plugin.tr("§f§lТень души", "§f§lsʜᴀᴅᴇ ᴏꜰ sᴏᴜʟ"),
                plugin.getWeaponsConfig().getInt("pure-blade.shade-soul.cooldown", 45)
        );

        ItemStack cubeItem = new ItemStack(Material.CLAY_BALL);
        ItemMeta meta = cubeItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(6);
            cubeItem.setItemMeta(meta);
        }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead()) {
                    this.cancel();
                    return;
                }

                if (ticks >= 30) {
                    this.cancel();
                    fireShadeSoulBeam(p, cubeItem);
                    return;
                }

                if (Math.random() < 0.30) {
                    ItemDisplay popCube = (ItemDisplay) p.getWorld().spawnEntity(p.getLocation().add(0, 1, 0), EntityType.ITEM_DISPLAY);
                    popCube.setItemStack(cubeItem);
                    popCube.setTeleportDuration(1);

                    float initialSize = 0.2f + random.nextFloat() * 0.35f;
                    Vector randomDir = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(0.15);

                    new BukkitRunnable() {
                        float size = initialSize;

                        @Override
                        public void run() {
                            size -= 0.015f;
                            if (size <= 0) {
                                popCube.remove();
                                this.cancel();
                                return;
                            }

                            popCube.teleport(popCube.getLocation().add(randomDir));
                            popCube.setInterpolationDelay(0);
                            popCube.setInterpolationDuration(1);

                            Transformation t = popCube.getTransformation();
                            t.getScale().set(size, size, size);
                            popCube.setTransformation(t);
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void fireShadeSoulBeam(Player p, ItemStack cubeItem) {
        p.swingMainHand();

        try {
            Color whiteColor = Color.fromRGB(255, 255, 255);
            p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0, 1, 0), 3, 0.2, 0.5, 0.2, 0, whiteColor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);

        Location startLoc = p.getEyeLocation();

        for (int c = 0; c < 30; c++) {
            int r = 180 + random.nextInt(75);
            int g = 220 + random.nextInt(35);
            int b = 220 + random.nextInt(35);
            Particle.DustOptions paleDust = new Particle.DustOptions(Color.fromRGB(r, g, b), 1.5f);
            p.getWorld().spawnParticle(Particle.DUST, startLoc, 1, 0.5, 0.5, 0.5, paleDust);
        }

        Vector dir = startLoc.getDirection().normalize();
        final Set<UUID> hitTargets = new HashSet<>();

        int numCubes = 20;

        new BukkitRunnable() {
            int spawned = 0;

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead() || spawned >= numCubes) {
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 2; i++) {
                    if (spawned >= numCubes) break;

                    double dist = (spawned + 1) * 1.35;
                    Location cubeStartLoc = startLoc.clone().add(dir.clone().multiply(dist));

                    spawnShadeSoulCube(p, cubeStartLoc, 0.65f, cubeItem, hitTargets, dir);

                    if (random.nextBoolean()) {
                        Location smallLoc = cubeStartLoc.clone().add(
                                (Math.random() - 0.5) * 1.3,
                                (Math.random() - 0.5) * 1.3,
                                (Math.random() - 0.5) * 1.3
                        );
                        float smallScale = 0.2f + random.nextFloat() * 0.20f;
                        spawnShadeSoulCube(p, smallLoc, smallScale, cubeItem, hitTargets, dir);
                    }

                    spawned++;
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void spawnShadeSoulCube(Player p, Location baseLoc, float scaleStart, ItemStack item, Set<UUID> hitTargets, Vector launchDir) {
        Location currentLoc = baseLoc.clone().add(
                (Math.random() - 0.5) * 0.4,
                (Math.random() - 0.5) * 0.4,
                (Math.random() - 0.5) * 0.4
        );

        ItemDisplay display = (ItemDisplay) p.getWorld().spawnEntity(currentLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(item);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(3);
        display.setTeleportDuration(3);

        Transformation t = display.getTransformation();
        t.getScale().set(scaleStart, scaleStart, scaleStart);
        display.setTransformation(t);

        double yDrift = (random.nextBoolean() ? 1 : -1) * (0.008 + random.nextDouble() * 0.012);
        final Vector sideways;
        Vector sidewaysBase = new Vector(-launchDir.getZ(), 0, launchDir.getX());
        if (sidewaysBase.lengthSquared() > 0.0001) {
            sidewaysBase.normalize();
        }
        if (random.nextDouble() < 0.4) {
            sideways = sidewaysBase.multiply((random.nextBoolean() ? 1 : -1) * (0.006 + random.nextDouble() * 0.01));
        } else {
            sideways = new Vector();
        }
        int evaporateDelay = 25 + random.nextInt(20);
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.6f);

        new BukkitRunnable() {
            int life = 0;
            float scale = scaleStart;

            @Override
            public void run() {
                if (scale <= 0) {
                    display.remove();
                    this.cancel();
                    return;
                }

                if (life < evaporateDelay) {
                    currentLoc.add(0, yDrift, 0);
                    currentLoc.add(sideways);
                    if (life % 6 == 0) {
                        currentLoc.add(0, Math.sin(life * 0.35) * 0.01, 0);
                    }
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(3);
                    display.setTeleportDuration(3);
                    display.teleport(currentLoc);

                    if (Math.random() > 0.5) {
                        currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 1, 0.3, 0.3, 0.3, blackDust);
                    }
                } else {
                    scale -= 0.05f;
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(3);
                    display.setTeleportDuration(3);

                    currentLoc.add(launchDir.clone().multiply(-0.04));
                    currentLoc.add(0, yDrift, 0);
                    currentLoc.add(sideways.clone().multiply(0.5));
                    display.teleport(currentLoc);

                    Transformation trans = display.getTransformation();
                    trans.getScale().set(Math.max(0.01f, scale), Math.max(0.01f, scale), Math.max(0.01f, scale));
                    display.setTransformation(trans);
                }

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity victim && !victim.equals(p) && !hitTargets.contains(victim.getUniqueId())) {
                        if (victim instanceof Player targetPlayer) {
                            if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                            if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) continue;
                        }

                        applyPureDamage(victim, p, plugin.getWeaponsConfig().getDouble("pure-blade.shade-soul.damage", 8.0));

                        int durationTicks = plugin.getWeaponsConfig().getInt("pure-blade.shade-soul.effect-duration", 5) * 20;
                        int slowAmp = plugin.getWeaponsConfig().getInt("pure-blade.shade-soul.slowness-amplifier", 1);
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, false));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, slowAmp, false, false));

                        p.getWorld().playSound(victim.getLocation(), Sound.ENTITY_VEX_HURT, 1.0f, 0.5f);
                        hitTargets.add(victim.getUniqueId());
                    }
                }
                life++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void castCycloneSlash(Player p) {
        if (bossBarManager.isOnCooldown(p, "CycloneSlash")) return;
        bossBarManager.setCooldown(p, "CycloneSlash", plugin.tr("§f§lВихревой Разрез", "§f§lᴄʏᴄʟᴏɴᴇ sʟᴀsʜ"), plugin.getWeaponsConfig().getInt("pure-blade.cyclone-slash.cooldown", 30));

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.0f, 1.0f);

        int duration = plugin.getWeaponsConfig().getInt("pure-blade.cyclone-slash.speed-duration", 5) * 20;
        int amplifier = plugin.getWeaponsConfig().getInt("pure-blade.cyclone-slash.speed-amplifier", 10);

        PotionEffect oldSpeed = p.getPotionEffect(PotionEffectType.SPEED);
        boolean hadSpeed = oldSpeed != null && oldSpeed.getAmplifier() < amplifier;

        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, false, false));

        if (hadSpeed) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        p.addPotionEffect(oldSpeed);
                    }
                }
            }.runTaskLater(plugin, duration);
        }

        ItemStack slashItem = new ItemStack(Material.CLAY_BALL);
        ItemMeta meta = slashItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(7);
            slashItem.setItemMeta(meta);
        }

        double CYCLONE_HEIGHT = 5.0;

        ItemDisplay display = (ItemDisplay) p.getWorld().spawnEntity(p.getLocation().add(0, CYCLONE_HEIGHT, 0), EntityType.ITEM_DISPLAY);
        display.setItemStack(slashItem);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setTeleportDuration(1);

        new BukkitRunnable() {
            int ticks = 0;
            float entityYaw = 0f;

            float currentScale = 12.0f;
            float minScale = 3.0f;
            int shrinkDuration = 40;
            int totalDuration = 50;
            double BASE_HEIGHT = 1.0;

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead() || ticks > totalDuration) {
                    display.remove();
                    if (p.isOnline()) {
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1.0f, 1.0f);
                    }
                    this.cancel();
                    return;
                }

                if (ticks % 2 == 0) {
                    float randomPitch = 0.8f + (float) (Math.random() * 0.3f);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1.0f, randomPitch);
                }

                if (ticks <= shrinkDuration) {
                    currentScale -= ((12.0f - minScale) / shrinkDuration);
                }

                Location baseLoc = p.getLocation();
                baseLoc.setPitch(0);

                Location displayLoc = baseLoc.clone();
                displayLoc.add(0, BASE_HEIGHT + (currentScale / 2.0), 0);
                displayLoc.setYaw(0);

                entityYaw += 85f;

                display.teleport(displayLoc);

                display.setInterpolationDelay(0);
                display.setInterpolationDuration(1);

                Transformation t = display.getTransformation();
                t.getScale().set(currentScale, currentScale, currentScale);
                t.getTranslation().set(0, 0, 0);
                t.getLeftRotation().set(new Quaternionf().rotateY((float) Math.toRadians(entityYaw)));
                display.setTransformation(t);

                double particleRadius = (currentScale / 2.0) + 0.2;
                double fixedParticleY = baseLoc.getY() + 1.1;

                if (ticks % 2 == 0) {
                    for (int j = 0; j < 30; j++) {
                        double angle = Math.random() * Math.PI * 2;
                        double r = (particleRadius * 0.4) + (Math.random() * (particleRadius * 0.6));

                        Location windTopLoc = baseLoc.clone().add(Math.cos(angle) * r, BASE_HEIGHT + (currentScale / 2.0) + 0.5, Math.sin(angle) * r);

                        double dx = -Math.sin(angle);
                        double dz = Math.cos(angle);

                        p.getWorld().spawnParticle(Particle.CLOUD, windTopLoc, 0, dx, 0.02, dz, 0.4);
                    }

                    for (int i = 0; i < 35; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        Location critLoc = new Location(p.getWorld(),
                                baseLoc.getX() + Math.cos(angle) * particleRadius,
                                fixedParticleY,
                                baseLoc.getZ() + Math.sin(angle) * particleRadius);
                        p.getWorld().spawnParticle(Particle.CRIT, critLoc, 1, 0, 0, 0, 0);
                    }
                }

                for (int i = 0; i < 50; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location edgeLoc = new Location(p.getWorld(),
                            baseLoc.getX() + Math.cos(angle) * particleRadius,
                            fixedParticleY,
                            baseLoc.getZ() + Math.sin(angle) * particleRadius);
                    p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, edgeLoc, 1, 0, 0, 0, 0);
                }

                if (ticks % 2 == 0) {
                    int checkRad = (int) Math.ceil(particleRadius);
                    for (int x = -checkRad; x <= checkRad; x++) {
                        for (int z = -checkRad; z <= checkRad; z++) {
                            for (int y = 0; y <= 2; y++) {
                                Block b = baseLoc.clone().add(x, y, z).getBlock();
                                if (isPlantOrWeb(b.getType())) {
                                    if (b.getLocation().distance(baseLoc.clone().add(0, y, 0)) <= particleRadius + 0.5) {
                                        b.breakNaturally();
                                    }
                                }
                            }
                        }
                    }
                }

                if (ticks % 10 == 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(baseLoc.clone().add(0, 1.1, 0), particleRadius + 1, 2.0, particleRadius + 1)) {
                        if (e instanceof LivingEntity victim && !victim.equals(p)) {
                            if (victim instanceof Player targetPlayer) {
                                if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                                if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) continue;
                            }
                            CombatUtils.runSyntheticDamage(() -> victim.damage(plugin.getWeaponsConfig().getDouble("pure-blade.cyclone-slash.damage", 10.0), p));
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private boolean isPlantOrWeb(Material m) {
        if (m.isAir() || m == Material.GRASS_BLOCK || m == Material.DIRT || m == Material.PODZOL || m == Material.MYCELIUM) {
            return false;
        }

        if (m == Material.COBWEB) {
            return true;
        }

        String name = m.name();

        if (name.contains("FLOWER") || name.contains("FERN") || name.contains("BUSH") || name.contains("BERRY")
                || name.contains("VINE") || name.contains("MUSHROOM") || name.contains("FUNGUS")
                || name.contains("ROOTS") || name.contains("SPROUTS") || name.contains("LILY")
                || name.contains("DRIPLEAF") || name.contains("LICHEN") || name.contains("SAPLING")
                || name.contains("BAMBOO") || name.contains("CACTUS") || name.contains("SUGAR_CANE")
                || name.contains("KELP") || name.contains("SEAGRASS")
                || name.contains("WHEAT") || name.contains("CARROT") || name.contains("POTATO") || name.contains("BEETROOT")
                || (name.contains("GRASS") && m != Material.GRASS_BLOCK)) {
            return true;
        }

        if (!m.isSolid() && !name.contains("WATER") && !name.contains("LAVA")
                && m != Material.FIRE && m != Material.SOUL_FIRE && m != Material.LIGHT
                && !name.contains("SIGN") && !name.contains("BANNER") && !name.contains("TORCH")
                && !name.contains("LANTERN") && !name.contains("CAMPFIRE") && !name.contains("TRAPDOOR")
                && !name.contains("DOOR") && !name.contains("BUTTON") && !name.contains("PRESSURE_PLATE")
                && !name.contains("RAIL") && !name.contains("REDSTONE")) {
            return true;
        }

        return false;
    }

    public void resetCooldowns(Player player) {
        bossBarManager.resetPlayer(player);
    }
}

