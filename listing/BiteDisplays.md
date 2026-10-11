# BiteDisplays

*NPCs, holograms, and item and block displays, placed and styled in-game.*

## ▸ Overview

Your spawn deserves more than signs. BiteDisplays lets you place NPCs with real player skins, floating text, and displays of any item or block right where you stand. Style them with gradients and hex colors, have them run commands when players click them, and manage all of it in-game with one simple command.

## ▸ Key Features

- **Four Entity Types:** NPCs with skins, equipment, and any size, multi-line text displays, and item and block displays.
- **Click Actions:** NPCs and item and block displays run a command when right-clicked, as the console or the player, with `@p` filled in. NPCs can play a sound too.
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

## ▸ NPC Commands

| Command | Description |
|---|---|
| `/bd npc create <name>` | Create an NPC where you stand |
| `/bd npc skin <player>` | Use a player's skin |
| `/bd npc name [name]` | Set the name |
| `/bd npc desc [text]` | Set the line under the name |
| `/bd npc size <scale>` | Scale the NPC |
| `/bd npc glow [color]` | Glow in a color |
| `/bd npc cmd [<console\|player> <command>]` | Run a command on right-click |
| `/bd npc sound [sound]` | Play a sound on right-click |
| `/bd npc equipment` | Open the equipment menu |

## ▸ Text Display Commands

| Command | Description |
|---|---|
| `/bd text create [text]` | Create a text display where you stand |
| `/bd text addline <text>` | Add a line |
| `/bd text editline <line> <text>` | Replace a line |
| `/bd text delline <line>` | Remove a line |
| `/bd text background <hex>` | Set the background color |
| `/bd text glow [color]` | Glow in a color name or HEX color |
| `/bd text billboard <true\|false>` | Turn to face players (on by default) |
| `/bd text size <scale>` | Scale the text |

## ▸ Item & Block Display Commands

| Command | Description |
|---|---|
| `/bd item create` / `/bd block create` | Display the item or block you're holding |
| `/bd <item\|block> rotate <yaw\|pitch> <degrees>` | Rotate it |
| `/bd <item\|block> size <scale>` | Scale it |
| `/bd <item\|block> name [name]` | Show a name above it |
| `/bd <item\|block> cmd [<console\|player> <command>]` | Run a command on right-click |
| `/bd <item\|block> glow [color]` | Glow in a color name or HEX color |
| `/bd item bouncing <true\|false>` | Bob the item up and down |

Leave out a `[value]` to remove it. In commands, `@p` is the player who clicked.

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
- **Text:** The default first line, background, and billboard of new text displays.
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

text:
  default-line: "Text Display"
  default-background: "#64000000"
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
