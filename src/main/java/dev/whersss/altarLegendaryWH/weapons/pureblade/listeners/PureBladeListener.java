package dev.whersss.altarLegendaryWH.weapons.pureblade.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import dev.whersss.altarLegendaryWH.weapons.pureblade.managers.PureBladeManager;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

@SuppressWarnings("deprecation")
public class PureBladeListener implements Listener {

    private final PureBladeManager abilityManager;

    public PureBladeListener(PureBladeManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    private boolean isPureBlade(ItemStack item) {
        return WeaponFactory.isAltarWeapon(item, "pure_blade");
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isPureBlade(item)) return;

        event.setCancelled(true);

        if (player.isSneaking()) {
            abilityManager.castCycloneSlash(player);
        } else {
            abilityManager.castShadeSoul(player);
        }
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity victim) {
            if (isPureBlade(player.getInventory().getItemInMainHand())) {
                abilityManager.playMassiveSweep(player, victim);
            }
        }
    }
}
