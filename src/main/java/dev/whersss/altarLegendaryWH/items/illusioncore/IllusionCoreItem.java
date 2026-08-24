package dev.whersss.altarLegendaryWH.items.illusioncore;

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
public class IllusionCoreItem {

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(7);

            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(TextUtils.shadow(mm.deserialize("<!italic><gradient:#e64ce6:#990099:#e64ce6>" + tr("Ядро Иллюзий", "ɪʟʟᴜsɪᴏɴ ᴄᴏʀᴇ") + "</gradient>")));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><#d142f5>" + tr("Таинственное ядро, искажающее", "A mysterious core that twists") + "</#d142f5>"));
            lore.add(mm.deserialize("<!italic><#d142f5>" + tr("пространство и разум вокруг себя.", "space and the mind around it.") + "</#d142f5>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Как добыть:", "How to obtain:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Создается на", "Crafted at a") + "</gray> <gold>" + tr("Верстаке", "crafting table") + "</gold>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><white>" + tr("Применение:", "Use:") + "</white>"));
            lore.add(mm.deserialize("<!italic><dark_gray>•</dark_gray> <gray>" + tr("Наполняет легендарное оружие", "Fills legendary weapons with") + "</gray>"));
            lore.add(mm.deserialize("<!italic><gray>" + tr("мистической силой иллюзий.", "the mystical power of illusions.") + "</gray>"));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<!italic><dark_gray><i>" + tr("(( Материал для крафта ))", "(( Crafting material ))") + "</i></dark_gray>"));

            meta.lore(lore.stream().map(TextUtils::shadow).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
