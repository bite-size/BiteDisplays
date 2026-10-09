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
/bd npc cmd warp shop {player}
/bd text create <#FF0368>Welcome to the server
/bd item bouncing true
```

## Highlights

- **Four entity types.** NPCs with skins and equipment, multi-line text displays, and displays of any item or block.
- **Click actions.** NPCs and item and block displays run a console command when right-clicked, with `{player}` filled in. NPCs can play a sound too.
- **Modern formatting.** MiniMessage gradients, hex colors, and decorations in names, descriptions, and text lines.
- **Point and edit.** New entities are selected for you. Look at any other one and `/bd select` it, then move, inspect, or remove it.
- **Smart tab completion.** Suggests actions, glow colors, every game sound, online players for skins, and the lines on the text display you've selected.
- **Lightweight.** Entities are vanilla mannequins and display entities, saved to one file and respawned as their chunks load. No extra libraries to install.

## Getting started

1. Download the latest jar from [Releases](https://github.com/bite-size/BiteDisplays/releases).
2. Drop it into your server's `plugins/` folder.
3. Restart the server.

> [!NOTE]
> On its first start, the server downloads the Kotlin standard library from Maven Central, so it needs internet access once.

## Commands

The main command is `/bitedisplays`, with the shorter alias `/bd`. New entities appear where you stand and are selected right away, so you can keep editing them. Most other commands work on your selection.

Leave out a `[value]` to remove it, like `/bd npc glow` to stop an NPC glowing.

### NPCs

| Command | What it does |
|---|---|
| `/bd npc create <name>` | Creates an NPC |
| `/bd npc skin <player>` | Gives it a player's skin |
| `/bd npc name [name]` | Sets the name above its head |
| `/bd npc desc [text]` | Sets the line under the name |
| `/bd npc glow [color]` | Makes it glow in a color like `red` or `aqua` |
| `/bd npc cmd [command]` | Runs a console command when it's right-clicked |
| `/bd npc sound [sound]` | Plays a sound when it's right-clicked, like `entity.experience_orb.pickup` |
| `/bd npc equipment` | Opens a menu for its armor and held items |

### Text displays

| Command | What it does |
|---|---|
| `/bd text create [text]` | Creates a text display |
| `/bd text addline <text>` | Adds a line |
| `/bd text editline <line> <text>` | Replaces a line |
| `/bd text delline <line>` | Removes a line |
| `/bd text background <hex>` | Sets the background, like `#FF0368` or `#64000000` with opacity |
| `/bd text glow [color]` | Makes it glow in a color name or hex color |
| `/bd text billboard <true\|false>` | Turns it to face players |
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
| `/bd <item\|block> cmd [command]` | Runs a console command when it's right-clicked |
| `/bd <item\|block> glowing <true\|false>` | Makes it glow |
| `/bd item bouncing <true\|false>` | Bobs the item up and down |

### Managing entities

| Command | What it does |
|---|---|
| `/bd help` | Shows the commands you can use. `/bd npc`, `/bd text`, `/bd item`, and `/bd block` list their own. |
| `/bd select` | Selects the entity you're looking at |
| `/bd movehere` | Moves your selection to you |
| `/bd remove` | Removes your selection |
| `/bd info` | Shows everything about your selection |
| `/bd list [npc\|item\|block\|text]` | Lists every entity, or one type |
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

On Paper and Purpur, text keeps every MiniMessage feature, like custom fonts. Spigot only accepts classic text, so colors, gradients, and decorations work there, but tags like `<font>` and `<lang>` don't.

**Glow colors:** NPCs glow in the 16 color names (`red`, `aqua`, `gold`, ...). Text displays also take hex colors like `#FF0368`.

**Click commands** run from the console, so leave out the `/`. `{player}` becomes the name of the player who clicked.

## Configuration

<details>
<summary><b>config.yml</b>: features, selection, text defaults, and animations</summary>

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

text:
  # The first line of a new text display when you don't give one
  default-line: "Text Display"
  # Background of text displays without their own, as #RRGGBB or #AARRGGBB (AA is opacity)
  default-background: "#64000000"

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

Messages are grouped into `errors`, `messages`, `help`, `info`, and `gui`, and all of them support MiniMessage. The `prefix` and `error-prefix` at the top are added for you, and the `types` section names each entity type.

| Variable | Meaning |
|---|---|
| `<label>` | The command label the player used, like `bd` |
| `<type>` / `<id>` | An entity's type and short ID, like `NPC` and `1a2b3c4d` |
| `<name>` / `<description>` | A name or description, with its formatting |
| `<command>` / `<sound>` | A click command or sound |
| `<color>` | A glow or background color |
| `<player>` | The player whose skin is used |
| `<axis>` / `<degrees>` / `<size>` | A rotation or scale |
| `<line>` / `<lines>` | A text display line number / its number of lines |
| `<count>` | A number of entities |
| `<range>` | The selection range |
| `<world>`, `<x>`, `<y>`, `<z>` | An entity's location |

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
