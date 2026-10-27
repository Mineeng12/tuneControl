package com.tunecontrol;

import com.mojang.logging.LogUtils;
import com.tunecontrol.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * The main class. Forge finds it through the @Mod annotation.
 * The text in MOD_ID must match "mod_id" in gradle.properties and "modId" in mods.toml.
 *
 * Tune Control only changes things on your own computer (the "client"), so this class
 * stays empty. The real work happens in the "client" package.
 */
@Mod(TuneControl.MOD_ID)
public class TuneControl {
    public static final String MOD_ID = "tunecontrol";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TuneControl() {
        // The settings screen only exists on a client (a dedicated server has no screens).
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.registerConfigScreen();
        }
    }
}
