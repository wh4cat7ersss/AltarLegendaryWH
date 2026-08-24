package dev.whersss.altarLegendaryWH.weapons.frostscythe.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.frostscythe.managers.FrostAbilityManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class FrostListener implements Listener {

    private final FrostAbilityManager abilityManager;
    private final AltarLegendaryWH plugin;
    private final Set<UUID> checkingPlayers = new HashSet<>();

    public FrostListener(FrostAbilityManager abilityManager) {
        this.abilityManager = abilityManager;
        this.plugin = abilityManager.getPlugin();
    }

    private boolean isFrostScythe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        return meta != null &&
                meta.hasCustomModelData() &&
                meta.getCustomModelData() == 4 &&
                (item.getType() == Material.NETHERITE_SWORD || item.getType() == Material.TRIDENT);
    }

    public static void setScytheModel(ItemStack item, boolean isDrawing) {
        if (item == null || !item.hasItemMeta()) return;
        try {
            ItemMeta meta = item.getItemMeta();
            String keyStr = "minecraft:netherite_sword";
            String[] parts = keyStr.split(":");
            NamespacedKey key = new NamespacedKey(parts[0], parts[1]);

            meta.setItemModel(key);
            item.setItemMeta(meta);
        } catch (Throwable ignored) {
        }
    }

    private void morphScythe(ItemStack item, Material targetMaterial) {
        if (item.getType() == targetMaterial) {
            setScytheModel(item, false);
            return;
        }
        item.setType(targetMaterial);
        setScytheModel(item, false);
    }

    private void forceResetHands(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();

        if (isFrostScythe(main)) setScytheModel(main, false);
        if (isFrostScythe(off)) setScytheModel(off, false);
        p.updateInventory();
    }

    private void startCooldownCheck(Player p) {
        if (checkingPlayers.contains(p.getUniqueId())) return;
        checkingPlayers.add(p.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !abilityManager.isOnCooldown(p, "ScytheThrow")) {
                    if (p.isOnline()) {
                        for (int i = 0; i < p.getInventory().getSize(); i++) {
                            ItemStack item = p.getInventory().getItem(i);
                            if (isFrostScythe(item) && item.getType() == Material.NETHERITE_SWORD) {
                                morphScythe(item, Material.TRIDENT);
                            }
                        }
                        p.updateInventory();
                    }
                    checkingPlayers.remove(p.getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (!isFrostScythe(item)) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (p.isSneaking()) {
                e.setCancelled(true);
                if (plugin.isAboveLegendaryHeight(p)) return;
                abilityManager.castCommandOfIce(p);
            } else {
                if (abilityManager.isOnCooldown(p, "ScytheThrow")) {
                    if (item.getType() == Material.TRIDENT) {
                        morphScythe(item, Material.NETHERITE_SWORD);
                        p.updateInventory();
                        startCooldownCheck(p);
                    }
                    if (item.getType() == Material.TRIDENT) {
                        e.setCancelled(true);
                    }
                } else {
                    if (item.getType() == Material.TRIDENT) {
                        setScytheModel(item, true);
                        p.updateInventory();

                        final EquipmentSlot hand = e.getHand();

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!p.isOnline()) {
                                    this.cancel();
                                    return;
                                }

                                ItemStack currentItem = (hand == EquipmentSlot.HAND) ?
                                        p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();

                                if (!p.isHandRaised() || !isFrostScythe(currentItem) || currentItem.getType() != Material.TRIDENT) {
                                    forceResetHands(p);
                                    this.cancel();
                                }
                            }
                        }.runTaskTimer(plugin, 1L, 1L);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack oldItem = p.getInventory().getItem(e.getPreviousSlot());
        if (isFrostScythe(oldItem)) {
            setScytheModel(oldItem, false);
        }
        forceResetHands(p);
    }

    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent e) {
        ItemStack main = e.getMainHandItem();
        ItemStack off = e.getOffHandItem();

        if (isFrostScythe(main)) setScytheModel(main, false);
        if (isFrostScythe(off)) setScytheModel(off, false);
        e.getPlayer().updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof Trident trident && trident.getShooter() instanceof Player p) {
            ItemStack main = p.getInventory().getItemInMainHand();
            ItemStack off = p.getInventory().getItemInOffHand();

            EquipmentSlot hand = null;
            ItemStack item = null;

            if (isFrostScythe(main) && main.getType() == Material.TRIDENT) {
                item = main;
                hand = EquipmentSlot.HAND;
            } else if (isFrostScythe(off) && off.getType() == Material.TRIDENT) {
                item = off;
                hand = EquipmentSlot.OFF_HAND;
            }

            if (item != null) {
                e.setCancelled(true);
                p.updateInventory();

                if (!abilityManager.isOnCooldown(p, "ScytheThrow")) {
                    abilityManager.executeScytheThrow(p, item, hand);
                    startCooldownCheck(p);
                }
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

        if (e.getDamager() instanceof Player p) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (isFrostScythe(item) && e.getEntity() instanceof org.bukkit.entity.LivingEntity victim) {
                if (victim instanceof Player targetPlayer && targetPlayer.getGameMode() == GameMode.SPECTATOR) return;

                Vector dir = p.getLocation().getDirection();
                Location sweepLoc = victim.getLocation().clone().add(0, 1.2, 0).subtract(dir.clone().normalize().multiply(1.2));
                abilityManager.playSweepEffect(sweepLoc, dir);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        if (isFrostScythe(item)) {
            morphScythe(item, Material.NETHERITE_SWORD);
            e.getItemDrop().setItemStack(item);
        }
        forceResetHands(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        for (ItemStack item : e.getDrops()) {
            if (isFrostScythe(item)) {
                morphScythe(item, Material.NETHERITE_SWORD);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            Item itemEntity = e.getItem();
            ItemStack itemStack = itemEntity.getItemStack();

            if (isFrostScythe(itemStack)) {
                if (abilityManager.isOnCooldown(p, "ScytheThrow")) {
                    morphScythe(itemStack, Material.NETHERITE_SWORD);
                } else {
                    morphScythe(itemStack, Material.TRIDENT);
                }
                itemEntity.setItemStack(itemStack);
                startCooldownCheck(p);
            }
        }
    }
}