package com.tunecontrol.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tunecontrol.TuneControl;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The whole settings file (config/tunecontrol.json), plus the code that reads and writes it.
 * Gson (already inside Minecraft) turns this class into JSON and back.
 */
public class TuneConfig {
    public String _readme = "Rules are checked from top to bottom and the FIRST rule that matches wins. "
            + "Set \"enabled\" to true on an example rule to try it. In game, type /tunecontrol reload after saving this file. "
            + "You can also edit everything in game: Mods > Tune Control > Config, or type /tunecontrol config.";

    /** Master switch. false = this mod does nothing and vanilla music is untouched. */
    public boolean enabled = true;
    /** Show a "Now Playing" pop-up in the top-right corner when a song starts (like the music toast in newer Minecraft). */
    public boolean show_music_toast = true;
    /** What to do where no rule matches: "vanilla" (normal Minecraft music) or "silence". */
    public String when_no_rule_matches = "vanilla";
    /** Wait this long after joining a world (or opening the title screen) before the first song. */
    public double first_song_delay_seconds = 5.0;
    /** Time between songs for rules that do not set their own delays. (Vanilla uses 10 to 20 minutes.) */
    public double default_min_delay_seconds = 180.0;
    public double default_max_delay_seconds = 360.0;

    public List<Rule> rules = new ArrayList<>();

    public boolean silenceWhenNoRuleMatches() {
        return "silence".equalsIgnoreCase(when_no_rule_matches);
    }

    private void sanitize() {
        if (when_no_rule_matches == null) when_no_rule_matches = "vanilla";
        first_song_delay_seconds = Math.max(0.0, first_song_delay_seconds);
        default_min_delay_seconds = Math.max(0.0, default_min_delay_seconds);
        default_max_delay_seconds = Math.max(default_min_delay_seconds, default_max_delay_seconds);
        if (rules == null) rules = new ArrayList<>();
        rules.removeIf(r -> r == null);
        for (Rule rule : rules) rule.sanitize();
    }

    // ------------------------------------------------------------------
    // Loading and saving
    // ------------------------------------------------------------------

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile TuneConfig current = new TuneConfig();
    private static volatile String lastError = "";

    public static TuneConfig get() {
        return current;
    }

    /** Empty when the last load worked, otherwise a short description of what went wrong. */
    public static String getLastError() {
        return lastError;
    }

    public static Path getFile() {
        return FMLPaths.CONFIGDIR.get().resolve("tunecontrol.json");
    }

    /** A separate copy of the live settings. The in-game editor changes this copy, so nothing is applied until you save. */
    public static TuneConfig copyOfCurrent() {
        return GSON.fromJson(GSON.toJson(current), TuneConfig.class);
    }

    /**
     * Writes the given settings to config/tunecontrol.json and makes them the live settings.
     * Returns an empty text when it worked, otherwise a description of what went wrong.
     */
    public static String saveAndApply(TuneConfig updated) {
        updated.sanitize();
        Path file = getFile();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(updated, writer);
            }
        } catch (IOException e) {
            TuneControl.LOGGER.error("Tune Control: could not save {}", file, e);
            return "Could not save tunecontrol.json: " + e.getMessage();
        }
        current = updated;
        lastError = "";
        return "";
    }

    /**
     * Reads config/tunecontrol.json. If it does not exist yet, a starter file with example rules is created.
     * If the file has a typo, the previous settings stay active and the problem is reported by /tunecontrol status.
     * (A broken file is never overwritten, so you can fix your typo.)
     */
    public static void load() {
        Path file = getFile();
        try {
            if (!Files.exists(file)) {
                TuneConfig defaults = createDefaults();
                defaults.sanitize();
                Files.createDirectories(file.getParent());
                try (Writer writer = Files.newBufferedWriter(file)) {
                    GSON.toJson(defaults, writer);
                }
                current = defaults;
                lastError = "";
                TuneControl.LOGGER.info("Tune Control: created starter config at {}", file);
                return;
            }

            try (Reader reader = Files.newBufferedReader(file)) {
                TuneConfig loaded = GSON.fromJson(reader, TuneConfig.class);
                if (loaded == null) throw new IllegalStateException("the file is empty");
                loaded.sanitize();
                current = loaded;
                lastError = "";
                TuneControl.LOGGER.info("Tune Control: loaded {} rule(s) from {}", loaded.rules.size(), file);
            }
        } catch (IOException | RuntimeException e) {
            lastError = "Could not read tunecontrol.json: " + e.getMessage();
            TuneControl.LOGGER.error("Tune Control: {}", lastError, e);
        }
    }

    // ------------------------------------------------------------------
    // The starter file. Everything is switched off so installing the mod changes nothing
    // until you choose to turn an example on.
    // ------------------------------------------------------------------

    private static TuneConfig createDefaults() {
        TuneConfig config = new TuneConfig();

        Rule onlyVenus = example("Example: only \"Mice on Venus\" in the Overworld");
        onlyVenus.dimensions.add("minecraft:overworld");
        onlyVenus.tracks.add(new Track("tunecontrol:mice_on_venus", 1));
        onlyVenus.min_delay_seconds = 30.0;
        onlyVenus.max_delay_seconds = 120.0;
        config.rules.add(onlyVenus);

        Rule goldPad = example("Example (map makers): music while standing on gold blocks");
        goldPad.blocks.add("minecraft:gold_block");
        goldPad.tracks.add(new Track("tunecontrol:aria_math", 1));
        goldPad.min_delay_seconds = 0.0;
        goldPad.max_delay_seconds = 2.0;
        goldPad.stop_when_leaving = true;
        config.rules.add(goldPad);

        Rule customForest = example("Example: your own song in forests (needs the example resource pack)");
        customForest.biomes.add("#minecraft:is_forest");
        customForest.tracks.add(new Track("mytunes:forest_song", 1));
        customForest.chance_percent = 70;
        customForest.min_delay_seconds = 60.0;
        customForest.max_delay_seconds = 180.0;
        config.rules.add(customForest);

        Rule deepCaves = example("Example: Nether music in deep Overworld caves");
        deepCaves.dimensions.add("minecraft:overworld");
        deepCaves.max_y = 30;
        deepCaves.tracks.add(new Track("minecraft:music.nether.nether_wastes", 1));
        deepCaves.tracks.add(new Track("minecraft:music.nether.soul_sand_valley", 1));
        deepCaves.chance_percent = 60;
        deepCaves.min_delay_seconds = 90.0;
        deepCaves.max_delay_seconds = 240.0;
        config.rules.add(deepCaves);

        Rule quietNights = example("Example: no music at night in the Overworld");
        quietNights.dimensions.add("minecraft:overworld");
        quietNights.time = "night";
        // no tracks = silence while this rule applies
        config.rules.add(quietNights);

        Rule rareEnd = example("Example: the End plays music only 25% of the time");
        rareEnd.dimensions.add("minecraft:the_end");
        rareEnd.tracks.add(new Track("minecraft:music.end", 1));
        rareEnd.chance_percent = 25;
        rareEnd.min_delay_seconds = 120.0;
        rareEnd.max_delay_seconds = 300.0;
        config.rules.add(rareEnd);

        Rule menu = example("Example: the title screen plays the creative-mode track");
        menu.situations.add("menu");
        menu.tracks.add(new Track("minecraft:music.creative", 1));
        menu.min_delay_seconds = 5.0;
        menu.max_delay_seconds = 20.0;
        config.rules.add(menu);

        return config;
    }

    private static Rule example(String name) {
        Rule rule = new Rule();
        rule.name = name;
        rule.enabled = false;
        return rule;
    }
}
