package io.github.capsicum0907.laterna;


//? if neoforge {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
//?} else if fabric {
/*import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
*///?} else {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?}

//? if >=1.18 {
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
//?} else {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

/**
 * Entry point. {@link #MODID} must match {@code mod_id} in gradle.properties,
 * which is what the generated neoforge.mods.toml is filled from.
 */
//? if fabric {
/*public class Laterna implements ModInitializer {
*///?} else {
@Mod(Laterna.MODID)
public class Laterna {
//?}
    public static final String MODID = "laterna";

    //? if >=1.18 {
    private static final Logger LOGGER = LogUtils.getLogger();
    //?} else
    //private static final Logger LOGGER = LogManager.getLogger();

    //? if fabric {
    /*@Override
    public void onInitialize() {
        LaternaRegistry.BLOCKS.register();
        LaternaRegistry.ITEMS.register();
        //? if >=1.19.3
        LaternaRegistry.TABS.register();
        LOGGER.info("Laterna {} loaded, {} lamps.", FabricLoader.getInstance().getModContainer(MODID)
                .orElseThrow().getMetadata().getVersion().getFriendlyString(), Lamp.all().size());
    }
    *///?} else {
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
    //?}
}
