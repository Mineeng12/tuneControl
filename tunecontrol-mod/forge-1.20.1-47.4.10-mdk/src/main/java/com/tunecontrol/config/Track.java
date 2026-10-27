package com.tunecontrol.config;

/**
 * One song a rule can play.
 * "sound" is a sound event ID such as "minecraft:music.game" or "mytunes:forest_song".
 * "weight" is how likely this song is compared to the others in the same rule
 * (weight 3 is picked three times as often as weight 1).
 */
public class Track {
    public String sound = "minecraft:music.game";
    public int weight = 1;

    public Track() {
    }

    public Track(String sound, int weight) {
        this.sound = sound;
        this.weight = weight;
    }
}
