package dev.whersss.altarLegendaryWH.weapons.witherblade.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.witherblade.managers.WitherManager;
import dev.whersss.altarLegendaryWH.weapons.witherblade.tasks.WitherTasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import java.util.Random;

public class WitherBladeListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final WitherManager manager;
    private final Random random = new Random();

    public WitherBladeListener(AltarLegendaryWH plugin, WitherManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private boolean isWitherBlade(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        if (meta.getPersistentDataContainer().has(AltarLegendaryWH.getInstance().getWitherKey(), PersistentDataType.BYTE)) {
            return true;
        }
        return meta.hasCustomModelData() && (meta.getCustomModelData() == 9 || meta.getCustomModelData() == 3003);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (!isWitherBlade(item)) return;

        e.setCancelled(true);

        if (p.isSneaking()) {
            if (manager.isOnAuraCooldown(p)) {
                return;
            }
            WitherTasks.castAura(plugin, p, manager);
        } else {
            WitherTasks.castDash(plugin, p, manager);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

        if (e.getDamager() instanceof Player p && e.getEntity() instanceof LivingEntity victim) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (isWitherBlade(item)) {

                if (victim instanceof Player targetPlayer) {
                    if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                        e.setCancelled(true);
                        return;
                    }
                }


                manager.addAttackCharge(p);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            ItemStack item = killer.getInventory().getItemInMainHand();

            if (isWitherBlade(item)) {
                manager.fillMaxCharge(killer);

                Location loc = victim.getLocation().add(0, 1, 0);

                victim.getWorld().playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, 1.5f, 0.8f);

                Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.5f);
                victim.getWorld().spawnParticle(Particle.DUST, loc, 50, 0.5, 0.5, 0.5, 0.1, blackDust);

                for (int i = 0; i < 6; i++) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double ySpread = (random.nextDouble() - 0.2) * 0.8;
                    Vector dir = new Vector(Math.cos(angle), ySpread, Math.sin(angle)).normalize();

                    WitherTasks.launchSlimeProjectile(plugin, killer, victim.getEyeLocation(), dir, 5.0, false);
                }
            }
        }
    }
}