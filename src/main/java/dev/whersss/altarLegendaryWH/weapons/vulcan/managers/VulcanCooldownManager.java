package dev.whersss.altarLegendaryWH.weapons.vulcan.managers;

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

public class VulcanCooldownManager {

    private static final Map<UUID, BossBar> activeBars = new HashMap<>();

    public static boolean isOnCooldown(Player player) {
        return activeBars.containsKey(player.getUniqueId());
    }

    public static void startCooldown(Player player, int seconds) {
        UUID uuid = player.getUniqueId();
        String title = AltarLegendaryWH.getInstance().tr("§6§lГнев вулкана", "§6§lᴠᴜʟᴄᴀɴ ᴡʀᴀᴛʜ");

        BossBar bossBar = TextUtils.bossBar(
                ChatColor.YELLOW + ChatColor.stripColor(title),
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bossBar.addPlayer(player);
        activeBars.put(uuid, bossBar);

        new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / (seconds * 20.0);

            @Override
            public void run() {
                if (!player.isOnline()) {
                    bossBar.removeAll();
                    activeBars.remove(uuid);
                    cancel();
                    return;
                }

                progress -= step;
                if (progress <= 0) {
                    bossBar.removeAll();
                    activeBars.remove(uuid);
                    cancel();
                } else {
                    bossBar.setProgress(Math.max(0.0, progress));
                }
            }
        }.runTaskTimer(AltarLegendaryWH.getInstance(), 0L, 1L);
    }

    public static void clearAllBars() {
        for (BossBar bossBar : activeBars.values()) {
            bossBar.removeAll();
        }
        activeBars.clear();
    }

    public static void resetPlayer(Player player) {
        BossBar bossBar = activeBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
}
