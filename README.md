<div align="center">

# BiteDisplays

**NPCs, holograms, and item and block displays, placed and styled in-game.**

[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-FF0368?style=flat-square)](#compatibility)
[![Spigot | Paper | Purpur](https://img.shields.io/badge/Spigot%20%7C%20Paper%20%7C%20Purpur-supported-4ECDC4?style=flat-square)](#compatibility)
[![Java 25](https://img.shields.io/badge/Java-25-7209B7?style=flat-square)](#building-from-source)
[![Latest release](https://img.shields.io/github/v/release/bite-size/BiteDisplays?style=flat-square&color=FF0368&label=release)](https://github.com/bite-size/BiteDisplays/releases)
[![bStats servers](https://img.shields.io/bstats/servers/34592?style=flat-square&color=4ECDC4&label=servers)](https://bstats.org/plugin/bukkit/BiteDisplays/34592)

</div>

---

BiteDisplays is a lightweight way to bring your spawn, shops, and builds to life. Place NPCs with real player skins, floating text, and item or block displays right where you stand, style them with MiniMessage, and have them run commands when players click them. Everything is managed in-game with a single command.

```text
/bd create npc <gradient:#FF0368:#4ECDC4>Bite Keeper</gradient>
/bd skin Notch
/bd cmd console warp shop @p
/bd create text <#FF0368>Welcome to the server
/bd glow aqua
```

## Highlights

- **Four entity types.** NPCs with skins, equipment, and any size, multi-line text displays, and displays of any item or block. NPCs can't be hurt, even by players in creative mode.
- **Click actions.** NPCs, text, and item and block displays run a command when right-clicked, as the console or as the player who clicked, with `@p` filled in. NPCs can play a sound too.
- **Modern formatting.** MiniMessage gradients, hex colors, and decorations in names, descriptions, and text lines. Classic `&` color codes work too.
- **Point and edit.** New entities are selected for you. Look at any other one and `/bd select` it, or click **[SELECT]** in `/bd list`, then move, inspect, or remove it.
- **Smart tab completion.** Suggests actions, glow colors, every game sound, online players for skins, entity IDs, and the lines on the text display you've selected.
- **Lightweight.** Entities are vanilla mannequins and display entities, saved to one file and respawned as their chunks load. No extra libraries to install.

## Getting started

1. Download the latest jar from [Releases](https://github.com/bite-size/BiteDisplays/releases).
2. Drop it into your server's `plugins/` folder.
3. Restart the server.

> [!NOTE]
> On its first start, the server downloads the Kotlin standard library from Maven Central, so it needs internet access once.

## Commands

The main command is `/bitedisplays`, with the shorter alias `/bd`. New entities appear where you stand and are selected right away, so you can keep editing them. Every entity gets an ID like `npc-1` or `text-3`.

### Creating

| Command | What it does |
|---|---|
| `/bd create npc [name]` | Creates an NPC |
| `/bd create text <text>` | Creates a text display, with its first line |
| `/bd create item [name]` | Creates a display of the item you're holding |
| `/bd create block [name]` | Creates a display of the block you're holding |

### Editing your selection

Editing commands work on whatever you have selected, so you don't name the type. A command that doesn't fit your selection says which types it works on. Leave out a `[value]` to remove it, like `/bd glow` to stop something glowing (`none` works too).

| Command | Works on | What it does |
|---|---|---|
| `/bd name [name]` | NPC, item, block | Sets the name above it |
| `/bd size <scale>` | All | Scales it. NPCs go from `0.0625` to `16`. |
| `/bd glow [color]` | NPC, item, block | Makes it glow. NPCs take color names like `red` or `aqua`, displays hex colors too. |
| `/bd cmd [<console\|player> <command>]` | All | Runs a command when it's right-clicked |
| `/bd rotate <yaw\|pitch> <degrees>` | Item, block | Rotates it |
| `/bd setblock` | Block | Shows the block you're holding instead |
| `/bd setitem` | Item | Shows the item you're holding instead |
| `/bd skin <player>` | NPC | Gives it a player's skin |
| `/bd desc [text]` | NPC | Sets the line under the name |
| `/bd sound [sound]` | NPC | Plays a sound when it's right-clicked, like `entity.experience_orb.pickup` |
| `/bd equipment` | NPC | Opens a menu for its armor and held items |
| `/bd addline <text>` | Text | Adds a line |
| `/bd editline <line> <text>` | Text | Replaces a line |
| `/bd delline <line>` | Text | Removes a line |
| `/bd background [hex]` | Text | Sets the background, like `#FF0368` or `#80FF0368` with opacity. Leave it out to go back to the default. |
| `/bd shadow <true\|false>` | Text | Gives the text a drop shadow (off by default) |
| `/bd billboard <true\|false>` | Text | Turns it to face players (on by default) |
| `/bd bouncing <true\|false>` | Item | Bobs the item up and down |

### Managing entities

| Command | What it does |
|---|---|
| `/bd help [npc\|text\|item\|block]` | Shows the commands you can use, or every way to edit one type |
| `/bd select [id]` | Selects the entity you're looking at, or the one with that ID |
| `/bd movehere` | Moves your selection to you |
| `/bd setlocation <x> <y> <z>` | Moves your selection to exact coordinates, like `100.5 64 -20.5` for the center of a block. `/bd info` shows where it is now. |
| `/bd remove` | Removes your selection |
| `/bd info` | Shows everything about your selection |
| `/bd list [npc\|item\|block\|text]` | Lists every entity, or one type, with a **[SELECT]** button on each |
| `/bd save` | Saves all entities now |
| `/bd reload` | Reloads `config.yml` and `messages.yml` |

### Permissions

| Permission | Grants |
|---|---|
| `bitedisplays.*` | Every BiteDisplays permission |
| `bitedisplays.use` | `/bd` and the help menu |
| `bitedisplays.npc` | Creating and editing NPCs |
| `bitedisplays.text` | Creating and editing text displays |
| `bitedisplays.item` | Creating and editing item displays |
| `bitedisplays.block` | Creating and editing block displays |
| `bitedisplays.manage` | `select`, `movehere`, `setlocation`, `remove`, `info`, and `list` |
| `bitedisplays.admin` | `save` and `reload` |

All permissions default to **op**. Players without `bitedisplays.use` won't see the command at all.

## Formatting

Names, descriptions, and text lines all use [MiniMessage](https://docs.advntr.dev/minimessage/format.html):

| Style | Example |
|---|---|
| Named colors | `<red>Ruby`, `<gold>Gilded` |
| Hex colors | `<#FF0368>Magenta` |
| Gradients and rainbows | `<gradient:#FF0368:#4ECDC4>Bitesize</gradient>`, `<rainbow>Prism</rainbow>` |
| Decorations | `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>` |
| Classic codes | `&aGreen`, `&l&cBold red`, `&#FF0368Magenta` |

On Paper and Purpur, text keeps every MiniMessage feature, like custom fonts. Spigot only accepts classic text, so colors, gradients, and decorations work there, but tags like `<font>` and `<lang>` don't.

**Glow colors:** the color names are vanilla's 16 chat colors (`red`, `aqua`, `gold`, `light_purple`, ...), and look the same on NPCs and displays. Item and block displays also take hex colors like `#FF0368`. NPCs can't: they glow in their scoreboard team color, which the game limits to those 16. `none` turns the glow off. Text displays can't glow at all, so try `/bd shadow true` on them.

**Click commands** run as the console or as the player who clicked, with that player's own permissions. The `/` is optional, and `@p` becomes the name of the player who clicked. To stop players spamming them, set `clicks.cooldown` in config.yml.

## Configuration

<details>
<summary><b>config.yml</b>: features, selection, click cooldown, text defaults, and animations</summary>

```yaml
# Turn the commands for each entity type on or off. Entities already placed stay in the world.
features:
  npc: true
  item: true     # item displays
  block: true    # block displays
  text: true     # text displays

selection:
  # How far away /bd select reaches, in blocks
  range: 5.0

clicks:
  # Ticks a player waits between clicks on NPCs and displays (20 ticks = 1 second). 0 turns the cooldown off.
  cooldown: 0

text:
  # Background of text displays without their own, as #RRGGBB or #AARRGGBB (AA is opacity).
  # #40000000 is vanilla's. /bd background none puts a display back on this one.
  default-background: "#40000000"
  # Whether new text displays turn to face players. Change one later with /bd billboard
  default-billboard: true

animations:
  # Item displays with /bd bouncing true
  bounce:
    interval: 4    # ticks between steps, lower is smoother
    height: 0.12   # blocks above and below the resting point
    speed: 0.15    # higher bounces faster
```

Run `/bd reload` after editing.

</details>

<details>
<summary><b>messages.yml</b>: every message the plugin sends</summary>

Messages are grouped into `errors`, `messages`, `help`, `info`, and `gui`, and all of them support MiniMessage and `&` codes. The `prefix` and `error-prefix` at the top are added for you, the `types` section names each entity type, and `runners` names who runs a click command.

| Variable | Meaning |
|---|---|
| `<label>` | The command label the player used, like `bd` |
| `<type>` / `<id>` | An entity's type and ID, like `NPC` and `npc-1` |
| `<name>` / `<description>` | A name or description, with its formatting |
| `<command>` / `<runner>` / `<sound>` | A click command, who runs it, or a click sound |
| `<color>` | A glow or background color |
| `<player>` | The player whose skin is used |
| `<axis>` / `<degrees>` / `<size>` | A rotation or scale |
| `<line>` / `<lines>` | A text display line number / its number of lines |
| `<count>` | A number of entities |
| `<range>` | The selection range |
| `<world>`, `<x>`, `<y>`, `<z>` | An entity's location |
| `<select>` | The **[SELECT]** button in `/bd list` |

New messages from updates are filled in automatically, so you never need to regenerate the file.

</details>

<details>
<summary><b>entities.json</b>: placed entities</summary>

Every entity is saved here when the server stops and whenever you run `/bd save`. The file is only read on startup, so edit it only while the server is stopped.

</details>

## Compatibility

| | |
|---|---|
| **Minecraft** | 26.3 |
| **Servers** | Spigot, Paper, and Purpur |
| **Java** | 25 |

## Building from source

```bash
git clone https://github.com/bite-size/BiteDisplays.git
cd BiteDisplays
./gradlew build
```

The plugin jar is written to `build/libs/BiteDisplays-<version>.jar`.

To try it on a local test server with the freshly built plugin loaded:

| Platform | Command | Folder |
|---|---|---|
| Paper | `./gradlew runServer` | `run/` |
| Purpur | `./gradlew runPurpur` | `run-purpur/` |
| Spigot | `./gradlew runSpigot` | `run-spigot/` (the first run builds Spigot with BuildTools, which takes a few minutes) |

Server jars are cached in `.servers/`. Add `-PrefreshServers` to fetch the latest builds.
