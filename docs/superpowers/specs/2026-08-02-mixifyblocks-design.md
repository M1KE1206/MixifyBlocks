# MixifyBlocks — Design

Datum: 2026-08-02
Status: goedgekeurd door Mike

## Doel

Een Fabric-mod voor Minecraft 1.21.7 die het bouwen van gemengde texturen makkelijk maakt.
Zet de modus aan met een toets, en na elke geplaatste block springt je hotbar automatisch
naar een willekeurig ander block binnen een zelf ingesteld slotbereik. Je bouwt zo een
gemengde muur zonder handmatig te scrollen.

## Scope

Client-side mod. Een keybind en een ModMenu-configscherm bestaan alleen op de client,
dus de mod draait volledig client-side en werkt op elke server — ook servers zonder de mod.
Er wordt geen eigen netwerkverkeer verstuurd; het wisselen van hotbar-slot wordt door
vanilla Minecraft zelf naar de server gesynchroniseerd.

De oorspronkelijke instructie noemde "server side mod". Dat is met Mike besproken en
verworpen: keybind + ModMenu vereisen de client. Client-side is bevestigd als gewenst.

## Gedrag

### Schakelen

- De keybind (standaard `N`, herbindbaar via Opties → Besturing) schakelt de modus AAN/UIT.
- Bij het aanzetten gebeurt er verder niets: er wordt niet meteen van slot gewisseld.
- De modus is een client-sessie-status; hij wordt niet opgeslagen. Bij het betreden van een
  wereld of server wordt hij gezet op de waarde van `enabledOnJoin`; bij het verlaten ervan
  vervalt hij.
- Bij het aanzetten en uitzetten verschijnt een actionbar-melding (zie Feedback).

### De switch

Er wordt van slot gewisseld wanneer aan **alle** volgende voorwaarden is voldaan:

1. De modus staat AAN.
2. De speler heeft zojuist met succes een block geplaatst **met de hoofdhand**.
   Plaatsingen met de offhand tellen niet mee, omdat die losstaan van het hotbar-slot.
3. Het op dat moment geselecteerde hotbar-slot ligt binnen het ingestelde bereik
   `[minSlot, maxSlot]`.

Het nieuwe slot wordt willekeurig gekozen uit alle slots binnen `[minSlot, maxSlot]`
die een plaatsbaar block bevatten.

### Randgevallen

| Situatie | Gedrag |
|---|---|
| Huidig slot ligt buiten het bereik | Geen switch. De mod doet niets. |
| Geen enkel slot in het bereik bevat een block | Geen switch, geen foutmelding. |
| Precies één slot in het bereik bevat een block | Blijft op dat slot staan. |
| Het gekozen slot is hetzelfde als het huidige | Toegestaan. De keuze is puur willekeurig, herhaling mag. |
| Slot bevat een item dat geen block is (zwaard, eten, tool) | Telt niet mee als kandidaat. |
| Speler plaatst een block terwijl de modus UIT staat | Geen switch. |

"Plaatsbaar block" = de `ItemStack` in het slot is een `BlockItem`.

### Timing

De switch wordt met één tick uitgesteld: de plaatsingshook zet alleen een vlag, en de
daadwerkelijke slotwissel gebeurt in de volgende client-tick. Zo wordt de inventory nooit
gemuteerd terwijl Minecraft midden in de afhandeling van de interactie zit.

Vanilla roept elke tick `ensureHasSentCarriedItem()` aan, wat de slotwijziging naar de
server stuurt. Er is dus geen eigen packet nodig.

### Feedback

Actionbar-melding bij het schakelen, alleen als `showActionbar` aan staat:

- Aan: `MixifyBlocks: AAN (slots 1-4)`
- Uit: `MixifyBlocks: UIT`

Geen chatberichten, geen geluid.

## Configuratie

Beheerd via Cloth Config, bereikbaar via ModMenu. Opgeslagen als JSON in
`.minecraft/config/mixifyblocks.json`.

| Sleutel | Type | Standaard | Bereik | Betekenis |
|---|---|---|---|---|
| `minSlot` | int slider | 1 | 1-9 | Laagste hotbar-slot dat meedoet |
| `maxSlot` | int slider | 9 | 1-9 | Hoogste hotbar-slot dat meedoet |
| `enabledOnJoin` | boolean | false | — | Modus automatisch AAN bij het starten van een wereld |
| `showActionbar` | boolean | true | — | Actionbar-melding tonen bij schakelen |

Slotnummers zijn in de config 1-gebaseerd (zoals de speler ze op het scherm ziet).
Intern wordt gerekend met 0-gebaseerde indexen; de conversie gebeurt op één plek.

### Normalisatie

Als `minSlot > maxSlot` worden de twee waarden bij het laden en bij het opslaan omgewisseld.
Beide waarden worden geklemd op 1-9. Een handmatig bewerkt of beschadigd configbestand kan
de mod dus niet in een kapotte toestand brengen; bij een onleesbaar bestand vallen alle
waarden terug op hun standaardwaarde.

## Structuur

```
src/client/java/com/mixifyblocks/client/
  MixifyBlocksClient.java     ClientModInitializer: keybind, tick-handler, toggle-status
  MixifyConfig.java           velden, laden/opslaan, normalisatie
  MixifyConfigScreen.java     opbouw van het Cloth Config-scherm
  ModMenuIntegration.java     ModMenuApi: koppelt de knop in ModMenu aan het scherm
  SlotPicker.java             kiest een willekeurig block-slot binnen een bereik
  mixin/MultiPlayerGameModeMixin.java   detecteert een geslaagde block-plaatsing
```

Er is geen `src/main` gameplay-code nodig; de mod is `client`-only in `fabric.mod.json`.

### Verantwoordelijkheden

**`SlotPicker`** — Kernlogica, losgekoppeld van Minecraft-lifecycle. Krijgt de negen
hotbar-`ItemStack`s, het bereik, het huidige slot en een `RandomSource`; geeft het gekozen
slot terug of "geen keuze". Bevat alle randgevallen uit de tabel hierboven en is daardoor
apart te testen zonder draaiend spel.

**`MultiPlayerGameModeMixin`** — `@Inject` op `RETURN` van
`MultiPlayerGameMode.useItemOn(...)`. Controleert of het `InteractionResult` een geslaagde
actie is, of de hand de hoofdhand was, en of de gebruikte stack een `BlockItem` was; zo ja,
zet de vlag op `MixifyBlocksClient`. Doet verder geen logica.

De exacte signatuur van `useItemOn` en de manier waarop een geslaagd `InteractionResult`
wordt herkend moeten bij aanvang van de implementatie geverifieerd worden tegen de
gedecompileerde 1.21.7-bronnen. `InteractionResult` is in 1.21.2 omgebouwd van een enum naar
een sealed interface, dus oudere voorbeelden op internet kloppen niet meer. Dit is de enige
plek in de mod die van interne Minecraft-API afhangt.

**`MixifyBlocksClient`** — Registreert de keybind, luistert op `END_CLIENT_TICK`, handelt de
toggle af en voert de uitgestelde switch uit door `SlotPicker` te raadplegen.

**`MixifyConfig`** — Enige plek die het configbestand kent. Geeft genormaliseerde waarden af.

### Waarom een mixin en niet `UseBlockCallback`

`UseBlockCallback` vuurt vóór de plaatsing en ook bij het openen van kisten, deuren en ovens.
Het weet niet of er daadwerkelijk een block geplaatst is, wat valse switches zou opleveren.
De mixin op `useItemOn` weet dat wel, en werkt zowel bij losse klikken als bij het ingedrukt
houden van de rechtermuisknop.

Een derde optie — elke tick kijken of de stack in de hand kleiner is geworden — valt af omdat
die in creative niet werkt (blocks worden daar niet verbruikt).

## Afhankelijkheden

| Mod | Versie | Rol |
|---|---|---|
| Minecraft | 1.21.7 | — |
| Fabric Loader | 0.19.3 | — |
| Fabric API | 0.129.0+1.21.7 | keybind-registratie, client-tick-events |
| Cloth Config | `me.shedaniel.cloth:cloth-config-fabric:19.0.147` | configscherm |
| ModMenu | `com.terraformersmc:modmenu:15.0.2` | ingang naar het configscherm |

Cloth Config en ModMenu zijn verplichte dependencies in `fabric.mod.json`, zodat Fabric een
nette melding geeft als ze ontbreken in plaats van te crashen.

Build: Java 21, Gradle met `fabric-loom-remap`, `officialMojangMappings()` — gelijk aan de
bestaande ToggleEnch-mod.

## Testen

**Unit-tests op `SlotPicker`**, zonder draaiend spel:

- Kiest alleen slots binnen het bereik
- Slaat lege slots over
- Slaat niet-block items over
- Geeft "geen keuze" bij nul kandidaten
- Geeft hetzelfde slot terug bij precies één kandidaat
- Kan hetzelfde slot teruggeven als het huidige (herhaling is toegestaan)
- Geeft "geen keuze" wanneer het huidige slot buiten het bereik ligt

**Unit-tests op `MixifyConfig`**: omgewisselde min/max wordt genormaliseerd, waarden buiten
1-9 worden geklemd, een onleesbaar bestand valt terug op de standaardwaarden.

**Handmatige verificatie in het spel** (survival én creative): keybind schakelt, actionbar
verschijnt, bouwen binnen het bereik mengt de blokken, bouwen buiten het bereik verandert
niets, config-wijzigingen werken direct zonder herstart.

## Bewust buiten scope

- Geen slot-machine-animatie bij de switch.
- Geen gewichten of kansen per slot.
- Geen automatische switch bij het breken van blocks.
- Geen serverkant, geen configuratiesynchronisatie.
