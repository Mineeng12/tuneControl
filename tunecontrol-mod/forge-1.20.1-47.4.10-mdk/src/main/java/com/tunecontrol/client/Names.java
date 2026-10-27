package com.tunecontrol.client;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Small text helpers for turning IDs into readable names (and back).
 * There is no Minecraft code in this class, so it can be tested on its own.
 */
public final class Names {
    private Names() {
    }

    private static final Set<String> SMALL_WORDS = Set.of("of", "the", "on", "in", "to", "a", "an", "and");

    /** The form used to compare two entries: no spaces around it, all lowercase. */
    public static String canonical(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Turns a dimension entry into its full ID. The plain words overworld, nether, the_nether, end and the_end
     * work too ("nether" becomes "minecraft:the_nether"). Anything else is only tidied up.
     */
    public static String dimensionId(String text) {
        String tidy = canonical(text);
        if (!tidy.contains(":")) {
            switch (SongCatalog.normalize(tidy)) {
                case "overworld":
                    return "minecraft:overworld";
                case "nether":
                case "thenether":
                    return "minecraft:the_nether";
                case "end":
                case "theend":
                    return "minecraft:the_end";
                default:
                    break;
            }
        }
        return tidy;
    }

    /** "old_growth_pine_taiga" becomes "Old Growth Pine Taiga". Small words like "of" stay lowercase. */
    public static String prettyPath(String path) {
        if (path == null) return "";
        String[] words = path.replace('/', ' ').replace('_', ' ').replace('.', ' ').trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            if (i > 0 && SMALL_WORDS.contains(word)) {
                out.append(word);
            } else {
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return out.toString();
    }

    /**
     * A friendly name for an ID: "minecraft:old_growth_pine_taiga" becomes "Old Growth Pine Taiga",
     * a mod's "mymod:cool_block" becomes "Cool Block (mymod)", and a tag such as "#minecraft:is_forest"
     * becomes "Any Forest (tag)".
     */
    public static String pretty(String id) {
        String text = canonical(id);
        if (text.isEmpty()) return "";
        if (text.startsWith("#")) {
            String path = pathOf(text.substring(1));
            if (path.startsWith("is_")) path = path.substring(3);
            return "Any " + prettyPath(path) + " (tag)";
        }
        int colon = text.indexOf(':');
        if (colon < 0) return prettyPath(text);
        String namespace = text.substring(0, colon);
        String name = prettyPath(text.substring(colon + 1));
        return namespace.equals("minecraft") ? name : name + " (" + namespace + ")";
    }

    private static String pathOf(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    /**
     * True when the (already lowercase) text could be an ID or a tag: "minecraft:gold_block", "gold_block" or "#minecraft:logs".
     * Spaces and capital letters are not allowed in IDs.
     */
    public static boolean looksLikeId(String text) {
        return text != null && text.matches("#?([a-z0-9_.-]+:)?[a-z0-9_./-]+");
    }

    /** Cuts text that is longer than max characters and adds "...". */
    public static String shorten(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max || max < 4) return text;
        return text.substring(0, max - 3).stripTrailing() + "...";
    }

    /**
     * One short line for a list of names: "Anywhere" when empty, otherwise the names separated by commas,
     * and "+3 more" when they do not all fit in maxChars characters.
     */
    public static String summary(List<String> names, int maxChars) {
        if (names.isEmpty()) return "Anywhere";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (i > 0 && out.length() + 2 + name.length() > maxChars) {
                out.append(" +").append(names.size() - i).append(" more");
                return out.toString();
            }
            if (i > 0) out.append(", ");
            out.append(i == 0 ? shorten(name, maxChars) : name);
        }
        return out.toString();
    }
}
