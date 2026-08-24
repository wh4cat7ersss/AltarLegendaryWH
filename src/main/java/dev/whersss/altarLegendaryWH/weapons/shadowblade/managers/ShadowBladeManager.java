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
        border.setCenter(p.getLocation()); border.setSize(10000); border.setWarningDistance(10000);
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
            final Location currentTip = p.getEyeLocation().clone();
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

                double distTraveled = ticks * 0.6;
                Vector dir = p.getLocation().getDirection().normalize();

                RayTraceResult rt = p.getWorld().rayTraceBlocks(p.getEyeLocation(), dir, distTraveled, FluidCollisionMode.NEVER, true);

                if (rt != null && rt.getHitBlock() != null) {
                    actualTargetLoc = rt.getHitPosition().toLocation(p.getWorld());
                    actualTargetLoc.subtract(dir.clone().multiply(0.3));
                } else {
                    actualTargetLoc = p.getEyeLocation().add(dir.clone().multiply(distTraveled));
                }

                Vector tipDiff = actualTargetLoc.toVector().subtract(currentTip.toVector());
                currentTip.add(tipDiff.multiply(0.25));
                currentLeapLocs.put(p.getUniqueId(), currentTip);

                double maxTrail = Math.min(distTraveled, 1.0);

                for (double d = 0; d <= maxTrail; d += 0.1) {
                    Location partLoc = currentTip.clone().subtract(dir.clone().multiply(d));

                    float size;
                    double spread;
                    int count;

                    if (d <= 0.15) {
                        size = 2.2f;
                        spread = 0.1;
                        count = 12;
                    } else if (d <= 0.5) {
                        size = 1.8f;
                        spread = 0.2;
                        count = 3;
                    } else {
                        size = 1.2f;
                        spread = 0.32;
                        count = 1;
                    }

                    Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), size);
                    partLoc.getWorld().spawnParticle(Particle.DUST, partLoc, count, spread, spread, spread, 0.0, blackDust);

                    if (d <= 0.15 && Math.random() < 0.6) {
                        partLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, partLoc, 2, 0.07, 0.07, 0.07, 0.0);
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

        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.2f);
        p.swingMainHand();

        Vector dir = p.getLocation().getDirection().normalize();
        playCrossSweep(p.getEyeLocation().add(dir.clone().multiply(1.2)), dir);

        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Location center = p.getEyeLocation();

        Vector[] dirs = {
                dir.clone(),
                dir.clone().add(right.clone().multiply(-0.05)).normalize(),
                dir.clone().add(right.clone().multiply(0.05)).normalize()
        };

        boolean[] soundPlayed = new boolean[]{false};

        for (Vector d : dirs) {
            ItemDisplay dagger = p.getWorld().spawn(center, ItemDisplay.class, ent -> {
                ent.setItemStack(bladeItem);
                ent.setInterpolationDuration(1);
                ent.setTeleportDuration(1);
                Transformation t = ent.getTransformation();
                t.getLeftRotation().set(new Quaternionf().rotateX((float) Math.toRadians(90)));
                t.getScale().set(0.6f, 0.6f, 0.6f);
                ent.setTransformation(t);
            });

            new BukkitRunnable() {
                final Location curr = center.clone();
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks > 40 || !dagger.isValid()) {
                        dagger.remove();
                        this.cancel();
                        return;
                    }

                    curr.add(d.clone().multiply(2.0));
                    dagger.teleport(curr);

                    curr.getWorld().spawnParticle(Particle.DUST, curr, 1, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.BLACK, 0.6f));
                    if (ticks % 2 == 0) {
                        curr.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, curr, 1, 0.05, 0.05, 0.05, 0.02);
                    }

                    if (curr.getBlock().getType().isSolid()) {
                        dagger.remove();
                        this.cancel();
                        return;
                    }

                    for (Entity e : curr.getWorld().getNearbyEntities(curr, 1.0, 1.0, 1.0)) {
                        if (e instanceof LivingEntity victim && !victim.equals(p)) {

                            if (victim instanceof Player targetPlayer) {
                                if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                                    continue;
                                }
                            }

                            double dmg = plugin.getConfig().getDouble("shadow-blade.daggers.damage", 6.0);
                            plugin.getCleanDamageManager().apply(victim, p, dmg);

                            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false));

                            if (!soundPlayed[0]) {
                                p.getWorld().playSound(victim.getLocation(), Sound.BLOCK_SPONGE_ABSORB, 1.0f, 0.8f);
                                p.getWorld().playSound(victim.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3.0f, 0.7f);

                                Particle.DustOptions massiveDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 3.0f);
                                victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 150, 0.8, 1.2, 0.8, massiveDust);

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

        if (Math.random() > plugin.getConfig().getDouble("shadow-blade.passive.backstab-chance", 0.30)) return;

        backstabCooldowns.put(attacker.getUniqueId(), System.currentTimeMillis() + 8000L);
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
                if (!p.isOnline()) {
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
}