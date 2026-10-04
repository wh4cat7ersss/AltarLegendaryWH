package dev.whersss.altarLegendaryWH.altars;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AltarManager {

    private final AltarLegendaryWH plugin;
    private final Map<Integer, Altar> altars = new HashMap<>();
    private int nextId = 1;

    private File dataFolder;
    private File altarsDataFile;
    private YamlConfiguration altarsDataConfig;

    private final NamespacedKey altarKey;

    public AltarManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        this.altarKey = new NamespacedKey(plugin, "is_altar");
        initDataFile();
    }

    private void initDataFile() {
        dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        altarsDataFile = new File(dataFolder, "altars.yml");
        if (!altarsDataFile.exists()) {
            try {
                altarsDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data/altars.yml: " + e.getMessage());
            }
        }
        altarsDataConfig = YamlConfiguration.loadConfiguration(altarsDataFile);
    }

    public FileConfiguration getDataConfig() {
        if (altarsDataConfig == null) {
            initDataFile();
        }
        return altarsDataConfig;
    }

    public void saveDataConfig() {
        try {
            if (altarsDataConfig != null && altarsDataFile != null) {
                altarsDataConfig.save(altarsDataFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data/altars.yml: " + e.getMessage());
        }
    }

    public ItemStack createAltarItem() {
        ItemStack altarItem = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemMeta meta = altarItem.getItemMeta();
        if (meta != null) {
            String titleText = plugin.getAltarLanguageManager().plain("altar.item_name");
            Component displayName = MiniMessage.miniMessage()
                    .deserialize("<gradient:#FFAA00:#FFFFFF:#FFAA00>" + titleText + "</gradient>")
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(displayName);
            meta.getPersistentDataContainer().set(altarKey, PersistentDataType.BYTE, (byte) 1);
            altarItem.setItemMeta(meta);
        }
        return altarItem;
    }

    public NamespacedKey getAltarKey() {
        return altarKey;
    }

    public Altar createAltar(Location location) {
        while (altars.containsKey(nextId)) nextId++;
        Altar altar = new Altar(nextId, location, plugin);
        altars.put(nextId, altar);
        saveAltars();
        return altar;
    }

    public Altar getAltar(int id) {
        return altars.get(id);
    }

    public Set<Integer> getAltarsIds() {
        return altars.keySet();
    }

    public Altar getAltarAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return altars.values().stream().filter(a ->
                a.getLocation() != null &&
                        Objects.equals(a.getLocation().getWorld(), location.getWorld()) &&
                        a.getLocation().getBlockX() == location.getBlockX() &&
                        a.getLocation().getBlockY() == location.getBlockY() &&
                        a.getLocation().getBlockZ() == location.getBlockZ()
        ).findFirst().orElse(null);
    }

    public void removeAltar(int id) {
        Altar altar = altars.remove(id);
        if (altar != null) {
            altar.removeAllEntities();
            getDataConfig().set("altars." + id, null);
            saveDataConfig();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadAltars() {
        altars.clear();
        ConfigurationSection sec = getDataConfig().getConfigurationSection("altars");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                Location loc = sec.getLocation(key + ".location");
                if (loc == null) continue;

                Altar altar = new Altar(id, loc, plugin);
                if (sec.contains(key + ".result")) altar.setResultItem(sec.getItemStack(key + ".result"));
                if (sec.contains(key + ".recipe")) altar.setRecipe((List<ItemStack>) sec.getList(key + ".recipe"));
                if (sec.contains(key + ".crafted")) altar.setCrafted(sec.getBoolean(key + ".crafted"));

                altar.setMaxUses(sec.getInt(key + ".max_uses", 1));
                altar.setCurrentUses(sec.getInt(key + ".current_uses", altar.getMaxUses()));
                altar.setCooldownSeconds(sec.getInt(key + ".cooldown", 0));
                altar.setLastUsedTime(sec.getLong(key + ".last_used", 0));

                for (String type : List.of("title", "recipe", "item")) {
                    for (String axis : List.of("x", "y", "z")) {
                        String offsetKey = key + ".display." + type + ".offset." + axis;
                        if (sec.contains(offsetKey)) {
                            altar.setCustomOffset(type, axis, sec.getDouble(offsetKey));
                        }
                        String scaleKey = key + ".display." + type + ".scale." + axis;
                        if (sec.contains(scaleKey)) {
                            altar.setCustomScale(type, axis, (float) sec.getDouble(scaleKey));
                        }
                    }
                }

                altars.put(id, altar);
                altar.spawnEntities();
                nextId = Math.max(nextId, id + 1);
            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getAltarLanguageManager().plain("log.failed_load_altar", "id", key));
            }
        }
    }

    public void saveAltars() {
        FileConfiguration config = getDataConfig();
        for (Altar altar : altars.values()) {
            String path = "altars." + altar.getId();

            config.set(path + ".location", altar.getLocation());
            config.set(path + ".result", altar.getResultItem());
            config.set(path + ".recipe", altar.getRecipe());
            config.set(path + ".crafted", altar.isCrafted());

            config.set(path + ".max_uses", altar.getMaxUses());
            config.set(path + ".current_uses", altar.getCurrentUses());
            config.set(path + ".cooldown", altar.getCooldownSeconds());
            config.set(path + ".last_used", altar.getLastUsedTime());

            for (Map.Entry<String, Double> entry : altar.getCustomOffsets().entrySet()) {
                config.set(path + ".display." + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Float> entry : altar.getCustomScales().entrySet()) {
                config.set(path + ".display." + entry.getKey(), entry.getValue());
            }
        }
        saveDataConfig();
    }

    public void cleanupEntities() {
        altars.values().forEach(Altar::removeAllEntities);
    }
}
