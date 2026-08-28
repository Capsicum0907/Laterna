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
import io.github.capsicum0907.laterna.Wiring;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
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

        @Override
        protected void registerStatesAndModels() {
            Map<String, ModelFile> skins = new HashMap<>();
            for (Shape shape : Shape.values()) {
                for (DyeColor colour : DyeColor.values()) {
                    for (boolean lit : new boolean[] { true, false }) {
                        String skin = Lamp.skin(shape, colour, lit);
                        skins.put(skin, shaped(shape, skin));
                    }
                }
            }

            for (Lamp lamp : Lamp.all()) {
                Block block = LaternaRegistry.block(lamp).get();
                getVariantBuilder(block)
                        .partialState().with(LampBlock.LIT, true)
                        .modelForState().modelFile(skins.get(lamp.skin(true))).addModel()
                        .partialState().with(LampBlock.LIT, false)
                        .modelForState().modelFile(skins.get(lamp.skin(false))).addModel();
                itemModels().withExistingParent(lamp.id(),
                        modLoc("block/" + lamp.skin(lamp.litByDefault())));
            }
        }

        /** The geometry a form has. One arm today; the switch is where the next one lands. */
        private ModelFile shaped(Shape shape, String skin) {
            return switch (shape) {
                case LAMP -> models().cubeAll(skin, modLoc("block/" + skin));
            };
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
            }
        }

        /** Glowstone in a frame of stone, around whichever way the redstone reads. */
        private void base(RecipeOutput output, Lamp white) {
            ShapedRecipeBuilder builder = ShapedRecipeBuilder
                    .shaped(RecipeCategory.DECORATIONS, LaternaRegistry.item(white).get(), 4)
                    .pattern("aba")
                    .pattern("bcb")
                    .pattern("aba")
                    .define('a', Tags.Items.STONES)
                    .define('b', Items.GLOWSTONE)
                    .unlockedBy("has_glowstone", has(Items.GLOWSTONE));
            if (white.wiring() == Wiring.INVERTED) {
                builder.define('c', Items.REDSTONE_TORCH);
            } else {
                builder.define('c', Tags.Items.DUSTS_REDSTONE);
            }
            builder.save(output, name(white));
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
