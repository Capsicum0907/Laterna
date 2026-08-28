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
            Shape.PANEL, Shape.VERTICAL_PANEL, Shape.BULB, Shape.FIXTURE };

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
     * Every plate, in every direction its own form can be mounted in, giving off light and
     * pointing the way it was put.
     *
     * <p>⚠ <b>What this cannot check is where the plate is drawn.</b> The rotation
     * table lives in the model generator, and a test can only see the state - so if the
     * rotations were inverted, this would pass with every light on the wrong surface.
     * That one is checked by looking.
     *
     * <p>The light asked for here is what the block gives off, not what has reached the
     * air around it: one is true the moment the block exists and the other takes a tick to
     * spread. That it spreads at all is {@link #aPlateLightsTheRoom}.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void platesLightInEveryDirectionTheyHave(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            PlateBlock plate = plate(shape);
            for (Direction facing : plate.facings()) {
                BlockState state = put(helper, plate, facing);
                if (plate.facing(state) != facing) {
                    throw new GameTestAssertException(shape + " facing " + facing + " turned");
                }
                int given = state.getLightEmission(helper.getLevel(), helper.absolutePos(WHERE));
                if (given != 15) {
                    throw new GameTestAssertException(shape + " facing " + facing
                            + " gives off " + given);
                }
                clear(helper, facing);
            }
        }
        helper.succeed();
    }

    /** And the light does reach the air, which the one above deliberately does not wait for. */
    @GameTest(template = TestStructures.FLOOR)
    public static void aPlateLightsTheRoom(GameTestHelper helper) {
        put(helper, plate(Shape.SPOTLIGHT), Direction.UP);
        helper.succeedWhen(() -> brightness(helper, WHERE.above(), 14));
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
                case NONE, AXIS -> throw new GameTestAssertException(shape + " is not a plate");
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
                BlockState state = put(helper, plate, facing);
                Direction against = PlateBlock.against(facing);
                VoxelShape outline = state.getShape(helper.getLevel(),
                        helper.absolutePos(WHERE));
                boolean high = against.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                double near = high
                        ? outline.min(against.getAxis())
                        : outline.max(against.getAxis());
                double depth = shape.fit(facing).deep() / 16.0;
                double wanted = high ? 1.0 - depth : depth;
                if (Math.abs(near - wanted) > 1.0E-6) {
                    throw new GameTestAssertException(shape + " facing " + facing
                            + " should hug its " + against + " face, but its outline reaches "
                            + near);
                }
                clear(helper, facing);
            }
        }
        helper.succeed();
    }

    /**
     * ⚠ <b>The two shapes a plate has, and where they part company.</b> A
     * {@code VoxelShape} of no thickness is a block that cannot be selected or broken, so
     * every plate keeps one to be pointed at. What differs is whether there is anything
     * to stand on: recessed means flush, so the spotlight has no collision at all, while
     * a slab is a step and a bulb is something to bump into.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void onlyTheRecessedOneIsNotAStep(GameTestHelper helper) {
        BlockPos absolute = helper.absolutePos(WHERE);
        for (Shape shape : PLATES) {
            PlateBlock plate = plate(shape);
            BlockState state = put(helper, plate, plate.facings().iterator().next());
            if (state.getShape(helper.getLevel(), absolute).isEmpty()) {
                throw new GameTestAssertException(shape + " cannot be pointed at");
            }
            boolean step = !state.getCollisionShape(helper.getLevel(), absolute).isEmpty();
            if (step != (shape != Shape.SPOTLIGHT)) {
                throw new GameTestAssertException(shape + " is "
                        + (step ? "something" : "nothing") + " to stand on, which is wrong");
            }
            clear(helper, plate.facings().iterator().next());
        }
        helper.succeed();
    }

    /** Placed in water they hold the water, rather than leaving a bubble in it. */
    @GameTest(template = TestStructures.FLOOR)
    public static void platesHoldWater(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            PlateBlock plate = plate(shape);
            Direction facing = plate.facings().iterator().next();
            helper.setBlock(WHERE.relative(PlateBlock.against(facing)), Blocks.STONE);
            helper.setBlock(WHERE, plate.withFacing(plate.defaultBlockState(), facing)
                    .setValue(PlateBlock.WATERLOGGED, true));
            if (!helper.getBlockState(WHERE).getFluidState().is(Fluids.WATER)) {
                throw new GameTestAssertException("the water went missing under a " + shape);
            }
            clear(helper, facing);
        }
        helper.succeed();
    }

    /**
     * ⚠ <b>Anything mounted on a face falls with that face; anything that lies or
     * stands stays.</b> Taking a wall down should take its lights with it rather than
     * leaving a field of them hanging to be knocked out one at a time - and a bulb or a
     * fitting is as plainly attached to its wall as a recessed light is. A slab or a panel
     * is a thing in its own right, and pulling the block behind one is not a reason for it
     * to go.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void onlyTheRecessedOneFallsWithItsWall(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            PlateBlock plate = plate(shape);
            Direction facing = plate.facings().iterator().next();
            put(helper, plate, facing);
            helper.setBlock(WHERE.relative(PlateBlock.against(facing)), Blocks.AIR);
            boolean gone = helper.getBlockState(WHERE).isAir();
            if (gone != (shape.mount() == Shape.Mount.ANY)) {
                throw new GameTestAssertException(shape + (gone ? " fell" : " stayed up")
                        + " when its wall went, which is the wrong answer for it");
            }
            clear(helper, facing);
        }
        helper.succeed();
    }

    /**
     * A plate at {@link #WHERE}, with something behind it to cling to.
     *
     * <p>⚠ <b>The support is not decoration.</b> A recessed light falls when what it
     * was set into goes, so one placed into thin air is gone before it can be looked at -
     * which is how these tests found out that they had been placing everything in mid-air.
     */
    private static BlockState put(GameTestHelper helper, PlateBlock plate, Direction facing) {
        helper.setBlock(WHERE.relative(PlateBlock.against(facing)), Blocks.STONE);
        helper.setBlock(WHERE, plate.withFacing(plate.defaultBlockState(), facing));
        BlockState state = helper.getBlockState(WHERE);
        if (state.isAir()) {
            throw new GameTestAssertException("the plate did not stay where it was put");
        }
        return state;
    }

    private static void clear(GameTestHelper helper, Direction facing) {
        helper.setBlock(WHERE, Blocks.AIR);
        helper.setBlock(WHERE.relative(PlateBlock.against(facing)), Blocks.AIR);
    }

    /** Every form that stacks, which is the slab lying down and the slab standing up. */
    private static final Shape[] STACKING = { Shape.SLAB, Shape.VERTICAL_SLAB };

    /**
     * Two of a stacking form laid together are one whole block.
     *
     * <p>The shape is the cell, not part of it, and the loot table for a stacked form
     * gives back the pair - writing the ordinary one would quietly halve what a doubled
     * slab is worth.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void stackedSlabsFillTheirCell(GameTestHelper helper) {
        for (Shape shape : STACKING) {
            PlateBlock plate = plate(shape);
            helper.setBlock(WHERE, plate.asWhole(plate.defaultBlockState()));
            BlockState state = helper.getBlockState(WHERE);
            if (!plate.whole(state)) {
                throw new GameTestAssertException("a stacked " + shape + " is not whole");
            }
            if (!Block.isShapeFullBlock(state.getShape(helper.getLevel(),
                    helper.absolutePos(WHERE)))) {
                throw new GameTestAssertException(
                        "a stacked " + shape + " does not fill its cell");
            }
        }
        helper.succeed();
    }

    /**
     * ⚠ <b>Only a stacked plate stops light.</b> Every other one is declared
     * {@code noOcclusion}, because a thin thing is not a wall - but two laid together are
     * a whole block, and a room walled with them would be lit straight through if they
     * were not asked to occlude.
     *
     * <p>Checked as the light each state blocks rather than by building a room and
     * looking at the far side: the number is the thing that decides it, and a probe in an
     * open room measures how light goes round a wall as much as through it.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void onlyAStackedSlabStopsLight(GameTestHelper helper) {
        BlockPos absolute = helper.absolutePos(WHERE);
        for (Shape shape : STACKING) {
            PlateBlock plate = plate(shape);
            for (BlockState state : new BlockState[] {
                    plate.defaultBlockState(), plate.asWhole(plate.defaultBlockState()) }) {
                helper.setBlock(WHERE, state);
                int blocked = helper.getBlockState(WHERE)
                        .getLightBlock(helper.getLevel(), absolute);
                if ((blocked >= 15) != plate.whole(state)) {
                    throw new GameTestAssertException("a " + (plate.whole(state)
                            ? "stacked " : "single ") + shape + " blocks " + blocked
                            + ", which is the wrong answer for it");
                }
            }
        }
        helper.succeed();
    }

    /**
     * ⚠ <b>A standing pair keeps the axis it was stacked along.</b> Two lying slabs are
     * the same cube whichever way you built them, but a standing pair has its lit faces
     * north and south, or east and west - and the model is chosen from the facing. Losing
     * the facing when the second one goes in would turn every stacked wall the same way.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aStandingPairRemembersItsAxis(GameTestHelper helper) {
        PlateBlock plate = plate(Shape.VERTICAL_SLAB);
        for (Direction facing : plate.facings()) {
            helper.setBlock(WHERE, plate.asWhole(
                    plate.withFacing(plate.defaultBlockState(), facing)));
            Direction kept = plate.facing(helper.getBlockState(WHERE));
            if (kept.getAxis() != facing.getAxis()) {
                throw new GameTestAssertException("a pair stacked along " + facing.getAxis()
                        + " came back along " + kept.getAxis());
            }
        }
        helper.succeed();
    }

    /**
     * The outline of a form is the size that form says it is on the face it is on.
     *
     * <p>⚠ <b>What this does not check is that the inset matches the drawing.</b> Both
     * the outline and the number it is compared against come from {@code Shape}, so a form
     * whose inset is simply wrong would pass. What it does catch is the block ignoring the
     * inset - which is how a spotlight drawn eight pixels across came to have an outline
     * of the whole sixteen, so that pointing anywhere near one caught it. Whether the
     * number is the right one is settled by looking, like the rotation table.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void anOutlineIsNoWiderThanItsLamp(GameTestHelper helper) {
        for (Shape shape : PLATES) {
            PlateBlock plate = plate(shape);
            Direction facing = plate.facings().iterator().next();
            BlockState state = put(helper, plate, facing);
            VoxelShape outline = state.getShape(helper.getLevel(), helper.absolutePos(WHERE));
            Direction.Axis deep = PlateBlock.against(facing).getAxis();
            Shape.Fit fit = shape.fit(facing);
            for (Direction.Axis axis : Direction.Axis.values()) {
                double wanted = (axis == deep ? fit.deep()
                        : deep == Direction.Axis.Y || axis != Direction.Axis.Y
                                ? fit.wide() : fit.tall()) / 16.0;
                double across = outline.max(axis) - outline.min(axis);
                if (Math.abs(across - wanted) > 1.0E-6) {
                    throw new GameTestAssertException(shape + " is " + across
                            + " across on " + axis + ", wanted " + wanted);
                }
            }
            clear(helper, facing);
        }
        helper.succeed();
    }

    /**
     * A rod runs the whole length of its cell along the axis it was given, and is thin
     * across it.
     *
     * <p>⚠ <b>Full length is the point of the form.</b> A bar that stopped short would
     * leave a gap at every block boundary, and a row of them would read as dashes rather
     * than as one line - which is the only reason to have a strip light rather than more
     * fittings.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aRodRunsTheLengthOfItsCell(GameTestHelper helper) {
        RodBlock rod = (RodBlock) LaternaRegistry.block(
                new Lamp(Shape.ROD, Wiring.ALWAYS, DyeColor.WHITE)).get();
        double thin = (16.0 - 2 * Shape.ROD.inset()) / 16.0;
        for (Direction.Axis along : Direction.Axis.values()) {
            helper.setBlock(WHERE, rod.defaultBlockState().setValue(RodBlock.AXIS, along));
            BlockState state = helper.getBlockState(WHERE);
            if (state.getLightEmission(helper.getLevel(), helper.absolutePos(WHERE)) != 15) {
                throw new GameTestAssertException("a rod along " + along + " gives no light");
            }
            VoxelShape outline = state.getShape(helper.getLevel(), helper.absolutePos(WHERE));
            for (Direction.Axis axis : Direction.Axis.values()) {
                double span = outline.max(axis) - outline.min(axis);
                double wanted = axis == along ? 1.0 : thin;
                if (Math.abs(span - wanted) > 1.0E-6) {
                    throw new GameTestAssertException("a rod along " + along + " spans "
                            + span + " on " + axis + ", wanted " + wanted);
                }
            }
        }
        helper.succeed();
    }

    /** ⚠ <b>And nothing takes a rod down</b>, because it was mounted on nothing. */
    @GameTest(template = TestStructures.FLOOR)
    public static void aRodClingsToNothing(GameTestHelper helper) {
        helper.setBlock(WHERE.below(), Blocks.STONE);
        helper.setBlock(WHERE, LaternaRegistry.block(
                new Lamp(Shape.ROD, Wiring.ALWAYS, DyeColor.WHITE)).get());
        helper.setBlock(WHERE.below(), Blocks.AIR);
        if (helper.getBlockState(WHERE).isAir()) {
            throw new GameTestAssertException("the rod fell, and it was holding on to nothing");
        }
        helper.succeed();
    }

    private static PlateBlock plate(Shape shape) {
        return (PlateBlock) LaternaRegistry.block(
                new Lamp(shape, Wiring.ALWAYS, DyeColor.WHITE)).get();
    }

    /** A spot of its own for each form and face, so one test can hold all of them. */
    private static BlockPos at(Shape shape, Direction facing) {
        return new BlockPos(1 + facing.ordinal(), 1 + shape.ordinal() % 3,
                1 + shape.ordinal() / 3);
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
