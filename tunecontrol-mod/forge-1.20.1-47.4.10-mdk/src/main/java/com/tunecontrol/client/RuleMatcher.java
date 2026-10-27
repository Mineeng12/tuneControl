package com.tunecontrol.client;

import com.tunecontrol.config.Rule;
import com.tunecontrol.config.TuneConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;

import java.util.Collections;
import java.util.Locale;

/** Decides which rule (if any) applies to a given Context. */
public final class RuleMatcher {
    private RuleMatcher() {
    }

    /** Returns the first enabled rule that matches, or null if none does. */
    public static Rule findFirst(TuneConfig config, Context ctx) {
        for (Rule rule : config.rules) {
            if (matches(rule, ctx)) return rule;
        }
        return null;
    }

    public static boolean matches(Rule rule, Context ctx) {
        if (!rule.enabled) return false;

        if (!rule.situations.isEmpty() && Collections.disjoint(rule.situations, ctx.situations())) {
            return false;
        }

        if (!ctx.inWorld()) {
            // On the title screen only "situations" can be checked.
            return rule.dimensions.isEmpty() && rule.biomes.isEmpty() && rule.blocks.isEmpty() && rule.time.equals("any");
        }

        if (!rule.dimensions.isEmpty() && !matchesDimension(rule, ctx)) return false;
        if (!rule.biomes.isEmpty() && !matchesBiome(rule, ctx)) return false;
        if (!rule.blocks.isEmpty() && !matchesBlock(rule, ctx)) return false;

        if (rule.time.equals("day") && !ctx.day()) return false;
        if (rule.time.equals("night") && ctx.day()) return false;

        return ctx.y() >= rule.min_y && ctx.y() <= rule.max_y;
    }

    private static boolean matchesDimension(Rule rule, Context ctx) {
        for (String entry : rule.dimensions) {
            ResourceLocation id = parseDimension(entry);
            if (id != null && id.equals(ctx.dimension())) return true;
        }
        return false;
    }

    /**
     * Like parse(), but also understands the plain words "overworld", "nether" and "end"
     * (also written the_nether, "the end" and so on). Dimensions from mods still need their full ID.
     */
    public static ResourceLocation parseDimension(String text) {
        if (text == null) return null;
        return parse(Names.dimensionId(text));
    }

    private static boolean matchesBiome(Rule rule, Context ctx) {
        for (String entry : rule.biomes) {
            String text = entry.trim();
            if (text.startsWith("#")) {
                // A biome tag, e.g. #minecraft:is_forest
                ResourceLocation tagId = parse(text.substring(1));
                if (tagId != null && ctx.biomeHolder().is(TagKey.create(Registries.BIOME, tagId))) return true;
            } else {
                ResourceLocation id = parse(text);
                if (id != null && id.equals(ctx.biome())) return true;
            }
        }
        return false;
    }

    /**
     * "Standing on" a block: true when the block under your feet, or the thin block you are standing in
     * (a carpet, a pressure plate, a slab), is one of the rule's blocks. Entries starting with # are block tags.
     */
    private static boolean matchesBlock(Rule rule, Context ctx) {
        for (String entry : rule.blocks) {
            String text = entry.trim();
            if (text.startsWith("#")) {
                ResourceLocation tagId = parse(text.substring(1));
                if (tagId == null) continue;
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
                if (hasTag(ctx.feetBlock(), tag) || hasTag(ctx.groundBlock(), tag)) return true;
            } else {
                ResourceLocation id = parse(text);
                if (id == null) continue;
                if (isBlock(ctx.feetBlock(), id) || isBlock(ctx.groundBlock(), id)) return true;
            }
        }
        return false;
    }

    private static boolean isBlock(BlockState state, ResourceLocation id) {
        return state != null && !state.isAir() && id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static boolean hasTag(BlockState state, TagKey<Block> tag) {
        return state != null && !state.isAir() && state.is(tag);
    }

    /** "plains" and "minecraft:plains" both work. Returns null for text that is not a valid ID. */
    public static ResourceLocation parse(String text) {
        if (text == null) return null;
        return ResourceLocation.tryParse(text.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Like parse(), for songs: a title such as "Mice on Venus" becomes tunecontrol:mice_on_venus
     * (see SongCatalog). Everything else is read as an ordinary sound ID.
     */
    public static ResourceLocation parseSound(String text) {
        if (text == null) return null;
        return parse(SongCatalog.resolve(text));
    }
}
