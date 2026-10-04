package dev.whersss.altarLegendaryWH.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of temporary display and effect entities spawned by legendary weapons.
 * Ensures zero entity leaks on plugin reload, disable, server stop, or crash recovery.
 */
public class VisualCleanupManager {

    private final AltarLegendaryWH plugin;
    private final NamespacedKey tagKey;
    private final Set<Entity> trackedEntities = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public VisualCleanupManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        this.tagKey = new NamespacedKey(plugin, "altar_temp_display");
        cleanupWorldOrphans();
    }

    /**
     * Tags and registers a temporary visual entity for automatic tracking and cleanup.
     */
    public void track(Entity entity) {
        if (entity == null || !entity.isValid()) return;
        try {
            entity.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
        } catch (Throwable ignored) {
        }
        trackedEntities.add(entity);
    }

    /**
     * Untracks an entity if removed normally.
     */
    public void untrack(Entity entity) {
        if (entity != null) {
            trackedEntities.remove(entity);
        }
    }

    public boolean isTrackedEntity(Entity entity) {
        if (entity == null) return false;
        return trackedEntities.contains(entity)
                || entity.getPersistentDataContainer().has(tagKey, PersistentDataType.BYTE);
    }

    /**
     * Safely removes all tracked entities and cleans up orphaned display entities in loaded worlds.
     */
    public void cleanupAll() {
        for (Entity entity : trackedEntities) {
            try {
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            } catch (Throwable ignored) {
            }
        }
        trackedEntities.clear();
        cleanupWorldOrphans();
    }

    private void cleanupWorldOrphans() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                try {
                    if (entity.getPersistentDataContainer().has(tagKey, PersistentDataType.BYTE)) {
                        entity.remove();
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
