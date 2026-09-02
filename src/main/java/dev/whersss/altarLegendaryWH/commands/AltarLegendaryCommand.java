package dev.whersss.altarLegendaryWH.commands;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.InventoryUtils;
import dev.whersss.altarLegendaryWH.items.copperarmor.utils.CopperArmorFactory;
import dev.whersss.altarLegendaryWH.items.copperpickaxe.CopperPickaxeItem;
import dev.whersss.altarLegendaryWH.items.illusioncore.IllusionCoreItem;
import dev.whersss.altarLegendaryWH.items.vulcanskull.VulcanSkullItem;
import dev.whersss.altarLegendaryWH.items.wardenheart.WardenHeartItem;
import dev.whersss.altarLegendaryWH.items.weaponshandle.WeaponsHandleItem;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AltarLegendaryCommand implements CommandExecutor {

    private static final int MENU_SIZE = 18;

    private final AltarLegendaryWH plugin;

    public AltarLegendaryCommand(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    private static ItemStack filler() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtils.legacy(" "));
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private static void openMenu(Player player, String titleText, List<ItemStack> items) {
        Component title = TextUtils.shadow(Component.text(titleText, NamedTextColor.GRAY));
        Inventory inv = Bukkit.createInventory(null, MENU_SIZE, title);

        for (int i = 0; i < MENU_SIZE; i++) {
            inv.setItem(i, filler());
        }

        for (int i = 0; i < items.size() && i < MENU_SIZE; i++) {
            inv.setItem(i, items.get(i));
        }

        player.openInventory(inv);
    }

    public static void openWeaponsMenu(Player player) {
        openMenu(player,
                "Weapons",
                List.of(
                        WeaponFactory.getBoneBlade(),
                        WeaponFactory.getBloodLust(0),
                        WeaponFactory.getNightpiercer(),
                        WeaponFactory.getVulcanCrossbow(),
                        WeaponFactory.getPaleGun(),
                        WeaponFactory.getFrostScythe(),
                        WeaponFactory.getPureBlade(),
                        WeaponFactory.getKnightfall(0),
                        WeaponFactory.getShadowBlade(),
                        WeaponFactory.getWindWeaver(),
                        WeaponFactory.getHyperion(),
                        WeaponFactory.getWitherBlade(),
                        WeaponFactory.getEarthGauntlet(),
                        WeaponFactory.getCutlass()
                ));
    }

    public static void openItemsMenu(Player player) {
        openMenu(player,
                "Items",
                List.of(
                        WeaponsHandleItem.create(),
                        IllusionCoreItem.create(),
                        WardenHeartItem.create(),
                        VulcanSkullItem.create(),
                        CopperArmorFactory.getHelmet(),
                        CopperArmorFactory.getChestplate(),
                        CopperArmorFactory.getLeggings(),
                        CopperArmorFactory.getBoots(),
                        CopperPickaxeItem.create()
                ));
    }

    public static void openLegendaryMenu(Player player) {
        openWeaponsMenu(player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("altarlegendary.admin")) {
            sender.sendMessage(TextUtils.legacy("§c" + plugin.tr("У вас нет прав.", "You do not have permission.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(TextUtils.legacy("§e" + plugin.tr("Использование: /al <give|show weapons|show items|reload|cooldownreset> ...", "Usage: /al <give|show weapons|show items|reload|cooldownreset> ...")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.reloadWeaponsConfig();
                plugin.reloadItemsConfig();
                plugin.registerRecipes();
                sender.sendMessage(TextUtils.legacy("§2" + plugin.tr("Конфиг был перезагружен!", "Config reloaded!")));
            }
            case "cooldownreset" -> {
                if (args.length < 2) {
                    sender.sendMessage(TextUtils.legacy("§e" + plugin.tr("Использование: /al cooldownreset <игрок>", "Usage: /al cooldownreset <player>")));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(TextUtils.legacy("§c" + plugin.tr("Игрок не найден.", "Player not found.")));
                    return true;
                }

                plugin.resetAllCooldowns(target);
                sender.sendMessage(TextUtils.legacy("§a" + plugin.tr("Перезарядки сброшены для ", "Cooldowns reset for ") + target.getName()));
            }
            case "show" -> {
                if (sender instanceof Player player) {
                    if (args.length >= 2 && args[1].equalsIgnoreCase("items")) {
                        openItemsMenu(player);
                    } else if (args.length >= 2 && args[1].equalsIgnoreCase("weapons")) {
                        openWeaponsMenu(player);
                    } else {
                        sender.sendMessage(TextUtils.legacy("§e" + plugin.tr("Использование: /al show weapons|items", "Usage: /al show weapons|items")));
                    }
                } else {
                    sender.sendMessage(TextUtils.legacy(plugin.tr("§cКоманда доступна только игроку.", "§cThis command is only available to players.")));
                }
            }
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(TextUtils.legacy("§e" + plugin.tr("Использование: /al give <предмет> <игрок> [убийства]", "Usage: /al give <item> <player> [kills]")));
                    return true;
                }

                String weapon = args[1].toLowerCase();
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(TextUtils.legacy("§c" + plugin.tr("Игрок не найден.", "Player not found.")));
                    return true;
                }

                int kills = 0;
                if (args.length >= 4) {
                    try {
                        kills = Math.max(0, Integer.parseInt(args[3]));
                    } catch (NumberFormatException ignored) {
                    }
                }

                ItemStack itemToGive = switch (weapon) {
                    case "boneblade" -> WeaponFactory.getBoneBlade();
                    case "bloodlust" -> WeaponFactory.getBloodLust(kills);
                    case "nightpiercer" -> WeaponFactory.getNightpiercer();
                    case "vulcan" -> WeaponFactory.getVulcanCrossbow();
                    case "pale_gun" -> WeaponFactory.getPaleGun();
                    case "frost_scythe" -> WeaponFactory.getFrostScythe();
                    case "pure_blade" -> WeaponFactory.getPureBlade();
                    case "knightfall" -> WeaponFactory.getKnightfall(kills);
                    case "shadow_blade" -> WeaponFactory.getShadowBlade();
                    case "windweaver" -> WeaponFactory.getWindWeaver();
                    case "hyperion" -> WeaponFactory.getHyperion();
                    case "wither_blade" -> WeaponFactory.getWitherBlade();
                    case "earth_gauntlet" -> WeaponFactory.getEarthGauntlet();
                    case "cutlass" -> WeaponFactory.getCutlass();
                    case "warden_heart" -> WardenHeartItem.create();
                    case "vulcan_skull" -> VulcanSkullItem.create();
                    case "weapons_handle" -> WeaponsHandleItem.create();
                    case "illusion_core" -> IllusionCoreItem.create();
                    case "copper_helmet" -> CopperArmorFactory.getHelmet();
                    case "copper_chestplate" -> CopperArmorFactory.getChestplate();
                    case "copper_leggings" -> CopperArmorFactory.getLeggings();
                    case "copper_boots" -> CopperArmorFactory.getBoots();
                    case "copper_pickaxe" -> CopperPickaxeItem.create();
                    default -> null;
                };

                if (itemToGive == null) {
                    sender.sendMessage(TextUtils.legacy("§c" + plugin.tr("Неизвестный предмет.", "Unknown item.")));
                    return true;
                }

                InventoryUtils.giveOrDrop(target, itemToGive);

                String itemName = plugin.tr("предмет", "item");
                if (itemToGive.hasItemMeta() && itemToGive.getItemMeta().hasDisplayName()) {
                    itemName = PlainTextComponentSerializer.plainText().serialize(itemToGive.getItemMeta().displayName());
                }

                boolean needsKills = weapon.equals("bloodlust") || weapon.equals("knightfall");
                String suffix = needsKills
                        ? plugin.tr(" §2(Убийств: §6", " §2(Kills: §6") + kills + "§2)"
                        : "";
                sender.sendMessage(TextUtils.legacy("§2" + target.getName() + " " + plugin.tr("получил §6", "received §6") + itemName + suffix));
            }
            default -> sender.sendMessage(TextUtils.legacy("§e" + plugin.tr("Использование: /al <give|show weapons|show items|reload|cooldownreset> ...", "Usage: /al <give|show weapons|show items|reload|cooldownreset> ...")));
        }
        return true;
    }
}