package com.tunecontrol.client.gui;

import com.tunecontrol.client.Context;
import com.tunecontrol.client.Names;
import com.tunecontrol.client.gui.ChoiceScreen.Choice;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The lists behind the pick screens: situations, dimensions, biomes and blocks. */
public final class Choices {
    private Choices() {
    }

    // ------------------------------------------------------------------
    // Situations (a fixed list)
    // ------------------------------------------------------------------

    private static final String[][] SITUATIONS = {
            {"menu", "Title screen / menus"},
            {"in_game", "In a world"},
            {"survival", "Survival mode"},
            {"creative", "Creative mode"},
            {"spectator", "Spectator mode"},
            {"underwater", "Underwater"},
            {"raining", "Raining"},
            {"thundering", "Thunderstorm"},
    };

    public static List<Choice> situations() {
        List<Choice> list = new ArrayList<>();
        for (String[] pair : SITUATIONS) list.add(new Choice(pair[0], pair[1]));
        return list;
    }

    /** The shown name for a situation word ("in_game" is shown as "In a world"). */
    public static String situationLabel(String id) {
        String key = Names.canonical(id);
        for (String[] pair : SITUATIONS) {
            if (pair[0].equals(key)) return pair[1];
        }
        return Names.pretty(id);
    }

    // ------------------------------------------------------------------
    // Dimensions
    // ------------------------------------------------------------------

    /** The three Minecraft dimensions, plus the one you are in right now when it comes from a map or a mod. */
    public static List<Choice> dimensions(Minecraft mc) {
        List<Choice> list = new ArrayList<>();
        for (String id : new String[]{"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"}) {
            list.add(new Choice(id, Names.pretty(id)));
        }
        Context now = Context.capture(mc);
        if (now.dimension() != null) {
            String id = now.dimension().toString();
            boolean known = list.stream().anyMatch(choice -> choice.id().equals(id));
            if (!known) list.add(new Choice(id, "Right now: " + Names.pretty(id)));
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Biomes
    // ------------------------------------------------------------------

    private static final String[] BIOME_TAGS = {
            "is_overworld", "is_nether", "is_end", "is_ocean", "is_deep_ocean", "is_river", "is_beach",
            "is_mountain", "is_hill", "is_forest", "is_taiga", "is_jungle", "is_savanna", "is_badlands",
    };

    private static final String[] BIOMES = {
            // Overworld
            "plains", "sunflower_plains", "snowy_plains", "ice_spikes", "desert", "swamp", "mangrove_swamp",
            "forest", "flower_forest", "birch_forest", "old_growth_birch_forest", "dark_forest", "cherry_grove",
            "taiga", "snowy_taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga",
            "savanna", "savanna_plateau", "windswept_savanna", "jungle", "sparse_jungle", "bamboo_jungle",
            "badlands", "eroded_badlands", "wooded_badlands", "meadow", "grove", "snowy_slopes", "frozen_peaks",
            "jagged_peaks", "stony_peaks", "windswept_hills", "windswept_gravelly_hills", "windswept_forest",
            "mushroom_fields", "dripstone_caves", "lush_caves", "deep_dark",
            "river", "frozen_river", "beach", "snowy_beach", "stony_shore",
            "ocean", "deep_ocean", "warm_ocean", "lukewarm_ocean", "deep_lukewarm_ocean", "cold_ocean",
            "deep_cold_ocean", "frozen_ocean", "deep_frozen_ocean",
            // Nether
            "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas",
            // End
            "the_end", "small_end_islands", "end_midlands", "end_highlands", "end_barrens",
            "the_void",
    };

    /** Minecraft's biomes and biome groups. Biomes from mods or data packs can be added by typing their ID in the search box. */
    public static List<Choice> biomes() {
        List<Choice> list = new ArrayList<>();
        for (String tag : BIOME_TAGS) {
            String id = "#minecraft:" + tag;
            list.add(new Choice(id, Names.pretty(id)));
        }
        List<Choice> biomes = new ArrayList<>();
        for (String name : BIOMES) {
            String id = "minecraft:" + name;
            biomes.add(new Choice(id, Names.pretty(id)));
        }
        biomes.sort(Comparator.comparing(Choice::label, String.CASE_INSENSITIVE_ORDER));
        list.addAll(biomes);
        return list;
    }

    /** "Right now: Plains" (the biome you are in), or null on the title screen. */
    public static Choice currentBiome(Minecraft mc) {
        ResourceLocation biome = Context.capture(mc).biome();
        return biome == null ? null : new Choice(biome.toString(), "Right now: " + Names.pretty(biome.toString()));
    }

    // ------------------------------------------------------------------
    // Blocks
    // ------------------------------------------------------------------

    private static final String[] BLOCK_TAGS = {
            "planks", "logs", "leaves", "wool", "wool_carpets", "sand", "dirt", "ice", "snow", "flowers",
            "terracotta", "stone_bricks", "base_stone_overworld", "base_stone_nether", "beds", "slabs", "stairs",
    };

    /** Every block in the game (modded ones too), plus a few block groups. Shown as "Gold Block (minecraft:gold_block)". */
    public static List<Choice> blocks() {
        List<Choice> list = new ArrayList<>();
        for (String tag : BLOCK_TAGS) {
            String id = "#minecraft:" + tag;
            list.add(new Choice(id, Names.pretty(id)));
        }
        List<Choice> blocks = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            String path = id.getPath();
            boolean air = id.getNamespace().equals("minecraft")
                    && (path.equals("air") || path.equals("cave_air") || path.equals("void_air"));
            if (air) continue;
            blocks.add(new Choice(id.toString(), Names.prettyPath(path) + " (" + id + ")"));
        }
        blocks.sort(Comparator.comparing(Choice::label, String.CASE_INSENSITIVE_ORDER));
        list.addAll(blocks);
        return list;
    }

    /** "Right now: Gold Block (minecraft:gold_block)" (the block under your feet), or null when there is none. */
    public static Choice currentBlock(Minecraft mc) {
        String id = Context.capture(mc).groundBlockId();
        if (id == null) return null;
        ResourceLocation location = ResourceLocation.tryParse(id);
        String name = location == null ? id : Names.prettyPath(location.getPath()) + " (" + id + ")";
        return new Choice(id, "Right now: " + name);
    }
}
