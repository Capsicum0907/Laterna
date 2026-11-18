package io.github.capsicum0907.laterna;

//? if forge && <1.19 {
/*import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class RenderLayers {
    private static final String TABLE = "/assets/" + Laterna.MODID + "/render_layers.json";

    private RenderLayers() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RenderLayers::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
        JsonObject table = read();
        event.enqueueWork(() -> {
            for (Map.Entry<String, JsonElement> entry : table.entrySet()) {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getKey()));
                Set<RenderType> layers = new HashSet<>();
                for (JsonElement layer : entry.getValue().getAsJsonArray()) {
                    layers.add(layer(layer.getAsString()));
                }
                ItemBlockRenderTypes.setRenderLayer(block, layers::contains);
            }
        });
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
