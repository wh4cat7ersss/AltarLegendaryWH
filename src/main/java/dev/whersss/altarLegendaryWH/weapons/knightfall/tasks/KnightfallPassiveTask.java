package dev.whersss.altarLegendaryWH.weapons.knightfall.tasks;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KnightfallPassiveTask extends BukkitRunnable {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, PotionEffect> savedSpeed = new HashMap<>();
    private final Map<UUID, Boolean> wasHolding = new HashMap<>();

    public KnightfallPassiveTask(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack item = p.getInventory().getItemInMainHand();
            boolean isHoldingKnightfall = false;

            if (item != null && item.getType() == Material.MACE && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer().has(plugin.getKillsKey(), PersistentDataType.INTEGER)) {
                    int kills = item.getItemMeta().getPersistentDataContainer().get(plugin.getKillsKey(), PersistentDataType.INTEGER);

                    int cmd = item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 1;
                    boolean hasDensity = item.getItemMeta().hasEnchant(Enchantment.DENSITY);

                    boolean needsUpgrade = (kills >= 4 && cmd < 2) ||
                            (kills >= 8 && !hasDensity) ||
                            (kills >= 10 && cmd < 3);

                    if (needsUpgrade) {
                        p.sendActionBar(TextUtils.legacy(plugin.tr("&aНажмите &2[Смена руки] &aчтобы улучшить оружие.", "&aPress &2[OffHand] &ato upgrade the weapon.")));
                    }

                    if (kills >= 6) {
                        isHoldingKnightfall = true;
                    }
                }
            }

            PotionEffect curSpeed = p.getPotionEffect(PotionEffectType.SPEED);
            boolean isWeaponSpeed = curSpeed != null && curSpeed.getAmplifier() == 1 && curSpeed.getDuration() <= 80;

            if (isHoldingKnightfall) {
                if (!isWeaponSpeed && curSpeed != null) {
                    savedSpeed.put(p.getUniqueId(), curSpeed);
                }

                if (curSpeed == null || isWeaponSpeed || curSpeed.getAmplifier() < 1) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false, true));
                }

                wasHolding.put(p.getUniqueId(), true);
            } else {
                boolean heldBefore = wasHolding.getOrDefault(p.getUniqueId(), false);

                if (heldBefore) {
                    if (isWeaponSpeed) {
                        p.removePotionEffect(PotionEffectType.SPEED);
                    }

                    if (savedSpeed.containsKey(p.getUniqueId())) {
                        p.addPotionEffect(savedSpeed.remove(p.getUniqueId()));
                    }

                    wasHolding.put(p.getUniqueId(), false);
                }
            }
        }
    }
}