package com.tunecontrol.client.gui;

import com.tunecontrol.config.Rule;
import com.tunecontrol.config.Track;
import com.tunecontrol.config.TuneConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Collections;

/**
 * The list of rules, a page at a time. Each row: [rule name] [On/Off] [up] [down] [delete].
 * Rules are checked from the top and the first one that matches wins, so the order matters.
 */
public class RuleListScreen extends TuneScreen {
    private final TuneConfig working;
    private final int requestedPage;

    private int perPage = 1;
    private int pages = 1;
    private int shown = 0;

    public RuleListScreen(Screen parent, TuneConfig working, int page) {
        super(Component.literal("Rules"), parent);
        this.working = working;
        this.requestedPage = page;
    }

    @Override
    protected void build() {
        perPage = Math.max(1, (this.height - 58 - rowY(0)) / 22);
        pages = Math.max(1, (working.rules.size() + perPage - 1) / perPage);
        shown = Math.max(0, Math.min(requestedPage, pages - 1));

        int x = left();
        for (int i = 0; i < perPage; i++) {
            int index = shown * perPage + i;
            if (index >= working.rules.size()) break;
            Rule rule = working.rules.get(index);
            int y = rowY(i);

            // Name: opens the editor
            String name = rule.name + (rule.tracks.isEmpty() ? " (silence)" : "");
            button(name, x, y, 172, b -> open(new RuleEditScreen(this, rule, 0)));

            // On / Off
            button(rule.enabled ? "On" : "Off", x + 175, y, 38, b -> {
                rule.enabled = !rule.enabled;
                b.setMessage(Component.literal(rule.enabled ? "On" : "Off"));
            });

            // Move up / down
            Button up = button("^", x + 216, y, 20, b -> move(index, -1));
            up.active = index > 0;
            Button down = button("v", x + 239, y, 20, b -> move(index, 1));
            down.active = index < working.rules.size() - 1;

            // Delete (asks "Sure?" first)
            final boolean[] armed = {false};
            button("X", x + 262, y, 38, b -> {
                if (!armed[0]) {
                    armed[0] = true;
                    b.setMessage(Component.literal("Sure?"));
                    return;
                }
                working.rules.remove(rule);
                int lastPage = Math.max(0, (working.rules.size() - 1) / perPage);
                open(new RuleListScreen(parent, working, Math.min(shown, lastPage)));
            });
        }

        int rowA = this.height - 52;
        int rowB = this.height - 28;
        button("Add rule", x, rowA, 120, b -> addRule());
        Button prev = button("< Prev", x + 124, rowA, 84, b -> open(new RuleListScreen(parent, working, shown - 1)));
        prev.active = shown > 0;
        Button next = button("Next >", x + 212, rowA, 88, b -> open(new RuleListScreen(parent, working, shown + 1)));
        next.active = shown < pages - 1;
        button("Back", x, rowB, 300, b -> this.onClose());
    }

    /** Swaps a rule with its neighbour and shows the page it ended up on. */
    private void move(int index, int delta) {
        int other = index + delta;
        if (other < 0 || other >= working.rules.size()) return;
        Collections.swap(working.rules, index, other);
        open(new RuleListScreen(parent, working, other / perPage));
    }

    /** New rules go to the TOP so they take effect straight away (the first matching rule wins). */
    private void addRule() {
        Rule rule = new Rule();
        rule.name = "New rule";
        rule.tracks.add(new Track());
        working.rules.add(0, rule);
        RuleListScreen list = new RuleListScreen(parent, working, 0);
        open(new RuleEditScreen(list, rule, 0));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        centeredText(graphics, "The top rule that matches wins - page " + (shown + 1) + " of " + pages, 24, 0xA0A0A0);
        if (working.rules.isEmpty()) {
            centeredText(graphics, "No rules yet. Press \"Add rule\".", rowY(1), 0xA0A0A0);
        }
    }
}
