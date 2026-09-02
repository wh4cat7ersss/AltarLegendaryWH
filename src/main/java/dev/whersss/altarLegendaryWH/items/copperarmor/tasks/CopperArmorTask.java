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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CopperArmorTask extends BukkitRunnable implements Listener {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Integer> sneakTicks = new HashMap<>();
    private final Map<UUID, Integer> activeTicks = new HashMap<>();
    private final Map<UUID, Set<UUID>> glowingTargets = new HashMap<>();
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

    private int getChargeHoldTicks() {
        return Math.max(20, plugin.getItemsConfig().getInt("copper-armor.helmet.charge-hold-ticks", 100));
    }

    private int getChargeSoundIntervalTicks() {
        return Math.max(1, plugin.getItemsConfig().getInt("copper-armor.helmet.charge-sound-interval-ticks", 25));
    }

    private int getGlowDurationTicks() {
        int ticks = plugin.getItemsConfig().getInt("copper-armor.helmet.glowing-duration-ticks", 0);
        if (ticks > 0) {
            return ticks;
        }

        int seconds = plugin.getItemsConfig().getInt("copper-armor.helmet.glowing-duration-seconds", 20);
        return Math.max(1, seconds * 20);
    }

    private boolean isVisibleOnlyToOwner() {
        return plugin.getItemsConfig().getBoolean("copper-armor.helmet.visible-only-to-owner", true);
    }

    private boolean hasActiveGlow(UUID playerId) {
        return activeTicks.containsKey(playerId);
    }

    private void stopCharging(Player player) {
        sneakTicks.remove(player.getUniqueId());
    }

    private void updateVisibleGlows(Player owner) {
        Set<UUID> currentTargets = glowingTargets.computeIfAbsent(owner.getUniqueId(), ignored -> new HashSet<>());
        Set<UUID> nextTargets = new HashSet<>();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(owner)) continue;

            nextTargets.add(target.getUniqueId());
            if (!currentTargets.contains(target.getUniqueId())) {
                setGlowingForPlayer(owner, target, true);
            }
        }

        for (UUID targetId : new HashSet<>(currentTargets)) {
            if (nextTargets.contains(targetId)) continue;
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                setGlowingForPlayer(owner, target, false);
            }
            currentTargets.remove(targetId);
        }

        currentTargets.addAll(nextTargets);
    }

    private void startHelmetAbility(Player player) {
        UUID playerId = player.getUniqueId();
        activeTicks.put(playerId, getGlowDurationTicks());
        glowingTargets.put(playerId, new HashSet<>());
        stopCharging(player);
        player.getWorld().strikeLightningEffect(player.getLocation());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f);
        player.showElderGuardian();
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
    }

    private void endHelmetAbility(Player player) {
        UUID playerId = player.getUniqueId();
        Set<UUID> targets = glowingTargets.remove(playerId);
        if (targets != null) {
            for (UUID targetId : targets) {
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) {
                    setGlowingForPlayer(player, target, false);
                }
            }
        }
        activeTicks.remove(playerId);
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
        int duration = Math.max(40, getGlowDurationTicks());
        if (glow) {
            if (isVisibleOnlyToOwner()) {
                viewer.sendPotionEffectChange(target, new PotionEffect(PotionEffectType.GLOWING, duration, 0, false, false, false));
            } else {
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0, false, false, false));
            }
            return;
        }

        if (isVisibleOnlyToOwner()) {
            PotionEffect actualEffect = target.getPotionEffect(PotionEffectType.GLOWING);
            if (actualEffect != null) {
                viewer.sendPotionEffectChange(target, actualEffect);
            } else {
                viewer.sendPotionEffectChangeRemove(target, PotionEffectType.GLOWING);
            }
        } else {
            target.removePotionEffect(PotionEffectType.GLOWING);
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
                if (hasActiveGlow(player.getUniqueId())) {
                    endHelmetAbility(player);
                }
                stopCharging(player);
                continue;
            }

            if (isCopperArmor(player.getInventory().getHelmet(), "helmet")) {
                if (player.isInWater()) {
                    applyInfinite(player, PotionEffectType.WATER_BREATHING);
                } else {
                    removeIfInfinite(player, PotionEffectType.WATER_BREATHING);
                }

                UUID playerId = player.getUniqueId();
                if (hasActiveGlow(playerId)) {
                    int remaining = activeTicks.get(playerId) - 1;
                    if (remaining <= 0) {
                        endHelmetAbility(player);
                    } else {
                        activeTicks.put(playerId, remaining);
                        updateVisibleGlows(player);
                    }
                } else if (player.isSneaking()) {
                    int ticks = sneakTicks.getOrDefault(playerId, 0) + 1;
                    sneakTicks.put(playerId, ticks);

                    if (ticks >= getChargeHoldTicks()) {
                        startHelmetAbility(player);
                        updateVisibleGlows(player);
                    } else if (ticks % getChargeSoundIntervalTicks() == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, 1f, 1.0f);
                    }
                } else {
                    stopCharging(player);
                }
            } else {
                removeIfInfinite(player, PotionEffectType.WATER_BREATHING);
                stopCharging(player);
                if (hasActiveGlow(player.getUniqueId())) {
                    endHelmetAbility(player);
                }
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
        activeTicks.clear();
        glowingTargets.clear();
    }

    private void cleanupPlayer(Player player) {
        stopCharging(player);
        if (hasActiveGlow(player.getUniqueId())) {
            endHelmetAbility(player);
        }
        Map<PotionEffectType, Integer> playerEffects = managedEffects.get(player.getUniqueId());
        if (playerEffects != null) {
            for (PotionEffectType type : playerEffects.keySet().toArray(PotionEffectType[]::new)) {
                removeIfInfinite(player, type);
            }
        }
        sneakTicks.remove(player.getUniqueId());
        activeTicks.remove(player.getUniqueId());
        glowingTargets.remove(player.getUniqueId());
    }
    private void removeBootsSpeed(Player player) {
        removeIfInfinite(player, PotionEffectType.SPEED);
    }
}
