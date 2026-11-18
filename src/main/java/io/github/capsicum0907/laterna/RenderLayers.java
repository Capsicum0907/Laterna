package io.github.capsicum0907.laterna;

//? if (forge && <1.19) || fabric {
/*import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
*///?}
//? if fabric {
/*import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;

public final class RenderLayers implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        apply();
    }

    private static void set(String id, List<RenderType> layers) {
        if (layers.size() != 1) {
            throw new IllegalStateException(id + " needs " + layers.size() + " layers");
        }
        //? if >=1.21 {
        /^Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        ^///?} else if >=1.19.3 {
        /^Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
        ^///?} else
        /^Block block = net.minecraft.core.Registry.BLOCK.get(new ResourceLocation(id));^/
        BlockRenderLayerMap.INSTANCE.putBlock(block, layers.get(0));
    }
*///?} else if forge && <1.19 {
/*import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class RenderLayers {
    private RenderLayers() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RenderLayers::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(RenderLayers::apply);
    }

    private static void set(String id, List<RenderType> layers) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        ItemBlockRenderTypes.setRenderLayer(block, layers::contains);
    }
*///?}
//? if (forge && <1.19) || fabric {

    /*private static final String TABLE = "/assets/" + Laterna.MODID + "/render_layers.json";

    private static void apply() {
        for (Map.Entry<String, JsonElement> entry : read().entrySet()) {
            List<RenderType> layers = new ArrayList<>();
            for (JsonElement layer : entry.getValue().getAsJsonArray()) {
                layers.add(layer(layer.getAsString()));
            }
            set(entry.getKey(), layers);
        }
    }

    private static JsonObject read() {
        try (InputStream in = RenderLayers.class.getResourceAsStream(TABLE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + TABLE);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new JsonParser().parse(reader).getAsJsonObject();
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static RenderType layer(String name) {
        switch (name) {
            case "solid": return RenderType.solid();
            case "cutout_mipped": return RenderType.cutoutMipped();
            case "cutout": return RenderType.cutout();
            case "translucent": return RenderType.translucent();
            default: throw new IllegalArgumentException("Unknown layer " + name);
        }
    }
}
*///?}
