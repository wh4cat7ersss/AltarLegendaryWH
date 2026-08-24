package dev.whersss.altarLegendaryWH.weapons.vulcan.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MagmaProjectile extends BukkitRunnable {
    private final Player shooter;
    private final Vector velocity;
    private final BlockDisplay display;
    private Location currentLoc;
    private int ticks = 0;
    private static final Set<Material> PROTECTED_BLOCKS = new HashSet<>();

    static {
        PROTECTED_BLOCKS.add(Material.BEDROCK);
        PROTECTED_BLOCKS.add(Material.OBSIDIAN);
        PROTECTED_BLOCKS.add(Material.REINFORCED_DEEPSLATE);
        PROTECTED_BLOCKS.add(Material.CRYING_OBSIDIAN);
    }

    public MagmaProjectile(Player shooter, Location start, Vector velocity) {
        this.shooter = shooter;
        this.currentLoc = start.clone();

        this.velocity = velocity.multiply(1.8);

        this.display = (BlockDisplay) start.getWorld().spawnEntity(start, EntityType.BLOCK_DISPLAY);
        display.setBlock(Material.MAGMA_BLOCK.createBlockData());
        display.setInterpolationDuration(1);
        display.setTeleportDuration(1);
        display.setInterpolationDelay(0);

        Matrix4f transform = new Matrix4f().translate(-0.5f, -0.5f, -0.5f);
        display.setTransformationMatrix(transform);
    }

    @Override
    public void run() {
        if (ticks++ > 150 || display.isDead()) {
            this.cancel();
            return;
        }

        velocity.setY(velocity.getY() - 0.05);

        RayTraceResult rayResult = currentLoc.getWorld().rayTrace(
                currentLoc, velocity.clone().normalize(), velocity.length() + 0.1,
                FluidCollisionMode.NEVER, true, 0.5, entity -> entity != shooter && entity != display
        );

        if (rayResult != null) {
            impact(rayResult.getHitPosition().toLocation(currentLoc.getWorld()), rayResult.getHitBlockFace());
            this.cancel();
            return;
        }

        currentLoc.add(velocity);
        display.teleport(currentLoc);

        Vector particleOffset = velocity.clone().normalize().multiply(0.9);
        Location trailLoc = currentLoc.clone().subtract(particleOffset);
        currentLoc.getWorld().spawnParticle(Particle.FLAME, trailLoc, 5, 0.02, 0.02, 0.02, 0.05);
    }

    private void impact(Location loc, BlockFace hitFace) {
        display.remove();

        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        float power = (float) plugin.getConfig().getDouble("vulcan_crossbow.magma-attack.explosion-power", 2.0);
        loc.getWorld().createExplosion(loc, power, false, false, shooter);

        loc.getWorld().playSound(loc, Sound.BLOCK_SCULK_SPREAD, 1.0f, 1.0f);
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);

        int radius = plugin.getConfig().getInt("vulcan_crossbow.magma-attack.radius", 3);
        double spreadChance = plugin.getConfig().getDouble("vulcan_crossbow.magma-attack.spread-chance", 0.8);
        boolean autoReset = plugin.getConfig().getBoolean("vulcan_crossbow.magma-attack.auto-reset", false);

        loc.getWorld().spawnParticle(Particle.FLAME, loc, 150, radius / 1.5, radius / 1.5, radius / 1.5, 1.2);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);

        Map<Location, Material> oldBlocks = new HashMap<>();
        boolean isFloor = (hitFace == BlockFace.UP);

        if (autoReset) {
            int saveRadius = isFloor ? radius + 2 : radius;
            for (int x = -saveRadius; x <= saveRadius; x++) {
                for (int y = -saveRadius; y <= saveRadius; y++) {
                    for (int z = -saveRadius; z <= saveRadius; z++) {
                        if (x * x + y * y + z * z <= saveRadius * saveRadius) {
                            Block b = loc.clone().add(x, y, z).getBlock();
                            oldBlocks.put(b.getLocation(), b.getType());
                            Block up = b.getRelative(0, 1, 0);
                            oldBlocks.putIfAbsent(up.getLocation(), up.getType());
                        }
                    }
                }
            }
        }

        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, radius + 1, radius + 1, radius + 1)) {
            if (e instanceof LivingEntity target && e != shooter) {
                if (target instanceof Player targetPlayer) {
                    if (plugin.getFriendManager().isFriend(shooter.getUniqueId(), targetPlayer.getUniqueId())) {
                        continue;
                    }
                }
                target.setFireTicks(100);
            }
        }

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= 0; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        Block b = loc.clone().add(x, y, z).getBlock();
                        Material type = b.getType();

                        if (type == Material.AIR || type == Material.CAVE_AIR) continue;
                        if (PROTECTED_BLOCKS.contains(type)) continue;
                        if (!b.getType().isSolid() && type != Material.WATER) continue;

                        if (Math.random() < spreadChance) {
                            if (Math.random() > 0.5) b.setType(Material.MAGMA_BLOCK);
                            else b.setType(Material.LAVA);
                        }
                    }
                }
            }
        }

        if (isFloor) {
            int moundRadius = Math.max(1, radius - 1);
            for (int x = -moundRadius; x <= moundRadius; x++) {
                for (int z = -moundRadius; z <= moundRadius; z++) {
                    if (x * x + z * z <= moundRadius * moundRadius) {
                        Block b = loc.clone().add(x, 0, z).getBlock();
                        Material type = b.getType();

                        if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.WATER ||
                                type == Material.SHORT_GRASS || type == Material.TALL_GRASS || type == Material.SNOW || type == Material.FIRE) {

                            if (Math.random() < spreadChance * 0.75) {
                                if (Math.random() > 0.4) b.setType(Material.MAGMA_BLOCK);
                                else b.setType(Material.LAVA);
                            }
                        }
                    }
                }
            }
        }

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= 2; y++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == Material.MAGMA_BLOCK || b.getType() == Material.LAVA) {
                        Block up = b.getRelative(0, 1, 0);
                        if (up.getType() == Material.AIR && Math.random() > 0.4) {
                            up.setType(Material.FIRE);
                        }
                    }
                }
            }
        }

        if (autoReset && !oldBlocks.isEmpty()) {
            int delaySeconds = plugin.getConfig().getInt("vulcan_crossbow.magma-attack.reset-delay", 20);
            Location holoLoc = loc.clone().add(0, radius + 1.5, 0);
            if (isFloor) holoLoc.add(0, 1.5, 0);

            new VulcanRestoreTask(plugin, oldBlocks, holoLoc, delaySeconds).start();
        }
    }
}