package io.github.capsicum0907.laterna;

import io.github.capsicum0907.laterna.data.TestStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the two wirings are supposed to be, checked in a world rather than by reading.
 *
 * <p>Run with {@code gradlew runGameTestServer}.
 */
@GameTestHolder(Laterna.MODID)
@PrefixGameTestTemplate(false)
public final class LaternaTests {
    private static final BlockPos WHERE = new BlockPos(4, 1, 4);
    private static final BlockPos SOURCE = WHERE.north();

    private static final Lamp NORMAL = new Lamp(Shape.LAMP, Wiring.NORMAL, DyeColor.WHITE);
    private static final Lamp INVERTED = new Lamp(Shape.LAMP, Wiring.INVERTED, DyeColor.WHITE);
    private static final Lamp SPOTLIGHT = new Lamp(Shape.SPOTLIGHT, Wiring.ALWAYS, DyeColor.WHITE);

    private LaternaTests() {
    }

    /** Put down with nothing near it, a normal lamp is dark and gives off nothing. */
    @GameTest(template = TestStructures.FLOOR)
    public static void normalStartsDark(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(NORMAL).get());
        helper.succeedWhen(() -> {
            lit(helper, WHERE, false);
            brightness(helper, WHERE, 0);
        });
    }

    /**
     * The same, inverted: lit from the moment it is placed.
     *
     * <p>This is the whole of what the second block is for, and the reason it has to be a
     * second block rather than a state on the first.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void invertedStartsLit(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(INVERTED).get());
        helper.succeedWhen(() -> {
            lit(helper, WHERE, true);
            brightness(helper, WHERE, 15);
        });
    }

    /** A signal lights a normal lamp. */
    @GameTest(template = TestStructures.FLOOR)
    public static void powerLightsNormal(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(NORMAL).get());
        helper.setBlock(SOURCE, Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            lit(helper, WHERE, true);
            brightness(helper, WHERE, 15);
        });
    }

    /** The same signal puts an inverted one out, which is the only difference between them. */
    @GameTest(template = TestStructures.FLOOR)
    public static void powerDarkensInverted(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(INVERTED).get());
        helper.setBlock(SOURCE, Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            lit(helper, WHERE, false);
            brightness(helper, WHERE, 0);
        });
    }

    /**
     * ⚠ <b>Being switched is not something a lamp can carry.</b> An inverted lamp held
     * dark by a signal, taken away and put back with nothing near it, comes up lit again:
     * the state went with the block, not with the item. That is why "inverted" is a
     * property of which block it is and cannot be a property of a lamp's state.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void breakingForgetsBeingSwitched(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(INVERTED).get());
        helper.setBlock(SOURCE, Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenWaitUntil(() -> lit(helper, WHERE, false))
                .thenExecute(() -> {
                    helper.setBlock(SOURCE, Blocks.AIR);
                    helper.setBlock(WHERE, Blocks.AIR);
                    helper.setBlock(WHERE, LaternaRegistry.block(INVERTED).get());
                })
                .thenWaitUntil(() -> {
                    lit(helper, WHERE, true);
                    brightness(helper, WHERE, 15);
                })
                .thenSucceed();
    }

    /**
     * All sixteen, because the product is the claim.
     *
     * <p>A colour that failed to register, or registered under a name something else
     * already had, would show up here and nowhere else - the four tests above would pass
     * on white alone.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void everyColourLights(GameTestHelper helper) {
        for (DyeColor colour : DyeColor.values()) {
            BlockPos at = new BlockPos(colour.getId() % 8, 1 + colour.getId() / 8, 0);
            helper.setBlock(at, LaternaRegistry.block(
                    new Lamp(Shape.LAMP, Wiring.INVERTED, colour)).get());
        }
        helper.succeedWhen(() -> {
            for (DyeColor colour : DyeColor.values()) {
                BlockPos at = new BlockPos(colour.getId() % 8, 1 + colour.getId() / 8, 0);
                lit(helper, at, true);
            }
        });
    }

    /**
     * A spotlight on each of the six faces, lit and facing the way it was put.
     *
     * <p>⚠ <b>What this cannot check is where the plate is drawn.</b> The rotation table
     * lives in the model generator, and a test can only see the state - so if the
     * rotations were inverted, this would pass with every light on the wrong surface.
     * That one is checked by looking.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void spotlightLightsOnEveryFace(GameTestHelper helper) {
        for (Direction facing : Direction.values()) {
            BlockPos at = new BlockPos(1 + facing.ordinal(), 2, 1);
            helper.setBlock(at, LaternaRegistry.block(SPOTLIGHT).get().defaultBlockState()
                    .setValue(SpotlightBlock.FACING, facing));
        }
        helper.succeedWhen(() -> {
            for (Direction facing : Direction.values()) {
                BlockPos at = new BlockPos(1 + facing.ordinal(), 2, 1);
                BlockState state = helper.getBlockState(at);
                if (state.getValue(SpotlightBlock.FACING) != facing) {
                    throw new GameTestAssertException("spotlight at " + at + " turned");
                }
                brightness(helper, at, 15);
            }
        });
    }

    /**
     * ⚠ <b>The two shapes a plate has, and why they differ.</b> Nothing to walk on, so a
     * light set into a floor is flush with it; but still something to point at, because a
     * {@code VoxelShape} of no thickness is a block that cannot be selected or broken.
     * This is the trap the form is built around, and it is cheap to check.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void spotlightIsFlushButStillThere(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(SPOTLIGHT).get());
        BlockState state = helper.getBlockState(WHERE);
        BlockPos absolute = helper.absolutePos(WHERE);
        if (!state.getCollisionShape(helper.getLevel(), absolute).isEmpty()) {
            throw new GameTestAssertException("a recessed light should not be a step");
        }
        if (state.getShape(helper.getLevel(), absolute).isEmpty()) {
            throw new GameTestAssertException("a light nobody can point at cannot be broken");
        }
        helper.succeed();
    }

    /**
     * ⚠ <b>The outline sits on the face the plate is drawn on, for all six.</b>
     *
     * <p>This is the test that was missing. The six shapes used to be a list written out
     * by hand, copied from another mod with east and west swapped in the copying, and the
     * outline of every east- or west-facing light was a block away from the light. Five
     * of the six were right, which is why nothing looked wrong until one was pointed at.
     *
     * <p>It cannot see where the model was drawn - but the model's rotation and this
     * shape now come from the same {@link SpotlightBlock#against}, so a mistake there
     * moves both together instead of pulling them apart.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void spotlightOutlineIsWhereThePlateIs(GameTestHelper helper) {
        for (Direction facing : Direction.values()) {
            helper.setBlock(WHERE, LaternaRegistry.block(SPOTLIGHT).get().defaultBlockState()
                    .setValue(SpotlightBlock.FACING, facing));
            Direction against = SpotlightBlock.against(facing);
            VoxelShape shape = helper.getBlockState(WHERE)
                    .getShape(helper.getLevel(), helper.absolutePos(WHERE));
            boolean high = against.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            double near = high ? shape.min(against.getAxis()) : shape.max(against.getAxis());
            double thickness = SpotlightBlock.DEPTH / 16.0;
            double wanted = high ? 1.0 - thickness : thickness;
            if (Math.abs(near - wanted) > 1.0E-6) {
                throw new GameTestAssertException("a light facing " + facing
                        + " should hug its " + against + " face, but its outline reaches "
                        + near);
            }
        }
        helper.succeed();
    }

    /** Placed in water it holds the water, rather than leaving a bubble in it. */
    @GameTest(template = TestStructures.FLOOR)
    public static void spotlightHoldsWater(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(SPOTLIGHT).get().defaultBlockState()
                .setValue(SpotlightBlock.WATERLOGGED, true));
        helper.succeedWhen(() -> {
            if (!helper.getBlockState(WHERE).getFluidState().is(Fluids.WATER)) {
                throw new GameTestAssertException("the water went missing");
            }
            brightness(helper, WHERE, 15);
        });
    }

    private static void lit(GameTestHelper helper, BlockPos pos, boolean expected) {
        BlockState state = helper.getBlockState(pos);
        if (!state.hasProperty(LampBlock.LIT)) {
            throw new GameTestAssertException("no lamp at " + pos + ", found " + state);
        }
        if (state.getValue(LampBlock.LIT) != expected) {
            throw new GameTestAssertException(
                    "lamp at " + pos + " should be " + (expected ? "lit" : "dark"));
        }
    }

    private static void brightness(GameTestHelper helper, BlockPos pos, int expected) {
        int light = helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(pos));
        if (light != expected) {
            throw new GameTestAssertException(
                    "light at " + pos + " is " + light + ", wanted " + expected);
        }
    }
}
