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
/bd npc create <gradient:#FF0368:#4ECDC4>Bite Keeper</gradient>
/bd npc skin Notch
/bd npc cmd console warp shop @p
/bd text create <#FF0368>Welcome to the server
/bd item bouncing true
```

## Highlights

- **Four entity types.** NPCs with skins, equipment, and any size, multi-line text displays, and displays of any item or block. NPCs can't be hurt, even by players in creative mode.
- **Click actions.** NPCs and item and block displays run a command when right-clicked, as the console or as the player who clicked, with `@p` filled in. NPCs can play a sound too.
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

The main command is `/bitedisplays`, with the shorter alias `/bd`. New entities appear where you stand and are selected right away, so you can keep editing them. Most other commands work on your selection. Every entity gets an ID like `npc-1` or `text-3`.

Leave out a `[value]` to remove it, like `/bd npc glow` to stop an NPC glowing (`none` works too).

### NPCs

| Command | What it does |
|---|---|
| `/bd npc create <name>` | Creates an NPC |
| `/bd npc skin <player>` | Gives it a player's skin |
| `/bd npc name [name]` | Sets the name above its head |
| `/bd npc desc [text]` | Sets the line under the name |
| `/bd npc size <scale>` | Scales it, from `0.0625` to `16` |
| `/bd npc glow [color]` | Makes it glow in a color like `red` or `aqua` |
| `/bd npc cmd [<console\|player> <command>]` | Runs a command when it's right-clicked |
| `/bd npc sound [sound]` | Plays a sound when it's right-clicked, like `entity.experience_orb.pickup` |
| `/bd npc equipment` | Opens a menu for its armor and held items |

### Text displays

| Command | What it does |
|---|---|
| `/bd text create [text]` | Creates a text display |
| `/bd text addline <text>` | Adds a line |
| `/bd text editline <line> <text>` | Replaces a line |
| `/bd text delline <line>` | Removes a line |
| `/bd text background [hex]` | Sets the background, like `#FF0368` or `#80FF0368` with opacity. Leave it out to go back to the default. |
| `/bd text cmd [<console\|player> <command>]` | Runs a command when it's right-clicked. Leave it out to remove the command. |
| `/bd text shadow <true\|false>` | Gives the text a drop shadow (off by default) |
| `/bd text billboard <true\|false>` | Turns it to face players (on by default) |
| `/bd text size <scale>` | Scales it |

### Item and block displays

Hold the item or block you want to show, then use `item` or `block`:

| Command | What it does |
|---|---|
| `/bd item create` | Creates a display of the item you're holding |
| `/bd block create` | Creates a display of the block you're holding |
| `/bd <item\|block> rotate <yaw\|pitch> <degrees>` | Rotates it |
| `/bd <item\|block> size <scale>` | Scales it |
| `/bd <item\|block> name [name]` | Shows a name above it |
| `/bd <item\|block> cmd [<console\|player> <command>]` | Runs a command when it's right-clicked |
| `/bd <item\|block> glow [color]` | Makes it glow in a color name or hex color |
| `/bd item bouncing <true\|false>` | Bobs the item up and down |

### Managing entities

| Command | What it does |
|---|---|
| `/bd help` | Shows the commands you can use. `/bd npc`, `/bd text`, `/bd item`, and `/bd block` list their own. |
| `/bd select [id]` | Selects the entity you're looking at, or the one with that ID |
| `/bd movehere` | Moves your selection to you |
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
| `bitedisplays.npc` | NPC commands |
| `bitedisplays.text` | Text display commands |
| `bitedisplays.item` | Item display commands |
| `bitedisplays.block` | Block display commands |
| `bitedisplays.manage` | `select`, `movehere`, `remove`, `info`, and `list` |
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

**Glow colors:** NPCs glow in the 16 color names (`red`, `aqua`, `gold`, ...). Text, item, and block displays also take hex colors like `#FF0368`. `none` turns the glow off.

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
  # The first line of a new text display when you don't give one
  default-line: "Text Display"
  # Background of text displays without their own, as #RRGGBB or #AARRGGBB (AA is opacity).
  # #40000000 is vanilla's. /bd text background none puts a display back on this one.
  default-background: "#40000000"
  # Whether new text displays turn to face players. Change one later with /bd text billboard
  default-billboard: true

animations:
  # Item displays with /bd item bouncing true
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
