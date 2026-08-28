package io.github.capsicum0907.laterna.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import io.github.capsicum0907.laterna.Lamp;
import io.github.capsicum0907.laterna.LampBlock;
import io.github.capsicum0907.laterna.Laterna;
import io.github.capsicum0907.laterna.LaternaRegistry;
import io.github.capsicum0907.laterna.LaternaTags;
import io.github.capsicum0907.laterna.Shape;
import io.github.capsicum0907.laterna.PlateBlock;
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
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
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
                for (DyeColor colour : DyeColor.values()) {
                    for (boolean lit : new boolean[] { true, false }) {
                        if (!lit && !shape.switched()) {
                            continue;
                        }
                        String skin = Lamp.skin(shape, colour, lit);
                        models.put(skin, shaped(shape, skin));
                    }
                }
            }

            for (Lamp lamp : Lamp.all()) {
                Block block = LaternaRegistry.block(lamp).get();
                ModelFile model = models.get(lamp.skin(lamp.litByDefault()));
                switch (lamp.shape().mount()) {
                    case NONE -> getVariantBuilder(block)
                            .partialState().with(LampBlock.LIT, true)
                            .modelForState().modelFile(models.get(lamp.skin(true))).addModel()
                            .partialState().with(LampBlock.LIT, false)
                            .modelForState().modelFile(models.get(lamp.skin(false))).addModel();
                    // One arm for every way of clinging to a face. Which states a form
                    // keeps is its own business - the plate is asked which way it faces
                    // and the answer is turned onto that face, so a fourth kind of
                    // mounting would arrive here needing nothing.
                    case ANY, FLAT, UPRIGHT -> {
                        PlateBlock plate = (PlateBlock) block;
                        getVariantBuilder(block).forAllStatesExcept(state -> {
                            int[] turn = ROTATION.get(PlateBlock.against(plate.facing(state)));
                            return ConfiguredModel.builder()
                                    .modelFile(model)
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
            };
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
            return models().getBuilder(skin)
                    .texture("face", modLoc("block/" + skin))
                    .texture("edge", modLoc("block/" + skin + Masters.Layer.EDGE.suffix()))
                    .texture("particle", modLoc("block/" + skin))
                    .element()
                        .from(0, 0, 0).to(16, 16, (float) shape.depth())
                        .allFaces((direction, face) -> {
                            // The two broad sides are the lamp; the four cut ones are a rim.
                            boolean broad = direction.getAxis() == Direction.Axis.Z;
                            face.texture(broad ? "#face" : "#edge");
                            if (direction == Direction.NORTH) {
                                face.cullface(Direction.NORTH);
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
                case LAMP, SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL ->
                        itemModels().withExistingParent(lamp.id(), model.getLocation());
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
                    dropSelf(LaternaRegistry.block(lamp).get());
                }
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
                for (Wiring wiring : shape.wirings()) {
                    base(output, new Lamp(shape, wiring, DyeColor.WHITE));
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
                        .define('a', Tags.Items.STONES)
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
                        .define('a', Tags.Items.STONES)
                        .define('b', Items.GLOWSTONE));
                // Cut thinner, the way the game cuts a block into slabs, and by the same
                // arithmetic: three slabs of eight are six panels of four.
                case PANEL -> raw(output, white, ShapedRecipeBuilder
                        .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 6)
                        .pattern("aaa")
                        .define('a', LaternaRegistry.item(
                                new Lamp(Shape.SLAB, Wiring.ALWAYS, DyeColor.WHITE)).get()));
                // Nothing. A form that stands on its edge is the one that lies down,
                // turned - see turning() - and giving it a recipe of its own as well would
                // be two ways to make one thing, drifting apart the first time either
                // changes.
                case VERTICAL_SLAB, VERTICAL_PANEL -> {
                }
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
                            new Lamp(other, lamp.wiring(), lamp.colour())).get())
                    .unlockedBy("has_glowstone", has(Items.GLOWSTONE))
                    .save(output, ResourceLocation.fromNamespaceAndPath(
                            Laterna.MODID, lamp.id() + "_from_turning")));
        }

        /** Saved the same way whatever the pattern was, so the arms above stay patterns. */
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
                    .define('a', LaternaTags.items(lamp.shape(), lamp.wiring()))
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
                tag(LaternaTags.blocks(lamp.shape(), lamp.wiring()))
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
                for (Wiring wiring : shape.wirings()) {
                    copy(LaternaTags.blocks(shape, wiring), LaternaTags.items(shape, wiring));
                }
            }
        }
    }
}
