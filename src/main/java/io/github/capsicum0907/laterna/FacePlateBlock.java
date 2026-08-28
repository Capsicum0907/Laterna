package io.github.capsicum0907.laterna;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import org.jetbrains.annotations.Nullable;

/**
 * A plate that goes on whichever face you point at - the recessed spotlight.
 *
 * <p><b>The face you click is the answer, and for this form it always is.</b> A light
 * sunk into a surface has no second reading: clicked on a ceiling it is a ceiling light,
 * clicked on a wall it is a wall light. That is not true of a slab, which is why the slab
 * does not use this class - see {@link FlatPlateBlock}.
 */
public class FacePlateBlock extends PlateBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public FacePlateBlock(Shape shape, Properties properties) {
        super(shape, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public Direction facing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public BlockState withFacing(BlockState state, Direction facing) {
        return state.setValue(FACING, facing);
    }

    @Override
    public Collection<Direction> facings() {
        return List.of(Direction.values());
    }

    /**
     * Faces the way it was clicked on - and, when that click landed on another of these,
     * the way that one faces instead.
     *
     * <p>⭐ <b>This is what makes a run of them possible.</b> Lights inset along a ceiling
     * are placed by clicking the side of the last one, and the side of a block faces
     * sideways; without this, every second light would stand on end. Crouching turns it
     * off, which is how you start a new run against the one you just placed.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        Player player = context.getPlayer();
        BlockPos clicked = context.getClickedPos().relative(facing.getOpposite());
        BlockState against = context.getLevel().getBlockState(clicked);
        if (player != null && !player.isCrouching()
                && against.getBlock() instanceof FacePlateBlock plate) {
            facing = plate.facing(against);
        }
        return defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, flooded(context));
    }

    /**
     * ⚠ <b>These fall when what they are mounted on goes.</b> A light recessed into a
     * wall is a hole in that wall, and a bulb or a fitting is bolted to one; with the wall
     * gone there is nothing holding any of them, and they drop as items rather than
     * hanging in the air.
     *
     * <p>Only the forms that go on a face. A slab or a panel is a thing in its own right
     * and stays where it is - the game's own {@code glow_lichen} pops off and Simply
     * Light's thin lamps do not bother, and each is right about a different shape. The
     * reason to pop off here is the practical one: taking a wall down should take its
     * lights with it, rather than leaving a field of them to be knocked out one at a
     * time.
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction against = against(facing(state));
        BlockPos support = pos.relative(against);
        return level.getBlockState(support).isFaceSturdy(level, support, against.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == against(facing(state)) && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
