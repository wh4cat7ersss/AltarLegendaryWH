package dev.whersss.altarLegendaryWH.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtils {

    private static final boolean HAS_COMPONENT_META;

    static {
        boolean has = false;
        try {
            ItemMeta.class.getMethod("displayName", Component.class);
            has = true;
        } catch (Throwable ignored) {
        }
        HAS_COMPONENT_META = has;
    }

    private ItemUtils() {
    }

    public static void setDisplayName(ItemMeta meta, Component component) {
        if (meta == null || component == null) return;
        if (HAS_COMPONENT_META) {
            try {
                meta.displayName(component);
                return;
            } catch (Throwable ignored) {
            }
        }
        meta.setDisplayName(TextUtils.toLegacy(component));
    }

    public static void setLore(ItemMeta meta, List<Component> lore) {
        if (meta == null) return;
        if (HAS_COMPONENT_META) {
            try {
                meta.lore(lore);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (lore == null) {
            meta.setLore(null);
        } else {
            List<String> list = new ArrayList<>(lore.size());
            for (Component c : lore) {
                list.add(TextUtils.toLegacy(c));
            }
            meta.setLore(list);
        }
    }
}
