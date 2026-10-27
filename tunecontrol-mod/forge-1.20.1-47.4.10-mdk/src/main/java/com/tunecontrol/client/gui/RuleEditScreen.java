package com.tunecontrol.client.gui;

import com.tunecontrol.client.Names;
import com.tunecontrol.client.RuleMatcher;
import com.tunecontrol.config.Rule;
import com.tunecontrol.config.Track;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Edits one rule. Three tabs keep everything on screen:
 * Basics (name, on/off, chance, timing), Where (time, height, biomes...) and Songs.
 * Every change is written straight into the rule; it is only saved to disk
 * when you press Save & Close on the main screen.
 */
public class RuleEditScreen extends TuneScreen {
    private static final String[] TAB_NAMES = {"Basics", "Where", "Songs"};

    private final Rule rule;
    private final int tab;

    public RuleEditScreen(Screen parent, Rule rule, int tab) {
        super(Component.literal("Edit rule"), parent);
        this.rule = rule;
        this.tab = tab;
    }

    /** The tabs take the first row, so everything starts a little lower than on other screens. */
    @Override
    protected int rowY(int row) {
        return 54 + row * 22;
    }

    @Override
    protected void build() {
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int target = i;
            Button tabButton = button(TAB_NAMES[i], left() + i * 101, 26, 98,
                    b -> open(new RuleEditScreen(parent, rule, target)));
            tabButton.active = (i != tab); // the tab you are on is greyed out
        }

        switch (tab) {
            case 1 -> buildWhere();
            case 2 -> buildSongs();
            default -> buildBasics();
        }

        button("Done", left(), this.height - 28, 300, b -> this.onClose());
    }

    // ------------------------------------------------------------------
    // Tab 1: Basics
    // ------------------------------------------------------------------

    private void buildBasics() {
        int x = left();

        labeledBox("Name", rowY(0), rule.name, 60, null, text -> rule.name = text);

        this.addRenderableWidget(CycleButton.onOffBuilder(rule.enabled)
                .create(x, rowY(1), 300, 20, Component.literal("Rule enabled"),
                        (button, value) -> rule.enabled = value));

        this.addRenderableWidget(new PercentSlider(x, rowY(2), 300, 20, rule.chance_percent,
                percent -> rule.chance_percent = percent));

        labeledBox("Wait at least (s)", rowY(3), blank(rule.min_delay_seconds), 8, DECIMAL,
                text -> rule.min_delay_seconds = optional(text)).setHint(Component.literal("default"));
        labeledBox("Wait at most (s)", rowY(4), blank(rule.max_delay_seconds), 8, DECIMAL,
                text -> rule.max_delay_seconds = optional(text)).setHint(Component.literal("default"));

        this.addRenderableWidget(CycleButton.onOffBuilder(rule.stop_when_leaving)
                .create(x, rowY(5), 300, 20, Component.literal("Stop song when you leave"),
                        (button, value) -> rule.stop_when_leaving = value));

        note("The game waits a random time between songs.", rowY(6));
        note("Blank = the default from the main screen.", rowY(6) + 11);
    }

    // ------------------------------------------------------------------
    // Tab 2: Where
    // ------------------------------------------------------------------

    private void buildWhere() {
        int x = left();

        List<String> timeValues = List.of("any", "day", "night");
        this.addRenderableWidget(CycleButton.<String>builder(
                        value -> Component.literal(value.substring(0, 1).toUpperCase() + value.substring(1)))
                .withValues(timeValues)
                .withInitialValue(timeValues.contains(rule.time) ? rule.time : "any")
                .create(x, rowY(0), 300, 20, Component.literal("Time of day"),
                        (button, value) -> rule.time = value));

        // Height: lowest and highest on one line.
        label("Height (Y)", x, rowY(1) + 6);
        box(x + 130, rowY(1), 78, Integer.toString(rule.min_y), 6, INTEGER,
                text -> rule.min_y = GuiText.parseInt(text, rule.min_y));
        label("to", x + 212, rowY(1) + 6);
        box(x + 226, rowY(1), 74, Integer.toString(rule.max_y), 6, INTEGER,
                text -> rule.max_y = GuiText.parseInt(text, rule.max_y));

        // Each of these opens a pick list (see ChoiceScreen). Nothing ticked = anywhere.
        chooser("Situations", rowY(2), "Pick situations", rule.situations,
                Choices::situations, Names::canonical, Choices::situationLabel, false, null);
        chooser("Dimensions", rowY(3), "Pick dimensions", rule.dimensions,
                () -> Choices.dimensions(this.minecraft), Names::dimensionId,
                entry -> Names.pretty(Names.dimensionId(entry)), true, null);
        chooser("Biomes", rowY(4), "Pick biomes", rule.biomes,
                Choices::biomes, Names::canonical, Names::pretty, true,
                () -> Choices.currentBiome(this.minecraft));
        chooser("Standing on", rowY(5), "Pick blocks to stand on", rule.blocks,
                Choices::blocks, Names::canonical, Names::pretty, true,
                () -> Choices.currentBlock(this.minecraft));

        // The most useful lines come first: small windows only have room for the first two.
        note("Pick as many as you like. None picked = anywhere.", rowY(6));
        note("Standing on = the block under your feet (for map makers).", rowY(6) + 11);
        note("An ID that is not listed? Type it in the search box.", rowY(6) + 22);
    }

    /**
     * A button that shows what is picked ("Biomes: Plains, Forest +2 more") and opens the pick list when pressed.
     * The pick list edits "target" (the rule's own list) directly.
     */
    private void chooser(String label, int y, String title, List<String> target,
                         Supplier<List<ChoiceScreen.Choice>> choices, UnaryOperator<String> canonical,
                         Function<String, String> labelOf, boolean allowCustom,
                         Supplier<ChoiceScreen.Choice> pinned) {
        List<String> names = new ArrayList<>();
        for (String entry : target) names.add(labelOf.apply(entry));
        button(label + ": " + Names.summary(names, 26), left(), y, 300, b ->
                open(new ChoiceScreen(this, title, choices.get(), target, canonical, allowCustom,
                        pinned == null ? null : pinned.get())));
    }

    // ------------------------------------------------------------------
    // Tab 3: Songs
    // ------------------------------------------------------------------

    private void buildSongs() {
        int x = left();

        note("Songs by title, separated by commas (empty = silence).", rowY(0));
        note("Add *3 after a song to make it 3x as likely.", rowY(0) + 11);

        EditBox songs = box(x, rowY(1), 300, GuiText.tracksToText(rule.tracks), 2000, null, null);
        songs.setHint(Component.literal("Mice on Venus, Sweden*2"));
        songs.setTextColor(allKnown(rule.tracks) ? 0xE0E0E0 : 0xFF6060);
        songs.setResponder(text -> {
            rule.tracks = GuiText.parseTracks(text);
            songs.setTextColor(allKnown(rule.tracks) ? 0xE0E0E0 : 0xFF6060);
        });

        button("Pick songs from a list...", x, rowY(2), 300, b -> open(new SongPickerScreen(this, rule, 0, "", 0)));

        note("Red text = Minecraft does not know that song.", rowY(3));
    }

    /** True when Minecraft knows every song in the list (so none of them would be silent). */
    private boolean allKnown(List<Track> tracks) {
        for (Track track : tracks) {
            ResourceLocation id = RuleMatcher.parseSound(track.sound);
            if (id == null || this.minecraft.getSoundManager().getSoundEvent(id) == null) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    /** A missing delay shows as an empty box (and means "use the default"). */
    private static String blank(Double value) {
        return value == null ? "" : GuiText.number(value);
    }

    private static Double optional(String text) {
        if (text.isEmpty()) return null;
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
