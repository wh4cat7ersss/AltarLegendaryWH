package dev.whersss.altarLegendaryWH.weapons.paladinsbattleaxe.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.paladinsbattleaxe.managers.PaladinsBattleAxeManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PaladinsBattleAxeListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final PaladinsBattleAxeManager manager;

    public PaladinsBattleAxeListener(AltarLegendaryWH plugin, PaladinsBattleAxeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private final java.util.Map<java.util.UUID, Long> debounceMap = new java.util.concurrent.ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ItemStack mainItem = player.getInventory().getItemInMainHand();

        if (!manager.isPaladinsBattleAxe(mainItem)) return;

        event.setCancelled(true);
        player.updateInventory();

        // Prevent double-firing (e.g. if cooldown is set to 0 during testing)
        if (System.currentTimeMillis() - debounceMap.getOrDefault(player.getUniqueId(), 0L) < 250) {
            return;
        }
        debounceMap.put(player.getUniqueId(), System.currentTimeMillis());

        if (player.isSneaking()) {
            manager.useStalwartAbsorption(player);
        } else {
            manager.useEarthShatter(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMeleeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!manager.isPaladinsBattleAxe(weapon)) return;

        Vector dir = attacker.getEyeLocation().getDirection().normalize();
        Location center = attacker.getLocation().add(0, 1.0, 0).add(dir.clone().multiply(1.0));
        center.setDirection(dir);
        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        spawnSlashArc(attacker.getWorld(), center, right, true, Color.fromRGB(255, 200, 50));

        // Trident throw sound with pitch 0.6 on sweep hit
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 0.6f);

        if (manager.isAbsorbing(attacker.getUniqueId())) {
            attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.8f);
        }
    }

    private void spawnSlashArc(World world, Location center, Vector right, boolean flip, Color color) {
        int points = 22;
        double width = 2.2;
        double height = 1.5;
        Vector forward = center.getDirection();

        for (int i = 0; i < points; i++) {
            double progress = (double) i / (double) (points - 1);
            double xOffset = (progress - 0.5) * width * (flip ? 1.0 : -1.0);
            double yBase = (0.5 - progress) * height;
            double yCurve = Math.sin(progress * Math.PI) * 0.3;
            double yOffset = yBase + yCurve;
            double zOffset = Math.sin(progress * Math.PI) * 0.4;

            Location pLoc = center.clone()
                    .add(right.clone().multiply(xOffset))
                    .add(forward.clone().multiply(zOffset))
                    .add(0, yOffset, 0);

            // Medium and small size dust particles (0.6f - 0.8f)
            float particleSize = (i % 2 == 0) ? 0.6f : 0.8f;
            world.spawnParticle(Particle.DUST, pLoc, 1, 0.02, 0.02, 0.02, 0.0, new Particle.DustOptions(color, particleSize));
            if (i % 2 == 0) {
                world.spawnParticle(Particle.CRIT, pLoc, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        if (manager.isAbsorbing(victim.getUniqueId())) {
            double damage = event.getDamage();
            manager.recordAbsorbedDamage(victim.getUniqueId(), damage);
            event.setDamage(0.0);

            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.0, 0), 10, 0.3, 0.5, 0.3, 0.0,
                    new Particle.DustOptions(Color.YELLOW, 1.5f));
        }
    }
}
