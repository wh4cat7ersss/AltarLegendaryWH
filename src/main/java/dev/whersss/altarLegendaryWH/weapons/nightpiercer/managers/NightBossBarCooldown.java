package dev.whersss.altarLegendaryWH.weapons.nightpiercer.managers;

import dev.whersss.altarLegendaryWH.utils.TextUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NightBossBarCooldown extends BukkitRunnable {
    private static final Map<UUID, BossBar> activeBars = new HashMap<>();
    private final BossBar bar;
    private final UUID uuid;
    private final int total;
    private int current;

    public NightBossBarCooldown(JavaPlugin plugin, Player p, String title, int sec) {
        this.uuid = p.getUniqueId();
        this.total = sec * 20;
        this.current = total;
        String translated = ChatColor.translateAlternateColorCodes('&', title == null ? "" : title);
        this.bar = TextUtils.bossBar(ChatColor.YELLOW + ChatColor.stripColor(translated), BarColor.YELLOW, BarStyle.SOLID);
        this.bar.addPlayer(p);
        BossBar oldBar = activeBars.put(uuid, this.bar);
        if (oldBar != null) {
            oldBar.removeAll();
        }
        this.runTaskTimer(plugin, 0, 1);
    }

    @Override
    public void run() {
        if (current <= 0) {
            bar.removeAll();
            activeBars.remove(uuid, bar);
            this.cancel();
            return;
        }
        bar.setProgress((double) current / total);
        current--;
    }

    public void start() {}

    public static void resetPlayer(Player player) {
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }
}
