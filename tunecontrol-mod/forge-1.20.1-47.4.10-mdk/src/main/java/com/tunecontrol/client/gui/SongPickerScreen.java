package com.tunecontrol.client.gui;

import com.tunecontrol.client.MusicController;
import com.tunecontrol.client.RuleMatcher;
import com.tunecontrol.client.SongCatalog;
import com.tunecontrol.config.Rule;
import com.tunecontrol.config.Track;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pick songs by their real titles. Click a row to add or remove the song, click Play to hear it.
 * The button at the bottom left switches between groups (Overworld, Creative, Nether...), and the
 * search box finds a song by any part of its title.
 */
public class SongPickerScreen extends TuneScreen {
    /** What the group button cycles through. */
    private enum View {
        ALL("All songs", null),
        OVERWORLD("Overworld", SongCatalog.Group.OVERWORLD),
        CREATIVE("Creative", SongCatalog.Group.CREATIVE),
        TITLE_SCREEN("Title screen", SongCatalog.Group.TITLE_SCREEN),
        UNDERWATER("Underwater", SongCatalog.Group.UNDERWATER),
        NETHER("Nether", SongCatalog.Group.NETHER),
        THE_END("The End", SongCatalog.Group.THE_END),
        DISCS("Music discs", null),
        RANDOM_SETS("Random sets", null),
        PACKS("Packs & mods", null);

        private final String label;
        private final SongCatalog.Group group;

        View(String label, SongCatalog.Group group) {
            this.label = label;
            this.group = group;
        }
    }

    /** One line in the list: the sound ID that gets saved, and the text that is shown. */
    private record Row(String id, String label) {
    }

    private final Rule rule;
    private final int viewIndex;
    private final String filter;
    private final int requestedPage;

    private String typed;           // what is currently typed in the search box
    private String status = "";
    private View view = View.ALL;
    private int count = 0;
    private int pages = 1;
    private int shown = 0;

    public SongPickerScreen(Screen parent, Rule rule, int viewIndex, String filter, int page) {
        super(Component.literal("Pick songs"), parent);
        this.rule = rule;
        this.viewIndex = Math.floorMod(viewIndex, View.values().length);
        this.filter = filter.trim();
        this.requestedPage = page;
        this.typed = this.filter;
    }

    @Override
    protected void build() {
        view = View.values()[viewIndex];
        List<Row> rows = findRows();
        count = rows.size();

        int perPage = Math.max(1, (this.height - 58 - rowY(0)) / 22);
        pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        shown = Math.max(0, Math.min(requestedPage, pages - 1));

        // Which songs does the rule already have?
        Set<ResourceLocation> chosen = new HashSet<>();
        for (Track track : rule.tracks) {
            ResourceLocation id = RuleMatcher.parseSound(track.sound);
            if (id != null) chosen.add(id);
        }

        int x = left();
        for (int i = 0; i < perPage; i++) {
            int index = shown * perPage + i;
            if (index >= rows.size()) break;
            Row row = rows.get(index);
            ResourceLocation id = ResourceLocation.tryParse(row.id());
            if (id == null) continue;
            boolean selected = chosen.contains(id);
            int y = rowY(i);

            button((selected ? "[x] " : "[ ] ") + row.label(), x, y, 250, b -> {
                if (selected) {
                    rule.tracks.removeIf(track -> id.equals(RuleMatcher.parseSound(track.sound)));
                } else {
                    // "Add rule" starts a rule with the standard "any Overworld song" entry. If you have not touched it,
                    // your first pick replaces it, so the rule plays only the song you chose.
                    if (rule.tracks.size() == 1 && isStarterTrack(rule.tracks.get(0))) rule.tracks.clear();
                    rule.tracks.add(new Track(row.id(), 1));
                }
                reopen(viewIndex, shown);
            });
            button("Play", x + 254, y, 46, b -> preview(id, row.label()));
        }

        int rowA = this.height - 52;
        int rowB = this.height - 28;

        button(view.label, x, rowA, 100, b -> reopen(viewIndex + 1, 0));
        EditBox search = box(x + 102, rowA, 118, typed, 60, null, text -> typed = text);
        search.setHint(Component.literal("search"));
        button("Search", x + 222, rowA, 78, b -> reopen(viewIndex, 0));

        Button prev = button("< Prev", x, rowB, 72, b -> reopen(viewIndex, shown - 1));
        prev.active = shown > 0;
        Button next = button("Next >", x + 76, rowB, 72, b -> reopen(viewIndex, shown + 1));
        next.active = shown < pages - 1;
        button("Stop", x + 152, rowB, 72, b -> MusicController.cutCurrent(this.minecraft));
        button("Done", x + 228, rowB, 72, b -> this.onClose());
    }

    /** The entry a brand-new rule starts with (see the Track class): plain minecraft:music.game at weight 1. */
    private static boolean isStarterTrack(Track track) {
        return "minecraft:music.game".equals(track.sound) && track.weight == 1;
    }

    /** Opens this screen again with new settings. Whatever is typed in the search box is applied too. */
    private void reopen(int newView, int page) {
        String search = typed.trim();
        int newPage = search.equals(filter) ? page : 0; // a new search starts on the first page
        open(new SongPickerScreen(parent, rule, newView, search, newPage));
    }

    /** The rows for the chosen group, filtered by the search text and sorted by title. */
    private List<Row> findRows() {
        List<Row> rows = new ArrayList<>();

        if (view == View.DISCS || view == View.RANDOM_SETS || view == View.PACKS) {
            for (ResourceLocation id : this.minecraft.getSoundManager().getAvailableSounds()) {
                String namespace = id.getNamespace();
                String path = id.getPath();
                if (view == View.DISCS) {
                    if (namespace.equals("minecraft") && path.startsWith("music_disc.")) {
                        rows.add(new Row(id.toString(), "Disc: " + path.substring("music_disc.".length())));
                    }
                } else if (view == View.RANDOM_SETS) {
                    if (namespace.equals("minecraft") && path.startsWith("music.")) {
                        rows.add(new Row(id.toString(), path));
                    }
                } else if (!namespace.equals("minecraft") && !namespace.equals(SongCatalog.NAMESPACE)) {
                    rows.add(new Row(id.toString(), id.toString()));
                }
            }
        } else {
            for (SongCatalog.Song song : SongCatalog.all()) {
                if (view.group != null && song.group() != view.group) continue;
                ResourceLocation id = ResourceLocation.tryParse(song.id());
                // Only list songs that Minecraft actually loaded.
                if (id != null && this.minecraft.getSoundManager().getSoundEvent(id) != null) {
                    rows.add(new Row(song.id(), song.title()));
                }
            }
        }

        rows.removeIf(row -> !SongCatalog.matchesSearch(filter, row.label(), row.id()));
        rows.sort(Comparator.comparing(Row::label, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private void preview(ResourceLocation id, String label) {
        status = MusicController.play(this.minecraft, id, null, false)
                ? "Playing " + label
                : MusicController.getLastProblem();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (!status.isEmpty()) {
            centeredText(graphics, status, 24, 0xFFD080);
        } else {
            String what = filter.isEmpty() ? view.label : view.label + " matching \"" + filter + "\"";
            String unit = view == View.RANDOM_SETS ? " random sets" : view == View.PACKS ? " sounds" : " songs";
            centeredText(graphics, what + ": " + count + unit + ", page " + (shown + 1) + " of " + pages,
                    24, 0xA0A0A0);
        }

        if (count == 0) {
            boolean ourList = view != View.DISCS && view != View.RANDOM_SETS && view != View.PACKS;
            centeredText(graphics, ourList && filter.isEmpty()
                    ? "The song list did not load. Try restarting the game."
                    : "Nothing found.", rowY(1), 0xA0A0A0);
        }
    }
}
