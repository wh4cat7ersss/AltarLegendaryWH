package dev.whersss.altarLegendaryWH.weapons.witherblade.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WitherManager {
    private final AltarLegendaryWH plugin;

    private final Map<UUID, Integer> dashCharges = new HashMap<>();
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> dashBars = new HashMap<>();

    private final Map<UUID, Long> auraCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> auraBars = new HashMap<>();

    private final Map<UUID, Integer> attackCharges = new HashMap<>();

    public WitherManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        startPassiveTask();
    }

    public int getDashCharges(Player player) {
        return dashCharges.getOrDefault(player.getUniqueId(), 3);
    }

    public boolean isOnAuraCooldown(Player player) {
        return auraCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public void useDashCharge(Player player) {
        int charges = getDashCharges(player);
        if (charges <= 0) return;

        charges--;
        dashCharges.put(player.getUniqueId(), charges);

        if (charges == 0) {
            int cooldown = plugin.getWeaponsConfig().getInt("wither-blade.dash.cooldown", 30);
            dashCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));

            BossBar bar = TextUtils.bossBar(yellowTitle(plugin.tr("иссушительный прыжок", "ᴡɪᴛʜᴇʀ ʟᴇᴀᴘ")), BarColor.YELLOW, BarStyle.SOLID);
            bar.addPlayer(player);
            dashBars.put(player.getUniqueId(), bar);

            new BukkitRunnable() {
                double progress = 1.0;
                final double step = 1.0 / (cooldown * 20.0);

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        bar.removeAll();
                        cancel();
                        return;
                    }

                    progress -= step;
                    if (progress <= 0) {
                        bar.removeAll();
                        dashBars.remove(player.getUniqueId());
                        dashCharges.put(player.getUniqueId(), 3);
                        cancel();
                    } else {
                        bar.setProgress(Math.max(0.0, progress));
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    public void setAuraCooldown(Player player) {
        int cooldown = plugin.getWeaponsConfig().getInt("wither-blade.aura.cooldown", 50);
        auraCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));

        BossBar bar = TextUtils.bossBar(yellowTitle(plugin.tr("иссушительное высвобождение", "ᴡɪᴛʜᴇʀ ʀᴇʟᴇᴀsᴇ")), BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(player);
        auraBars.put(player.getUniqueId(), bar);

        new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / (cooldown * 20.0);

            @Override
            public void run() {
                if (!player.isOnline()) {
                    bar.removeAll();
                    cancel();
                    return;
                }

                progress -= step;
                if (progress <= 0) {
                    bar.removeAll();
                    auraBars.remove(player.getUniqueId());
                    cancel();
                } else {
                    bar.setProgress(Math.max(0.0, progress));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public int getAttackCharge(Player player) {
        return attackCharges.getOrDefault(player.getUniqueId(), 0);
    }

    public void addAttackCharge(Player player) {
        int current = getAttackCharge(player);
        if (current < 30) {
            attackCharges.put(player.getUniqueId(), current + 1);
        }
    }

    public void fillMaxCharge(Player player) {
        attackCharges.put(player.getUniqueId(), 30);
    }

    public void resetAttackCharge(Player player) {
        attackCharges.put(player.getUniqueId(), 0);
    }

    private void startPassiveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item != null
                            && item.getType() == Material.NETHERITE_SWORD
                            && item.hasItemMeta()
                            && item.getItemMeta().hasCustomModelData()
                            && item.getItemMeta().getCustomModelData() == 9) {

                        int charge = getAttackCharge(player);
                        StringBuilder bar = new StringBuilder("§8☠ §7- ");
                        for (int i = 0; i < 30; i++) {
                            bar.append(i < charge ? "&f|" : "&8|");
                        }
                        player.sendActionBar(TextUtils.legacy(bar.toString()));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private String yellowTitle(String title) {
        return ChatColor.YELLOW + ChatColor.stripColor(title);
    }

    public void resetCooldowns(Player player) {
        UUID uuid = player.getUniqueId();
        dashCharges.remove(uuid);
        dashCooldowns.remove(uuid);
        auraCooldowns.remove(uuid);
        attackCharges.remove(uuid);

        BossBar dashBar = dashBars.remove(uuid);
        BossBar auraBar = auraBars.remove(uuid);
        if (dashBar != null) dashBar.removeAll();
        if (auraBar != null) auraBar.removeAll();
    }
}
