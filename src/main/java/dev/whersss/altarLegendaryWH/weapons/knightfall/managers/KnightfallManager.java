package dev.whersss.altarLegendaryWH.weapons.knightfall.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KnightfallManager {
    private static final int THROW_INTERPOLATION_TICKS = 2;

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Integer> hookCharges = new HashMap<>();
    private final Map<UUID, Long> hookNextCharge = new HashMap<>();
    private final Map<UUID, BossBar> hookBars = new HashMap<>();
    private final Map<UUID, BossBar> cloakCooldownBars = new HashMap<>();
    private final Map<UUID, Long> cloakCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> throwCooldownBars = new HashMap<>();
    private final Map<UUID, Long> throwCooldowns = new HashMap<>();

    public KnightfallManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        startChargeTask();
    }

    private boolean useParticleChains() {
        return plugin.getWeaponsConfig().getBoolean("knightfall.chain.use-particles", true);
    }

    public boolean canUseCloak(Player player) {
        return cloakCooldowns.getOrDefault(player.getUniqueId(), 0L) <= System.currentTimeMillis();
    }

    public void triggerCloak(Player player) {
        if (plugin.isAboveLegendaryHeight(player)) return;

        int duration = plugin.getWeaponsConfig().getInt("knightfall.cloak.duration", 3) * 20;
        int cooldown = plugin.getWeaponsConfig().getInt("knightfall.cloak.cooldown", 8);

        cloakCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        startCloakCooldownBar(player, cooldown);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.2f);
        spawnBlackDust(player.getLocation());

        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(player.getLocation());
        border.setSize(10000);
        border.setWarningDistance(10000);
        player.setWorldBorder(border);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false, false));
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hidePlayer(plugin, player);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= duration) {
                    player.setWorldBorder(null);
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.showPlayer(plugin, player);
                    }

                    if (player.isOnline()) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.2f);
                        spawnBlackDust(player.getLocation());
                    }
                    cancel();
                } else {
                    player.sendActionBar(TextUtils.legacy(
                            plugin.tr("&c[ &c&lНЕВИДИМОСТЬ &c]", "&c[ &c&lɪɴᴠɪsɪʙɪʟɪᴛʏ &c]")));
                    ticks++;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startCloakCooldownBar(Player player, int seconds) {
        BossBar bar = TextUtils.bossBar(yellowTitle(plugin.tr("Плащ", "ᴄʟᴏᴀᴋ")), BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(player);
        cloakCooldownBars.put(player.getUniqueId(), bar);

        new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / (seconds * 20.0);

            @Override
            public void run() {
                if (!player.isOnline()) {
                    bar.removeAll();
                    cancel();
                    return;
                }
                progress -= step;
                if (progress <= 0) {
                    bar.removeAll();
                    cloakCooldownBars.remove(player.getUniqueId());
                    cancel();
                } else {
                    bar.setProgress(Math.max(0, progress));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnBlackDust(Location loc) {
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.5f);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 60, 0.5, 1.0, 0.5, blackDust);
    }

    public int getCharges(Player player) {
        return hookCharges.getOrDefault(player.getUniqueId(), plugin.getWeaponsConfig().getInt("knightfall.grapple.max-charges", 3));
    }

    private void startChargeTask() {
        int chargeCooldown = plugin.getWeaponsConfig().getInt("knightfall.grapple.cooldown", 10) * 1000;
        int maxCharges = plugin.getWeaponsConfig().getInt("knightfall.grapple.max-charges", 3);

        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID id = player.getUniqueId();
                    int charges = getCharges(player);
                    int usedCharges = maxCharges - charges;

                    if (charges < maxCharges) {
                        long next = hookNextCharge.getOrDefault(id, now);
                        if (now >= next) {
                            hookCharges.put(id, charges + 1);
                            hookNextCharge.put(id, now + chargeCooldown);
                        } else {
                            updateHookBar(player, usedCharges, (double) (next - now) / chargeCooldown);
                            continue;
                        }
                    }

                    if (getCharges(player) == maxCharges && hookBars.containsKey(id)) {
                        hookBars.remove(id).removeAll();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void updateHookBar(Player player, int usedCharges, double timeRemainingRaw) {
        if (!hookBars.containsKey(player.getUniqueId())) {
            BossBar bar = TextUtils.bossBar("", BarColor.YELLOW, BarStyle.SOLID);
            bar.addPlayer(player);
            hookBars.put(player.getUniqueId(), bar);
        }
        BossBar bar = hookBars.get(player.getUniqueId());
        bar.setTitle(yellowTitle(plugin.tr("Абордажный крюк x", "ɢʀᴀᴘᴘʟɪɴɢ ʜᴏᴏᴋ x") + usedCharges));
        bar.setProgress(Math.max(0, Math.min(1.0, 1.0 - timeRemainingRaw)));
    }

    private void updateDynamicChain(Player player, Location target, List<BlockDisplay> chainLinks) {
        Location startLoc = player.getLocation().add(0, 1.0, 0);
        if (useParticleChains()) {
            for (BlockDisplay display : chainLinks) {
                display.remove();
            }
            chainLinks.clear();
            spawnParticleChain(startLoc, target);
            return;
        }

        double step = Math.max(0.22, plugin.getWeaponsConfig().getDouble("knightfall.chain.segment-length", 0.22));
        double dist = startLoc.distance(target);
        Vector tempDir = target.toVector().subtract(startLoc.toVector());

        final Vector dir = (tempDir.lengthSquared() > 0) ? tempDir.normalize() : new Vector(0, 1, 0);
        int linksNeeded = (int) Math.ceil(dist / step);

        for (int i = 0; i < linksNeeded; i++) {
            Location linkLoc = startLoc.clone().add(dir.clone().multiply(i * step));
            linkLoc.setDirection(dir);

            if (i >= chainLinks.size()) {
                BlockDisplay display = linkLoc.getWorld().spawn(linkLoc, BlockDisplay.class, ent -> {
                    ent.setBlock(Material.IRON_CHAIN.createBlockData());
                    ent.setTeleportDuration(0);
                    updateChainSegmentRotation(ent);
                });
                chainLinks.add(display);
            } else {
                BlockDisplay display = chainLinks.get(i);
                display.setTeleportDuration(0);
                display.teleport(linkLoc);
                updateChainSegmentRotation(display);
            }
        }

        while (chainLinks.size() > linksNeeded) {
            BlockDisplay display = chainLinks.removeLast();
            display.remove();
        }
    }

    private void spawnParticleChain(Location startLoc, Location target) {
        double step = Math.max(0.22, plugin.getWeaponsConfig().getDouble("knightfall.chain.segment-length", 0.22));
        Vector delta = target.toVector().subtract(startLoc.toVector());
        double length = Math.max(0.001, delta.length());
        Vector dir = delta.clone().normalize();
        org.bukkit.block.data.BlockData chainData = Material.IRON_CHAIN.createBlockData();

        int points = Math.max(1, (int) Math.ceil(length / step));
        for (int i = 0; i <= points; i++) {
            Location point = startLoc.clone().add(dir.clone().multiply(i * step));
            point.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, point, 2, 0.015, 0.015, 0.015, chainData);
        }
    }

    private void updateChainSegmentRotation(BlockDisplay ent) {
        Quaternionf q = new Quaternionf().rotationX((float) Math.PI / 2);
        Vector3f translation = new Vector3f(-0.5f, 0f, -0.5f).rotate(q);

        Transformation t = new Transformation(
                translation,
                q,
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()
        );

        ent.setInterpolationDelay(0);
        ent.setInterpolationDuration(1);
        ent.setTransformation(t);
    }

    public void playKillEffect(Location loc) {
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.5f);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 100, 0.5, 1.0, 0.5, blackDust);

        new BukkitRunnable() {
            int ticks = 0;
            final java.util.List<Location> particlesLocs = new java.util.ArrayList<>();
            final java.util.List<Vector> directions = new java.util.ArrayList<>();

            {
                for (int i = 0; i < 25; i++) {
                    double x = (Math.random() - 0.5) * 1.5;
                    double z = (Math.random() - 0.5) * 1.5;
                    particlesLocs.add(loc.clone().add(x, 1.0, z));

                    double motionX = (Math.random() - 0.5) * 1.1;
                    double motionY = 0.25 + (Math.random() * 0.35);
                    double motionZ = (Math.random() - 0.5) * 1.1;
                    directions.add(new Vector(motionX, motionY, motionZ));
                }
            }

            @Override
            public void run() {
                if (ticks > 40) {
                    cancel();
                    return;
                }
                for (int i = 0; i < particlesLocs.size(); i++) {
                    Location pLoc = particlesLocs.get(i);
                    Vector moveDir = directions.get(i);

                    pLoc.add(moveDir.getX(), moveDir.getY(), moveDir.getZ());

                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pLoc, 1, 0, 0, 0, 0.0);
                    loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, blackDust);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    public void useHook(Player player) {
        if (plugin.isAboveLegendaryHeight(player)) return;

        int charges = getCharges(player);
        if (charges <= 0) return;

        Location startDirLoc = player.getEyeLocation();
        Vector dir = startDirLoc.getDirection().normalize();
        double maxDist = plugin.getWeaponsConfig().getDouble("knightfall.grapple.range", 25.0);
        RayTraceResult narrowTrace = player.getWorld().rayTrace(
                startDirLoc, dir, maxDist, org.bukkit.FluidCollisionMode.NEVER, true, 0.3,
                entity -> !entity.equals(player) && !(entity instanceof Projectile)
        );
        RayTraceResult wideTrace = player.getWorld().rayTrace(
                startDirLoc, dir, maxDist, org.bukkit.FluidCollisionMode.NEVER, true, 2.0,
                entity -> entity instanceof Projectile && !entity.equals(player)
        );
        RayTraceResult rayTrace = null;
        if (narrowTrace != null && wideTrace != null) {
            double d1 = narrowTrace.getHitPosition().distanceSquared(startDirLoc.toVector());
            double d2 = wideTrace.getHitPosition().distanceSquared(startDirLoc.toVector());
            rayTrace = (d1 < d2) ? narrowTrace : wideTrace;
        } else if (narrowTrace != null) {
            rayTrace = narrowTrace;
        } else {
            rayTrace = wideTrace;
        }

        if (rayTrace == null) return;

        Location targetLoc = null;
        boolean isFloor = false;

        if (rayTrace.getHitBlock() != null) {
            targetLoc = rayTrace.getHitPosition().toLocation(player.getWorld());
            if (rayTrace.getHitBlockFace() == org.bukkit.block.BlockFace.UP) {
                isFloor = true;
            }
        } else if (rayTrace.getHitEntity() != null) {
            Entity hitEnt = rayTrace.getHitEntity();
            if (hitEnt instanceof LivingEntity || hitEnt instanceof Projectile) {
                targetLoc = hitEnt.getLocation().add(0, hitEnt.getHeight() / 2, 0);
                isFloor = true;
                if (hitEnt instanceof LivingEntity victim) {
                    if (victim instanceof Player && victim.getHealth() <= 2.0) {
                        playKillEffect(victim.getLocation());
                    }
                    CombatUtils.runSyntheticDamage(() -> victim.damage(2.0, player));
                }
            }
        }

        if (targetLoc == null) return;

        hookCharges.put(player.getUniqueId(), charges - 1);
        if (charges == plugin.getWeaponsConfig().getInt("knightfall.grapple.max-charges", 3)) {
            hookNextCharge.put(player.getUniqueId(), System.currentTimeMillis() + (plugin.getWeaponsConfig().getInt("knightfall.grapple.cooldown", 10) * 1000L));
        }

        player.swingMainHand();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 2.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 2f);

        List<BlockDisplay> chainLinks = new ArrayList<>();
        updateDynamicChain(player, targetLoc, chainLinks);

        pullPlayer(player, targetLoc, chainLinks, isFloor);
    }

    private void pullPlayer(Player player, Location target, List<BlockDisplay> chainLinks, boolean isFloor) {
        target.getWorld().spawnParticle(Particle.EXPLOSION, target, 1, 0.0, 0.0, 0.0, 0.0);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target, 1, 0.0, 0.0, 0.0, 0.0);
        target.getWorld().spawnParticle(Particle.BLOCK, target, 25, 0.3, 0.3, 0.3, Material.IRON_CHAIN.createBlockData());
        player.getWorld().playSound(target, Sound.BLOCK_CHAIN_BREAK, 1f, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            Location lastLoc = player.getLocation();
            int stuckTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    chainLinks.forEach(Entity::remove);
                    cancel();
                    return;
                }

                updateDynamicChain(player, target, chainLinks);

                if (ticks % 1 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_STEP, 2.0f, 0.9f);
                }

                if (player.getLocation().distanceSquared(target) < 2.5 || player.getEyeLocation().distanceSquared(target) < 2.5) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 2.0f, 1.0f);

                    if (isFloor) {
                        Vector boost = player.getLocation().getDirection().setY(0).normalize().multiply(0.5).setY(1.1);
                        player.setVelocity(boost);
                    }

                    chainLinks.forEach(Entity::remove);
                    cancel();
                    return;
                }

                if (ticks > 0 && ticks % 5 == 0) {
                    if (player.getLocation().distanceSquared(lastLoc) < 0.2) {
                        stuckTicks++;
                        if (stuckTicks >= 2) {
                            if (isFloor) {
                                Vector boost = player.getLocation().getDirection().setY(0).normalize().multiply(0.5).setY(1.1);
                                player.setVelocity(boost);
                            } else {
                                player.setVelocity(new Vector(0, 1.2, 0));
                            }
                            chainLinks.forEach(Entity::remove);
                            cancel();
                            return;
                        }
                    } else {
                        stuckTicks = 0;
                    }
                    lastLoc = player.getLocation();
                }

                Vector pullVec = target.toVector().subtract(player.getLocation().toVector());
                double distance = pullVec.length();
                if (distance > 0.001) {
                    pullVec.normalize().multiply(Math.min(1.6, 0.5 + (distance * 0.12)));
                }
                player.setVelocity(pullVec);

                if (ticks > 60) {
                    chainLinks.forEach(Entity::remove);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void throwHammer(Player player, ItemStack item) {
        if (plugin.isAboveLegendaryHeight(player)) return;
        if (throwCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) return;

        int cooldown = plugin.getWeaponsConfig().getInt("knightfall.throw.cooldown", 20);
        throwCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        startThrowCooldownBar(player, cooldown);

        player.getInventory().setItemInMainHand(null);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 2.0f);

        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        double maxDist = plugin.getWeaponsConfig().getDouble("knightfall.throw.range", 30.0);
        double configDmg = plugin.getWeaponsConfig().getDouble("knightfall.throw.damage", 15.0);
        double flightSpeed = plugin.getWeaponsConfig().getDouble("knightfall.throw.flight-speed", 1.5);
        double returnSpeed = plugin.getWeaponsConfig().getDouble("knightfall.throw.return-speed", 1.5);
        int turningDelayTicks = plugin.getWeaponsConfig().getInt("knightfall.throw.turning-delay-ticks", 15);

        ItemDisplay display = player.getWorld().spawn(start, ItemDisplay.class, ent -> {
            ent.setItemStack(item);
            ent.setInterpolationDuration(THROW_INTERPOLATION_TICKS);
            ent.setTeleportDuration(THROW_INTERPOLATION_TICKS);
            ent.setInterpolationDelay(0);
            Transformation t = ent.getTransformation();
            t.getLeftRotation().set(new Quaternionf().rotateX((float) Math.toRadians(90)));
            ent.setTransformation(t);
        });

        new BukkitRunnable() {
            final int flyingState = 0;
            final int turningState = 1;
            final int returningState = 2;
            final int waitingState = 3;

            double traveled = 0;
            Location curr = start.clone();
            Entity caughtEntity = null;
            int state = flyingState;
            int stateTimer = 0;
            float roll = 0f;
            final List<BlockDisplay> throwChain = new ArrayList<>();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    if (player.isDead() && state == waitingState) {
                        curr.getWorld().playSound(curr, Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.6f);
                    }
                    curr.getWorld().dropItem(curr, item);
                    display.remove();
                    throwChain.forEach(Entity::remove);
                    if (caughtEntity != null) caughtEntity.setFallDistance(0);
                    cancel();
                    return;
                }

                Location playerHandLoc = player.getEyeLocation().subtract(0, 0.2, 0);
                if (state != waitingState) {
                    updateDynamicChain(player, curr, throwChain);
                }

                if (state == flyingState) {
                    LivingEntity targetVictim = null;
                    for (Entity target : curr.getWorld().getNearbyEntities(curr, 1.4, 1.4, 1.4)) {
                        if (target.equals(player) || target.equals(display) || target.isDead()) continue;
                        if (target instanceof LivingEntity entity) {
                            if (!(entity instanceof Player targetPlayer)
                                    || !plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                targetVictim = entity;
                                break;
                            }
                        }
                    }

                    if (targetVictim != null) {
                        if (targetVictim instanceof Player targetPlayer) {
                            boolean hasTotem = targetPlayer.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                                    targetPlayer.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;

                            if (targetPlayer.getHealth() - configDmg <= 0.0 && !hasTotem) {
                                playKillEffect(targetVictim.getLocation());
                            }
                        }
                        plugin.getCleanDamageManager().apply(targetVictim, player, configDmg);
                        player.getWorld().playSound(curr, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.5f);
                        caughtEntity = targetVictim;
                        state = turningState;
                        stateTimer = turningDelayTicks;
                        display.setInterpolationDelay(0);
                        display.setInterpolationDuration(turningDelayTicks);
                    } else {
                        RayTraceResult result = curr.getWorld().rayTrace(
                                curr, dir.clone(), flightSpeed + 0.2,
                                org.bukkit.FluidCollisionMode.NEVER, true, 0.0,
                                entity -> false
                        );

                        if (result != null && result.getHitBlock() != null) {
                            curr = result.getHitPosition().toLocation(curr.getWorld()).subtract(dir.clone().multiply(0.65));
                            display.teleport(curr);
                            state = turningState;
                            stateTimer = turningDelayTicks;
                            display.setInterpolationDelay(0);
                            display.setInterpolationDuration(turningDelayTicks);
                        }
                    }

                    if (state == flyingState) {
                        traveled += flightSpeed;
                        curr.add(dir.clone().multiply(flightSpeed));
                        display.setInterpolationDelay(0);
                        display.setInterpolationDuration(THROW_INTERPOLATION_TICKS);
                        display.setTeleportDuration(THROW_INTERPOLATION_TICKS);
                        roll -= 0.56f;
                        Transformation transformation = display.getTransformation();
                        transformation.getLeftRotation().set(new Quaternionf().rotateX((float) (Math.PI / 2)).rotateY((float) Math.PI + roll));
                        display.setTransformation(transformation);
                        curr.setDirection(dir);
                        display.teleport(curr);
                    }

                    curr.getWorld().spawnParticle(Particle.CRIT, curr, 2, 0.05, 0.05, 0.05, 0.0);

                    if (traveled >= maxDist && state == flyingState) {
                        state = turningState;
                        stateTimer = turningDelayTicks;
                        display.setInterpolationDelay(0);
                        display.setInterpolationDuration(turningDelayTicks);
                    }
                } else if (state == turningState || state == returningState) {
                    if (caughtEntity != null && caughtEntity.getLocation().getBlock().getType() == Material.COBWEB) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 1f);
                        caughtEntity.setFallDistance(0);
                        caughtEntity = null;
                    }

                    Vector toPlayer = playerHandLoc.toVector().subtract(curr.toVector());
                    double currentReturnSpeed = state == turningState ? returnSpeed / 3.0 : returnSpeed;

                    if (state == turningState && --stateTimer <= 0) {
                        state = returningState;
                        display.setInterpolationDuration(THROW_INTERPOLATION_TICKS);
                    }

                    if (toPlayer.length() < 2.0) {
                        if (caughtEntity != null) {
                            caughtEntity.setFallDistance(0);
                            caughtEntity.setVelocity(new Vector(0, 0, 0));
                            caughtEntity = null;
                        }

                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(item);
                            display.remove();
                            throwChain.forEach(Entity::remove);
                            cancel();
                        } else {
                            state = waitingState;
                            stateTimer = 15 * 20;
                            throwChain.forEach(Entity::remove);
                            throwChain.clear();
                        }
                    } else {
                        Vector returnStep = toPlayer.normalize().multiply(currentReturnSpeed);
                        curr.add(returnStep);
                        curr.setDirection(dir);

                        display.setInterpolationDelay(0);
                        display.setInterpolationDuration(THROW_INTERPOLATION_TICKS);
                        display.setTeleportDuration(THROW_INTERPOLATION_TICKS);

                        roll -= 0.56f;
                        Transformation transformation = display.getTransformation();
                        transformation.getLeftRotation().set(new Quaternionf().rotateX((float) (Math.PI / 2)).rotateY((float) Math.PI + roll));
                        display.setTransformation(transformation);
                        display.teleport(curr);

                        curr.getWorld().spawnParticle(Particle.CRIT, curr, 2, 0.05, 0.05, 0.05, 0.0);

                        if (caughtEntity != null && !caughtEntity.isDead()) {
                            Vector pullVector = curr.toVector().subtract(caughtEntity.getLocation().toVector());
                            caughtEntity.setVelocity(pullVector);
                            caughtEntity.setFallDistance(0);
                        }
                    }
                } else if (state == waitingState) {
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(item);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                        display.remove();
                        cancel();
                        return;
                    }

                    stateTimer--;
                    if (stateTimer <= 0) {
                        player.getWorld().dropItem(player.getLocation(), item);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.6f);
                        display.remove();
                        cancel();
                        return;
                    }
                    double orbitRadius = 0.8;
                    double orbitSpeed = 0.4;
                    double currentAngle = (15 * 20 - stateTimer) * orbitSpeed;

                    curr = player.getLocation().add(
                            Math.cos(currentAngle) * orbitRadius,
                            2.3,
                            Math.sin(currentAngle) * orbitRadius
                    );

                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(1);
                    display.setTeleportDuration(1);

                    roll -= 0.56f;
                    Transformation transformation = display.getTransformation();
                    transformation.getLeftRotation().set(new Quaternionf().rotateX((float) (Math.PI / 2)).rotateY((float) Math.PI + roll));
                    display.setTransformation(transformation);
                    display.teleport(curr);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startThrowCooldownBar(Player player, int seconds) {
        BossBar bar = TextUtils.bossBar(yellowTitle(plugin.tr("Бросок молота", "ʜᴀᴍᴍᴇʀ ᴛʜʀᴏᴡ")), BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(player);
        throwCooldownBars.put(player.getUniqueId(), bar);

        new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / (seconds * 20.0);

            @Override
            public void run() {
                if (!player.isOnline()) {
                    bar.removeAll();
                    cancel();
                    return;
                }
                progress -= step;
                if (progress <= 0) {
                    bar.removeAll();
                    throwCooldownBars.remove(player.getUniqueId());
                    cancel();
                } else {
                    bar.setProgress(Math.max(0, progress));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private String yellowTitle(String title) {
        if (title == null) return "";
        return "§e" + title.replaceAll("(?i)[§&][0-9A-FK-OR]", "");
    }

    public void resetCooldowns(Player player) {
        UUID uuid = player.getUniqueId();
        hookCharges.remove(uuid);
        hookNextCharge.remove(uuid);
        cloakCooldowns.remove(uuid);
        throwCooldowns.remove(uuid);

        BossBar hookBar = hookBars.remove(uuid);
        BossBar cloakBar = cloakCooldownBars.remove(uuid);
        BossBar throwBar = throwCooldownBars.remove(uuid);
        if (hookBar != null) hookBar.removeAll();
        if (cloakBar != null) cloakBar.removeAll();
        if (throwBar != null) throwBar.removeAll();
    }
}

