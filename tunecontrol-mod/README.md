# Tune Control: a Minecraft music mod

Tune Control lets you decide **which music plays, where it plays, and how often it plays**, using a **settings screen inside the game** (or a settings file you can edit in any text editor).

- **Which music:** any single Minecraft song by its real title (like *Mice on Venus*), any music disc, or your own `.ogg` files
- **Where:** by dimension, biome (or biome group), height, day/night, weather, game mode, underwater, the title screen, or the **block you are standing on** (for map makers)
- **How often:** a chance (0 to 100%) per song, plus how long to wait between songs
- **Now Playing pop-up:** a small toast in the top-right corner shows the song's title and composer whenever a song starts (you can switch it off)

It targets **Minecraft 1.20.1 with Forge** and runs only on your own computer (it also works when you join servers that don't have it).

> **Honest note:** this code was written without being able to compile it against Minecraft's real code. Earlier builds found a few small mistakes (all fixed). The in-game settings screen, the song list and the Now Playing pop-up are the newest parts and have not been tried in a real game yet, so if the build reports an error or a screen looks wrong, copy the message (or describe it) to Claude and it will be fixed quickly.

---

## Part 1: One-time setup

1. **Install Java 17** (a free "JDK"). Search for "Temurin JDK 17" and install it.
2. **Install IntelliJ IDEA Community Edition** (free). It's the easiest editor for mods.
3. **Download the Forge MDK for 1.20.1.** The MDK is Forge's starter project.
   - Go to https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html
   - Under "Download Recommended" click **Mdk**.
   - Unzip it somewhere, for example `Documents/tunecontrol`.

## Part 2: Put this mod into the starter project

1. In the unzipped MDK, **delete the whole folder** `src/main/java/com/example` (the sample mod). It isn't part of this mod and can cause confusing errors.
2. **Copy** this zip's `src` folder into the MDK, and choose "merge/replace" when asked. That adds `com/tunecontrol/...` and replaces `META-INF/mods.toml`.
3. Open `gradle.properties` in the MDK and change **only** these lines:

```
mod_id=tunecontrol
mod_name=Tune Control
mod_version=1.0.0
mod_group_id=com.tunecontrol
mod_authors=YourNameHere
mod_description=Customize Minecraft's background music: what plays, where, and how often.
```

   Leave the Minecraft/Forge version lines alone.

## Part 3: Try it

1. In IntelliJ choose **File > Open** and pick the MDK's `build.gradle`. Choose "Open as Project", and wait for the Gradle sync to finish (the first time takes several minutes).
2. In a terminal inside the project folder run:
   - Windows: `gradlew.bat runClient`
   - Mac/Linux: `./gradlew runClient`

   Minecraft starts with your mod loaded. Its settings file appears at `run/config/tunecontrol.json`.
3. **Build the real mod file:** `gradlew build`. The `.jar` appears in `build/libs/`.
4. **Install it for normal play:** install Forge 1.20.1 from the same Forge download page (the "Installer"), then drop the `.jar` into `.minecraft/mods`.

---

## Part 4: Changing the music

There are two ways, and you can mix them: the **settings screen** in the game (easiest), or the **settings file** `config/tunecontrol.json` (inside the `run` folder while testing, inside `.minecraft/config` for normal play).

### The settings screen (in the game)

Open it from either place:

- **Title screen:** **Mods > Tune Control > Config**
- **In a world (or anywhere):** type `/tunecontrol config` in chat

What you'll see:

- **Main screen:** the on/off switch, the **Music toast** on/off switch (the Now Playing pop-up), what happens where no rule applies (normal Minecraft music or silence), the default timing boxes, **Edit rules...**, and **Previous / Stop / Skip** buttons.
- **Edit rules...:** your list of rules, a page at a time. Click a rule's name to edit it. **On/Off** switches the rule, **^ / v** move it up or down (the top rule that matches wins), **X** deletes it (click twice to confirm). **Add rule** puts a new rule at the top.
- **Editing a rule** has three tabs:
  - **Basics:** the name, on/off, a **chance slider (0 to 100%)**, how long to wait between songs, and whether to stop the song when you leave.
  - **Where:** time of day, a height range (Y), and four **pick lists**: **Situations**, **Dimensions**, **Biomes** and **Standing on** (a block). Each shows what you picked (for example `Biomes: Plains, Forest +2 more`). Click one to open its list. Pick nothing for "anywhere".
  - **Songs:** type song titles separated by commas (like `Mice on Venus, Sweden*2`; the `*2` makes a song twice as likely), or press **Pick songs from a list...**. Every song is shown by its real title. Click a row to add or remove it, **Play** to hear it, the button at the bottom left to switch groups (All songs, Overworld, Creative, Title screen, Underwater, Nether, The End, Music discs, Random sets, Packs & mods), and the search box to find a song by any part of its name. Text turns **red** if Minecraft does not know that song.
- **Save & Close** writes your changes to `config/tunecontrol.json` and applies them immediately. **Cancel** (or Escape on the main screen) throws the changes away. While you edit, nothing changes until you save. The Previous / Stop / Skip buttons act right away, using your last saved settings.

Because the screen saves into the same file, you can still open `config/tunecontrol.json` in a text editor whenever you like.

#### Example: "Mice on Venus" in the Overworld only

1. Open the settings screen, press **Edit rules...**, then **Add rule**. The new rule goes to the top, so it wins over the rules below it.
2. On the **Songs** tab press **Pick songs from a list...**, click **Mice on Venus** so it shows `[x]` (the **Play** button lets you hear it first), then press **Done**. A new rule starts with "any Overworld song"; your first pick replaces that, so the rule plays only what you choose. (Tip: type "venus" in the search box to find it quickly.)
3. On the **Where** tab press **Dimensions**, click **Overworld** so it shows `[x]`, then press **Done**.
4. On the **Basics** tab give the rule a name, and set the chance and waiting times you want (100% and a short wait are good for testing).
5. Press **Done**, then **Back**, then **Save & Close**. Press **Skip** (or type `/tunecontrol skip`) to hear it right away.

In the Nether and the End the rule does not apply, so normal Minecraft music plays there (or silence, if you chose that on the main screen).

#### The pick lists

Minecraft has no built-in drop-down menu, so each list opens as its own small screen. It works like the suggestions under the chat box:

- **Click a row** to tick or untick it (`[x]` = picked). Pick as many as you like.
- **Type in the search box** and the list shrinks as you type (type `gold` and you get Gold Block, Gold Ore, and so on). **Enter** ticks the top result.
- **Prev / Next** or the **mouse wheel** turn the pages. **Clear all** unticks everything. **Done** goes back. There is nothing to save here: your picks go straight into the rule.
- **Something that is not in the list** (a block or biome from a mod, a custom dimension from a map)? Type its ID, for example `mymap:glow_cave`. When nothing matches, an **Add "mymap:glow_cave"** row appears. Click it.
- At the top of the Biomes and Standing on lists, a **Right now:** row (while you are in a world) gives you your current biome or the block under your feet in one click.
- The lists also contain groups, shown as **Any Forest (tag)** or **Any Planks (tag)**. One tick covers every biome or block in the group.

#### Example, for map makers: music while standing on one block

1. Make a new rule and give it a name on the **Basics** tab. Set **Wait at least** and **Wait at most** to `0` and `2` so the music starts about as soon as you step on the block. Turn **Stop song when you leave** on if you want it to stop when you step off.
2. On the **Songs** tab, pick the song.
3. On the **Where** tab press **Standing on**. Stand on your block in the world first, and the top row says **Right now: ...** with its name. Click it. Or type part of the name (`gold`) and click the result.
4. Press **Done** until you are back at the main screen, then **Save & Close**.

"Standing on" means the block directly under your feet, or a thin block you are standing in, such as a carpet, a pressure plate or a slab. Not sure what a block is called? Stand on it and type `/tunecontrol status`. It says `standing on minecraft:...`.

### The "Now Playing" pop-up

Whenever Tune Control starts a song, a small pop-up slides in at the top-right of the screen for a few seconds, like the music toast in newer versions of Minecraft:

```
Now Playing
Mice on Venus - C418
```

- It names the song that is **really** playing. For a random set such as `minecraft:music.game`, it shows whichever song Minecraft picked.
- The composer is shown for the songs in the list at the bottom of this page. A song from a resource pack or a map shows its name made from the file name (`forest_song` becomes *Forest Song*), with no composer.
- Music discs show as, for example, `cat (music disc) - C418`.
- If two songs start close together (for example you press **Skip** quickly), the same pop-up changes its text instead of piling up a queue.
- It stays hidden when your **Music** or **Master** volume is at 0, and the song picker's **Play** preview button does not show it (the picker already says what is playing).
- Turn it off with the **Music toast** button on the main settings screen, or set `"show_music_toast": false` in `config/tunecontrol.json`.
- It announces **all** background music while Tune Control is switched on: the songs Tune Control starts, and normal Minecraft music where no rule applies (just like the newer game). If you switch Tune Control off (the master switch), it does nothing at all.

### Editing the file by hand

Open `config/tunecontrol.json`.

### Your first rule (about 2 minutes)

1. Start the game and open any Overworld world. Type `/tunecontrol status` in chat. If it prints your biome and a rule count, the mod is working.
2. Open `config/tunecontrol.json` in Notepad. Find the line `"rules": [` and paste this **directly after the `[`**, keeping the comma at the end:

```json
{
  "name": "My test rule",
  "enabled": true,
  "dimensions": ["minecraft:overworld"],
  "tracks": [
    { "sound": "minecraft:music.creative", "weight": 1 },
    { "sound": "minecraft:music.game", "weight": 1 }
  ],
  "chance_percent": 100,
  "min_delay_seconds": 5,
  "max_delay_seconds": 10
},
```

3. Save the file, then type `/tunecontrol reload` in game. Within about 10 seconds a song should start. `/tunecontrol status` shows which rule matched and when the next song roll happens.
4. Now experiment: lower `chance_percent` to make songs rarer, raise the delays to space them out, change the `tracks`, or add `"biomes": ["minecraft:plains"]` to limit the rule to one biome.

It ships with seven **example rules**, all switched off (`"enabled": false`) so that installing the mod changes nothing until you choose. Change one to `true`, save, then type **`/tunecontrol reload`** in game.

A rule looks like this:

```json
{
  "name": "Nether music in deep Overworld caves",
  "enabled": true,
  "dimensions": ["minecraft:overworld"],
  "max_y": 30,
  "tracks": [
    { "sound": "minecraft:music.nether.nether_wastes", "weight": 1 },
    { "sound": "minecraft:music.nether.soul_sand_valley", "weight": 1 }
  ],
  "chance_percent": 60,
  "min_delay_seconds": 90,
  "max_delay_seconds": 240
}
```

### How rules work

- Rules are checked **top to bottom, and the first match wins.** Put specific rules above general ones.
- A condition you leave out (or leave as an empty `[]`) means "anywhere".
- Several conditions in one rule must **all** be true. Inside one list, matching **any** entry is enough.
- A rule with an empty `"tracks": []` means **silence** in that area.

### The settings

| Setting | What it does |
|---|---|
| `situations` | `menu`, `in_game`, `survival`, `creative`, `spectator`, `underwater`, `raining`, `thundering` |
| `dimensions` | `overworld`, `nether`, `the_end` (the long forms like `minecraft:the_nether` work too), or a modded dimension's full ID |
| `biomes` | Biome IDs like `minecraft:plains`, or tags starting with `#` like `#minecraft:is_forest` |
| `blocks` | For map makers: the rule only applies while you stand on one of these blocks, like `"minecraft:gold_block"`, or a block tag like `"#minecraft:planks"`. It counts the block under your feet, or a thin block you stand in (carpet, pressure plate, slab). |
| `time` | `any`, `day`, or `night` (Overworld only) |
| `min_y` / `max_y` | Height limits (the Y number on the F3 screen) |
| `tracks` | The songs: `sound` is a song title (`"Mice on Venus"`) or a sound ID, `weight` is how likely it is (3 = three times as likely as 1) |
| `chance_percent` | 0 to 100. The chance that a song really starts each time the game is ready for one. If the roll fails, the mod waits another delay and rolls again. |
| `min_delay_seconds` / `max_delay_seconds` | How long to wait between songs. If left out, the global defaults at the top of the file are used. |
| `stop_when_leaving` | `true` cuts the song off when you leave the area. `false` (default) lets it finish. |

Top-level settings: `enabled` (master switch), `show_music_toast` (`true` or `false`, the Now Playing pop-up), `when_no_rule_matches` (`"vanilla"` or `"silence"`), `first_song_delay_seconds`, `default_min_delay_seconds`, `default_max_delay_seconds`.

### Finding names to use

- **Where am I?** Type `/tunecontrol status`. It prints your current dimension, biome, height and the block you are standing on, and which rule matches. Copy the names straight into your rule.
- **What songs exist?** Open the song list in the settings screen, or see "Every song you can pick" at the bottom of this page. Music discs are `minecraft:music_disc.cat`, `minecraft:music_disc.13`, and so on.
- **Hear a song immediately:** `/tunecontrol test Mice on Venus`

### Commands

| Command | What it does |
|---|---|
| `/tunecontrol status` | Shows where you are, the matching rule, what's playing and when the next roll happens |
| `/tunecontrol reload` | Re-reads the config file (a typo is reported and the old settings stay active) |
| `/tunecontrol stop` | Stops the current song and keeps everything quiet (no automatic songs, no normal Minecraft music) until you type `/tunecontrol start` |
| `/tunecontrol start` | Leaves quiet mode and plays a song from the rule that applies where you are, right now (ignores the timer and the chance roll) |
| `/tunecontrol skip` | Stops the song and rolls for the next one immediately |
| `/tunecontrol previous` | Goes back to the song you heard before this one (press again to keep going back). It plays that exact song again, even if it came from a random set. Between songs, or after `stop`, it replays the most recent song. |
| `/tunecontrol config` | Opens the settings screen |
| `/tunecontrol test <sound>` | Plays any sound ID right now |

`previous` remembers the last 50 songs you heard, both the ones Tune Control started and normal Minecraft music, and forgets them when you close the game. It can replay exactly the songs in the list at the bottom of this page. A song from a resource pack or another mod that is part of a random set cannot be told apart, so going back to one of those plays its set again (which may pick a different song).

---

## Part 5: Using your own music

Minecraft only plays **`.ogg`** (Ogg Vorbis) files, and they must be added through a resource pack. A ready-made starter is in `example_resource_pack/`:

1. Convert your song to `.ogg` (the free tool Audacity can export it).
2. Name it `forest_song.ogg` and put it in `example_resource_pack/assets/mytunes/sounds/`.
3. Copy the whole `example_resource_pack` folder into `.minecraft/resourcepacks/` (rename the folder if you like).
4. In Minecraft go to **Options > Resource Packs** and switch the pack on.
5. In the config, use `"sound": "mytunes:forest_song"` in a rule (the first example rule already does).

To add more songs, add more entries to `example_resource_pack/assets/mytunes/sounds.json` (copy the `forest_song` block, rename it) and drop matching `.ogg` files next to the first. After changing a resource pack in game, press **F3 + T** to reload it.

---

## Every song you can pick

Minecraft's own music settings only know *sets* of songs (for example `minecraft:music.game` plays a random Overworld song). To let a rule play **one specific song**, Tune Control adds one sound per song, named `tunecontrol:<name>`. Each one simply plays Minecraft's own music file, so no music is copied.

In a rule you can write the title (`Mice on Venus`), the short name (`mice_on_venus`) or the full ID (`tunecontrol:mice_on_venus`). Capital letters and accents don't matter.

> **Composers** are the ones credited for each song on Minecraft's music pages (Wikipedia's "Music of Minecraft" and the Minecraft wiki). They are only used for the Now Playing pop-up. *Boss* has no composer listed on any page we could check, so the pop-up shows its title only instead of guessing.

> These songs are set up for **Minecraft 1.20.1**. A newer Minecraft version can rename its music files, in which case the list (and `sounds.json`) would need updating. If a song ever plays silence, search the game's `logs/latest.log` for "tunecontrol": Minecraft prints a line there naming the missing file.

**Overworld**

| Title | Full ID | Composer |
|---|---|---|
| A Familiar Room | `tunecontrol:a_familiar_room` | Aaron Cherof |
| Aerie | `tunecontrol:aerie` | Lena Raine |
| An Ordinary Day | `tunecontrol:an_ordinary_day` | Kumi Tanioka |
| Ancestry | `tunecontrol:ancestry` | Lena Raine |
| Bromeliad | `tunecontrol:bromeliad` | Aaron Cherof |
| Clark | `tunecontrol:clark` | C418 |
| Comforting Memories | `tunecontrol:comforting_memories` | Kumi Tanioka |
| Crescent Dunes | `tunecontrol:crescent_dunes` | Aaron Cherof |
| Danny | `tunecontrol:danny` | C418 |
| Dry Hands | `tunecontrol:dry_hands` | C418 |
| Echo in the Wind | `tunecontrol:echo_in_the_wind` | Aaron Cherof |
| Firebugs | `tunecontrol:firebugs` | Lena Raine |
| Floating Dream | `tunecontrol:floating_dream` | Kumi Tanioka |
| Haggstrom | `tunecontrol:haggstrom` | C418 |
| Infinite Amethyst | `tunecontrol:infinite_amethyst` | Lena Raine |
| Key | `tunecontrol:key` | C418 |
| Labyrinthine | `tunecontrol:labyrinthine` | Lena Raine |
| Left to Bloom | `tunecontrol:left_to_bloom` | Lena Raine |
| Living Mice | `tunecontrol:living_mice` | C418 |
| Mice on Venus | `tunecontrol:mice_on_venus` | C418 |
| Minecraft | `tunecontrol:minecraft` | C418 |
| One More Day | `tunecontrol:one_more_day` | Lena Raine |
| Oxygène | `tunecontrol:oxygene` | C418 |
| Stand Tall | `tunecontrol:stand_tall` | Lena Raine |
| Subwoofer Lullaby | `tunecontrol:subwoofer_lullaby` | C418 |
| Sweden | `tunecontrol:sweden` | C418 |
| Wending | `tunecontrol:wending` | Lena Raine |
| Wet Hands | `tunecontrol:wet_hands` | C418 |

**Creative mode**

| Title | Full ID | Composer |
|---|---|---|
| Aria Math | `tunecontrol:aria_math` | C418 |
| Biome Fest | `tunecontrol:biome_fest` | C418 |
| Blind Spots | `tunecontrol:blind_spots` | C418 |
| Dreiton | `tunecontrol:dreiton` | C418 |
| Haunt Muskie | `tunecontrol:haunt_muskie` | C418 |
| Taswell | `tunecontrol:taswell` | C418 |

**Title screen**

| Title | Full ID | Composer |
|---|---|---|
| Beginning 2 | `tunecontrol:beginning_2` | C418 |
| Floating Trees | `tunecontrol:floating_trees` | C418 |
| Moog City 2 | `tunecontrol:moog_city_2` | C418 |
| Mutation | `tunecontrol:mutation` | C418 |

**Underwater**

| Title | Full ID | Composer |
|---|---|---|
| Axolotl | `tunecontrol:axolotl` | C418 |
| Dragon Fish | `tunecontrol:dragon_fish` | C418 |
| Shuniji | `tunecontrol:shuniji` | C418 |

**The Nether**

| Title | Full ID | Composer |
|---|---|---|
| Ballad of the Cats | `tunecontrol:ballad_of_the_cats` | C418 |
| Chrysopoeia | `tunecontrol:chrysopoeia` | Lena Raine |
| Concrete Halls | `tunecontrol:concrete_halls` | C418 |
| Dead Voxel | `tunecontrol:dead_voxel` | C418 |
| Rubedo | `tunecontrol:rubedo` | Lena Raine |
| So Below | `tunecontrol:so_below` | Lena Raine |
| Warmth | `tunecontrol:warmth` | C418 |

**The End**

| Title | Full ID | Composer |
|---|---|---|
| Alpha | `tunecontrol:alpha` | C418 |
| Boss | `tunecontrol:boss` | not listed |
| The End | `tunecontrol:the_end` | C418 |

Music discs are ordinary sounds, such as `minecraft:music_disc.cat`. Songs from resource packs or other mods use their own sound IDs.

## Troubleshooting

- **The build prints `warning:` or `Note: ... uses or overrides a deprecated API`:** harmless. Only lines that say `error:` stop the build.
- **Nothing changes:** type `/tunecontrol status`. It shows whether the mod is on, which rule matches, and any config or sound-ID problems.
- **"Unknown sound":** a typo in the ID, or the resource pack that defines it isn't switched on.
- **No Now Playing pop-up:** check that **Music toast** is On on the main settings screen, that your Music and Master volume in Options > Music & Sounds are above 0, and that **Tune Control itself** is switched On (the pop-up is part of it).
- **Music slider at 0:** the Music volume in Options > Music & Sounds applies to this mod too.
- **After you leave a rule's area,** normal Minecraft music comes back on its own slow timer (up to about 20 minutes, same as vanilla). To make it fully silent instead, set `"when_no_rule_matches": "silence"`.
- **Starting a song cuts off** any vanilla background song that is still playing.
- **While a rule is in charge,** other mods' background music (anything using Minecraft's "music" sound category) is blocked too, so songs never overlap.
- **Jukeboxes** are not affected by this mod.

## What each file does

| File | Purpose |
|---|---|
| `TuneControl.java` | The mod's entry point (on a client it adds the Config button to the Mods list) |
| `config/TuneConfig.java`, `Rule.java`, `Track.java` | The settings file and its structure |
| `client/Context.java` | "Where is the player right now?" |
| `client/RuleMatcher.java` | Decides which rule applies |
| `client/MusicController.java` | Timer, dice roll, song picking and playing |
| `client/ClientEvents.java` | Hooks into Minecraft and defines the `/tunecontrol` commands |
| `client/ModEvents.java` | Loads the config at startup |
| `client/SongCatalog.java` | The list of songs with their real titles and composers, and the code that understands a typed title |
| `assets/tunecontrol/sounds.json` | Gives each song its own sound name (`tunecontrol:mice_on_venus`) so a rule can play exactly one |
| `client/ClientSetup.java` | Registers the settings screen so Forge's Mods list shows a Config button |
| `client/gui/TuneConfigScreen.java` | The main settings screen |
| `client/gui/RuleListScreen.java`, `RuleEditScreen.java`, `SongPickerScreen.java` | The rule list, the three-tab rule editor and the song list |
| `client/gui/ChoiceScreen.java`, `Choices.java` | The searchable pick list (the stand-in for a drop-down) and the lists of situations, dimensions, biomes and blocks |
| `client/SongToast.java`, `SongInfo.java` | The Now Playing pop-up, and the code that works out the title and composer to show |
| `client/Names.java` | Turns IDs into readable names ("minecraft:old_growth_pine_taiga" into "Old Growth Pine Taiga") |
| `client/gui/TuneScreen.java`, `PercentSlider.java`, `GuiText.java` | Shared building blocks for the screens |
| `META-INF/mods.toml` | Mod information for Forge |
| `pack.mcmeta` | Tiny metadata file Minecraft expects in every mod (without it Forge logs a harmless "ResourcePackInfo" warning) |

## Ideas for later

- Fading songs in and out
- Key bindings for previous / skip / stop (so you don't have to type commands)
- Rules for boss fights, nearby mobs, or structures (villages, strongholds)
- Ports to NeoForge or Minecraft 26.1
