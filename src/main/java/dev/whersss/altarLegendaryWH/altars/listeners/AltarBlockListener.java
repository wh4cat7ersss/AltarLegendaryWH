package dev.whersss.altarLegendaryWH.altars.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class AltarBlockListener implements Listener {

    private final AltarLegendaryWH plugin;

    public AltarBlockListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Location loc = event.getBlockPlaced().getLocation();

        for (int y = 0; y <= 3; y++) {
            Location checkLoc = loc.clone().subtract(0, y, 0);
            if (plugin.getAltarManager().getAltarAt(checkLoc) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
