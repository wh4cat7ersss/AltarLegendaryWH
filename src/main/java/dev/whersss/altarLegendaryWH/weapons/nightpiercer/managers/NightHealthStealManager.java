package dev.whersss.altarLegendaryWH.weapons.nightpiercer.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NightHealthStealManager implements Listener {
    private final AltarLegendaryWH plugin;
    private final Map<UUID, Double> baseHealth = new HashMap<>();

    public NightHealthStealManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void applySteal(Player attacker, LivingEntity victim) {
        double amount = plugin.getWeaponsConfig().getDouble("nightpiercer.bite.health-steal", 4.0);
        int durationTicks = plugin.getWeaponsConfig().getInt("nightpiercer.bite.duration", 10) * 20;

        modifyMaxHealth(attacker, amount);
        if (victim instanceof Player targetPlayer) {
            modifyMaxHealth(targetPlayer, -amount);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                resetHealth(attacker);
                if (victim instanceof Player targetPlayer) {
                    resetHealth(targetPlayer);
                }
            }
        }.runTaskLater(plugin, durationTicks);
    }

    private void modifyMaxHealth(Player p, double delta) {
        if (p == null || !p.isOnline()) return;

        AttributeInstance maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double currentBase = maxHealthAttr.getBaseValue();
            if (!baseHealth.containsKey(p.getUniqueId())) {
                baseHealth.put(p.getUniqueId(), currentBase);
            }

            double newHealth = currentBase + delta;
            if (newHealth < 1.0) newHealth = 1.0;

            maxHealthAttr.setBaseValue(newHealth);

            if (delta > 0) {
                p.setHealth(Math.min(p.getHealth() + delta, maxHealthAttr.getValue()));
            }
        }
    }

    private void resetHealth(Player p) {
        if (p != null && baseHealth.containsKey(p.getUniqueId())) {
            AttributeInstance maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double original = baseHealth.get(p.getUniqueId());
                maxHealthAttr.setBaseValue(original);

                if (p.getHealth() > original) {
                    p.setHealth(original);
                }
            }
            baseHealth.remove(p.getUniqueId());
        }
    }

    public void revertAll() {
        for (UUID id : new HashSet<>(baseHealth.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) resetHealth(p);
        }
        baseHealth.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        resetHealth(e.getPlayer());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player p) {
            resetHealth(p);
        }
    }
}

