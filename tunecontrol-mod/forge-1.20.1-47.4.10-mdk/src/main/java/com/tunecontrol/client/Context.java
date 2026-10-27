package com.tunecontrol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A snapshot of "where the player is right now": which dimension, biome, height, block and so on.
 * Rules are compared against this snapshot.
 *
 * feetBlock is the block the player's feet are inside (air in most places, but a carpet, pressure plate or slab
 * when you stand on one). groundBlock is the block directly under that.
 */
public record Context(boolean inWorld,
                      ResourceLocation dimension,
                      ResourceLocation biome,
                      Holder<Biome> biomeHolder,
                      int y,
                      boolean day,
                      Set<String> situations,
                      BlockState feetBlock,
                      BlockState groundBlock) {

    public static Context capture(Minecraft mc) {
        Set<String> situations = new HashSet<>();

        // No world loaded (or still loading): the player is on a menu screen.
        if (mc.level == null || mc.player == null) {
            situations.add("menu");
            return new Context(false, null, null, null, 0, false, situations, null, null);
        }

        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;

        situations.add("in_game");
        if (player.isSpectator()) {
            situations.add("spectator");
        } else if (player.getAbilities().instabuild) {
            situations.add("creative");
        } else {
            situations.add("survival");
        }
        if (player.isEyeInFluid(FluidTags.WATER)) situations.add("underwater");
        if (level.isRaining()) situations.add("raining");
        if (level.isThundering()) situations.add("thundering");

        Holder<Biome> biomeHolder = level.getBiome(player.blockPosition());
        ResourceLocation biomeId = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);

        BlockPos feet = player.blockPosition();
        BlockState feetBlock = level.getBlockState(feet);
        BlockState groundBlock = level.getBlockState(feet.below());

        return new Context(true, level.dimension().location(), biomeId, biomeHolder,
                player.getBlockY(), level.isDay(), situations, feetBlock, groundBlock);
    }

    /** The ID of the block under your feet (for example "minecraft:gold_block"), or null when it is air. */
    public String groundBlockId() {
        return idOf(groundBlock);
    }

    /** The ID of the block your feet are inside (a carpet, a pressure plate...), or null when it is air. */
    public String feetBlockId() {
        return idOf(feetBlock);
    }

    private static String idOf(BlockState state) {
        if (state == null || state.isAir()) return null;
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    /** A human-readable summary, used by /tunecontrol status. */
    public String describe() {
        if (!inWorld) return "on the title screen / a menu";
        String ground = groundBlockId();
        String feet = feetBlockId();
        return "dimension " + dimension
                + ", biome " + biome
                + ", height (Y) " + y
                + ", " + (day ? "daytime" : "night / no daylight")
                + ", situations " + new TreeSet<>(situations)
                + ", standing on " + (ground == null ? "nothing (air)" : ground)
                + (feet == null ? "" : " (inside " + feet + ")");
    }
}
