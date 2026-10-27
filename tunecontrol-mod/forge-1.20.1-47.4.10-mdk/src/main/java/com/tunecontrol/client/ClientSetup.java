package com.tunecontrol.client;

import com.tunecontrol.client.gui.TuneConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only setup. (Only ever called when the game is running as a client; see TuneControl.) */
public final class ClientSetup {
    private ClientSetup() {
    }

    /**
     * Gives the mod a "Config" button in the Mods list (title screen > Mods > Tune Control > Config).
     * The button opens our settings screen, and "parent" is the screen to return to.
     */
    @SuppressWarnings("removal") // Forge marks ModLoadingContext.get() for future removal; it works fine in 1.20.1
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new TuneConfigScreen(parent)));
    }
}
