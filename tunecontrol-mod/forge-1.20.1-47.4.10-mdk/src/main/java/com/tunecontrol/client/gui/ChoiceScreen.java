package com.tunecontrol.client.gui;

import com.tunecontrol.client.Names;
import com.tunecontrol.client.SongCatalog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * A pick list: the stand-in for a drop-down menu (Minecraft has no drop-down widget of its own).
 * Click a row to tick or untick it. Type in the search box and the list shrinks as you type,
 * like the suggestions under the chat box. Enter ticks the top row. The mouse wheel turns the pages.
 *
 * It edits the rule's own list directly, so there is nothing to save here: Done just goes back.
 * An empty list means "anywhere".
 */
public class ChoiceScreen extends TuneScreen {
    /** One thing you can pick: the text that is saved ("minecraft:plains") and the text that is shown ("Plains"). */
    public record Choice(String id, String label) {
    }

    /** A searchable entry: a Choice plus its lowercase "search keys", worked out once so typing stays fast. */
    private record Entry(Choice choice, String labelKey, String idKey) {
        static Entry of(Choice choice) {
            return new Entry(choice, SongCatalog.normalize(choice.label()), SongCatalog.normalize(choice.id()));
        }
    }

    /** One line of the list. choice == null means "the typed text can not be an ID". addNew means "add the typed text". */
    private record Row(Choice choice, boolean addNew) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<String> target;            // the rule's own list
    private final UnaryOperator<String> canonical; // makes two spellings of the same thing equal
    private final boolean allowCustom;             // may the player add values that are not in the list?
    private final Choice pinned;                   // optional first line while the search box is empty ("Right now: ...")

    // Everything that changes lives in fields, so a window resize rebuilds the screen without losing your place.
    private String query = "";
    private int page = 0;
    private int perPage = 1;
    private int pages = 1;
    private int matches = 0;
    private String chosenText = "";

    private final List<Button> rowButtons = new ArrayList<>();
    private Row[] shown = new Row[0];
    private Button prev;
    private Button next;

    public ChoiceScreen(Screen parent, String title, List<Choice> choices, List<String> target,
                        UnaryOperator<String> canonical, boolean allowCustom, Choice pinned) {
        super(Component.literal(title), parent);
        this.target = target;
        this.canonical = canonical;
        this.allowCustom = allowCustom;
        this.pinned = pinned;
        for (Choice choice : choices) entries.add(Entry.of(choice));
        // Entries that are already in the rule but not in the list (typed in the file, or from a mod) still show up,
        // so you can untick them.
        for (String entry : target) {
            if (!canonical.apply(entry).isEmpty() && !hasEntry(entry)) {
                addEntry(new Choice(entry, Names.pretty(entry)));
            }
        }
    }

    @Override
    protected void build() {
        rowButtons.clear();
        int x = left();

        perPage = Math.max(1, (this.height - 34 - rowY(1)) / 22);

        EditBox search = box(x, rowY(0), 300, query, 60, null, text -> {
            query = text;
            page = 0;
            refresh();
        });
        search.setHint(Component.literal("type to search"));
        this.setInitialFocus(search); // so you can start typing straight away

        for (int i = 0; i < perPage; i++) {
            final int slot = i;
            rowButtons.add(button("", x, rowY(1) + i * 22, 300, b -> pick(slot)));
        }

        int y = this.height - 28;
        prev = button("< Prev", x, y, 66, b -> turnPage(-1));
        next = button("Next >", x + 70, y, 66, b -> turnPage(1));
        button("Clear all", x + 140, y, 78, b -> {
            target.clear();
            refresh();
        });
        button("Done", x + 222, y, 78, b -> this.onClose());

        refresh();
    }

    // ------------------------------------------------------------------
    // The list
    // ------------------------------------------------------------------

    /** Works out which rows match the search box and puts them on the buttons. */
    private void refresh() {
        List<Row> rows = findRows();
        matches = rows.size();
        pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));

        shown = new Row[perPage];
        for (int i = 0; i < perPage; i++) {
            Button button = rowButtons.get(i);
            int index = page * perPage + i;
            if (index < rows.size()) {
                Row row = rows.get(index);
                shown[i] = row;
                button.visible = true;
                button.active = row.choice() != null;
                button.setMessage(Component.literal(rowText(row)));
            } else {
                shown[i] = null;
                button.visible = false;
            }
        }
        prev.active = page > 0;
        next.active = page < pages - 1;

        List<String> names = new ArrayList<>();
        for (String entry : target) names.add(labelOf(entry));
        chosenText = target.isEmpty() ? "Nothing chosen = anywhere" : "Chosen: " + Names.summary(names, 46);
    }

    /** The entries matching the search text. Names that START with the typed text come first. */
    private List<Row> findRows() {
        String typed = query.trim();
        List<Row> rows = new ArrayList<>();

        if (typed.isEmpty()) {
            if (pinned != null) rows.add(new Row(pinned, false));
            for (Entry entry : entries) rows.add(new Row(entry.choice(), false));
            return rows;
        }

        String wanted = SongCatalog.normalize(typed);
        List<Row> later = new ArrayList<>();
        for (Entry entry : entries) {
            boolean startsWith = entry.labelKey().startsWith(wanted) || entry.idKey().startsWith(wanted);
            boolean contains = startsWith || entry.labelKey().contains(wanted) || entry.idKey().contains(wanted);
            if (startsWith) rows.add(new Row(entry.choice(), false));
            else if (contains) later.add(new Row(entry.choice(), false));
        }
        rows.addAll(later);

        // Nothing matches: offer to add what was typed (for IDs from mods, maps and data packs).
        if (rows.isEmpty() && allowCustom) {
            String id = typed.toLowerCase(Locale.ROOT);
            rows.add(Names.looksLikeId(id) ? new Row(new Choice(id, Names.pretty(id)), true) : new Row(null, false));
        }
        return rows;
    }

    private String rowText(Row row) {
        if (row.choice() == null) return "Not a valid ID (lowercase letters, numbers and _ : / # only)";
        if (row.addNew()) return "Add \"" + row.choice().id() + "\"";
        return (isTicked(row.choice().id()) ? "[x] " : "[ ] ") + row.choice().label();
    }

    private boolean isTicked(String id) {
        String key = canonical.apply(id);
        for (String entry : target) {
            if (canonical.apply(entry).equals(key)) return true;
        }
        return false;
    }

    private boolean hasEntry(String id) {
        String key = canonical.apply(id);
        for (Entry entry : entries) {
            if (canonical.apply(entry.choice().id()).equals(key)) return true;
        }
        return false;
    }

    private void addEntry(Choice choice) {
        entries.add(Entry.of(choice));
    }

    /** The shown name for something in the rule's list. */
    private String labelOf(String id) {
        String key = canonical.apply(id);
        for (Entry entry : entries) {
            if (canonical.apply(entry.choice().id()).equals(key)) return entry.choice().label();
        }
        return Names.pretty(id);
    }

    // ------------------------------------------------------------------
    // Clicks and keys
    // ------------------------------------------------------------------

    /** Ticks or unticks the row on button number "slot". */
    private void pick(int slot) {
        if (slot < 0 || slot >= shown.length || shown[slot] == null) return;
        Row row = shown[slot];
        if (row.choice() == null) return;

        String id = row.choice().id();
        String key = canonical.apply(id);
        if (row.addNew()) {
            if (!isTicked(id)) target.add(id);
            if (!hasEntry(id)) addEntry(row.choice()); // from now on it is a normal row you can untick
        } else if (!target.removeIf(entry -> canonical.apply(entry).equals(key))) {
            target.add(id);
        }
        refresh();
    }

    private void turnPage(int direction) {
        page += direction;
        refresh();
    }

    /** Enter ticks the top row of the search results, like accepting the first suggestion. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == 257 || keyCode == 335; // Enter and the keypad Enter
        if (enter && !query.trim().isEmpty() && shown.length > 0 && shown[0] != null) {
            pick(0);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** The mouse wheel turns the pages. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && page > 0) {
            turnPage(-1);
        } else if (delta < 0 && page < pages - 1) {
            turnPage(1);
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        centeredText(graphics, chosenText, 24, 0xA0A0A0);
        String info = matches + " found, page " + (page + 1) + "/" + pages;
        graphics.drawString(this.font, info, left() + 300 - this.font.width(info), 12, 0x808080);
        if (matches == 0) centeredText(graphics, "Nothing found.", rowY(1) + 6, 0xA0A0A0);
    }
}
