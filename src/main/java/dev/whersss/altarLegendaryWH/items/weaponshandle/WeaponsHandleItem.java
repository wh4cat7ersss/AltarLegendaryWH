package dev.whersss.altarLegendaryWH.items.weaponshandle;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class WeaponsHandleItem {

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(9);

            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(TextUtils.shadow(mm.deserialize("<!italic><gradient:#ff5555:#aa0000:#ff5555>" + tr("Рукоятка Оружия", "ᴡᴇᴀᴘᴏɴ ʜᴀɴᴅʟᴇ") + "</gradient>")));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><#ff5555>" + tr("Прочная рукоять, выкованная из", "A sturdy handle forged from") + "</#ff5555>"));
            lore.add(mm.deserialize("<!italic><#ff5555>" + tr("незерита и энергии звезды незера.", "netherite and the energy of a nether star.") + "</#ff5555>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Как добыть:", "How to obtain:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Создается на", "Crafted at a") + "</gray> <gold>" + tr("Верстаке", "crafting table") + "</gold>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Применение:", "Use:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Служит идеальной основой для", "Serves as the perfect base for") + "</gray>"));
            lore.add(mm.deserialize("<!italic><gray>" + tr("создания легендарных оружий.", "creating legendary weapons.") + "</gray>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><dark_gray><i>" + tr("(( Материал для крафта ))", "(( Crafting material ))") + "</i></dark_gray>"));

            meta.lore(lore.stream().map(TextUtils::shadow).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
