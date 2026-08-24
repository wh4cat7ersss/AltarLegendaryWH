package dev.whersss.altarLegendaryWH.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public final class TextUtils {

    private static final ShadowColor TEXT_SHADOW = ShadowColor.shadowColor(0xFF000000);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtils() {
    }

    public static Component shadow(Component component) {
        return component.shadowColor(TEXT_SHADOW);
    }

    public static Component legacy(String text) {
        return shadow(LEGACY.deserialize(text.replace('§', '&')));
    }

    public static Component mini(String text) {
        return shadow(MINI_MESSAGE.deserialize(text));
    }

    public static Component empty() {
        return shadow(Component.empty());
    }
    public static org.bukkit.boss.BossBar bossBar(String title, BarColor color, BarStyle style) {
        return new ShadowBossBar(title, color, style);
    }

    public static void clearBossBars() {
        ShadowBossBar.clearAll();
    }
}