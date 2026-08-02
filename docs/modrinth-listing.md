# Modrinth-listing — invulblad

Alles wat je op de Modrinth-projectpagina moet invullen. Kopieer per veld.

---

## Naam

```
MixifyBlocks
```

## Slug / URL

```
mixifyblocks
```

## Summary (korte omschrijving, max 256 tekens)

```
Shuffles your hotbar to a random block after every block you place, within a slot range you choose. Build naturally mixed walls and floors without scrolling.
```

## License

**Kies: `MIT License`.** Laat het veld *License URL* leeg — Modrinth toont de licentietekst dan zelf.

Waarom MIT: kort, wereldwijd begrepen, iedereen mag je mod gebruiken en aanpassen zolang je
naam in de licentie blijft staan. Dit is de meest gebruikte licentie voor Fabric-mods.

Alternatieven, mocht je iets anders willen:

| Licentie | Wat het betekent |
|---|---|
| **MIT** | Alles mag, mits je naam vermeld blijft. Standaardkeuze. |
| **CC0-1.0** | Publiek domein, zelfs naamsvermelding niet vereist. Wat de Fabric-template standaard gebruikt. |
| **LGPL-3.0** | Wie jouw code aanpast en verspreidt, moet die aanpassingen ook open publiceren. Kies dit als je niet wilt dat iemand er een closed-source clone van maakt. |
| **ARR** (All Rights Reserved) | Niemand mag iets. Alleen zinvol als je echt geen hergebruik wilt. |

Wat je hier kiest moet overeenkomen met het `LICENSE`-bestand in de repo en met het
`"license"`-veld in `fabric.mod.json`.

## Environments

- Client side: **Required**
- Server side: **Unsupported**

De mod draait volledig op de client. Dat is geen beperking maar een voordeel: je kunt hem op
elke server gebruiken, ook op servers waar de mod niet geïnstalleerd is.

## Categories

- Utility
- Game Mechanics

## Loaders / Game versions

- Loader: Fabric
- Game version: 1.21.7

## Links

- Source code: `https://github.com/M1KE1206/MixifyBlocks`
- Issue tracker: `https://github.com/M1KE1206/MixifyBlocks/issues`

---

## Description (de body van de pagina)

Vanaf hier alles kopiëren:

```markdown
## MixifyBlocks

Building a wall out of mixed blocks normally means scrolling your hotbar between every single
placement. MixifyBlocks does it for you.

Press a key to turn mixing on, then just build. After every block you place, your hotbar jumps
to a random block within a slot range you pick yourself. Stone, cobble, andesite and deepslate
in slots 1 to 4, and your wall comes out naturally mixed without a single scroll.

## How it works

1. Put the blocks you want to mix in a group of hotbar slots, for example 1 to 4.
2. Set that range in the config screen.
3. Press **N** to turn mixing on.
4. Build. Your hotbar shuffles itself after every block you place.

Press **N** again to turn it off.

## Details

- Only slots holding a **placeable block** are picked. Swords, tools and food are skipped, and
  so are empty slots.
- Mixing only kicks in while your selected slot is **inside** your configured range. Scroll to
  slot 8 with a range of 1 to 4 and the mod stays out of your way.
- Works with right-click held down, so placing a whole row mixes as you go.
- Opening a chest or a door while holding a block does **not** trigger a switch.
- Works in survival and creative. When a stack runs out in survival, that slot simply drops
  out of the rotation.

## Configuration

Configure it through Mod Menu, or edit `config/mixifyblocks.json`.

| Setting | Default | What it does |
|---|---|---|
| Lowest slot | 1 | First hotbar slot that takes part |
| Highest slot | 9 | Last hotbar slot that takes part |
| Enabled on join | off | Turn mixing on automatically when you join a world |
| Show action bar message | on | Short message above your hotbar when you toggle |

The keybind is rebindable under Options, Controls.

## Client side only

MixifyBlocks runs entirely on your client, so you can use it on **any** server without the
server needing the mod. Nothing is sent to the server beyond the normal hotbar slot change
that vanilla already sends when you scroll.

## Requirements

- Fabric Loader 0.19.3 or newer
- Fabric API
- Cloth Config
- Mod Menu
```
