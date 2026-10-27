package com.tunecontrol.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.tunecontrol.TuneControl;
import com.tunecontrol.client.gui.TuneConfigScreen;
import com.tunecontrol.config.TuneConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "Events" are Forge's way of telling a mod that something happened.
 * Each @SubscribeEvent method below is called by Forge when its event occurs.
 * The annotation on the class makes Forge register everything automatically, on the client only.
 */
@Mod.EventBusSubscriber(modid = TuneControl.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    private static boolean tickFailed = false;
    private static Screen pendingScreen;

    /**
     * Opens a screen on the next tick. (A chat command cannot open a screen directly: the chat window
     * closes itself right after the command runs and would close the new screen too.)
     */
    public static void openScreenLater(Screen screen) {
        pendingScreen = screen;
    }

    /** Runs 20 times per second. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pendingScreen != null) {
            Screen screen = pendingScreen;
            pendingScreen = null;
            Minecraft.getInstance().setScreen(screen);
        }
        if (tickFailed) return;
        try {
            MusicController.tick(Minecraft.getInstance());
        } catch (RuntimeException e) {
            // Never crash the game over music: give control back to vanilla and say what happened.
            tickFailed = true;
            MusicController.releaseVanilla();
            TuneControl.LOGGER.error("Tune Control hit an error and switched itself off until /tunecontrol reload.", e);
        }
    }

    /**
     * Forge calls this just before ANY sound starts playing.
     * While a rule is in charge we cancel vanilla's background music here (setting the sound to null cancels it),
     * but let through the songs this mod starts itself.
     */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;
        if (MusicController.shouldBlock(sound)) {
            event.setSound(null);
        } else if (sound.getSource() == SoundSource.MUSIC) {
            // A normal Minecraft song is starting. Note it, so /tunecontrol previous can go back to it
            // and the Now Playing toast can announce it.
            MusicController.noteVanilla(sound);
        }
    }

    /** Adds the /tunecontrol commands (they only exist on your own computer). */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tunecontrol")
                        .then(Commands.literal("reload").executes(ctx -> {
                            TuneConfig.load();
                            tickFailed = false;
                            if (TuneConfig.getLastError().isEmpty()) {
                                MusicController.say("Tune Control: config reloaded (" + TuneConfig.get().rules.size() + " rules).");
                            } else {
                                MusicController.say("Tune Control: " + TuneConfig.getLastError());
                                MusicController.say("The previous settings are still active. Fix the file and try again.");
                            }
                            return 1;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            for (String line : MusicController.statusLines(Minecraft.getInstance())) {
                                MusicController.say(line);
                            }
                            return 1;
                        }))
                        .then(Commands.literal("stop").executes(ctx -> {
                            MusicController.say(MusicController.stop(Minecraft.getInstance()));
                            return 1;
                        }))
                        .then(Commands.literal("start").executes(ctx -> {
                            MusicController.say(MusicController.start(Minecraft.getInstance()));
                            return 1;
                        }))
                        .then(Commands.literal("skip").executes(ctx -> {
                            MusicController.say(MusicController.skip(Minecraft.getInstance()));
                            return 1;
                        }))
                        .then(Commands.literal("previous").executes(ctx -> {
                            MusicController.say(MusicController.previous(Minecraft.getInstance()));
                            return 1;
                        }))
                        .then(Commands.literal("config").executes(ctx -> {
                            openScreenLater(new TuneConfigScreen(null));
                            return 1;
                        }))
                        .then(Commands.literal("test").then(
                                Commands.argument("sound", StringArgumentType.greedyString()).executes(ctx -> {
                                    String text = StringArgumentType.getString(ctx, "sound").trim();
                                    ResourceLocation id = RuleMatcher.parseSound(text);
                                    if (MusicController.play(Minecraft.getInstance(), id, null)) {
                                        MusicController.say("Tune Control: playing " + MusicController.lastPlayedName());
                                    } else {
                                        MusicController.say("Tune Control: could not play '" + text + "'. " + MusicController.getLastProblem());
                                    }
                                    return 1;
                                })))
        );
    }
}
