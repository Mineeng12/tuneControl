package com.tunecontrol.client.gui;

import com.tunecontrol.client.SongCatalog;
import com.tunecontrol.config.Track;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Small text helpers for the settings screens. There is no Minecraft code in here. */
public final class GuiText {
    private GuiText() {
    }

    /** 5.0 becomes "5" and 2.5 stays "2.5". (Never produces 1.0E-4 style text.) */
    public static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0";
        return new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
    }

    public static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** "a, b,, c" becomes [a, b, c]. */
    public static List<String> parseList(String text) {
        List<String> result = new ArrayList<>();
        for (String part : text.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    public static String joinList(List<String> list) {
        return String.join(", ", list);
    }

    /**
     * "Mice on Venus, minecraft:music.creative*3" becomes two tracks with weights 1 and 3.
     * Song titles are turned into their sound IDs (Mice on Venus -> tunecontrol:mice_on_venus).
     */
    public static List<Track> parseTracks(String text) {
        List<Track> result = new ArrayList<>();
        for (String part : parseList(text)) {
            String sound = part;
            int weight = 1;
            int star = part.lastIndexOf('*');
            if (star > 0) {
                sound = part.substring(0, star).trim();
                try {
                    weight = Math.max(1, Integer.parseInt(part.substring(star + 1).trim()));
                } catch (NumberFormatException ignored) {
                    // keep weight 1
                }
            }
            if (!sound.isEmpty()) result.add(new Track(SongCatalog.resolve(sound), weight));
        }
        return result;
    }

    /** The reverse of parseTracks: songs are shown by title, and weight 1 is left out. */
    public static String tracksToText(List<Track> tracks) {
        StringBuilder text = new StringBuilder();
        for (Track track : tracks) {
            if (text.length() > 0) text.append(", ");
            text.append(SongCatalog.displayName(track.sound));
            if (track.weight != 1) text.append('*').append(track.weight);
        }
        return text.toString();
    }
}
