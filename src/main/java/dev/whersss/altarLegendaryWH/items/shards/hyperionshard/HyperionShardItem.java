package dev.whersss.altarLegendaryWH.items.shards.hyperionshard;

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
public class HyperionShardItem {

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(3);
            meta.setTooltipStyle(NamespacedKey.minecraft("gold"));

            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(TextUtils.shadow(mm.deserialize("<!italic><gold>" + tr("Гиперионовый Осколок", "ʜʏᴘᴇʀɪᴏɴ sʜᴀʀᴅ") + "</gold>")));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><yellow>" + tr("Сияющий осколок, наполненный", "A radiant shard filled with") + "</yellow>"));
            lore.add(mm.deserialize("<!italic><yellow>" + tr("священной и божественной силой.", "sacred and divine power.") + "</yellow>"));
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
