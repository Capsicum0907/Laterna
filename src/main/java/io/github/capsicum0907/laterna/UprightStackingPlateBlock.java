package io.github.capsicum0907.laterna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * A plate that stands on its edge and can be stacked with another of itself into a whole
 * block: the vertical lamp slab.
 *
 * <p>The standing counterpart of {@link StackingPlateBlock}, and it keeps that one's
 * rules - both are asked through {@link PlateBlock#stacksInto}, so the two cannot come to
 * disagree about when a second slab fills a cell rather than replacing what is in it.
 *
 * <p>⚠ <b>Standing, the axis survives the stacking.</b> A pair of lying slabs is a cube
 * with its lit face up and down whichever way you built it; a standing pair has its face
 * north and south, or east and west, and the two are not the same block to look at. So
 * {@code FACING} is kept when {@code double} is set, and only its axis is read - which is
 * why north and south give one appearance between them rather than two.
 */
public class UprightStackingPlateBlock extends UprightPlateBlock {
    public static final BooleanProperty DOUBLE = BooleanProperty.create("double");

    public UprightStackingPlateBlock(Shape shape, Properties properties) {
        super(shape, properties);
        // ⚠ Set here and not left to the parent: a boolean property offers true first, so
        // a default built before this line can come up already doubled.
        registerDefaultState(defaultBlockState().setValue(DOUBLE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DOUBLE);
    }

    @Override
    public boolean whole(BlockState state) {
        return state.getValue(DOUBLE);
    }

    @Override
    public BlockState asWhole(BlockState state) {
        return state.setValue(DOUBLE, true);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return whole(state) ? Shapes.block() : super.getShape(state, level, pos, context);
    }

    /** @see StackingPlateBlock the reason a stacked plate is the one that stops light */
    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return !whole(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState standing = context.getLevel().getBlockState(context.getClickedPos());
        if (standing.is(this)) {
            return standing.setValue(DOUBLE, true).setValue(WATERLOGGED, false);
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack held = context.getItemInHand();
        if (whole(state) || !held.is(asItem())) {
            return false;
        }
        Direction against = against(facing(state));
        return stacksInto(context, against.getAxis(),
                against.getAxisDirection() == Direction.AxisDirection.NEGATIVE);
    }
}
