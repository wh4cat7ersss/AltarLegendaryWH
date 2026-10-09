package dev.whersss.altarLegendaryWH.utils;

import dev.whersss.altarLegendaryWH.items.copperarmor.utils.CopperArmorFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

public final class SpeedBuffUtils {

    private SpeedBuffUtils() {}

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
     * Applies a temporary burst speed effect while intelligently preserving any previous speed potion.
     * Takes into account:
     * - remaining duration of previous potion (does not revive expired potions)
     * - copper armor boots (does not mistakenly restore Speed V when off copper blocks)
     * - existing stronger speed effects (does not downgrade)
     */
    public static void applyBurstSpeed(Player player, int durationTicks, int targetAmplifier, Plugin plugin) {
        if (player == null || !player.isOnline()) return;

        PotionEffect cur = player.getPotionEffect(PotionEffectType.SPEED);

        // If player already has a stronger speed effect, don't downgrade
        if (cur != null && cur.getAmplifier() > targetAmplifier) {
            return;
        }

        // If player has equal speed with longer duration, don't shorten
        if (cur != null && cur.getAmplifier() == targetAmplifier && cur.getDuration() >= durationTicks) {
            return;
        }

        // If player had a lower speed effect, preserve it
        if (cur != null && cur.getAmplifier() < targetAmplifier) {
            final PotionEffect oldEffect = cur;
            final boolean wasInfinite = oldEffect.getDuration() == PotionEffect.INFINITE_DURATION;
            final int remainingTicks = wasInfinite ? PotionEffect.INFINITE_DURATION : oldEffect.getDuration() - durationTicks;

            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, targetAmplifier, false, false));

            // Only schedule restore if remaining duration > 0 or was infinite
            if (wasInfinite || remainingTicks > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;

                    // If player is wearing copper boots, let the boots task manage speed dynamically
                    if (isWearingCopperBoots(player)) {
                        return;
                    }

                    PotionEffect activeNow = player.getPotionEffect(PotionEffectType.SPEED);
                    // Only restore if player has no speed or lower speed
                    if (activeNow == null || activeNow.getAmplifier() <= oldEffect.getAmplifier()) {
                        int durToApply = wasInfinite ? PotionEffect.INFINITE_DURATION : remainingTicks;
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.SPEED,
                                durToApply,
                                oldEffect.getAmplifier(),
                                oldEffect.isAmbient(),
                                oldEffect.hasParticles(),
                                oldEffect.hasIcon()
                        ));
                    }
                }, durationTicks);
            }
            return;
        }

        // Player had no speed effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, targetAmplifier, false, false));
    }
}
