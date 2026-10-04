package dev.whersss.altarLegendaryWH.utils;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

@SuppressWarnings({"deprecation", "SpellCheckingInspection"})
public class WeaponFactory {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static String tr(String ru, String en) {
        return AltarLegendaryWH.getInstance().tr(ru, en);
    }

    private static Component txt(String text) {
        return TextUtils.legacy(text).decoration(TextDecoration.ITALIC, false);
    }

    private static Component line(String ru, String en) {
        return txt(tr(ru, en));
    }

    private static Component blank() {
        return TextUtils.empty();
    }

    public static String getSymbol(String key, String fallback) {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        if (plugin.getModelsConfig() != null) {
            return plugin.getModelsConfig().getString("symbols." + key, fallback);
        }
        return fallback;
    }

    public static Material resolveWeaponMaterial(String weaponKey, Material fallback) {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        if (plugin.getModelsConfig() != null) {
            String matName = plugin.getModelsConfig().getString("weapons." + weaponKey + ".material");
            if (matName != null && !matName.isBlank()) {
                Material m = Material.matchMaterial(matName.trim().toUpperCase(java.util.Locale.ROOT));
                if (m != null) return m;
            }
        }
        return fallback;
    }

    public static void applyModelSettings(ItemMeta meta, String weaponKey, int defaultCmd, String defaultTooltip) {
        applyModelSettings(meta, weaponKey, defaultCmd, "", defaultTooltip);
    }

    public static void applyModelSettings(ItemMeta meta, String weaponKey, int defaultCmd, String defaultItemModel, String defaultTooltip) {
        if (meta == null) return;
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int cmd = defaultCmd;
        String itemModel = defaultItemModel;
        String tooltip = defaultTooltip;

        if (plugin.getModelsConfig() != null) {
            String path = "weapons." + weaponKey;
            cmd = plugin.getModelsConfig().getInt(path + ".custom-model-data", defaultCmd);
            itemModel = plugin.getModelsConfig().getString(path + ".item-model", defaultItemModel);
            tooltip = plugin.getModelsConfig().getString(path + ".tooltip-style", defaultTooltip);
        }

        if (cmd > 0) {
            meta.setCustomModelData(cmd);
        }
        if (itemModel != null && !itemModel.isBlank()) {
            try {
                NamespacedKey key = NamespacedKey.fromString(itemModel.trim());
                if (key != null) {
                    meta.setItemModel(key);
                }
            } catch (Throwable ignored) {}
        }
        if (tooltip != null && !tooltip.isBlank()) {
            try {
                meta.setTooltipStyle(NamespacedKey.fromString(tooltip.trim()));
            } catch (Throwable ignored) {}
        }
    }

    public static void applyKnightfallModelSettings(ItemMeta meta, int tier, int cmd, String defaultTooltip) {
        if (meta == null) return;
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        String levelKey = (tier >= 3) ? "level-3" : ((tier >= 1) ? "level-2" : "level-1");
        int defaultCmdForTier = (tier >= 3) ? 3003 : ((tier >= 1) ? 3002 : 3001);
        int finalCmd = defaultCmdForTier;
        String itemModel = "";
        String tooltip = defaultTooltip;

        if (plugin.getModelsConfig() != null) {
            String path = "weapons.knightfall.levels." + levelKey;
            finalCmd = plugin.getModelsConfig().getInt(path + ".custom-model-data", defaultCmdForTier);
            itemModel = plugin.getModelsConfig().getString(path + ".item-model", "");
            tooltip = plugin.getModelsConfig().getString("weapons.knightfall.tooltip-style", defaultTooltip);
        }

        if (finalCmd > 0) {
            meta.setCustomModelData(finalCmd);
        }
        if (itemModel != null && !itemModel.isBlank()) {
            try {
                NamespacedKey key = NamespacedKey.fromString(itemModel.trim());
                if (key != null) {
                    meta.setItemModel(key);
                }
            } catch (Throwable ignored) {}
        }
        if (tooltip != null && !tooltip.isBlank()) {
            try {
                meta.setTooltipStyle(NamespacedKey.fromString(tooltip.trim()));
            } catch (Throwable ignored) {}
        }
    }

    private static Component cooldownLine(int seconds) {
        String icon = getSymbol("charges-icon", "\uE80D");
        return line("&f" + icon + " &8" + seconds + "с перезарядка", "&f" + icon + " &8" + seconds + "s cooldown");
    }

    private static Component gradientTitle(String hex1, String hex2, String hex3, String name) {
        return TextUtils.shadow(MM.deserialize("<!italic><gradient:" + hex1 + ":" + hex2 + ":" + hex3 + ">" + name + "</gradient>"));
    }

    private static Component gradientLine(String hex1, String hex2, String hex3, String ru, String en) {
        return gradientTitle(hex1, hex2, hex3, tr(ru, en));
    }

    private static List<Component> lore(Component... lines) {
        List<Component> lore = new ArrayList<>();
        for (Component line : lines) {
            lore.add(line);
        }
        return lore;
    }

    private static void addCommonSwordEnchants(ItemMeta meta) {
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.LOOTING, 3, true);
        meta.setUnbreakable(true);
    }

    public static ItemStack getBoneBlade() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int dashCooldown = plugin.getWeaponsConfig().getInt("bone-blade.dash.cooldown", 15);
        int cageCooldown = plugin.getWeaponsConfig().getInt("bone-blade.cage.cooldown", 45);
        Material mat = resolveWeaponMaterial("bone_blade", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#CBA073", "#FFFFFF", "#CBA073", tr("Костяной Клинок", "ʙᴏɴᴇ ʙʟᴀᴅᴇ")));
            applyModelSettings(meta, "bone_blade", 3000, "minecraft:bone");
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bone_blade"), PersistentDataType.BYTE, (byte) 1);
            meta.lore(lore(
                    line("&o&8Клинок, созданный из древнейших костей,", "&o&8A blade forged from the oldest bones known to humankind,"),
                    line("&o&8oизвестных человечеству. Некоторые говорят,", "&o&8Some say those bones are still alive,"),
                    line("&o&8что эти кости все еще живы.", "&o&8and still remember life."),
                    blank(),
                    line("&eᴄᴋᴇлᴇᴛный прыжоᴋ &6(Смена Руки)", "&esᴋᴇʟᴇᴛᴏɴ ʟᴇᴀᴘ &6(OffHand)"),
                    line("&7Рывок в направлении вашего взгляда,", "&7Dash in the direction you are looking,"),
                    line("&7дающий &fСкорость III &7на несколько секунд.", "&7granting &fSpeed III &7for a few seconds."),
                    cooldownLine(dashCooldown),
                    blank(),
                    line("&eᴋоᴄᴛянᴀя ᴋлᴇᴛᴋᴀ &6(SHIFT+Смена Руки)", "&eʙᴏɴᴇ ᴄᴀɢᴇ &6(Crouch+OffHand)"),
                    line("&7Бросьте быстрый снаряд перед собой,", "&7Throw a fast projectile in front of you,"),
                    line("&7который огнушает любого игрока", "&7which stuns any player"),
                    line("&7при попадании на несколько секунд.", "&7for a few seconds on hit."),
                    cooldownLine(cageCooldown)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }


    public static ItemStack getBloodLust(int kills) {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int infectionCooldown = plugin.getWeaponsConfig().getInt("bloodlust.infection.cooldown", 15);
        int bloodTrailCooldown = plugin.getWeaponsConfig().getInt("bloodlust.blood-trail.cooldown", 60);
        int bloodHookCooldown = plugin.getWeaponsConfig().getInt("bloodlust.blood-hook.cooldown", 30);
        Material mat = resolveWeaponMaterial("bloodlust", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#8B0000", "#FF5555", "#8B0000", tr("Жажда крови", "ʙʟᴏᴏᴅʟᴜsᴛ")));
            applyModelSettings(meta, "bloodlust", 3001, "minecraft:red");
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(plugin.getKillsKey(), PersistentDataType.INTEGER, kills);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bloodlust"), PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            meta.lore(lore(
                    line("&o&8Могущественный клинок, созданный из застывшей крови.", "&o&8A mighty blade forged from congealed blood."),
                    line("&o&8Чем больше вы убиваете им, тем сильнее становитесь.", "&o&8The more you kill with it, the stronger you become."),
                    blank(),
                    line("&b!! &3SHIFT+ЛКМ, чтобы просмотреть счетчик убийств.", "&b!! &3Crouch+LMB to view the kill counter."),
                    blank(),
                    line("&eинɸᴇᴋция &6(0+ Убийств)", "&eɪɴꜰᴇᴄᴛɪᴏɴ &6(0+ kills)"),
                    line("&715% шанс вызвать кровотечение у врага при ударе.", "&715% chance to inflict bleeding on an enemy when you hit them."),
                    cooldownLine(infectionCooldown),
                    blank(),
                    line("&eᴄᴋороᴄᴛь II &6(1+ Убийство)", "&esᴘᴇᴇᴅ ɪɪ &6(1+ kill)"),
                    line("&7Получите постоянную Скорость II, когда", "&7Gain permanent Speed II while"),
                    line("&7держите Жажду крови.", "&7holding bloodlust."),
                    blank(),
                    line("&eᴋроʙᴀʙый ᴛрᴇᴋᴇр &6(2+ Убийства)", "&eʙʟᴏᴏᴅ ᴛʀᴀᴄᴋᴇʀ &6(2+ kills)"),
                    line("&7Отслеживайте кровь ближайших игроков,", "&7Track the blood of nearby players"),
                    line("&7когда держите Жажду крови.", "&7while holding bloodlust."),
                    line("&7Только вы видите частицы.", "&7Only you can see the particles."),
                    blank(),
                    line("&eᴋроʙᴀʙый ᴄлᴇд &6(3+ Убийства)", "&eʙʟᴏᴏᴅ ᴛʀᴀɪʟ &6(3+ kills)"),
                    line("&7Погрузитесь в лужу крови &6[SHIFT+Смена Руки]", "&7Submerge yourself in a pool of blood &6[Crouch+OffHand]"),
                    cooldownLine(bloodTrailCooldown),
                    blank(),
                    line("&eᴄилᴀ I &6(4+ Убийства)", "&esᴛʀᴇɴɢᴛʜ ɪ &6(4+ kills)"),
                    line("&7Получите постоянную Силу I, когда", "&7Gain permanent Strength I while"),
                    line("&7держите Жажду крови.", "&7holding bloodlust."),
                    blank(),
                    line("&eᴋроʙᴀʙый ᴋрюᴋ &6(5+ Убийств)", "&eʙʟᴏᴏᴅ ʜᴏᴏᴋ &6(5+ kills)"),
                    line("&7Бросьте кровавую цепь перед собой,", "&7Throw a bloody chain in front of you,"),
                    line("&7притягивая все, чего она коснется, к себе &6[Смена Руки]", "&7pulling everything it touches toward you &6[OffHand]"),
                    cooldownLine(bloodHookCooldown)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getNightpiercer() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int transformationCooldown = plugin.getWeaponsConfig().getInt("nightpiercer.transformation.cooldown", 20);
        int biteCooldown = plugin.getWeaponsConfig().getInt("nightpiercer.bite.cooldown", 45);
        Material mat = resolveWeaponMaterial("nightpiercer", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#4A0000", "#AA0000", "#4A0000", tr("Пронзатель ночи", "ɴɪɢʜᴛᴘɪᴇʀᴄᴇʀ")));
            applyModelSettings(meta, "nightpiercer", 3009, "minecraft:red");
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "nightpiercer"), PersistentDataType.BYTE, (byte) 1);

            meta.lore(lore(
                    line("&7Вы получаете пассивную &fРегенерацию I &7ночью,", "&7You gain passive &fRegeneration &7I at night,"),
                    line("&7когда держите Пронзатель ночи.", "&7while holding nightpiercer."),
                    blank(),
                    line("&o&8Vampiri sunt praedatores noctis ultimi.", "&o&8Vampiri sunt praedatores noctis ultimi."),
                    blank(),
                    line("&eᴛрᴀнᴄɸормᴀция &6(SHIFT+Смена Руки)", "&eᴛʀᴀɴsꜰᴏʀᴍᴀᴛɪᴏɴ &6(Crouch+OffHand)"),
                    line("&7Временно превратитесь в стаю летучих мышей,", "&7Temporarily transform into a swarm of bats,"),
                    line("&7которая летит туда, куда вы смотрите.", "&7flying wherever you are looking."),
                    cooldownLine(transformationCooldown),
                    blank(),
                    line("&eбᴀгроʙый уᴋуᴄ &6(Смена Руки)", "&eᴄʀɪᴍsᴏɴ ʙɪᴛᴇ &6(OffHand)"),
                    line("&7Нанесите следующий удар в виде укуса.", "&7Turn your next attack into a bite."),
                    line("&7Жертва временно потеряет здоровье,", "&7The victim will temporarily lose health,"),
                    line("&7а вы восстановите свои силы.", "&7while you restore your own strength."),
                    cooldownLine(biteCooldown)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getVulcanCrossbow() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int magmaCooldown = plugin.getWeaponsConfig().getInt("vulcan_crossbow.magma-attack.cooldown", 25);
        Material mat = resolveWeaponMaterial("vulcan_crossbow", Material.CROSSBOW);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#FF4444", "#FFAA00", "#FF4444", tr("Арбалет Вулкана", "ᴠᴜʟᴄᴀɴ's ᴄʀᴏssʙᴏᴡ")));
            applyModelSettings(meta, "vulcan_crossbow", 3000, "minecraft:vulcan");
            meta.setUnbreakable(true);
            meta.addEnchant(Enchantment.POWER, 7, true);
            meta.addEnchant(Enchantment.PIERCING, 4, true);
            meta.addEnchant(Enchantment.MULTISHOT, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            meta.getPersistentDataContainer().set(plugin.getVulcanKey(), PersistentDataType.BYTE, (byte) 1);

            meta.lore(lore(
                    line("&o&8Легендарный арбалет Вулкана.", "&o&8A legendary crossbow of Vulcan."),
                    line("&o&8Кажется, все вставленные в него стрелы", "&o&8It seems every bolt loaded into it"),
                    line("&o&8обретают огненные свойства.", "&o&8takes on fiery properties."),
                    blank(),
                    line("&eгнᴇʙ ʙулᴋᴀнᴀ &6(SHIFT+Выстрел)", "&eᴠᴜʟᴄᴀɴ's ᴡʀᴀᴛʜ &6(Crouch+RMB)"),
                    line("&7Ваш следующий выстрел станет взрывным", "&7Your next shot becomes an explosive"),
                    line("&7огненным шаром, уничтожающим всё на пути.", "&7fireball that destroys everything in its path."),
                    cooldownLine(magmaCooldown),
                    blank(),
                    line("&eогнᴇнный зᴀлп &6(Пассивно)", "&eꜰɪʀᴇ ʙᴀʀʀᴀɢᴇ &6(passive)"),
                    line("&7Арбалет выпускает плотный веер стрел,", "&7The crossbow fires a dense spread of bolts,"),
                    line("&7пробивающих щиты. Центральная стрела", "&7piercing shields. The central bolt"),
                    line("&7поджигает врага.", "&7sets the enemy on fire.")
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getPaleGun() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int shootCooldown = plugin.getWeaponsConfig().getInt("pale-gun.shoot.cooldown", 10);
        int rootsCooldown = plugin.getWeaponsConfig().getInt("pale-gun.pale-roots.cooldown", 25);
        Material mat = resolveWeaponMaterial("pale_gun", Material.CROSSBOW);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#D7CFBC", "#F5F1E4", "#A99D83", tr("зцᴋᴇᴇп ᴄшцйлɸы", "ᴘᴡʀᴛᴛɢ ᴄɪᴡǫᴋᴀs")));
            applyModelSettings(meta, "pale_gun", 3001, "minecraft:gold");
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(plugin.getPaleGunKey(), PersistentDataType.BYTE, (byte) 1);
            meta.lore(lore(
                    line("&8&k7xv0a ствол помнит каждую трещину плоти &8&kr4m8", "&8&k7xv0a the barrel remembers every split in flesh &8&kr4m8"),
                    line("&8&kСмоляной Залп &6(Выстрел) &8для &8&kefefeffefefe", "&8&kResin Burst &6(Shoot) &8to &8&kefefeffefefe"),
                    line("&8&kВыстреливает бледным бревном, которое взрывается", "&8&kLaunches a pale timber round that bursts"),
                    line("&8Нажмите &6(Смена Руки) &8для активаций &8&k6767676767676767", "&8Press &6(OffHand) &8to activate &8&k6767676767676767")
            ));
            item.setItemMeta(meta);
            try {
                item.setData(io.papermc.paper.datacomponent.DataComponentTypes.USE_COOLDOWN,
                        io.papermc.paper.datacomponent.item.UseCooldown.useCooldown(1.0f)
                                .cooldownGroup(new NamespacedKey(plugin, "pale_gun_group"))
                                .build()
                );
            } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            }
        }
        return item;
    }
    public static ItemStack getFrostScythe() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int iceCommandCooldown = plugin.getWeaponsConfig().getInt("frost-scythe.ice-command.cooldown", 45);
        int throwCooldown = plugin.getWeaponsConfig().getInt("frost-scythe.throw.cooldown", 30);
        Material mat = resolveWeaponMaterial("frost_scythe", Material.TRIDENT);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#00AAAA", "#55FFFF", "#00AAAA", tr("Морозная Коса", "ꜰʀᴏsᴛ sᴄʏᴛʜᴇ")));
            applyModelSettings(meta, "frost_scythe", 4, "minecraft:netherite_sword", "minecraft:frost");
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
            meta.addEnchant(Enchantment.LOOTING, 3, true);
            meta.setUnbreakable(true);
            meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
            meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

            NamespacedKey dmgKey = new NamespacedKey(AltarLegendaryWH.getInstance(), "frost_dmg");
            NamespacedKey spdKey = new NamespacedKey(AltarLegendaryWH.getInstance(), "frost_spd");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                    new AttributeModifier(dmgKey, 8.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                    new AttributeModifier(spdKey, -2.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));

            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "frost_scythe"), PersistentDataType.BYTE, (byte) 1);

            String crouchRmb = getSymbol("crouch-rmb", "\uE80E");
            meta.lore(lore(
                    line("&o&8Древняя коса, использовавшаяся для жатвы", "&o&8An ancient scythe once used to harvest"),
                    line("&o&8ледяных морей. Коса холодна на ощупь.", "&o&8the frozen seas. The scythe is cold to the touch."),
                    blank(),
                    line("&eпоʙᴇлᴇниᴇ льдᴀ &f" + crouchRmb + " &6(SHIFT+ПКМ)", "&eᴄᴏᴍᴍᴀɴᴅ ᴏꜰ ɪᴄᴇ &f" + crouchRmb + " &6(Crouch+RMB)"),
                    line("&7Призывает три блока льда над вашей", "&7Summons three blocks of ice above your"),
                    line("&7головой, которые затем летят во врагов.", "&7head, which then fly toward enemies."),
                    cooldownLine(iceCommandCooldown),
                    blank(),
                    line("&eброᴄоᴋ ᴋоᴄы &6(Зажать ПКМ)", "&esᴄʏᴛʜᴇ ᴛʜʀᴏᴡ &6(Hold RMB)"),
                    line("&7Бросьте Морозную Косу в направлении взгляда,", "&7Throw the frost scythe in the direction you are looking,"),
                    line("&7замедляя всё, во что она попадет.", "&7slowing everything it hits."),
                    cooldownLine(throwCooldown)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getPureBlade() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int shadeSoulCooldown = plugin.getWeaponsConfig().getInt("pure-blade.shade-soul.cooldown", 45);
        int cycloneCooldown = plugin.getWeaponsConfig().getInt("pure-blade.cyclone-slash.cooldown", 30);
        Material mat = resolveWeaponMaterial("pure_blade", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#AAAAAA", "#FFFFFF", "#AAAAAA", tr("Чистый Клинок", "ᴘᴜʀᴇ ʙʟᴀᴅᴇ")));
            applyModelSettings(meta, "pure_blade", 3008, "minecraft:pure");
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "pure_blade"), PersistentDataType.BYTE, (byte) 1);

            meta.lore(lore(
                    line("&o&8Создано на окраинах Города Слез,", "&o&8Created on the outskirts of the City of Tears,"),
                    line("&o&8это оружие дарует владельцу силу пустоты", "&o&8this weapon grants its wielder the power of the void"),
                    line("&o&8и непревзойденное мастерство мечника.", "&o&8and unmatched swordsmanship."),
                    blank(),
                    gradientLine("#AAAAAA", "#FFFFFF", "#AAAAAA", "ᴛᴇнь души", "sʜᴀᴅᴇ ᴏꜰ sᴏᴜʟ"),
                    line("&7[Смена Руки]", "&7[OffHand]"),
                    blank(),
                    line("&7Призывает тень, которая летит вперед и уничтожает врагов", "&7Summons a shadow that flies forward and destroys enemies"),
                    line("&7на своем пути. Пронзает врагов и стены.", "&7in its path. It pierces through enemies and walls."),
                    line("&7Задетые враги получают &fСвечение и Замедление II &7на &f5с&7.", "&7Enemies struck receive &fGlowing and Slowness II &7for &f5s&7."),
                    cooldownLine(shadeSoulCooldown),
                    blank(),
                    gradientLine("#AAAAAA", "#FFFFFF", "#AAAAAA", "ʙихрᴇʙой рᴀзрᴇз", "ᴄʏᴄʟᴏɴᴇ sʟᴀsʜ"),
                    line("&7[SHIFT + Смена Руки]", "&7[Crouch+OffHand]"),
                    blank(),
                    line("&7Высвобождает вращающуюся атаку, которая наносит урон врагам", "&7Releases a spinning attack that deals damage to enemies"),
                    line("&7со всех сторон, разрезая паутину и даруя вам", "&7from every side, cutting through cobwebs and granting you"),
                    line("&fогромную скорость &7на &fкороткое время&7.", "&fenormous speed &7for a &fshort time&7."),
                    cooldownLine(cycloneCooldown)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getKnightfall(int kills) {
        return getKnightfallCustom(kills, 3001, false);
    }

    public static ItemStack getKnightfallCustom(int kills, int cmd, boolean hasDensity) {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int grappleCooldown = plugin.getWeaponsConfig().getInt("knightfall.grapple.cooldown", 10);
        int hammerThrowCooldown = plugin.getWeaponsConfig().getInt("knightfall.throw.cooldown", 20);
        Material mat = resolveWeaponMaterial("knightfall", Material.MACE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#AAAAAA", "#FFFFFF", "#AAAAAA", tr("Падший Рыцарь", "ᴋɴɪɢʜᴛꜰᴀʟʟ")));
            int tier = 0;
            if (cmd >= 3003 || cmd == 3) {
                tier = 3;
            } else if (hasDensity) {
                tier = 2;
            } else if (cmd >= 3002 || cmd == 2) {
                tier = 1;
            }
            applyKnightfallModelSettings(meta, tier, cmd, "minecraft:pure");
            meta.setUnbreakable(true);

            if (kills >= 2) {
                meta.addEnchant(Enchantment.WIND_BURST, 1, true);
            }
            if (hasDensity) {
                meta.addEnchant(Enchantment.DENSITY, 2, true);
            }
            meta.getPersistentDataContainer().set(plugin.getKnightfallTierKey(), PersistentDataType.INTEGER, tier);
            meta.getPersistentDataContainer().set(plugin.getKillsKey(), PersistentDataType.INTEGER, kills);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "knightfall"), PersistentDataType.BYTE, (byte) 1);
            String chargesIcon = getSymbol("charges-icon", "\uE80D");
            meta.lore(lore(
                    line("&o&8Луна ярко светит над темным рыцарем.", "&o&8The moon shines brightly above the dark knight."),
                    line("&cУбийств: " + kills, "&cKills: " + kills),
                    blank(),
                    line("&eплᴀщ &6[0 убийств]", "&eᴄʟᴏᴀᴋ &6[0 kills]"),
                    line("&710% шанс получить &fИстинную невидимость", "&710% chance to gain &ftrue invisibility"),
                    line("&7на &f3 секунды &7при сокрушительном ударе.", "&7for &f3 seconds &7on a crushing strike."),
                    blank(),
                    line("&eпорыʙ ʙᴇᴛрᴀ &6[2 убийства]", "&eᴡɪɴᴅ ʙᴜʀsᴛ &6[2 kills]"),
                    line("&7Наделяет булаву чарами Порыв Ветра I.", "&7Empowers the mace with Wind Burst I."),
                    blank(),
                    line("&eᴀбордᴀжный ᴋрюᴋ &6[4 убийства]", "&eɢʀᴀᴘᴘʟɪɴɢ ʜᴏᴏᴋ &6[4 kills]"),
                    line("&e[Смена руки]", "&e[OffHand]"),
                    line("&7Зацепитесь за блок и притянитесь к нему.", "&7Latch onto a block and pull yourself toward it."),
                    line("&7В конце пути вас подбросит вверх.", "&7At the end of the pull, you are launched upward."),
                    line("&f" + chargesIcon + " &8x3 заряда (" + grappleCooldown + "с перезарядка)", "&f" + chargesIcon + " &8x3 charges (" + grappleCooldown + "s cooldown)"),
                    blank(),
                    line("&eᴄᴋороᴄᴛь II &6[6 убийств]", "&esᴘᴇᴇᴅ ɪɪ &6[6 kills]"),
                    line("&7Дарует владельцу постоянную Скорость II.", "&7Grants the wielder permanent Speed II."),
                    blank(),
                    line("&eплоᴛноᴄᴛь &6[8 убийств]", "&eᴅᴇɴsɪᴛʏ &6[8 kills]"),
                    line("&7Наделяет булаву чарами Плотность II.", "&7Empowers the mace with Density II."),
                    blank(),
                    line("&eброᴄоᴋ молоᴛᴀ &6[10 убийств]", "&eʜᴀᴍᴍᴇʀ ᴛʜʀᴏᴡ &6[10 kills]"),
                    line("&e[SHIFT + Смена Руки]", "&e[Crouch+OffHand]"),
                    line("&7Бросьте Булаву в направлении взгляда, нанося", "&7Throw the mace in the direction you are looking, dealing"),
                    line("&7огромный урон и притягивая врага к себе.", "&7massive damage and pulling the enemy toward you."),
                    cooldownLine(hammerThrowCooldown)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getShadowBlade() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int leapCooldown = plugin.getWeaponsConfig().getInt("shadow-blade.leap.cooldown", 30);
        int daggersCooldown = plugin.getWeaponsConfig().getInt("shadow-blade.daggers.cooldown", 45);
        Material mat = resolveWeaponMaterial("shadow_blade", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#AAAAAA", "#FFFFFF", "#AAAAAA", tr("ᴛᴇнᴇʙой ᴋлиноᴋ", "sʜᴀᴅᴏᴡ ʙʟᴀᴅᴇ")));
            applyModelSettings(meta, "shadow_blade", 3007, "minecraft:pure");
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shadow_blade"), PersistentDataType.BYTE, (byte) 1);

            String crouchRmb = getSymbol("crouch-rmb", "\uE80E");
            meta.lore(lore(
                    line("&o&8Клинок, выкованный из отсутствия света.", "&o&8A blade forged from the absence of light."),
                    line("&o&8Даже для тени, он выглядит невероятно острым.", "&o&8Even for a shadow, it looks impossibly sharp."),
                    blank(),
                    line("&f&lᴛᴇнᴇʙой прыжоᴋ", "&f&lsʜᴀᴅᴏᴡ ʟᴇᴀᴘ"),
                    line("&f" + crouchRmb + " &f[SHIFT+ПКМ]", "&f" + crouchRmb + " &f[Crouch+RMB]"),
                    blank(),
                    line("&7Погрузитесь в тень на &f1с&7, управляя тем, куда", "&7Sink into the shadows for &f1s&7, controlling where"),
                    line("&7вы телепортируетесь, с помощью взгляда. Нажмите &fПКМ", "&7you will teleport with your gaze. Press &fRMB"),
                    line("&7после активации, чтобы телепортироваться досрочно.", "&7after activation to teleport early."),
                    cooldownLine(leapCooldown),
                    blank(),
                    line("&f&lᴛᴇнᴇʙыᴇ ᴋинжᴀлы", "&f&lsʜᴀᴅᴏᴡ ᴅᴀɢɢᴇʀs"),
                    line("&f[Смена Руки]", "&f[OffHand]"),
                    blank(),
                    line("&7Бросьте три Теневых Кинжала туда, куда смотрите,", "&7Throw three shadow daggers wherever you are looking,"),
                    line("&7накладывая &fЗамедление II &7и &fСлепоту &7на короткое время.", "&7applying &fSlowness II &7and &fBlindness &7for a short time."),
                    cooldownLine(daggersCooldown),
                    blank(),
                    line("&f&lпᴀᴄᴄиʙно", "&f&lᴘᴀssɪᴠᴇ"),
                    line("&7- При ударе в спину у вас есть &f30% &7шанс притянуть врагов.", "&7- Hitting an enemy in the back gives you a &f30% &7chance to pull them."),
                    line("&7- Когда в руках, дает постоянную &fСкорость II&7.", "&7- While held, grants permanent &fSpeed II&7.")
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getWindWeaver() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int leapCooldown = plugin.getWeaponsConfig().getInt("windweaver.wind-leap.cooldown", 10);
        int burstCooldown = plugin.getWeaponsConfig().getInt("windweaver.wind-burst.cooldown", 35);
        Material mat = resolveWeaponMaterial("windweaver", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#D8F7FF", "#7EC8E3", "#D8F7FF", tr("Ветроплёт", "ᴡɪɴᴅᴡᴇᴀᴠᴇʀ")));
            applyModelSettings(meta, "windweaver", 3006, "minecraft:pure");
            addCommonSwordEnchants(meta);
            meta.setUnbreakable(true);
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            meta.addEnchant(Enchantment.LOOTING, 3, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "windweaver"), PersistentDataType.BYTE, (byte) 1);

            meta.lore(lore(
                    line("&o&8Клинок, в котором слышен только свист ветра.", "&o&8A blade in which only the whisper of wind can be heard."),
                    line("&o&8Он режет воздух так же легко, как и плоть.", "&o&8It cuts through air as easily as flesh."),
                    blank(),
                    line("&f&lʙᴇᴛроʙой прыжоᴋ", "&f&lᴡɪɴᴅ ʟᴇᴀᴘ"),
                    line("&f[Смена руки]", "&f[OffHand]"),
                    blank(),
                    line("&7Совершите прыжок в направлении взгляда.", "&7Leap in the direction you're looking."),
                    line("&7Кратковременно дает &fСкорость IV", "&7Briefly gain &fSpeed IV"),
                    cooldownLine(leapCooldown),
                    blank(),
                    line("&f&lпорыʙ ʙᴇᴛрᴀ", "&f&lᴡɪɴᴅ ʙᴜʀsᴛ"),
                    line("&f[SHIFT + Смена руки]", "&f[Crouch + OffHand]"),
                    blank(),
                    line("&7Удерживайте приседание, чтобы зарядить мощный", "&7Hold crouch to charge a powerful"),
                    line("&7отбрасывающий порыв. Чем дольше вы приседаете,", "&7knockback burst. The longer you stay"),
                    line("&7тем мощнее он становится.", "&7crouched, the more powerful it gets."),
                    cooldownLine(burstCooldown)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getHyperion() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int scorchingCooldown = plugin.getWeaponsConfig().getInt("hyperion.scorching-blade.cooldown", 30);
        int holyLanceCooldown = plugin.getWeaponsConfig().getInt("hyperion.holy-lance.cooldown", 60);
        Material mat = resolveWeaponMaterial("hyperion", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#FFC000", "#FF4000", "#FFC000", tr("Гиперион", "ʜʏᴘᴇʀɪᴏɴ")));
            applyModelSettings(meta, "hyperion", 3005, "minecraft:gold");
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(AltarLegendaryWH.getInstance().getHyperionKey(), PersistentDataType.BYTE, (byte) 1);

            meta.lore(lore(
                    line("&7Вы неуязвимы к огню, когда держите Гиперион.", "&7You are immune to fire while holding hyperion."),
                    line("&7Ваши способности накладывают &fСвященное Пламя &7на врагов.", "&7Your abilities apply &fSacred Flame &7to enemies."),
                    blank(),
                    line("&o&8Не уходи безропотно во тьму.", "&o&8Do not go gentle into that good night."),
                    line("&o&8Бунтуй, бунтуй против угасания света.", "&o&8Rage, rage against the dying of the light."),
                    blank(),
                    line("&eобжигᴀющий ᴋлиноᴋ &6(SHIFT+Смена Руки)", "&esᴄᴏʀᴄʜɪɴɢ ʙʟᴀᴅᴇ &6(Crouch+OffHand)"),
                    line("&7Наполните свой следующий взмах святым огнем,", "&7Charge your next swing with holy fire,"),
                    line("&7запуская клинок пламени во врага.", "&7launching a blade of flame at the enemy."),
                    cooldownLine(scorchingCooldown),
                    blank(),
                    line("&eᴄʙяᴛоᴇ ᴋопьᴇ &6(Смена Руки)", "&eʜᴏʟʏ ʟᴀɴᴄᴇ &6(OffHand)"),
                    line("&7Призовите мощный луч света туда, куда вы смотрите,", "&7Call down a powerful beam of light where you are looking,"),
                    line("&7нанося огромный урон врагам.", "&7dealing massive damage to enemies."),
                    cooldownLine(holyLanceCooldown)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getWitherBlade() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int dashCooldown = plugin.getWeaponsConfig().getInt("wither-blade.dash.cooldown", 30);
        int auraCooldown = plugin.getWeaponsConfig().getInt("wither-blade.aura.cooldown", 50);
        Material mat = resolveWeaponMaterial("wither_blade", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#2b2b2b", "#5c5c5c", "#2b2b2b", tr("Иссушенный Костяной Клинок", "ᴡɪᴛʜᴇʀᴇᴅ ʙᴏɴᴇ ʙʟᴀᴅᴇ")));
            applyModelSettings(meta, "wither_blade", 3003, "minecraft:pure");
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(AltarLegendaryWH.getInstance().getWitherKey(), PersistentDataType.BYTE, (byte) 1);

            meta.lore(lore(
                    line("&o&8Он каким-то образом вернулся...", "&o&8It somehow returned..."),
                    line("&o&8Но он уже не такой, как прежде...", "&o&8But it is no longer the same as before..."),
                    blank(),
                    line("&8иᴄᴄушиᴛᴇльный прыжоᴋ", "&8ᴡɪᴛʜᴇʀ ʟᴇᴀᴘ"),
                    line("&f[Смена Руки]", "&f[OffHand]"),
                    blank(),
                    line("&7Совершите прыжок в направлении взгляда (до 3 раз), получая эффект &fСкорость 3 &7на", "&7Dash in the direction you are looking (up to 3 times), gaining &fSpeed III &7for"),
                    line("&7несколько секунд. Вы можете врезаться во врагов и нанести им урон иссушением.", "&7a few seconds. You can crash into enemies and deal wither damage to them."),
                    cooldownLine(dashCooldown),
                    blank(),
                    line("&8иᴄᴄушиᴛᴇльноᴇ ʙыᴄʙобождᴇниᴇ", "&8ᴡɪᴛʜᴇʀ ʀᴇʟᴇᴀsᴇ"),
                    line("&f[SHIFT+Смена Руки]", "&f[Crouch+OffHand]"),
                    blank(),
                    line("&7По мере нанесения урона заполняется ваша шкала иссушения, и вам нужно избавиться от", "&7As you deal damage, your wither gauge fills up, and you must get rid of"),
                    line("&7него, как только она заполнится. При высвобождении шкалы вы выстрелите черной слизью,", "&7it as soon as it is full. When you release the gauge, you will fire black slime,"),
                    line("&7которая замедляет врагов. Если вы не очистите себя, когда шкала заполнена, вас ждет ", "&7which slows enemies. If you fail to cleanse yourself when the gauge is full,"),
                    line("&7суровое наказание.", "&7a harsh punishment awaits you."),
                    cooldownLine(auraCooldown)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getEarthGauntlet() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int meteorCooldown = plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.cooldown", 35);
        int mudslideCooldown = plugin.getWeaponsConfig().getInt("earth-gauntlet.mudslide.cooldown", 40);
        Material mat = resolveWeaponMaterial("earth_gauntlet", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#d6a058", "#8a5e27", "#d6a058", tr("Землянная Перчатка", "ᴇᴀʀᴛʜ ɢᴀᴜɴᴛʟᴇᴛ")));
            applyModelSettings(meta, "earth_gauntlet", 3004, "minecraft:earth");
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            meta.addEnchant(Enchantment.LOOTING, 3, true);
            meta.setUnbreakable(true);

            meta.lore(lore(
                    line("&o&8oПохожая на глину перчатка с камнем в центре.", "&o&8A clay-like gauntlet with a stone set in the center."),
                    line("&o&8Камень, кажется, обладает могущественными свойствами...", "&o&8The stone seems to possess mighty properties..."),
                    blank(),
                    gradientLine("#d6a058", "#8a5e27", "#d6a058", "мᴇᴛᴇориᴛный удᴀр", "ᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ"),
                    line("&e[Смена руки]", "&e[OffHand]"),
                    blank(),
                    line("&7Заряжает ваш следующий удар дополнительным уроном", "&7Charges your next strike with additional damage"),
                    line("&7и отбрасыванием, оглушая цель.", "&7and knockback, stunning the target."),
                    cooldownLine(meteorCooldown),
                    blank(),
                    gradientLine("#d6a058", "#8a5e27", "#d6a058", "оползᴇнь", "ᴍᴜᴅsʟɪᴅᴇ"),
                    line("&e[Shift + Смена руки]", "&e[Crouch+OffHand]"),
                    blank(),
                    line("&7Бросает грязевой снаряд в направлении вашего", "&7Throws a mud projectile in the direction of your"),
                    line("&7взгляда. Если вы попадете им во врага,", "&7gaze. If you hit an enemy with it,"),
                    line("&7он получит дебафф &fЗаляпанный грязью &7на &f4с&7.", "&7they receive the &fMud-Splattered &7debuff for &f4s&7."),
                    line("&7В течение этого времени у вас есть выбор:", "&7During this time, you have a choice:"),
                    line("&fпритянуть цель к себе [Смена руки]&7 или", "&fpull the target to yourself [OffHand]&7 or"),
                    line("&7оставить дебафф на оставшееся время.", "&7leave the debuff active for the remaining time."),
                    line("&7Снаряд непредсказуем и может быть", "&7The projectile is unpredictable and may be"),
                    line("&7неточным на больших дистанциях.", "&7inaccurate at long distances."),
                    cooldownLine(mudslideCooldown)
            ));

            meta.getPersistentDataContainer().set(new NamespacedKey(AltarLegendaryWH.getInstance(), "earth_gauntlet"), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getCutlass() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        Material mat = resolveWeaponMaterial("cutlass", Material.NETHERITE_SWORD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#179DD0", "#59BEDF", "#179DD0", tr("Абордажная Сабля", "ᴄᴜᴛʟᴀss")));
            applyModelSettings(meta, "cutlass", 3002, "minecraft:tidebreaker");
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "cutlass"), PersistentDataType.BYTE, (byte) 1);

            int thousandCooldown = plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.cooldown", 45);
            int parryCooldown = plugin.getWeaponsConfig().getInt("cutlass.parry.cooldown", 30);
            int parryCharges = plugin.getWeaponsConfig().getInt("cutlass.parry.max-charges", 3);
            int parryDuration = plugin.getWeaponsConfig().getInt("cutlass.parry.max-duration-seconds", 7);

            String chargesIcon = getSymbol("charges-icon", "\uE80D");
            meta.lore(lore(
                    line("&o&8Легендарная сабля, оттачивающая ваши навыки", "&o&8A legendary cutlass that sharpens your skills"),
                    line("&o&8владения мечом до совершенства.", "&o&8of swordsmanship to perfection."),
                    blank(),
                    gradientLine("#179DD0", "#59BEDF", "#179DD0", "ᴄᴛо цᴀрᴀпин", "ʜᴜɴᴅʀᴇᴅ ᴄᴜᴛs"),
                    line("&3[Смена Руки]", "&3[OffHand]"),
                    blank(),
                    line("&7Обрушьте шквал ударов перед собой, за которым", "&7Unleash a flurry of strikes in front of you, followed by"),
                    line("&7следует колющий выпад, продвигающий вас вперед,", "&7a thrusting lunge that pushes you forward"),
                    line("&7и накладывающий &fЗамедление II &7на цель на &f3 сек&7.", "&7and applies &fSlowness II &7to the target for &f3s&7."),
                    cooldownLine(thousandCooldown),
                    blank(),
                    gradientLine("#179DD0", "#59BEDF", "#179DD0", "пирᴀᴛᴄᴋоᴇ оᴛрᴀжᴇниᴇ", "ᴘɪʀᴀᴛᴇ's ᴘᴀʀʀʏ"),
                    line("&3[SHIFT + Смена Руки]", "&3[Crouch+OffHand]"),
                    blank(),
                    line("&7Примите стойку парирования, в которой вы можете", "&7Enter a parry stance in which you can"),
                    line("&7заблокировать до " + parryCharges + " снарядов или атак ближнего", "&7block up to " + parryCharges + " projectiles or melee attacks"),
                    line("&7боя, отбрасывая врага назад. Длится до &f" + parryDuration + " секунд&7.", "&7while knocking the attacker back. Lasts up to &f" + parryDuration + " seconds&7."),
                    line("&7Вы слегка замедляетесь в этой стойке.", "&7You are slightly slowed while in this stance."),
                    line("&f" + chargesIcon + " &8" + parryCooldown + "с перезарядка &7(" + parryCharges + " " + tr("заряда", "charges") + "&7)", "&f" + chargesIcon + " &8" + parryCooldown + "s cooldown &7(" + parryCharges + " charges&7)")
            ));

            item.setItemMeta(meta);
        }

        if (plugin.getCutlassManager() != null) {
            double damage = plugin.getWeaponsConfig().getDouble("cutlass.base.damage", 8.0);
            double speed = plugin.getWeaponsConfig().getDouble("cutlass.base.attack-speed", 1.6);
            plugin.getCutlassManager().setCutlassAttributes(item, damage, speed);
        }

        return item;
    }

    public static ItemStack getPaladinsBattleAxe() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        Material mat = resolveWeaponMaterial("paladins_battle_axe", Material.NETHERITE_AXE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#FFAA00", "#FFD700", "#FFAA00", tr("Боевой Топор паладина", "ᴘᴀʟᴀᴅɪɴ's ʙᴀᴛᴛʟᴇ ᴀxᴇ")));
            applyModelSettings(meta, "paladins_battle_axe", 2, "minecraft:gold");
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(plugin.getPaladinsBattleAxeKey(), PersistentDataType.BYTE, (byte) 1);

            int shatterCooldown = plugin.getWeaponsConfig().getInt("paladins-battle-axe.earth-shatter.cooldown", 45);
            int stalwartCooldown = plugin.getWeaponsConfig().getInt("paladins-battle-axe.stalwart-absorption.cooldown", 45);

            meta.lore(lore(
                    line("&o&8Магический боевой топор, созданный для того,", "&o&8A magical battle axe made for someone"),
                    line("&o&8кто обладает неукротимым боевым духом.", "&o&8with an indomitable fighting spirit."),
                    blank(),
                    line("&o&8\"Никогда не гаси свое пламя, благородный.\"", "&o&8\"Never quench thy flame, noble one.\""),
                    blank(),
                    gradientLine("#FFAA00", "#FFD700", "#FFAA00", "ᴄоᴋрушᴇниᴇ зᴇмли", "ᴇᴀʀᴛʜsʜᴀᴛᴛᴇʀ"),
                    line("&6[Смена Руки]", "&6[Offhand]"),
                    blank(),
                    line("&7Призовите призрачную версию своего топора туда, куда смотрите,", "&7Summon a spectral version of your axe where you look,"),
                    line("&7создавая мощную ударную волну, наносящую урон", "&7creating a massive shockwave that deals damage to"),
                    line("&7всем, кто стоит на земле или рядом с топором.", "&7anyone standing on ground, or near the axe."),
                    cooldownLine(shatterCooldown),
                    blank(),
                    line("&7У вас увеличена скорость атаки при удерживании топора.", "&7You have increased swing speed while holding the axe."),
                    blank(),
                    gradientLine("#FFAA00", "#FFD700", "#FFAA00", "ᴄᴛойᴋоᴇ поглощᴇниᴇ", "sᴛᴀʟᴡᴀʀᴛ ᴀʙsᴏʀᴘᴛɪᴏɴ"),
                    line("&6[SHIFT + Смена Руки]", "&6[Offhand + Crouch]"),
                    blank(),
                    line("&7В течение 5 секунд поглощайте весь получаемый урон, чтобы затем", "&7For 5 seconds, absorb all received damage to then"),
                    line("&7использовать его для мощной ударной волны после окончания эффекта.", "&7use for a powerful shockwave after the effect ends."),
                    line("&7Вы получаете &fСопротивление I &7во время действия эффекта.", "&7You gain &fResistance I &7while this effect is present."),
                    cooldownLine(stalwartCooldown)
            ));

            item.setItemMeta(meta);
        }

        if (plugin.getPaladinsBattleAxeManager() != null) {
            double damage = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.base.damage", 10.0);
            double speed = plugin.getWeaponsConfig().getDouble("paladins-battle-axe.base.attack-speed", 1.0);
            plugin.getPaladinsBattleAxeManager().setPaladinsBattleAxeAttributes(item, damage, speed);
        }

        return item;
    }

    public static ItemStack getCrazySlots() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        Material mat = resolveWeaponMaterial("crazyslots", Material.CLAY_BALL);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#FFAA00", "#FFD700", "#FFAA00", tr("бᴇзумныᴇ ᴄлоᴛы", "ᴄʀᴀᴢʏ sʟᴏᴛs")));
            applyModelSettings(meta, "crazyslots", 4, "minecraft:pure");
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "crazyslots"), PersistentDataType.BYTE, (byte) 1);

            int transformDuration = plugin.getWeaponsConfig().getInt("crazy-slots.transform-duration", 30);
            int cooldown = plugin.getWeaponsConfig().getInt("crazy-slots.cooldown", 60);

            meta.lore(lore(
                    line("&fНажмите ПКМ, чтобы превратить в случайное легендарное оружие!",
                            "&fRight-click to transform into a random legendary weapon!"),
                    line("&7Превращается на &f" + transformDuration + " секунд",
                            "&7Transforms for &f" + transformDuration + " seconds"),
                    line("&8Перезарядка: " + cooldown + " секунд",
                            "&8Cooldown: " + cooldown + " seconds")
            ));

            item.setItemMeta(meta);
        }
        return item;
    }
}