package dev.whersss.altarLegendaryWH.altars;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Altar {
    private final int id;
    private Location location;
    private final AltarLegendaryWH plugin;

    private ItemStack resultItem;
    private List<ItemStack> recipe = new ArrayList<>();
    private boolean isCrafted = false;

    private int maxUses = 1;
    private int currentUses = 1;
    private int cooldownSeconds = 0;
    private long lastUsedTime = 0;

    private UUID itemEntityId;
    private UUID titleEntityId;
    private UUID recipeEntityId;
    private UUID blockDisplayId;

    private float currentRotation = 0.0f;

    private final Map<String, Double> customOffsets = new HashMap<>();
    private final Map<String, Float> customScales = new HashMap<>();

    public Altar(int id, Location location, AltarLegendaryWH plugin) {
        this.id = id;
        this.location = location;
        this.plugin = plugin;
    }

    public void spawnEntities() {
        removeHolograms();

        if (location == null || location.getWorld() == null) return;
        FileConfiguration config = plugin.getAltarsConfig();
        Display.Billboard billboard = Display.Billboard.valueOf(config.getString("settings.display.billboard", "VERTICAL"));
        NamespacedKey tagKey = new NamespacedKey(plugin, "altar_entity");

        if (blockDisplayId == null || Bukkit.getEntity(blockDisplayId) == null) {
            Location blockLoc = location.clone().add(0, 0.001, 0);
            BlockDisplay blockDisplay = location.getWorld().spawn(blockLoc, BlockDisplay.class, (display) -> {
                display.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
            });
            blockDisplay.setBlock(Bukkit.createBlockData(Material.STRUCTURE_BLOCK));
            this.blockDisplayId = blockDisplay.getUniqueId();
        }

        if (resultItem == null || (isCrafted && maxUses != -1 && currentUses <= 0)) return;
        long cd = getCooldownRemaining();
        if (cd > 0) {
            updateRecipeDisplay();
            return;
        }

        Location itemLoc = location.clone().add(
                getOffset("item", "x", 0.5),
                getOffset("item", "y", 1.5),
                getOffset("item", "z", 0.5));

        org.bukkit.entity.Item droppedItem = location.getWorld().spawn(itemLoc, org.bukkit.entity.Item.class, (item) -> {
            item.setItemStack(resultItem.clone());
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setCanMobPickup(false);
            item.setGravity(false);
            item.setInvulnerable(true);
            item.setUnlimitedLifetime(true);
            item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            item.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
        });
        this.itemEntityId = droppedItem.getUniqueId();

        Location titleLoc = location.clone().add(
                getOffset("title", "x", 0.5),
                getOffset("title", "y", 3.2),
                getOffset("title", "z", 0.5));

        TextDisplay titleDisplay = location.getWorld().spawn(titleLoc, TextDisplay.class, (display) -> {
            display.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
        });
        titleDisplay.setBillboard(billboard);
        titleDisplay.text(getItemDisplayName(resultItem));
        titleDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        titleDisplay.setShadowed(true);

        applyScale(titleDisplay, "title");
        this.titleEntityId = titleDisplay.getUniqueId();
        updateRecipeDisplay();
    }

    public void rotateItemDisplay() {
        if (itemEntityId == null) return;
        org.bukkit.entity.Entity entity = Bukkit.getEntity(itemEntityId);

        if (!(entity instanceof ItemDisplay display)) return;

        currentRotation += 0.05f;
        if (currentRotation >= (float) (Math.PI * 2)) {
            currentRotation = 0.0f;
        }

        Transformation trans = display.getTransformation();
        Quaternionf rot = new Quaternionf(new AxisAngle4f(currentRotation, 0, 1, 0));
        display.setTransformation(new Transformation(
                trans.getTranslation(),
                rot,
                trans.getScale(),
                trans.getLeftRotation()
        ));
    }

    public void updateRecipeDisplay() {
        if (recipeEntityId != null) {
            org.bukkit.entity.Entity e = Bukkit.getEntity(recipeEntityId);
            if (e != null) e.remove();
            recipeEntityId = null;
        }
        if (location == null || location.getWorld() == null || (isCrafted && maxUses != -1 && currentUses <= 0)) return;

        long cd = getCooldownRemaining();
        if (recipe.isEmpty() && cd == 0) return;

        FileConfiguration config = plugin.getAltarsConfig();
        Display.Billboard billboard = Display.Billboard.valueOf(config.getString("settings.display.billboard", "VERTICAL"));
        NamespacedKey tagKey = new NamespacedKey(plugin, "altar_entity");

        Location recipeLoc = location.clone().add(
                getOffset("recipe", "x", 0.5),
                getOffset("recipe", "y", 3.5),
                getOffset("recipe", "z", 0.5));

        TextDisplay recipeDisplay = location.getWorld().spawn(recipeLoc, TextDisplay.class, (display) -> {
            display.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
        });
        recipeDisplay.setBillboard(billboard);
        recipeDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        recipeDisplay.setShadowed(true);

        if (cd > 0) {
            recipeDisplay.text(plugin.getAltarLanguageManager().component("altar.cooldown", "seconds", String.valueOf(cd)));
        } else {
            net.kyori.adventure.text.TextComponent.Builder builder = Component.text();
            for (int i = 0; i < recipe.size(); i++) {
                ItemStack req = recipe.get(i);
                builder.append(plugin.getAltarLanguageManager().component("altar.recipe_amount",
                                "amount", String.valueOf(req.getAmount())))
                        .append(getItemDisplayName(req));
                if (i < recipe.size() - 1) builder.append(Component.newline());
            }
            recipeDisplay.text(builder.build());
        }

        applyScale(recipeDisplay, "recipe");
        this.recipeEntityId = recipeDisplay.getUniqueId();
    }

    public long getCooldownRemaining() {
        if (lastUsedTime == 0) return 0;
        long end = lastUsedTime + (cooldownSeconds * 1000L);
        long now = System.currentTimeMillis();
        return Math.max(0, (end - now) / 1000L);
    }

    public static Component getItemDisplayName(ItemStack item) {
        if (item == null) return Component.empty();
        if (item.hasItemMeta()) {
            if (item.getItemMeta().hasDisplayName()) return item.getItemMeta().displayName();
            if (item.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.hasOwner()) {
                String ownerName = skullMeta.getOwningPlayer() != null ? skullMeta.getOwningPlayer().getName() : "Player";
                return Component.text("Голова " + (ownerName != null ? ownerName : "Player"));
            }
        }
        return Component.translatable(item.getType().translationKey());
    }

    public double getOffset(String type, String axis, double def) {
        String key = type + ".offset." + axis;
        if (customOffsets.containsKey(key)) {
            return customOffsets.get(key);
        }
        return plugin.getAltarsConfig().getDouble("settings.display." + type + ".offset." + axis, def);
    }

    public void setCustomOffset(String type, String axis, double value) {
        customOffsets.put(type + ".offset." + axis, value);
    }

    public Map<String, Double> getCustomOffsets() {
        return customOffsets;
    }

    public float getScale(String type, String axis, double def) {
        String key = type + ".scale." + axis;
        if (customScales.containsKey(key)) {
            return customScales.get(key);
        }
        return (float) plugin.getAltarsConfig().getDouble("settings.display." + type + ".scale." + axis, def);
    }

    public void setCustomScale(String type, String axis, float value) {
        customScales.put(type + ".scale." + axis, value);
    }

    public Map<String, Float> getCustomScales() {
        return customScales;
    }

    private void applyScale(Display display, String path) {
        float sx = getScale(path, "x", 1.0f);
        float sy = getScale(path, "y", 1.0f);
        float sz = getScale(path, "z", 1.0f);
        Transformation trans = display.getTransformation();
        trans.getScale().set(new Vector3f(sx, sy, sz));
        display.setTransformation(trans);
    }

    public void removeAllEntities() {
        removeHolograms();
        if (blockDisplayId != null) {
            org.bukkit.entity.Entity e = Bukkit.getEntity(blockDisplayId);
            if (e != null) e.remove();
            blockDisplayId = null;
        }
    }

    public void removeHolograms() {
        UUID[] ids = {itemEntityId, titleEntityId, recipeEntityId};
        for (UUID uuid : ids) {
            if (uuid != null) {
                org.bukkit.entity.Entity e = Bukkit.getEntity(uuid);
                if (e != null) e.remove();
            }
        }
        itemEntityId = null;
        titleEntityId = null;
        recipeEntityId = null;

        if (location != null && location.getWorld() != null) {
            NamespacedKey tagKey = new NamespacedKey(plugin, "altar_entity");
            location.getWorld().getNearbyEntities(location, 2.0, 4.0, 2.0).forEach(e -> {
                if (e.getPersistentDataContainer().has(tagKey, PersistentDataType.BYTE)) {
                    if (!(e instanceof BlockDisplay)) {
                        e.remove();
                    }
                }
            });
        }
    }

    public int getId() { return id; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public ItemStack getResultItem() { return resultItem; }
    public void setResultItem(ItemStack resultItem) { this.resultItem = resultItem; this.isCrafted = false; }
    public List<ItemStack> getRecipe() { return recipe; }
    public void setRecipe(List<ItemStack> recipe) { this.recipe = (recipe != null) ? recipe : new ArrayList<>(); }
    public void addRecipeItem(ItemStack item) { this.recipe.add(item); }
    public boolean isCrafted() { return isCrafted; }
    public void setCrafted(boolean crafted) { this.isCrafted = crafted; }

    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; this.currentUses = maxUses; }
    public int getCurrentUses() { return currentUses; }
    public void setCurrentUses(int currentUses) { this.currentUses = currentUses; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public long getLastUsedTime() { return lastUsedTime; }
    public void setLastUsedTime(long lastUsedTime) { this.lastUsedTime = lastUsedTime; }
}
