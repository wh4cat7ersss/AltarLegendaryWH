package dev.whersss.altarLegendaryWH.items.shards.paleshard;

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
public class PaleShardItem {

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(2);
            meta.setTooltipStyle(NamespacedKey.minecraft("pure"));

            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(TextUtils.shadow(mm.deserialize("<!italic><gray>" + tr("Бледный Осколок", "ᴘᴀʟᴇ sʜᴀʀᴅ") + "</gray>")));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Холодный осколок, хранящий в себе", "A cold shard holding within it") + "</white>"));
            lore.add(mm.deserialize("<!italic><white>" + tr("тихую и бледную силу иного мира.", "the quiet, pale power of another world.") + "</white>"));
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
