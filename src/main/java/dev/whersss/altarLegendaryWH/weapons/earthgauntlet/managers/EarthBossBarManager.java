package dev.whersss.altarLegendaryWH.weapons.earthgauntlet.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EarthBossBarManager {

    private final AltarLegendaryWH plugin;

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();
    private final Map<UUID, BossBar> debuffBars = new HashMap<>();

    public EarthBossBarManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player player, String ability) {
        if (!cooldowns.containsKey(player.getUniqueId())) return false;
        if (!cooldowns.get(player.getUniqueId()).containsKey(ability)) return false;
        long expireTime = cooldowns.get(player.getUniqueId()).get(ability);
        return System.currentTimeMillis() < expireTime;
    }

    public void setCooldown(Player player, String ability, String name, int seconds) {
        long expireTime = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).put(ability, expireTime);

        BossBar bar = TextUtils.bossBar(
                ChatColor.YELLOW + ChatColor.stripColor(resolveCooldownTitle(ability, name)),
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bar.addPlayer(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    bar.removeAll();
                    cancel();
                    return;
                }

                long timeLeft = expireTime - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    bar.removeAll();

                    Map<String, Long> pCooldowns = cooldowns.get(player.getUniqueId());
                    if (pCooldowns != null) {
                        pCooldowns.remove(ability);
                        if (pCooldowns.isEmpty()) {
                            cooldowns.remove(player.getUniqueId());
                        }
                    }

                    cancel();
                    return;
                }

                double progress = (double) timeLeft / (seconds * 1000L);
                bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void setActiveBar(Player player, String id, String title, int seconds) {
        removeActiveBar(player, id);

        BossBar bar = TextUtils.bossBar(title, BarColor.GREEN, BarStyle.SOLID);
        bar.addPlayer(player);

        activeBars.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).put(id, bar);
        long expireTime = System.currentTimeMillis() + (seconds * 1000L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeBars.getOrDefault(player.getUniqueId(), new HashMap<>()).containsKey(id)) {
                    bar.removeAll();
                    cancel();
                    return;
                }

                long timeLeft = expireTime - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    removeActiveBar(player, id);
                    cancel();
                    return;
                }

                double progress = (double) timeLeft / (seconds * 1000L);
                bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void removeActiveBar(Player player, String id) {
        if (activeBars.containsKey(player.getUniqueId())) {
            BossBar bar = activeBars.get(player.getUniqueId()).remove(id);
            if (bar != null) {
                bar.removeAll();
            }
        }
    }

    public void setDebuffBar(Player player, String title, int seconds) {
        removeDebuffBar(player);

        BossBar bar = TextUtils.bossBar(title, BarColor.RED, BarStyle.SOLID);
        bar.addPlayer(player);
        debuffBars.put(player.getUniqueId(), bar);

        long expireTime = System.currentTimeMillis() + (seconds * 1000L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !debuffBars.containsKey(player.getUniqueId()) || !debuffBars.get(player.getUniqueId()).equals(bar)) {
                    bar.removeAll();
                    cancel();
                    return;
                }

                long timeLeft = expireTime - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    removeDebuffBar(player);
                    cancel();
                    return;
                }

                double progress = (double) timeLeft / (seconds * 1000L);
                bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void removeDebuffBar(Player player) {
        BossBar bar = debuffBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        Map<String, BossBar> bars = activeBars.remove(uuid);
        if (bars != null) {
            for (BossBar bar : bars.values()) {
                bar.removeAll();
            }
        }
        removeDebuffBar(player);
    }

    private String resolveCooldownTitle(String ability, String fallback) {
        return switch (ability) {
            case "MeteorStrike" -> plugin.tr("§e§lМетеоритный удар", "§e§lᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ");
            case "Mudslide" -> plugin.tr("§e§lОползень", "§e§lᴍᴜᴅsʟɪᴅᴇ");
            default -> fallback;
        };
    }
}
