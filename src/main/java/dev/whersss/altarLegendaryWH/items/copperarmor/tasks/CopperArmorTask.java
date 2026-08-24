package dev.whersss.altarLegendaryWH.items.copperarmor.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.items.copperarmor.utils.CopperArmorFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CopperArmorTask extends BukkitRunnable implements Listener {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Integer> sneakTicks = new HashMap<>();
    private final Map<UUID, Boolean> glowingActive = new HashMap<>();
    private final Map<UUID, Map<PotionEffectType, Integer>> managedEffects = new HashMap<>();

    public CopperArmorTask(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    private boolean isCopperArmor(ItemStack item, String type) {
        if (item == null || !item.hasItemMeta()) return false;
        String value = item.getItemMeta().getPersistentDataContainer().get(CopperArmorFactory.getArmorKey(), PersistentDataType.STRING);
        return type.equals(value);
    }

    private void applyInfinite(Player player, PotionEffectType type) {
        applyInfinite(player, type, 0);
    }

    private void applyInfinite(Player player, PotionEffectType type, int amplifier) {
        Map<PotionEffectType, Integer> playerEffects = managedEffects.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        Integer ownedAmplifier = playerEffects.get(type);
        PotionEffect current = player.getPotionEffect(type);

        if (ownedAmplifier != null) {
            if (matchesManagedEffect(current, ownedAmplifier)) {
                if (ownedAmplifier == amplifier) return;
                player.removePotionEffect(type);
                current = null;
            }
            playerEffects.remove(type);
        }

        if (current != null) {
            if (playerEffects.isEmpty()) managedEffects.remove(player.getUniqueId());
            return;
        }

        PotionEffect effect = new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, false, false, true);
        if (player.addPotionEffect(effect)) {
            playerEffects.put(type, amplifier);
        } else if (playerEffects.isEmpty()) {
            managedEffects.remove(player.getUniqueId());
        }
    }

    private boolean matchesManagedEffect(PotionEffect effect, int amplifier) {
        return effect != null
                && effect.getDuration() == PotionEffect.INFINITE_DURATION
                && effect.getAmplifier() == amplifier;
    }

    private void removeIfInfinite(Player player, PotionEffectType type) {
        Map<PotionEffectType, Integer> playerEffects = managedEffects.get(player.getUniqueId());
        if (playerEffects == null) return;

        Integer ownedAmplifier = playerEffects.remove(type);
        if (ownedAmplifier != null && matchesManagedEffect(player.getPotionEffect(type), ownedAmplifier)) {
            player.removePotionEffect(type);
        }

        if (playerEffects.isEmpty()) managedEffects.remove(player.getUniqueId());
    }

    private void setGlowingForPlayer(Player viewer, Player target, boolean glow) {
        if (glow) {
            viewer.sendPotionEffectChange(target, new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, false));
            return;
        }

        PotionEffect actualEffect = target.getPotionEffect(PotionEffectType.GLOWING);
        if (actualEffect != null) {
            viewer.sendPotionEffectChange(target, actualEffect);
        } else {
            viewer.sendPotionEffectChangeRemove(target, PotionEffectType.GLOWING);
        }
    }
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.isAboveLegendaryHeight(player)) {
                removeIfInfinite(player, PotionEffectType.WATER_BREATHING);
                removeIfInfinite(player, PotionEffectType.RESISTANCE);
                removeIfInfinite(player, PotionEffectType.FIRE_RESISTANCE);
                removeBootsSpeed(player);
                handleSneakRelease(player);
                continue;
            }

            if (isCopperArmor(player.getInventory().getHelmet(), "helmet")) {
                if (player.isInWater()) {
                    applyInfinite(player, PotionEffectType.WATER_BREATHING);
                } else {
                    removeIfInfinite(player, PotionEffectType.WATER_BREATHING);
                }

                if (player.isSneaking()) {
                    int ticks = sneakTicks.getOrDefault(player.getUniqueId(), 0) + 1;
                    sneakTicks.put(player.getUniqueId(), ticks);

                    if (ticks == 20) {
                        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 2.0f);
                    } else if (ticks == 40) {
                        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.8f);
                    } else if (ticks == 80) {
                        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.6f);
                    } else if (ticks == 120) {
                        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.3f);
                    } else if (ticks == 180) {
                        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.5f, 0.3f);
                        player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1f, 1.7f);

                        player.getWorld().strikeLightningEffect(player.getLocation());

                        Location baseLoc = player.getLocation().add(0, 0.1, 0);
                        for (int i = 0; i < 36; i++) {
                            double angle = i * Math.PI / 18;
                            double dx = Math.cos(angle);
                            double dz = Math.sin(angle);
                            player.getWorld().spawnParticle(Particle.SCULK_SOUL, baseLoc, 0, dx, 0.0, dz, 0.15);
                        }

                        applyInfinite(player, PotionEffectType.DARKNESS);
                        glowingActive.put(player.getUniqueId(), true);

                        for (Player target : Bukkit.getOnlinePlayers()) {
                            if (!target.equals(player) && !plugin.isAboveLegendaryHeight(target)) {
                                setGlowingForPlayer(player, target, true);
                            }
                        }
                    }

                    if (glowingActive.getOrDefault(player.getUniqueId(), false) && ticks % 20 == 0) {
                        for (Player target : Bukkit.getOnlinePlayers()) {
                            if (!target.equals(player) && !plugin.isAboveLegendaryHeight(target)) {
                                setGlowingForPlayer(player, target, true);
                            }
                        }
                    }
                } else {
                    handleSneakRelease(player);
                }
            } else {
                removeIfInfinite(player, PotionEffectType.WATER_BREATHING);
                handleSneakRelease(player);
            }

            if (isCopperArmor(player.getInventory().getChestplate(), "chestplate")) {
                applyInfinite(player, PotionEffectType.RESISTANCE);
            } else {
                removeIfInfinite(player, PotionEffectType.RESISTANCE);
            }

            if (isCopperArmor(player.getInventory().getBoots(), "boots")) {
                applyInfinite(player, PotionEffectType.FIRE_RESISTANCE);
                Material blockFeet = player.getLocation().getBlock().getType();
                Material blockBelow = player.getLocation().subtract(0, 0.1, 0).getBlock().getType();
                boolean onCopper = blockFeet.name().contains("COPPER") || blockBelow.name().contains("COPPER");

                int desiredAmp = Math.max(0, onCopper ? plugin.getItemsConfig().getInt("copper-armor.boots.copper-speed-level", 5) - 1
                        : plugin.getItemsConfig().getInt("copper-armor.boots.speed-level", 3) - 1);

                applyInfinite(player, PotionEffectType.SPEED, desiredAmp);

                Location particleLoc = player.getLocation().add(0, 0.05, 0);
                player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.0, 0.0, 0.0, 0.01);
            } else {
                removeIfInfinite(player, PotionEffectType.FIRE_RESISTANCE);
                removeBootsSpeed(player);
            }
        }
    }

    private void handleSneakRelease(Player player) {
        if (sneakTicks.containsKey(player.getUniqueId())) {
            if (glowingActive.getOrDefault(player.getUniqueId(), false)) {
                player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1f, 1f);
                removeIfInfinite(player, PotionEffectType.DARKNESS);
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.equals(player)) {
                        setGlowingForPlayer(player, target, false);
                    }
                }
            }
            sneakTicks.remove(player.getUniqueId());
            glowingActive.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer());
    }

    public void cleanup() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }
        managedEffects.clear();
        sneakTicks.clear();
        glowingActive.clear();
    }

    private void cleanupPlayer(Player player) {
        handleSneakRelease(player);
        Map<PotionEffectType, Integer> playerEffects = managedEffects.get(player.getUniqueId());
        if (playerEffects != null) {
            for (PotionEffectType type : playerEffects.keySet().toArray(PotionEffectType[]::new)) {
                removeIfInfinite(player, type);
            }
        }
        sneakTicks.remove(player.getUniqueId());
        glowingActive.remove(player.getUniqueId());
    }
    private void removeBootsSpeed(Player player) {
        removeIfInfinite(player, PotionEffectType.SPEED);
    }
}