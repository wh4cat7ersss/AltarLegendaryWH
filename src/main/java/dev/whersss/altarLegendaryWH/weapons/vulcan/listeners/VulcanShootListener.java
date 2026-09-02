package dev.whersss.altarLegendaryWH.weapons.vulcan.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.vulcan.managers.VulcanCooldownManager;
import dev.whersss.altarLegendaryWH.weapons.vulcan.tasks.ArrowParticleTask;
import dev.whersss.altarLegendaryWH.weapons.vulcan.tasks.MagmaProjectile;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class VulcanShootListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;

        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        if (!bow.getItemMeta().getPersistentDataContainer().has(plugin.getVulcanKey(), PersistentDataType.BYTE)) return;

        if (plugin.isAboveLegendaryHeight(player)) {
            event.setCancelled(true);
            return;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();

        if (player.isSneaking() && !VulcanCooldownManager.isOnCooldown(player)) {
            event.setCancelled(true);

            player.getWorld().playSound(eyeLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
            new MagmaProjectile(player, eyeLoc, dir.clone().multiply(1.5)).runTaskTimer(plugin, 0, 1);

            Vector recoil = dir.clone().multiply(-1).normalize().multiply(0.8).setY(0.25);
            player.setVelocity(recoil);

            int cooldownTime = plugin.getWeaponsConfig().getInt("vulcan_crossbow.magma-attack.cooldown", 25);
            VulcanCooldownManager.startCooldown(player, cooldownTime);
        }
        else if (event.getProjectile() instanceof Arrow arrow) {
            arrow.getPersistentDataContainer().set(plugin.getArrowKey(), PersistentDataType.BYTE, (byte) 1);

            player.getWorld().playSound(eyeLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.5f);
            player.getWorld().playSound(eyeLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.0f);

            Vector centerVec = player.getEyeLocation().getDirection().normalize().multiply(arrow.getVelocity().length());
            Vector originalVelocity = arrow.getVelocity();

            Vector newVec = originalVelocity.clone().multiply(0.25).add(centerVec.clone().multiply(0.75));
            arrow.setVelocity(newVec);

            double angle = originalVelocity.normalize().angle(player.getEyeLocation().getDirection().normalize());
            boolean isCentral = angle < 0.05;

            if (isCentral) {
                arrow.setFireTicks(1000);
            }

            new ArrowParticleTask(arrow, isCentral).runTaskTimer(plugin, 1, 1);
        }
    }
}

