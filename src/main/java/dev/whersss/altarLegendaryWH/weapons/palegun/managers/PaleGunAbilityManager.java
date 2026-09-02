package dev.whersss.altarLegendaryWH.weapons.palegun.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.weapons.palegun.tasks.PaleProjectileTask;
import dev.whersss.altarLegendaryWH.weapons.palegun.tasks.PaleRootsTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PaleGunAbilityManager {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Long> rootsCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> rootsBars = new HashMap<>();
    private final Set<PaleRootsTask> activeRoots = new HashSet<>();
    private final Set<PaleProjectileTask> activeProjectiles = new HashSet<>();

    public PaleGunAbilityManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        startMarkedUpdater();
    }

    public boolean activatePaleRoots(Player player) {
        if (isRootsOnCooldown(player)) {
            return false;
        }

        int durationSeconds = plugin.getWeaponsConfig().getInt("pale-gun.pale-roots.duration-seconds", 18);
        double damage = plugin.getWeaponsConfig().getDouble("pale-gun.pale-roots.damage", 6.0);
        int cooldownSeconds = plugin.getWeaponsConfig().getInt("pale-gun.pale-roots.cooldown", 25);

        PaleRootsTask task = new PaleRootsTask(player, durationSeconds, damage, plugin, this);
        activeRoots.add(task);
        task.runTaskTimer(plugin, 0L, 1L);
        startRootsCooldown(player, cooldownSeconds);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CREAKING_ATTACK, 1.0f, 0.8f);
        return true;
    }

    public boolean isPaleGun(ItemStack item) {
        return item != null
                && item.getType() == Material.CROSSBOW
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(plugin.getPaleGunKey(), PersistentDataType.BYTE);
    }

    public void trackProjectile(PaleProjectileTask task) {
        activeProjectiles.add(task);
    }

    public void untrackProjectile(PaleProjectileTask task) {
        activeProjectiles.remove(task);
    }

    public void untrackRootTask(PaleRootsTask task) {
        activeRoots.remove(task);
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        rootsCooldowns.remove(uuid);

        BossBar bar = rootsBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (isPaleGun(item)) {
                player.setCooldown(item, 0);
            }
        }
    }

    public void clearAll() {
        for (PaleProjectileTask task : new ArrayList<>(activeProjectiles)) {
            task.forceCleanup();
        }
        activeProjectiles.clear();

        for (PaleRootsTask task : new ArrayList<>(activeRoots)) {
            task.forceCleanup();
        }
        activeRoots.clear();

        for (BossBar bar : rootsBars.values()) {
            bar.removeAll();
        }
        rootsBars.clear();
        rootsCooldowns.clear();
    }

    private boolean isRootsOnCooldown(Player player) {
        Long expireAt = rootsCooldowns.get(player.getUniqueId());
        return expireAt != null && System.currentTimeMillis() < expireAt;
    }

    private void startRootsCooldown(Player player, int seconds) {
        UUID uuid = player.getUniqueId();
        long expireAt = System.currentTimeMillis() + (seconds * 1000L);
        rootsCooldowns.put(uuid, expireAt);

        BossBar oldBar = rootsBars.remove(uuid);
        if (oldBar != null) {
            oldBar.removeAll();
        }
        BossBar bar = TextUtils.bossBar(
                plugin.tr("§e§kБледные §r§eКорни", "§e§kᴘᴀʟᴇ §r§eʀᴏᴏᴛs"),
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bar.addPlayer(player);
        rootsBars.put(uuid, bar);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeRootsCooldown(uuid);
                    cancel();
                    return;
                }

                long timeLeft = expireAt - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    removeRootsCooldown(uuid);
                    cancel();
                    return;
                }

                double progress = Math.max(0.0, Math.min(1.0, (double) timeLeft / (seconds * 1000L)));
                bar.setProgress(progress);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void removeRootsCooldown(UUID uuid) {
        rootsCooldowns.remove(uuid);
        BossBar bar = rootsBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }

    private void startMarkedUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (LivingEntity entity : world.getLivingEntities()) {
                        if (entity.hasPotionEffect(PotionEffectType.LUCK)) {
                            entity.getWorld().spawnParticle(Particle.INFESTED, entity.getLocation().add(0, 1.0, 0), 2, 0.3, 0.45, 0.3, 0.0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
}
