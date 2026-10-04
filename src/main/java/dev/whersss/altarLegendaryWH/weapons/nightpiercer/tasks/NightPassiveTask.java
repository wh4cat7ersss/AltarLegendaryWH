package dev.whersss.altarLegendaryWH.weapons.nightpiercer.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class NightPassiveTask extends BukkitRunnable {

    @SuppressWarnings("deprecation")
    private boolean isNightpiercer(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (meta.getPersistentDataContainer().has(new NamespacedKey(AltarLegendaryWH.getInstance(), "nightpiercer"), PersistentDataType.BYTE)) {
            return true;
        }
        return meta.hasCustomModelData() && (meta.getCustomModelData() == 5 || meta.getCustomModelData() == 3009);
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isNightpiercer(p.getInventory().getItemInMainHand())) {
                long time = p.getWorld().getTime();

                if (time >= 13000 && time <= 23000) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 0, false, false, false));
                }
            }
        }
    }
}