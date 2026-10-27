package com.tunecontrol.client;

import com.tunecontrol.TuneControl;
import com.tunecontrol.config.TuneConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/**
 * The "Now Playing" pop-up in the top-right corner of the screen, like the music toast in newer Minecraft versions.
 * It uses the same kind of toast the game uses for its own notices (it slides in, waits a few seconds and slides out).
 */
public final class SongToast {
    private SongToast() {
    }

    /**
     * Shows the toast for a song that has just been started.
     * Does nothing when the toast is switched off in the settings, or when the music volume is turned all the way down.
     */
    public static void show(Minecraft mc, SoundInstance instance, ResourceLocation eventId) {
        if (!TuneConfig.get().show_music_toast) return;
        // Nobody needs to be told about a song they have muted.
        if (mc.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F
                || mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0.0F) {
            return;
        }

        try {
            // Minecraft has already picked the real music file by now (a random one for sets like music.game).
            String file = null;
            Sound sound = instance.getSound();
            if (sound != null) {
                ResourceLocation location = sound.getLocation();
                if (location != null) file = location.toString();
            }
            if ("minecraft:empty".equals(file)) return; // an "empty" sound event means silence: nothing to announce

            SongInfo.Info info = SongInfo.describe(file, eventId == null ? null : eventId.toString());
            // One toast is reused, so skipping through songs quickly changes its text instead of piling up a queue.
            SystemToast.addOrUpdate(mc.getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                    Component.literal("Now Playing"), Component.literal(info.line()));
        } catch (RuntimeException e) {
            // A pop-up is never worth breaking the music for.
            TuneControl.LOGGER.warn("Tune Control: could not show the music toast", e);
        }
    }
}
