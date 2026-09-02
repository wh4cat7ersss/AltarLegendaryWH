package dev.whersss.altarLegendaryWH.weapons.boneblade.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoneCooldownManager {
    private final AltarLegendaryWH plugin;
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, Long> cageCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> dashBars = new HashMap<>();
    private final Map<UUID, BossBar> cageBars = new HashMap<>();

    public BoneCooldownManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    public boolean isOnDashCooldown(Player player) {
        return dashCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public boolean isOnCageCooldown(Player player) {
        return cageCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public void setDashCooldown(Player player, int seconds) {
        dashCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
        if (!dashBars.containsKey(player.getUniqueId())) {
            BossBar bar = TextUtils.bossBar("§e" + plugin.tr("Скелетный прыжок", "Sᴋᴇʟᴇᴛᴏɴ ʟᴇᴀᴘ"), BarColor.YELLOW, BarStyle.SOLID);
            bar.addPlayer(player);
            dashBars.put(player.getUniqueId(), bar);
        }
    }

    public void setCageCooldown(Player player, int seconds) {
        cageCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
        if (!cageBars.containsKey(player.getUniqueId())) {
            BossBar bar = TextUtils.bossBar("§e" + plugin.tr("Костяная клетка", "Bᴏɴᴇ ᴄᴀɢᴇ"), BarColor.YELLOW, BarStyle.SOLID);
            bar.addPlayer(player);
            cageBars.put(player.getUniqueId(), bar);
        }
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                dashBars.entrySet().removeIf(entry -> {
                    UUID uuid = entry.getKey();
                    BossBar bar = entry.getValue();
                    long expire = dashCooldowns.getOrDefault(uuid, 0L);
                    if (now >= expire) {
                        bar.removeAll();
                        return true;
                    }
                    double total = plugin.getWeaponsConfig().getInt("bone-blade.dash.cooldown", 10) * 1000.0;
                    bar.setProgress(Math.max(0.0, Math.min(1.0, (expire - now) / total)));
                    return false;
                });

                cageBars.entrySet().removeIf(entry -> {
                    UUID uuid = entry.getKey();
                    BossBar bar = entry.getValue();
                    long expire = cageCooldowns.getOrDefault(uuid, 0L);
                    if (now >= expire) {
                        bar.removeAll();
                        return true;
                    }
                    double total = plugin.getWeaponsConfig().getInt("bone-blade.cage.cooldown", 35) * 1000.0;
                    bar.setProgress(Math.max(0.0, Math.min(1.0, (expire - now) / total)));
                    return false;
                });
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void removeAllBars() {
        dashBars.values().forEach(BossBar::removeAll);
        cageBars.values().forEach(BossBar::removeAll);
        dashBars.clear();
        cageBars.clear();
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        dashCooldowns.remove(uuid);
        cageCooldowns.remove(uuid);
        BossBar dashBar = dashBars.remove(uuid);
        BossBar cageBar = cageBars.remove(uuid);
        if (dashBar != null) dashBar.removeAll();
        if (cageBar != null) cageBar.removeAll();
    }
}

