package com.tunecontrol.client;

import com.tunecontrol.TuneControl;
import com.tunecontrol.config.Rule;
import com.tunecontrol.config.Track;
import com.tunecontrol.config.TuneConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The brain of the mod. It runs once per game tick (20 times per second) and does this:
 *
 *   1. Work out where the player is (Context) and which rule applies (RuleMatcher).
 *   2. Count down a timer between songs.
 *   3. When the timer reaches zero, roll the dice using the rule's chance_percent.
 *      If the roll succeeds, pick a song (by weight) and play it. If not, wait another delay.
 *   4. While we are in charge, block vanilla's background music from starting (see ClientEvents.onPlaySound).
 */
public final class MusicController {
    private MusicController() {
    }

    private static final Random RANDOM = new Random();

    private static SoundInstance current;     // the song this mod is playing right now (null = none)
    private static Rule currentRule;           // the rule that started it
    private static ResourceLocation lastSoundId;      // the sound a rule asked for (used to avoid repeats)
    private static ResourceLocation lastPlayedId;     // the exact song that really played last (used for names)
    private static SoundInstance vanilla;             // a normal Minecraft song that is playing (not started by us)
    private static SoundInstance pendingVanilla;      // a normal song that just started; we still have to find out which one
    private static int pendingTicks;
    private static Rule lastMatched;
    private static int cooldownTicks;          // ticks left until the next "roll"
    private static boolean started;            // has the very first tick happened yet?
    private static boolean wasInWorld;
    private static boolean suppressVanilla;
    private static boolean paused;             // true after /tunecontrol stop: no songs at all until /tunecontrol start
    private static String lastProblem = "";

    // Every song that has played, oldest first (the ones this mod started AND normal Minecraft music).
    // The last entry is the current (or most recent) song. /tunecontrol previous walks backwards through this list.
    // Each entry is the EXACT song: for a random set such as minecraft:music.game it is the song that was picked
    // (for example tunecontrol:mice_on_venus), so going back replays that song and not another random one.
    private static final List<ResourceLocation> history = new ArrayList<>();
    private static final int HISTORY_LIMIT = 50;

    private static final String OFF_MESSAGE =
            "Tune Control is switched off (\"enabled\" is false in config/tunecontrol.json).";

    /**
     * True for a background-music sound that should be cancelled: we are in charge, it is in the
     * "music" category, and it is not a song this mod started itself.
     */
    public static boolean shouldBlock(SoundInstance sound) {
        return suppressVanilla && sound != current && sound.getSource() == SoundSource.MUSIC;
    }

    /** Called when something went wrong: hand control back to vanilla music. */
    public static void releaseVanilla() {
        suppressVanilla = false;
    }

    public static String getLastProblem() {
        return lastProblem;
    }

    // ------------------------------------------------------------------
    // The tick
    // ------------------------------------------------------------------

    public static void tick(Minecraft mc) {
        TuneConfig config = TuneConfig.get();
        if (!config.enabled) {
            suppressVanilla = false;
            return;
        }
        trackVanilla(mc);
        if (mc.level != null && mc.player == null) return; // a world is still loading

        if (!started) {
            started = true;
            cooldownTicks = seconds(config.first_song_delay_seconds);
        }

        boolean inWorld = mc.level != null;
        if (inWorld != wasInWorld) {
            // Joined or left a world: start fresh.
            wasInWorld = inWorld;
            if (current != null) mc.getSoundManager().stop(current);
            current = null;
            currentRule = null;
            lastMatched = null;
            cooldownTicks = seconds(config.first_song_delay_seconds);
        }

        Context ctx = Context.capture(mc);
        Rule matched = RuleMatcher.findFirst(config, ctx);
        boolean inCharge = matched != null || config.silenceWhenNoRuleMatches();

        // 1. Did our song just finish?
        if (current != null && !mc.getSoundManager().isActive(current)) {
            current = null;
            cooldownTicks = randomDelayTicks(matched != null ? matched : currentRule, config);
        }

        // 2. Did the player walk out of the area of a "stop_when_leaving" rule?
        if (current != null && currentRule != null && currentRule.stop_when_leaving && matched != currentRule) {
            mc.getSoundManager().stop(current);
            current = null;
            cooldownTicks = randomDelayTicks(matched != null ? matched : currentRule, config);
        }

        // 3. Entered a different rule: let its delay apply if it is shorter than what we were waiting for.
        if (matched != lastMatched) {
            if (matched != null) cooldownTicks = Math.min(cooldownTicks, randomDelayTicks(matched, config));
            lastMatched = matched;
        }

        // 4. Count down, then try to start a song. (Not while stopped with /tunecontrol stop.)
        if (current == null && matched != null && !paused) {
            if (cooldownTicks > 0) {
                cooldownTicks--;
            } else {
                tryStartSong(mc, matched, config);
            }
        }

        // Keep vanilla quiet while a rule applies, while one of our songs is still playing,
        // or while the music is stopped with /tunecontrol stop.
        suppressVanilla = inCharge || current != null || paused;
    }

    private static void tryStartSong(Minecraft mc, Rule rule, TuneConfig config) {
        // The dice roll. chance_percent 100 always passes, 0 never does.
        if (RANDOM.nextDouble() * 100.0 >= rule.chance_percent) {
            cooldownTicks = randomDelayTicks(rule, config);
            return;
        }

        Track track = pickTrack(rule);
        if (track == null) { // rule has no songs = silence
            cooldownTicks = randomDelayTicks(rule, config);
            return;
        }

        if (!play(mc, RuleMatcher.parseSound(track.sound), rule)) {
            cooldownTicks = randomDelayTicks(rule, config);
        }
    }

    // ------------------------------------------------------------------
    // Playing and picking songs
    // ------------------------------------------------------------------

    /** Starts a song right now and shows the "Now Playing" toast. Returns false (and remembers why) if it could not be played. */
    public static boolean play(Minecraft mc, ResourceLocation id, Rule rule) {
        return play(mc, id, rule, true);
    }

    /** Same, but "announce" = false skips the toast (the song picker uses that for its preview button). */
    public static boolean play(Minecraft mc, ResourceLocation id, Rule rule, boolean announce) {
        if (id == null) {
            problem("A sound ID in the config is not valid (use lowercase text like minecraft:music.game).");
            return false;
        }
        if (mc.getSoundManager().getSoundEvent(id) == null) {
            problem("Unknown sound '" + id + "'. Check the spelling, and that the resource pack that defines it is switched on.");
            return false;
        }

        // Cut off any vanilla background song that is still fading out, so two songs never overlap.
        mc.getSoundManager().stop(null, SoundSource.MUSIC);

        SoundInstance instance = SimpleSoundInstance.forMusic(SoundEvent.createVariableRangeEvent(id));

        // Remember it BEFORE playing, so ClientEvents.onPlaySound recognises it as ours and lets it through.
        current = instance;
        currentRule = rule;
        lastSoundId = id;
        vanilla = null;
        pendingVanilla = null;
        mc.getSoundManager().play(instance);

        // Minecraft has now picked the real music file. Remember THAT song, not just the set it came from.
        ResourceLocation exact = exactId(mc, instance, id);
        lastPlayedId = exact;
        remember(exact);
        if (announce) SongToast.show(mc, instance, id);
        return true;
    }

    /** Adds a song to the history that /tunecontrol previous walks through. */
    private static void remember(ResourceLocation id) {
        history.add(id);
        if (history.size() > HISTORY_LIMIT) history.remove(0);
    }

    /**
     * The sound ID that replays exactly the song this instance is playing. For a random set (minecraft:music.game)
     * Minecraft has picked one music file; when that file is one of our named songs, the answer is that song's
     * own ID (tunecontrol:mice_on_venus). Otherwise the requested ID is returned unchanged.
     */
    private static ResourceLocation exactId(Minecraft mc, SoundInstance instance, ResourceLocation requested) {
        Sound sound = instance.getSound();
        if (sound == null || sound.getLocation() == null) return requested;
        SongCatalog.Song song = SongCatalog.byFile(sound.getLocation().toString());
        if (song == null) return requested;
        ResourceLocation exact = ResourceLocation.tryParse(song.id());
        return exact != null && mc.getSoundManager().getSoundEvent(exact) != null ? exact : requested;
    }

    /** True while one of our songs, or a normal Minecraft song, is playing. */
    private static boolean isPlaying(Minecraft mc) {
        return (current != null && mc.getSoundManager().isActive(current))
                || (vanilla != null && mc.getSoundManager().isActive(vanilla));
    }

    /**
     * Called (from ClientEvents) when a normal Minecraft background song is about to start and has not been
     * cancelled. The real song is only known a moment later, so it is looked up on the next ticks (trackVanilla).
     */
    public static void noteVanilla(SoundInstance sound) {
        if (!TuneConfig.get().enabled || sound == current || sound == vanilla) return;
        pendingVanilla = sound;
        pendingTicks = 0;
    }

    /**
     * Finds out which song a normal Minecraft song turned out to be, adds it to the history (so /tunecontrol
     * previous can go back to it) and shows the Now Playing toast for it, like newer versions of Minecraft.
     */
    private static void trackVanilla(Minecraft mc) {
        if (pendingVanilla != null) {
            SoundInstance sound = pendingVanilla;
            Sound file = sound.getSound();
            if (file != null) {
                pendingVanilla = null;
                if (!isEmptySound(file)) {
                    vanilla = sound;
                    ResourceLocation requested = sound.getLocation();
                    ResourceLocation exact = requested == null ? null : exactId(mc, sound, requested);
                    if (exact != null) {
                        lastPlayedId = exact;
                        remember(exact);
                    }
                    SongToast.show(mc, sound, requested);
                }
            } else if (++pendingTicks > 40) {
                pendingVanilla = null; // it never really started (for example the sound did not load)
            }
        }
        if (vanilla != null && !mc.getSoundManager().isActive(vanilla)) vanilla = null;
    }

    private static boolean isEmptySound(Sound sound) {
        return sound.getLocation() != null && "minecraft:empty".equals(sound.getLocation().toString());
    }

    /**
     * /tunecontrol stop: cut the current song and stay quiet (no automatic songs, and no normal
     * Minecraft music either) until /tunecontrol start. Returns the message to show in chat.
     */
    public static String stop(Minecraft mc) {
        if (!TuneConfig.get().enabled) return OFF_MESSAGE;
        paused = true;
        mc.getSoundManager().stop(null, SoundSource.MUSIC);
        current = null;
        suppressVanilla = true; // take effect right away, not only on the next tick
        return "Tune Control: music stopped. It stays quiet until you type /tunecontrol start.";
    }

    /**
     * /tunecontrol start: leave quiet mode and play a song from the rule that applies where you are, right now.
     * It ignores the timer and the chance roll, because you asked for a song. Returns the message to show in chat.
     */
    public static String start(Minecraft mc) {
        TuneConfig config = TuneConfig.get();
        if (!config.enabled) return OFF_MESSAGE;

        paused = false; // automatic songs are back on, whatever happens below

        if (current != null && mc.getSoundManager().isActive(current)) {
            return "Tune Control: a song is already playing (" + nameOf(lastPlayedId) + "). Use /tunecontrol skip for another one.";
        }

        Rule rule = RuleMatcher.findFirst(config, Context.capture(mc));
        if (rule == null) {
            return "Tune Control: automatic songs are on again, but no rule applies where you are, "
                    + "so there is no song to start. (Try /tunecontrol status.)";
        }

        Track track = pickTrack(rule);
        if (track == null) {
            return "Tune Control: the rule '" + rule.name + "' has no tracks (it means silence here), so there is no song to start.";
        }

        ResourceLocation id = RuleMatcher.parseSound(track.sound);
        if (!play(mc, id, rule)) {
            return "Tune Control: could not play '" + track.sound + "'. " + lastProblem;
        }
        return "Tune Control: started " + lastPlayedName() + " (rule: " + rule.name + ").";
    }

    /**
     * /tunecontrol previous: go back one song. While a song is playing, this plays the song that came before it.
     * Between songs (or after /tunecontrol stop) it replays the most recent song. Pressing it again keeps going back.
     * Returns the message to show in chat.
     */
    public static String previous(Minecraft mc) {
        TuneConfig config = TuneConfig.get();
        if (!config.enabled) return OFF_MESSAGE;

        boolean playing = isPlaying(mc);
        int target = playing ? history.size() - 2 : history.size() - 1;
        if (target < 0) {
            return playing
                    ? "Tune Control: this is the first song, so there is no earlier one to go back to."
                    : "Tune Control: no song has played yet, so there is nothing to go back to.";
        }

        ResourceLocation id = history.get(target);
        // Forget that song and everything after it. play() adds it again, so pressing "previous" repeatedly keeps walking back.
        while (history.size() > target) history.remove(history.size() - 1);

        paused = false; // you asked for a song, so automatic songs are on again
        Rule rule = RuleMatcher.findFirst(config, Context.capture(mc));
        if (!play(mc, id, rule)) {
            return "Tune Control: could not play '" + nameOf(id) + "'. " + lastProblem;
        }
        return "Tune Control: going back to " + nameOf(id) + ".";
    }

    /** Cuts off whatever music is playing, without switching automatic songs off (used by the song picker's Stop button). */
    public static void cutCurrent(Minecraft mc) {
        mc.getSoundManager().stop(null, SoundSource.MUSIC);
        // Wait a normal delay before the next automatic song, so pressing Stop is not undone a moment later.
        if (current != null) cooldownTicks = randomDelayTicks(currentRule, TuneConfig.get());
        current = null;
    }

    /** True after /tunecontrol stop, until /tunecontrol start. */
    public static boolean isPaused() {
        return paused;
    }

    /** One line saying what is playing, for the settings screen. */
    public static String nowPlayingText(Minecraft mc) {
        if (paused) return "Music is stopped.";
        return isPlaying(mc) ? "Now playing: " + nameOf(lastPlayedId) : "Nothing is playing right now.";
    }

    /** Stops the current song and rolls the dice for the next one straight away. Returns the chat message. */
    public static String skip(Minecraft mc) {
        if (!TuneConfig.get().enabled) return OFF_MESSAGE;
        if (paused) return "Tune Control: music is stopped. Type /tunecontrol start to play a song.";
        mc.getSoundManager().stop(null, SoundSource.MUSIC);
        current = null;
        cooldownTicks = 0;
        return "Tune Control: song stopped, rolling for the next one.";
    }

    private static Track pickTrack(Rule rule) {
        List<Track> candidates = new ArrayList<>();
        for (Track track : rule.tracks) {
            if (track.weight > 0) candidates.add(track);
        }
        if (candidates.isEmpty()) return null;

        // Avoid the same song twice in a row, when there is a choice.
        if (candidates.size() > 1 && lastSoundId != null) {
            List<Track> others = new ArrayList<>();
            for (Track track : candidates) {
                if (!lastSoundId.equals(RuleMatcher.parseSound(track.sound))) others.add(track);
            }
            if (!others.isEmpty()) candidates = others;
        }

        int total = 0;
        for (Track track : candidates) total += track.weight;
        int roll = RANDOM.nextInt(total);
        for (Track track : candidates) {
            roll -= track.weight;
            if (roll < 0) return track;
        }
        return candidates.get(candidates.size() - 1);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static int randomDelayTicks(Rule rule, TuneConfig config) {
        double min = config.default_min_delay_seconds;
        double max = config.default_max_delay_seconds;
        if (rule != null) {
            if (rule.min_delay_seconds != null) min = rule.min_delay_seconds;
            if (rule.max_delay_seconds != null) max = rule.max_delay_seconds;
        }
        if (max < min) max = min;
        return seconds(min + RANDOM.nextDouble() * (max - min));
    }

    /** The song's title when it is one of Minecraft's named songs ("Mice on Venus"), otherwise its sound ID. */
    public static String nameOf(ResourceLocation id) {
        return id == null ? "nothing" : SongCatalog.displayName(id.toString());
    }

    /** The name of the song that played last: its title when we know it, otherwise its sound ID. */
    public static String lastPlayedName() {
        return nameOf(lastPlayedId);
    }

    private static int seconds(double seconds) {
        return (int) Math.round(Math.max(0.0, seconds) * 20.0);
    }

    private static void problem(String message) {
        if (!message.equals(lastProblem)) {
            TuneControl.LOGGER.warn("Tune Control: {}", message);
        }
        lastProblem = message;
    }

    /** Shows a line of text in the player's chat (only the player sees it). */
    public static void say(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        }
    }

    /** The lines shown by /tunecontrol status. */
    public static List<String> statusLines(Minecraft mc) {
        TuneConfig config = TuneConfig.get();
        Context ctx = Context.capture(mc);
        Rule matched = RuleMatcher.findFirst(config, ctx);

        List<String> lines = new ArrayList<>();
        lines.add("Tune Control is " + (config.enabled ? "ON" : "OFF") + " with " + config.rules.size() + " rule(s) loaded.");
        lines.add("You are: " + ctx.describe());
        lines.add("Matching rule: " + (matched != null
                ? matched.name
                : "none -> " + (config.silenceWhenNoRuleMatches() ? "silence" : "normal Minecraft music")));
        boolean playing = current != null && mc.getSoundManager().isActive(current);
        lines.add("Playing from this mod: " + (playing ? nameOf(lastPlayedId) : "nothing"));
        if (!playing && vanilla != null && mc.getSoundManager().isActive(vanilla)) {
            lines.add("Normal Minecraft music is playing: " + nameOf(lastPlayedId));
        }
        if (paused) {
            lines.add("Music is STOPPED (no automatic songs). Type /tunecontrol start to resume.");
        } else if (!playing && matched != null) {
            lines.add("Next song roll in about " + Math.round(cooldownTicks / 20.0) + " second(s).");
        }
        if (!TuneConfig.getLastError().isEmpty()) lines.add("CONFIG PROBLEM: " + TuneConfig.getLastError());
        if (!lastProblem.isEmpty()) lines.add("Last problem: " + lastProblem);
        return lines;
    }
}
