package io.github.capsicum0907.laterna;

//? if fabric && <1.19.4 {
/*import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ItemLayers {
    private static final String TABLE = "/assets/" + Laterna.MODID + "/item_layers.json";

    private ItemLayers() {
    }

    public static void register() {
        List<ResourceLocation> models = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : read().entrySet()) {
            List<Part> parts = new ArrayList<>();
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                JsonArray pair = element.getAsJsonArray();
                Part part = new Part(new ResourceLocation(pair.get(0).getAsString()),
                        "translucent".equals(pair.get(1).getAsString()));
                parts.add(part);
                models.add(part.model);
            }
            Item item = net.minecraft.core.Registry.ITEM.get(new ResourceLocation(entry.getKey()));
            BuiltinItemRendererRegistry.INSTANCE.register(item,
                    (stack, mode, pose, buffers, light, overlay) -> render(parts, pose, buffers, light, overlay));
        }
        ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> models.forEach(out));
    }

    private static void render(List<Part> parts, PoseStack pose, MultiBufferSource buffers, int light,
            int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        for (Part part : parts) {
            BakedModel model = BakedModelManagerHelper.getModel(minecraft.getModelManager(), part.model);
            VertexConsumer consumer = buffers.getBuffer(
                    part.translucent ? Sheets.translucentCullBlockSheet() : Sheets.cutoutBlockSheet());
            minecraft.getBlockRenderer().getModelRenderer()
                    .renderModel(pose.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay);
        }
    }

    private static JsonObject read() {
        try (InputStream in = ItemLayers.class.getResourceAsStream(TABLE)) {
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

    private static final class Part {
        private final ResourceLocation model;
        private final boolean translucent;

        private Part(ResourceLocation model, boolean translucent) {
            this.model = model;
            this.translucent = translucent;
        }
    }
}
*///?}
