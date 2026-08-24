package dev.whersss.altarLegendaryWH.weapons.bloodlust.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.weapons.bloodlust.listeners.BloodLustListener;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BloodAbilityManager {

    private final AltarLegendaryWH plugin;
    private final BloodBossBarManager bossBarManager;
    private final Set<UUID> activePuddles = new HashSet<>();
    private final Map<UUID, Collection<PotionEffect>> savedEffects = new HashMap<>();
    private final Random random = new Random();

    private final BlockData bloodBlockData = Bukkit.createBlockData(Material.REDSTONE_BLOCK);

    public BloodAbilityManager(AltarLegendaryWH plugin, BloodBossBarManager bossBarManager) {
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
    }

    public void updateHandCheck(Player p) {
        if (isInBloodTrail(p)) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (!BloodLustListener.isBloodLust(item)) cancelBloodTrail(p, false);
        }
    }

    public void launchTrackerProjectile(Player source, Player target) {
        if (source == null || target == null) return;
        final Location current = source.getLocation().add(0, 1.5, 0);
        final Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.2f);

        new BukkitRunnable() {
            int life = 0;

            @Override
            public void run() {
                if (!target.isOnline() || life > 60 || !source.isOnline() || !target.getWorld().equals(current.getWorld())) {
                    this.cancel();
                    return;
                }

                Location targetLoc = target.getLocation().add(0, 1.0, 0);
                Vector dir = targetLoc.toVector().subtract(current.toVector());

                if (dir.length() < 0.6) {
                    source.spawnParticle(Particle.ENTITY_EFFECT, current, 1, 0.0, 0.0, 0.0, 0.0, Color.RED);
                    this.cancel();
                    return;
                }

                dir.normalize().multiply(0.5);
                current.add(dir);

                source.spawnParticle(Particle.ELECTRIC_SPARK, current, 1, 0.0, 0.0, 0.0, 0.0);
                source.spawnParticle(Particle.DUST, current, 1, 0.0, 0.0, 0.0, 0.0, redDust);

                life++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void applyInfection(Player attacker, Entity target) {
        if (!(target instanceof LivingEntity victim)) return;

        if (victim instanceof Player targetPlayer) {
            if (plugin.getFriendManager().isFriend(attacker.getUniqueId(), targetPlayer.getUniqueId())) return;
        }

        if (Math.random() > 0.15) return;
        if (bossBarManager.isOnCooldown(attacker, "Infection")) return;

        bossBarManager.setCooldown(
                attacker,
                "Infection",
                plugin.tr("§4§lИнфекция", "§4§lɪɴꜰᴇᴄᴛɪᴏɴ"),
                plugin.getConfig().getInt("bloodlust.infection.cooldown")
        );

        int maxHits = plugin.getConfig().getInt("bloodlust.infection.hits", 6);
        double bleedDamage = plugin.getConfig().getDouble("bloodlust.infection.damage", 2.0);
        String barId = "BloodInfection";

        if (victim instanceof Player pVictim) {
            bossBarManager.setDebuffBar(pVictim, barId, plugin.tr("§4§l! §c§lКРОВОТЕЧЕНИЕ §4§l!", "§4§l! §c§lBLEEDING §4§l!"), maxHits);
        }

        final Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.5f);

        new BukkitRunnable() {
            int currentHit = 0;

            @Override
            public void run() {
                if (currentHit >= maxHits || target.isDead() || !target.isValid()) {
                    if (victim instanceof Player pVictim) {
                        bossBarManager.removeDebuffBar(pVictim, barId);
                    }
                    this.cancel();
                    return;
                }

                victim.damage(bleedDamage, attacker);
                victim.getWorld().playSound(victim.getLocation(), "bloodlust.hit", 1f, 1f);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);

                if (victim.isDead()) {
                    playKillVisual(attacker);
                    if (victim instanceof Player pVictim) {
                        bossBarManager.removeDebuffBar(pVictim, barId);
                    }
                    this.cancel();
                    return;
                }

                Location effectLoc = victim.getLocation().add(0, 1, 0);
                victim.getWorld().spawnParticle(Particle.BLOCK, effectLoc, 45, 0.4, 0.5, 0.4, 0, bloodBlockData);
                victim.getWorld().spawnParticle(Particle.DUST, effectLoc, 30, 0.4, 0.5, 0.4, 0, redDust);

                launchBloodSteal(effectLoc, attacker);

                currentHit++;
                if (victim instanceof Player pVictim && !victim.isDead()) {
                    double progress = (double) (maxHits - currentHit) / maxHits;
                    bossBarManager.updateProgress(pVictim, barId, progress);
                    if (currentHit >= maxHits) {
                        bossBarManager.removeDebuffBar(pVictim, barId);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void launchBloodSteal(Location startLoc, Player attacker) {
        new BukkitRunnable() {
            Location current = startLoc.clone();
            int life = 0;

            @Override
            public void run() {
                if (!attacker.isOnline() || attacker.isDead() || life > 60) {
                    this.cancel();
                    return;
                }

                Location targetLoc = attacker.getLocation().add(0, 1.0, 0);
                Vector dir = targetLoc.toVector().subtract(current.toVector());

                if (dir.lengthSquared() < 1.0) {
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1.2f);
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 2, false, false, true));
                    this.cancel();
                    return;
                }

                dir.normalize().multiply(0.6);
                current.add(dir);
                current.getWorld().spawnParticle(Particle.BLOCK, current, 1, 0.0, 0.0, 0.0, 0, bloodBlockData);
                life++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void castBloodTrail(Player p) {
        if (bossBarManager.isOnCooldown(p, "BloodTrail")) return;

        bossBarManager.setCooldown(
                p,
                "BloodTrail",
                plugin.tr("§4§lКровавый след", "§4§lʙʟᴏᴏᴅ ᴛʀᴀɪʟ"),
                plugin.getConfig().getInt("bloodlust.blood-trail.cooldown")
        );

        int maxTicks = plugin.getConfig().getInt("bloodlust.blood-trail.duration", 20) * 20;

        activePuddles.add(p.getUniqueId());
        savedEffects.put(p.getUniqueId(), new ArrayList<>(p.getActivePotionEffects()));

        p.getWorld().playSound(p.getLocation(), "bloodlust.dive", 1f, 1f);
        p.getAttribute(Attribute.SCALE).setBaseValue(0.3);
        p.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(1.0);

        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(p.getLocation());
        border.setSize(10000);
        border.setWarningDistance(10000);
        p.setWorldBorder(border);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(p)) {
                online.hidePlayer(plugin, p);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!activePuddles.contains(p.getUniqueId()) || !p.isOnline() || ticks >= maxTicks) {
                    if (activePuddles.contains(p.getUniqueId())) cancelBloodTrail(p, true);
                    this.cancel();
                    return;
                }

                if (ticks % 20 == 0) {
                    p.sendActionBar(TextUtils.legacy(plugin.tr("§cНажмите §e[ПРЫЖОК] §cдля выхода", "§cPress §e[JUMP] §cto exit")));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30, 0, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 3, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30, 3, false, false));
                }

                Location loc = p.getLocation();
                for (int i = 0; i < 4; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random() * 0.8;
                    Location partLoc = loc.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                    loc.getWorld().spawnParticle(Particle.BLOCK, partLoc, 1, 0, 0, 0, 0, bloodBlockData);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void cancelBloodTrail(Player p, boolean forceJump) {
        if (!activePuddles.contains(p.getUniqueId())) return;
        activePuddles.remove(p.getUniqueId());

        p.getWorld().playSound(p.getLocation(), "bloodlust.resurface", 1f, 1f);
        p.getAttribute(Attribute.SCALE).setBaseValue(1.0);
        p.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(0.6);
        p.setWorldBorder(null);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, p);
        }

        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.removePotionEffect(PotionEffectType.SPEED);
        p.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        if (savedEffects.containsKey(p.getUniqueId())) {
            savedEffects.remove(p.getUniqueId()).forEach(p::addPotionEffect);
        }
        if (forceJump) p.setVelocity(p.getVelocity().setY(0.5));
    }

    public boolean isInBloodTrail(Player p) {
        return activePuddles.contains(p.getUniqueId());
    }

    public void playKillVisual(Player attacker) {
        attacker.getWorld().playSound(attacker.getLocation(), "bloodlust.kill", 1.0f, 1.0f);

        List<Location> startPoints = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 3.0;
            double offsetZ = (random.nextDouble() - 0.5) * 3.0;
            double offsetY = 2.5 + random.nextDouble() * 1.5;
            startPoints.add(attacker.getLocation().add(offsetX, offsetY, offsetZ));
        }

        final Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.25f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!attacker.isOnline() || ticks > 100) {
                    this.cancel();
                    return;
                }

                Location target = attacker.getLocation().add(0, 1.2, 0);
                boolean allReached = true;

                for (int i = 0; i < startPoints.size(); i++) {
                    Location current = startPoints.get(i);
                    Vector direction = target.toVector().subtract(current.toVector());

                    if (direction.lengthSquared() > 0.6) {
                        allReached = false;
                        direction.normalize().multiply(0.3);
                        current.add(direction);
                        current.getWorld().spawnParticle(Particle.DUST, current, 8, 0.08, 0.08, 0.08, 0.0, redDust);
                    }
                }

                if (allReached) {
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.7f, 1.3f);
                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void castBloodHook(Player p) {
        if (bossBarManager.isOnCooldown(p, "BloodHook")) return;

        bossBarManager.setCooldown(
                p,
                "BloodHook",
                plugin.tr("§4§lКровавый крюк", "§4§lʙʟᴏᴏᴅ ʜᴏᴏᴋ"),
                plugin.getConfig().getInt("bloodlust.blood-hook.cooldown")
        );

        p.swingMainHand();
        p.getWorld().playSound(p.getLocation(), "bloodlust.resurface", 1.0f, 1.5f);

        Location startLoc = p.getLocation().add(0, 1.0, 0);
        Vector direction = p.getEyeLocation().getDirection().normalize();
        startLoc.setDirection(direction);

        BlockDisplay display = p.getWorld().spawn(startLoc, BlockDisplay.class, ent -> {
            ent.setBlock(bloodBlockData);
            ent.setInterpolationDuration(1);
            ent.setTeleportDuration(1);
        });

        new BukkitRunnable() {
            double distanceTraveled = 0;
            float currentAngle = 0f;
            boolean isMissed = false;
            float scale = 1.0f;
            double tailDistance = 0;

            final double maxDist = plugin.getConfig().getDouble("bloodlust.blood-hook.range");
            final double speed = plugin.getConfig().getDouble("bloodlust.blood-hook.speed", 1.3);
            final float flightYaw = startLoc.getYaw();

            @Override
            public void run() {
                if (!p.isOnline() || (!display.isValid() && !isMissed)) {
                    if (display.isValid()) display.remove();
                    this.cancel();
                    return;
                }

                Location nextLoc = display.getLocation().add(direction.clone().multiply(speed));
                nextLoc.setYaw(flightYaw);
                nextLoc.setPitch(0);

                if (!isMissed) {
                    if (distanceTraveled >= maxDist || nextLoc.getBlock().getType().isSolid()) {
                        isMissed = true;
                        p.getWorld().playSound(nextLoc, Sound.BLOCK_NETHER_GOLD_ORE_BREAK, 1f, 0.5f);
                        playImpactBurst(nextLoc);
                    } else {
                        for (Entity e : nextLoc.getWorld().getNearbyEntities(nextLoc, 1.2, 1.2, 1.2)) {
                            if (e instanceof LivingEntity victim && !victim.equals(p)) {
                                if (victim instanceof Player targetPlayer) {
                                    if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                                    if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) continue;
                                }

                                p.getWorld().playSound(nextLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.5f);
                                display.remove();
                                playImpactBurst(victim.getLocation().add(0, 1.0, 0));

                                double damage = plugin.getConfig().getDouble("bloodlust.blood-hook.damage", 8.0);
                                plugin.getCleanDamageManager().apply(victim, p, damage);

                                returnHook(p, nextLoc, victim);
                                this.cancel();
                                return;
                            }
                        }
                    }
                }

                if (isMissed) {
                    scale -= 0.08f;
                    tailDistance += speed * 0.8;

                    if (scale <= 0) {
                        display.remove();
                        this.cancel();
                        return;
                    }
                }

                display.setInterpolationDelay(0);
                display.setInterpolationDuration(2);

                Vector3f rotAxis = new Vector3f(1f, 0f, 0f);
                Quaternionf q = new Quaternionf().rotationAxis(currentAngle, rotAxis.x, rotAxis.y, rotAxis.z);

                Transformation t = new Transformation(
                        new Vector3f(-0.5f * scale, -0.5f * scale, -0.5f * scale).rotate(q),
                        q,
                        new Vector3f(scale, scale, scale),
                        new Quaternionf()
                );

                display.setTransformation(t);
                display.teleport(nextLoc);

                Location tailStart = p.getLocation().add(0, 1.0, 0);
                if (isMissed) {
                    Vector tailToHead = display.getLocation().toVector().subtract(tailStart.toVector());
                    double maxTailLen = tailToHead.length();
                    if (tailDistance > maxTailLen) tailDistance = maxTailLen;
                    if (maxTailLen > 0) {
                        tailStart.add(tailToHead.normalize().multiply(tailDistance));
                    }
                }

                renderChain(tailStart, display.getLocation());

                distanceTraveled += speed;
                currentAngle += 0.5f;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void returnHook(Player p, Location hitLoc, LivingEntity caught) {
        new BukkitRunnable() {
            Location currentLoc = hitLoc.clone();
            LivingEntity target = caught;

            @Override
            public void run() {
                if (!p.isOnline() || target == null || target.isDead()) {
                    this.cancel();
                    return;
                }

                Location targetLoc = p.getLocation().add(0, 1.0, 0);
                Vector toPlayer = targetLoc.toVector().subtract(currentLoc.toVector());

                if (toPlayer.length() < 1.5) {
                    this.cancel();
                    return;
                }

                Vector dir = toPlayer.normalize();
                currentLoc.add(dir.clone().multiply(0.8));
                renderChain(p.getLocation().add(0, 1.0, 0), currentLoc);

                for (int i = 0; i < 3; i++) {
                    p.getWorld().playSound(p.getLocation(), "bloodlust.resurface", 1.0f, 1.2f + (random.nextFloat() * 0.5f));
                }

                if (target.getLocation().getBlock().getType() == Material.COBWEB) {
                    target = null;
                } else {
                    target.teleport(currentLoc.clone().subtract(0, 0.5, 0));
                    target.setFallDistance(0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void renderChain(Location start, Location end) {
        Vector bridge = end.toVector().subtract(start.toVector());
        double dist = bridge.length();
        Vector step = bridge.normalize().multiply(0.4);
        Location pLoc = start.clone();

        for (double i = 0; i < dist; i += 0.4) {
            pLoc.add(step);
            pLoc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pLoc, 1, 0.0, 0.0, 0.0, 0, bloodBlockData);
        }
    }

    public void playImpactBurst(Location center) {
        Location baseLoc = center.clone().add(0, 0.55, 0);

        for (int i = 0; i < 45; i++) {
            double u = ThreadLocalRandom.current().nextDouble();
            double v = ThreadLocalRandom.current().nextDouble();
            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);
            double speed = ThreadLocalRandom.current().nextDouble(0.15, 0.4);
            double dx = Math.sin(phi) * Math.cos(theta) * speed;
            double dy = Math.abs(Math.cos(phi)) * speed + 0.1;
            double dz = Math.sin(phi) * Math.sin(theta) * speed;

            baseLoc.getWorld().spawnParticle(Particle.BLOCK, baseLoc, 0, dx, dy, dz, 1.0, bloodBlockData);
        }
    }
    public void cleanup() {
        for (UUID uuid : new ArrayList<>(activePuddles)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) cancelBloodTrail(p, false);
        }
        activePuddles.clear();
        savedEffects.clear();
    }
}