package dev.whersss.altarLegendaryWH.weapons.cutlass.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.weapons.cutlass.managers.CutlassManager;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class CutlassListener implements Listener {

    private final CutlassManager manager;

    public CutlassListener(CutlassManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onCutlassAbility(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!manager.isCutlass(item)) return;

        event.setCancelled(true);

        if (player.isSneaking()) {
            if (manager.checkCooldown(player, "parry")) return;
            if (manager.hasActiveParry(player.getUniqueId())) return;
            manager.startParry(player);
            return;
        }

        if (AltarLegendaryWH.getInstance().isAboveLegendaryHeight(player)) return;
        if (manager.checkCooldown(player, "thousand_cuts")) return;
        if (manager.isDoingThousandCuts(player)) return;

        manager.startThousandCuts(player);
    }

    @EventHandler
    public void onDropWeapon(PlayerDropItemEvent event) {
        if (manager.isDoingThousandCuts(event.getPlayer())) {
            manager.cancelThousandCuts(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (manager.isDoingThousandCuts(event.getEntity())) {
            manager.cancelThousandCuts(event.getEntity());
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        if (manager.isDoingThousandCuts(event.getPlayer())) {
            manager.cancelThousandCuts(event.getPlayer());
        }
    }


    @EventHandler
    public void onMeleeHit(EntityDamageByEntityEvent event) {
        if (!CombatUtils.isDirectMeleeHit(event)) return;
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        if (!manager.isCutlass(player.getInventory().getItemInMainHand())) return;
        if (manager.isDoingThousandCuts(player)) return;
        if (event.getFinalDamage() <= 0) return;
        manager.playCutlassSwingFX(player);

        boolean fullyCharged = player.getCooledAttackStrength(0) >= 0.85f;
        if (!fullyCharged) return;
        if (ThreadLocalRandom.current().nextInt(100) < 20) {
            Vector pull = player.getLocation().toVector()
                    .subtract(target.getLocation().toVector())
                    .normalize()
                    .multiply(0.55)
                    .setY(0.2);
            target.setVelocity(pull);
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.5f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onParryHitReceive(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!manager.hasActiveParry(victim.getUniqueId())) return;

        event.setCancelled(true);
        event.setDamage(0);

        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            if (event.isApplicable(modifier)) {
                try {
                    event.setDamage(modifier, 0);
                } catch (UnsupportedOperationException ignored) {
                }
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            if (event instanceof EntityDamageByEntityEvent damageEvent && damageEvent.getDamager() instanceof Player attacker) {
                if (attacker.getCooledAttackStrength(0) < 0.8f) return;
            }
        }

        CutlassManager.ParryData data = manager.getParryData(victim.getUniqueId());
        if (data == null) return;

        data.lastHitTime = System.currentTimeMillis();
        data.charges--;

        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);

        if (event instanceof EntityDamageByEntityEvent damageEvent && damageEvent.getDamager() instanceof LivingEntity attacker) {
            double knockbackPower = AltarLegendaryWH.getInstance().getWeaponsConfig().getDouble("cutlass.parry.knockback-power", 0.8);
            Vector knockback = attacker.getLocation().toVector()
                    .subtract(victim.getLocation().toVector())
                    .normalize()
                    .multiply(knockbackPower)
                    .setY(0.3);
            attacker.setVelocity(knockback);
        }

        if (data.charges <= 0) {
            manager.endParry(victim);
        } else {
            data.bar.setTitle(manager.parryTitle(data.charges));
        }
    }
}

