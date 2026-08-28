package io.github.capsicum0907.laterna;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import org.jetbrains.annotations.Nullable;

/**
 * A plate that stands on its edge: the vertical lamp slab and the vertical lamp panel.
 *
 * <p>The other half of the split described in {@link FlatPlateBlock}. Standing up is what
 * the flat one deliberately cannot do, and lying down is what this one cannot; each is
 * placed the way its own shape is expected to be placed, and a single item on the bench
 * turns one into the other.
 *
 * <p>Clicked on a wall it stands against that wall. Clicked on a floor or a ceiling,
 * where the face gives no answer, it turns to face the player - the same fallback every
 * wall-facing block in the game uses.
 */
public class UprightPlateBlock extends PlateBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public UprightPlateBlock(double depth, Properties properties) {
        super(depth, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
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
        return List.copyOf(Direction.Plane.HORIZONTAL.stream().toList());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction facing = clicked.getAxis().isHorizontal()
                ? clicked
                : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, flooded(context));
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
