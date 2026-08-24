package dev.whersss.altarLegendaryWH.commands;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import dev.whersss.altarLegendaryWH.utils.InventoryUtils;
import dev.whersss.altarLegendaryWH.items.copperarmor.utils.CopperArmorFactory;
import dev.whersss.altarLegendaryWH.items.illusioncore.IllusionCoreItem;
import dev.whersss.altarLegendaryWH.items.vulcanskull.VulcanSkullItem;
import dev.whersss.altarLegendaryWH.items.wardenheart.WardenHeartItem;
import dev.whersss.altarLegendaryWH.items.weaponshandle.WeaponsHandleItem;
import dev.whersss.altarLegendaryWH.utils.WeaponFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

public class AltarLegendaryCommand implements CommandExecutor {

    private final AltarLegendaryWH plugin;

    public AltarLegendaryCommand(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    public static void openLegendaryMenu(Player player) {
        AltarLegendaryWH plugin = AltarLegendaryWH.getInstance();
        String titleText = plugin.tr("Легендарные оружия", "Legendary weapons");
        Component title = TextUtils.shadow(MiniMessage.miniMessage()
                .deserialize("<!italic><gradient:#AAAAAA:#FFFFFF:#AAAAAA>" + titleText + "</gradient>"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack glass = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtils.legacy(" "));
            glass.setItemMeta(meta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        inv.setItem(0, WeaponFactory.getBoneBlade());
        inv.setItem(2, WeaponFactory.getKnightfall(0));
        inv.setItem(4, WeaponFactory.getShadowBlade());
        inv.setItem(6, WeaponFactory.getWitherBlade());
        inv.setItem(8, WeaponFactory.getHyperion());
        inv.setItem(10, WeaponFactory.getEarthGauntlet());
        inv.setItem(12, WeaponFactory.getFrostScythe());
        inv.setItem(14, WeaponFactory.getVulcanCrossbow());
        inv.setItem(16, WeaponFactory.getPureBlade());
        inv.setItem(18, WeaponFactory.getPaleGun());
        inv.setItem(20, WeaponFactory.getNightpiercer());
        inv.setItem(22, WeaponFactory.getCutlass());
        inv.setItem(24, WeaponFactory.getBloodLust(0));

        player.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("altarlegendary.admin")) {
            sender.sendMessage(TextUtils.legacy("§c" + plugin.tr("У вас нет прав.", "You do not have permission.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(TextUtils.legacy("§e" + plugin.tr("Использование: /al <give|show|reload|cooldownreset> ...", "Usage: /al <give|show|reload|cooldownreset> ...")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
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
                    openLegendaryMenu(player);
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
            default -> sender.sendMessage(TextUtils.legacy("§e" + plugin.tr("Использование: /al <give|show|reload|cooldownreset> ...", "Usage: /al <give|show|reload|cooldownreset> ...")));
        }
        return true;
    }
}
