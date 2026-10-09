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
    private final Map<UUID, Long> savedSpeedTime = new HashMap<>();
    private final Map<UUID, Boolean> wasHolding = new HashMap<>();

    public KnightfallPassiveTask(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack item = p.getInventory().getItemInMainHand();
            boolean isHoldingKnightfall = false;

            if (dev.whersss.altarLegendaryWH.utils.WeaponFactory.isAltarWeapon(item, "knightfall")) {
                if (item.getItemMeta().getPersistentDataContainer().has(plugin.getKillsKey(), PersistentDataType.INTEGER)) {
                    int kills = item.getItemMeta().getPersistentDataContainer().get(plugin.getKillsKey(), PersistentDataType.INTEGER);

                    int cmd = item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 1;
                    boolean hasDensity = item.getItemMeta().hasEnchant(Enchantment.DENSITY);
                    int tier = item.getItemMeta().getPersistentDataContainer().getOrDefault(plugin.getKnightfallTierKey(), PersistentDataType.INTEGER,
                            (cmd >= 3003 || cmd == 3) ? 3 : (hasDensity ? 2 : ((cmd >= 3002 || cmd == 2) ? 1 : 0)));

                    boolean needsUpgrade = (kills >= 4 && tier < 1) ||
                            (kills >= 8 && tier < 2) ||
                            (kills >= 10 && tier < 3);

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
                if (!wasHolding.getOrDefault(p.getUniqueId(), false) && curSpeed != null && !isWeaponSpeed) {
                    if (!dev.whersss.altarLegendaryWH.utils.SpeedBuffUtils.isWearingCopperBoots(p)) {
                        savedSpeed.put(p.getUniqueId(), curSpeed);
                        savedSpeedTime.put(p.getUniqueId(), System.currentTimeMillis());
                    }
                }

                if (curSpeed == null || isWeaponSpeed || curSpeed.getAmplifier() < 1) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true));
                }

                wasHolding.put(p.getUniqueId(), true);
            } else {
                boolean heldBefore = wasHolding.getOrDefault(p.getUniqueId(), false);

                if (heldBefore) {
                    if (isWeaponSpeed) {
                        p.removePotionEffect(PotionEffectType.SPEED);
                    }

                    if (savedSpeed.containsKey(p.getUniqueId())) {
                        PotionEffect old = savedSpeed.remove(p.getUniqueId());
                        Long startTime = savedSpeedTime.remove(p.getUniqueId());
                        if (old != null && startTime != null && !dev.whersss.altarLegendaryWH.utils.SpeedBuffUtils.isWearingCopperBoots(p)) {
                            if (old.getDuration() == PotionEffect.INFINITE_DURATION) {
                                p.addPotionEffect(old);
                            } else {
                                long elapsed = (System.currentTimeMillis() - startTime) / 50L;
                                int rem = old.getDuration() - (int) elapsed;
                                if (rem > 0) {
                                    p.addPotionEffect(new PotionEffect(old.getType(), rem, old.getAmplifier(), old.isAmbient(), old.hasParticles(), old.hasIcon()));
                                }
                            }
                        }
                    }

                    wasHolding.put(p.getUniqueId(), false);
                }
            }
        }
    }
}