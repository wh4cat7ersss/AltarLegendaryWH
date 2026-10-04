package dev.whersss.altarLegendaryWH.weapons.bloodlust.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.CombatUtils;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import dev.whersss.altarLegendaryWH.weapons.bloodlust.managers.BloodAbilityManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BloodLustListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final BloodAbilityManager abilityManager;

    public BloodLustListener(AltarLegendaryWH plugin, BloodAbilityManager abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }

    public static int getKills(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(AltarLegendaryWH.getInstance().getKillsKey(), PersistentDataType.INTEGER, 0);
    }

    public static boolean isBloodLust(ItemStack item) {
        return WeaponFactory.isAltarWeapon(item, "bloodlust");
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player && e.getEntity().getKiller() != null) {
            Player p = e.getEntity().getKiller();
            ItemStack item = p.getInventory().getItemInMainHand();
            if (isBloodLust(item)) {
                int kills = getKills(item) + 1;
                updateItemLore(item, kills);

                p.sendActionBar(TextUtils.legacy(plugin.tr("&cЖажда крови пополняется новыми жертвами", "&cʙʟᴏᴏᴅʟᴜsᴛ feeds on new victims")));
                abilityManager.playKillVisual(p, e.getEntity().getLocation());
            }
        }
    }

    private void updateItemLore(ItemStack item, int kills) {
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(plugin.getKillsKey(), PersistentDataType.INTEGER, kills);

        ItemStack dummyItem = WeaponFactory.getBloodLust(kills);
        if (dummyItem.getItemMeta() != null && dummyItem.getItemMeta().hasLore()) {
            meta.lore(dummyItem.getItemMeta().lore());
        }

        item.setItemMeta(meta);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (!isBloodLust(item)) return;

        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (p.isSneaking()) {
                p.sendActionBar(TextUtils.legacy(plugin.tr("&cЖажда крови имеет &4&l" + getKills(item) + " &cубийств", "&cʙʟᴏᴏᴅʟᴜsᴛ has &4&l" + getKills(item) + " &ckills")));
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (!isBloodLust(item)) return;

        e.setCancelled(true);


        if (p.isSneaking()) {
            if (getKills(item) >= 3) abilityManager.castBloodTrail(p);
        } else {
            if (getKills(item) >= 5) {
                abilityManager.castBloodHook(p);
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!CombatUtils.isDirectMeleeHit(e)) return;
        if (e.getDamager() instanceof Player p) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (isBloodLust(item)) {
                abilityManager.applyInfection(p, e.getEntity());
            }
        }
    }

    @EventHandler
    public void onJump(PlayerMoveEvent event) {
        if (!abilityManager.isInBloodTrail(event.getPlayer())) return;
        if (event.getTo() == null) return;
        if (event.getTo().getY() > event.getFrom().getY() + 0.2 && event.getPlayer().getVelocity().getY() > 0.0) {
            abilityManager.cancelBloodTrail(event.getPlayer(), true);
        }
    }

    @EventHandler
    public void onItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        if (abilityManager.isInBloodTrail(p)) {
            ItemStack newItem = p.getInventory().getItem(event.getNewSlot());
            if (!isBloodLust(newItem)) {
                abilityManager.cancelBloodTrail(p, true);
            }
        }
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (abilityManager.isInBloodTrail(p) && isBloodLust(event.getItemDrop().getItemStack())) {
            abilityManager.cancelBloodTrail(p, true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (abilityManager.isInBloodTrail(p)) {
            abilityManager.cancelBloodTrail(p, false);
        }
    }
}

