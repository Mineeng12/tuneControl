package com.tunecontrol.client;

import com.tunecontrol.TuneControl;
import com.tunecontrol.config.TuneConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Startup events (these use Forge's separate "mod" event bus). */
@Mod.EventBusSubscriber(modid = TuneControl.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModEvents {
    private ModEvents() {
    }

    /** Runs once while the game is starting: read (or create) config/tunecontrol.json. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TuneConfig.load();
    }
}
