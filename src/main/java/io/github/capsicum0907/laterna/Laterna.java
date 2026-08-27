package io.github.capsicum0907.laterna;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import org.slf4j.Logger;

/**
 * Entry point. {@link #MODID} must match {@code mod_id} in gradle.properties,
 * which is what the generated neoforge.mods.toml is filled from.
 */
@Mod(Laterna.MODID)
public class Laterna {
    public static final String MODID = "laterna";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Laterna(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Laterna {} loaded.", modContainer.getModInfo().getVersion());
    }
}
