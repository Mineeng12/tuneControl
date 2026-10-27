package com.tunecontrol.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One music rule. It says WHERE it applies, WHAT it plays and HOW OFTEN.
 * The field names are exactly what you see in config/tunecontrol.json.
 */
public class Rule {
    public String name = "Unnamed rule";
    public boolean enabled = true;

    // ---- WHERE / WHEN ----
    // Every list below that is not empty must match. An empty list means "no restriction".
    // Inside one list, matching ANY entry is enough.

    /** menu, in_game, survival, creative, spectator, underwater, raining, thundering */
    public List<String> situations = new ArrayList<>();
    /** For example "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end" (or modded ones). */
    public List<String> dimensions = new ArrayList<>();
    /** Biome IDs like "minecraft:plains", or biome tags starting with # like "#minecraft:is_forest". */
    public List<String> biomes = new ArrayList<>();
    /**
     * For map makers: the rule only applies while you stand on one of these blocks, e.g. "minecraft:gold_block".
     * Tags work too, e.g. "#minecraft:planks". It counts the block under your feet, or a thin block you stand in
     * (carpet, pressure plate, slab).
     */
    public List<String> blocks = new ArrayList<>();
    /** "any", "day" or "night" (only meaningful in the Overworld). */
    public String time = "any";
    /** Height limits (the Y coordinate shown in the F3 screen). */
    public int min_y = -2048;
    public int max_y = 2048;

    // ---- WHAT ----
    /** The songs to pick from. An empty list means "silence while this rule applies". */
    public List<Track> tracks = new ArrayList<>();

    // ---- HOW OFTEN ----
    /** Chance (0 to 100) that a song really starts each time the game is ready to start one. */
    public double chance_percent = 100.0;
    /** Optional. How long to wait between songs. If left out, the global defaults are used. */
    public Double min_delay_seconds = null;
    public Double max_delay_seconds = null;
    /** If true, the song is cut off when you walk out of the area where this rule applies. */
    public boolean stop_when_leaving = false;

    /** Repairs missing or silly values so the rest of the mod never has to worry about them. */
    public void sanitize() {
        if (name == null) name = "Unnamed rule";
        if (situations == null) situations = new ArrayList<>();
        if (dimensions == null) dimensions = new ArrayList<>();
        if (biomes == null) biomes = new ArrayList<>();
        if (blocks == null) blocks = new ArrayList<>();
        if (tracks == null) tracks = new ArrayList<>();

        situations.removeIf(s -> s == null);
        dimensions.removeIf(s -> s == null);
        biomes.removeIf(s -> s == null);
        blocks.removeIf(s -> s == null);
        blocks.replaceAll(s -> s.trim().toLowerCase(Locale.ROOT));
        tracks.removeIf(t -> t == null || t.sound == null);

        situations.replaceAll(s -> s.trim().toLowerCase(Locale.ROOT));

        time = (time == null) ? "any" : time.trim().toLowerCase(Locale.ROOT);
        if (!time.equals("day") && !time.equals("night")) time = "any";

        chance_percent = Math.max(0.0, Math.min(100.0, chance_percent));

        if (min_delay_seconds != null) min_delay_seconds = Math.max(0.0, min_delay_seconds);
        if (max_delay_seconds != null) max_delay_seconds = Math.max(0.0, max_delay_seconds);
        if (min_delay_seconds != null && max_delay_seconds != null && max_delay_seconds < min_delay_seconds) {
            double swap = min_delay_seconds;
            min_delay_seconds = max_delay_seconds;
            max_delay_seconds = swap;
        }
    }
}
