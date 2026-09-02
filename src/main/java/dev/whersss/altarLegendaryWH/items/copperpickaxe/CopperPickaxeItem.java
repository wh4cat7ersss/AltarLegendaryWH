package dev.whersss.altarLegendaryWH.items.copperpickaxe;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CopperPickaxeItem {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static final NamespacedKey KEY_IDENTIFIER = new NamespacedKey(AltarLegendaryWH.getInstance(), "copper_pickaxe");
    public static final NamespacedKey KEY_MODE = new NamespacedKey(AltarLegendaryWH.getInstance(), "copper_pickaxe_mode_3x3");
    private static final NamespacedKey KEY_DAMAGE = new NamespacedKey(AltarLegendaryWH.getInstance(), "copper_pickaxe_damage");
    private static final NamespacedKey KEY_SPEED = new NamespacedKey(AltarLegendaryWH.getInstance(), "copper_pickaxe_speed");

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.COPPER_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Component name = MM.deserialize("<!italic><!bold><gradient:#E77C56:#8A4B26>"
                + tr("Медная Кирка", "ᴄᴏᴘᴘᴇʀ ᴘɪᴄᴋᴀxᴇ") + "</gradient>");
        meta.displayName(TextUtils.shadow(name).decoration(TextDecoration.BOLD, false));

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic><dark_gray><i>" + tr("Медная кирка с особыми способностями...", "A copper pickaxe with special properties...") + "</i></dark_gray>"));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><yellow>" + tr("[Смена руки] ", "[Offhand] ")
                + "</yellow><white>" + tr("Переключить добычу 3х3.", "To toggle 3x3 mining.") + "</white>"));

        meta.lore(lore.stream().map(TextUtils::shadow).toList());

        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.FORTUNE, 3, true);
        meta.setTooltipStyle(NamespacedKey.minecraft("gold"));
        meta.setUnbreakable(true);

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                KEY_DAMAGE, 5.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                KEY_SPEED, -2.8, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_IDENTIFIER, PersistentDataType.BYTE, (byte) 1);
        pdc.set(KEY_MODE, PersistentDataType.BYTE, (byte) 0);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isCopperPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.COPPER_PICKAXE || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(KEY_IDENTIFIER, PersistentDataType.BYTE);
    }

    public static boolean isModeEnabled(ItemStack item) {
        if (!isCopperPickaxe(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte value = meta.getPersistentDataContainer().get(KEY_MODE, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public static void toggleMode(ItemStack item) {
        if (!isCopperPickaxe(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte value = pdc.get(KEY_MODE, PersistentDataType.BYTE);
        byte newValue = (value != null && value == (byte) 1) ? (byte) 0 : (byte) 1;
        pdc.set(KEY_MODE, PersistentDataType.BYTE, newValue);
        item.setItemMeta(meta);
    }
}
