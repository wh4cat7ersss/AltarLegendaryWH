package dev.whersss.altarLegendaryWH.weapons.frostscythe.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.weapons.frostscythe.listeners.FrostListener;
import dev.whersss.altarLegendaryWH.weapons.frostscythe.managers.FrostAbilityManager;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class FrostScytheTask extends BukkitRunnable {
    private static final int MOTION_INTERPOLATION_TICKS = 2;

    private final AltarLegendaryWH plugin;
    private final Player owner;
    private final FrostAbilityManager abilityManager;
    private final ItemDisplay display;
    private final Vector direction;
    private final ItemStack item;
    private final double returnSpeed;
    private boolean returning = false;
    private float roll = 0;
    private int orbitTicks = 0;

    private final Set<UUID> hitOutward = new HashSet<>();
    private final Set<UUID> hitOnReturn = new HashSet<>();

    public FrostScytheTask(AltarLegendaryWH plugin, Player owner, FrostAbilityManager abilityManager, ItemStack item) {
        this.plugin = plugin;
        this.owner = owner;
        this.abilityManager = abilityManager;
        this.item = item;

        double flightSpeed = plugin.getWeaponsConfig().getDouble("frost-scythe.throw.flight-speed", 1.65);
        this.returnSpeed = plugin.getWeaponsConfig().getDouble("frost-scythe.throw.return-speed", 1.55);
        this.direction = owner.getEyeLocation().getDirection().normalize().multiply(flightSpeed);

        this.display = (ItemDisplay) owner.getWorld().spawnEntity(owner.getEyeLocation(), EntityType.ITEM_DISPLAY);
        plugin.getVisualCleanupManager().track(display);

        ItemStack visualItem = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = visualItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(1);
            visualItem.setItemMeta(meta);
        }

        display.setItemStack(visualItem);
        display.setInterpolationDuration(MOTION_INTERPOLATION_TICKS);
        display.setTeleportDuration(MOTION_INTERPOLATION_TICKS);
        display.setInterpolationDelay(0);

        Transformation transformation = display.getTransformation();
        transformation.getLeftRotation().set(new Quaternionf());
        display.setTransformation(transformation);
    }

    @Override
    public void run() {
        if (!owner.isOnline() || !display.isValid()) {
            dropItem(display.isValid() ? display.getLocation() : null);
            return;
        }
        if (owner.isDead()) {
            dropItem(owner.getLocation());
            return;
        }

        Location current = display.getLocation();

        current.getWorld().spawnParticle(Particle.SNOWFLAKE, current, 6, 0.22, 0.22, 0.22, 0.03);

        if (returning) {
            handleReturn(current);
        } else {
            handleFlight(current);
        }
    }

    private void handleFlight(Location current) {
        double speed = direction.length();
        RayTraceResult result = current.getWorld().rayTrace(
                current,
                direction.clone().normalize(),
                speed + 0.2,
                FluidCollisionMode.NEVER,
                true,
                0.8,
                entity -> entity instanceof LivingEntity && !entity.equals(owner) && !entity.equals(display)
        );

        if (result != null) {
            if (result.getHitBlock() != null) {
                abilityManager.playImpactBurst(result.getHitPosition().toLocation(current.getWorld()), direction.clone().normalize());
                startReturn();
                return;
            }
            if (result.getHitEntity() instanceof LivingEntity victim) {
                if (isValidTarget(victim) && !hitOutward.contains(victim.getUniqueId())) {
                    Location sweepLoc = victim.getLocation().clone().add(0, 1.2, 0).subtract(direction.clone().normalize().multiply(1.2));
                    abilityManager.playSweepEffect(sweepLoc, direction);
                    abilityManager.playImpactBurst(victim.getLocation().clone().add(0, 1, 0), direction.clone().normalize());
                    abilityManager.applyFreeze(owner, victim, 3, 0);
                    CombatUtils.runSyntheticDamage(() -> victim.damage(plugin.getWeaponsConfig().getDouble("frost-scythe.throw.damage", 12.0), owner));
                    hitOutward.add(victim.getUniqueId());
                }
                startReturn();
                return;
            }
        }

        roll -= 0.6f;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(MOTION_INTERPOLATION_TICKS);
        display.setTeleportDuration(MOTION_INTERPOLATION_TICKS);
        Transformation transformation = display.getTransformation();

        transformation.getLeftRotation().set(new Quaternionf().rotateY(roll));
        display.setTransformation(transformation);

        Location nextLocation = current.clone().add(direction);
        display.teleport(nextLocation);

        if (current.distance(owner.getLocation()) > 40) {
            startReturn();
        }
    }

    private void handleReturn(Location current) {
        Location target = owner.getEyeLocation().subtract(0, 0.5, 0);
        Vector toOwner = target.toVector().subtract(current.toVector());
        double distance = toOwner.length();

        for (Entity entity : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
            if (entity instanceof LivingEntity victim && !victim.equals(owner)) {
                if (isValidTarget(victim) && !hitOnReturn.contains(victim.getUniqueId())) {
                    Location sweepLoc = victim.getLocation().clone().add(0, 1.2, 0).subtract(toOwner.clone().normalize().multiply(1.2));
                    abilityManager.playSweepEffect(sweepLoc, toOwner.clone().normalize());
                    abilityManager.playImpactBurst(victim.getLocation().clone().add(0, 1, 0), toOwner.clone().normalize());
                    abilityManager.applyFreeze(owner, victim, 3, 0);
                    CombatUtils.runSyntheticDamage(() -> victim.damage(plugin.getWeaponsConfig().getDouble("frost-scythe.throw.damage", 12.0), owner));
                    hitOnReturn.add(victim.getUniqueId());
                }
            }
        }

        if (distance <= 1.5) {
            if (owner.getInventory().firstEmpty() != -1) {
                ItemStack returnItem = item.clone();
                boolean isCooldown = abilityManager.isOnCooldown(owner, "ScytheThrow");
                returnItem.setType(isCooldown ? Material.NETHERITE_SWORD : Material.TRIDENT);

                FrostListener.setScytheModel(returnItem, false);

                owner.getInventory().addItem(returnItem);
                owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
                cleanup();
                return;
            }
            orbitTicks++;
            if (orbitTicks >= 300) {
                dropItem(current);
                return;
            }

            roll += 0.4f;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(MOTION_INTERPOLATION_TICKS);
            display.setTeleportDuration(MOTION_INTERPOLATION_TICKS);
            Transformation transformation = display.getTransformation();
            transformation.getLeftRotation().set(new Quaternionf().rotateY(roll));
            display.setTransformation(transformation);

            Vector lerp = target.clone().add(0, 1.5, 0).toVector().subtract(current.toVector()).multiply(0.15);
            display.teleport(current.clone().add(lerp));
            return;
        }

        roll += 0.6f;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(MOTION_INTERPOLATION_TICKS);
        display.setTeleportDuration(MOTION_INTERPOLATION_TICKS);
        Transformation transformation = display.getTransformation();
        transformation.getLeftRotation().set(new Quaternionf().rotateY(roll));
        display.setTransformation(transformation);

        current.add(toOwner.normalize().multiply(returnSpeed));
        display.teleport(current);
    }

    private boolean isValidTarget(LivingEntity victim) {
        if (victim instanceof Player targetPlayer) {
            return targetPlayer.getGameMode() != GameMode.SPECTATOR
                    && !plugin.getFriendManager().isFriend(owner.getUniqueId(), targetPlayer.getUniqueId());
        }
        return true;
    }

    private void startReturn() {
        returning = true;
    }

    private void dropItem() {
        dropItem(null);
    }

    private void dropItem(Location dropLoc) {
        Location loc = dropLoc != null ? dropLoc : (display.isValid() ? display.getLocation() : owner.getLocation());

        ItemStack drop = item.clone();
        drop.setType(Material.TRIDENT);
        FrostListener.setScytheModel(drop, false);

        loc.getWorld().dropItemNaturally(loc, drop);
        loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.6f);

        cleanup();
    }

    private void cleanup() {
        if (display.isValid()) {
            display.remove();
        }
        cancel();
    }
}

