package dev.whersss.altarLegendaryWH.utils;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

final class ShadowBossBar implements org.bukkit.boss.BossBar {

    private static final Set<ShadowBossBar> ACTIVE_BARS = Collections.newSetFromMap(new WeakHashMap<>());

    private final BossBar delegate;
    private final Set<Player> players = new LinkedHashSet<>();
    private final Set<BarFlag> flags = EnumSet.noneOf(BarFlag.class);
    private String title;
    private BarColor color;
    private BarStyle style;
    private boolean visible = true;

    ShadowBossBar(String title, BarColor color, BarStyle style) {
        this.title = title;
        this.color = color;
        this.style = style;
        this.delegate = BossBar.bossBar(TextUtils.legacy(title), 1.0f, adventureColor(color), adventureOverlay(style));
        ACTIVE_BARS.add(this);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
        delegate.name(TextUtils.legacy(title));
    }

    @Override
    public BarColor getColor() {
        return color;
    }

    @Override
    public void setColor(BarColor color) {
        this.color = color;
        delegate.color(adventureColor(color));
    }

    @Override
    public BarStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(BarStyle style) {
        this.style = style;
        delegate.overlay(adventureOverlay(style));
    }

    @Override
    public void removeFlag(BarFlag flag) {
        flags.remove(flag);
        delegate.removeFlag(adventureFlag(flag));
    }

    @Override
    public void addFlag(BarFlag flag) {
        flags.add(flag);
        delegate.addFlag(adventureFlag(flag));
    }

    @Override
    public boolean hasFlag(BarFlag flag) {
        return flags.contains(flag);
    }

    @Override
    public void setProgress(double progress) {
        delegate.progress((float) progress);
    }

    @Override
    public double getProgress() {
        return delegate.progress();
    }

    @Override
    public void addPlayer(Player player) {
        if (players.add(player) && visible) player.showBossBar(delegate);
    }

    @Override
    public void removePlayer(Player player) {
        if (players.remove(player)) player.hideBossBar(delegate);
    }

    @Override
    public void removeAll() {
        for (Player player : List.copyOf(players)) player.hideBossBar(delegate);
        players.clear();
    }

    @Override
    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        for (Player player : players) {
            if (visible) player.showBossBar(delegate);
            else player.hideBossBar(delegate);
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void show() {
        setVisible(true);
    }

    @Override
    public void hide() {
        setVisible(false);
    }


    static void clearAll() {
        for (ShadowBossBar bar : List.copyOf(ACTIVE_BARS)) {
            bar.removeAll();
        }
        ACTIVE_BARS.clear();
    }
    private static BossBar.Color adventureColor(BarColor color) {
        return switch (color) {
            case PINK -> BossBar.Color.PINK;
            case BLUE -> BossBar.Color.BLUE;
            case RED -> BossBar.Color.RED;
            case GREEN -> BossBar.Color.GREEN;
            case YELLOW -> BossBar.Color.YELLOW;
            case PURPLE -> BossBar.Color.PURPLE;
            case WHITE -> BossBar.Color.WHITE;
        };
    }

    private static BossBar.Overlay adventureOverlay(BarStyle style) {
        return switch (style) {
            case SOLID -> BossBar.Overlay.PROGRESS;
            case SEGMENTED_6 -> BossBar.Overlay.NOTCHED_6;
            case SEGMENTED_10 -> BossBar.Overlay.NOTCHED_10;
            case SEGMENTED_12 -> BossBar.Overlay.NOTCHED_12;
            case SEGMENTED_20 -> BossBar.Overlay.NOTCHED_20;
        };
    }

    private static BossBar.Flag adventureFlag(BarFlag flag) {
        return switch (flag) {
            case DARKEN_SKY -> BossBar.Flag.DARKEN_SCREEN;
            case PLAY_BOSS_MUSIC -> BossBar.Flag.PLAY_BOSS_MUSIC;
            case CREATE_FOG -> BossBar.Flag.CREATE_WORLD_FOG;
        };
    }
}
