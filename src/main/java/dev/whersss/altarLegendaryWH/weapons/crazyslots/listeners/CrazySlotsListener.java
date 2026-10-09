package dev.whersss.altarLegendaryWH.weapons.crazyslots.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.weapons.crazyslots.managers.CrazySlotsManager;
import org.bukkit.Sound;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CrazySlotsListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final CrazySlotsManager manager;

    public CrazySlotsListener(AltarLegendaryWH plugin, CrazySlotsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!manager.isCrazySlots(item)) return;

        event.setCancelled(true);
        manager.startTransformation(player, item);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID instanceId = manager.getInstanceIdForPlayer(player.getUniqueId());

        // 1. Check drops list (when keepInventory is false)
        List<ItemStack> drops = event.getDrops();
        for (int i = 0; i < drops.size(); i++) {
            ItemStack drop = drops.get(i);
            if (drop != null && manager.isTransformedItem(drop)) {
                UUID id = manager.getTransformedInstanceId(drop);
                if (id != null) {
                    drops.set(i, manager.getCleanCrazySlots(id));
                    if (instanceId == null) instanceId = id;
                }
            }
        }

        // 2. Check player inventory (when keepInventory is true)
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack cur = inv.getItem(i);
            if (cur != null && manager.isTransformedItem(cur)) {
                UUID id = manager.getTransformedInstanceId(cur);
                if (id != null) {
                    inv.setItem(i, manager.getCleanCrazySlots(id));
                    if (instanceId == null) instanceId = id;
                }
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && manager.isTransformedItem(offhand)) {
            UUID id = manager.getTransformedInstanceId(offhand);
            if (id != null) {
                player.getInventory().setItemInOffHand(manager.getCleanCrazySlots(id));
                if (instanceId == null) instanceId = id;
            }
        }

        if (instanceId != null) {
            manager.endTransformationAndApplyCooldown(player, instanceId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (manager.isTransformedItem(item)) {
            UUID instanceId = manager.getTransformedInstanceId(item);
            if (instanceId != null) {
                ItemStack cleanCrazySlots = manager.getCleanCrazySlots(instanceId);
                event.getItemDrop().setItemStack(cleanCrazySlots);
                Player player = event.getPlayer();
                manager.endTransformationAndApplyCooldown(player, instanceId);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory bottomInv = event.getView().getBottomInventory();
        Inventory clickedInv = event.getClickedInventory();

        boolean isTopContainer = isExternalContainer(topInv);

        ItemStack curItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        if (isBundle(curItem) && manager.isTransformedItem(cursorItem)) {
            event.setCancelled(true);
            return;
        }
        if (isBundle(cursorItem) && manager.isTransformedItem(curItem)) {
            event.setCancelled(true);
            return;
        }

        // Block placing transformed weapon into modification stations (anvil, grindstone, smithing, etc.)
        if (isModificationStation(topInv)) {
            if (clickedInv != null && clickedInv.equals(topInv)) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && manager.isTransformedItem(cursor)) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getClick() == ClickType.NUMBER_KEY) {
                    int hb = event.getHotbarButton();
                    if (hb >= 0 && hb < 9) {
                        ItemStack hbItem = player.getInventory().getItem(hb);
                        if (hbItem != null && manager.isTransformedItem(hbItem)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
            if (clickedInv != null && clickedInv.equals(bottomInv) && event.isShiftClick()) {
                ItemStack cur = event.getCurrentItem();
                if (cur != null && manager.isTransformedItem(cur)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Case A: Cursor placement into top external container
        if (isTopContainer && clickedInv != null && clickedInv.equals(topInv)) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && manager.isTransformedItem(cursor)) {
                UUID instanceId = manager.getTransformedInstanceId(cursor);
                if (instanceId != null) {
                    event.setCancelled(true);
                    ItemStack cleanCrazySlots = manager.getCleanCrazySlots(instanceId);
                    int slot = event.getSlot();
                    ItemStack currentInSlot = topInv.getItem(slot);

                    topInv.setItem(slot, cleanCrazySlots);
                    if (currentInSlot != null && !currentInSlot.getType().isAir()) {
                        player.setItemOnCursor(currentInSlot);
                    } else {
                        player.setItemOnCursor(null);
                    }
                    player.updateInventory();

                    manager.endTransformationAndApplyCooldown(player, instanceId);
                    return;
                }
            }

            // Case B: Hotbar number swap (keys 1-9) into container slot
            if (event.getClick() == ClickType.NUMBER_KEY) {
                int hotbarButton = event.getHotbarButton();
                if (hotbarButton >= 0 && hotbarButton < 9) {
                    ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                    if (hotbarItem != null && manager.isTransformedItem(hotbarItem)) {
                        UUID instanceId = manager.getTransformedInstanceId(hotbarItem);
                        if (instanceId != null) {
                            event.setCancelled(true);
                            ItemStack cleanCrazySlots = manager.getCleanCrazySlots(instanceId);
                            int slot = event.getSlot();
                            ItemStack currentInSlot = topInv.getItem(slot);

                            topInv.setItem(slot, cleanCrazySlots);
                            player.getInventory().setItem(hotbarButton, currentInSlot);
                            player.updateInventory();

                            manager.endTransformationAndApplyCooldown(player, instanceId);
                            return;
                        }
                    }
                }
            }

            // Case C: Offhand swap ('F' key) into container slot
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                ItemStack offhandItem = player.getInventory().getItemInOffHand();
                if (offhandItem != null && manager.isTransformedItem(offhandItem)) {
                    UUID instanceId = manager.getTransformedInstanceId(offhandItem);
                    if (instanceId != null) {
                        event.setCancelled(true);
                        ItemStack cleanCrazySlots = manager.getCleanCrazySlots(instanceId);
                        int slot = event.getSlot();
                        ItemStack currentInSlot = topInv.getItem(slot);

                        topInv.setItem(slot, cleanCrazySlots);
                        player.getInventory().setItemInOffHand(currentInSlot);
                        player.updateInventory();

                        manager.endTransformationAndApplyCooldown(player, instanceId);
                        return;
                    }
                }
            }
        }

        // Case D: Shift-click from bottom inventory (player inventory) into top external container
        if (isTopContainer && clickedInv != null && clickedInv.equals(bottomInv) && event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && manager.isTransformedItem(clickedItem)) {
                UUID instanceId = manager.getTransformedInstanceId(clickedItem);
                if (instanceId != null) {
                    event.setCancelled(true);
                    ItemStack cleanCrazySlots = manager.getCleanCrazySlots(instanceId);

                    Map<Integer, ItemStack> leftover = topInv.addItem(cleanCrazySlots);
                    if (leftover.isEmpty()) {
                        event.setCurrentItem(null);
                        player.updateInventory();
                        manager.endTransformationAndApplyCooldown(player, instanceId);
                    } else {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    }
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!isExternalContainer(topInv)) return;

        ItemStack oldCursor = event.getOldCursor();
        if (oldCursor == null || !manager.isTransformedItem(oldCursor)) return;

        UUID instanceId = manager.getTransformedInstanceId(oldCursor);
        if (instanceId == null) return;

        int topSize = topInv.getSize();
        int targetSlot = -1;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                targetSlot = rawSlot;
                break;
            }
        }

        if (targetSlot >= 0) {
            event.setCancelled(true);
            ItemStack cleanCrazySlots = manager.getCleanCrazySlots(instanceId);
            topInv.setItem(targetSlot, cleanCrazySlots);
            player.setItemOnCursor(null);
            player.updateInventory();
            manager.endTransformationAndApplyCooldown(player, instanceId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView() == null) return;
        Inventory topInv = event.getView().getTopInventory();
        if (!isExternalContainer(topInv)) return;

        Player player = event.getPlayer() instanceof Player p ? p : null;

        for (int i = 0; i < topInv.getSize(); i++) {
            ItemStack item = topInv.getItem(i);
            if (item != null && manager.isTransformedItem(item)) {
                UUID instanceId = manager.getTransformedInstanceId(item);
                if (instanceId != null) {
                    topInv.setItem(i, manager.getCleanCrazySlots(instanceId));
                    manager.endTransformationAndApplyCooldown(player, instanceId);
                }
            }
        }
    }

    private boolean isExternalContainer(Inventory inventory) {
        if (inventory == null) return false;
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Player) return false;

        InventoryType type = inventory.getType();
        return switch (type) {
            case CHEST, ENDER_CHEST, BARREL, SHULKER_BOX, DISPENSER, DROPPER, HOPPER,
                 FURNACE, BLAST_FURNACE, SMOKER, BREWING, CHISELED_BOOKSHELF, JUKEBOX, CRAFTER -> true;
            default -> (holder instanceof Container || holder instanceof DoubleChest || holder instanceof Entity);
        };
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.ItemFrame frame) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItem(event.getHand());
            if (item != null && manager.isTransformedItem(item)) {
                UUID instanceId = manager.getTransformedInstanceId(item);
                if (instanceId != null) {
                    event.setCancelled(true);
                    if (frame.getItem().getType().isAir()) {
                        ItemStack cleanCrazySlots = manager.getCleanCrazySlots(instanceId);
                        frame.setItem(cleanCrazySlots);
                        player.getInventory().setItem(event.getHand(), null);
                        player.updateInventory();
                        manager.endTransformationAndApplyCooldown(player, instanceId);
                    }
                }
            }
        }
    }

    private boolean isBundle(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("BUNDLE");
    }

    private boolean isModificationStation(Inventory inventory) {
        if (inventory == null) return false;
        InventoryType type = inventory.getType();
        return switch (type) {
            case ANVIL, SMITHING, ENCHANTING, GRINDSTONE, MERCHANT, LOOM, CARTOGRAPHY, STONECUTTER -> true;
            default -> false;
        };
    }
}
