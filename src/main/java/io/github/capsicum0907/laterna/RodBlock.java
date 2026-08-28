package io.github.capsicum0907.laterna;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * A thin bar of light running the length of its cell.
 *
 * <p>⚠ <b>Not a plate, and that is the whole of why it is its own class.</b> Everything
 * else in this mod sits against one face of its cell and is described by which face and
 * how deep. A rod sits against none of them: it runs through the middle from one side to
 * the other, so what it keeps is an axis, and it is built like the game's own chain rather
 * than like a lamp stuck to a wall.
 *
 * <p>Running the full length is what lets a row of them read as one continuous line, which
 * is the only reason to have a strip light rather than more fittings.
 *
 * <p>It clings to nothing, so nothing takes it down: unlike the fittings, breaking what is
 * beside a rod leaves the rod. There is no face it was mounted on to lose.
 */
public class RodBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final Map<Direction.Axis, VoxelShape> shapes;

    public RodBlock(double inset, Properties properties) {
        super(properties);
        this.shapes = shapes(inset);
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(WATERLOGGED, false));
    }

    /** Thin across, and the whole cell along - worked out from the axis, never listed. */
    private static Map<Direction.Axis, VoxelShape> shapes(double inset) {
        Map<Direction.Axis, VoxelShape> shapes = new EnumMap<>(Direction.Axis.class);
        for (Direction.Axis along : Direction.Axis.values()) {
            double[] low = new double[3];
            double[] top = new double[3];
            for (Direction.Axis axis : Direction.Axis.values()) {
                low[axis.ordinal()] = axis == along ? 0.0 : inset;
                top[axis.ordinal()] = axis == along ? 16.0 : 16.0 - inset;
            }
            shapes.put(along, Block.box(low[0], low[1], low[2], top[0], top[1], top[2]));
        }
        return Map.copyOf(shapes);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return shapes.get(state.getValue(AXIS));
    }

    /**
     * Runs the way you were pointing when you put it down.
     *
     * <p>The face clicked gives the axis, which is the game's own rule for anything with
     * an axis - a log, a chain. Click the top of a block and the rod stands up; click the
     * side of one and it lies along the way you were looking.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType()
                == Fluids.WATER;
        return defaultBlockState()
                .setValue(AXIS, context.getClickedFace().getAxis())
                .setValue(WATERLOGGED, water);
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

    /** Turning the world turns which way a rod runs, exactly as it does for a log. */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                case Y -> state;
            };
            default -> state;
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }
}
