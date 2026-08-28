package io.github.capsicum0907.laterna;

import io.github.capsicum0907.laterna.data.TestStructures;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
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
    /** Every form that clings to a face, which is every form the plate tests are about. */
    private static final Shape[] PLATES = { Shape.SPOTLIGHT, Shape.SLAB, Shape.VERTICAL_SLAB,
            Shape.PANEL, Shape.VERTICAL_PANEL };

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
     * Every plate, in every direction its own form can be mounted in, lit and pointing
     * the way it was put.
     *
     * <p>⚠ <b>What this cannot check is where the plate is drawn.</b> The rotation
     * table lives in the model generator, and a test can only see the state - so if the
     * rotations were inverted, this would pass with every light on the wrong surface.
     * That one is checked by looking.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void platesLightInEveryDirectionTheyHave(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            PlateBlock plate = plate(shape);
            for (Direction facing : plate.facings()) {
                helper.setBlock(at(shape, facing),
                        plate.withFacing(plate.defaultBlockState(), facing));
            }
        }
        helper.succeedWhen(() -> {
            for (Shape shape : PLATES) {
                PlateBlock plate = plate(shape);
                for (Direction facing : plate.facings()) {
                    BlockPos where = at(shape, facing);
                    if (plate.facing(helper.getBlockState(where)) != facing) {
                        throw new GameTestAssertException(shape + " at " + where + " turned");
                    }
                    brightness(helper, where, 15);
                }
            }
        });
    }

    /**
     * ⚠ <b>Which directions a form has at all.</b> A slab lies down and a vertical one
     * stands up, and neither can be put the other way: that is the whole reason they are
     * two blocks rather than one, and it is what lets each be placed the way its own
     * shape is expected to be.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void eachFormIsMountedItsOwnWay(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            Collection<Direction> facings = plate(shape).facings();
            Collection<Direction> wanted = switch (shape.mount()) {
                case ANY -> List.of(Direction.values());
                case FLAT -> List.of(Direction.UP, Direction.DOWN);
                case UPRIGHT -> Direction.Plane.HORIZONTAL.stream().toList();
                case NONE -> throw new GameTestAssertException(shape + " is not a plate");
            };
            if (!Set.copyOf(facings).equals(Set.copyOf(wanted))) {
                throw new GameTestAssertException(
                        shape + " can be mounted " + facings + ", wanted " + wanted);
            }
        }
        helper.succeed();
    }

    /**
     * ⚠ <b>The outline sits on the face the plate is drawn on, for every direction and
     * every depth.</b>
     *
     * <p>This is the test that was missing. The six shapes used to be a list written out
     * by hand, copied from another mod with east and west swapped in the copying, and the
     * outline of every east- or west-facing light was a block away from the light. Five
     * of the six were right, which is why nothing looked wrong until one was pointed at.
     *
     * <p>It cannot see where the model was drawn - but the model's rotation and this
     * shape now come from the same {@link PlateBlock#against}, so a mistake there moves
     * both together instead of pulling them apart.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void plateOutlinesAreWhereThePlatesAre(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            PlateBlock plate = plate(shape);
            for (Direction facing : plate.facings()) {
                helper.setBlock(WHERE, plate.withFacing(plate.defaultBlockState(), facing));
                Direction against = PlateBlock.against(facing);
                VoxelShape outline = helper.getBlockState(WHERE)
                        .getShape(helper.getLevel(), helper.absolutePos(WHERE));
                boolean high = against.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                double near = high
                        ? outline.min(against.getAxis())
                        : outline.max(against.getAxis());
                double depth = shape.depth() / 16.0;
                double wanted = high ? 1.0 - depth : depth;
                if (Math.abs(near - wanted) > 1.0E-6) {
                    throw new GameTestAssertException(shape + " facing " + facing
                            + " should hug its " + against + " face, but its outline reaches "
                            + near);
                }
            }
        }
        helper.succeed();
    }

    /**
     * ⚠ <b>The two shapes a plate has, and where they part company.</b> A
     * {@code VoxelShape} of no thickness is a block that cannot be selected or broken, so
     * every plate keeps one to be pointed at. What differs is whether there is anything
     * to stand on: recessed means flush, so the spotlight has no collision at all, while
     * a slab is a step and is supposed to be.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void onlyTheRecessedOneIsNotAStep(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            helper.setBlock(WHERE, plate(shape));
            BlockState state = helper.getBlockState(WHERE);
            BlockPos absolute = helper.absolutePos(WHERE);
            if (state.getShape(helper.getLevel(), absolute).isEmpty()) {
                throw new GameTestAssertException(shape + " cannot be pointed at");
            }
            boolean step = !state.getCollisionShape(helper.getLevel(), absolute).isEmpty();
            if (step != (shape != Shape.SPOTLIGHT)) {
                throw new GameTestAssertException(shape + " is "
                        + (step ? "something" : "nothing") + " to stand on, which is wrong");
            }
        }
        helper.succeed();
    }

    /** Placed in water they hold the water, rather than leaving a bubble in it. */
    @GameTest(template = TestStructures.FLOOR)
    public static void platesHoldWater(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            helper.setBlock(WHERE, plate(shape).defaultBlockState()
                    .setValue(PlateBlock.WATERLOGGED, true));
            if (!helper.getBlockState(WHERE).getFluidState().is(Fluids.WATER)) {
                throw new GameTestAssertException("the water went missing under a " + shape);
            }
        }
        helper.succeed();
    }

    /**
     * Two slabs laid together are one whole block, and are still worth two.
     *
     * <p>The shape is the cell, not half of it, and the loot table for a stacked form is
     * the game's own - writing the ordinary one would quietly halve what a doubled slab
     * is worth.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void stackedSlabsFillTheirCell(GameTestHelper helper) {
        StackingPlateBlock slab = (StackingPlateBlock) plate(Shape.SLAB);
        helper.setBlock(WHERE, slab.defaultBlockState()
                .setValue(StackingPlateBlock.TYPE, SlabType.DOUBLE));
        BlockState state = helper.getBlockState(WHERE);
        if (!slab.whole(state)) {
            throw new GameTestAssertException("a stacked slab does not know it is whole");
        }
        if (!Block.isShapeFullBlock(state.getShape(helper.getLevel(),
                helper.absolutePos(WHERE)))) {
            throw new GameTestAssertException("a stacked slab is not a whole block");
        }
        helper.succeed();
    }

    /**
     * ⚠ <b>Only the stacked one stops light.</b> Every other plate is declared
     * {@code noOcclusion}, because a thin thing is not a wall - but two slabs together
     * are a whole block, and a room walled with them would be lit straight through if
     * they were not asked to occlude.
     *
     * <p>Checked as the light each state blocks rather than by building a room and
     * looking at the far side: the number is the thing that decides it, and a probe in an
     * open room measures how light goes round a wall as much as through it.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void onlyAStackedSlabStopsLight(GameTestHelper helper) {
        StackingPlateBlock slab = (StackingPlateBlock) plate(Shape.SLAB);
        BlockPos absolute = helper.absolutePos(WHERE);
        for (SlabType type : SlabType.values()) {
            helper.setBlock(WHERE,
                    slab.defaultBlockState().setValue(StackingPlateBlock.TYPE, type));
            int blocked = helper.getBlockState(WHERE).getLightBlock(helper.getLevel(), absolute);
            if ((blocked >= 15) != (type == SlabType.DOUBLE)) {
                throw new GameTestAssertException("a " + type + " slab blocks " + blocked
                        + ", which is the wrong answer for it");
            }
        }
        helper.succeed();
    }

    private static PlateBlock plate(Shape shape) {
        return (PlateBlock) LaternaRegistry.block(
                new Lamp(shape, Wiring.ALWAYS, DyeColor.WHITE)).get();
    }

    /** A spot of its own for each form and face, so one test can hold all of them. */
    private static BlockPos at(Shape shape, Direction facing) {
        return new BlockPos(1 + facing.ordinal(), 1 + shape.ordinal(), 1);
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
