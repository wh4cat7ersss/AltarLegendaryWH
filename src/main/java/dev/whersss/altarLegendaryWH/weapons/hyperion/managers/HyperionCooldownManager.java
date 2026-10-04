package dev.whersss.altarLegendaryWH.weapons.hyperion.managers;

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

public class HyperionCooldownManager {
    private static final Map<UUID, Long> scorchingCooldowns = new HashMap<>();
    private static final Map<UUID, Long> holyLanceCooldowns = new HashMap<>();

    private static final Map<UUID, BossBar> scorchingBars = new HashMap<>();
    private static final Map<UUID, BossBar> lanceBars = new HashMap<>();

    public static boolean isOnScorchingCooldown(Player p) {
        return scorchingCooldowns.containsKey(p.getUniqueId()) && scorchingCooldowns.get(p.getUniqueId()) > System.currentTimeMillis();
    }

    public static void setScorchingCooldown(Player p) {
        long timeMs = 30 * 1000L;
        scorchingCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + timeMs);
        startBossBar(p, "scorching", AltarLegendaryWH.getInstance().tr("§eОбжигающий клинок", "§e sᴄᴏʀᴄʜɪɴɢ ʙʟᴀᴅᴇ"), timeMs);
    }

    public static boolean isOnHolyLanceCooldown(Player p) {
        return holyLanceCooldowns.containsKey(p.getUniqueId()) && holyLanceCooldowns.get(p.getUniqueId()) > System.currentTimeMillis();
    }

    public static void setHolyLanceCooldown(Player p) {
        long timeMs = 60 * 1000L;
        holyLanceCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + timeMs);
        startBossBar(p, "lance", AltarLegendaryWH.getInstance().tr("§eСвятое Копьё", "§e ʜᴏʟʏ ʟᴀɴᴄᴇ"), timeMs);
    }

    private static void startBossBar(Player p, String type, String title, long maxTimeMs) {
        UUID id = p.getUniqueId();
        Map<UUID, BossBar> activeBars = type.equals("scorching") ? scorchingBars : lanceBars;

        if (activeBars.containsKey(id)) {
            activeBars.get(id).removeAll();
            activeBars.remove(id);
        }

        BossBar bar = TextUtils.bossBar(ChatColor.YELLOW + ChatColor.stripColor(title), BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(p);
        activeBars.put(id, bar);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + maxTimeMs;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !activeBars.containsKey(id) || activeBars.get(id) != bar) {
                    bar.removeAll();
                    activeBars.remove(id, bar);
                    this.cancel();
                    return;
                }

                long now = System.currentTimeMillis();
                if (now >= endTime) {
                    bar.removeAll();
                    activeBars.remove(id);
                    this.cancel();
                    return;
                }

                double progress = (double) (endTime - now) / maxTimeMs;
                bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            }
        }.runTaskTimer(AltarLegendaryWH.getInstance(), 0L, 2L);
    }

    public static void clearAllBars() {
        for (BossBar bar : scorchingBars.values()) bar.removeAll();
        for (BossBar bar : lanceBars.values()) bar.removeAll();
        scorchingBars.clear();
        lanceBars.clear();
    }

    public static void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        scorchingCooldowns.remove(uuid);
        holyLanceCooldowns.remove(uuid);

        BossBar scorchingBar = scorchingBars.remove(uuid);
        BossBar lanceBar = lanceBars.remove(uuid);
        if (scorchingBar != null) scorchingBar.removeAll();
        if (lanceBar != null) lanceBar.removeAll();
    }
}
