package dev.whersss.altarLegendaryWH.weapons.cutlass.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "SpellCheckingInspection", "unused"})
public class CutlassManager {

    private final AltarLegendaryWH plugin;
    private final NamespacedKey cutlassKey;

    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, BossBar> activeBars = new HashMap<>();
    private final Map<UUID, ParryData> activeParries = new HashMap<>();
    private final Map<UUID, BukkitTask> activeThousandCuts = new HashMap<>();

    public CutlassManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        this.cutlassKey = new NamespacedKey(plugin, "cutlass");
    }

    public boolean isCutlass(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(cutlassKey, PersistentDataType.BYTE);
    }

    public void setCutlassAttributes(ItemStack item, double damage, double attackSpeed) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        NamespacedKey damageKey = new NamespacedKey(plugin, "cutlass_damage");
        NamespacedKey speedKey = new NamespacedKey(plugin, "cutlass_speed");

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(damageKey, damage - 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                new AttributeModifier(speedKey, attackSpeed - 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
    }

    public boolean checkCooldown(Player player, String ability) {
        String key = player.getUniqueId() + ":" + ability;
        return cooldowns.getOrDefault(key, 0L) > System.currentTimeMillis();
    }

    public void applyCooldown(Player player, String ability, int seconds, String title) {
        String key = player.getUniqueId() + ":" + ability;
        cooldowns.put(key, System.currentTimeMillis() + (seconds * 1000L));

        String barKey = player.getUniqueId() + ":cd_" + ability;
        removeOldBar(barKey);

        BossBar bar = TextUtils.bossBar(yellowTitle(title), BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(player);
        activeBars.put(barKey, bar);

        new BukkitRunnable() {
            int ticksLeft = seconds * 20;

            @Override
            public void run() {
                if (ticksLeft-- <= 0 || !player.isOnline()) {
                    removeOldBar(barKey);
                    cancel();
                    return;
                }
                bar.setProgress(ticksLeft / (double) (seconds * 20));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public boolean hasActiveParry(UUID uuid) {
        return activeParries.containsKey(uuid);
    }

    public ParryData getParryData(UUID uuid) {
        return activeParries.get(uuid);
    }

    public void startParry(Player player) {
        int maxCharges = plugin.getWeaponsConfig().getInt("cutlass.parry.max-charges", 3);
        int durationSeconds = plugin.getWeaponsConfig().getInt("cutlass.parry.max-duration-seconds", 7);
        int slownessAmplifier = Math.max(0, plugin.getWeaponsConfig().getInt("cutlass.parry.slowness-amplifier", 0));
        long durationMillis = durationSeconds * 1000L;

        String barKey = player.getUniqueId() + ":parry_active";
        removeOldBar(barKey);

        BossBar bar = TextUtils.bossBar(parryTitle(maxCharges), BarColor.RED, BarStyle.SOLID);
        bar.addPlayer(player);
        activeBars.put(barKey, bar);

        ParryData data = new ParryData(maxCharges, System.currentTimeMillis(), durationMillis, bar);
        activeParries.put(player.getUniqueId(), data);

        if (slownessAmplifier > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    (durationSeconds * 20) + 10,
                    slownessAmplifier - 1,
                    false,
                    false,
                    true
            ));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.2f);

        Particle.DustOptions blueDust = new Particle.DustOptions(Color.fromRGB(0, 191, 255), 0.6f);

        new BukkitRunnable() {
            @Override
            public void run() {
                ParryData current = activeParries.get(player.getUniqueId());
                if (current == null || !player.isOnline()) {
                    removeOldBar(barKey);
                    cancel();
                    return;
                }

                Location waistLoc = player.getLocation().add(0, 0.9, 0);
                double radius = 0.85;
                int points = 24;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    waistLoc.add(x, 0, z);
                    player.getWorld().spawnParticle(Particle.DUST, waistLoc, 1, 0, 0, 0, 0, blueDust);
                    waistLoc.subtract(x, 0, z);
                }

                long elapsed = System.currentTimeMillis() - current.lastHitTime;
                double remaining = current.durationMillis - elapsed;
                current.bar.setProgress(Math.max(0.0, remaining / current.durationMillis));

                if (elapsed >= current.durationMillis) {
                    current.charges--;
                    current.lastHitTime = System.currentTimeMillis();

                    if (current.charges <= 0) {
                        endParry(player);
                        cancel();
                    } else {
                        current.bar.setTitle(parryTitle(current.charges));
                        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.2f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void endParry(Player player) {
        activeParries.remove(player.getUniqueId());
        removeOldBar(player.getUniqueId() + ":parry_active");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
        applyCooldown(
                player,
                "parry",
                plugin.getWeaponsConfig().getInt("cutlass.parry.cooldown", 30),
                plugin.tr("Пиратское отражение", "Pɪʀᴀᴛᴇ's ᴘᴀʀʀʏ")
        );
    }

    public void startThousandCuts(Player player) {
        int cooldown = plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.cooldown", 45);
        int durationTicks = plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.duration-ticks", 42);
        int slownessAmplifier = Math.max(0, plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.slowness-amplifier", 1));
        int slownessDurationTicks = plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.slowness-duration", 3) * 20;

        applyCooldown(player, "thousand_cuts", cooldown, plugin.tr("Сто царапин", "Hᴜɴᴅʀᴇᴅ ᴄᴜᴛs"));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) {
                    if (player.isOnline() && ticks >= durationTicks) {
                        Vector dash = player.getLocation().getDirection().normalize().multiply(1.5).setY(0.2);
                        player.setVelocity(dash);
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);
                    }
                    cancelThousandCuts(player);
                    return;
                }

                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (!isCutlass(itemInHand)) {
                    cancelThousandCuts(player);
                    return;
                }

                if (ticks <= 10) {
                    for (org.bukkit.entity.Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
                        if (entity instanceof LivingEntity target && !target.equals(player)) {
                            if (target instanceof Player targetPlayer
                                    && plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                continue;
                            }
                            target.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS,
                                    slownessDurationTicks,
                                    slownessAmplifier,
                                    false,
                                    false,
                                    true
                            ));
                        }
                    }
                }

                if (ticks % 3 == 0) {
                    player.swingMainHand();
                    playCutlassSwingFX(player);

                    RayTraceResult result = player.getWorld().rayTraceEntities(
                            player.getEyeLocation(),
                            player.getEyeLocation().getDirection(),
                            3.0,
                            entity -> entity instanceof LivingEntity
                                    && !entity.equals(player)
                                    && !(entity instanceof Player
                                    && plugin.getFriendManager().isFriend(player.getUniqueId(), entity.getUniqueId()))
                    );

                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        dealThousandCutsHit(player, target);
                        Vector knockback = player.getLocation().getDirection().setY(0).normalize().multiply(0.2).setY(0.1);
                        target.setVelocity(target.getVelocity().add(knockback));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeThousandCuts.put(player.getUniqueId(), task);
    }

    public void cancelThousandCuts(Player player) {
        BukkitTask task = activeThousandCuts.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public boolean isDoingThousandCuts(Player player) {
        return activeThousandCuts.containsKey(player.getUniqueId());
    }

    public void cleanupAll() {
        for (BossBar bar : activeBars.values()) {
            bar.removeAll();
        }
        activeBars.clear();

        for (BukkitTask task : activeThousandCuts.values()) {
            task.cancel();
        }
        activeThousandCuts.clear();
        activeParries.clear();
    }

    public void playCutlassSwingFX(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.1f, 1.5f);

        Location eyeLoc = player.getEyeLocation();
        Vector look = eyeLoc.getDirection().normalize();
        Vector right = look.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = look.clone().crossProduct(right).normalize();

        double tiltRad = Math.toRadians((Math.random() * 60 + 15) * (Math.random() > 0.5 ? 1 : -1));
        Vector diagonalAxis = right.clone().multiply(Math.cos(tiltRad)).add(up.clone().multiply(Math.sin(tiltRad)));

        double radius = 2.0;
        double arcHalfWidth = 65.0;
        int particleCount = 35;

        for (int i = 0; i < particleCount; i++) {
            double angle = Math.toRadians(-arcHalfWidth + (i * ((arcHalfWidth * 2) / (particleCount - 1))));
            Vector offset = look.clone().multiply(Math.cos(angle))
                    .add(diagonalAxis.clone().multiply(Math.sin(angle)))
                    .multiply(radius);

            Location particleLoc = eyeLoc.clone().add(offset);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.02, 0.02, 0.02, 0);
            player.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0.03, 0.03, 0.03, 0);
        }
    }

    private void removeOldBar(String key) {
        BossBar old = activeBars.remove(key);
        if (old != null) {
            old.removeAll();
        }
    }

    private void dealThousandCutsHit(Player player, LivingEntity target) {
        double damage = plugin.getWeaponsConfig().getDouble("cutlass.thousand-cuts.hit-damage", 3.5);
        target.setNoDamageTicks(0);
        plugin.getCleanDamageManager().apply(target, player, damage);

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.hasItemMeta()) {
            int fireAspect = weapon.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.FIRE_ASPECT);
            if (fireAspect > 0) {
                target.setFireTicks(Math.max(target.getFireTicks(), (fireAspect * 4) * 20));
            }
        }
    }

    public void resetCooldowns(Player player) {
        String prefix = player.getUniqueId() + ":";
        cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
        activeBars.entrySet().removeIf(entry -> {
            if (!entry.getKey().startsWith(prefix)) {
                return false;
            }
            entry.getValue().removeAll();
            return true;
        });
        activeParries.remove(player.getUniqueId());
        cancelThousandCuts(player);
    }

    public String parryTitle(int charges) {
        return "§6§l!! §c§l" + plugin.tr("ОТРАЖЕНИЕ", "ᴘᴀʀʀʏ") + " §6x" + charges + " §6§l!!";
    }

    public String yellowTitle(String title) {
        return ChatColor.YELLOW + ChatColor.stripColor(title);
    }

    public static class ParryData {
        public int charges;
        public long lastHitTime;
        public final long durationMillis;
        public final BossBar bar;

        public ParryData(int charges, long lastHitTime, long durationMillis, BossBar bar) {
            this.charges = charges;
            this.lastHitTime = lastHitTime;
            this.durationMillis = durationMillis;
            this.bar = bar;
        }
    }
}