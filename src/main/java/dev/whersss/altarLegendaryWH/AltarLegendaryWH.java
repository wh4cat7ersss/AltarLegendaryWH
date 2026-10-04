package dev.whersss.altarLegendaryWH;

import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;

import dev.whersss.altarLegendaryWH.commands.AltarLegendaryCommand;
import dev.whersss.altarLegendaryWH.commands.AltarLegendaryTabCompleter;
import dev.whersss.altarLegendaryWH.commands.FriendListCommand;
import dev.whersss.altarLegendaryWH.commands.FriendListTabCompleter;
import dev.whersss.altarLegendaryWH.listeners.MenuListener;
import dev.whersss.altarLegendaryWH.listeners.ChatInputListener;

import dev.whersss.altarLegendaryWH.managers.FriendManager;
import dev.whersss.altarLegendaryWH.utils.CleanDamageManager;

import dev.whersss.altarLegendaryWH.weapons.boneblade.managers.BoneCooldownManager;
import dev.whersss.altarLegendaryWH.weapons.bloodlust.managers.BloodAbilityManager;
import dev.whersss.altarLegendaryWH.weapons.bloodlust.managers.BloodBossBarManager;
import dev.whersss.altarLegendaryWH.weapons.knightfall.managers.KnightfallManager;
import dev.whersss.altarLegendaryWH.weapons.knightfall.tasks.KnightfallPassiveTask;
import dev.whersss.altarLegendaryWH.weapons.nightpiercer.listeners.NightAbilityListener;
import dev.whersss.altarLegendaryWH.weapons.nightpiercer.managers.NightBossBarCooldown;
import dev.whersss.altarLegendaryWH.weapons.nightpiercer.managers.NightHealthStealManager;
import dev.whersss.altarLegendaryWH.weapons.vulcan.managers.VulcanChargeManager;
import dev.whersss.altarLegendaryWH.weapons.vulcan.managers.VulcanCooldownManager;
import dev.whersss.altarLegendaryWH.weapons.frostscythe.managers.FrostBossBarManager;
import dev.whersss.altarLegendaryWH.weapons.frostscythe.managers.FrostAbilityManager;
import dev.whersss.altarLegendaryWH.weapons.frostscythe.listeners.FrostListener;
import dev.whersss.altarLegendaryWH.weapons.palegun.listeners.PaleGunListener;
import dev.whersss.altarLegendaryWH.weapons.palegun.managers.PaleGunAbilityManager;
import dev.whersss.altarLegendaryWH.weapons.pureblade.managers.PureBladeManager;
import dev.whersss.altarLegendaryWH.weapons.pureblade.listeners.PureBladeListener;
import dev.whersss.altarLegendaryWH.weapons.pureblade.managers.PureBossBarManager;

import dev.whersss.altarLegendaryWH.weapons.shadowblade.managers.ShadowBladeManager;
import dev.whersss.altarLegendaryWH.weapons.shadowblade.listeners.ShadowBladeListener;
import dev.whersss.altarLegendaryWH.weapons.shadowblade.tasks.ShadowBladePassiveTask;
import dev.whersss.altarLegendaryWH.weapons.windweaver.listeners.WindWeaverListener;

import dev.whersss.altarLegendaryWH.weapons.hyperion.listeners.HyperionListener;
import dev.whersss.altarLegendaryWH.weapons.hyperion.managers.HyperionCooldownManager;

import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import dev.whersss.altarLegendaryWH.weapons.witherblade.managers.WitherManager;
import dev.whersss.altarLegendaryWH.weapons.witherblade.listeners.WitherBladeListener;

import dev.whersss.altarLegendaryWH.weapons.crazyslots.managers.CrazySlotsManager;
import dev.whersss.altarLegendaryWH.weapons.crazyslots.listeners.CrazySlotsListener;

import dev.whersss.altarLegendaryWH.weapons.earthgauntlet.managers.EarthGauntletManager;
import dev.whersss.altarLegendaryWH.weapons.earthgauntlet.listeners.EarthGauntletListener;

import dev.whersss.altarLegendaryWH.weapons.cutlass.managers.CutlassManager;
import dev.whersss.altarLegendaryWH.weapons.cutlass.listeners.CutlassListener;

import dev.whersss.altarLegendaryWH.items.copperarmor.listeners.CopperArmorListener;
import dev.whersss.altarLegendaryWH.items.copperarmor.tasks.CopperArmorTask;

import dev.whersss.altarLegendaryWH.items.copperpickaxe.listeners.CopperPickaxeListener;

import dev.whersss.altarLegendaryWH.items.weaponshandle.WeaponsHandleItem;
import dev.whersss.altarLegendaryWH.items.illusioncore.IllusionCoreItem;
import dev.whersss.altarLegendaryWH.items.vulcanskull.VulcanSkullItem;
import dev.whersss.altarLegendaryWH.altars.Altar;
import dev.whersss.altarLegendaryWH.altars.AltarManager;
import dev.whersss.altarLegendaryWH.altars.AltarLanguageManager;
import dev.whersss.altarLegendaryWH.altars.commands.EditAltarCommand;
import dev.whersss.altarLegendaryWH.altars.listeners.AltarListener;
import dev.whersss.altarLegendaryWH.altars.listeners.AltarBlockListener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.Locale;

import dev.whersss.altarLegendaryWH.weapons.paladinsbattleaxe.managers.PaladinsBattleAxeManager;
import dev.whersss.altarLegendaryWH.weapons.paladinsbattleaxe.listeners.PaladinsBattleAxeListener;
import dev.whersss.altarLegendaryWH.integrations.worldguard.WorldGuardManager;
import dev.whersss.altarLegendaryWH.integrations.worldguard.WorldGuardAbilityGate;
import dev.whersss.altarLegendaryWH.managers.VisualCleanupManager;

public class AltarLegendaryWH extends JavaPlugin {
    @Override
    public void onLoad() {
        WorldGuardManager.registerFlag();
    }


    private static AltarLegendaryWH instance;

    private NamespacedKey killsKey;
    private NamespacedKey vulcanKey;
    private NamespacedKey arrowKey;
    private NamespacedKey paleGunKey;
    private NamespacedKey hyperionKey;
    private NamespacedKey witherKey;
    private NamespacedKey paladinsBattleAxeKey;
    private NamespacedKey knightfallTierKey;
    private PaladinsBattleAxeManager paladinsBattleAxeManager;
    private PaladinsBattleAxeListener paladinsBattleAxeListener;
    private CrazySlotsManager crazySlotsManager;
    private CrazySlotsListener crazySlotsListener;
    private WorldGuardManager worldGuardManager;
    private VisualCleanupManager visualCleanupManager;
    private EditAltarCommand editAltarCommand;


    private FriendManager friendManager;
    private CleanDamageManager cleanDamageManager;
    private EarthGauntletManager earthGauntletManager;
    private CutlassManager cutlassManager;

    private BoneCooldownManager boneCooldownManager;
    private BloodAbilityManager bloodAbilityManager;
    private BloodBossBarManager bloodBossBarManager;
    private NightAbilityListener nightAbilityListener;
    private NightHealthStealManager nightHealthManager;
    private FrostBossBarManager frostBossBarManager;
    private FrostAbilityManager frostAbilityManager;
    private PaleGunAbilityManager paleGunAbilityManager;
    private KnightfallManager knightfallManager;
    private PureBossBarManager pureBossBarManager;
    private PureBladeManager pureBladeManager;
    private ShadowBladeManager shadowBladeManager;
    private WindWeaverListener windWeaverListener;
    private CopperArmorTask copperArmorTask;
    private WitherManager witherManager;
    private File weaponsFile;
    private FileConfiguration weaponsConfig;
    private File itemsFile;
    private FileConfiguration itemsConfig;
    private File altarsConfigFile;
    private FileConfiguration altarsConfig;
    private File modelsFile;
    private FileConfiguration modelsConfig;
    private AltarManager altarManager;
    private AltarLanguageManager altarLanguageManager;
    private final java.util.Map<Integer, Boolean> wasAltarOnCooldown = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveDefaultWeaponsConfig();
        saveDefaultItemsConfig();
        saveDefaultAltarsConfig();
        saveDefaultModelsConfig();

        killsKey = new NamespacedKey(this, "bloodlust_kills");
        vulcanKey = new NamespacedKey(this, "vulcan_crossbow");
        arrowKey = new NamespacedKey(this, "vulcan_arrow");
        paleGunKey = new NamespacedKey(this, "pale_gun");
        hyperionKey = new NamespacedKey(this, "hyperion_sword");
        witherKey = new NamespacedKey(this, "wither_blade");
        knightfallTierKey = new NamespacedKey(this, "knightfall_tier");
        visualCleanupManager = new VisualCleanupManager(this);
        worldGuardManager = new WorldGuardManager(this);
        getServer().getPluginManager().registerEvents(new WorldGuardAbilityGate(this, worldGuardManager), this);

        paladinsBattleAxeKey = new NamespacedKey(this, "paladins_battle_axe");
        paladinsBattleAxeManager = new PaladinsBattleAxeManager(this);
        paladinsBattleAxeListener = new PaladinsBattleAxeListener(this, paladinsBattleAxeManager);
        getServer().getPluginManager().registerEvents(paladinsBattleAxeListener, this);

        crazySlotsManager = new CrazySlotsManager(this);
        crazySlotsListener = new CrazySlotsListener(this, crazySlotsManager);
        getServer().getPluginManager().registerEvents(crazySlotsListener, this);

        friendManager = new FriendManager(this);
        cleanDamageManager = new CleanDamageManager(this);
        earthGauntletManager = new EarthGauntletManager(this);
        cutlassManager = new CutlassManager(this);

        boneCooldownManager = new BoneCooldownManager(this);
        bloodBossBarManager = new BloodBossBarManager(this);
        bloodAbilityManager = new BloodAbilityManager(this, bloodBossBarManager);
        nightAbilityListener = new NightAbilityListener(this);
        nightHealthManager = new NightHealthStealManager(this);
        knightfallManager = new KnightfallManager(this);
        shadowBladeManager = new ShadowBladeManager(this);
        witherManager = new WitherManager(this);
        windWeaverListener = new WindWeaverListener(this);

        frostBossBarManager = new FrostBossBarManager(this);
        frostAbilityManager = new FrostAbilityManager(this, frostBossBarManager);
        paleGunAbilityManager = new PaleGunAbilityManager(this);

        pureBossBarManager = new PureBossBarManager(this);
        pureBladeManager = new PureBladeManager(this, pureBossBarManager);

        PluginCommand cmd = getCommand("altarlegendary");
        if (cmd != null) {
            cmd.setExecutor(new AltarLegendaryCommand(this));
            cmd.setTabCompleter(new AltarLegendaryTabCompleter(this));
        }

        PluginCommand friendCmd = getCommand("friendlist");
        if (friendCmd != null) {
            friendCmd.setExecutor(new FriendListCommand(this));
            friendCmd.setTabCompleter(new FriendListTabCompleter(this));
        }

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.whersss.altarLegendaryWH.weapons.boneblade.listeners.BladeListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.whersss.altarLegendaryWH.weapons.bloodlust.listeners.BloodLustListener(this, bloodAbilityManager), this);
        new dev.whersss.altarLegendaryWH.weapons.bloodlust.tasks.PassiveAuraTask(this).runTaskTimer(this, 0L, 1L);
        getServer().getPluginManager().registerEvents(nightAbilityListener, this);
        new dev.whersss.altarLegendaryWH.weapons.nightpiercer.tasks.NightPassiveTask().runTaskTimer(this, 0L, 20L);
        getServer().getPluginManager().registerEvents(new dev.whersss.altarLegendaryWH.weapons.vulcan.listeners.VulcanShootListener(), this);
        getServer().getPluginManager().registerEvents(new dev.whersss.altarLegendaryWH.weapons.vulcan.listeners.VulcanHitListener(), this);
        getServer().getPluginManager().registerEvents(new PaleGunListener(this, paleGunAbilityManager), this);
        getServer().getPluginManager().registerEvents(new dev.whersss.altarLegendaryWH.listeners.ItemProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new FrostListener(frostAbilityManager), this);
        getServer().getPluginManager().registerEvents(new PureBladeListener(pureBladeManager), this);
        getServer().getPluginManager().registerEvents(new dev.whersss.altarLegendaryWH.weapons.knightfall.listeners.KnightfallListener(this, knightfallManager), this);
        new KnightfallPassiveTask(this).runTaskTimer(this, 0L, 20L);
        getServer().getPluginManager().registerEvents(new ShadowBladeListener(this, shadowBladeManager), this);
        new ShadowBladePassiveTask().runTaskTimer(this, 0L, 2L);
        getServer().getPluginManager().registerEvents(windWeaverListener, this);
        getServer().getPluginManager().registerEvents(new dev.whersss.altarLegendaryWH.items.wardenheart.listeners.WardenHeartDropListener(this), this);

        getServer().getPluginManager().registerEvents(new HyperionListener(), this);

        getServer().getPluginManager().registerEvents(new CopperArmorListener(this), this);
        copperArmorTask = new CopperArmorTask(this);
        getServer().getPluginManager().registerEvents(copperArmorTask, this);
        copperArmorTask.runTaskTimer(this, 0L, 1L);

        getServer().getPluginManager().registerEvents(new WitherBladeListener(this, witherManager), this);

        getServer().getPluginManager().registerEvents(new EarthGauntletListener(this, earthGauntletManager), this);

        getServer().getPluginManager().registerEvents(new CutlassListener(cutlassManager), this);

        getServer().getPluginManager().registerEvents(new CopperPickaxeListener(this), this);

        altarLanguageManager = new AltarLanguageManager(this);
        altarLanguageManager.reload();
        cleanupPhantomAltarEntities();

        altarManager = new AltarManager(this);
        altarManager.loadAltars();

        editAltarCommand = new EditAltarCommand(this);

        getServer().getPluginManager().registerEvents(new AltarListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarBlockListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (altarManager == null) return;
            for (Integer id : altarManager.getAltarsIds()) {
                Altar altar = altarManager.getAltar(id);
                if (altar == null) continue;

                altar.rotateItemDisplay();

                long remaining = altar.getCooldownRemaining();
                boolean currentlyOnCooldown = remaining > 0;

                if (currentlyOnCooldown) {
                    altar.updateRecipeDisplay();
                } else if (wasAltarOnCooldown.getOrDefault(id, false)) {
                    altar.spawnEntities();
                    handleAltarCooldownEnd(altar);
                }
                wasAltarOnCooldown.put(id, currentlyOnCooldown);
            }
        }, 0L, 1L);

        registerRecipes();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : getServer().getOnlinePlayers()) {
                    ItemStack item = p.getInventory().getItemInMainHand();
                    if (WeaponFactory.isAltarWeapon(item, "knightfall")) {
                        int kills = item.getItemMeta().getPersistentDataContainer().getOrDefault(killsKey, PersistentDataType.INTEGER, 0);
                        int cmd = item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 0;
                        boolean hasDensity = item.getItemMeta().hasEnchant(Enchantment.DENSITY);
                        int tier = item.getItemMeta().getPersistentDataContainer().getOrDefault(knightfallTierKey, PersistentDataType.INTEGER,
                                (cmd >= 3003 || cmd == 3) ? 3 : (hasDensity ? 2 : ((cmd >= 3002 || cmd == 2) ? 1 : 0)));

                        if ((kills >= 4 && tier < 1) || (kills >= 8 && tier < 2) || (kills >= 10 && tier < 3)) {
                            p.sendActionBar(TextUtils.legacy(tr("&aНажмите &2[Смена руки] &aчтобы улучшить оружие.", "&aPress &2[OffHand] &ato upgrade the weapon.")));
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        getLogger().info("AltarLegendaryWH do 676767");
    }

    public void registerRecipes() {
        try {
            registerConfiguredRecipe(
                    new NamespacedKey(this, "weapons_handle"),
                    WeaponsHandleItem.create(),
                    "items.recipes.weapon-handle",
                    new String[]{" N ", " S ", " N "},
                    new char[]{'N', 'S'},
                    new Material[]{Material.NETHERITE_INGOT, Material.NETHER_STAR}
            );
            registerConfiguredRecipe(
                    new NamespacedKey(this, "illusion_core"),
                    IllusionCoreItem.create(),
                    "items.recipes.illusion-core",
                    new String[]{"DSD", "SHS", "DSD"},
                    new char[]{'D', 'S', 'H'},
                    new Material[]{Material.ANCIENT_DEBRIS, Material.SKELETON_SKULL, Material.HEAVY_CORE}
            );
            registerConfiguredRecipe(
                    new NamespacedKey(this, "vulcan_skull"),
                    VulcanSkullItem.create(),
                    "items.recipes.vulcan-skull",
                    new String[]{"DMD", "MSM", "DMD"},
                    new char[]{'D', 'M', 'S'},
                    new Material[]{Material.ANCIENT_DEBRIS, Material.MAGMA_BLOCK, Material.WITHER_SKELETON_SKULL}
            );
        } catch (IllegalStateException ignored) {
        }
    }

    private void registerConfiguredRecipe(NamespacedKey key, ItemStack result, String path, String[] defaultShape, char[] ingredientKeys, Material[] defaultMaterials) {
        Bukkit.removeRecipe(key);
        if (!getItemsConfig().getBoolean(path + ".enabled", true)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        List<String> shapeList = getItemsConfig().getStringList(path + ".shape");
        String[] shape = shapeList.size() == 3 ? shapeList.toArray(new String[0]) : defaultShape;
        recipe.shape(shape);

        for (int i = 0; i < ingredientKeys.length; i++) {
            char symbol = ingredientKeys[i];
            Material material = resolveMaterial(getItemsConfig().getString(path + ".ingredients." + symbol), defaultMaterials[i]);
            recipe.setIngredient(symbol, material);
        }

        Bukkit.addRecipe(recipe);
    }

    public void saveDefaultWeaponsConfig() {
        weaponsFile = new File(getDataFolder(), "weapons.yml");
        if (!weaponsFile.exists()) {
            saveResource("weapons.yml", false);
        }
        weaponsConfig = YamlConfiguration.loadConfiguration(weaponsFile);
    }

    public void reloadWeaponsConfig() {
        if (weaponsFile == null) {
            weaponsFile = new File(getDataFolder(), "weapons.yml");
        }
        weaponsConfig = YamlConfiguration.loadConfiguration(weaponsFile);
    }

    public FileConfiguration getWeaponsConfig() {
        if (weaponsConfig == null) {
            reloadWeaponsConfig();
        }
        return weaponsConfig;
    }

    public void saveDefaultItemsConfig() {
        itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    public void reloadItemsConfig() {
        if (itemsFile == null) {
            itemsFile = new File(getDataFolder(), "items.yml");
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    public FileConfiguration getItemsConfig() {
        if (itemsConfig == null) {
            reloadItemsConfig();
        }
        return itemsConfig;
    }

    private Material resolveMaterial(String configuredName, Material fallback) {
        if (configuredName == null || configuredName.isBlank()) {
            return fallback;
        }

        Material material = Material.matchMaterial(configuredName.toUpperCase(Locale.ROOT));
        return material != null ? material : fallback;
    }

    @Override
    public void onDisable() {
        if (boneCooldownManager != null) boneCooldownManager.removeAllBars();
        if (bloodBossBarManager != null) bloodBossBarManager.clearAll();
        if (bloodAbilityManager != null) bloodAbilityManager.cleanup();
        if (nightHealthManager != null) nightHealthManager.revertAll();
        NightBossBarCooldown.clearAll();
        if (pureBossBarManager != null) pureBossBarManager.clearAll();
        if (frostBossBarManager != null) frostBossBarManager.clearAll();
        if (paleGunAbilityManager != null) paleGunAbilityManager.clearAll();
        if (windWeaverListener != null) windWeaverListener.removeAllBars();
        if (knightfallManager != null) knightfallManager.clearAllBars();
        if (shadowBladeManager != null) shadowBladeManager.clearAllBars();
        if (witherManager != null) witherManager.clearAllBars();
        if (earthGauntletManager != null) earthGauntletManager.cleanupAll();
        if (friendManager != null) friendManager.saveFriends();
        if (cutlassManager != null) cutlassManager.cleanupAll();
        if (visualCleanupManager != null) visualCleanupManager.cleanupAll();
        if (paladinsBattleAxeManager != null) paladinsBattleAxeManager.clearAllBars();
        if (crazySlotsManager != null) crazySlotsManager.forceRevertAll();

        if (cleanDamageManager != null) cleanDamageManager.clear();
        if (copperArmorTask != null) copperArmorTask.cleanup();
        TextUtils.clearBossBars();

        VulcanCooldownManager.clearAllBars();
        VulcanChargeManager.cancelAll();

        HyperionCooldownManager.clearAllBars();

        if (altarManager != null) {
            altarManager.saveAltars();
            altarManager.cleanupEntities();
        }

        getLogger().info("AltarLegendaryWH выключен!");
    }

    public static AltarLegendaryWH getInstance() { return instance; }
    public NamespacedKey getKillsKey() { return killsKey; }
    public NamespacedKey getVulcanKey() { return vulcanKey; }
    public NamespacedKey getArrowKey() { return arrowKey; }
    public NamespacedKey getPaleGunKey() { return paleGunKey; }
    public NamespacedKey getHyperionKey() { return hyperionKey; }
    public NamespacedKey getWitherKey() { return witherKey; }
    public NamespacedKey getKnightfallTierKey() { return knightfallTierKey; }

    public FriendManager getFriendManager() { return friendManager; }
    public CleanDamageManager getCleanDamageManager() { return cleanDamageManager; }
    public EarthGauntletManager getEarthGauntletManager() { return earthGauntletManager; }
    public CutlassManager getCutlassManager() { return cutlassManager; }

    public BoneCooldownManager getBoneCooldownManager() { return boneCooldownManager; }
    public BloodAbilityManager getBloodAbilityManager() { return bloodAbilityManager; }
    public BloodBossBarManager getBloodBossBarManager() { return bloodBossBarManager; }
    public NightAbilityListener getNightAbilityListener() { return nightAbilityListener; }
    public NightHealthStealManager getNightHealthManager() { return nightHealthManager; }
    public PureBladeManager getPureBladeManager() { return pureBladeManager; }
    public WindWeaverListener getWindWeaverListener() { return windWeaverListener; }

    public boolean isEnglish() {
        return "en_US".equalsIgnoreCase(getConfig().getString("lang", "en_US"));
    }

    public String tr(String ru, String en) {
        return isEnglish() ? en : ru;
    }

    public boolean isLimitsEnabled() {
        return getConfig().getBoolean("limits.enabled", false);
    }

    public double getLegendaryMaxY() {
        return getConfig().getDouble("limits.max-y-height", 200.0);
    }

    public boolean isAboveLegendaryHeight(Player player) {
        return isLimitsEnabled() && player.getLocation().getY() > getLegendaryMaxY();
    }
    public void resetAllCooldowns(Player player) {
        if (boneCooldownManager != null) boneCooldownManager.resetPlayer(player);
        if (bloodBossBarManager != null) bloodBossBarManager.resetPlayer(player);
        if (nightAbilityListener != null) nightAbilityListener.resetCooldowns(player);
        NightBossBarCooldown.resetPlayer(player);
        if (frostBossBarManager != null) frostBossBarManager.resetPlayer(player);
        if (paleGunAbilityManager != null) paleGunAbilityManager.resetPlayer(player);
        if (pureBladeManager != null) pureBladeManager.resetCooldowns(player);
        if (knightfallManager != null) knightfallManager.resetCooldowns(player);
        if (shadowBladeManager != null) shadowBladeManager.resetCooldowns(player);
        if (witherManager != null) witherManager.resetCooldowns(player);
        if (earthGauntletManager != null) earthGauntletManager.resetCooldowns(player);
        if (cutlassManager != null) cutlassManager.resetCooldowns(player);
        if (windWeaverListener != null) windWeaverListener.resetPlayer(player);
        VulcanCooldownManager.resetPlayer(player);
        HyperionCooldownManager.resetPlayer(player);
        if (paladinsBattleAxeManager != null) paladinsBattleAxeManager.resetCooldowns(player);
        if (crazySlotsManager != null) crazySlotsManager.resetPlayer(player);
        TextUtils.removePlayerFromAllBossBars(player);
    }

    private void handleAltarCooldownEnd(Altar altar) {
        if (altar.getResultItem() == null || altar.getLocation() == null) return;

        org.bukkit.Location loc = altar.getLocation();
        String coordsText = loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();

        Component msg = altarLanguageManager.component("altar.ready_prefix")
                .append(Component.text(coordsText))
                .append(altarLanguageManager.component("altar.ready_suffix"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_GOAT_HORN_SOUND_1, 1.0f, 1.0f);
            } catch (Throwable t) {
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            }
            p.sendMessage(msg);
        }
    }

    private void cleanupPhantomAltarEntities() {
        NamespacedKey tagKey = new NamespacedKey(this, "altar_entity");
        Bukkit.getWorlds().forEach(world -> {
            world.getEntities().forEach(entity -> {
                if (entity.getPersistentDataContainer().has(tagKey, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            });
        });
    }

    public void saveDefaultAltarsConfig() {
        altarsConfigFile = new File(getDataFolder(), "altars.yml");
        if (!altarsConfigFile.exists()) {
            saveResource("altars.yml", false);
        }
        altarsConfig = YamlConfiguration.loadConfiguration(altarsConfigFile);
    }

    public void reloadAltarsConfig() {
        if (altarsConfigFile == null) {
            altarsConfigFile = new File(getDataFolder(), "altars.yml");
        }
        altarsConfig = YamlConfiguration.loadConfiguration(altarsConfigFile);
    }

    public void saveAltarsConfig() {
        try {
            if (altarsConfig != null && altarsConfigFile != null) {
                altarsConfig.save(altarsConfigFile);
            }
        } catch (Exception e) {
            getLogger().severe("Could not save altars.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getAltarsConfig() {
        if (altarsConfig == null) {
            reloadAltarsConfig();
        }
        return altarsConfig;
    }

    public void saveDefaultModelsConfig() {
        modelsFile = new File(getDataFolder(), "models.yml");
        if (!modelsFile.exists()) {
            saveResource("models.yml", false);
        }
        modelsConfig = YamlConfiguration.loadConfiguration(modelsFile);
    }

    public void reloadModelsConfig() {
        if (modelsFile == null) {
            modelsFile = new File(getDataFolder(), "models.yml");
        }
        modelsConfig = YamlConfiguration.loadConfiguration(modelsFile);
    }

    public FileConfiguration getModelsConfig() {
        if (modelsConfig == null) {
            reloadModelsConfig();
        }
        return modelsConfig;
    }

        public NamespacedKey getPaladinsBattleAxeKey() { return paladinsBattleAxeKey; }
    public PaladinsBattleAxeManager getPaladinsBattleAxeManager() { return paladinsBattleAxeManager; }
    public CrazySlotsManager getCrazySlotsManager() { return crazySlotsManager; }
    public WorldGuardManager getWorldGuardManager() { return worldGuardManager; }
    public VisualCleanupManager getVisualCleanupManager() { return visualCleanupManager; }
    public PaleGunAbilityManager getPaleGunAbilityManager() { return paleGunAbilityManager; }
    public EditAltarCommand getEditAltarCommand() { return editAltarCommand; }

    public AltarManager getAltarManager() { return altarManager; }
    public AltarLanguageManager getAltarLanguageManager() { return altarLanguageManager; }
}
