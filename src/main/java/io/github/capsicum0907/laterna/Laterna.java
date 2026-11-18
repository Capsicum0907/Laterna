package io.github.capsicum0907.laterna;

import com.mojang.logging.LogUtils;

//? if neoforge {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
//?} else {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?}

import org.slf4j.Logger;

/**
 * Entry point. {@link #MODID} must match {@code mod_id} in gradle.properties,
 * which is what the generated neoforge.mods.toml is filled from.
 */
@Mod(Laterna.MODID)
public class Laterna {
    public static final String MODID = "laterna";

    private static final Logger LOGGER = LogUtils.getLogger();

    //? if neoforge {
    public Laterna(IEventBus modEventBus, ModContainer modContainer) {
    //?} else {
    /*public Laterna() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
    *///?}
        LaternaRegistry.BLOCKS.register(modEventBus);
        LaternaRegistry.ITEMS.register(modEventBus);
        //? if forge && <1.19 {
        /*if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            RenderLayers.register(modEventBus);
        }
        *///?}
        //? if >=1.19.3
        LaternaRegistry.TABS.register(modEventBus);

        LOGGER.info("Laterna {} loaded, {} lamps.", modContainer.getModInfo().getVersion(),
                Lamp.all().size());
    }
}
