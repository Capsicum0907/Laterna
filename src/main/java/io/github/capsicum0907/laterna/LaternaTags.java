package io.github.capsicum0907.laterna;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * A tag per form and wiring, holding all sixteen colours of it.
 *
 * <p>These exist for the dyeing recipes. A recipe that named a colour would be sixteen
 * recipes per colour; one that takes the tag is one recipe that says "any lamp of this
 * kind", and the kind is what has to be preserved - dye an inverted lamp and an inverted
 * lamp is what comes back.
 *
 * <p>Blocks and items both, because a recipe takes items and the block tag is where the
 * membership is written; the item tag is copied from it at generation time.
 */
public final class LaternaTags {
    private static final Map<String, TagKey<Block>> BLOCK_TAGS = new LinkedHashMap<>();
    private static final Map<String, TagKey<Item>> ITEM_TAGS = new LinkedHashMap<>();

    static {
        for (Shape shape : Shape.values()) {
            for (Frame frame : shape.frames()) {
                for (Wiring wiring : shape.wirings()) {
                    String path = Lamp.tag(shape, wiring, frame);
                    ResourceLocation name =
                            ResourceLocation.fromNamespaceAndPath(Laterna.MODID, path);
                    BLOCK_TAGS.put(path, TagKey.create(Registries.BLOCK, name));
                    ITEM_TAGS.put(path, TagKey.create(Registries.ITEM, name));
                }
            }
        }
    }

    private LaternaTags() {
    }

    public static TagKey<Block> blocks(Shape shape, Wiring wiring, Frame frame) {
        return BLOCK_TAGS.get(Lamp.tag(shape, wiring, frame));
    }

    public static TagKey<Item> items(Shape shape, Wiring wiring, Frame frame) {
        return ITEM_TAGS.get(Lamp.tag(shape, wiring, frame));
    }

    /** Every pair there is, for the generators that have to walk all of them. */
    public static Map<String, TagKey<Block>> allBlockTags() {
        return BLOCK_TAGS;
    }
}
