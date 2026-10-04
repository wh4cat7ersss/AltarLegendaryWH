package dev.whersss.altarLegendaryWH.weapons.crazyslots.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrazySlotsManager {

    private final AltarLegendaryWH plugin;
    private final NamespacedKey uniqueKey;
    private final NamespacedKey crazySlotsKey;

    private final Map<UUID, ItemStack> activeTransformations = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToInstance = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> instanceToPlayer = new ConcurrentHashMap<>();

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> inFlightInstances = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();

    public CrazySlotsManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        this.uniqueKey = new NamespacedKey(plugin, "crazyslots_unique_id");
        this.crazySlotsKey = new NamespacedKey(plugin, "crazyslots");
    }

    public NamespacedKey getUniqueKey() {
        return uniqueKey;
    }

    public NamespacedKey getCrazySlotsKey() {
        return crazySlotsKey;
    }

    public void markInFlight(UUID instanceId) {
        if (instanceId != null) inFlightInstances.add(instanceId);
    }

    public void unmarkInFlight(UUID instanceId) {
        if (instanceId != null) inFlightInstances.remove(instanceId);
    }

    public boolean isInFlight(UUID instanceId) {
        return instanceId != null && inFlightInstances.contains(instanceId);
    }

    public boolean isCrazySlots(ItemStack item) {
        return WeaponFactory.isAltarWeapon(item, "crazyslots");
    }

    public boolean isTransformedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(uniqueKey, PersistentDataType.STRING);
    }

    public UUID getTransformedInstanceId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String idStr = meta.getPersistentDataContainer().get(uniqueKey, PersistentDataType.STRING);
        if (idStr == null) return null;
        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public UUID getInstanceIdForPlayer(UUID playerUUID) {
        return playerToInstance.get(playerUUID);
    }

    public boolean hasActiveTransformation(UUID instanceId) {
        return instanceId != null && activeTransformations.containsKey(instanceId);
    }

    public ItemStack getOriginalCrazySlots(UUID instanceId) {
        return instanceId != null ? activeTransformations.get(instanceId) : null;
    }

    public ItemStack getCleanCrazySlots(UUID instanceId) {
        ItemStack original = instanceId != null ? activeTransformations.get(instanceId) : null;
        if (original == null) {
            original = WeaponFactory.getCrazySlots();
        }
        ItemStack clean = original.clone();
        ItemMeta meta = clean.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(uniqueKey);
            clean.setItemMeta(meta);
        }
        return clean;
    }

    public boolean hasTransformedItem(Player player, UUID instanceId) {
        if (isInFlight(instanceId)) return true;
        if (player == null || !player.isOnline()) return false;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (hasTargetId(inv.getItem(i), instanceId)) return true;
        }
        if (hasTargetId(player.getInventory().getItemInOffHand(), instanceId)) return true;
        if (hasTargetId(player.getItemOnCursor(), instanceId)) return true;

        if (player.getOpenInventory() != null) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top != null && (top.getHolder() instanceof Player || top.getType() == org.bukkit.event.inventory.InventoryType.CRAFTING)) {
                for (int i = 0; i < top.getSize(); i++) {
                    if (hasTargetId(top.getItem(i), instanceId)) return true;
                }
            }
        }

        return false;
    }

    public void endTransformationAndApplyCooldown(Player player, UUID instanceId) {
        UUID pUuid = null;
        if (instanceId != null) {
            inFlightInstances.remove(instanceId);
            activeTransformations.remove(instanceId);
            BukkitTask task = activeTasks.remove(instanceId);
            if (task != null) task.cancel();
            pUuid = instanceToPlayer.remove(instanceId);
            if (pUuid != null) {
                playerToInstance.remove(pUuid);
            }
        }
        if (player == null && pUuid != null) {
            player = Bukkit.getPlayer(pUuid);
        }
        if (player != null && player.isOnline()) {
            playerToInstance.remove(player.getUniqueId());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
            player.updateInventory();
            int cooldownSec = plugin.getWeaponsConfig().getInt("crazy-slots.cooldown", 60);
            setCooldown(player, cooldownSec);
        }
    }

    public boolean isOnCooldown(Player player) {
        Long exp = cooldowns.get(player.getUniqueId());
        return exp != null && exp > System.currentTimeMillis();
    }

    public long getCooldownRemainingSeconds(Player player) {
        Long exp = cooldowns.get(player.getUniqueId());
        if (exp == null) return 0;
        long rem = exp - System.currentTimeMillis();
        return rem > 0 ? (rem + 999) / 1000 : 0;
    }

    public void setCooldown(Player player, int seconds) {
        long expireTime = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.put(player.getUniqueId(), expireTime);

        BossBar old = activeBars.remove(player.getUniqueId());
        if (old != null) old.removeAll();

        String title = ChatColor.YELLOW + plugin.tr("бᴇзумныᴇ ᴄлоᴛы", "ᴄʀᴀᴢʏ sʟᴏᴛs");
        BossBar bar = TextUtils.bossBar(
                title,
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bar.addPlayer(player);
        bar.setProgress(1.0);
        activeBars.put(player.getUniqueId(), bar);

        new BukkitRunnable() {
            int ticksLeft = seconds * 20;
            final int totalTicks = seconds * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticksLeft <= 0 || !activeBars.containsKey(player.getUniqueId()) || !activeBars.get(player.getUniqueId()).equals(bar)) {
                    bar.removeAll();
                    activeBars.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                bar.setProgress(Math.max(0.0, Math.min(1.0, (double) ticksLeft / totalTicks)));
                ticksLeft--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void startTransformation(Player player, ItemStack originalItem) {
        if (isOnCooldown(player)) {
            return;
        }

        if (playerToInstance.containsKey(player.getUniqueId())) {
            return;
        }

        if (plugin.getWorldGuardManager() != null && !plugin.getWorldGuardManager().isLocationAllowed(player.getLocation(), player)) {
            plugin.getWorldGuardManager().notifyDeniedChat(player);
            return;
        }

        Location loc = player.getLocation();
        World world = player.getWorld();

        world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.8f);
        world.playSound(loc, Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        world.spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.2);

        ItemStack rolledWeapon = rollRandomWeapon();
        if (rolledWeapon == null || rolledWeapon.getType().isAir()) {
            rolledWeapon = WeaponFactory.getBloodLust(0);
        }

        UUID instanceId = UUID.randomUUID();
        ItemMeta meta = rolledWeapon.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(uniqueKey, PersistentDataType.STRING, instanceId.toString());
            rolledWeapon.setItemMeta(meta);
        }

        activeTransformations.put(instanceId, originalItem.clone());
        playerToInstance.put(player.getUniqueId(), instanceId);
        instanceToPlayer.put(instanceId, player.getUniqueId());

        // Replace main hand item with rolled weapon
        player.getInventory().setItemInMainHand(rolledWeapon);
        player.updateInventory();

        // Celebration sound & particles
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.3f);
        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        world.spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1.2, 0), 20, 0.3, 0.4, 0.3, 0.05);

        int duration = plugin.getWeaponsConfig().getInt("crazy-slots.transform-duration", 30);

        // Transformation duration timer: reverts after 30 seconds, then cooldown starts with bossbar
        // Also periodically checks if the player still has the weapon in inventory; if not, grants Crazy Slots
        BukkitTask task = new BukkitRunnable() {
            int elapsed = 0;
            final int maxDurationTicks = duration * 20;

            @Override
            public void run() {
                if (!activeTransformations.containsKey(instanceId)) {
                    cancel();
                    return;
                }

                if (!player.isOnline()) {
                    return;
                }

                if (player.isDead()) {
                    return;
                }

                elapsed += 10;

                boolean hasItem = hasTransformedItem(player, instanceId);

                if (elapsed >= maxDurationTicks) {
                    cancel();
                    revertItem(instanceId);
                    return;
                }

                if (!hasItem) {
                    cancel();
                    ItemStack clean = getCleanCrazySlots(instanceId);
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(clean);
                    if (!leftover.isEmpty()) {
                        for (ItemStack rem : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), rem);
                        }
                    }
                    endTransformationAndApplyCooldown(player, instanceId);
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);

        activeTasks.put(instanceId, task);
    }

    private ItemStack rollRandomWeapon() {
        List<String> poolConfig = plugin.getWeaponsConfig().getStringList("crazy-slots.weapons-pool");
        List<ItemStack> pool = new ArrayList<>();
        if (poolConfig != null && !poolConfig.isEmpty()) {
            for (String id : poolConfig) {
                ItemStack item = resolveWeaponById(id.trim().toLowerCase());
                if (item != null) pool.add(item);
            }
        }

        if (pool.isEmpty()) {
            pool.add(WeaponFactory.getBloodLust(0));
            pool.add(WeaponFactory.getBoneBlade());
            pool.add(WeaponFactory.getCutlass());
            pool.add(WeaponFactory.getEarthGauntlet());
            pool.add(WeaponFactory.getFrostScythe());
            pool.add(WeaponFactory.getHyperion());
            pool.add(WeaponFactory.getKnightfall(0));
            pool.add(WeaponFactory.getNightpiercer());
            pool.add(WeaponFactory.getPaladinsBattleAxe());
            pool.add(WeaponFactory.getPaleGun());
            pool.add(WeaponFactory.getPureBlade());
            pool.add(WeaponFactory.getShadowBlade());
            pool.add(WeaponFactory.getVulcanCrossbow());
            pool.add(WeaponFactory.getWindWeaver());
            pool.add(WeaponFactory.getWitherBlade());
        }

        return pool.get(random.nextInt(pool.size()));
    }

    private ItemStack resolveWeaponById(String id) {
        int bloodlustKills = plugin.getWeaponsConfig().getInt("crazy-slots.bloodlust-kills", 5);
        int knightfallKills = plugin.getWeaponsConfig().getInt("crazy-slots.knightfall-kills", 10);
        return switch (id) {
            case "bloodlust" -> WeaponFactory.getBloodLust(bloodlustKills);
            case "boneblade", "bone_blade" -> WeaponFactory.getBoneBlade();
            case "cutlass" -> WeaponFactory.getCutlass();
            case "earth_gauntlet", "earthgauntlet" -> WeaponFactory.getEarthGauntlet();
            case "frost_scythe", "frostscythe" -> WeaponFactory.getFrostScythe();
            case "hyperion" -> WeaponFactory.getHyperion();
            case "knightfall" -> WeaponFactory.getKnightfall(knightfallKills);
            case "nightpiercer" -> WeaponFactory.getNightpiercer();
            case "paladins_battle_axe", "paladin_axe", "paladin", "battle_axe" -> WeaponFactory.getPaladinsBattleAxe();
            case "pale_gun", "palegun" -> WeaponFactory.getPaleGun();
            case "pure_blade", "pureblade" -> WeaponFactory.getPureBlade();
            case "shadow_blade", "shadowblade" -> WeaponFactory.getShadowBlade();
            case "vulcans_crossbow", "vulcan", "vulcan_crossbow" -> WeaponFactory.getVulcanCrossbow();
            case "windweaver", "wind_weaver" -> WeaponFactory.getWindWeaver();
            case "wither_blade", "witherblade" -> WeaponFactory.getWitherBlade();
            default -> null;
        };
    }

    public void revertItem(UUID instanceId) {
        revertItem(instanceId, true);
    }

    public void revertItem(UUID instanceId, boolean applyCooldown) {
        ItemStack original = activeTransformations.remove(instanceId);
        if (original == null) return;

        BukkitTask task = activeTasks.remove(instanceId);
        if (task != null) task.cancel();

        UUID playerUUID = instanceToPlayer.remove(instanceId);
        if (playerUUID != null) {
            playerToInstance.remove(playerUUID);
        }

        ItemStack cleanOriginal = original.clone();
        ItemMeta meta = cleanOriginal.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(uniqueKey);
            cleanOriginal.setItemMeta(meta);
        }

        boolean restored = false;

        Player targetPlayer = null;

        // 1. Search online player inventories (all slots, offhand, cursor)
        for (Player p : Bukkit.getOnlinePlayers()) {
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack cur = inv.getItem(i);
                if (hasTargetId(cur, instanceId)) {
                    inv.setItem(i, cleanOriginal.clone());
                    restored = true;
                    targetPlayer = p;
                    break;
                }
            }
            if (restored) break;

            if (hasTargetId(p.getInventory().getItemInOffHand(), instanceId)) {
                p.getInventory().setItemInOffHand(cleanOriginal.clone());
                restored = true;
                targetPlayer = p;
                break;
            }

            if (hasTargetId(p.getItemOnCursor(), instanceId)) {
                p.setItemOnCursor(cleanOriginal.clone());
                restored = true;
                targetPlayer = p;
                break;
            }
        }

        // 2. Search dropped items on ground in loaded worlds
        if (!restored) {
            for (World w : Bukkit.getWorlds()) {
                for (Entity ent : w.getEntities()) {
                    if (ent instanceof Item itemEnt) {
                        if (hasTargetId(itemEnt.getItemStack(), instanceId)) {
                            itemEnt.setItemStack(cleanOriginal.clone());
                            restored = true;
                            break;
                        }
                    }
                }
                if (restored) break;
            }
        }

        // 3. Search loaded chunk block containers
        if (!restored) {
            for (World w : Bukkit.getWorlds()) {
                for (Chunk chunk : w.getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Container container) {
                            Inventory inv = container.getInventory();
                            for (int i = 0; i < inv.getSize(); i++) {
                                if (hasTargetId(inv.getItem(i), instanceId)) {
                                    inv.setItem(i, cleanOriginal.clone());
                                    restored = true;
                                    break;
                                }
                            }
                        } else if (state instanceof InventoryHolder holder) {
                            Inventory inv = holder.getInventory();
                            for (int i = 0; i < inv.getSize(); i++) {
                                if (hasTargetId(inv.getItem(i), instanceId)) {
                                    inv.setItem(i, cleanOriginal.clone());
                                    restored = true;
                                    break;
                                }
                            }
                        }
                        if (restored) break;
                    }
                    if (restored) break;
                }
                if (restored) break;
            }
        }

        // 4. Fallback: if not found anywhere, give directly to owner player if online
        if (!restored && playerUUID != null) {
            Player owner = Bukkit.getPlayer(playerUUID);
            if (owner != null && owner.isOnline()) {
                Map<Integer, ItemStack> leftover = owner.getInventory().addItem(cleanOriginal.clone());
                if (!leftover.isEmpty()) {
                    for (ItemStack rem : leftover.values()) {
                        owner.getWorld().dropItemNaturally(owner.getLocation(), rem);
                    }
                }
                targetPlayer = owner;
                restored = true;
            }
        }

        Player playerToCooldown = targetPlayer;
        if (playerToCooldown == null && playerUUID != null) {
            playerToCooldown = Bukkit.getPlayer(playerUUID);
        }

        if (playerToCooldown != null && playerToCooldown.isOnline()) {
            playerToCooldown.getWorld().playSound(playerToCooldown.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            playerToCooldown.getWorld().playSound(playerToCooldown.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
            playerToCooldown.updateInventory();
            if (applyCooldown) {
                int cooldownSec = plugin.getWeaponsConfig().getInt("crazy-slots.cooldown", 60);
                setCooldown(playerToCooldown, cooldownSec);
            }
        }
    }

    private void handlePlayerReverted(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
        player.updateInventory();
        int cooldownSec = plugin.getWeaponsConfig().getInt("crazy-slots.cooldown", 60);
        setCooldown(player, cooldownSec);
    }

    private boolean hasTargetId(ItemStack item, UUID targetId) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String val = meta.getPersistentDataContainer().get(uniqueKey, PersistentDataType.STRING);
        return val != null && val.equals(targetId.toString());
    }

    public void handleQuit(Player player) {
        UUID instanceId = playerToInstance.remove(player.getUniqueId());
        if (instanceId != null) {
            revertItem(instanceId);
        }
    }

    public void handleDeath(Player player) {
        UUID instanceId = playerToInstance.remove(player.getUniqueId());
        if (instanceId != null) {
            revertItem(instanceId);
        }
    }

    public void forceRevertAll() {
        for (UUID instanceId : new ArrayList<>(activeTransformations.keySet())) {
            revertItem(instanceId, false);
        }
        activeTransformations.clear();
        activeTasks.clear();
        playerToInstance.clear();
        instanceToPlayer.clear();
        clearAllBars();
    }

    public void resetPlayer(Player player) {
        UUID instanceId = playerToInstance.get(player.getUniqueId());
        if (instanceId != null) {
            revertItem(instanceId, false);
        }
        cooldowns.remove(player.getUniqueId());
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    public void clearAllBars() {
        for (BossBar bar : activeBars.values()) {
            bar.removeAll();
        }
        activeBars.clear();
    }
}
