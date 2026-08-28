package io.github.capsicum0907.laterna;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;

import org.jetbrains.annotations.Nullable;

/**
 * A plate that lies down: the lamp slab and the lamp panel.
 *
 * <p>⚠ <b>This class exists because of how it is placed, not because of how it looks.</b>
 * The first version of the slab took the face you clicked, the way the spotlight does,
 * and it was wrong in the hand: clicking the side of a block gave a slab standing against
 * that side, when every player's hands expect a slab lying at the bottom or the top of
 * the cell. Clicking a wall is the common case and it had the least expected answer -
 * an upper slab could only be got by finding a ceiling to click.
 *
 * <p><b>So the rule here is vanilla's, copied from {@code SlabBlock} and not invented:</b>
 * the underside of a block gives a top slab, the upper side gives a bottom one, and a
 * click on a side is decided by whether it landed in the upper or the lower half of that
 * face. A plate standing on its edge is a different block, {@link UprightPlateBlock}, and
 * a crafting step away.
 */
public class FlatPlateBlock extends PlateBlock {
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    public FlatPlateBlock(double depth, Properties properties) {
        super(depth, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HALF, Half.BOTTOM)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, WATERLOGGED);
    }

    /** Lying at the bottom of its cell, it shines up; lying at the top, down. */
    @Override
    public Direction facing(BlockState state) {
        return state.getValue(HALF) == Half.BOTTOM ? Direction.UP : Direction.DOWN;
    }

    @Override
    public BlockState withFacing(BlockState state, Direction facing) {
        return state.setValue(HALF, facing == Direction.UP ? Half.BOTTOM : Half.TOP);
    }

    @Override
    public Collection<Direction> facings() {
        return List.of(Direction.UP, Direction.DOWN);
    }

    /** Vanilla's rule for a slab, which is the whole reason this form is its own block. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();
        boolean upper = clicked == Direction.DOWN
                || (clicked != Direction.UP && context.getClickLocation().y - pos.getY() > 0.5);
        return defaultBlockState()
                .setValue(HALF, upper ? Half.TOP : Half.BOTTOM)
                .setValue(WATERLOGGED, flooded(context));
    }

    /** Turning the world does not turn something that only knows up from down. */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }
}
