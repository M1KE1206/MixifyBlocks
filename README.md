# MixifyBlocks

MixifyBlocks is a client-side Fabric mod for Minecraft 1.21.7. Toggle it on, and after every block you place, your hotbar jumps to a random block-holding slot within a range you choose. It turns ordinary building into a constant scramble to keep track of what you're holding.

## How it works

- Press **N** (default keybind, rebindable under Options -> Controls) to turn mixing on or off.
- While mixing is on, every time you successfully place a block with your main hand, your selected hotbar slot switches to a random slot that also holds a placeable block.
- Only slots within your configured range are considered, and only slots that actually hold a block item - empty slots, tools, food, and weapons are never selected.
- Mixing only does anything while your currently selected slot is inside the configured range. If you switch to a slot outside the range, placing blocks there is left alone.
- Only main-hand placements trigger a switch; placing a block from the offhand does not.
- The same slot can be picked twice in a row - there's no memory of what was picked last.
- Interacting with blocks that aren't placements (opening a chest, a door, etc.) never triggers a switch, even while holding a block.

The mod works in both survival and creative, in single-player and on any server, since it only touches your local hotbar selection. It is entirely client-side: nobody else needs it installed, and you don't need it on a server to play there.

## Settings

Configure MixifyBlocks through ModMenu (click the gear icon next to the mod in your mods list) or by editing `config/mixifyblocks.json` directly:

| Setting | Default | Description |
| --- | --- | --- |
| `minSlot` | `1` | Lowest hotbar slot (1-9) included in the mixing range. |
| `maxSlot` | `9` | Highest hotbar slot (1-9) included in the mixing range. |
| `enabledOnJoin` | `false` | Whether mixing starts turned on when you join a world. |
| `showActionbar` | `true` | Whether toggling mixing shows an action bar message. |

If `minSlot` ends up greater than `maxSlot`, the mod swaps them automatically so the range is always valid.

## Requirements

- Minecraft 1.21.7
- Fabric Loader 0.19.3 or newer
- Fabric API
- Cloth Config 19.0.147
- Mod Menu 15.0.2
- Java 21

## Building from source

```
./gradlew build
```

The built jar is written to `build/libs/`.

## Links

- Source: https://github.com/M1KE1206/MixifyBlocks
- Issues: https://github.com/M1KE1206/MixifyBlocks/issues
- Author: M1KE1206

## License

MIT - see [LICENSE](LICENSE).
