package dev.whersss.altarLegendaryWH.weapons.palegun.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.ParticleUtils;
import dev.whersss.altarLegendaryWH.weapons.palegun.managers.PaleGunAbilityManager;
import dev.whersss.altarLegendaryWH.weapons.palegun.tasks.PaleProjectileTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class PaleGunListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final PaleGunAbilityManager abilityManager;

    public PaleGunListener(AltarLegendaryWH plugin, PaleGunAbilityManager abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !abilityManager.isPaleGun(item)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.hasCooldown(item)) {
            int ticks = player.getCooldown(item);
            if (ticks <= 20) {
                player.setCooldown(item, 0);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack bow = event.getBow();
        if (bow == null || !abilityManager.isPaleGun(bow)) {
            return;
        }


        if (!player.hasCooldown(bow)) {
            int cdSec = plugin.getWeaponsConfig().getInt("pale-gun.shoot.cooldown", 10);
            player.setCooldown(bow, cdSec * 20);
        }

        if (event.getProjectile() instanceof AbstractArrow arrow) {
            arrow.remove();
        }

        clearCharge(player, bow);
        shoot(player, bow);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (!abilityManager.isPaleGun(event.getMainHandItem()) && !abilityManager.isPaleGun(event.getOffHandItem())) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);


        if (!abilityManager.activatePaleRoots(player)) {
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBackstab(EntityDamageByEntityEvent event) {
        if (!CombatUtils.isDirectMeleeHit(event)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (victim instanceof Player playerVictim && plugin.getFriendManager().isFriend(attacker.getUniqueId(), playerVictim.getUniqueId())) {
            return;
        }
        if (!victim.hasPotionEffect(PotionEffectType.LUCK)) {
            return;
        }

        Vector victimLook = victim.getEyeLocation().getDirection().setY(0);
        Vector toAttacker = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).setY(0);
        if (victimLook.lengthSquared() < 0.0001 || toAttacker.lengthSquared() < 0.0001) {
            return;
        }

        double dot = victimLook.normalize().dot(toAttacker.normalize());
        if (dot < -0.3) {
            double multiplier = plugin.getWeaponsConfig().getDouble("pale-gun.backstab.multiplier", 1.5);
            event.setDamage(event.getDamage() * multiplier);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_CREAKING_ATTACK, 0.8f, 1.05f);
            victim.getWorld().spawnParticle(Particle.INFESTED, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.01);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        abilityManager.resetPlayer(event.getPlayer());
    }

    private void shoot(Player player, ItemStack item) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);

        Vector recoil = direction.clone().multiply(-1).normalize().multiply(0.8).setY(0.25);
        player.setVelocity(recoil);

        double speed = plugin.getWeaponsConfig().getDouble("pale-gun.shoot.projectile-speed", 1.8);
        PaleProjectileTask projectileTask = new PaleProjectileTask(player, eyeLoc, direction.multiply(speed), plugin, abilityManager);
        abilityManager.trackProjectile(projectileTask);
        projectileTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void clearCharge(Player player, ItemStack item) {
        if (!(item.getItemMeta() instanceof CrossbowMeta meta) || !meta.hasChargedProjectiles()) {
            return;
        }

        meta.setChargedProjectiles(new ArrayList<>());
        item.setItemMeta(meta);
        player.updateInventory();
    }
}

