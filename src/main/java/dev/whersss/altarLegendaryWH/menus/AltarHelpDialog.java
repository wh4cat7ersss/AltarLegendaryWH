package dev.whersss.altarLegendaryWH.menus;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AltarHelpDialog {

    public static void open(Player player, AltarLegendaryWH plugin) {
        boolean ru = !plugin.isEnglish();

        try {
            // Attempt native Paper 1.21.6+ Dialog API
            io.papermc.paper.dialog.Dialog dialog = io.papermc.paper.dialog.Dialog.create(factory -> {
                Component title = TextUtils.shadow(Component.text(
                        ru ? "AltarLegendaryWH — Руководство" : "AltarLegendaryWH — Tutorial Guide",
                        NamedTextColor.GOLD,
                        TextDecoration.BOLD
                ));

                List<io.papermc.paper.registry.data.dialog.body.DialogBody> bodies = new ArrayList<>();

                String tutorialText = ru ?
                        """
                        §6§l=== РУКОВОДСТВО ПО ПЛАГИНУ ===§r

                        §e§l1. Алтари Спавна (/al altar):§r
                        §7• §f/al altar give <игрок> §7— выдать предмет спавна алтаря
                        §7• §f/al altar create <id> <имя> §7— создать новый алтарь на месте
                        §7• §f/al altar set <id> recipe §7— настроить рецепт крафта
                        §7• §f/al altar set <id> result §7— задать награду (легендарку)
                        §7• §f/al altar set <id> cooldown <сек> §7— задать время отката
                        §7• §f/al altar save §7— сохранить все алтари в altars.yml
                        §7• При готовности алтаря проигрывается звук рога на весь сервер!

                        §e§l2. Легендарные Оружия (/al):§r
                        §7• §f/al show weapons §7— визуальное меню всех легендарок
                        §7• §f/al show items §7— меню ядер, сердец и медной брони
                        §7• §f/al give <оружие> <игрок> [киллы] §7— выдать оружие
                        §7• §f/al cooldown reset <игрок> §7— сбросить все перезарядки

                        §e§l3. Боевой Топор Паладина:§r
                        §7• [Смена Руки] §6ᴄоᴋрушᴇниᴇ зᴇмли §7— призыв гигантского топора,
                          наносящего прямой урон под лезвием и поднимающего волну блоков.
                        §7• [Shift + Смена Руки] §6ᴄᴛойᴋоᴇ поглощᴇниᴇ §7— поглощение
                          всего входящего урона на 5 сек с мощным взрывом в конце.

                        §e§l4. бᴇзумныᴇ ᴄлоᴛы:§r
                        §7• [ПКМ] Превращается в случайную легендарку на 30 сек!
                        §7• Перезарядка: 60 секунд с таймером в BossBar.
                        §7• Пул выпадающих оружий настраивается в weapons.yml.

                        §e§l5. WorldGuard и Безопасные Зоны:§r
                        §7• Флаг региона: §aaltar-weapons deny/allow§r
                        §7• В зонах с deny способности, дисплеи и притягивания отключены.

                        §e§l6. Союзники и Друзья:§r
                        §7• Команды: §f/friendlist §7или §f/trust <add|remove|list>§r
                        §7• Друзья полностью защищены от ваших легендарных способностей!
                        """ :
                        """
                        §6§l=== PLUGIN TUTORIAL GUIDE ===§r

                        §e§l1. Spawn Altars (/al altar):§r
                        §7• §f/al altar give <player> §7— give the altar placer item
                        §7• §f/al altar create <id> <name> §7— create a new altar at your spot
                        §7• §f/al altar set <id> recipe §7— configure the craft recipe
                        §7• §f/al altar set <id> result §7— set reward legendary item
                        §7• §f/al altar set <id> cooldown <sec> §7— set altar cooldown
                        §7• §f/al altar save §7— save all altars to altars.yml
                        §7• When an altar is ready, a server-wide goat horn sound is broadcast!

                        §e§l2. Legendary Weapons (/al):§r
                        §7• §f/al show weapons §7— visual menu of all legendary weapons
                        §7• §f/al show items §7— menu of cores, shards and copper armor
                        §7• §f/al give <weapon> <player> [kills] §7— give specific weapon
                        §7• §f/al cooldownreset <player> §7— reset player ability cooldowns

                        §e§l3. Paladin's Battle Axe:§r
                        §7• [Offhand] §6ᴇᴀʀᴛʜsʜᴀᴛᴛᴇʀ §7— summons a giant spectral axe,
                          dealing heavy direct impact damage and an earth block shockwave.
                        §7• [Shift + Offhand] §6sᴛᴀʟᴡᴀʀᴛ ᴀʙsᴏʀᴘᴛɪᴏɴ §7— absorbs all
                          incoming damage for 5s then releases a powerful shockwave.

                        §e§l4. ᴄʀᴀᴢʏ sʟᴏᴛs:§r
                        §7• [RMB] Transforms into a random legendary weapon for 30 seconds!
                        §7• Cooldown: 60 seconds with live BossBar countdown.
                        §7• Drop pool is fully configurable in weapons.yml.

                        §e§l5. WorldGuard & Safe Zones:§r
                        §7• Region flag: §aaltar-weapons deny/allow§r
                        §7• In deny zones, abilities, projectiles and pulls are strictly blocked.

                        §e§l6. Allies & Friends:§r
                        §7• Commands: §f/friendlist §7or §f/trust <add|remove|list>§r
                        §7• Friends take zero damage from your legendary abilities!
                        """;

                bodies.add(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(TextUtils.legacy(tutorialText)));

                io.papermc.paper.registry.data.dialog.DialogBase base = io.papermc.paper.registry.data.dialog.DialogBase.builder(title)
                        .externalTitle(title)
                        .canCloseWithEscape(true)
                        .body(bodies)
                        .build();

                List<io.papermc.paper.registry.data.dialog.ActionButton> actionButtons = new ArrayList<>();

                // Weapons Menu Button
                actionButtons.add(io.papermc.paper.registry.data.dialog.ActionButton.builder(
                                Component.text(ru ? "⚔ Оружия" : "⚔ Weapons", NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .tooltip(Component.text(ru ? "Открыть меню легендарных оружий" : "Open legendary weapons menu", NamedTextColor.GRAY))
                        .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(ClickEvent.runCommand("/al show weapons")))
                        .build());

                // Items Menu Button
                actionButtons.add(io.papermc.paper.registry.data.dialog.ActionButton.builder(
                                Component.text(ru ? "📦 Предметы" : "📦 Items", NamedTextColor.AQUA, TextDecoration.BOLD))
                        .tooltip(Component.text(ru ? "Открыть меню ядер и брони" : "Open items and armor menu", NamedTextColor.GRAY))
                        .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(ClickEvent.runCommand("/al show items")))
                        .build());

                // Altars Help Button
                actionButtons.add(io.papermc.paper.registry.data.dialog.ActionButton.builder(
                                Component.text(ru ? "🏛 Алтари" : "🏛 Altars", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .tooltip(Component.text(ru ? "Показать команды алтарей" : "Show altar commands", NamedTextColor.GRAY))
                        .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(ClickEvent.runCommand("/al altar help")))
                        .build());

                // Discord Button
                actionButtons.add(io.papermc.paper.registry.data.dialog.ActionButton.builder(
                                Component.text("💬 Discord", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                        .tooltip(Component.text(ru ? "Скачать актуальный ресурспак" : "Download latest resource pack", NamedTextColor.GRAY))
                        .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(ClickEvent.openUrl("https://dsc.gg/WHersssDevNotes")))
                        .build());

                io.papermc.paper.registry.data.dialog.ActionButton exitButton = io.papermc.paper.registry.data.dialog.ActionButton.builder(
                                Component.text(ru ? "✖ Закрыть" : "✖ Close", NamedTextColor.RED))
                        .build();

                io.papermc.paper.registry.data.dialog.type.MultiActionType multiAction = io.papermc.paper.registry.data.dialog.type.DialogType.multiAction(actionButtons)
                        .exitAction(exitButton)
                        .columns(2)
                        .build();

                factory.empty().base(base).type(multiAction);
            });

            player.showDialog(dialog);
        } catch (Throwable t) {
            // Fallback for environments or clients where native Dialog UI is unavailable
            sendChatFallback(player, ru);
        }
    }

    public static void sendChatFallback(Player player, boolean ru) {
        if (ru) {
            player.sendMessage(TextUtils.legacy("""
                    §6§l================== [AltarLegendaryWH — Обучение] ==================§r
                    §e1. Алтари Спавна (/al altar):
                    §7• §f/al altar give <игрок> §7— выдать спавнер алтаря
                    §7• §f/al altar create <id> <имя> §7— создать алтарь
                    §7• §f/al altar set <id> recipe|result|cooldown §7— настройка
                    §7• §f/al altar save §7— сохранить настройки в altars.yml
                    §e2. Легендарные Оружия:
                    §7• §f/al show weapons §7— интерактивное меню оружий
                    §7• §f/al show items §7— меню ядер, сердец и медной брони
                    §7• §f/al give <оружие> <игрок> [киллы] §7— выдать предмет
                    §e3. Боевой Топор Паладина:
                    §7• [Смена Руки] §6ᴄоᴋрушᴇниᴇ зᴇмли §7(мощный удар и волна блоков)
                    §7• [Shift + Смена Руки] §6ᴄᴛойᴋоᴇ поглощᴇниᴇ §7(поглощение урона на 5с)
                    §e4. бᴇзумныᴇ ᴄлоᴛы:
                    §7• [ПКМ] Случайная легендарка на 30с (настройка в weapons.yml)
                    §e5. WorldGuard:
                    §7• Флаг: §aaltar-weapons deny/allow
                    §e6. Список друзей:
                    §7• Команды: §f/friendlist §7или §f/trust <add|remove|list>
                    §6§l================================================================§r"""));
        } else {
            player.sendMessage(TextUtils.legacy("""
                    §6§l================ [AltarLegendaryWH — Tutorial] ================§r
                    §e1. Spawn Altars (/al altar):
                    §7• §f/al altar give <player> §7— give altar placer item
                    §7• §f/al altar create <id> <name> §7— create altar
                    §7• §f/al altar set <id> recipe|result|cooldown §7— configure altar
                    §7• §f/al altar save §7— save altars to altars.yml
                    §e2. Legendary Weapons:
                    §7• §f/al show weapons §7— interactive weapon menu
                    §7• §f/al show items §7— items, cores, and armor menu
                    §7• §f/al give <weapon> <player> [kills] §7— give weapon
                    §e3. Paladin's Battle Axe:
                    §7• [Offhand] §6ᴇᴀʀᴛʜsʜᴀᴛᴛᴇʀ §7(heavy direct impact & block wave)
                    §7• [Shift + Offhand] §6sᴛᴀʟᴡᴀʀᴛ ᴀʙsᴏʀᴘᴛɪᴏɴ §7(absorb damage for 5s)
                    §e4. ᴄʀᴀᴢʏ sʟᴏᴛs:
                    §7• [RMB] Random legendary weapon for 30s (configured in weapons.yml)
                    §e5. WorldGuard:
                    §7• Flag: §aaltar-weapons deny/allow
                    §e6. Friends:
                    §7• Commands: §f/friendlist §7or §f/trust <add|remove|list>
                    §6§l================================================================§r"""));
        }
    }
}
