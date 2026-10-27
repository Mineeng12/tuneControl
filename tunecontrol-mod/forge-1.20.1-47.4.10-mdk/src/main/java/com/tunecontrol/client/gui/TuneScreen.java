package com.tunecontrol.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The shared base of all Tune Control screens: a title, a "parent" screen to go back to,
 * and small helpers for adding buttons, text boxes and labels in a neat 300-pixel-wide column.
 */
public abstract class TuneScreen extends Screen {
    /** Typing filters for number boxes. */
    protected static final Predicate<String> DECIMAL = text -> text.matches("[0-9]*\\.?[0-9]*");
    protected static final Predicate<String> INTEGER = text -> text.matches("-?[0-9]*");

    private record Label(String text, int x, int y, int color) {
    }

    /** The screen that opened this one (null means "back to the game"). */
    protected final Screen parent;

    private final List<Label> labels = new ArrayList<>();
    private final List<EditBox> boxes = new ArrayList<>();

    protected TuneScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** Create all buttons, boxes and labels here. It runs again whenever the window is resized. */
    protected abstract void build();

    @Override
    protected void init() {
        labels.clear();
        boxes.clear();
        build();
    }

    /** Left edge of the 300-pixel column. */
    protected int left() {
        return this.width / 2 - 150;
    }

    /** Top of row number "row" (rows are 22 pixels apart). */
    protected int rowY(int row) {
        return 36 + row * 22;
    }

    protected void open(Screen screen) {
        this.minecraft.setScreen(screen);
    }

    protected Button button(String text, int x, int y, int width, Button.OnPress onPress) {
        return this.addRenderableWidget(Button.builder(Component.literal(text), onPress).bounds(x, y, width, 20).build());
    }

    /** A text box. "onChange" (optional) is called every time the text changes. */
    protected EditBox box(int x, int y, int width, String value, int maxLength,
                          Predicate<String> filter, Consumer<String> onChange) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.empty());
        box.setMaxLength(maxLength);               // must come before setValue, which cuts the text to this length
        if (filter != null) box.setFilter(filter);
        box.setValue(value);
        if (onChange != null) box.setResponder(onChange); // after setValue, so filling the box does not count as a change
        boxes.add(box);
        return this.addRenderableWidget(box);
    }

    /** A text box with a label on its left. */
    protected EditBox labeledBox(String label, int y, String value, int maxLength,
                                 Predicate<String> filter, Consumer<String> onChange) {
        labels.add(new Label(label, left(), y + 6, 0xE0E0E0));
        return box(left() + 130, y, 170, value, maxLength, filter, onChange);
    }

    /** A piece of white text at an exact spot (for example the word "to" between two boxes). */
    protected void label(String text, int x, int y) {
        labels.add(new Label(text, x, y, 0xE0E0E0));
    }

    /** A line of grey help text. It is skipped when there is no room left above the bottom buttons. */
    protected void note(String text, int y) {
        if (y + 9 > this.height - 30) return;
        labels.add(new Label(text, left(), y, 0xA0A0A0));
    }

    protected void centeredText(GuiGraphics graphics, String text, int y, int color) {
        graphics.drawCenteredString(this.font, text, this.width / 2, y, color);
    }

    @Override
    public void tick() {
        super.tick();
        for (EditBox box : boxes) box.tick(); // makes the text cursor blink
    }

    /** Escape and the Back/Done buttons go back to the parent screen. */
    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    /** Keep the game running behind the screen, so you can hear songs while you edit. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        for (Label label : labels) {
            graphics.drawString(this.font, label.text(), label.x(), label.y(), label.color());
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
