package dev.whersss.altarLegendaryWH.weapons.nightpiercer.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.weapons.nightpiercer.managers.NightBossBarCooldown;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class NightAbilityListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, Long> biteCooldowns = new HashMap<>();
    private final Map<UUID, Long> transformationCooldowns = new HashMap<>();
    private final Set<UUID> preparedBites = new HashSet<>();

    public NightAbilityListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    private boolean isNightpiercer(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;
        return item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 5;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isNightpiercer(item)) return;

        event.setCancelled(true);
        if (plugin.isAboveLegendaryHeight(player)) return;

        if (player.isSneaking()) {
            if (isOnCooldown(player, transformationCooldowns)) return;
            executeTransformation(player);
            return;
        }

        if (isOnCooldown(player, biteCooldowns)) return;
        if (preparedBites.contains(player.getUniqueId())) return;

        preparedBites.add(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 1.0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isNightpiercer(player.getInventory().getItemInMainHand())) return;

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (preparedBites.contains(player.getUniqueId())) {
                executeBite(player, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!CombatUtils.isDirectMeleeHit(event)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!preparedBites.contains(player.getUniqueId())) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        if (victim instanceof Player targetPlayer) {
            if (targetPlayer.getGameMode() == GameMode.SPECTATOR) return;
            if (plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) return;
        }

        event.setCancelled(true);
        executeBite(player, victim);
    }

    private void executeBite(Player player, LivingEntity target) {
        preparedBites.remove(player.getUniqueId());

        int cooldown = plugin.getWeaponsConfig().getInt("nightpiercer.bite.cooldown", 45);
        double damage = plugin.getWeaponsConfig().getDouble("nightpiercer.bite.damage", 4.0);
        double range = 2.5;

        biteCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        new NightBossBarCooldown(plugin, player, plugin.tr("багровый укус", "ᴄʀɪᴍsᴏɴ ʙɪᴛᴇ"), cooldown).start();

        Location effectLocation;
        Vector viewDirection = player.getLocation().getDirection();

        if (target != null) {
            effectLocation = target.getLocation().add(0, 1.1, 0).subtract(viewDirection.multiply(0.2));
            applyBiteEffects(player, target, damage);
        } else {
            effectLocation = player.getEyeLocation().add(viewDirection.multiply(1.2));
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (!(entity instanceof LivingEntity victim) || entity.equals(player)) continue;

                if (victim instanceof Player targetPlayer) {
                    if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                    if (plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) continue;
                }

                Vector toTarget = victim.getLocation().toVector().subtract(player.getLocation().toVector());
                if (viewDirection.angle(toTarget) < 0.8) {
                    effectLocation = victim.getLocation().add(0, 1.1, 0).subtract(viewDirection.multiply(0.2));
                    applyBiteEffects(player, victim, damage);
                    break;
                }
            }
        }

        spawnMassiveCross(effectLocation, viewDirection);
        player.getWorld().playSound(effectLocation, Sound.ENTITY_PHANTOM_BITE, 1.5f, 1.0f);
    }

    private void applyBiteEffects(Player attacker, LivingEntity victim, double damage) {
        plugin.getNightHealthManager().applySteal(attacker, victim);
        CombatUtils.runSyntheticDamage(() -> victim.damage(damage, attacker));

        if (victim instanceof Player victimPlayer) {
            int durationSeconds = plugin.getWeaponsConfig().getInt("nightpiercer.bite.duration", 10);
            int totalTicks = durationSeconds * 20;

            BossBar victimBar = TextUtils.bossBar(
                    plugin.tr("§6§l!! §c§lУКУШЕН §6§l!!", "§6§l!! §c§lBITTEN §6§l!!"),
                    BarColor.RED,
                    BarStyle.SOLID
            );
            victimBar.addPlayer(victimPlayer);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!victimPlayer.isOnline() || ticks >= totalTicks) {
                        victimBar.removeAll();
                        cancel();
                        return;
                    }

                    double progress = 1.0 - ((double) ticks / totalTicks);
                    victimBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void spawnMassiveCross(Location center, Vector direction) {
        World world = center.getWorld();
        if (world == null) return;

        Vector side = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        Vector up = side.clone().crossProduct(direction).normalize();
        Vector diagonalOne = up.clone().add(side).normalize();
        Vector diagonalTwo = up.clone().subtract(side).normalize();

        double size = 2.5;
        for (double i = -size; i <= size; i += 0.2) {
            double bulge = Math.cos((i / size) * (Math.PI / 2)) * 2.0;
            Location pointOne = center.clone().add(diagonalOne.clone().multiply(i)).add(direction.clone().multiply(bulge));
            Location pointTwo = center.clone().add(diagonalTwo.clone().multiply(i)).add(direction.clone().multiply(bulge));
            world.spawnParticle(Particle.BLOCK, pointOne, 2, 0.05, 0.05, 0.05, 0.0, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
            world.spawnParticle(Particle.BLOCK, pointTwo, 2, 0.05, 0.05, 0.05, 0.0, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
        }
    }

    private void executeTransformation(Player player) {
        int cooldown = plugin.getWeaponsConfig().getInt("nightpiercer.transformation.cooldown", 20);
        int maxTicks = plugin.getWeaponsConfig().getInt("nightpiercer.transformation.duration", 2) * 20;
        double speed = plugin.getWeaponsConfig().getDouble("nightpiercer.transformation.speed", 1.0);
        double damage = plugin.getWeaponsConfig().getDouble("nightpiercer.transformation.damage", 4.0);

        transformationCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        new NightBossBarCooldown(plugin, player, plugin.tr("трансформация", "ᴛʀᴀɴsꜰᴏʀᴍᴀᴛɪᴏɴ"), cooldown).start();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.2f);
        spawnBlackDust(player.getLocation(), 140);

        List<Bat> bats = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Bat bat = player.getWorld().spawn(player.getLocation().add(0, 1, 0), Bat.class);
            bat.setInvulnerable(true);
            bat.setSilent(true);
            bat.setAI(true);
            bat.setPersistent(true);
            bat.setCollidable(false);
            bats.add(bat);
        }

        Set<UUID> hitEntities = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline() || !isNightpiercer(player.getInventory().getItemInMainHand())) {
                    bats.forEach(Entity::remove);
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.showPlayer(plugin, player);
                    }
                    spawnBlackDust(player.getLocation(), 140);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.2f);
                    cancel();
                    return;
                }

                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 5, 1, false, false, false));
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.canSee(player)) {
                        online.hidePlayer(plugin, player);
                    }
                }

                player.setVelocity(player.getLocation().getDirection().normalize().multiply(speed));

                if (ticks == 0 || ticks == 5 || ticks == 15 || ticks == 25 || ticks == 35) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_AMBIENT, 0.8f, 1.0f);
                }

                Location center = player.getLocation().add(0, 0.8, 0);
                for (Bat bat : bats) {
                    if (bat.getLocation().distance(center) > 0.1) {
                        Vector offset = new Vector(
                                ThreadLocalRandom.current().nextDouble(-1.5, 1.5),
                                ThreadLocalRandom.current().nextDouble(-0.5, 1.0),
                                ThreadLocalRandom.current().nextDouble(-1.5, 1.5)
                        );
                        bat.teleport(center.clone().add(offset));
                    }
                }

                for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (!(entity instanceof LivingEntity victim) || entity.equals(player)) continue;
                    if (bats.contains(victim)) continue;
                    if (hitEntities.contains(victim.getUniqueId())) continue;

                    if (victim instanceof Player targetPlayer) {
                        if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                        if (plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) continue;
                    }

                    hitEntities.add(victim.getUniqueId());
                    plugin.getCleanDamageManager().apply(victim, player, damage);
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    private void spawnBlackDust(Location location, int count) {
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.8f);
        location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, 1, 0), count, 0.7, 0.9, 0.7, blackDust);
    }

    private boolean isOnCooldown(Player player, Map<UUID, Long> cooldownMap) {
        return cooldownMap.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public void resetCooldowns(Player player) {
        UUID uuid = player.getUniqueId();
        biteCooldowns.remove(uuid);
        transformationCooldowns.remove(uuid);
        preparedBites.remove(uuid);
    }
}

