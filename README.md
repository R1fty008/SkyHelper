# SkyHelper

An all-in-one Hypixel Skyblock helper mod for Minecraft 1.21.11 (Fabric).

## Features

- **Calculator / Smart Storage Search Bar** — Press `]` to open. Type math expressions to calculate, or type text to search storage items. Search persists across page navigation.
- **Copy Chat Messages** — Ctrl+Click any chat message to copy its text to clipboard.
- **Hyperion Chat Filter** — Suppresses the "There are blocks in the way!" spam from Wither Impact.
- **Skill XP Overlay** — Shows current skill XP progress with a progress bar on the HUD.
- **Potion Effects Overlay** — Displays active potion effects with remaining durations.
- **Config GUI** — Press `Right Shift` to open the in-game config. Toggle any feature on/off.

## Building

```bash
./gradlew build
```

The output JAR will be in `build/libs/SkyHelper-1.0.0.jar`.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.11
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Copy `SkyHelper-1.0.0.jar` into your `.minecraft/mods/` folder
4. Launch Minecraft

## Keybinds

| Key | Action |
|-----|--------|
| Right Shift | Open SkyHelper config GUI |
| ] | Toggle calculator / search bar |

## Config

Settings are saved to `config/skyhelper.json` and persist between sessions.
