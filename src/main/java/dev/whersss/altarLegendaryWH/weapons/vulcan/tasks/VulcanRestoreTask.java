package dev.whersss.altarLegendaryWH.weapons.vulcan.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VulcanRestoreTask {

    private final AltarLegendaryWH plugin;
    private final Map<Location, Material> blocksToRestore;
    private final Location holoLoc;
    private final int totalSeconds;
    private TextDisplay hologram;

    public VulcanRestoreTask(AltarLegendaryWH plugin, Map<Location, Material> blocksToRestore, Location holoLoc, int totalSeconds) {
        this.plugin = plugin;
        this.blocksToRestore = blocksToRestore;
        this.holoLoc = holoLoc;
        this.totalSeconds = totalSeconds;
    }

    public void start() {
        Location spawnLoc = holoLoc.clone().subtract(0, 2.0, 0);

        hologram = (TextDisplay) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setShadowed(true);
        hologram.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

        Transformation transformation = hologram.getTransformation();
        transformation.getScale().set(2.0f, 2.0f, 2.0f);
        hologram.setTransformation(transformation);

        updateHolo(totalSeconds);

        new BukkitRunnable() {
            int timeLeft = totalSeconds;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    hologram.remove();
                    startRestoringBlocks();
                    cancel();
                    return;
                }

                updateHolo(timeLeft);
                timeLeft--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void updateHolo(int seconds) {
        if (hologram != null && hologram.isValid()) {
            Component text = TextUtils.legacy(
                    plugin.tr("&6⚠&f " + seconds + " секунд", "&6⚠&f " + seconds + " seconds")
            );
            hologram.text(text);
        }
    }

    private void startRestoringBlocks() {
        List<Location> locations = new ArrayList<>(blocksToRestore.keySet());

        locations.sort((first, second) -> {
            boolean airFirst = blocksToRestore.get(first).isAir();
            boolean airSecond = blocksToRestore.get(second).isAir();

            if (airFirst && !airSecond) return -1;
            if (!airFirst && airSecond) return 1;

            if (airFirst) {
                return Double.compare(second.getY(), first.getY());
            }
            return Double.compare(first.getY(), second.getY());
        });

        new BukkitRunnable() {
            int index = 0;
            final int blocksPerTick = 5;

            @Override
            public void run() {
                if (index >= locations.size()) {
                    cancel();
                    return;
                }

                for (int i = 0; i < blocksPerTick; i++) {
                    if (index >= locations.size()) break;

                    Location location = locations.get(index);
                    Material oldMaterial = blocksToRestore.get(location);
                    Material currentType = location.getBlock().getType();

                    if (currentType != oldMaterial) {
                        location.getBlock().setType(oldMaterial, false);
                        location.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.05);
                        if (Math.random() < 0.2) {
                            location.getWorld().playSound(location, Sound.BLOCK_LAVA_EXTINGUISH, 0.3f, 1.5f);
                        }
                    }
                    index++;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
