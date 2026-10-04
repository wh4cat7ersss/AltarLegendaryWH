package dev.whersss.altarLegendaryWH.integrations.worldguard;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class WorldGuardManager {

    private final AltarLegendaryWH plugin;
    private static Object altarWeaponsFlag = null;
    private boolean available = false;

    public WorldGuardManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        init();
    }

    /**
     * Must be invoked during JavaPlugin#onLoad to properly register the custom flag with WorldGuard.
     */
    public static void registerFlag() {
        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstanceMethod = wgClass.getMethod("getInstance");
            Object wgInstance = getInstanceMethod.invoke(null);

            Method getFlagRegistry = wgClass.getMethod("getFlagRegistry");
            Object registry = getFlagRegistry.invoke(wgInstance);

            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            Object newFlag = stateFlagClass.getConstructor(String.class, boolean.class).newInstance("altar-weapons", true);

            Method registerMethod = registry.getClass().getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag"));
            try {
                registerMethod.invoke(registry, newFlag);
                altarWeaponsFlag = newFlag;
            } catch (Exception ex) {
                // Already registered / conflict, lookup existing
                Method getMethod = registry.getClass().getMethod("get", String.class);
                altarWeaponsFlag = getMethod.invoke(registry, "altar-weapons");
            }
        } catch (Throwable ignored) {
            // WorldGuard is not installed on the server
        }
    }

    private void init() {
        Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        available = wgPlugin != null && wgPlugin.isEnabled() && altarWeaponsFlag != null;
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Checks if abilities can be used by the player at their current location.
     */
    public boolean canUseAbilities(Player player) {
        if (!available || player == null) return true;
        return isLocationAllowed(player.getLocation(), player);
    }

    /**
     * Checks if abilities can affect a specific target location.
     */
    public boolean isLocationAllowed(Location location, Player player) {
        if (!available || location == null) return true;
        try {
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptLocMethod = bukkitAdapter.getMethod("adapt", Location.class);
            Object weLoc = adaptLocMethod.invoke(null, location);

            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

            Object associable = null;
            if (player != null) {
                Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
                Object wgPluginInst = wgPluginClass.getMethod("inst").invoke(null);
                associable = wgPluginClass.getMethod("wrapPlayer", Player.class).invoke(wgPluginInst, player);
            }

            Class<?> locationClass = Class.forName("com.sk89q.worldedit.util.Location");
            Class<?> associableClass = Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable");
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");

            Method testStateMethod = query.getClass().getMethod("testState", locationClass, associableClass, stateFlagClass.arrayType());
            Object flagsArray = java.lang.reflect.Array.newInstance(stateFlagClass, 1);
            java.lang.reflect.Array.set(flagsArray, 0, altarWeaponsFlag);

            Object result = testStateMethod.invoke(query, weLoc, associable, flagsArray);
            if (result instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Checks if a player can target or pull another entity.
     * Prevents pulling or targeting entities that are inside a protected region.
     */
    public boolean canAffectTarget(Player source, Entity target) {
        if (!available || target == null) return true;
        if (source != null && !canUseAbilities(source)) return false;
        return isLocationAllowed(target.getLocation(), source);
    }

    /**
     * Sends the localized warning message when ability usage is denied.
     */
    public void notifyDenied(Player player) {
        if (player == null) return;
        Component msg = TextUtils.shadow(Component.text(
                plugin.tr("Способности легендарного оружия отключены в этой зоне!",
                        "Legendary weapon abilities are disabled in this safe zone!"),
                NamedTextColor.RED
        ));
        player.sendActionBar(msg);
    }

    public void notifyDeniedChat(Player player) {
        if (player == null) return;
        player.sendMessage(TextUtils.legacy("§c" + plugin.tr(
                "Способности легендарного оружия отключены в этой зоне.",
                "Legendary weapon abilities are disabled in this zone."
        )));
    }
}
