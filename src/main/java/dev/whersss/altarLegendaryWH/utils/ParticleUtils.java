package dev.whersss.altarLegendaryWH.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ParticleUtils {

    /**
     * Single-shot vanilla debris burst: particles are spawned ONCE and the client physics
     * (velocity + gravity + drag) throws them outward 3-6 blocks.
     *
     * Why Particle.ITEM instead of Particle.BLOCK:
     * - Client TerrainParticle (BLOCK) normalizes any passed velocity to a fixed tiny magnitude,
     *   so "speed" is ignored no matter what API is used.
     * - BLOCK_CRUMBLE always has its velocity forced to 0 by the client.
     * - BreakingItemParticle (ITEM) keeps the passed velocity as-is. With an ItemStack of the
     *   block it renders the same block texture chunks.
     *
     * With count = 0 the offsets are used as the exact velocity vector (offset * extra).
     */
    public static void spawnBlockDispersion(Plugin plugin, Location center, BlockData blockData) {
        spawnBlockDispersion(plugin, center, blockData, 4.5, 74);
    }

    public static void spawnBlockDispersion(Plugin plugin, Location center, BlockData blockData, double maxRadius) {
        spawnBlockDispersion(plugin, center, blockData, maxRadius, 74);
    }

    public static void spawnBlockDispersion(Plugin plugin, Location center, BlockData blockData, double maxRadius, int totalCount) {
        if (center == null || center.getWorld() == null || blockData == null) return;
        Material mat = blockData.getMaterial();
        if (!mat.isItem() || mat.isAir()) {
            mat = Material.COBBLESTONE;
        }

        World world = center.getWorld();
        Location origin = center.clone().add(0, 0.8, 0);
        ItemStack stack = new ItemStack(mat);

        // Increased horizontal scaling: debris bursts noticeably wider
        double baseH = Math.max(0.32, maxRadius * 0.14);

        int mainCount = Math.max(8, (int) (totalCount * 0.45));
        int fountainCount = Math.max(4, (int) (totalCount * 0.20));
        int lowCount = Math.max(4, (int) (totalCount * 0.15));
        int downCount = Math.max(4, totalCount - mainCount - fountainCount - lowCount);

        // 1. Main radial outward burst (arcs upwards in 3D dome)
        for (int i = 0; i < mainCount; i++) {
            double angle = (Math.PI * 2.0 * i / mainCount) + (Math.random() - 0.5) * 0.35;
            double h = baseH * (0.80 + Math.random() * 0.70);
            double vx = Math.cos(angle) * h;
            double vz = Math.sin(angle) * h;
            double vy = 0.42 + Math.random() * 0.38;
            world.spawnParticle(Particle.ITEM, origin, 0, vx, vy, vz, 1.0, stack, true);
        }

        // 2. Upward fountain chunks (erupts high upwards)
        for (int i = 0; i < fountainCount; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double h = 0.08 + Math.random() * (baseH * 0.50);
            double vy = 0.65 + Math.random() * 0.45;
            world.spawnParticle(Particle.ITEM, origin, 0, Math.cos(angle) * h, vy, Math.sin(angle) * h, 1.0, stack, true);
        }

        // 3. Medium-arc debris
        for (int i = 0; i < lowCount; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double h = baseH * (1.00 + Math.random() * 0.45);
            double vy = 0.22 + Math.random() * 0.20;
            world.spawnParticle(Particle.ITEM, origin, 0, Math.cos(angle) * h, vy, Math.sin(angle) * h, 1.0, stack, true);
        }

        // 4. Downward debris (shoots downwards towards ground/edges)
        for (int i = 0; i < downCount; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double h = baseH * (0.70 + Math.random() * 0.50);
            double vy = -0.15 - Math.random() * 0.35;
            world.spawnParticle(Particle.ITEM, origin, 0, Math.cos(angle) * h, vy, Math.sin(angle) * h, 1.0, stack, true);
        }
    }
}
