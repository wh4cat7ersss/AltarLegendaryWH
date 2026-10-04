package dev.whersss.altarLegendaryWH.weapons.shadowblade.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.*;

public class ShadowBladeManager {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, BossBar> leapCooldownBars = new HashMap<>();
    private final Map<UUID, Long> leapCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> daggersCooldownBars = new HashMap<>();
    private final Map<UUID, Long> daggersCooldowns = new HashMap<>();
    private final Map<UUID, Long> backstabCooldowns = new HashMap<>();

    public final Set<UUID> activeLeaps = new HashSet<>();
    public final Map<UUID, Location> currentLeapLocs = new HashMap<>();
    public final Map<UUID, Long> leapStartTimes = new HashMap<>();

    public ShadowBladeManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    public boolean isOnLeapCooldown(Player p) {
        return leapCooldowns.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public boolean isOnDaggersCooldown(Player p) {
        return daggersCooldowns.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public long getLeapStartTime(Player p) {
        return leapStartTimes.getOrDefault(p.getUniqueId(), 0L);
    }

    public void castShadowLeap(Player p) {
        if (isOnLeapCooldown(p) || activeLeaps.contains(p.getUniqueId())) return;

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        spawnBlackDust(p.getLocation());

        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(p.getLocation());
        border.setSize(10000);
        border.setWarningDistance(10000);
        p.setWorldBorder(border);

        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false, false));
        for (Player online : Bukkit.getOnlinePlayers()) online.hidePlayer(plugin, p);

        activeLeaps.add(p.getUniqueId());
        leapStartTimes.put(p.getUniqueId(), System.currentTimeMillis());

        BossBar activeBar = TextUtils.bossBar(plugin.tr("§7§l! §f§lнᴇʙидимоᴄᴛь §7§l!", "§7§l! §f§lɪɴᴠɪsɪʙɪʟɪᴛʏ §7§l!"), BarColor.WHITE, BarStyle.SOLID);
        activeBar.addPlayer(p);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 30;
            Location actualTargetLoc = p.getEyeLocation().clone();

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead() || ticks >= maxTicks || !activeLeaps.contains(p.getUniqueId())) {
                    activeBar.removeAll();

                    p.setWorldBorder(null);
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                    for (Player online : Bukkit.getOnlinePlayers()) online.showPlayer(plugin, p);

                    Location tpLoc = actualTargetLoc.clone();
                    tpLoc.setYaw(p.getLocation().getYaw());
                    tpLoc.setPitch(p.getLocation().getPitch());

                    p.teleport(tpLoc);
                    spawnBlackDust(p.getLocation());
                    p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.3f);
                    p.swingMainHand();

                    double endImpulse = plugin.getConfig().getDouble("shadow-blade.leap.end-impulse", 0.85);
                    Vector dashImpulse = p.getLocation().getDirection().normalize().multiply(endImpulse).setY(0.2);
                    p.setVelocity(dashImpulse);

                    activeLeaps.remove(p.getUniqueId());
                    currentLeapLocs.remove(p.getUniqueId());

                    int cooldown = plugin.getConfig().getInt("shadow-blade.leap.cooldown", 30);
                    leapCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
                    startCooldownBar(p, cooldown, plugin.tr("§f§lᴛᴇнᴇʙой прыжоᴋ", "§f§lsʜᴀᴅᴏᴡ ʟᴇᴀᴘ"), leapCooldownBars, leapCooldowns);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (p.isOnline()) p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                        }
                    }.runTaskLater(plugin, 8L);

                    this.cancel();
                    return;
                }

                double progress = (double) (maxTicks - ticks) / maxTicks;
                activeBar.setProgress(Math.max(0, progress));

                double trailSpeed = plugin.getConfig().getDouble("shadow-blade.leap.trail-speed", 0.6);
                double distTraveled = ticks * trailSpeed;
                Vector dir = p.getLocation().getDirection().normalize();

                RayTraceResult rt = p.getWorld().rayTraceBlocks(p.getEyeLocation(), dir, distTraveled, FluidCollisionMode.NEVER, true);

                if (rt != null && rt.getHitBlock() != null) {
                    actualTargetLoc = rt.getHitPosition().toLocation(p.getWorld());
                    actualTargetLoc.subtract(dir.clone().multiply(0.3));
                } else {
                    actualTargetLoc = p.getEyeLocation().add(dir.clone().multiply(distTraveled));
                }

                currentLeapLocs.put(p.getUniqueId(), actualTargetLoc);

                World world = actualTargetLoc.getWorld();
                if (world != null) {
                    Particle.DustOptions blackDustHead = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.2f);
                    Particle.DustOptions blackDustTail = new Particle.DustOptions(Color.fromRGB(15, 15, 15), 0.9f);

                    int headParticleCount = plugin.getConfig().getInt("shadow-blade.leap.head-particle-count", 8);
                    for (int i = 0; i < headParticleCount; i++) {
                        double rx = (Math.random() - 0.5) * 0.35;
                        double ry = (Math.random() - 0.5) * 0.35;
                        double rz = (Math.random() - 0.5) * 0.35;
                        Location headLoc = actualTargetLoc.clone().add(rx, ry, rz);

                        world.spawnParticle(Particle.DUST, headLoc, 1, 0, 0, 0, 0, blackDustHead);

                        Vector sparkVel = dir.clone().multiply(-0.2).add(new Vector((Math.random() - 0.5) * 0.03, (Math.random() - 0.5) * 0.03, (Math.random() - 0.5) * 0.03));
                        world.spawnParticle(Particle.ELECTRIC_SPARK, headLoc, 0, sparkVel.getX(), sparkVel.getY(), sparkVel.getZ(), 0.12);
                    }

                    double tailLength = plugin.getConfig().getDouble("shadow-blade.leap.tail-length", 2.5);
                    for (double d = 0.05; d <= tailLength; d += 0.04) {
                        double factor = 1.0 - (d / tailLength);
                        double spread = 0.18 * Math.pow(factor, 1.5);

                        Location tailLoc = actualTargetLoc.clone().subtract(dir.clone().multiply(d));

                        for (int k = 0; k < 2; k++) {
                            double ox = (Math.random() - 0.5) * spread;
                            double oy = (Math.random() - 0.5) * spread;
                            double oz = (Math.random() - 0.5) * spread;
                            Location pLoc = tailLoc.clone().add(ox, oy, oz);

                            Vector dustVel = dir.clone().multiply(-0.25 * factor);
                            world.spawnParticle(Particle.DUST, pLoc, 0, dustVel.getX(), dustVel.getY(), dustVel.getZ(), 1.0, blackDustTail);
                        }

                        if (Math.random() < 0.35) {
                            world.spawnParticle(Particle.ELECTRIC_SPARK, tailLoc, 1, 0.02, 0.02, 0.02, 0.01);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void castShadowDaggers(Player p, ItemStack bladeItem) {
        if (isOnDaggersCooldown(p)) return;

        int cooldown = plugin.getConfig().getInt("shadow-blade.daggers.cooldown", 45);
        daggersCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        startCooldownBar(p, cooldown, plugin.tr("§f§lᴛᴇнᴇʙыᴇ ᴋинжᴀлы", "§f§lsʜᴀᴅᴏᴡ ᴅᴀɢɢᴇʀs"), daggersCooldownBars, daggersCooldowns);

        for (long delay = 0; delay <= 4; delay += 2) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.2f);
                }
            }, delay);
        }

        p.swingMainHand();

        Vector dir = p.getLocation().getDirection().normalize();
        playCrossSweep(p.getEyeLocation().add(dir.clone().multiply(1.2)), dir);

        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Location center = p.getEyeLocation();

        Vector[] dirs = {
                dir.clone(),
                dir.clone().add(right.clone().multiply(-0.25)).normalize(),
                dir.clone().add(right.clone().multiply(0.25)).normalize()
        };

        boolean[] soundPlayed = new boolean[]{false};
        int interpTicks = plugin.getConfig().getInt("shadow-blade.daggers.interpolation-ticks", 3);

        for (Vector d : dirs) {
            // Смещаем начальную позицию кинжала ВПЕРЕД от игрока (например, на 1.2 блока вперед)
            Location forwardCenter = center.clone().add(d.clone().multiply(1.2));

            ItemDisplay dagger = p.getWorld().spawn(forwardCenter, ItemDisplay.class, ent -> {
                ent.setItemStack(bladeItem);
                ent.setInterpolationDuration(interpTicks);
                ent.setTeleportDuration(interpTicks);
                Transformation t = ent.getTransformation();
                t.getLeftRotation().set(new Quaternionf().rotateX((float) Math.toRadians(90)));
                t.getScale().set(0.6f, 0.6f, 0.6f);
                ent.setTransformation(t);
            });
            plugin.getVisualCleanupManager().track(dagger);

            new BukkitRunnable() {
                final Location curr = forwardCenter.clone();
                double daggerSpeed = plugin.getConfig().getDouble("shadow-blade.daggers.speed", 1.0);
                Vector currentDir = d.clone().multiply(daggerSpeed * 1.25);
                int ticks = 0;
                float roll = 0;

                @Override
                public void run() {
                    if (ticks > 50 || !dagger.isValid()) {
                        dagger.remove();
                        this.cancel();
                        return;
                    }

                    if (ticks >= 6) {
                        double grav = plugin.getConfig().getDouble("shadow-blade.daggers.gravity", 0.035);
                        currentDir.add(new Vector(0, -grav, 0));
                    }

                    curr.add(currentDir);
                    dagger.teleport(curr);

                    float spinSpeed = (float) plugin.getConfig().getDouble("shadow-blade.daggers.spin-speed", 0.10);
                    roll -= (spinSpeed * 4.5f);
                    Transformation t = dagger.getTransformation();
                    t.getLeftRotation().set(new Quaternionf().rotateX((float) (Math.PI / 2)).rotateY(roll));
                    dagger.setTransformation(t);

                    Vector flyVec = currentDir.clone().normalize();
                    double backOffset = plugin.getConfig().getDouble("shadow-blade.daggers.trail-offset-back", 0.85);
                    Location handleLoc = curr.clone().subtract(flyVec.clone().multiply(backOffset));

                    Vector backStep = flyVec.clone().multiply(-0.18);
                    int sparkCount = plugin.getConfig().getInt("shadow-blade.daggers.spark-count", 8);

                    for (int i = 0; i < sparkCount; i++) {
                        Location sparkLoc = handleLoc.clone().add(backStep.clone().multiply(i));
                        sparkLoc.getWorld().spawnParticle(
                                Particle.ELECTRIC_SPARK,
                                sparkLoc,
                                0,
                                flyVec.getX(), flyVec.getY(), flyVec.getZ(),
                                0.08
                        );
                    }

                    if (curr.getBlock().getType().isSolid()) {
                        Particle.DustOptions hitBlockDust = new Particle.DustOptions(Color.fromRGB(20, 20, 20), 0.8f);
                        curr.getWorld().spawnParticle(Particle.DUST, curr, 15, 0.1, 0.1, 0.1, 0.0, hitBlockDust);
                        curr.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, curr, 6, 0.05, 0.05, 0.05, 0.05);

                        dagger.remove();
                        this.cancel();
                        return;
                    }

                    for (Entity e : curr.getWorld().getNearbyEntities(curr, 0.8, 0.8, 0.8)) {
                        if (e instanceof LivingEntity victim && !victim.equals(p)) {

                            if (victim instanceof Player targetPlayer) {
                                if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                                    continue;
                                }
                            }

                            double dmg = plugin.getConfig().getDouble("shadow-blade.daggers.damage", 2.0);
                            plugin.getCleanDamageManager().apply(victim, p, dmg);

                            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false));

                            Location hitPoint = curr.clone();
                            Particle.DustOptions hitDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);
                            hitPoint.getWorld().spawnParticle(Particle.DUST, hitPoint, 35, 0.2, 0.2, 0.2, 0.0, hitDust);
                            hitPoint.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitPoint, 10, 0.1, 0.1, 0.1, 0.05);

                            if (!soundPlayed[0]) {
                                p.getWorld().playSound(victim.getLocation(), Sound.BLOCK_SPONGE_ABSORB, 1.0f, 0.8f);
                                p.getWorld().playSound(victim.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3.0f, 0.7f);
                                soundPlayed[0] = true;
                            }

                            dagger.remove();
                            this.cancel();
                            return;
                        }
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    public void triggerBackstab(Player attacker, LivingEntity mainVictim) {
        if (backstabCooldowns.getOrDefault(attacker.getUniqueId(), 0L) > System.currentTimeMillis()) return;

        if (mainVictim instanceof Player targetPlayer) {
            if (plugin.getFriendManager().isFriend(attacker.getUniqueId(), targetPlayer.getUniqueId())) {
                return;
            }
        }

        double chance = plugin.getConfig().getDouble("shadow-blade.passive.backstab-chance", 0.25);
        if (Math.random() > chance) return;

        long backstabCd = plugin.getConfig().getLong("shadow-blade.passive.backstab-cooldown", 8);
        backstabCooldowns.put(attacker.getUniqueId(), System.currentTimeMillis() + (backstabCd * 1000L));
        attacker.getWorld().playSound(mainVictim.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

        List<LivingEntity> targets = new ArrayList<>();
        targets.add(mainVictim);

        for (Entity e : mainVictim.getWorld().getNearbyEntities(mainVictim.getLocation(), 5.0, 5.0, 5.0)) {
            if (e instanceof LivingEntity le && !le.equals(attacker) && !le.equals(mainVictim)) {
                if (le instanceof Player p) {
                    if (p.getGameMode() == GameMode.SPECTATOR) continue;
                    if (plugin.getFriendManager().isFriend(attacker.getUniqueId(), p.getUniqueId())) continue;
                }
                targets.add(le);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!attacker.isOnline() || attacker.isDead() || ticks > 10) {
                    this.cancel();
                    return;
                }

                Location aLoc = attacker.getLocation().add(0, 0.5, 0);
                Particle.DustOptions black = new Particle.DustOptions(Color.BLACK, 1.2f);

                for (LivingEntity victim : targets) {
                    if (victim.isDead() || !victim.isValid()) continue;

                    if (ticks == 0) {
                        Vector pullVec = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize().multiply(0.55).setY(0.2);
                        victim.setVelocity(pullVec);
                    }

                    Location vLoc = victim.getLocation().add(0, 0.5, 0);
                    Vector bridge = aLoc.toVector().subtract(vLoc.toVector());
                    double dist = bridge.length();

                    if (dist > 0.5) {
                        Vector step = bridge.normalize().multiply(0.3);

                        for (int j = 0; j < 3; j++) {
                            Location pLoc = vLoc.clone().add(0, j * 0.5, 0);
                            for (double i = 0; i < dist; i += 0.3) {
                                pLoc.add(step);
                                pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, black);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void playCrossSweep(Location center, Vector direction) {
        World world = center.getWorld();
        if (world == null) return;

        Vector side;
        if (Math.abs(direction.getY()) > 0.99) {
            side = new Vector(1, 0, 0);
        } else {
            side = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        }

        Vector up = side.clone().crossProduct(direction).normalize();
        Vector diagonalOne = up.clone().add(side).normalize();
        Vector diagonalTwo = up.clone().subtract(side).normalize();

        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.0f);
        double size = 1.6;

        for (double i = -size; i <= size; i += 0.2) {
            double bulge = Math.cos((i / size) * (Math.PI / 2)) * 2.0;
            Location pointOne = center.clone().add(diagonalOne.clone().multiply(i)).add(direction.clone().multiply(bulge));
            Location pointTwo = center.clone().add(diagonalTwo.clone().multiply(i)).add(direction.clone().multiply(bulge));

            world.spawnParticle(Particle.DUST, pointOne, 2, 0.05, 0.05, 0.05, 0.0, blackDust);
            world.spawnParticle(Particle.DUST, pointTwo, 2, 0.05, 0.05, 0.05, 0.0, blackDust);
        }
    }

    private void spawnBlackDust(Location loc) {
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.5f);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 60, 0.5, 1.0, 0.5, blackDust);
    }

    private void startCooldownBar(Player p, int seconds, String title, Map<UUID, BossBar> barMap, Map<UUID, Long> cdMap) {
        BossBar bar = TextUtils.bossBar(yellowTitle(title), BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(p);
        barMap.put(p.getUniqueId(), bar);

        new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / (seconds * 20.0);
            @Override
            public void run() {
                if (!p.isOnline() || !barMap.containsKey(p.getUniqueId()) || barMap.get(p.getUniqueId()) != bar) {
                    bar.removeAll();
                    this.cancel();
                    return;
                }
                progress -= step;
                if (progress <= 0) {
                    bar.removeAll();
                    barMap.remove(p.getUniqueId());
                    this.cancel();
                } else {
                    bar.setProgress(Math.max(0, progress));
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private String yellowTitle(String title) {
        return ChatColor.YELLOW + ChatColor.stripColor(title);
    }

    public void resetCooldowns(Player player) {
        UUID uuid = player.getUniqueId();
        leapCooldowns.remove(uuid);
        daggersCooldowns.remove(uuid);
        backstabCooldowns.remove(uuid);
        activeLeaps.remove(uuid);
        currentLeapLocs.remove(uuid);
        leapStartTimes.remove(uuid);

        BossBar leapBar = leapCooldownBars.remove(uuid);
        BossBar daggersBar = daggersCooldownBars.remove(uuid);
        if (leapBar != null) leapBar.removeAll();
        if (daggersBar != null) daggersBar.removeAll();
    }

    public void clearAllBars() {
        leapCooldownBars.values().forEach(BossBar::removeAll);
        daggersCooldownBars.values().forEach(BossBar::removeAll);
        leapCooldownBars.clear();
        daggersCooldownBars.clear();
        leapCooldowns.clear();
        daggersCooldowns.clear();
        backstabCooldowns.clear();
        activeLeaps.clear();
        currentLeapLocs.clear();
        leapStartTimes.clear();
    }
}
