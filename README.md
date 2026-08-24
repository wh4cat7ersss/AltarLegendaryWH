# AltarLegendaryWH

An original Paper plugin created by **WHersss** for Minecraft 1.21.11. It adds legendary weapons, copper armor, crafting materials, combat abilities, and visual effects.

AltarLegendaryWH includes 13 legendary weapons, four copper armor pieces, custom crafting materials, a friend system, configurable limits, Russian and English localization, and unique effects. Item names, lore, chat messages, action bars, titles, holograms, and boss bars use Adventure components with text shadows.

## Requirements

- Paper 1.21.11
- Java 21
- Maven 3.9+ only when building from source
- No external plugins are required

A server resource pack is recommended for custom models and sounds. The gameplay mechanics still work without one, but custom model data will use vanilla appearances and the `bloodlust.*` sounds may be missing.

## Installation

1. Download a release or build the project with `mvn clean package`.
2. Move `target/AltarLegendaryWH-1.3.0.jar` into the Paper server's `plugins` directory.
3. Start the server once to generate `config.yml`, `items.yml`, and `friends.yml`.
4. Configure the language, abilities, limits, recipes, and copper armor.
5. Run `/al reload` or restart the server.

## Quick Start

- `/al show` — open the legendary item menu.
- `/al give <id> <player> [kills]` — give an item to a player.
- `/al cooldownreset <player>` — reset a player's ability cooldowns.
- `/al reload` — reload both configuration files and recipes.
- `/fl add <player>` — add a friend who will not be affected by friendly abilities.
- `/fl remove <player>` — remove a friend.
- `/fl list` — display the friend list.

Administrative commands require the `altarlegendary.admin` permission, which is granted to server operators by default.

## Item IDs

| Category | IDs for `/al give` |
| --- | --- |
| Weapons | `boneblade`, `bloodlust`, `nightpiercer`, `vulcan`, `pale_gun`, `frost_scythe`, `pure_blade`, `knightfall`, `shadow_blade`, `hyperion`, `wither_blade`, `earth_gauntlet`, `cutlass` |
| Materials | `warden_heart`, `vulcan_skull`, `weapons_handle`, `illusion_core` |
| Copper armor | `copper_helmet`, `copper_chestplate`, `copper_leggings`, `copper_boots` |

For `bloodlust` and `knightfall`, the fourth argument sets the initial kill count. Example: `/al give bloodlust Steve 5`.

## Weapons

The main controls are swap-hand, Shift + swap-hand, right-click, and Shift + right-click. The exact controls and ability descriptions are always shown in each item's lore.

| Weapon | Main mechanics |
| --- | --- |
| Bone Blade | Directional dash and a stunning bone cage |
| Bloodlust | Infection, blood trail, and hook; new passives unlock through kills |
| Night Piercer | Crimson bite and transformation into a swarm of bats |
| Vulcan Crossbow | Explosive magma shot and arrow volley |
| Pale Weapon | Explosive projectile and pale roots |
| Frost Scythe | Controllable scythe throw and ice command |
| Pure Blade | Soul shadow and spinning slash |
| Knightfall | Cloak, hook, passives, and hammer throw unlocked through kills |
| Shadow Blade | Shadow leap, three daggers, and a passive backstab |
| Hyperion | Scorching blade and holy spear |
| Wither Blade | Three jump charges and release of accumulated wither energy |
| Earth Gauntlet | Meteor strike and a pulling mudslide |
| Cutlass | Rapid slash sequence and parry stance |

## Copper Armor

- The helmet grants infinite Water Breathing while underwater. Holding Shift for nine seconds activates Copper Vision, making other players glow only for the wearer. It uses Paper's per-viewer API and does not enable global glowing.
- The chestplate grants infinite Resistance, protects against lightning, and creates a lightning ring after a configurable number of hits.
- The leggings cancel fall damage and create a shockwave whose power depends on fall height.
- The boots grant infinite Fire Resistance and Speed, with a separate boosted speed level on copper blocks.

Armor effects are tracked by their source. Removing an armor piece only removes effects applied by this plugin. Infinite effects from commands, beacons, or other plugins are preserved.

## Configuration

`config.yml` controls the language, global restrictions, damage, durations, radii, charges, and weapon cooldowns.

Important settings:

- `lang`: `ru_RU` or `en_US`.
- `limits.enabled`: enables height and item-count restrictions.
- `limits.max-y-height`: disables abilities and armor above this Y coordinate.
- `limits.max-legendary-weapons`: maximum number of legendary weapons in an inventory.
- `limits.max-copper-armor`: maximum number of copper armor pieces equipped at once.
- `bone-blade`, `bloodlust`, `nightpiercer`, `vulcan_crossbow`, `pale-gun`, `frost-scythe`, `pure-blade`, `knightfall`, `shadow-blade`, `hyperion`, `wither-blade`, `earth-gauntlet`, and `cutlass` configure their respective abilities.

`items.yml` controls Warden Heart drops, material recipes, and every copper armor setting. Existing configuration files are not overwritten automatically during updates, so new options may need to be copied from `src/main/resources`.

## BLOCK and BLOCK_CRUMBLE Particles

Bukkit provides a special vanilla directional mode when `count` is set to `0`: `offsetX`, `offsetY`, and `offsetZ` become the direction of a single particle, while `extra` controls its speed. Bloodlust and Earth Gauntlet use this mode for their impact debris.

After spawning, a regular `BLOCK` particle is still affected by client-side gravity, which the server cannot disable. Continuously rendered straight lines therefore use short-lived `BLOCK_CRUMBLE` particles with zero initial speed. This reduces the falling trail while preserving the block texture.

## Project Structure

```text
src/main/java/dev/whersss/altarLegendaryWH
├── AltarLegendaryWH.java       — startup, configuration, recipes, and registration
├── commands/                   — administrative menu and friend list
├── items/                      — materials and copper armor
├── listeners/                  — shared game events and item protection
├── managers/                   — friend storage
├── utils/                      — weapon factory, direct damage, and text components
└── weapons/<weapon>/           — listeners, managers, and scheduled tasks for each weapon
```

Plugin resources are located in `src/main/resources`: `plugin.yml`, `config.yml`, and `items.yml`.

## Building from Source

```bash
git clone <repository-url>
cd AltarLegendaryWH
mvn clean package
```

The compiled plugin will be available in `target`. The project builds only against the public `paper-api` and does not require a local server jar.

## Publishing on GitHub

If the repository has not been initialized yet:

```bash
git init
git add .
git commit -m "Initial release 1.3.0"
git branch -M main
git remote add origin <repository-url>
git push -u origin main
```

Attach the compiled jar to a GitHub Release instead of committing it to the source tree. The included `.gitignore` already excludes local jar files and the `target` directory.

## License and Attribution

Copyright (c) 2026 **WHersss**. The original plugin and its source code were created by WHersss.

This project is licensed under the [MIT License](LICENSE). You may use, copy, modify, publish, and distribute the code, including in commercial projects. The copyright notice and MIT permission notice must remain in all copies or substantial portions of the source code.
