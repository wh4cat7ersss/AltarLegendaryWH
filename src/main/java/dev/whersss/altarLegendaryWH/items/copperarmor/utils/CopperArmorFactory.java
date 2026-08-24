package dev.whersss.altarLegendaryWH.items.copperarmor.utils;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CopperArmorFactory {

    private static final String ORANGE = "&#E77C56";
    private static final String CYAN = "&#3EB3E6";
    private static final String RED = "&#C23636";
    private static final String YELLOW = "&#E6DB3E";

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    public static NamespacedKey getArmorKey() {
        return new NamespacedKey(AltarLegendaryWH.getInstance(), "copper_armor");
    }

    public static ItemStack getHelmet() {
        ItemStack item = new ItemStack(Material.valueOf("COPPER_HELMET"));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(c(ORANGE + tr("Медный Шлем", "ᴄᴏᴘᴘᴇʀ ʜᴇʟᴍᴇᴛ")));

        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.RESPIRATION, 3, true);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        List<Component> lore = new ArrayList<>();
        lore.add(c(""));
        lore.add(c(ORANGE + tr("Медное Зрение: &f Удерживайте Shift, чтобы дать", "Copper Sight: &f Hold Crouch to give")));
        lore.add(c(tr("&f игрокам Свечение", "&f players Glowing")));
        lore.add(c(CYAN + tr("Бесконечное Подводное Дыхание", "Infinite Water Breathing")));
        meta.lore(lore);

        applyFlags(meta);
        applyNetheriteAttributes(meta, EquipmentSlotGroup.HEAD, 3.0);
        meta.getPersistentDataContainer().set(getArmorKey(), PersistentDataType.STRING, "helmet");
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getChestplate() {
        ItemStack item = new ItemStack(Material.valueOf("COPPER_CHESTPLATE"));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(c(ORANGE + tr("Медный Нагрудник", "ᴄᴏᴘᴘᴇʀ ᴄʜᴇsᴛᴘʟᴀᴛᴇ")));

        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        List<Component> lore = new ArrayList<>();
        lore.add(c(""));
        lore.add(c(YELLOW + tr("Кольцо Молний: &f При атаке игроков, кольцо", "Ring of Lightning: &f When attacking players, a ring")));
        lore.add(c(tr("&f из молний будет появляться каждые 7 ударов", "&f of lightning appears every 7 hits")));
        lore.add(c(""));
        lore.add(c(ORANGE + tr("Иммунитет к Молниям", "Lightning Immunity")));
        lore.add(c(CYAN + tr("Бесконечное Сопротивление", "Infinite Resistance")));
        meta.lore(lore);

        applyFlags(meta);
        applyNetheriteAttributes(meta, EquipmentSlotGroup.CHEST, 8.0);
        meta.getPersistentDataContainer().set(getArmorKey(), PersistentDataType.STRING, "chestplate");
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getLeggings() {
        ItemStack item = new ItemStack(Material.valueOf("COPPER_LEGGINGS"));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(c(ORANGE + tr("Медные Поножи", "ᴄᴏᴘᴘᴇʀ ʟᴇɢɢɪɴɢs")));

        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addEnchant(Enchantment.SWIFT_SNEAK, 3, true);

        List<Component> lore = new ArrayList<>();
        lore.add(c(""));
        lore.add(c(RED + tr("Ударная Волна: &f Падение с высоты создает", "Shockwave: &f Falling from a height creates")));
        lore.add(c(tr("&f ударную волну (Работает как Булава)", "&f a shockwave (works like a mace)")));
        lore.add(c(""));
        lore.add(c(CYAN + tr("Иммунитет к Урону от Падения", "Fall Damage Immunity")));
        meta.lore(lore);

        applyFlags(meta);
        applyNetheriteAttributes(meta, EquipmentSlotGroup.LEGS, 6.0);
        meta.getPersistentDataContainer().set(getArmorKey(), PersistentDataType.STRING, "leggings");
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getBoots() {
        ItemStack item = new ItemStack(Material.valueOf("COPPER_BOOTS"));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(c(ORANGE + tr("Медные Ботинки", "ᴄᴏᴘᴘᴇʀ ʙᴏᴏᴛs")));

        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
        meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
        meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        List<Component> lore = new ArrayList<>();
        lore.add(c(""));
        lore.add(c(ORANGE + tr("Пылающий След: &f Бегите на Суперскорости", "Blazing Trail: &f Run at super speed")));
        lore.add(c(YELLOW + tr("Статический Шок: &f Бег по меди позволяет", "Static Shock: &f Running on copper lets")));
        lore.add(c(tr("&f вам бежать быстрее", "&f you run faster")));
        lore.add(c(""));
        lore.add(c(ORANGE + tr("Бесконечная Огнестойкость", "Infinite Fire Resistance")));
        meta.lore(lore);

        applyFlags(meta);
        applyNetheriteAttributes(meta, EquipmentSlotGroup.FEET, 3.0);
        meta.getPersistentDataContainer().set(getArmorKey(), PersistentDataType.STRING, "boots");
        item.setItemMeta(meta);
        return item;
    }

    private static void applyNetheriteAttributes(ItemMeta meta, EquipmentSlotGroup slot, double armorPoints) {
        meta.removeAttributeModifier(Attribute.ARMOR);
        meta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
        meta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);

        String slotName = slot.toString().toLowerCase();
        NamespacedKey armorKey = new NamespacedKey(AltarLegendaryWH.getInstance(), "copper_armor_" + slotName);
        NamespacedKey toughnessKey = new NamespacedKey(AltarLegendaryWH.getInstance(), "copper_toughness_" + slotName);
        NamespacedKey kbKey = new NamespacedKey(AltarLegendaryWH.getInstance(), "copper_kb_" + slotName);

        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(armorKey, armorPoints, AttributeModifier.Operation.ADD_NUMBER, slot));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS,
                new AttributeModifier(toughnessKey, 3.0, AttributeModifier.Operation.ADD_NUMBER, slot));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE,
                new AttributeModifier(kbKey, 0.1, AttributeModifier.Operation.ADD_NUMBER, slot));
    }

    private static void applyFlags(ItemMeta meta) {
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ARMOR_TRIM);
    }

    private static Component c(String text) {
        return TextUtils.legacy(text).decoration(TextDecoration.ITALIC, false);
    }
}
