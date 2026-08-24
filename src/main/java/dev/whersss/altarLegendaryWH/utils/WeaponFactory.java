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

    private static Component cooldownLine(int seconds) {
        return line("&f\uE80D &8" + seconds + "с перезарядка", "&f\uE80D &8" + seconds + "s cooldown");
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
        int dashCooldown = plugin.getConfig().getInt("bone-blade.dash.cooldown", 15);
        int cageCooldown = plugin.getConfig().getInt("bone-blade.cage.cooldown", 45);
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#CBA073", "#FFFFFF", "#CBA073", tr("Костяной Клинок", "ʙᴏɴᴇ ʙʟᴀᴅᴇ")));
            meta.setCustomModelData(1);
            addCommonSwordEnchants(meta);
            meta.lore(lore(
                    line("&o&8Клинок, созданный из древнейших костей,", "&o&8A blade forged from the oldest bones known to humankind,"),
                    line("&o&8oизвестных человечеству. Некоторые говорят,", "&o&8Some say those bones are still alive,"),
                    line("&o&8что эти кости все еще живы.", "&o&8and still remember life."),
                    blank(),
                    line("&eСкелетный Прыжок &6(Смена Руки)", "&esᴋᴇʟᴇᴛᴏɴ ʟᴇᴀᴘ &6(OffHand)"),
                    line("&7Рывок в направлении вашего взгляда,", "&7Dash in the direction you are looking,"),
                    line("&7дающий &fСкорость III &7на несколько секунд.", "&7granting &fSpeed III &7for a few seconds."),
                    cooldownLine(dashCooldown),
                    blank(),
                    line("&eКостяная Клетка &6(SHIFT+Смена Руки)", "&eʙᴏɴᴇ ᴄᴀɢᴇ &6(Crouch+OffHand)"),
                    line("&7Бросьте быстрый снаряд перед собой,", "&7Throw a fast projectile in front of you,"),
                    line("&7который оглушает любого игрока", "&7which stuns any player"),
                    line("&7при попадании на несколько секунд.", "&7for a few seconds on hit."),
                    cooldownLine(cageCooldown)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getBloodLust(int kills) {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int infectionCooldown = plugin.getConfig().getInt("bloodlust.infection.cooldown", 15);
        int bloodTrailCooldown = plugin.getConfig().getInt("bloodlust.blood-trail.cooldown", 60);
        int bloodHookCooldown = plugin.getConfig().getInt("bloodlust.blood-hook.cooldown", 30);
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#8B0000", "#FF5555", "#8B0000", tr("Жажда крови", "ʙʟᴏᴏᴅʟᴜsᴛ")));
            meta.setCustomModelData(3);
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(plugin.getKillsKey(), PersistentDataType.INTEGER, kills);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            meta.lore(lore(
                    line("&o&8Могущественный клинок, созданный из застывшей крови.", "&o&8A mighty blade forged from congealed blood."),
                    line("&o&8Чем больше вы убиваете им, тем сильнее становитесь.", "&o&8The more you kill with it, the stronger you become."),
                    blank(),
                    line("&b!! &3SHIFT+ЛКМ, чтобы просмотреть счетчик убийств.", "&b!! &3Crouch+LMB to view the kill counter."),
                    blank(),
                    line("&eИнфекция &6(0+ Убийств)", "&eɪɴꜰᴇᴄᴛɪᴏɴ &6(0+ kills)"),
                    line("&715% шанс вызвать кровотечение у врага при ударе.", "&715% chance to inflict bleeding on an enemy when you hit them."),
                    cooldownLine(infectionCooldown),
                    blank(),
                    line("&eСкорость II &6(1+ Убийство)", "&esᴘᴇᴇᴅ ɪɪ &6(1+ kill)"),
                    line("&7Получите постоянную Скорость II, когда", "&7Gain permanent Speed II while"),
                    line("&7держите Жажду крови.", "&7holding bloodlust."),
                    blank(),
                    line("&eКровавый Трекер &6(2+ Убийства)", "&eʙʟᴏᴏᴅ ᴛʀᴀᴄᴋᴇʀ &6(2+ kills)"),
                    line("&7Отслеживайте кровь ближайших игроков,", "&7Track the blood of nearby players"),
                    line("&7когда держите Жажду крови.", "&7while holding bloodlust."),
                    line("&7Только вы видите частицы.", "&7Only you can see the particles."),
                    blank(),
                    line("&eКровавый След &6(3+ Убийства)", "&eʙʟᴏᴏᴅ ᴛʀᴀɪʟ &6(3+ kills)"),
                    line("&7Погрузитесь в лужу крови &6[SHIFT+Смена Руки]", "&7Submerge yourself in a pool of blood &6[Crouch+OffHand]"),
                    cooldownLine(bloodTrailCooldown),
                    blank(),
                    line("&eСила I &6(4+ Убийства)", "&esᴛʀᴇɴɢᴛʜ ɪ &6(4+ kills)"),
                    line("&7Получите постоянную Силу I, когда", "&7Gain permanent Strength I while"),
                    line("&7держите Жажду крови.", "&7holding bloodlust."),
                    blank(),
                    line("&eКровавый Крюк &6(5+ Убийств)", "&eʙʟᴏᴏᴅ ʜᴏᴏᴋ &6(5+ kills)"),
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
        int transformationCooldown = plugin.getConfig().getInt("nightpiercer.transformation.cooldown", 20);
        int biteCooldown = plugin.getConfig().getInt("nightpiercer.bite.cooldown", 45);
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#4A0000", "#AA0000", "#4A0000", tr("Пронзитель ночи", "ɴɪɢʜᴛᴘɪᴇʀᴄᴇʀ")));
            meta.setCustomModelData(5);
            addCommonSwordEnchants(meta);

            meta.lore(lore(
                    line("&7Вы получаете пассивную &fРегенерацию I &7ночью,", "&7You gain passive &fRegeneration &7I at night,"),
                    line("&7когда держите Пронзитель ночи.", "&7while holding nightpiercer."),
                    blank(),
                    line("&o&8Vampiri sunt praedatores noctis ultimi.", "&o&8Vampiri sunt praedatores noctis ultimi."),
                    blank(),
                    line("&eТрансформация &6(SHIFT+Смена Руки)", "&eᴛʀᴀɴsꜰᴏʀᴍᴀᴛɪᴏɴ &6(Crouch+OffHand)"),
                    line("&7Временно превратитесь в стаю летучих мышей,", "&7Temporarily transform into a swarm of bats,"),
                    line("&7которая летит туда, куда вы смотрите.", "&7flying wherever you are looking."),
                    cooldownLine(transformationCooldown),
                    blank(),
                    line("&eБагровый укус &6(Смена Руки)", "&eᴄʀɪᴍsᴏɴ ʙɪᴛᴇ &6(OffHand)"),
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
        int magmaCooldown = plugin.getConfig().getInt("vulcan_crossbow.magma-attack.cooldown", 25);
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#FF4444", "#FFAA00", "#FF4444", tr("Арбалет вулкана", "ᴠᴜʟᴄᴀɴ ᴄʀᴏssʙᴏᴡ")));
            meta.setCustomModelData(1);
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
                    line("&eГнев вулкана &6(SHIFT+Выстрел)", "&eᴠᴜʟᴄᴀɴ ᴡʀᴀᴛʜ &6(Crouch+RMB)"),
                    line("&7Ваш следующий выстрел станет взрывным", "&7Your next shot becomes an explosive"),
                    line("&7огненным шаром, уничтожающим всё на пути.", "&7fireball that destroys everything in its path."),
                    cooldownLine(magmaCooldown),
                    blank(),
                    line("&eОгненный Залп &6(Пассивно)", "&eꜰɪʀᴇ ʙᴀʀʀᴀɢᴇ &6(passive)"),
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
        int shootCooldown = plugin.getConfig().getInt("pale-gun.shoot.cooldown", 10);
        int rootsCooldown = plugin.getConfig().getInt("pale-gun.pale-roots.cooldown", 25);
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#D7CFBC", "#F5F1E4", "#A99D83", tr("зцᴋᴇᴇп ᴄшцйлɸы", "ᴘᴡʀᴛᴛɢ ᴄɪᴡǫᴋᴀs")));
            meta.setCustomModelData(2);
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
        int iceCommandCooldown = plugin.getConfig().getInt("frost-scythe.ice-command.cooldown", 45);
        int throwCooldown = plugin.getConfig().getInt("frost-scythe.throw.cooldown", 30);
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#00AAAA", "#55FFFF", "#00AAAA", tr("Морозная Коса", "ꜰʀᴏsᴛ sᴄʏᴛʜᴇ")));
            meta.setCustomModelData(4);
            meta.setItemModel(NamespacedKey.fromString("minecraft:netherite_sword"));
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

            meta.lore(lore(
                    line("&o&8Древняя коса, использовавшаяся для жатвы", "&o&8An ancient scythe once used to harvest"),
                    line("&o&8ледяных морей. Коса холодна на ощупь.", "&o&8the frozen seas. The scythe is cold to the touch."),
                    blank(),
                    line("&eПовеление Льда &f\uE80E &6(SHIFT+ПКМ)", "&eᴄᴏᴍᴍᴀɴᴅ ᴏꜰ ɪᴄᴇ &f\uE80E &6(Crouch+RMB)"),
                    line("&7Призывает три блока льда над вашей", "&7Summons three blocks of ice above your"),
                    line("&7головой, которые затем летят во врагов.", "&7head, which then fly toward enemies."),
                    cooldownLine(iceCommandCooldown),
                    blank(),
                    line("&eБросок Косы &6(Зажать ПКМ)", "&esᴄʏᴛʜᴇ ᴛʜʀᴏᴡ &6(Hold RMB)"),
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
        int shadeSoulCooldown = plugin.getConfig().getInt("pure-blade.shade-soul.cooldown", 45);
        int cycloneCooldown = plugin.getConfig().getInt("pure-blade.cyclone-slash.cooldown", 30);
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#AAAAAA", "#FFFFFF", "#AAAAAA", tr("Чистый Клинок", "ᴘᴜʀᴇ ʙʟᴀᴅᴇ")));
            meta.setCustomModelData(7);
            addCommonSwordEnchants(meta);

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
        return getKnightfallCustom(kills, 1, false);
    }

    public static ItemStack getKnightfallCustom(int kills, int cmd, boolean hasDensity) {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int grappleCooldown = plugin.getConfig().getInt("knightfall.grapple.cooldown", 10);
        int hammerThrowCooldown = plugin.getConfig().getInt("knightfall.throw.cooldown", 20);
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#AAAAAA", "#FFFFFF", "#AAAAAA", tr("Падший Рыцарь", "ᴋɴɪɢʜᴛꜰᴀʟʟ")));
            meta.setCustomModelData(cmd);
            meta.setUnbreakable(true);

            if (kills >= 2) {
                meta.addEnchant(Enchantment.WIND_BURST, 1, true);
            }
            if (hasDensity) {
                meta.addEnchant(Enchantment.DENSITY, 2, true);
            }

            meta.getPersistentDataContainer().set(plugin.getKillsKey(), PersistentDataType.INTEGER, kills);
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
                    line("&f\uE80D &8x3 заряда (" + grappleCooldown + "с перезарядка)", "&f\uE80D &8x3 charges (" + grappleCooldown + "s cooldown)"),
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
        int leapCooldown = plugin.getConfig().getInt("shadow-blade.leap.cooldown", 30);
        int daggersCooldown = plugin.getConfig().getInt("shadow-blade.daggers.cooldown", 45);
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#AAAAAA", "#FFFFFF", "#AAAAAA", tr("ᴛᴇнᴇʙой ᴋлиноᴋ", "sʜᴀᴅᴏᴡ ʙʟᴀᴅᴇ")));
            meta.setCustomModelData(10);
            addCommonSwordEnchants(meta);

            meta.lore(lore(
                    line("&o&8Клинок, выкованный из отсутствия света.", "&o&8A blade forged from the absence of light."),
                    line("&o&8Даже для тени, он выглядит невероятно острым.", "&o&8Even for a shadow, it looks impossibly sharp."),
                    blank(),
                    line("&f&lᴛᴇнᴇʙой прыжоᴋ", "&f&lsʜᴀᴅᴏᴡ ʟᴇᴀᴘ"),
                    line("&f\uE80E &f[SHIFT+ПКМ]", "&f\uE80E &f[Crouch+RMB]"),
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

    public static ItemStack getHyperion() {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        int scorchingCooldown = plugin.getConfig().getInt("hyperion.scorching-blade.cooldown", 30);
        int holyLanceCooldown = plugin.getConfig().getInt("hyperion.holy-lance.cooldown", 60);
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#FFC000", "#FF4000", "#FFC000", tr("Гиперион", "ʜʏᴘᴇʀɪᴏɴ")));
            meta.setCustomModelData(6);
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(AltarLegendaryWH.getInstance().getHyperionKey(), PersistentDataType.BYTE, (byte) 1);

            meta.lore(lore(
                    line("&7Вы неуязвимы к огню, когда держите Гиперион.", "&7You are immune to fire while holding hyperion."),
                    line("&7Ваши способности накладывают &fСвященное Пламя &7на врагов.", "&7Your abilities apply &fSacred Flame &7to enemies."),
                    blank(),
                    line("&o&8Не уходи безропотно во тьму.", "&o&8Do not go gentle into that good night."),
                    line("&o&8Бунтуй, бунтуй против угасания света.", "&o&8Rage, rage against the dying of the light."),
                    blank(),
                    line("&e(Шифт + Смена Руки) Обжигающий Клинок", "&e(Crouch+OffHand) sᴄᴏʀᴄʜɪɴɢ ʙʟᴀᴅᴇ"),
                    line("&7Наполните свой следующий взмах святым огнем,", "&7Charge your next swing with holy fire,"),
                    line("&7запуская клинок пламени во врага.", "&7launching a blade of flame at the enemy."),
                    cooldownLine(scorchingCooldown),
                    blank(),
                    line("&e(Смена руки) Святое Копье", "&e(OffHand) ʜᴏʟʏ ʟᴀɴᴄᴇ"),
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
        int dashCooldown = plugin.getConfig().getInt("wither-blade.dash.cooldown", 30);
        int auraCooldown = plugin.getConfig().getInt("wither-blade.aura.cooldown", 50);
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#2b2b2b", "#5c5c5c", "#2b2b2b", tr("Иссушенный Костяной Клинок", "ᴡɪᴛʜᴇʀᴇᴅ ʙᴏɴᴇ ʙʟᴀᴅᴇ")));
            meta.setCustomModelData(9);
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
        int meteorCooldown = plugin.getConfig().getInt("earth-gauntlet.meteor_strike.cooldown", 35);
        int mudslideCooldown = plugin.getConfig().getInt("earth-gauntlet.mudslide.cooldown", 40);
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#d6a058", "#8a5e27", "#d6a058", tr("Землянная Перчатка", "ᴇᴀʀᴛʜ ɢᴀᴜɴᴛʟᴇᴛ")));
            meta.setCustomModelData(8);
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
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(gradientTitle("#179DD0", "#59BEDF", "#179DD0", tr("Абордажная Сабля", "ᴄᴜᴛʟᴀss")));
            meta.setCustomModelData(11);
            addCommonSwordEnchants(meta);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "cutlass"), PersistentDataType.BYTE, (byte) 1);

            int thousandCooldown = plugin.getConfig().getInt("cutlass.thousand-cuts.cooldown", 45);
            int parryCooldown = plugin.getConfig().getInt("cutlass.parry.cooldown", 30);
            int parryCharges = plugin.getConfig().getInt("cutlass.parry.max-charges", 3);
            int parryDuration = plugin.getConfig().getInt("cutlass.parry.max-duration-seconds", 7);

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
                    line("&f\uE80D &8" + parryCooldown + "с перезарядка &7(" + parryCharges + " " + tr("заряда", "charges") + "&7)", "&f\uE80D &8" + parryCooldown + "s cooldown &7(" + parryCharges + " charges&7)")
            ));

            item.setItemMeta(meta);
        }

        if (plugin.getCutlassManager() != null) {
            double damage = plugin.getConfig().getDouble("cutlass.base.damage", 8.0);
            double speed = plugin.getConfig().getDouble("cutlass.base.attack-speed", 1.6);
            plugin.getCutlassManager().setCutlassAttributes(item, damage, speed);
        }

        return item;
    }
}
