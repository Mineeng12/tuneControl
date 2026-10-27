package com.tunecontrol.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

/** A slider from 0 to 100 percent. */
public class PercentSlider extends AbstractSliderButton {
    private final DoubleConsumer onChange;

    public PercentSlider(int x, int y, int width, int height, double percent, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty(), Math.max(0.0, Math.min(100.0, percent)) / 100.0);
        this.onChange = onChange;
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal("Chance of playing: " + Math.round(this.value * 100.0) + "%"));
    }

    @Override
    protected void applyValue() {
        this.onChange.accept(Math.round(this.value * 100.0));
    }
}
