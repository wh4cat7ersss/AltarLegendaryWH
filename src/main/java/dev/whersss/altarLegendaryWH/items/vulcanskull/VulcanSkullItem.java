package dev.whersss.altarLegendaryWH.items.vulcanskull;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VulcanSkullItem {

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.CLAY_BALL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(3);

            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(TextUtils.shadow(mm.deserialize("<!italic><gradient:#ff5555:#aa0000:#ff5555>" + tr("Череп Вулкана", "ᴠᴜʟᴄᴀɴ sᴋᴜʟʟ") + "</gradient>")));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><#ff5555>" + tr("Пылающий череп древнего божества,", "The blazing skull of an ancient deity,") + "</#ff5555>"));
            lore.add(mm.deserialize("<!italic><#ff5555>" + tr("источающий невыносимый жар.", "radiating unbearable heat.") + "</#ff5555>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Как добыть:", "How to obtain:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Создается на", "Crafted at a") + "</gray> <gold>" + tr("Верстаке", "crafting table") + "</gold>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Применение:", "Use:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Используется для создания", "Used to create") + "</gray>"));
            lore.add(mm.deserialize("<!italic><gray>" + tr("других легендарных оружий.", "other legendary weapons.") + "</gray>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><dark_gray><i>" + tr("(( Материал для крафта ))", "(( Crafting material ))") + "</i></dark_gray>"));

            meta.lore(lore.stream().map(TextUtils::shadow).toList());
            NamespacedKey uuidKey = new NamespacedKey(AltarLegendaryWH.getInstance(), "unique_uuid");
            meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            item.setItemMeta(meta);
        }
        return item;
    }
}
