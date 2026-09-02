package dev.whersss.altarLegendaryWH.items.shards.vampireshard;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class VampireShardItem {

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(1);
            meta.setTooltipStyle(NamespacedKey.minecraft("shard"));

            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(TextUtils.shadow(mm.deserialize("<!italic><red>" + tr("Вампировый Осколок", "ᴠᴀᴍᴘɪʀᴇ sʜᴀʀᴅ") + "</red>")));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><dark_red>" + tr("Осколок, пропитанный жаждой", "A shard soaked in a thirst for") + "</dark_red>"));
            lore.add(mm.deserialize("<!italic><dark_red>" + tr("чужой крови и жизненной силы.", "blood and stolen life force.") + "</dark_red>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Применение:", "Use:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Используется для создания", "Used to create") + "</gray>"));
            lore.add(mm.deserialize("<!italic><gray>" + tr("легендарного оружия.", "legendary weapons.") + "</gray>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><dark_gray><i>" + tr("(( Материал для крафта ))", "(( Crafting material ))") + "</i></dark_gray>"));

            meta.lore(lore.stream().map(TextUtils::shadow).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
