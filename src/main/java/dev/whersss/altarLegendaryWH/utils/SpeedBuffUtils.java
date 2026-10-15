package dev.whersss.altarLegendaryWH.utils;

import dev.whersss.altarLegendaryWH.items.copperarmor.utils.CopperArmorFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpeedBuffUtils implements Listener {

    private static class SavedSpeed {
        final int amplifier;
        final int originalDuration;
        final boolean wasInfinite;
        final boolean ambient;
        final boolean particles;
        final boolean icon;
        final long startTime;
        long token;

        SavedSpeed(PotionEffect effect, long startTime, long token) {
            this.amplifier = effect.getAmplifier();
            this.originalDuration = effect.getDuration();
            this.wasInfinite = (effect.getDuration() == PotionEffect.INFINITE_DURATION);
            this.ambient = effect.isAmbient();
            this.particles = effect.hasParticles();
            this.icon = effect.hasIcon();
            this.startTime = startTime;
            this.token = token;
        }
    }

    private static final Map<UUID, SavedSpeed> savedSpeeds = new ConcurrentHashMap<>();
    private static long tokenCounter = 0;

    public SpeedBuffUtils() {}

    /**
     * Checks if the player is currently wearing copper boots.
     */
    public static boolean isWearingCopperBoots(Player player) {
        if (player == null) return false;
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || !boots.hasItemMeta()) return false;
        String val = boots.getItemMeta().getPersistentDataContainer().get(CopperArmorFactory.getArmorKey(), PersistentDataType.STRING);
        return "boots".equals(val);
    }

    /**
     * Clears tracked speed save for a player.
     */
    public static void cleanup(UUID uuid) {
        if (uuid != null) {
            savedSpeeds.remove(uuid);
        }
    }

    /**
     * Clears all tracked speed saves.
     */
    public static void cleanupAll() {
        savedSpeeds.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        cleanup(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.MILK_BUCKET) {
            cleanup(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Applies a temporary burst speed effect while intelligently preserving any previous speed potion.
     * Takes into account:
     * - remaining duration of previous potion (does not revive expired potions)
     * - copper armor boots (does not mistakenly restore Speed V when off copper blocks)
     * - existing stronger speed effects (does not downgrade)
     * - expiring burst speed effects (correctly clears burst and restores original potion)
     */
    public static void applyBurstSpeed(Player player, int durationTicks, int targetAmplifier, Plugin plugin) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        PotionEffect cur = player.getPotionEffect(PotionEffectType.SPEED);
        boolean isTrackingBurst = savedSpeeds.containsKey(uuid);

        // If player already has a stronger speed effect from an external source (and is not in a tracked burst), don't downgrade
        if (!isTrackingBurst && cur != null && cur.getAmplifier() > targetAmplifier) {
            return;
        }

        // If player has equal speed with longer duration (and is not in a tracked burst), don't shorten
        if (!isTrackingBurst && cur != null && cur.getAmplifier() == targetAmplifier && cur.getDuration() >= durationTicks) {
            return;
        }

        boolean wearingBoots = isWearingCopperBoots(player);

        // If player has a lower speed effect and we are not wearing copper boots, save the original potion!
        if (!wearingBoots && !isTrackingBurst) {
            if (cur != null && cur.getAmplifier() < targetAmplifier) {
                long token = ++tokenCounter;
                savedSpeeds.put(uuid, new SavedSpeed(cur, System.currentTimeMillis(), token));
            }
        }

        final long currentToken;
        SavedSpeed saved = savedSpeeds.get(uuid);
        if (saved != null) {
            currentToken = ++tokenCounter;
            saved.token = currentToken;
        } else {
            currentToken = 0;
        }

        // Apply burst speed effect (force/override to ensure it applies over any existing weaker speed)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, targetAmplifier, false, false), true);

        // If we have a saved effect to restore, schedule the restoration
        if (saved != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || player.isDead()) {
                    savedSpeeds.remove(uuid);
                    return;
                }

                if (isWearingCopperBoots(player)) {
                    savedSpeeds.remove(uuid);
                    return;
                }

                SavedSpeed currentSaved = savedSpeeds.get(uuid);
                if (currentSaved == null || currentSaved.token != currentToken) {
                    return; // Another burst was chained or already restored
                }

                savedSpeeds.remove(uuid);

                int remTicks;
                if (currentSaved.wasInfinite) {
                    remTicks = PotionEffect.INFINITE_DURATION;
                } else {
                    long elapsedTicks = (System.currentTimeMillis() - currentSaved.startTime) / 50L;
                    remTicks = (int) (currentSaved.originalDuration - elapsedTicks);
                }

                if (remTicks <= 0) {
                    return; // Original potion naturally expired during the burst
                }

                PotionEffect activeNow = player.getPotionEffect(PotionEffectType.SPEED);

                // If player drank or received a new potion with equal level and longer duration during the burst, keep it
                if (activeNow != null && activeNow.getAmplifier() == currentSaved.amplifier && activeNow.getDuration() > remTicks + 20) {
                    return;
                }

                // If player received a stronger effect from an external source (not the burst), keep it
                if (activeNow != null && activeNow.getAmplifier() > currentSaved.amplifier && activeNow.getAmplifier() != targetAmplifier) {
                    return;
                }

                // Check if current effect is the expiring burst effect or weaker/null
                boolean isExpiringBurst = (activeNow != null && activeNow.getAmplifier() == targetAmplifier && activeNow.getDuration() <= 20);
                boolean isWeakerOrNull = (activeNow == null || activeNow.getAmplifier() <= currentSaved.amplifier);

                if (isWeakerOrNull || isExpiringBurst) {
                    player.removePotionEffect(PotionEffectType.SPEED);
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SPEED,
                            remTicks,
                            currentSaved.amplifier,
                            currentSaved.ambient,
                            currentSaved.particles,
                            currentSaved.icon
                    ));
                }
            }, durationTicks);
        }
    }
}
