package dev.whersss.altarLegendaryWH.weapons.windweaver.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WindWeaverListener implements Listener {

    private static final int MAX_CHARGE = 100;

    private final AltarLegendaryWH plugin;
    private final Map<UUID, WindBurstSession> burstSessions = new HashMap<>();
    private final Map<UUID, BossBar> leapCooldownBars = new HashMap<>();
    private final Map<UUID, BossBar> burstCooldownBars = new HashMap<>();
    private final Map<UUID, Long> leapCooldowns = new HashMap<>();
    private final Map<UUID, Long> burstCooldowns = new HashMap<>();

    private static class WindBurstSession {
        int percent = 0;
        int ticks = 0;
        BukkitRunnable task;
    }

    public WindWeaverListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    public static boolean isWindWeaver(ItemStack item) {
        return WeaponFactory.isAltarWeapon(item, "windweaver");
    }

    public boolean onLeapCooldown(Player player) {
        return leapCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public boolean onBurstCooldown(Player player) {
        return burstCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        leapCooldowns.remove(uuid);
        burstCooldowns.remove(uuid);

        BossBar leapBar = leapCooldownBars.remove(uuid);
        if (leapBar != null) leapBar.removeAll();

        BossBar burstBar = burstCooldownBars.remove(uuid);
        if (burstBar != null) burstBar.removeAll();

        WindBurstSession session = burstSessions.remove(uuid);
        if (session != null && session.task != null) {
            session.task.cancel();
        }
    }

    public void removeAllBars() {
        leapCooldownBars.values().forEach(BossBar::removeAll);
        burstCooldownBars.values().forEach(BossBar::removeAll);
        leapCooldownBars.clear();
        burstCooldownBars.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        resetPlayer(event.getPlayer());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isWindWeaver(item)) return;

        event.setCancelled(true);

        UUID id = player.getUniqueId();
        WindBurstSession session = burstSessions.get(id);

        if (player.isSneaking()) {
            if (onBurstCooldown(player)) return;
            if (session == null) {
                startBurstCharge(player);
            }
            return;
        }

        castWindLeap(player);
    }

    private void castWindLeap(Player player) {
        if (onLeapCooldown(player)) return;

        Vector dir = player.getLocation().getDirection().normalize();
        int cooldown = plugin.getWeaponsConfig().getInt("windweaver.wind-leap.cooldown", 10);
        double velocity = plugin.getWeaponsConfig().getDouble("windweaver.wind-leap.velocity", 1.25);

        player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.8f);
        player.setVelocity(dir.multiply(velocity));
        player.setFallDistance(0);

        int duration = plugin.getWeaponsConfig().getInt("windweaver.wind-leap.speed-duration", 3) * 20;
        int amplifier = plugin.getWeaponsConfig().getInt("windweaver.wind-leap.speed-amplifier", 4) - 1;

        PotionEffect currentSpeed = player.getPotionEffect(PotionEffectType.SPEED);
        if (currentSpeed == null || currentSpeed.getAmplifier() < amplifier) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, false, false));
        }

        Location start = player.getLocation().add(0, 1.0, 0);
        for (int i = 0; i < 5; i++) {
            Location gust = start.clone().add((Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 0.5);
            gust.getWorld().spawnParticle(Particle.GUST, gust, 1, 0.02, 0.02, 0.02, 0.0);
        }

        leapCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        startCooldownBar(player, cooldown, plugin.tr("§eВетровой Прыжок", "§eᴡɪɴᴅ ʟᴇᴀᴘ"), leapCooldownBars, leapCooldowns);

        new BukkitRunnable() {
            int elapsedTicks = 0;

            @Override
            public void run() {
                elapsedTicks++;
                if (!player.isOnline() || player.isDead() || elapsedTicks > 200) {
                    cancel();
                    return;
                }

                if (elapsedTicks > 5 && ((Entity) player).isOnGround()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 0.95, 0);
                Vector back = player.getLocation().getDirection().normalize().multiply(-0.3);
                Location trail = loc.clone().add(back);

                trail.getWorld().spawnParticle(Particle.CLOUD, trail, 2, 0.15, 0.12, 0.15, 0.01);
                trail.getWorld().spawnParticle(Particle.SMALL_GUST, trail, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startBurstCharge(Player player) {
        if (onBurstCooldown(player)) return;

        UUID id = player.getUniqueId();
        WindBurstSession session = new WindBurstSession();
        session.percent = 0;
        session.ticks = 0;
        burstSessions.put(id, session);

        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_INHALE, 1.0f, 0.8f);

        session.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !isWindWeaver(player.getInventory().getItemInMainHand())) {
                    burstSessions.remove(id);
                    cancel();
                    return;
                }

                if (player.isSneaking()) {
                    session.ticks++;
                    if (session.percent < MAX_CHARGE) {
                        session.percent++;
                        if (session.ticks % 12 == 0) {
                            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_IDLE_GROUND, 1.0f, 1.2f);
                        }
                        spawnChargeParticles(player);
                    }
                    updateBurstActionBar(player, session.percent);
                } else {
                    releaseBurst(player, session);
                }
            }
        };

        session.task.runTaskTimer(plugin, 0L, 1L);
    }

    private void releaseBurst(Player player, WindBurstSession session) {
        UUID id = player.getUniqueId();
        burstSessions.remove(id);
        if (session.task != null) {
            session.task.cancel();
        }

        int percent = Math.max(0, Math.min(MAX_CHARGE, session.percent));
        double powerScale = percent / 100.0;
        int cooldown = plugin.getWeaponsConfig().getInt("windweaver.wind-burst.cooldown", 35);
        double knockbackPower = plugin.getWeaponsConfig().getDouble("windweaver.wind-burst.knockback-power", 3.6);
        double upwardPower = plugin.getWeaponsConfig().getDouble("windweaver.wind-burst.upward-power", 1.2);
        double radius = plugin.getWeaponsConfig().getDouble("windweaver.wind-burst.radius", 25.0);

        burstCooldowns.put(id, System.currentTimeMillis() + (cooldown * 1000L));
        startCooldownBar(player, cooldown, plugin.tr("§eПорыв Ветра", "§eᴡɪɴᴅ ʙᴜʀsᴛ"), burstCooldownBars, burstCooldowns);

        Location center = player.getLocation().add(0, 1.0, 0);
        player.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.8f);

        int cloudCount = Math.max(80, (int) (1200 * powerScale));
        for (int i = 0; i < cloudCount; i++) {
            double phi = Math.random() * Math.PI * 2.0;
            double theta = Math.acos((Math.random() * 2.0) - 1.0);
            double speed = (0.35 + (Math.random() * 0.85)) * (0.8 + (powerScale * 1.8));

            double dx = Math.sin(theta) * Math.cos(phi) * speed;
            double dy = Math.sin(theta) * Math.sin(phi) * speed;
            double dz = Math.cos(theta) * speed;

            center.getWorld().spawnParticle(Particle.CLOUD, center, 0, dx, dy, dz, 1.0);
        }

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || target.equals(player)) continue;
            if (target instanceof Player targetPlayer) {
                if (targetPlayer.getGameMode().name().equalsIgnoreCase("SPECTATOR")) continue;
                if (plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) continue;
            }

            double dist = target.getLocation().distance(center);
            double factor = Math.max(0.0, 1.0 - (dist / radius)) * powerScale;
            if (factor <= 0.0) continue;

            Vector away = target.getLocation().toVector().subtract(center.toVector());
            if (away.lengthSquared() == 0) {
                away = new Vector(0, 1, 0);
            } else {
                away.normalize();
            }

            Vector velocity = away.multiply(knockbackPower * factor);
            velocity.setY(upwardPower * factor + 0.35);
            target.setVelocity(target.getVelocity().add(velocity));
        }

        updateBurstActionBar(player, percent);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendActionBar(TextUtils.legacy(" "));
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    private void spawnChargeParticles(Player player) {
        Location base = player.getLocation().add(0, 1.0, 0);
        for (int i = 0; i < 25; i++) {
            double radius = 1.6 + (Math.random() * 1.0);
            double theta = Math.random() * Math.PI * 2.0;
            double phi = Math.acos((Math.random() * 2.0) - 1.0);

            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);

            Location loc = base.clone().add(x, y, z);
            loc.getWorld().spawnParticle(Particle.SMALL_GUST, loc, 1, 0.0, 0.0, 0.0, 0.0);

            if (Math.random() < 0.08) {
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void updateBurstActionBar(Player player, int percent) {
        int filled = Math.max(0, Math.min(10, percent / 10));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("&a&l|");
            } else {
                bar.append("&c&l|");
            }
        }

        String percentText = percent >= 100 ? "&a&l100%" : (percent < 30 ? "&c" + percent + "%" : "&a" + percent + "%");
        player.sendActionBar(TextUtils.legacy(percentText + " " + bar));
    }

    private void startCooldownBar(Player player, int seconds, String title, Map<UUID, BossBar> barMap, Map<UUID, Long> cooldowns) {
        BossBar bar = TextUtils.bossBar(title, BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(player);
        barMap.put(player.getUniqueId(), bar);

        new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / (seconds * 20.0);

            @Override
            public void run() {
                if (!player.isOnline() || !barMap.containsKey(player.getUniqueId()) || barMap.get(player.getUniqueId()) != bar) {
                    bar.removeAll();
                    cancel();
                    return;
                }

                if (cooldowns.getOrDefault(player.getUniqueId(), 0L) <= System.currentTimeMillis()) {
                    bar.removeAll();
                    barMap.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                progress -= step;
                bar.setProgress(Math.max(0.0, progress));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}