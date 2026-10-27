package com.tunecontrol.client;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Works out what the "Now Playing" toast says: the song's title and (when known) who made it.
 * There is no Minecraft code in this class, so it can be tested on its own.
 *
 * The toast looks at the music FILE that is really playing (for example "minecraft:music/game/piano3"),
 * not at the sound event that was asked for. That matters for random sets such as minecraft:music.game,
 * which pick a different file each time: the toast names the one that was picked.
 */
public final class SongInfo {
    private SongInfo() {
    }

    /** What to show. "artist" is empty when we do not know it. */
    public record Info(String title, String artist) {
        /** One line of text: "Mice on Venus - C418", or just the title when there is no artist. */
        public String line() {
            return artist.isEmpty() ? title : title + " - " + artist;
        }
    }

    // Music discs. Key = the file name under records/ (the same as the end of the sound event "music_disc.<name>").
    // Value = { title, artist }, as shown on the disc in the game.
    private static final Map<String, String[]> DISCS = new HashMap<>();

    private static void disc(String name, String title, String artist) {
        DISCS.put(name, new String[]{title, artist});
    }

    static {
        for (String name : new String[]{"13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait"}) {
            disc(name, name, SongCatalog.C418);
        }
        disc("otherside", "Otherside", SongCatalog.RAINE);
        disc("pigstep", "Pigstep", SongCatalog.RAINE);
        disc("5", "5", SongCatalog.ABERG);
        disc("relic", "Relic", SongCatalog.CHEROF);
    }

    /**
     * Describes the song that is playing.
     *
     * @param fileId  the music file that was picked ("minecraft:music/game/piano3"), or null when it is not known
     * @param eventId the sound event that was asked for ("tunecontrol:mice_on_venus", "minecraft:music.game", ...)
     */
    public static Info describe(String fileId, String eventId) {
        // 1. One of the songs in our list, found by its file.
        SongCatalog.Song song = SongCatalog.byFile(fileId);
        if (song != null) return new Info(song.title(), song.artist());

        // 2. A music disc.
        String filePath = pathOf(fileId);
        if (filePath.startsWith("records/")) {
            String name = filePath.substring("records/".length());
            String[] disc = DISCS.get(name);
            if (disc != null) return new Info(disc[0] + " (music disc)", disc[1]);
            return new Info(Names.prettyPath(name) + " (music disc)", "");
        }

        // 3. The event itself is one of our songs (used when the file is not known yet).
        song = SongCatalog.byId(eventId);
        if (song != null) return new Info(song.title(), song.artist());

        // 4. Something from a mod, a resource pack or a map: make a readable name from the file, else from the event.
        String name = lastSegment(filePath);
        if (name.isEmpty()) name = lastSegment(pathOf(eventId));
        return new Info(Names.prettyPath(name), "");
    }

    /** "minecraft:music/game/piano3" becomes "music/game/piano3"; null becomes "". */
    private static String pathOf(String id) {
        if (id == null) return "";
        String text = id.trim().toLowerCase(Locale.ROOT);
        int colon = text.indexOf(':');
        return colon < 0 ? text : text.substring(colon + 1);
    }

    /** "music/game/forest_song" becomes "forest_song". */
    private static String lastSegment(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }
}
