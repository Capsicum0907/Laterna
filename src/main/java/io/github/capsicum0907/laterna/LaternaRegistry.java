package io.github.capsicum0907.laterna;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.20 {
import net.minecraft.world.level.material.MapColor;
//?} else {
/*import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
*///?}
//? if neoforge {
import net.neoforged.neoforge.registries.DeferredRegister;
//?} else
//import net.minecraftforge.registries.DeferredRegister;

/**
 * Registration, one block and one item per entry in {@link Lamp#all()}.
 *
 * <p>There is no list here. The loop below is the only place blocks are created, and
 * what it loops over is the product; a form added to {@link Shape} arrives in the game,
 * in the creative tab and in every generated file without anything else being edited.
 */
public final class LaternaRegistry {
    //? if >=1.17 {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Laterna.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Laterna.MODID);
    //?} else {
    /*public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            net.minecraftforge.registries.ForgeRegistries.BLOCKS, Laterna.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            net.minecraftforge.registries.ForgeRegistries.ITEMS, Laterna.MODID);
    *///?}
    //? if >=1.19.3 {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Laterna.MODID);
    //?}

    private static final Map<Lamp, Supplier<Block>> BLOCK = new LinkedHashMap<>();
    private static final Map<Lamp, Supplier<BlockItem>> ITEM = new LinkedHashMap<>();

    static {
        for (Lamp lamp : Lamp.all()) {
            Supplier<Block> block =
                    BLOCKS.register(lamp.id(), () -> built(lamp).apply(properties(lamp)));
            BLOCK.put(lamp, block);
            ITEM.put(lamp, ITEMS.register(lamp.id(),
                    () -> new BlockItem(block.get(), new Item.Properties()
                            //? if <1.19.3
                            //.tab(LaternaRegistry.TAB)
                    )));
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
            // ⚠ A cube that is always lit needs no LIT state and so no LampBlock -
            // there is nothing for redstone to flip.
            // ⚠ And one switched cube is not a lamp: a shade wires up exactly as a lamp
            // does and spends the state on opacity instead of on brightness.
            case NONE -> switch (shape) {
                case SHADE -> properties -> new ShadeBlock(lamp.wiring(), properties);
                case GLASS -> GlowingGlassBlock::new;
                default -> shape.switched()
                        ? properties -> new LampBlock(lamp.wiring(), properties)
                        : Block::new;
            };
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
        // A form with no colour in its name has none on a map either, which is what the
        // game already says about glass.
        //? if >=1.20 {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(lamp.colour().map(DyeColor::getMapColor).orElse(MapColor.NONE))
        //?} else {
        /*BlockBehaviour.Properties properties = BlockBehaviour.Properties.of(Material.BUILDABLE_GLASS,
                        lamp.colour().map(DyeColor::getMaterialColor).orElse(MaterialColor.NONE))
        *///?}
                .strength(lamp.shape().strength())
                .sound(lamp.shape().sound())
                .lightLevel(light(lamp));
        return switch (lamp.shape().mount()) {
            // ⚠ A case is see-through, so it must not be treated as a solid wall, but
            // it does fill its cell and is something to walk into.
            // ⚠ A shade is neither: it is not a wall and it is not there to walk into.
            // What stops the light is its opacity, which it answers for itself, so
            // everything else about it has to say "empty cell" or it would be a pane of
            // invisible glass in the way.
            case NONE -> switch (lamp.shape()) {
                case CASED -> properties.noOcclusion();
                case SHADE -> properties.noOcclusion().noCollission();
                // ⚠ Everything the game says about its own glass, and for the same
                // reasons: it is not a wall to light, not a wall to redstone, you do not
                // suffocate in it, you can see out of it, and nothing spawns on it. Left
                // to the defaults a see-through block is a solid one that happens to be
                // drawn see-through.
                case GLASS -> properties.noOcclusion()
                        .isValidSpawn(LaternaRegistry::never)
                        .isRedstoneConductor(LaternaRegistry::never)
                        .isSuffocating(LaternaRegistry::never)
                        .isViewBlocking(LaternaRegistry::never);
                default -> properties;
            };
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

    /** The game's own answer for glass, which it keeps to itself. */
    private static boolean never(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    /** The same, for the one of these that is asked about an entity as well. */
    private static boolean never(BlockState state, BlockGetter level, BlockPos pos,
            EntityType<?> entity) {
        return false;
    }

    /**
     * How bright a form is, which is not the same question as how it is wired.
     *
     * <p>⚠ <b>A switched block is not necessarily a lamp.</b> This used to read the
     * brightness straight off the wiring, because every form there was gave off light;
     * a shade is switched and gives off none, and reading its state the old way would
     * have made the block that takes light away the brightest thing in the room.
     */
    private static ToIntFunction<BlockState> light(Lamp lamp) {
        if (!lamp.shape().emits()) {
            return state -> 0;
        }
        return lamp.switched()
                ? state -> state.getValue(LampBlock.LIT) ? LampBlock.BRIGHTEST : 0
                : state -> LampBlock.BRIGHTEST;
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
    //? if >=1.19.3 {
    public static final Supplier<CreativeModeTab> TAB =
            TABS.register("laterna", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Laterna.MODID))
                    .icon(() -> new ItemStack(item(Lamp.all().get(0)).get()))
                    .displayItems((parameters, output) -> {
                        for (Lamp lamp : Lamp.all()) {
                            output.accept(item(lamp).get());
                        }
                    })
                    .build());
    //?} else {
    /*public static final CreativeModeTab TAB = new CreativeModeTab(Laterna.MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(item(Lamp.all().get(0)).get());
        }
    };
    *///?}

    private LaternaRegistry() {
    }

    public static Supplier<Block> block(Lamp lamp) {
        return BLOCK.get(lamp);
    }

    public static Supplier<BlockItem> item(Lamp lamp) {
        return ITEM.get(lamp);
    }
}
