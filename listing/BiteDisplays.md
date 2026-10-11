# BiteDisplays

*NPCs, holograms, and item and block displays, placed and styled in-game.*

## ▸ Overview

Your spawn deserves more than signs. BiteDisplays lets you place NPCs with real player skins, floating text, and displays of any item or block right where you stand. Style them with gradients and hex colors, have them run commands when players click them, and manage all of it in-game with one simple command.

## ▸ Key Features

- **Four Entity Types:** NPCs with skins, equipment, and any size, multi-line text displays, and item and block displays.
- **Click Actions:** NPCs, text, and item and block displays run a command when right-clicked, as the console or the player, with `@p` filled in. NPCs can play a sound too.
- **Modern Formatting:** MiniMessage gradients, HEX colors, and decorations in every name and line. Classic `&` codes work too.
- **Point & Edit:** New entities are selected for you. Look at any other one, or click **[SELECT]** in the list, then move, inspect, or remove it.
- **Spigot, Paper & Purpur:** One jar that runs on all three, tested on each.

## ▸ Formatting

Names, descriptions, and text lines all understand MiniMessage.

| Example | Result |
|---|---|
| `<red>Ruby`, `<gold>Gilded` | Named colors |
| `<#FF0368>Magenta` | HEX colors |
| `<gradient:#FF0368:#4ECDC4>Bitesize</gradient>` | Smooth gradients (rainbows too) |
| `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>` | Decorations |
| `&a`, `&l`, `&#FF0368` | Classic color codes |

On Paper and Purpur, text keeps every MiniMessage feature, like custom fonts.

## ▸ Additional Features

- Every single message is completely configurable in the `messages.yml`.
- Smart TAB completion suggests actions, glow colors, every game sound, online players for skins, entity IDs, and the lines on your selected text display.
- An equipment menu to dress your NPCs in any armor and held items.
- NPCs that can't be hurt, even by players in creative mode.
- Bouncing item displays, glowing in any color, and text that turns to face players.
- Turn NPCs, item, block, or text displays on or off individually.
- Lightweight: entities are vanilla mannequins and displays, saved to one file and respawned as their chunks load.

## ▸ Creating

| Command | Description |
|---|---|
| `/bd create npc [name]` | Create an NPC where you stand |
| `/bd create text <text>` | Create a text display with its first line |
| `/bd create item [name]` / `/bd create block [name]` | Display the item or block you're holding |

## ▸ Editing

Editing commands work on your selection, whatever it is. If one doesn't fit, it tells you which types it works on.

| Command | Works on | Description |
|---|---|---|
| `/bd name [name]` | NPC, item, block | Set the name |
| `/bd size <scale>` | All | Scale it |
| `/bd glow [color]` | NPC, item, block | Glow in a color name (or HEX for displays) |
| `/bd cmd [<console\|player> <command>]` | All | Run a command on right-click |
| `/bd rotate <yaw\|pitch> <degrees>` | Item, block | Rotate it |
| `/bd skin <player>` | NPC | Use a player's skin |
| `/bd desc [text]` | NPC | Set the line under the name |
| `/bd sound [sound]` | NPC | Play a sound on right-click |
| `/bd equipment` | NPC | Open the equipment menu |
| `/bd addline <text>` / `editline <line> <text>` / `delline <line>` | Text | Add, replace, or remove a line |
| `/bd background [hex]` | Text | Set the background color |
| `/bd shadow <true\|false>` | Text | Drop shadow (off by default) |
| `/bd billboard <true\|false>` | Text | Turn to face players (on by default) |
| `/bd bouncing <true\|false>` | Item | Bob the item up and down |

Leave out a `[value]` to remove it. In commands, `@p` is the player who clicked. `/bd help <type>` lists every command for one type.

## ▸ Managing Entities & Permissions

| Command / Permission | Description |
|---|---|
| `/bd select [id]` | Select the entity you're looking at, or by ID like `npc-1` |
| `/bd movehere` | Move your selection to you |
| `/bd remove` | Remove your selection |
| `/bd info` | Show your selection's details |
| `/bd list [type]` | List every entity with a **[SELECT]** button, or one type |
| `/bd save` / `/bd reload` | Save entities / reload config & messages (`bitedisplays.admin`) |
| `bitedisplays.npc`, `.text`, `.item`, `.block` | Each entity type's commands |
| `bitedisplays.manage` | Select, move, remove, inspect, and list |
| `bitedisplays.*` | Every BiteDisplays permission |

All permissions default to op. *Every command also works as `/bitedisplays`.*

## ▸ Configuration

- **Features:** Toggle NPCs, item, block, and text displays individually.
- **Selection:** How far away `/bd select` reaches.
- **Clicks:** An optional cooldown between clicks, off by default.
- **Text:** The default background and billboard of new text displays.
- **Animations:** The height, speed, and smoothness of bouncing items.

<details>
<summary><b>config.yml</b></summary>

```yaml
features:
  npc: true
  item: true     # item displays
  block: true    # block displays
  text: true     # text displays

selection:
  range: 5.0

clicks:
  cooldown: 0   # ticks

text:
  default-background: "#40000000"
  default-billboard: true

animations:
  bounce:
    interval: 4
    height: 0.12
    speed: 0.15
```

</details>

## ▸ Messages

Every message lives in `messages.yml`, grouped into `errors`, `messages`, `help`, `info`, and `gui`, with full MiniMessage and `&` code support. Set your own prefix and error prefix, rename the entity types, and use variables like `<type>`, `<id>`, `<name>`, `<command>`, and `<line>`.

New messages added in updates are filled in automatically, so you never have to regenerate the file.

## ▸ Compatibility

- **Minecraft:** 26.3
- **Servers:** Spigot, Paper, and Purpur
- **Java:** 25

*On first start, the server downloads the Kotlin standard library once, so it needs internet access.*

## ▸ Video Tutorial

Coming soon...

## ▸ Support

Lost, confused, or stuck at any point? Reach out to me here, on GitHub, or on Discord (username: **.bite.size**).
