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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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
 * A lamp that clings to a face: the recessed spotlight, the slab and the panel.
 *
 * <p><b>One class for all three, because the only thing that differs is how deep it is.</b>
 * Each sits against one face of its own cell, turns with {@code FACING}, holds water, and
 * takes its depth from {@link Shape#depth}. The spotlight is the degenerate one - a single
 * pixel deep, and drawn as a plate with no thickness at all.
 *
 * <p><b>What is drawn and what can be touched are two different things.</b> The
 * spotlight's model is a plate - an element whose {@code from} and {@code to} agree on one
 * axis, which is what the game's own {@code glow_lichen}, {@code vine} and
 * {@code lily_pad} are. A {@link VoxelShape} of no thickness, though, is a block that
 * cannot be pointed at, selected or broken, so its shape is one pixel deep while its
 * drawing is flat. Getting these two confused is the whole difficulty of that form.
 *
 * <p><b>Whether there is anything to stand on is decided per form, and not here.</b>
 * Recessed means flush, so the spotlight has no collision at all - a box standing a
 * sixteenth of a block proud of the floor is something you would feel underfoot. A slab is
 * a step and is supposed to be. That choice lives with the properties, in
 * {@code LaternaRegistry}.
 *
 * <p>{@code FACING} is the direction the light shines, so the plate sits against the
 * opposite face of its own cell - put down on a floor it faces up, and the plate lies at
 * the bottom. That agrees with how Simply Light's thin lamps read the same property.
 *
 * <p>⚠ <b>Whether it needs something to cling to is not decided.</b> Break the wall
 * behind one and it stays where it is. The game's own {@code glow_lichen} pops off;
 * Simply Light's thin lamps do not bother.
 */
public class PlateBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final Map<Direction, VoxelShape> shapes;

    public PlateBlock(double depth, Properties properties) {
        super(properties);
        this.shapes = shapes(depth);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(WATERLOGGED, false));
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
    private static Map<Direction, VoxelShape> shapes(double depth) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        double far = 16.0 - depth;
        for (Direction facing : Direction.values()) {
            Direction against = against(facing);
            boolean high = against.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            Direction.Axis axis = against.getAxis();
            shapes.put(facing, Block.box(
                    high && axis == Direction.Axis.X ? far : 0,
                    high && axis == Direction.Axis.Y ? far : 0,
                    high && axis == Direction.Axis.Z ? far : 0,
                    !high && axis == Direction.Axis.X ? depth : 16,
                    !high && axis == Direction.Axis.Y ? depth : 16,
                    !high && axis == Direction.Axis.Z ? depth : 16));
        }
        return Map.copyOf(shapes);
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    /**
     * Faces the way it was clicked on - and, when that click landed on another plate, the
     * way that one faces instead.
     *
     * <p>⭐ <b>This is what makes a run of them possible.</b> Lights inset along a ceiling
     * are placed by clicking the side of the last one, and the side of a block faces
     * sideways; without this, every second light would stand on end. Crouching turns it
     * off, which is how you start a new run against the one you just placed. Borrowed
     * from Simply Light, which is the one detail of theirs worth taking.
     *
     * <p>Any plate follows any other, not only its own form: a panel put against a slab
     * lies the way the slab lies, which is the answer wanted when the two are used
     * together on one ceiling.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        Player player = context.getPlayer();
        BlockPos clicked = context.getClickedPos().relative(facing.getOpposite());
        BlockState against = context.getLevel().getBlockState(clicked);
        if (player != null && !player.isCrouching() && against.getBlock() instanceof PlateBlock) {
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
