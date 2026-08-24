package dev.whersss.altarLegendaryWH.items.wardenheart.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.items.wardenheart.WardenHeartItem;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class WardenHeartDropListener implements Listener {

    private final AltarLegendaryWH plugin;

    public WardenHeartDropListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWardenDeath(EntityDeathEvent e) {
        if (e.getEntity().getType() != EntityType.WARDEN) {
            return;
        }

        if (!plugin.getItemsConfig().getBoolean("items.warden-heart.drop.enabled", true)) {
            return;
        }

        int amount = Math.max(0, plugin.getItemsConfig().getInt("items.warden-heart.drop.amount", 1));
        for (int i = 0; i < amount; i++) {
            e.getDrops().add(WardenHeartItem.create());
        }
    }
}
