package dev.whersss.altarLegendaryWH.weapons.bloodlust.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.ParticleUtils;
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
    private final Random random = new Random();

    private final BlockData bloodBlockData = Bukkit.createBlockData(Material.REDSTONE_BLOCK);

    public BloodAbilityManager(AltarLegendaryWH plugin, BloodBossBarManager bossBarManager) {
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
    }

    public void updateHandCheck(Player p) {
        if (isInBloodTrail(p)) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (!BloodLustListener.isBloodLust(item)) cancelBloodTrail(p, true);
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
                plugin.getWeaponsConfig().getInt("bloodlust.infection.cooldown")
        );

        int maxHits = plugin.getWeaponsConfig().getInt("bloodlust.infection.hits", 6);
        double bleedDamage = plugin.getWeaponsConfig().getDouble("bloodlust.infection.damage", 2.0);
        String barId = "BloodInfection";

        if (victim instanceof Player pVictim) {
            bossBarManager.setDebuffBar(pVictim, barId, plugin.tr("§4§l! §c§lᴋроʙоᴛᴇчᴇниᴇ §4§l!", "§4§l! §c§lʙʟᴇᴇᴅɪɴɢ §4§l!"), maxHits);
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

                CombatUtils.runSyntheticDamage(() -> victim.damage(bleedDamage, attacker));
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

                // Envelop victim in swirling bloody aura of red dust and redstone block shards
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI / 20) * i;
                    double yOff = 0.2 + (i / 20.0) * 1.6;
                    Location swirl = victim.getLocation().add(Math.cos(angle) * 0.65, yOff, Math.sin(angle) * 0.65);
                    victim.getWorld().spawnParticle(Particle.DUST, swirl, 2, 0.05, 0.05, 0.05, 0.0, redDust);
                }
                victim.getWorld().spawnParticle(Particle.DUST, effectLoc, 35, 0.35, 0.45, 0.35, 0.0, redDust);

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

                Location targetLoc = attacker.getLocation().add(0, 1.2, 0);
                Vector dir = targetLoc.toVector().subtract(current.toVector());

                if (dir.lengthSquared() < 1.0) {
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1.2f);
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 2, false, false, true));
                    this.cancel();
                    return;
                }

                Location prev = current.clone();
                dir.normalize().multiply(0.7);
                current.add(dir);

                // Static non-falling BLOCK_CRUMBLE particles forming a solid line to the owner
                Vector stepVec = current.toVector().subtract(prev.toVector());
                double dist = stepVec.length();
                int steps = Math.max(1, (int) Math.ceil(dist / 0.22));
                Vector subStep = stepVec.clone().multiply(1.0 / steps);
                for (int s = 0; s <= steps; s++) {
                    Location pt = prev.clone().add(subStep.clone().multiply(s));
                    pt.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pt, 1, 0.0, 0.0, 0.0, 0, bloodBlockData);
                }
                life++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void castBloodTrail(Player p) {
        if (bossBarManager.isOnCooldown(p, "BloodTrail")) return;

        bossBarManager.setCooldown(
                p,
                "BloodTrail",
                plugin.tr("§4§lᴋроʙᴀʙый ᴄлᴇд", "§4§lʙʟᴏᴏᴅ ᴛʀᴀɪʟ"),
                plugin.getWeaponsConfig().getInt("bloodlust.blood-trail.cooldown")
        );

        int maxTicks = plugin.getWeaponsConfig().getInt("bloodlust.blood-trail.duration", 20) * 20;

        activePuddles.add(p.getUniqueId());

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
        p.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        PotionEffect speed = p.getPotionEffect(PotionEffectType.SPEED);
        if (speed != null && speed.getAmplifier() == 3 && speed.getDuration() <= 40) {
            p.removePotionEffect(PotionEffectType.SPEED);
        }

        if (forceJump) {
            p.setVelocity(p.getVelocity().setY(0.55));
            p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation().add(0, 0.2, 0), 20, 0.3, 0.1, 0.3, 0, bloodBlockData);
        }
    }

    public boolean isInBloodTrail(Player p) {
        return activePuddles.contains(p.getUniqueId());
    }

    public void playKillVisual(Player attacker) {
        playKillVisual(attacker, null);
    }

    public void playKillVisual(Player attacker, Location victimLoc) {
        if (attacker == null || !attacker.isOnline()) return;
        attacker.getWorld().playSound(attacker.getLocation(), "bloodlust.kill", 1.0f, 1.0f);

        Location origin = (victimLoc != null) ? victimLoc.clone().add(0, 1.0, 0)
                : attacker.getLocation().add(attacker.getLocation().getDirection().multiply(2.5)).add(0, 1.0, 0);

        final Particle.DustOptions crimsonDust = new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.5f);
        final Particle.DustOptions tailDust = new Particle.DustOptions(Color.fromRGB(160, 10, 10), 0.9f);

        int numComets = 6;
        List<Location> cometLocs = new ArrayList<>();
        List<Double> phases = new ArrayList<>();
        List<Double> spiralSpeeds = new ArrayList<>();

        for (int i = 0; i < numComets; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.8;
            double oy = (random.nextDouble() - 0.2) * 0.8;
            double oz = (random.nextDouble() - 0.5) * 0.8;
            cometLocs.add(origin.clone().add(ox, oy, oz));
            phases.add(random.nextDouble() * Math.PI * 2);
            spiralSpeeds.add(0.35 + random.nextDouble() * 0.3);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!attacker.isOnline() || ticks > 60) {
                    cancel();
                    return;
                }

                Location target = attacker.getLocation().add(0, 1.2, 0);
                boolean allReached = true;

                for (int i = 0; i < cometLocs.size(); i++) {
                    Location comet = cometLocs.get(i);
                    Vector toTarget = target.toVector().subtract(comet.toVector());
                    double distSq = toTarget.lengthSquared();

                    if (distSq > 0.8) {
                        allReached = false;
                        double speed = Math.min(0.85, 0.35 + (ticks * 0.035));
                        Vector step = toTarget.normalize().multiply(speed);

                        // Animated corkscrew / swirl around trajectory line
                        double phase = phases.get(i) + ticks * spiralSpeeds.get(i);
                        Vector right = step.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                        if (right.lengthSquared() < 0.001) right = new Vector(1, 0, 0);
                        Vector up = right.clone().crossProduct(step).normalize();

                        double swirlR = Math.max(0.08, 0.45 * Math.sin(Math.min(1.0, (double) ticks / 20.0) * Math.PI));
                        Vector swirlOffset = right.multiply(Math.cos(phase) * swirlR).add(up.multiply(Math.sin(phase) * swirlR));

                        comet.add(step).add(swirlOffset.multiply(0.35));

                        // Comet head
                        comet.getWorld().spawnParticle(Particle.DUST, comet, 3, 0.05, 0.05, 0.05, 0.0, crimsonDust);
                        comet.getWorld().spawnParticle(Particle.BLOCK, comet, 1, 0.02, 0.02, 0.02, 0.0, bloodBlockData);

                        // Comet tail
                        Location tailLoc = comet.clone().subtract(step.clone().multiply(0.4));
                        comet.getWorld().spawnParticle(Particle.DUST, tailLoc, 2, 0.04, 0.04, 0.04, 0.0, tailDust);
                    }
                }

                if (allReached) {
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.2f);
                    attacker.getWorld().spawnParticle(Particle.DUST, target, 35, 0.4, 0.4, 0.4, 0.0, crimsonDust);
                    cancel();
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
                plugin.tr("§4§lᴋроʙᴀʙый ᴋрюᴋ", "§4§lʙʟᴏᴏᴅ ʜᴏᴏᴋ"),
                plugin.getWeaponsConfig().getInt("bloodlust.blood-hook.cooldown")
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
        plugin.getVisualCleanupManager().track(display);

        new BukkitRunnable() {
            double distanceTraveled = 0;
            float currentAngle = 0f;
            final double maxDist = plugin.getWeaponsConfig().getDouble("bloodlust.blood-hook.range");
            final double speed = plugin.getWeaponsConfig().getDouble("bloodlust.blood-hook.speed", 1.3);
            final float flightYaw = startLoc.getYaw();

            @Override
            public void run() {
                if (!p.isOnline() || !display.isValid()) {
                    if (display.isValid()) {
                        plugin.getVisualCleanupManager().untrack(display);
                        display.remove();
                    }
                    this.cancel();
                    return;
                }

                Location nextLoc = display.getLocation().add(direction.clone().multiply(speed));
                nextLoc.setYaw(flightYaw);
                nextLoc.setPitch(0);

                if (distanceTraveled >= maxDist || nextLoc.getBlock().getType().isSolid()) {
                    p.getWorld().playSound(nextLoc, Sound.BLOCK_NETHER_GOLD_ORE_BREAK, 1f, 0.5f);
                    p.getWorld().playSound(nextLoc, Sound.BLOCK_CHAIN_BREAK, 1f, 1.2f);
                    playImpactBurst(nextLoc);
                    plugin.getVisualCleanupManager().untrack(display);
                    display.remove();
                    this.cancel();
                    return;
                }

                for (Entity e : nextLoc.getWorld().getNearbyEntities(nextLoc, 1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity victim && !victim.equals(p)) {
                        if (victim instanceof Player targetPlayer) {
                            if (targetPlayer.getGameMode() == GameMode.SPECTATOR) continue;
                            if (plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) continue;
                        }

                        p.getWorld().playSound(nextLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.5f);
                        plugin.getVisualCleanupManager().untrack(display);
                        display.remove();
                        playImpactBurst(victim.getLocation().add(0, 1.0, 0));

                        double damage = plugin.getWeaponsConfig().getDouble("bloodlust.blood-hook.damage", 8.0);
                        plugin.getCleanDamageManager().apply(victim, p, damage);

                        returnHook(p, nextLoc, victim);
                        this.cancel();
                        return;
                    }
                }

                display.setInterpolationDelay(0);
                display.setInterpolationDuration(2);

                Vector3f rotAxis = new Vector3f(1f, 0f, 0f);
                Quaternionf q = new Quaternionf().rotationAxis(currentAngle, rotAxis.x, rotAxis.y, rotAxis.z);

                Transformation t = new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f).rotate(q),
                        q,
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new Quaternionf()
                );

                display.setTransformation(t);
                display.teleport(nextLoc);

                Location tailStart = p.getLocation().add(0, 1.0, 0);
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
        Vector step = bridge.normalize().multiply(0.35);
        Location pLoc = start.clone();

        for (double i = 0; i < dist; i += 0.35) {
            pLoc.add(step);
            pLoc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pLoc, 1, 0.0, 0.0, 0.0, 0, bloodBlockData);
        }
    }

    public void playImpactBurst(Location center) {
        Location baseLoc = center.clone().add(0, 0.2, 0);
        Particle.DustOptions crimson = new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.5f);
        center.getWorld().spawnParticle(Particle.DUST, baseLoc, 25, 0.4, 0.4, 0.4, 0.05, crimson);
        ParticleUtils.spawnBlockDispersion(plugin, baseLoc, bloodBlockData, 4.8);
    }
    public void cleanup() {
        for (UUID uuid : new ArrayList<>(activePuddles)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) cancelBloodTrail(p, false);
        }
        activePuddles.clear();
    }
}

