package dev.whersss.altarLegendaryWH.weapons.pureblade.managers;

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

public class PureBossBarManager {
    private final AltarLegendaryWH plugin;
    private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();

    public PureBossBarManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player player, String ability) {
        return activeBars.containsKey(player.getUniqueId()) && activeBars.get(player.getUniqueId()).containsKey(ability);
    }

    public void setCooldown(Player player, String ability, String title, int seconds) {
        UUID uuid = player.getUniqueId();
        activeBars.putIfAbsent(uuid, new HashMap<>());

        BossBar bossBar = TextUtils.bossBar(
                ChatColor.YELLOW + ChatColor.stripColor(title),
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bossBar.addPlayer(player);
        activeBars.get(uuid).put(ability, bossBar);

        new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / (seconds * 20.0);

            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeBarSafely();
                    bossBar.removeAll();
                    cancel();
                    return;
                }

                progress -= step;
                if (progress <= 0) {
                    bossBar.removeAll();
                    removeBarSafely();
                    cancel();
                } else {
                    bossBar.setProgress(Math.max(0.0, progress));
                }
            }

            private void removeBarSafely() {
                Map<String, BossBar> playerBars = activeBars.get(uuid);
                if (playerBars != null) {
                    playerBars.remove(ability);
                    if (playerBars.isEmpty()) {
                        activeBars.remove(uuid);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void clearAll() {
        for (Map<String, BossBar> bars : activeBars.values()) {
            for (BossBar bar : bars.values()) {
                bar.removeAll();
            }
        }
        activeBars.clear();
    }

    public void resetPlayer(Player player) {
        Map<String, BossBar> bars = activeBars.remove(player.getUniqueId());
        if (bars == null) return;
        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }
    }
}