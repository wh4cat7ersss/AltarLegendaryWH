package dev.whersss.altarLegendaryWH.items.wardenheart;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

public class WardenHeartItem {

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(1);
            meta.setTooltipStyle(NamespacedKey.minecraft("ancient"));

            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(TextUtils.shadow(mm.deserialize("<!italic><gradient:#0b4d53:#23c7c8:#0b4d53>" + tr("Сердце Вардена", "ᴡᴀʀᴅᴇɴ ʜᴇᴀʀᴛ") + "</gradient>")));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><#169c9d>" + tr("Редкий артефакт, пульсирующий", "A rare artifact pulsing with") + "</#169c9d>"));
            lore.add(mm.deserialize("<!italic><#169c9d>" + tr("темной энергией скалка.", "the dark energy of the deep stone.") + "</#169c9d>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Как добыть:", "How to obtain:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Выпадает при убийстве", "Drops when killing the") + "</gray> <dark_aqua>" + tr("Вардена", "warden") + "</dark_aqua>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Применение:", "Use:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Используется для создания", "Used to create") + "</gray>"));
            lore.add(mm.deserialize("<!italic><gray>" + tr("других легендарных оружий.", "other legendary weapons.") + "</gray>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><dark_gray><i>" + tr("(( Материал для крафта ))", "(( Crafting material ))") + "</i></dark_gray>"));

            meta.lore(lore.stream().map(TextUtils::shadow).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
