package dev.whersss.altarLegendaryWH.weapons.earthgauntlet.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.ParticleUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class EarthGauntletManager implements Listener {

    private final AltarLegendaryWH plugin;
    private final EarthBossBarManager bossBarManager;
    private final Random random = new Random();

    private final Map<UUID, BukkitTask> activeMeteorStrikes = new HashMap<>();
    private final Map<UUID, UUID> pullableTargets = new HashMap<>();
    private final Map<UUID, LinkedList<BlockState>> activeMudTrails = new HashMap<>();
    private final Set<UUID> stunnedTargets = new HashSet<>();

    private final BlockData mudBlockData = Bukkit.createBlockData(Material.MUD);
    private final BlockData packedMudBlockData = Bukkit.createBlockData(Material.PACKED_MUD);

    public EarthGauntletManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        this.bossBarManager = new EarthBossBarManager(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void playPassiveHitSound(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.MASTER, 1.0f, 0.6f);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!stunnedTargets.contains(e.getPlayer().getUniqueId())) return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;

        if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() < to.getY()) {
            Location newTo = from.clone();
            newTo.setYaw(to.getYaw());
            newTo.setPitch(to.getPitch());
            e.setTo(newTo);
        }
    }

    public void activateMeteorStrike(Player p) {
        if (bossBarManager.isOnCooldown(p, "MeteorStrike")) return;
        if (activeMeteorStrikes.containsKey(p.getUniqueId())) return;

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1f, 1f);
        p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation().add(0, 1, 0), 20, 0.5, 1.0, 0.5, 1.0, packedMudBlockData);
        p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p.getLocation().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.05, 1.0f);

        int activeWindow = plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.active_window", 30);
        bossBarManager.setActiveBar(p, "MeteorStrikeActive", plugin.tr("§a§lмᴇᴛᴇориᴛный удᴀр", "§a§lᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ"), activeWindow);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!p.isOnline() || ticks >= activeWindow * 20) {
                    cleanupMeteor(p);
                    bossBarManager.removeActiveBar(p, "MeteorStrikeActive");
                    bossBarManager.setCooldown(p, "MeteorStrike", plugin.tr("§e§lмᴇᴛᴇориᴛный удᴀр", "§e§lᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ"), plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.cooldown", 35));
                    return;
                }

                p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation().add(0, 1, 0), 1, 0.3, 0.5, 0.3, 0.0);
                p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.5, packedMudBlockData);
                p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.01, 1.0f);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeMeteorStrikes.put(p.getUniqueId(), task);
    }

    public void triggerMeteorStrikeHit(Player attacker, LivingEntity victim) {
        if (!activeMeteorStrikes.containsKey(attacker.getUniqueId())) return;

        activeMeteorStrikes.get(attacker.getUniqueId()).cancel();
        activeMeteorStrikes.remove(attacker.getUniqueId());
        bossBarManager.removeActiveBar(attacker, "MeteorStrikeActive");

        bossBarManager.setCooldown(attacker, "MeteorStrike", plugin.tr("§e§lмᴇᴛᴇориᴛный удᴀр", "§e§lᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ"), plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.cooldown", 35));

        attacker.getWorld().playSound(victim.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        attacker.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 1f);

        try {
            attacker.getWorld().spawnParticle(Particle.valueOf("GUST_EMITTER_LARGE"), victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        } catch (Exception ex) {
            attacker.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        }
        attacker.getWorld().spawnParticle(Particle.DRAGON_BREATH, victim.getLocation().add(0, 1, 0), 80, 1.0, 1.0, 1.0, 0.1, 1.0f);

        double dmg = plugin.getWeaponsConfig().getDouble("earth-gauntlet.meteor_strike.damage", 4.0);
        plugin.getCleanDamageManager().apply(victim, attacker, dmg);
        victim.playHurtAnimation(0.0F);

        double kbMultiplier = plugin.getWeaponsConfig().getDouble("earth-gauntlet.meteor_strike.knockback", 2.5);
        Vector kb = attacker.getLocation().getDirection().setY(0).normalize().multiply(kbMultiplier).setY(0.35);
        victim.setVelocity(kb);

        final int stunDurationTicks = plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.stun_duration_ticks", 100);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isDead() || !victim.isValid()) return;

            stunnedTargets.add(victim.getUniqueId());

            if (victim instanceof Mob mob) {
                mob.setAware(false);
            }

            new BukkitRunnable() {
                int elapsedTicks = 0;
                @Override
                public void run() {
                    if (elapsedTicks >= stunDurationTicks || !victim.isValid() || victim.isDead()) {
                        stunnedTargets.remove(victim.getUniqueId());
                        if (victim instanceof Mob mob) {
                            mob.setAware(true);
                        }
                        this.cancel();
                        return;
                    }
                    if (elapsedTicks % 20 == 0) {
                        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.8f);
                    }
                    elapsedTicks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

        }, 10L);
    }

    public void castMudslide(Player p) {
        if (bossBarManager.isOnCooldown(p, "Mudslide")) return;

        int cooldown = plugin.getWeaponsConfig().getInt("earth-gauntlet.mudslide.cooldown", 40);
        bossBarManager.setCooldown(p, "Mudslide", plugin.tr("§e§lоползᴇнь", "§e§lᴍᴜᴅsʟɪᴅᴇ"), cooldown);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_INHALE, 1f, 1f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                launchMudBall(p);
            }
        }, 13L);
    }

    private void launchMudBall(Player p) {
        p.swingMainHand();

        Vector initialDirection = p.getEyeLocation().getDirection().normalize();
        Location startLoc = p.getEyeLocation();

        BlockDisplay display = p.getWorld().spawn(startLoc, BlockDisplay.class, ent -> {
            ent.setBlock(mudBlockData);
            ent.setInterpolationDuration(1);
            ent.setTeleportDuration(1);
        });
        plugin.getVisualCleanupManager().track(display);

        Vector right = initialDirection.clone().setY(0).normalize().crossProduct(new Vector(0, 1, 0)).normalize();
        LinkedList<BlockState> trail = new LinkedList<>();

        final Vector flatDirection = initialDirection.clone().setY(0).normalize();

        final double maxDist = plugin.getWeaponsConfig().getDouble("earth-gauntlet.mudslide.range", 50.0);
        final double speed = plugin.getWeaponsConfig().getDouble("earth-gauntlet.mudslide.speed", 1.2);

        final double curveThreshold = 0.55 + random.nextDouble() * 0.20;

        new BukkitRunnable() {
            double distanceTraveled = 0;
            float currentAngle = 0f;

            Location currentLoc = startLoc.clone();

            boolean isCurving = false;
            int curveSign = 0;
            double curveIntensity = 0;

            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                if (!p.isOnline() || !display.isValid()) {
                    this.cancel();
                    return;
                }

                currentAngle += 0.35f;

                double stepSize = 0.25;
                int steps = (int) Math.ceil(speed / stepSize);
                double actualStep = speed / steps;

                for (int s = 0; s < steps; s++) {
                    double nextX = currentLoc.getX() + flatDirection.getX() * actualStep;
                    double nextZ = currentLoc.getZ() + flatDirection.getZ() * actualStep;

                    int nX = (int) Math.floor(nextX);
                    int nZ = (int) Math.floor(nextZ);

                    int startY = (int) Math.floor(currentLoc.getY() + 3.0);
                    int endY = (int) Math.floor(currentLoc.getY() - 4.0);
                    double highestSolidY = Double.NEGATIVE_INFINITY;

                    for (int y = startY; y >= endY; y--) {
                        Block b = currentLoc.getWorld().getBlockAt(nX, y, nZ);
                        if (b.getType().isSolid()) {
                            highestSolidY = y;
                            break;
                        }
                    }

                    boolean isClimbing = false;
                    if (highestSolidY != Double.NEGATIVE_INFINITY) {
                        double targetY = highestSolidY + 2.15;

                        if (targetY > currentLoc.getY() + 0.2) {
                            isClimbing = true;
                            double climbStep = actualStep * 0.45;
                            currentLoc.add(0, climbStep, 0);
                            distanceTraveled += climbStep;

                            Block wallBlock1 = currentLoc.getWorld().getBlockAt(nX, (int) Math.floor(currentLoc.getY()), nZ);
                            Block wallBlock2 = currentLoc.getWorld().getBlockAt(nX, (int) Math.floor(currentLoc.getY() - 1.0), nZ);
                            infectBlock(wallBlock1, trail);
                            infectBlock(wallBlock2, trail);
                            infectBlock(wallBlock1.getLocation().add(right).getBlock(), trail);
                            infectBlock(wallBlock2.getLocation().add(right).getBlock(), trail);

                        } else {
                            currentLoc.setX(nextX);
                            currentLoc.setZ(nextZ);
                            currentLoc.setY(targetY);
                            distanceTraveled += actualStep;

                            Block centerBlock = currentLoc.getWorld().getBlockAt(nX, (int) highestSolidY, nZ);
                            Block sideBlock = centerBlock.getLocation().add(right).getBlock();
                            infectBlock(centerBlock, trail);
                            infectBlock(sideBlock, trail);
                        }
                    } else {
                        currentLoc.setX(nextX);
                        currentLoc.setZ(nextZ);
                        currentLoc.subtract(0, actualStep, 0);
                        distanceTraveled += actualStep;
                    }

                    trampleVegetation(currentLoc, right);

                    double pct = distanceTraveled / maxDist;
                    if (pct >= curveThreshold && !isClimbing) {
                        if (!isCurving) {
                            isCurving = true;
                            curveSign = random.nextBoolean() ? 1 : -1;
                            curveIntensity = 0.015 + random.nextDouble() * 0.015;
                        }
                        flatDirection.rotateAroundY(curveSign * curveIntensity);
                    }

                    if (distanceTraveled >= maxDist) {
                        playImpactBurst(currentLoc, flatDirection);
                        display.remove();
                        if (!trail.isEmpty()) clearTrailGradually(trail);
                        this.cancel();
                        return;
                    }

                    boolean hit = false;
                    for (LivingEntity victim : currentLoc.getWorld().getLivingEntities()) {
                        if (victim.equals(p) || victim.isDead() || hitTargets.contains(victim.getUniqueId())) continue;

                        if (victim.getLocation().distanceSquared(currentLoc) < 5.0) {
                            if (victim instanceof Player targetPlayer) {
                                if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                                if (plugin.getFriendManager() != null && plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) continue;
                            }

                            hitTargets.add(victim.getUniqueId());
                            hitEntity(p, victim, trail);
                            playImpactBurst(victim.getLocation().add(0, 1.0, 0), flatDirection);
                            display.remove();
                            hit = true;
                            break;
                        }
                    }

                    if (hit) {
                        this.cancel();
                        return;
                    }
                }

                if (display.isValid()) {
                    Location dirLoc = currentLoc.clone();
                    dirLoc.setDirection(flatDirection);
                    updateDisplayRotation(display, currentLoc, dirLoc.getYaw(), currentAngle);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void trampleVegetation(Location loc, Vector right) {
        Block[] blocksToCheck = {
                loc.getBlock(),
                loc.getBlock().getRelative(BlockFace.UP),
                loc.getBlock().getRelative(BlockFace.DOWN),
                loc.clone().add(right).getBlock(),
                loc.clone().subtract(right).getBlock()
        };

        for (Block b : blocksToCheck) {
            Material m = b.getType();
            if (isPlantOrWeb(m)) {
                b.breakNaturally();
            }
        }
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

    private void updateDisplayRotation(BlockDisplay display, Location target, float dynamicYaw, float currentAngle) {
        Location loc = target.clone();
        loc.setYaw(dynamicYaw);
        loc.setPitch(0);

        display.setInterpolationDelay(0);
        display.setInterpolationDuration(1);

        Vector3f rotAxis = new Vector3f(1f, 0f, 0f);
        Quaternionf q = new Quaternionf().rotationAxis(currentAngle, rotAxis.x, rotAxis.y, rotAxis.z);
        Transformation t = new Transformation(new Vector3f(-0.9f, -0.75f, -0.9f).rotate(q), q, new Vector3f(1.8f, 1.8f, 1.8f), new Quaternionf());
        display.setTransformation(t);
        display.teleport(loc);
    }

    public void playImpactBurst(Location center, Vector dir) {
        center.getWorld().playSound(center, Sound.BLOCK_MUD_BREAK, 1.65f, 0.4f);
        center.getWorld().playSound(center, Sound.BLOCK_MUD_BREAK, 1.5f, 0.01f);
        center.getWorld().playSound(center, Sound.BLOCK_PACKED_MUD_BREAK, 1.2f, 0.4f);

        Location baseLoc = center.clone().add(0, -0.2, 0);
        ParticleUtils.spawnBlockDispersion(plugin, baseLoc, mudBlockData, 4.8);
    }
    private void infectBlock(Block toInfect, LinkedList<BlockState> trail) {
        if (toInfect.getType().isSolid() && toInfect.getType() != Material.MUD && toInfect.getType() != Material.BEDROCK) {
            trail.add(toInfect.getState());
            toInfect.setType(Material.MUD);
            toInfect.getWorld().playSound(toInfect.getLocation(), Sound.BLOCK_MUD_PLACE, 1.3f, 0.4f);
            ParticleUtils.spawnBlockDispersion(plugin, toInfect.getLocation().add(0.5, 0.5, 0.5), mudBlockData, 1.2, 16);
        }
    }

    private void hitEntity(Player p, LivingEntity victim, LinkedList<BlockState> trail) {
        if (plugin.getWorldGuardManager() != null
                && !plugin.getWorldGuardManager().canAffectTarget(p, victim)) {
            return;
        }

        double mudDmg = plugin.getWeaponsConfig().getDouble("earth-gauntlet.mudslide.damage", 4.0);
        plugin.getCleanDamageManager().apply(victim, p, mudDmg);
        victim.playHurtAnimation(0.0F);

        p.getWorld().playSound(victim.getLocation(), Sound.BLOCK_SPONGE_ABSORB, 1f, 0.3f);
        p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.MASTER, 1f, 0.7f);

        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, false, false, true));
        // 2x spread and count when mud projectile hits victim
        ParticleUtils.spawnBlockDispersion(plugin, victim.getLocation().add(0, 0.5, 0), mudBlockData, 9.0, 170);

        int debuffTime = plugin.getWeaponsConfig().getInt("earth-gauntlet.mudslide.debuff_time_seconds", 4);

        if (victim instanceof Player pVictim) {
            bossBarManager.setDebuffBar(pVictim, plugin.tr("§4§l!! §c§lзᴀляпᴀнный грязью §4§l!!", "§4§l!! §c§lᴍᴜᴅᴅᴇᴅ §4§l!!"), debuffTime);
        }

        bossBarManager.setActiveBar(p, "PullPrompt", plugin.tr("§2§l!! §a§lприᴛянуᴛь цᴇль §2§l!!", "§2§l!! §a§lᴩᴜʟʟ ᴛᴀʀɢᴇᴛ §2§l!!"), debuffTime);

        pullableTargets.put(p.getUniqueId(), victim.getUniqueId());
        activeMudTrails.put(p.getUniqueId(), trail);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pullableTargets.containsKey(p.getUniqueId()) && pullableTargets.get(p.getUniqueId()).equals(victim.getUniqueId())) {
                    pullableTargets.remove(p.getUniqueId());
                    bossBarManager.removeActiveBar(p, "PullPrompt");
                    if (activeMudTrails.containsKey(p.getUniqueId())) {
                        clearTrailGradually(activeMudTrails.remove(p.getUniqueId()));
                    }
                }
            }
        }.runTaskLater(plugin, debuffTime * 20L);
    }

    public boolean hasMuddiedTarget(Player p) {
        return pullableTargets.containsKey(p.getUniqueId());
    }

    public void pullTarget(Player p) {
        UUID victimId = pullableTargets.remove(p.getUniqueId());
        bossBarManager.removeActiveBar(p, "PullPrompt");

        if (activeMudTrails.containsKey(p.getUniqueId())) {
            clearTrailGradually(activeMudTrails.remove(p.getUniqueId()));
        }

        Entity entity = Bukkit.getEntity(victimId);
        if (!(entity instanceof LivingEntity victim) || victim.isDead()) return;
        if (plugin.getWorldGuardManager() != null
                && !plugin.getWorldGuardManager().canAffectTarget(p, victim)) {
            return;
        }

        if (victim instanceof Player pVictim) {
            bossBarManager.removeDebuffBar(pVictim);
        }
        victim.removePotionEffect(PotionEffectType.SLOWNESS);

        Location pLoc = p.getLocation();
        Location vLoc = victim.getLocation();

        Vector dirAway = vLoc.toVector().subtract(pLoc.toVector()).normalize();
        vLoc.setDirection(dirAway);
        victim.teleport(vLoc);

        new BukkitRunnable() {
            int ticks = 0;
            final double pullSpeed = plugin.getWeaponsConfig().getDouble("earth-gauntlet.mudslide.pull_speed", 1.5);

            @Override
            public void run() {
                if (!p.isOnline() || !victim.isValid() || victim.isDead()) { this.cancel(); return; }
                if (plugin.getWorldGuardManager() != null
                        && !plugin.getWorldGuardManager().canAffectTarget(p, victim)) {
                    this.cancel();
                    return;
                }

                Location currentVLoc = victim.getLocation();
                Location targetPLoc = p.getLocation().add(0, 1, 0);

                if (currentVLoc.getBlock().getRelative(BlockFace.UP).getType().isSolid()) {
                    victim.setVelocity(new Vector(0, -1.0, 0));
                }

                Vector diff = targetPLoc.toVector().subtract(currentVLoc.toVector().add(new Vector(0, 1, 0)));
                double beamDist = diff.length();
                if (beamDist > 0.5) {
                    Vector directionStep = diff.normalize();
                    for (double d = 0; d < beamDist; d += 0.4) {
                        Location point = currentVLoc.clone().add(0, 1, 0).add(directionStep.clone().multiply(d));
                        point.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, point, 1, 0.0, 0.0, 0.0, 0.0, mudBlockData);
                    }
                }

                double hDistSquared = Math.pow(currentVLoc.getX() - targetPLoc.getX(), 2) + Math.pow(currentVLoc.getZ() - targetPLoc.getZ(), 2);

                if (hDistSquared < 1.0 || currentVLoc.distanceSquared(targetPLoc) < 2.0 || ticks > 60) {
                    victim.setVelocity(new Vector(0, -0.1, 0));
                    victim.setFallDistance(0);
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
                    this.cancel();
                    return;
                }

                if (ticks % 2 == 0) {
                    victim.getWorld().playSound(currentVLoc, Sound.BLOCK_PACKED_MUD_BREAK, 1f, 0.4f);
                }

                Vector pullVec = targetPLoc.toVector().subtract(currentVLoc.toVector());
                double dist = pullVec.length();

                if (dist > pullSpeed) {
                    pullVec.normalize().multiply(pullSpeed);
                }

                if (currentVLoc.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                    if (pullVec.getY() < 0.3) {
                        pullVec.setY(0.3);
                    }
                }

                victim.setVelocity(pullVec);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void clearTrailGradually(LinkedList<BlockState> trail) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trail.isEmpty()) {
                    this.cancel();
                    return;
                }

                BlockState state = trail.poll();
                state.update(true, false);
                state.getBlock().getWorld().playSound(state.getLocation(), Sound.BLOCK_MUD_BREAK, 1f, 0.4f);
                ParticleUtils.spawnBlockDispersion(plugin, state.getLocation().add(0.5, 0.5, 0.5), mudBlockData, 2.5);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void cleanupMeteor(Player p) {
        if (activeMeteorStrikes.containsKey(p.getUniqueId())) {
            activeMeteorStrikes.get(p.getUniqueId()).cancel();
            activeMeteorStrikes.remove(p.getUniqueId());
        }
    }

    public void resetCooldowns(Player player) {
        cleanupMeteor(player);
        bossBarManager.resetPlayer(player);
        pullableTargets.remove(player.getUniqueId());
        LinkedList<BlockState> trail = activeMudTrails.remove(player.getUniqueId());
        if (trail != null) {
            clearTrailGradually(trail);
        }
    }

    public void cleanupAll() {
        for (BukkitTask task : activeMeteorStrikes.values()) {
            if (task != null) task.cancel();
        }
        activeMeteorStrikes.clear();
        pullableTargets.clear();
        stunnedTargets.clear();
        for (LinkedList<BlockState> trail : activeMudTrails.values()) {
            if (trail != null) {
                while (!trail.isEmpty()) {
                    BlockState s = trail.poll();
                    if (s != null) s.update(true, false);
                }
            }
        }
        activeMudTrails.clear();
        bossBarManager.clearAll();
    }
}
