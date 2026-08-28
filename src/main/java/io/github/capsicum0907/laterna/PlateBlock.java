package io.github.capsicum0907.laterna;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A lamp that clings to a face, in whichever way its form is mounted.
 *
 * <p><b>What every plate has in common is here; how it is mounted is not.</b> All of them
 * sit against one face of their own cell, hold water, and take their depth from
 * {@link Shape#depth}. What differs is which faces they can sit on and how a click is
 * read - and that difference is not a setting but a different set of block states, so it
 * is a subclass each: {@link FacePlateBlock} for any of the six, {@link FlatPlateBlock}
 * for the two that lie down, {@link UprightPlateBlock} for the four that stand up.
 *
 * <p><b>Everything above this line reads a plate through {@link #facing}.</b> The model
 * generator, the outline and the tests all ask a plate which way it faces rather than
 * which property it keeps that in, so a form added with a fourth kind of mounting needs
 * no change anywhere but here.
 *
 * <p><b>What is drawn and what can be touched are two different things.</b> The
 * spotlight's model is a plate - an element whose {@code from} and {@code to} agree on one
 * axis, which is what the game's own {@code glow_lichen}, {@code vine} and
 * {@code lily_pad} are. A {@link VoxelShape} of no thickness, though, is a block that
 * cannot be pointed at, selected or broken, so its shape is one pixel deep while its
 * drawing is flat.
 *
 * <p>{@code facing} is the direction the light shines, so the plate sits against the
 * opposite face of its own cell - lying on a floor it faces up, and the plate is at the
 * bottom of the cell.
 *
 * <p>⚠ <b>Whether it needs something to cling to is not decided.</b> Break the wall
 * behind one and it stays where it is. The game's own {@code glow_lichen} pops off;
 * Simply Light's thin lamps do not bother.
 */
public abstract class PlateBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final Map<Direction, VoxelShape> shapes;

    protected PlateBlock(Shape shape, Properties properties) {
        super(properties);
        this.shapes = shapes(shape);
    }

    /** Which way this plate shines, whatever property its form keeps that in. */
    public abstract Direction facing(BlockState state);

    /** The same, the other way round, for anything building states rather than reading them. */
    public abstract BlockState withFacing(BlockState state, Direction facing);

    /** Every direction this form can be mounted in, which is not always all six. */
    public abstract Collection<Direction> facings();

    /**
     * Whether this state fills its cell rather than clinging to one face of it.
     *
     * <p>True only of a stacked pair - see {@link StackingPlateBlock}. Everything that
     * turns a plate onto a face asks this first, because a whole block has no face to be
     * turned onto.
     */
    public boolean whole(BlockState state) {
        return false;
    }

    /**
     * The same state with two of the plate laid together, for anything building states
     * rather than reading them.
     *
     * <p>Refused rather than ignored by a form that does not stack: quietly handing back
     * a half slab would let a test of stacking pass without stacking anything.
     */
    public BlockState asWhole(BlockState state) {
        throw new UnsupportedOperationException(this + " does not stack");
    }

    /**
     * Whether a click landed past the middle of the cell it is building in, along one axis.
     *
     * <p>Vanilla's expression with the axis left open, shared so that the rule for which
     * half a slab lands in and the rule for when a second slab stacks cannot drift apart -
     * they are the same question asked twice.
     */
    protected static boolean beyondHalf(BlockPlaceContext context, Direction.Axis axis) {
        BlockPos pos = context.getClickedPos();
        return axis.choose(context.getClickLocation().x - pos.getX(),
                context.getClickLocation().y - pos.getY(),
                context.getClickLocation().z - pos.getZ()) > 0.5;
    }

    /** The same question for the one axis a lying slab knows about. */
    protected static boolean upperHalf(BlockPlaceContext context) {
        return beyondHalf(context, Direction.Axis.Y);
    }

    /**
     * Whether a second plate lands in the cell that is already half full, rather than in
     * the next one along.
     *
     * <p><b>Vanilla's rule for a slab, with the axis left open.</b> Theirs reads: a bottom
     * slab takes another from above, or from a side click that landed high; a top one from
     * below, or from a side click that landed low. Written out for the vertical forms as
     * well it becomes this - the open side is whichever end of the axis is still empty,
     * and a click on a face across the axis goes by which half it landed in.
     *
     * ⚠ <b>Not paraphrased.</b> Getting this subtly wrong gives "sometimes the second
     * one replaces instead of stacking", which is the same kind of wrongness under the
     * hand that splitting the forms was meant to fix.
     *
     * @param low whether what is already there occupies the low end of the axis
     */
    protected static boolean stacksInto(BlockPlaceContext context, Direction.Axis axis,
            boolean low) {
        if (!context.replacingClickedOnBlock()) {
            return true;
        }
        Direction open = Direction.fromAxisAndDirection(axis, low
                ? Direction.AxisDirection.POSITIVE
                : Direction.AxisDirection.NEGATIVE);
        Direction clicked = context.getClickedFace();
        return clicked == open
                || (clicked.getAxis() != axis && beyondHalf(context, axis) == low);
    }

    /**
     * A slab of the cell hugging the face the plate sits on, worked out rather than listed.
     *
     * <p>⚠ <b>Listing the six by hand is how this went wrong the first time.</b> The
     * boxes were transcribed from another mod and east and west were swapped in the
     * copying, which put the outline of every east- or west-facing light on the far side
     * of its own block - a whole block away from the thing you were pointing at. The five
     * others were right, so nothing about the shape of the mistake suggested itself.
     * Derived from the direction, there is no list to get out of order.
     */
    private static Map<Direction, VoxelShape> shapes(Shape shape) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            Direction against = against(facing);
            Shape.Fit fit = shape.fit(facing);
            Direction.Axis deep = against.getAxis();
            boolean high = against.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            double[] low = new double[3];
            double[] top = new double[3];
            for (Direction.Axis axis : Direction.Axis.values()) {
                int at = axis.ordinal();
                if (axis == deep) {
                    // Out of the face it sits on, from that face inwards.
                    low[at] = high ? 16.0 - fit.deep() : 0.0;
                    top[at] = high ? 16.0 : fit.deep();
                } else {
                    // Across the face, centred on it.
                    double span = across(axis, deep, fit);
                    low[at] = (16.0 - span) / 2.0;
                    top[at] = low[at] + span;
                }
            }
            shapes.put(facing, Block.box(low[0], low[1], low[2], top[0], top[1], top[2]));
        }
        return Map.copyOf(shapes);
    }

    /**
     * How far a form reaches along one of the two axes across the face it is on.
     *
     * <p>Up the face is {@code tall} and along it is {@code wide}. On a floor or a ceiling
     * there is no up, and a form is the same either way - which is why a fitting there is
     * a disc rather than a bar lying in some arbitrary direction.
     */
    private static double across(Direction.Axis axis, Direction.Axis deep, Shape.Fit fit) {
        if (deep == Direction.Axis.Y) {
            return fit.wide();
        }
        return axis == Direction.Axis.Y ? fit.tall() : fit.wide();
    }

    /**
     * Which face of its own cell the plate of a lamp facing this way sits on.
     *
     * <p>The model generator turns its one model onto that same face. Both the drawing
     * and the outline come through here, so the two cannot disagree about which side of
     * the block the thing is on.
     */
    public static Direction against(Direction facing) {
        return facing.getOpposite();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return shapes.get(facing(state));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    /** Whether the cell being built in is already water, for every form of plate. */
    protected static boolean flooded(BlockPlaceContext context) {
        return context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
    }
}
