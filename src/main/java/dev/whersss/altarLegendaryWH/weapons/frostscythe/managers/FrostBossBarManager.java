package dev.whersss.altarLegendaryWH.weapons.frostscythe.managers;

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

@SuppressWarnings("SpellCheckingInspection")
public class FrostBossBarManager {
    private final AltarLegendaryWH plugin;
    private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldownEnds = new HashMap<>();

    public FrostBossBarManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player player, String ability) {
        if (!cooldownEnds.containsKey(player.getUniqueId())) return false;
        Long end = cooldownEnds.get(player.getUniqueId()).get(ability);
        return end != null && System.currentTimeMillis() < end;
    }

    public void setCooldown(Player player, String ability, String title, int seconds) {
        UUID uuid = player.getUniqueId();
        cooldownEnds.computeIfAbsent(uuid, key -> new HashMap<>()).put(ability, System.currentTimeMillis() + (seconds * 1000L));

        BossBar bar = TextUtils.bossBar(
                ChatColor.YELLOW + ChatColor.stripColor(resolveTitle(ability, title)),
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bar.addPlayer(player);
        activeBars.computeIfAbsent(uuid, key -> new HashMap<>()).put(ability, bar);

        new BukkitRunnable() {
            int ticksLeft = seconds * 20;
            final int totalTicks = seconds * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticksLeft <= 0) {
                    bar.removePlayer(player);
                    Map<String, BossBar> playerBars = activeBars.get(uuid);
                    if (playerBars != null) {
                        playerBars.remove(ability);
                        if (playerBars.isEmpty()) {
                            activeBars.remove(uuid);
                        }
                    }
                    cancel();
                    return;
                }

                bar.setProgress((double) ticksLeft / totalTicks);
                ticksLeft--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        cooldownEnds.remove(uuid);
        Map<String, BossBar> bars = activeBars.remove(uuid);
        if (bars != null) {
            for (BossBar bar : bars.values()) {
                bar.removeAll();
            }
        }
    }

    private String resolveTitle(String ability, String fallback) {
        return switch (ability) {
            case "ScytheThrow" -> plugin.tr("§b§lБросок косы", "§b§lsᴄʏᴛʜᴇ ᴛʜʀᴏᴡ");
            case "CommandOfIce" -> plugin.tr("§b§lПовеление льда", "§b§lᴄᴏᴍᴍᴀɴᴅ ᴏғ ɪᴄᴇ");
            default -> fallback;
        };
    }
}