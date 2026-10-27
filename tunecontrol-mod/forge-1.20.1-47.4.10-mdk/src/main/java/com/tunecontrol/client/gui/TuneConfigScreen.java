package com.tunecontrol.client.gui;

import com.tunecontrol.client.MusicController;
import com.tunecontrol.config.TuneConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The main settings screen. Open it with Mods > Tune Control > Config, or with /tunecontrol config.
 * It edits a COPY of your settings: nothing changes until you press Save & Close.
 */
public class TuneConfigScreen extends TuneScreen {
    private final TuneConfig working = TuneConfig.copyOfCurrent();
    private String status = "";

    public TuneConfigScreen(Screen parent) {
        super(Component.literal("Tune Control"), parent);
    }

    @Override
    protected void build() {
        int x = left();

        // Master switch, and the "Now Playing" pop-up
        this.addRenderableWidget(CycleButton.onOffBuilder(working.enabled)
                .create(x, rowY(0), 148, 20, Component.literal("Tune Control"),
                        (button, value) -> working.enabled = value));
        this.addRenderableWidget(CycleButton.onOffBuilder(working.show_music_toast)
                .create(x + 152, rowY(0), 148, 20, Component.literal("Music toast"),
                        (button, value) -> working.show_music_toast = value));

        // What happens where no rule applies
        this.addRenderableWidget(CycleButton.<String>builder(
                        value -> Component.literal(value.equals("silence") ? "Silence" : "Normal Minecraft music"))
                .withValues(List.of("vanilla", "silence"))
                .withInitialValue(working.silenceWhenNoRuleMatches() ? "silence" : "vanilla")
                .create(x, rowY(1), 300, 20, Component.literal("Where no rule matches"),
                        (button, value) -> working.when_no_rule_matches = value));

        // Timing defaults
        labeledBox("First song delay (s)", rowY(2), GuiText.number(working.first_song_delay_seconds), 8, DECIMAL,
                text -> working.first_song_delay_seconds = GuiText.parseDouble(text, working.first_song_delay_seconds));
        labeledBox("Default min delay (s)", rowY(3), GuiText.number(working.default_min_delay_seconds), 8, DECIMAL,
                text -> working.default_min_delay_seconds = GuiText.parseDouble(text, working.default_min_delay_seconds));
        labeledBox("Default max delay (s)", rowY(4), GuiText.number(working.default_max_delay_seconds), 8, DECIMAL,
                text -> working.default_max_delay_seconds = GuiText.parseDouble(text, working.default_max_delay_seconds));

        // The rules
        button("Edit rules (" + working.rules.size() + ")...", x, rowY(5), 300,
                b -> open(new RuleListScreen(this, working, 0)));

        // Playback controls. These act right away and use your SAVED settings.
        button("Previous", x, rowY(6), 98, b -> status = MusicController.previous(this.minecraft));
        button(MusicController.isPaused() ? "Start" : "Stop", x + 101, rowY(6), 98, b -> {
            status = MusicController.isPaused()
                    ? MusicController.start(this.minecraft)
                    : MusicController.stop(this.minecraft);
            b.setMessage(Component.literal(MusicController.isPaused() ? "Start" : "Stop"));
        });
        button("Skip", x + 202, rowY(6), 98, b -> status = MusicController.skip(this.minecraft));

        // Footer
        button("Save & Close", x, this.height - 28, 148, b -> {
            String error = TuneConfig.saveAndApply(working);
            if (error.isEmpty()) {
                this.onClose();
            } else {
                status = error;
            }
        });
        button("Cancel", x + 152, this.height - 28, 148, b -> this.onClose());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (status.isEmpty()) {
            centeredText(graphics, MusicController.nowPlayingText(this.minecraft), 24, 0xA0A0A0);
        } else {
            centeredText(graphics, status, 24, 0xFFD080);
        }
    }
}
