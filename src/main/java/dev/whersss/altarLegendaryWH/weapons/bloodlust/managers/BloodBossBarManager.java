package dev.whersss.altarLegendaryWH.weapons.bloodlust.managers;

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

public class BloodBossBarManager {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();
    private final Map<UUID, Map<String, BossBar>> debuffBars = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldownEnds = new HashMap<>();

    public BloodBossBarManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player p, String ability) {
        if (!cooldownEnds.containsKey(p.getUniqueId())) return false;
        Long end = cooldownEnds.get(p.getUniqueId()).get(ability);
        return end != null && System.currentTimeMillis() < end;
    }

    public void setCooldown(Player p, String ability, String title, int seconds) {
        UUID uuid = p.getUniqueId();
        cooldownEnds.computeIfAbsent(uuid, k -> new HashMap<>()).put(ability, System.currentTimeMillis() + (seconds * 1000L));

        removeBar(activeBars, uuid, ability);

        BossBar bar = TextUtils.bossBar(yellowTitle(resolveCooldownTitle(ability, title)), BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(p);
        activeBars.computeIfAbsent(uuid, k -> new HashMap<>()).put(ability, bar);

        new BukkitRunnable() {
            int ticksLeft = seconds * 20;
            final int totalTicks = seconds * 20;

            @Override
            public void run() {
                if (!p.isOnline() || ticksLeft <= 0) {
                    removeBar(activeBars, uuid, ability);
                    this.cancel();
                    return;
                }
                bar.setProgress((double) ticksLeft / totalTicks);
                ticksLeft--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void setDebuffBar(Player p, String id, String title, int totalHits) {
        UUID uuid = p.getUniqueId();
        removeBar(debuffBars, uuid, id);

        BossBar bar = TextUtils.bossBar(resolveDebuffTitle(id, title), BarColor.RED, BarStyle.SEGMENTED_10);
        bar.addPlayer(p);
        bar.setProgress(1.0);
        debuffBars.computeIfAbsent(uuid, k -> new HashMap<>()).put(id, bar);
    }

    public void updateProgress(Player p, String id, double progress) {
        Map<String, BossBar> playerBars = debuffBars.get(p.getUniqueId());
        if (playerBars == null) return;

        BossBar bar = playerBars.get(id);
        if (bar == null) return;

        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    public void removeDebuffBar(Player p, String id) {
        removeBar(debuffBars, p.getUniqueId(), id);
    }

    public void clearAll() {
        clearMap(activeBars);
        clearMap(debuffBars);
        cooldownEnds.clear();
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        cooldownEnds.remove(uuid);
        clearBars(activeBars.remove(uuid));
        clearBars(debuffBars.remove(uuid));
    }

    private void clearMap(Map<UUID, Map<String, BossBar>> source) {
        for (Map<String, BossBar> map : source.values()) {
            for (BossBar bar : map.values()) {
                bar.removeAll();
            }
        }
        source.clear();
    }

    private void clearBars(Map<String, BossBar> bars) {
        if (bars == null) return;
        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }
    }

    private void removeBar(Map<UUID, Map<String, BossBar>> source, UUID uuid, String id) {
        Map<String, BossBar> playerBars = source.get(uuid);
        if (playerBars == null) return;

        BossBar old = playerBars.remove(id);
        if (old != null) {
            old.removeAll();
        }

        if (playerBars.isEmpty()) {
            source.remove(uuid);
        }
    }

    private String yellowTitle(String title) {
        String translated = ChatColor.translateAlternateColorCodes('&', title == null ? "" : title);
        return ChatColor.YELLOW + ChatColor.stripColor(translated);
    }

    private String resolveCooldownTitle(String ability, String fallback) {
        return switch (ability) {
            case "Infection" -> plugin.tr("§4§lинɸᴇᴋция", "§4§lɪɴꜰᴇᴄᴛɪᴏɴ");
            case "BloodTrail" -> plugin.tr("§4§lᴋроʙᴀʙый ᴄлᴇд", "§4§lʙʟᴏᴏᴅ ᴛʀᴀɪʟ");
            case "BloodHook" -> plugin.tr("§4§lᴋроʙᴀʙый ᴋрюᴋ", "§4§lʙʟᴏᴏᴅ ʜᴏᴏᴋ");
            default -> fallback;
        };
    }

    private String resolveDebuffTitle(String id, String fallback) {
        if ("BloodInfection".equals(id)) {
            return plugin.tr("§4§l!ᴋроʙоᴛᴇчᴇниᴇ!", "§4§l!ʙʟᴇᴇᴅɪɴɢ!");
        }
        return fallback;
    }
}
