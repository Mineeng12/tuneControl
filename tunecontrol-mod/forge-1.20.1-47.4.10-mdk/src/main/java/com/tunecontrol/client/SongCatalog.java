package com.tunecontrol.client;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The list of Minecraft's individual songs, by their real titles.
 *
 * Minecraft itself only knows "sound events" such as music.game, which pick a RANDOM song from a set.
 * To play ONE specific song, this mod ships a small file (assets/tunecontrol/sounds.json) that gives
 * every song its own sound event, named tunecontrol:<slug> (for example tunecontrol:mice_on_venus).
 * Each of those events plays exactly one of Minecraft's own music files, so no music is copied.
 *
 * The "file" column is the song's file name in Minecraft 1.20.1. If you add a row here, add the matching
 * entry to sounds.json too (the entry is "minecraft:" + file).
 *
 * There is no Minecraft code in this class.
 */
public final class SongCatalog {
    private SongCatalog() {
    }

    /** The namespace (the part before the colon) of every song ID: tunecontrol:mice_on_venus. */
    public static final String NAMESPACE = "tunecontrol";

    /** Where a song normally plays in Minecraft. */
    public enum Group {
        OVERWORLD("Overworld"),
        CREATIVE("Creative"),
        TITLE_SCREEN("Title screen"),
        UNDERWATER("Underwater"),
        NETHER("Nether"),
        THE_END("The End");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    // Who composed the songs (shown in the "Now Playing" toast). Credits are only listed where a source states them.
    // "Boss" has no credit listed anywhere we could check, so it is shown without an artist rather than guessed.
    static final String C418 = "C418";
    static final String RAINE = "Lena Raine";
    static final String TANIOKA = "Kumi Tanioka";
    static final String ABERG = "Samuel \u00c5berg";
    static final String CHEROF = "Aaron Cherof";

    /**
     * One song. "slug" is the lowercase ID part, "file" is Minecraft's own file for it (without .ogg),
     * "artist" is the composer ("" when not known).
     */
    public record Song(String slug, String title, Group group, String file, String artist) {
        public String id() {
            return NAMESPACE + ":" + slug;
        }
    }

    private static final List<Song> SONGS = new ArrayList<>();
    private static final Map<String, Song> BY_ID = new HashMap<>();
    private static final Map<String, Song> BY_NAME = new HashMap<>(); // lookup by a "simplified" title, see normalize()
    private static final Map<String, Song> BY_FILE = new HashMap<>(); // lookup by Minecraft's file, "minecraft:music/game/piano3"

    private static void song(String slug, String title, Group group, String file, String artist) {
        Song song = new Song(slug, title, group, file, artist);
        SONGS.add(song);
        BY_ID.put(song.id(), song);
        BY_FILE.put("minecraft:" + file, song);
        for (String key : new String[]{normalize(title), normalize(slug), normalize(song.id())}) {
            Song other = BY_NAME.put(key, song);
            if (other != null && other != song) {
                throw new IllegalStateException("SongCatalog: '" + title + "' clashes with '" + other.title() + "'");
            }
        }
    }

    static {
        // ---- Overworld: the classic C418 songs ----
        song("minecraft", "Minecraft", Group.OVERWORLD, "music/game/calm1", C418);
        song("clark", "Clark", Group.OVERWORLD, "music/game/calm2", C418);
        song("sweden", "Sweden", Group.OVERWORLD, "music/game/calm3", C418);
        song("subwoofer_lullaby", "Subwoofer Lullaby", Group.OVERWORLD, "music/game/hal1", C418);
        song("living_mice", "Living Mice", Group.OVERWORLD, "music/game/hal2", C418);
        song("haggstrom", "Haggstrom", Group.OVERWORLD, "music/game/hal3", C418);
        song("danny", "Danny", Group.OVERWORLD, "music/game/hal4", C418);
        song("key", "Key", Group.OVERWORLD, "music/game/nuance1", C418);
        song("oxygene", "Oxyg\u00e8ne", Group.OVERWORLD, "music/game/nuance2", C418); // the title has an accented e
        song("dry_hands", "Dry Hands", Group.OVERWORLD, "music/game/piano1", C418);
        song("wet_hands", "Wet Hands", Group.OVERWORLD, "music/game/piano2", C418);
        song("mice_on_venus", "Mice on Venus", Group.OVERWORLD, "music/game/piano3", C418);

        // ---- Overworld: newer songs (caves, biomes, swamp, deep dark) ----
        song("a_familiar_room", "A Familiar Room", Group.OVERWORLD, "music/game/a_familiar_room", CHEROF);
        song("an_ordinary_day", "An Ordinary Day", Group.OVERWORLD, "music/game/an_ordinary_day", TANIOKA);
        song("ancestry", "Ancestry", Group.OVERWORLD, "music/game/ancestry", RAINE);
        song("bromeliad", "Bromeliad", Group.OVERWORLD, "music/game/bromeliad", CHEROF);
        song("comforting_memories", "Comforting Memories", Group.OVERWORLD, "music/game/comforting_memories", TANIOKA);
        song("crescent_dunes", "Crescent Dunes", Group.OVERWORLD, "music/game/crescent_dunes", CHEROF);
        song("echo_in_the_wind", "Echo in the Wind", Group.OVERWORLD, "music/game/echo_in_the_wind", CHEROF);
        song("floating_dream", "Floating Dream", Group.OVERWORLD, "music/game/floating_dream", TANIOKA);
        song("infinite_amethyst", "Infinite Amethyst", Group.OVERWORLD, "music/game/infinite_amethyst", RAINE);
        song("left_to_bloom", "Left to Bloom", Group.OVERWORLD, "music/game/left_to_bloom", RAINE);
        song("one_more_day", "One More Day", Group.OVERWORLD, "music/game/one_more_day", RAINE);
        song("stand_tall", "Stand Tall", Group.OVERWORLD, "music/game/stand_tall", RAINE);
        song("wending", "Wending", Group.OVERWORLD, "music/game/wending", RAINE);
        song("aerie", "Aerie", Group.OVERWORLD, "music/game/swamp/aerie", RAINE);
        song("firebugs", "Firebugs", Group.OVERWORLD, "music/game/swamp/firebugs", RAINE);
        song("labyrinthine", "Labyrinthine", Group.OVERWORLD, "music/game/swamp/labyrinthine", RAINE);

        // ---- Creative mode ----
        song("biome_fest", "Biome Fest", Group.CREATIVE, "music/game/creative/creative1", C418);
        song("blind_spots", "Blind Spots", Group.CREATIVE, "music/game/creative/creative2", C418);
        song("haunt_muskie", "Haunt Muskie", Group.CREATIVE, "music/game/creative/creative3", C418);
        song("aria_math", "Aria Math", Group.CREATIVE, "music/game/creative/creative4", C418);
        song("dreiton", "Dreiton", Group.CREATIVE, "music/game/creative/creative5", C418);
        song("taswell", "Taswell", Group.CREATIVE, "music/game/creative/creative6", C418);

        // ---- Title screen ----
        song("mutation", "Mutation", Group.TITLE_SCREEN, "music/menu/menu1", C418);
        song("moog_city_2", "Moog City 2", Group.TITLE_SCREEN, "music/menu/menu2", C418);
        song("beginning_2", "Beginning 2", Group.TITLE_SCREEN, "music/menu/menu3", C418);
        song("floating_trees", "Floating Trees", Group.TITLE_SCREEN, "music/menu/menu4", C418);

        // ---- Underwater ----
        song("axolotl", "Axolotl", Group.UNDERWATER, "music/game/water/axolotl", C418);
        song("dragon_fish", "Dragon Fish", Group.UNDERWATER, "music/game/water/dragon_fish", C418);
        song("shuniji", "Shuniji", Group.UNDERWATER, "music/game/water/shuniji", C418);

        // ---- The Nether ----
        song("concrete_halls", "Concrete Halls", Group.NETHER, "music/game/nether/nether1", C418);
        song("dead_voxel", "Dead Voxel", Group.NETHER, "music/game/nether/nether2", C418);
        song("warmth", "Warmth", Group.NETHER, "music/game/nether/nether3", C418);
        song("ballad_of_the_cats", "Ballad of the Cats", Group.NETHER, "music/game/nether/nether4", C418);
        song("rubedo", "Rubedo", Group.NETHER, "music/game/nether/nether_wastes/rubedo", RAINE);
        song("chrysopoeia", "Chrysopoeia", Group.NETHER, "music/game/nether/crimson_forest/chrysopoeia", RAINE);
        song("so_below", "So Below", Group.NETHER, "music/game/nether/soulsand_valley/so_below", RAINE);

        // ---- The End ----
        song("the_end", "The End", Group.THE_END, "music/game/end/end", C418);
        song("boss", "Boss", Group.THE_END, "music/game/end/boss", "");
        song("alpha", "Alpha", Group.THE_END, "music/game/end/credits", C418);
    }

    /** Every song, in the order above. */
    public static List<Song> all() {
        return Collections.unmodifiableList(SONGS);
    }

    /** The song with this ID (for example "tunecontrol:mice_on_venus"), or null. */
    public static Song byId(String id) {
        if (id == null) return null;
        return BY_ID.get(id.trim().toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * The song that plays this Minecraft music file, or null. The file looks like "minecraft:music/game/piano3"
     * (the "minecraft:" part may be left out, and a trailing ".ogg" is ignored).
     */
    public static Song byFile(String file) {
        if (file == null) return null;
        String key = file.trim().toLowerCase(java.util.Locale.ROOT);
        if (key.endsWith(".ogg")) key = key.substring(0, key.length() - 4);
        if (!key.contains(":")) key = "minecraft:" + key;
        return BY_FILE.get(key);
    }

    /** The title for a song ID, or null when the ID is not one of ours. */
    public static String titleOf(String id) {
        Song song = byId(id);
        return song == null ? null : song.title();
    }

    /** A friendly name for showing to the player: the title when we know it, otherwise the ID itself. */
    public static String displayName(String id) {
        String title = titleOf(id);
        return title != null ? title : id;
    }

    /**
     * Turns what a person typed into a song ID when it names one of our songs: a title ("Mice on Venus"),
     * the short name ("mice_on_venus") or the full ID ("tunecontrol:mice_on_venus"), in any capitals and
     * with or without accents. Anything else (for example "minecraft:music.game") is returned unchanged.
     */
    public static String resolve(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        Song song = BY_NAME.get(normalize(trimmed));
        return song != null ? song.id() : trimmed;
    }

    /**
     * Lowercase letters and digits only, accents removed: "Oxygene" (with the accent) and "oxygene" and "Oxy-gene" all become "oxygene".
     * This is what makes typing and searching forgiving.
     */
    public static String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < decomposed.length(); i++) {
            char c = Character.toLowerCase(decomposed.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) out.append(c);
        }
        return out.toString();
    }

    /** True when the search words appear in the label or in the ID (ignoring capitals, accents and spaces). */
    public static boolean matchesSearch(String search, String label, String id) {
        String wanted = normalize(search);
        if (wanted.isEmpty()) return true;
        return normalize(label).contains(wanted) || normalize(id).contains(wanted);
    }
}
