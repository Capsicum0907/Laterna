package io.github.capsicum0907.laterna;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * A lamp with no thickness: a lens set flush into the face it is put on.
 *
 * <p><b>What is drawn and what can be touched are two different things.</b> The model is
 * a plate - an element whose {@code from} and {@code to} agree on one axis, which is what
 * the game's own {@code glow_lichen}, {@code vine} and {@code lily_pad} are. A
 * {@link VoxelShape} of no thickness, though, is a block that cannot be pointed at,
 * selected or broken, so the shape here is one pixel deep while the drawing is flat.
 * Getting these two confused is the whole difficulty of the form.
 *
 * <p><b>Nothing to walk on.</b> Recessed means flush, and a collision box standing a
 * sixteenth of a block proud of the floor is not flush - you would feel it underfoot. The
 * block has no collision at all; the one-pixel shape is only for pointing at it.
 *
 * <p>{@code FACING} is the direction the light shines, so the plate sits against the
 * opposite face of its own cell - put down on a floor it faces up, and the plate lies at
 * the bottom. That agrees with how Simply Light's thin lamps read the same property.
 *
 * <p>⚠ <b>Whether it needs something to cling to is not decided.</b> Break the wall
 * behind one and it stays where it is. The game's own {@code glow_lichen} pops off;
 * Simply Light's thin lamps do not bother. Leaving it floating degrades gracefully here,
 * because both sides of the plate are drawn, so there is nothing to see through.
 */
public class SpotlightBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Deep enough to point at, shallow enough not to be a step. */
    static final double DEPTH = 1.0;

    private static final Map<Direction, VoxelShape> SHAPES = shapes();

    public SpotlightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(WATERLOGGED, false));
    }

    /**
     * A slab of the cell hugging the face the plate is drawn on, worked out rather than
     * listed.
     *
     * <p>⚠ <b>Listing the six by hand is how this went wrong the first time.</b> The
     * boxes were transcribed from another mod and east and west were swapped in the
     * copying, which put the outline of every east- or west-facing light on the far side
     * of its own block - a whole block away from the thing you were pointing at. The five
     * others were right, so nothing about the shape of the mistake suggested itself.
     * Derived from the direction, there is no list to get out of order.
     */
    private static Map<Direction, VoxelShape> shapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        double far = 16.0 - DEPTH;
        for (Direction facing : Direction.values()) {
            Direction against = facing.getOpposite();
            boolean high = against.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            double x = high && against.getAxis() == Direction.Axis.X ? far : 0;
            double y = high && against.getAxis() == Direction.Axis.Y ? far : 0;
            double z = high && against.getAxis() == Direction.Axis.Z ? far : 0;
            shapes.put(facing, Block.box(x, y, z,
                    against.getAxis() == Direction.Axis.X && !high ? DEPTH : 16,
                    against.getAxis() == Direction.Axis.Y && !high ? DEPTH : 16,
                    against.getAxis() == Direction.Axis.Z && !high ? DEPTH : 16));
        }
        return Map.copyOf(shapes);
    }

    /** Which face of its own cell the plate of a lamp facing this way is drawn on. */
    public static Direction against(Direction facing) {
        return facing.getOpposite();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /**
     * Faces the way it was clicked on - and, when that click landed on another spotlight,
     * the way that one faces instead.
     *
     * <p>⭐ <b>This is what makes a run of them possible.</b> Lights inset along a ceiling
     * are placed by clicking the side of the last one, and the side of a block faces
     * sideways; without this, every second light would stand on end. Crouching turns it
     * off, which is how you start a new run against the one you just placed. Borrowed
     * from Simply Light, which is the one detail of theirs worth taking.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        Player player = context.getPlayer();
        BlockPos clicked = context.getClickedPos().relative(facing.getOpposite());
        BlockState against = context.getLevel().getBlockState(clicked);
        if (player != null && !player.isCrouching() && against.getBlock() instanceof SpotlightBlock) {
            facing = against.getValue(FACING);
        }
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType()
                == Fluids.WATER;
        return defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, water);
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

    /** Turning and mirroring a plate is turning and mirroring which face it is on. */
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
