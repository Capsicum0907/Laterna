package io.github.capsicum0907.laterna.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import io.github.capsicum0907.laterna.Frame;
import io.github.capsicum0907.laterna.Lamp;
import io.github.capsicum0907.laterna.LampBlock;
import io.github.capsicum0907.laterna.Laterna;
import io.github.capsicum0907.laterna.LaternaRegistry;
import io.github.capsicum0907.laterna.LaternaTags;
import io.github.capsicum0907.laterna.Shape;
import io.github.capsicum0907.laterna.PlateBlock;
import io.github.capsicum0907.laterna.RodBlock;
import io.github.capsicum0907.laterna.Shape.Mount;
import io.github.capsicum0907.laterna.Wiring;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import io.github.capsicum0907.laterna.StackingPlateBlock;
import io.github.capsicum0907.laterna.UprightStackingPlateBlock;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;

/**
 * Everything under {@code src/generated} - and every texture - written from
 * {@link Lamp#all()}.
 *
 * <p>Nothing in here names a colour or a block. Each provider walks the product and asks
 * the lamp what it is called, what it is made of and what it looks like, which is why
 * adding a form to {@link Shape} needs no edit to this file at all.
 */
@EventBusSubscriber(modid = Laterna.MODID)
public final class LaternaDataGen {
    private LaternaDataGen() {
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        // ⚠ The model provider refuses a texture it cannot find, and every texture this
        // mod has is made by the provider added two lines below - in this same run, and
        // not yet. Saying so up front is what stops it failing the build over a file that
        // is about to exist.
        ExistingFileHelper.ResourceType texture =
                new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", "textures");
        for (String skin : LampTextures.skins()) {
            helper.trackGenerated(
                    ResourceLocation.fromNamespaceAndPath(Laterna.MODID, "block/" + skin), texture);
        }

        generator.addProvider(event.includeClient(), new LampTextures(output));
        generator.addProvider(event.includeClient(), new Models(output, helper));
        generator.addProvider(event.includeClient(), new Language(output));
        generator.addProvider(event.includeClient(), new ModIcon());

        generator.addProvider(event.includeServer(), new Loot(output, lookup));
        generator.addProvider(event.includeServer(), new Recipes(output, lookup));
        generator.addProvider(event.includeServer(), new TestStructures(output));

        // The item tags are copies of the block tags, so the block provider has to be
        // held on to rather than merely registered: its contents are the other one's input.
        BlockTags blocks = new BlockTags(output, lookup, helper);
        generator.addProvider(event.includeServer(), blocks);
        generator.addProvider(event.includeServer(),
                new ItemTags(output, lookup, blocks.contentsGetter(), helper));
    }

    /**
     * A model per form, colour and state, and a block state that picks between them.
     *
     * <p><b>The models are built once and shared.</b> A normal lamp and an inverted one
     * point at the same pair of files - building them per lamp would write each one twice
     * and, worse, would suggest they could differ.
     *
     * <p>The item takes the model of the state the block is placed in, so an inverted
     * lamp looks lit in the hand. That is not decoration: it is what you get when you put
     * it down.
     */
    private static class Models extends BlockStateProvider {
        Models(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, Laterna.MODID, existingFileHelper);
        }

        /**
         * ⚠ <b>Where the plate is drawn, as a rotation of one model.</b> The model is
         * authored with its plate on the north face of the cell; these are the rotations
         * that carry it onto each of the six, read off vanilla's own {@code glow_lichen}
         * blockstate rather than worked out. {@code FACING} is the direction the light
         * shines, so the face wanted is always its opposite.
         *
         * <p>⚠ <b>A game test cannot check this.</b> It can say the block faces up; it
         * cannot say the plate was drawn on the floor rather than the ceiling. If the
         * {@code getOpposite} were dropped, every test would still pass.
         */
        private static final Map<Direction, int[]> ROTATION = Map.of(
                Direction.NORTH, new int[] { 0, 0 },
                Direction.EAST, new int[] { 0, 90 },
                Direction.SOUTH, new int[] { 0, 180 },
                Direction.WEST, new int[] { 0, 270 },
                Direction.UP, new int[] { 270, 0 },
                Direction.DOWN, new int[] { 90, 0 });

        /**
         * How far off the face the fitting stands, and the lens a shade further still.
         *
         * <p>⚠ <b>The order matters now.</b> The ring is a solid disc rather than a ring
         * with a hole in it, because the lens softens its edge onto it - so the ring is
         * the one at the back, and putting them the other way round hides the lens
         * entirely.
         */
        private static final float PLATE = 0.1F;
        private static final float OVER = 0.11F;

        @Override
        protected void registerStatesAndModels() {
            Map<String, ModelFile> models = new HashMap<>();
            for (Shape shape : Shape.values()) {
                for (Frame frame : shape.frames()) {
                    for (DyeColor colour : DyeColor.values()) {
                        for (boolean lit : new boolean[] { true, false }) {
                            if (!lit && !shape.switched()) {
                                continue;
                            }
                            String skin = Lamp.skin(shape, frame, colour, lit);
                            models.put(skin, shaped(shape, skin));
                        }
                    }
                }
            }

            for (Lamp lamp : Lamp.all()) {
                Block block = LaternaRegistry.block(lamp).get();
                ModelFile model = models.get(lamp.skin(lamp.litByDefault()));
                switch (lamp.shape().mount()) {
                    // A cube has one look and no states to pick between.
                    case NONE -> {
                        if (!lamp.switched()) {
                            getVariantBuilder(block).partialState()
                                    .modelForState().modelFile(model).addModel();
                            break;
                        }
                        getVariantBuilder(block)
                            .partialState().with(LampBlock.LIT, true)
                            .modelForState().modelFile(models.get(lamp.skin(true))).addModel()
                            .partialState().with(LampBlock.LIT, false)
                            .modelForState().modelFile(models.get(lamp.skin(false))).addModel();
                    }
                    // One arm for every way of clinging to a face. Which states a form
                    // keeps is its own business - the plate is asked which way it faces
                    // and the answer is turned onto that face, so a fourth kind of
                    // mounting would arrive here needing nothing.
                    // ⚠ A rod is not a plate: it has no face to be turned onto, only an
                    // axis to lie along. Standing up is the model as authored; the other
                    // two are it turned, which is what vanilla does with a log.
                    case AXIS -> getVariantBuilder(block)
                            .partialState().with(RodBlock.AXIS, Direction.Axis.Y)
                            .modelForState().modelFile(model).addModel()
                            .partialState().with(RodBlock.AXIS, Direction.Axis.Z)
                            .modelForState().modelFile(model).rotationX(90).addModel()
                            .partialState().with(RodBlock.AXIS, Direction.Axis.X)
                            .modelForState().modelFile(model).rotationX(90).rotationY(90)
                            .addModel();
                    case ANY, FLAT, UPRIGHT -> {
                        PlateBlock plate = (PlateBlock) block;
                        // ⚠ A pair of lying slabs is the same cube whichever way it
                        // was built; a standing pair is not - its lit faces are north and
                        // south, or east and west. So the whole block is drawn flat
                        // against the axis the pair was stacked along, and turned onto the
                        // other one the same way a single plate is.
                        boolean upright = lamp.shape().mount() == Mount.UPRIGHT;
                        ModelFile whole = lamp.shape().stacks()
                                ? doubled(lamp.skin(true),
                                        upright ? Direction.Axis.Z : Direction.Axis.Y)
                                : null;
                        // ⚠ A form whose size differs by face needs a model per face,
                        // not one turned onto them. A fitting is a bar on a wall and a disc
                        // on a floor, and no rotation of the first is the second.
                        ModelFile flat = lamp.shape().fit(Direction.UP)
                                .equals(lamp.shape().fit(Direction.NORTH))
                                ? null
                                : fitting(lamp.shape(), lamp.skin(true), Direction.UP);
                        getVariantBuilder(block).forAllStatesExcept(state -> {
                            if (plate.whole(state)) {
                                boolean across = upright && plate.facing(state).getAxis()
                                        == Direction.Axis.X;
                                return ConfiguredModel.builder().modelFile(whole)
                                        .rotationY(across ? 90 : 0).build();
                            }
                            Direction facing = plate.facing(state);
                            int[] turn = ROTATION.get(PlateBlock.against(facing));
                            return ConfiguredModel.builder()
                                    .modelFile(flat != null
                                            && facing.getAxis() == Direction.Axis.Y
                                            ? flat : model)
                                    .rotationX(turn[0]).rotationY(turn[1])
                                    // ⚠ Only the flat one. Locking the texture to the
                                    // world is what keeps the spotlight's bevel pointing
                                    // the same way whichever face it is on; on a box it
                                    // would instead change how the four thin sides are
                                    // cut out of a texture drawn for a square face.
                                    .uvLock(lamp.shape().mount() == Mount.ANY)
                                    .build();
                        }, PlateBlock.WATERLOGGED);
                    }
                }
                item(lamp, model);
            }
        }

        /** The geometry a form has. */
        private ModelFile shaped(Shape shape, String skin) {
            return switch (shape) {
                case LAMP -> models().cubeAll(skin, modLoc("block/" + skin));
                case SPOTLIGHT -> plate(skin);
                case SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL -> box(shape, skin);
                case BULB, FIXTURE -> fitting(shape, skin, Direction.NORTH);
                case ROD -> rod(shape, skin);
                case CASED -> cased(shape, skin);
            };
        }

        /**
         * A fitting on the middle of the north face of the cell: a base against the
         * surface, and the part that glows standing out of it.
         *
         * <p>⚠ <b>Two parts, and that is the whole of why these read as fittings.</b>
         * One box with a lit face on it is a tile stuck to a wall. A plate flush with the
         * surface with something raised out of it is a lamp bolted to one - which is how
         * the mod this follows builds both of these, and it was the difference the first
         * pass missed.
         *
         * <p>⚠ <b>Every face is given the whole texture, rather than the piece of it
         * the game would work out.</b> A face four pixels across takes a four-pixel corner
         * of a sixteen-pixel picture if nothing says otherwise, which is the crude slice
         * the broad forms were pulled up on. Written out, the small face wears the whole
         * lamp, scaled - and the masters for these forms are drawn without a border for
         * exactly that reason.
         */
        private ModelFile fitting(Shape shape, String skin, Direction facing) {
            Shape.Fit fit = shape.fit(facing);
            float wide = (float) fit.wide();
            float tall = (float) fit.tall();
            float deep = (float) fit.deep();
            // ⚠ The model gets a name per face, because a fitting is a different size
            // on a wall than on a floor - but the textures do not: there is one set per
            // colour, and every model of that colour wears it.
            boolean flat = facing.getAxis() == Direction.Axis.Y;
            String name = skin + (flat ? "_flat" : "");
            if (shape == Shape.BULB) {
                return bulb(name, skin, wide, tall, deep, false);
            }
            return flat
                    ? plated(name, skin, wide, tall, deep)
                    : hooded(name, skin, wide, tall, deep);
        }

        /**
         * A fitting on a ceiling or a floor: a dark plate against the surface and the lamp
         * below it.
         *
         * <p>Lit on every side but the one against the plate, because from underneath a
         * ceiling light you see its underside and its edges at once and both are the lamp.
         */
        private ModelFile plated(String name, String skin, float wide, float tall, float deep) {
            BlockModelBuilder builder = dressed(models().getBuilder(name), skin);
            part(builder, box(wide, tall, 0, 1), "#edge", true);
            part(builder, box(wide, tall, 1, deep), "#face", false);
            return builder;
        }

        /**
         * A fitting on a wall: a hood over a lamp.
         *
         * <p>⚠ <b>The housing is an L seen from the side</b> - down the back, where it
         * meets the wall, and along the top, where the hood is. The lamp fills the rest, so
         * the light shows on the front, the underside and the two ends, and what you see
         * from above is all housing.
         *
         * <pre>
         *     back  #■■■■■   the top of the fitting, its whole depth
         *      of    ■.....
         *      it    ■.....   the lamp
         *            ■.....
         * </pre>
         *
         * <p>Painting all five faces dark, which is what a slab does, is what this replaces:
         * on a slab the sides are cut material and belong to the block, but on a fitting
         * they are the lamp and are meant to be seen.
         */
        private ModelFile hooded(String name, String skin, float wide, float tall, float deep) {
            float low = (16 - tall) / 2;
            float high = (16 + tall) / 2;
            BlockModelBuilder builder = dressed(models().getBuilder(name), skin);
            part(builder, box(wide, tall, 0, 1), "#edge", true);
            part(builder, new float[] { (16 - wide) / 2, high - 1, 1,
                    (16 + wide) / 2, high, deep }, "#edge", false);
            part(builder, new float[] { (16 - wide) / 2, low, 1,
                    (16 + wide) / 2, high - 1, deep }, "#face", false);
            return builder;
        }

        /**
         * A bulb: a dark base, a dark neck, a coloured body, and a shell over the body.
         *
         * <p>⚠ <b>The neck is housing, not lamp.</b> Where the body meets its base there
         * has to be something holding it, and drawing that in the lamp's own colour makes
         * the light look as though it starts at the ceiling. One pixel of dark between the
         * two is the whole difference.
         *
         * <p>⚠ <b>The shell is a second model, drawn translucent.</b> One model cannot be
         * part solid and part see-through - the render type belongs to the whole of it - so
         * the two are separate models joined by the game's composite loader. This is the
         * nesting vanilla's beacon uses as well, though the beacon reaches it with a cutout
         * texture rather than a translucent one.
         */
        private ModelFile bulb(String name, String skin, float wide, float tall, float deep,
                boolean standing) {
            BlockModelBuilder solid = dressed(models().getBuilder(name + "_body"), skin)
                    .renderType("minecraft:solid");
            part(solid, upright(box(wide, tall, 0, 1), standing), "#edge", !standing);
            part(solid, upright(box(wide - 2, tall - 2, 1, 2), standing), "#edge", false);
            part(solid, upright(box(wide - 2, tall - 2, 2, deep), standing), "#face", false);

            BlockModelBuilder shell = models().getBuilder(name + "_shell")
                    .parent(new ModelFile.UncheckedModelFile("block/block"))
                    .renderType("minecraft:translucent")
                    .ao(false)
                    .texture("halo", modLoc("block/" + skin + Masters.Layer.HALO.suffix()))
                    .texture("particle", modLoc("block/" + skin));
            // A shade wider than the body and clear of the neck, so the body is seen
            // through it rather than coincident with it.
            float[] glass = upright(new float[] { (16 - wide) / 2 + 0.75F,
                    (16 - tall) / 2 + 0.75F, 2, (16 + wide) / 2 - 0.75F,
                    (16 + tall) / 2 - 0.75F, deep + 0.25F }, standing);
            shell.element()
                    .from(glass[0], glass[1], glass[2]).to(glass[3], glass[4], glass[5])
                    .shade(false)
                    .allFaces((direction, face) -> {
                        face.texture("#halo");
                        face.uvs(0, 0, 16, 16);
                    })
                    .end();

            return models().getBuilder(name)
                    // ⚠ Without a parent a composite inherits no display transforms, and
                    // the game draws it square-on - which is how a bulb came to be held
                    // lying on its side.
                    .parent(new ModelFile.UncheckedModelFile("block/block"))
                    .texture("particle", modLoc("block/" + skin))
                    .customLoader(CompositeModelBuilder::begin)
                        .child("body", models().nested()
                                .parent(solid).renderType("minecraft:solid"))
                        .child("shell", models().nested()
                                .parent(shell).renderType("minecraft:translucent"))
                        .itemRenderOrder("body", "shell")
                    .end();
        }

        /**
         * The same box stood on end.
         *
         * <p>⚠ <b>A block model is built against the north face so one rotation table can
         * turn it onto any other; an item is never turned.</b> So the bulb is authored a
         * second time standing on its base, which is the way it is held and the way it sits
         * in a slot - the same trick the lying slab needs, for the same reason.
         */
        private float[] upright(float[] box, boolean standing) {
            return standing
                    ? new float[] { box[0], box[2], box[1], box[3], box[5], box[4] }
                    : box;
        }

        /** The textures and the parent every fitting wears. */
        private BlockModelBuilder dressed(BlockModelBuilder builder, String skin) {
            return builder
                    .parent(new ModelFile.UncheckedModelFile("block/block"))
                    .texture("face", modLoc("block/" + skin))
                    .texture("edge", modLoc("block/" + skin + Masters.Layer.EDGE.suffix()))
                    .texture("particle", modLoc("block/" + skin));
        }

        /**
         * A lamp in a case: an opaque core with a clear cover around it, filling the cell.
         *
         * <p>WARN <b>The cover has to be a second model.</b> A render type belongs to the
         * whole of a model, so a block that is solid in the middle and see-through around
         * it is two models joined by the composite loader - the same arrangement as the
         * bulb, and the same nesting vanilla's beacon uses.
         *
         * <p>The core sits four pixels in on every side, so the cover reads as a case with
         * something inside it rather than as a tinted block.
         */
        private ModelFile cased(Shape shape, String skin) {
            float inset = (float) shape.inset();
            BlockModelBuilder core = models().getBuilder(skin + "_core")
                    .parent(new ModelFile.UncheckedModelFile("block/block"))
                    .renderType("minecraft:solid")
                    .texture("face", modLoc("block/" + skin))
                    .texture("particle", modLoc("block/" + skin));
            core.element()
                    .from(inset, inset, inset).to(16 - inset, 16 - inset, 16 - inset)
                    .allFaces((direction, face) -> {
                        face.texture("#face");
                        face.uvs(0, 0, 16, 16);
                    })
                    .end();

            BlockModelBuilder cover = models().getBuilder(skin + "_cover")
                    .parent(new ModelFile.UncheckedModelFile("block/block"))
                    .renderType("minecraft:translucent")
                    .ao(false)
                    .texture("halo", modLoc("block/" + skin + Masters.Layer.HALO.suffix()))
                    .texture("particle", modLoc("block/" + skin));
            cover.element()
                    .from(0, 0, 0).to(16, 16, 16)
                    .shade(false)
                    .allFaces((direction, face) -> {
                        face.texture("#halo");
                        face.uvs(0, 0, 16, 16);
                    })
                    .end();

            return models().getBuilder(skin)
                    .parent(new ModelFile.UncheckedModelFile("block/block"))
                    .texture("particle", modLoc("block/" + skin))
                    .customLoader(CompositeModelBuilder::begin)
                        .child("core", models().nested()
                                .parent(core).renderType("minecraft:solid"))
                        .child("cover", models().nested()
                                .parent(cover).renderType("minecraft:translucent"))
                        .itemRenderOrder("core", "cover")
                    .end();
        }

        /**
         * A thin bar running the whole height of the cell, which is the length of a rod.
         *
         * <p>Authored standing up, because that is the one orientation the axis property
         * names without a rotation. The other two are that model turned.
         */
        private ModelFile rod(Shape shape, String skin) {
            float thin = (float) shape.inset();
            return models().getBuilder(skin)
                    .parent(new ModelFile.UncheckedModelFile("block/block"))
                    .texture("face", modLoc("block/" + skin))
                    .texture("particle", modLoc("block/" + skin))
                    .element()
                        .from(thin, 0, thin).to(16 - thin, 16, 16 - thin)
                        .allFaces((direction, face) -> {
                            face.texture("#face");
                            face.uvs(0, 0, 16, 16);
                            if (direction.getAxis() == Direction.Axis.Y) {
                                face.cullface(direction);
                            }
                        })
                    .end();
        }

        /** A box of the given size, centred across the face and standing off it. */
        private float[] box(float wide, float tall, float from, float to) {
            return new float[] { (16 - wide) / 2, (16 - tall) / 2, from,
                    (16 + wide) / 2, (16 + tall) / 2, to };
        }

        /** One box of a fitting, wearing one texture, with the whole of it on every face. */
        private void part(BlockModelBuilder builder, float[] box, String texture,
                boolean against) {
            builder.element()
                    .from(box[0], box[1], box[2]).to(box[3], box[4], box[5])
                    .allFaces((direction, face) -> {
                        face.texture(texture);
                        face.uvs(0, 0, 16, 16);
                        if (against && direction == Direction.NORTH) {
                            face.cullface(Direction.NORTH);
                        }
                    })
                    .end();
        }

        /**
         * A lens and a ring, both with no thickness, drawn on the north face of the cell.
         *
         * <p><b>Two elements rather than one picture</b> so that the ring is a single file
         * for all sixteen colours, and so the lens can be told to draw at full brightness
         * while the fitting is not. They never both cover a pixel - the lens is transparent
         * outside its disc and the ring inside it - but the ring is set a hundredth of a
         * pixel further out anyway, which costs nothing and settles the question of what
         * happens where two quads share a plane.
         *
         * <p>Both sides of each plate are drawn, as vanilla does for the same shapes: the
         * block behind can be broken, and a plate with one side is a plate you can see
         * through.
         */
        private ModelFile plate(String skin) {
            return models().getBuilder(skin)
                    .renderType("minecraft:cutout")
                    .ao(false)
                    .texture("lens", modLoc("block/" + skin))
                    .texture("ring", modLoc("block/" + Shape.SPOTLIGHT.id() + "_ring"))
                    .texture("particle", modLoc("block/" + skin))
                    .element()
                        .from(0, 0, PLATE).to(16, 16, PLATE)
                        .shade(false)
                        .face(Direction.NORTH).texture("#ring").uvs(16, 0, 0, 16).end()
                        .face(Direction.SOUTH).texture("#ring").uvs(0, 0, 16, 16).end()
                    .end()
                    .element()
                        .from(0, 0, OVER).to(16, 16, OVER)
                        .shade(false)
                        .emissivity(15, 15)
                        .face(Direction.NORTH).texture("#lens").uvs(16, 0, 0, 16).end()
                        .face(Direction.SOUTH).texture("#lens").uvs(0, 0, 16, 16).end()
                    .end();
        }

        /**
         * A box of the form's own depth, laid against the north face of the cell.
         *
         * <p>Authored on the same face as the plate, so the one rotation table turns both.
         * The face against the wall is culled: it is never seen, and a quad that is never
         * seen is a quad the game can be told not to draw.
         *
         * <p>No {@code uv} is written for any face. Left out, the game works each one out
         * from where the element is, which is what keeps the four thin sides showing a
         * strip of the texture rather than a squashed copy of the whole of it.
         */
        private ModelFile box(Shape shape, String skin) {
            return cut(models().getBuilder(skin), shape.depth(), skin, Direction.Axis.Z);
        }

        /**
         * The same form with two of it laid together: a whole block, still wearing the
         * slab's own look rather than the cube's.
         *
         * <p>Which is the whole of what it is for. A cube of this mod carries its lit face
         * on all six sides; two slabs carry the face up and down and the plain rim around
         * the four cut sides, and a course of them beside a single slab has to match.
         * It is the same builder, asked for a depth of sixteen.
         */
        private ModelFile doubled(String skin, Direction.Axis flat) {
            return cut(models().getBuilder(skin + "_double"), 16.0, skin, flat);
        }

        /**
         * A box of the form's own depth, flat against one axis.
         *
         * <p>⚠ <b>Which axis is not the same question for a block and for its item.</b>
         * A block model is always built standing against the north face, because that is
         * the face the one rotation table turns onto the other five. An item is never
         * rotated at all - what is authored is what is held - so a form that lies down in
         * the world has to be authored lying down to be held that way. See {@link #item}.
         *
         * <p>The parent carries nothing but the display transforms; without one the game
         * draws the model square-on, which for a box eight pixels deep is a flat square.
         *
         * <p>No {@code uv} is written for any face. Left out, the game works each one out
         * from where the element is, which is what keeps the four cut sides showing a
         * strip of the rim rather than a squashed copy of the whole of it.
         */
        private <T extends ModelBuilder<T>> T cut(T builder, double thickness, String skin,
                Direction.Axis flat) {
            float depth = (float) thickness;
            Direction back = Direction.fromAxisAndDirection(flat,
                    Direction.AxisDirection.NEGATIVE);
            return builder
                    .parent(new ModelFile.UncheckedModelFile("block/block"))
                    .texture("face", modLoc("block/" + skin))
                    .texture("edge", modLoc("block/" + skin + Masters.Layer.EDGE.suffix()))
                    .texture("particle", modLoc("block/" + skin))
                    .element()
                        .from(0, 0, 0)
                        .to(flat == Direction.Axis.X ? depth : 16,
                                flat == Direction.Axis.Y ? depth : 16,
                                flat == Direction.Axis.Z ? depth : 16)
                        .allFaces((direction, face) -> {
                            // The two broad sides are the lamp; the four cut ones are a rim.
                            face.texture(direction.getAxis() == flat ? "#face" : "#edge");
                            // ⚠ Every face that reaches the wall of its cell is culled,
                            // not only the one at the back. The four cut sides span the
                            // cell in their own plane and so meet a neighbour too - a
                            // course of these is a lot of quads to draw that nobody sees.
                            if (direction != back.getOpposite() || depth == 16.0F) {
                                face.cullface(direction);
                            }
                        })
                    .end();
        }

        /**
         * What the thing looks like in your hand.
         *
         * <p>⚠ <b>A plate cannot be its own item model.</b> Held or in a slot the block
         * model is seen at an angle from which a shape with no thickness is a line. The
         * game gives its own flat blocks a flat item instead, and so does this: the layers
         * of the texture, stacked, exactly as {@code glow_lichen} does it.
         */
        private void item(Lamp lamp, ModelFile model) {
            switch (lamp.shape()) {
                // A box has thickness and reads perfectly well held at an angle.
                case LAMP, VERTICAL_SLAB, VERTICAL_PANEL, FIXTURE, ROD, CASED ->
                        itemModels().withExistingParent(lamp.id(), model.getLocation());
                case BULB -> itemModels().withExistingParent(lamp.id(),
                        bulb(lamp.skin(true) + "_held", lamp.skin(true),
                                (float) lamp.shape().fit(Direction.NORTH).wide(),
                                (float) lamp.shape().fit(Direction.NORTH).tall(),
                                (float) lamp.shape().fit(Direction.NORTH).deep(),
                                true).getLocation());
                // ⚠ A slab that lies down has to be authored lying down. The block
                // model of every plate stands against the north face so that one rotation
                // table can turn it onto any other, and the blockstate does the turning -
                // but an item is never turned, so reusing the block model handed back a
                // standing plate for the lying form and the standing one alike, and the
                // two were the same picture in the hand.
                case SLAB, PANEL -> cut(itemModels().getBuilder(lamp.id()),
                        lamp.shape().depth(), lamp.skin(true), Direction.Axis.Y);
                // The ring is the base and the lens goes over it, the same way round as
                // the two elements of the block model.
                case SPOTLIGHT -> itemModels().getBuilder(lamp.id())
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", modLoc("block/" + Shape.SPOTLIGHT.id() + "_ring"))
                        .texture("layer1", modLoc("block/" + lamp.skin(true)));
            }
        }
    }

    /** The name of every lamp, and of the tab, built from the parts the id is built from. */
    private static class Language extends LanguageProvider {
        Language(PackOutput output) {
            super(output, Laterna.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add("itemGroup." + Laterna.MODID, "Laterna");
            for (Lamp lamp : Lamp.all()) {
                add(LaternaRegistry.block(lamp).get(), lamp.displayName());
            }
        }
    }

    /** Each lamp drops itself; there is nothing else it could reasonably do. */
    private static class Loot extends LootTableProvider {
        Loot(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, Set.of(),
                    List.of(new LootTableProvider.SubProviderEntry(Blocks::new,
                            LootContextParamSets.BLOCK)),
                    registries);
        }

        private static class Blocks extends BlockLootSubProvider {
            Blocks(HolderLookup.Provider registries) {
                super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
            }

            @Override
            protected void generate() {
                for (Lamp lamp : Lamp.all()) {
                    Block block = LaternaRegistry.block(lamp).get();
                    // ⚠ A stacked pair has to give both back, or a doubled slab is
                    // quietly worth half what went into it. The game keeps a table for
                    // exactly this, but it asks the block for vanilla's own slab property
                    // - which the standing form does not have - so the standing one is
                    // written out with the property it does keep.
                    add(block, switch (lamp.shape()) {
                        case SLAB -> pair(block, StatePropertiesPredicate.Builder.properties()
                                .hasProperty(StackingPlateBlock.TYPE, SlabType.DOUBLE.getSerializedName()));
                        case VERTICAL_SLAB -> pair(block,
                                StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(UprightStackingPlateBlock.DOUBLE, true));
                        case LAMP, SPOTLIGHT, PANEL, VERTICAL_PANEL, BULB, FIXTURE, ROD, CASED ->
                                createSingleItemTable(block);
                    });
                }
            }

            /** Vanilla's slab table, with which state counts as stacked left open. */
            private LootTable.Builder pair(Block block, StatePropertiesPredicate.Builder whole) {
                return LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(applyExplosionDecay(block, LootItem.lootTableItem(block)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))
                                        .when(LootItemBlockStatePropertyCondition
                                                .hasBlockStateProperties(block)
                                                .setProperties(whole))))));
            }

            /**
             * ⚠ <b>Ours only.</b> Left alone this is every block in the game, and the
             * validation that follows then reports a missing loot table for each of
             * vanilla's - which fails the build for a reason that has nothing to do with
             * this mod.
             */
            @Override
            protected Iterable<Block> getKnownBlocks() {
                return Lamp.all().stream()
                        .map(lamp -> (Block) LaternaRegistry.block(lamp).get())
                        .toList();
            }
        }
    }

    /**
     * Two recipes per form and wiring - the white one, and dyeing.
     *
     * <p><b>The white lamp is the only one that is made of anything.</b> Every other
     * colour is that lamp with a dye, taken through the tag for its form and wiring so
     * that the recipe never names a colour and dyeing an inverted lamp gives back an
     * inverted lamp. Sixteen colours cost one recipe each and would cost the same if
     * there were thirty.
     *
     * <p>The two wirings differ by one ingredient, dust against a torch, which is also
     * the difference between them in the world.
     */
    private static class Recipes extends RecipeProvider {
        Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            for (Shape shape : Shape.values()) {
                for (Frame frame : shape.frames()) {
                    for (Wiring wiring : shape.wirings()) {
                        base(output, new Lamp(shape, wiring, frame, DyeColor.WHITE));
                    }
                }
            }
            for (Lamp lamp : Lamp.all()) {
                if (lamp.colour() != DyeColor.WHITE) {
                    dyeing(output, lamp);
                }
                turning(output, lamp);
            }
        }

        /**
         * What each form is made of, in white.
         *
         * <p>The cube is glowstone in a frame of stone around whichever way the redstone
         * reads - dust or a torch, which is the difference between the two wirings in the
         * world as well as on the bench. The spotlight is the same glowstone in a ring of
         * iron nuggets, which is the thing it is: a lens in a metal rim, and cheap, because
         * eight come out.
         */
        private void base(RecipeOutput output, Lamp white) {
            switch (white.shape()) {
                case LAMP -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 4)
                        .pattern("aba")
                        .pattern("bcb")
                        .pattern("aba")
                        .define('a', framing(white.frame()))
                        .define('b', Items.GLOWSTONE)
                        .define('c', white.wiring() == Wiring.INVERTED
                                ? Ingredient.of(Items.REDSTONE_TORCH)
                                : Ingredient.of(Tags.Items.DUSTS_REDSTONE)));
                case SPOTLIGHT -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 8)
                        .pattern("aaa")
                        .pattern("aba")
                        .pattern("aaa")
                        .define('a', Tags.Items.NUGGETS_IRON)
                        .define('b', Items.GLOWSTONE));
                case SLAB -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 6)
                        .pattern("bbb")
                        .pattern("aaa")
                        .define('a', framing(white.frame()))
                        .define('b', Items.GLOWSTONE));
                // Cut thinner, the way the game cuts a block into slabs, and by the same
                // arithmetic: three slabs of eight are six panels of four.
                case PANEL -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 6)
                        .pattern("aaa")
                        .define('a', LaternaRegistry.item(new Lamp(Shape.SLAB,
                                Wiring.ALWAYS, white.frame(), DyeColor.WHITE)).get()));
                // Nothing. A form that stands on its edge is the one that lies down,
                // turned - see turning() - and giving it a recipe of its own as well would
                // be two ways to make one thing, drifting apart the first time either
                // changes.
                case VERTICAL_SLAB, VERTICAL_PANEL -> {
                }
                // Stood on end rather than laid in a row, which is what tells this apart
                // from every other pattern here as well as being the shape of the thing.
                // Glass around a block of light, which is what it is.
                case CASED -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 8)
                        .pattern("aaa")
                        .pattern("aba")
                        .pattern("aaa")
                        .define('a', Tags.Items.GLASS_BLOCKS)
                        .define('b', Items.GLOWSTONE));
                case ROD -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 8)
                        .pattern("a")
                        .pattern("b")
                        .pattern("a")
                        .define('a', Tags.Items.NUGGETS_IRON)
                        .define('b', Items.GLOWSTONE));
                // A little glass on a stand, and eight of them from it.
                case BULB -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 8)
                        .pattern(" b ")
                        .pattern("aaa")
                        .define('a', Tags.Items.STONES)
                        .define('b', Items.GLOWSTONE));
                // A lens in a cross of iron, which is the ring of the spotlight opened out.
                case FIXTURE -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 8)
                        .pattern(" a ")
                        .pattern("aba")
                        .pattern(" a ")
                        .define('a', Tags.Items.NUGGETS_IRON)
                        .define('b', Items.GLOWSTONE));
            }
        }

        /**
         * One of a turnable form makes one of its twin, in every colour and both ways.
         *
         * <p>⚠ <b>This is why nothing else is a shapeless recipe of a single item.</b>
         * Two such recipes taking the same one item are ambiguous, and the game picks
         * between them by whichever it happened to load first. Reserving that shape of
         * recipe for turning is what keeps every other one unmistakable - it is the reason
         * the panel is three slabs in a row rather than one slab split.
         */
        private void turning(RecipeOutput output, Lamp lamp) {
            lamp.shape().turned().ifPresent(other -> ShapelessRecipeBuilder
                    .shapeless(RecipeCategory.DECORATIONS, LaternaRegistry.item(lamp).get())
                    .requires(LaternaRegistry.item(
                            new Lamp(other, lamp.wiring(), lamp.frame(), lamp.colour())).get())
                    .unlockedBy("has_glowstone", has(Items.GLOWSTONE))
                    .save(output, ResourceLocation.fromNamespaceAndPath(
                            Laterna.MODID, lamp.id() + "_from_turning")));
        }

        /** Saved the same way whatever the pattern was, so the arms above stay patterns. */
        /**
         * What the frame of a lamp is built out of.
         *
         * <p>⚠ <b>The material is what tells the three families apart on the bench.</b>
         * Dye is already spoken for - it says which colour - and a single item is reserved
         * for turning a slab on its edge, so neither could also mean "and fix the frame".
         * Building a black frame out of blackstone and a white one out of quartz needs no
         * marker at all: the frame is made of what it looks like it is made of, and the
         * three recipes cannot be confused with one another.
         */
        private Ingredient framing(Frame frame) {
            return switch (frame) {
                case OWN -> Ingredient.of(Tags.Items.STONES);
                case BLACK -> Ingredient.of(Items.BLACKSTONE, Items.POLISHED_BLACKSTONE);
                case WHITE -> Ingredient.of(Items.QUARTZ_BLOCK, Items.SMOOTH_QUARTZ);
            };
        }

        private void raw(RecipeOutput output, Lamp white, ShapedRecipeBuilder builder) {
            builder.unlockedBy("has_glowstone", has(Items.GLOWSTONE)).save(output, name(white));
        }

        /** Eight of a kind around one dye, the way the game dyes glass. */
        private void dyeing(RecipeOutput output, Lamp lamp) {
            ShapedRecipeBuilder
                    .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(lamp).get(), 8)
                    .pattern("aaa")
                    .pattern("aba")
                    .pattern("aaa")
                    .define('a',
                            LaternaTags.items(lamp.shape(), lamp.wiring(), lamp.frame()))
                    .define('b', DyeItem.byColor(lamp.colour()))
                    .unlockedBy("has_glowstone", has(Items.GLOWSTONE))
                    .save(output, name(lamp));
        }

        private static ResourceLocation name(Lamp lamp) {
            return ResourceLocation.fromNamespaceAndPath(Laterna.MODID, lamp.id());
        }
    }

    /** One tag per form and wiring, holding all sixteen colours of it. */
    private static class BlockTags extends BlockTagsProvider {
        BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                ExistingFileHelper existingFileHelper) {
            super(output, registries, Laterna.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            for (Lamp lamp : Lamp.all()) {
                tag(LaternaTags.blocks(lamp.shape(), lamp.wiring(), lamp.frame()))
                        .add(LaternaRegistry.block(lamp).get());
            }
        }
    }

    /** The same tags again as items, because a recipe takes items and not blocks. */
    private static class ItemTags extends ItemTagsProvider {
        ItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
                ExistingFileHelper existingFileHelper) {
            super(output, registries, blockTags, Laterna.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            for (Shape shape : Shape.values()) {
                for (Frame frame : shape.frames()) {
                    for (Wiring wiring : shape.wirings()) {
                        copy(LaternaTags.blocks(shape, wiring, frame),
                                LaternaTags.items(shape, wiring, frame));
                    }
                }
            }
        }
    }
}
