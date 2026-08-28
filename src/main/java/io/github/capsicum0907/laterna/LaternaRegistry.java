package io.github.capsicum0907.laterna;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration, one block and one item per entry in {@link Lamp#all()}.
 *
 * <p>There is no list here. The loop below is the only place blocks are created, and
 * what it loops over is the product; a form added to {@link Shape} arrives in the game,
 * in the creative tab and in every generated file without anything else being edited.
 */
public final class LaternaRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Laterna.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Laterna.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Laterna.MODID);

    private static final Map<Lamp, DeferredBlock<Block>> BLOCK = new LinkedHashMap<>();
    private static final Map<Lamp, DeferredItem<BlockItem>> ITEM = new LinkedHashMap<>();

    static {
        for (Lamp lamp : Lamp.all()) {
            DeferredBlock<Block> block =
                    BLOCKS.registerBlock(lamp.id(), built(lamp), properties(lamp));
            BLOCK.put(lamp, block);
            ITEM.put(lamp, ITEMS.registerSimpleBlockItem(block));
        }
    }

    /**
     * Which class a form is made of.
     *
     * <p>A switch over the forms rather than a field on {@link Shape}: what a block class
     * is belongs to the code, and putting a constructor in the enum would have the enum
     * import blocks in order to describe itself.
     */
    private static Function<BlockBehaviour.Properties, Block> built(Lamp lamp) {
        Shape shape = lamp.shape();
        return switch (shape.mount()) {
            case NONE -> properties -> new LampBlock(lamp.wiring(), properties);
            case ANY -> properties -> new FacePlateBlock(shape, properties);
            case FLAT -> shape.stacks()
                    ? properties -> new StackingPlateBlock(shape, properties)
                    : properties -> new FlatPlateBlock(shape, properties);
            case AXIS -> properties -> new RodBlock(shape.inset(), properties);
            case UPRIGHT -> shape.stacks()
                    ? properties -> new UprightStackingPlateBlock(shape, properties)
                    : properties -> new UprightPlateBlock(shape, properties);
        };
    }

    /**
     * Everything the game needs to know about a lamp as a block, all of it derived.
     *
     * <p><b>The map colour comes from the dye</b> rather than from a pair of greys chosen
     * by hand: the game already knows what colour each dye is on a map, and asking it
     * means sixteen colours cost one line and a seventeenth would cost none. The light is
     * read off the state, which is what keeps this a plain block.
     */
    private static BlockBehaviour.Properties properties(Lamp lamp) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(lamp.colour().getMapColor())
                .strength(lamp.shape().strength())
                .sound(lamp.shape().sound())
                .lightLevel(lamp.switched()
                        ? state -> state.getValue(LampBlock.LIT) ? 15 : 0
                        : state -> 15);
        return switch (lamp.shape().mount()) {
            case NONE -> properties;
            // A plate is not a cube and must not hide the face behind it. Recessed also
            // means flush, so the spotlight is nothing to stand on - but a slab is a step
            // and is supposed to be. See PlateBlock.
            // ⚠ Only the recessed one is nothing to stand on. A bulb or a fitting
            // stands proud of its surface and is something you can bump into, which is
            // what being proud of a surface means.
            case ANY -> lamp.shape() == Shape.SPOTLIGHT
                    ? properties.noOcclusion().noCollission()
                    : properties.noOcclusion();
            // ⚠ The one that stacks keeps its occlusion. A thin thing is not a wall,
            // but two of these laid together are a whole block and have to stop light -
            // so it is left alone and answers with its shape instead. See
            // StackingPlateBlock.
            case FLAT, UPRIGHT ->
                    lamp.shape().stacks() ? properties : properties.noOcclusion();
            case AXIS -> properties.noOcclusion();
        };
    }

    /**
     * One tab, in the product's own order: form, then wiring, then colour.
     *
     * <p>⚠ <b>A tab is not decoration.</b> Anything reading the creative menu - a recipe
     * browser, most of all - only sees items that are in one, and a hundred and twenty
     * eight lamps poured into a vanilla tab would bury it. Ordering by colour first would
     * scatter each form sixteen apart, which is the one arrangement that stops the ramp
     * of colours reading as a ramp.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("laterna", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Laterna.MODID))
                    .icon(() -> new ItemStack(item(Lamp.all().getFirst()).get()))
                    .displayItems((parameters, output) -> {
                        for (Lamp lamp : Lamp.all()) {
                            output.accept(item(lamp).get());
                        }
                    })
                    .build());

    private LaternaRegistry() {
    }

    public static DeferredBlock<Block> block(Lamp lamp) {
        return BLOCK.get(lamp);
    }

    public static DeferredItem<BlockItem> item(Lamp lamp) {
        return ITEM.get(lamp);
    }
}
