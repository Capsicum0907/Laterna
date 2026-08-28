package io.github.capsicum0907.laterna;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * A plate that lies down and can be stacked with another of itself into a whole block.
 *
 * <p><b>The stacked one is not the cube.</b> A cube of this mod wears its lit face on all
 * six sides; two slabs laid together keep the slab's own look - the lit face up and down,
 * the plain rim around the four cut sides - because that is what they are. Filling a wall
 * with these and then wanting a full-height course of the same thing is the reason it
 * exists.
 *
 * <p><b>The placement is vanilla's, taken from {@code SlabBlock} rather than paraphrased.</b>
 * Both the rule for which half a click lands in and the rule for when a second slab
 * doubles rather than replaces are theirs; either one written from memory gives
 * "sometimes it replaces instead of stacking", which is the same kind of wrongness under
 * the hand that having its own block was meant to fix.
 *
 * <p>⚠ <b>This is the one plate that occludes.</b> The others are declared
 * {@code noOcclusion} because a thin thing is not a wall; a doubled one is a whole block
 * and has to stop light, or a room walled with them is lit through. Vanilla arranges that
 * by leaving occlusion alone and answering {@link #useShapeForLightOcclusion} with "use
 * the shape unless I am doubled", and so does this.
 */
public class StackingPlateBlock extends PlateBlock {
    public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;

    public StackingPlateBlock(double depth, Properties properties) {
        super(depth, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(TYPE, SlabType.BOTTOM)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, WATERLOGGED);
    }

    @Override
    public boolean whole(BlockState state) {
        return state.getValue(TYPE) == SlabType.DOUBLE;
    }

    @Override
    public BlockState asWhole(BlockState state) {
        return state.setValue(TYPE, SlabType.DOUBLE);
    }

    /** Lying at the bottom of its cell it shines up, at the top down; doubled, both ways. */
    @Override
    public Direction facing(BlockState state) {
        return state.getValue(TYPE) == SlabType.TOP ? Direction.DOWN : Direction.UP;
    }

    @Override
    public BlockState withFacing(BlockState state, Direction facing) {
        return state.setValue(TYPE, facing == Direction.UP ? SlabType.BOTTOM : SlabType.TOP);
    }

    @Override
    public Collection<Direction> facings() {
        return List.of(Direction.UP, Direction.DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return whole(state) ? Shapes.block() : super.getShape(state, level, pos, context);
    }

    /** @see StackingPlateBlock the reason this is the plate that stops light */
    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return !whole(state);
    }

    @Override
    protected boolean isPathfindable(BlockState state,
            net.minecraft.world.level.pathfinder.PathComputationType path) {
        return false;
    }

    /** Vanilla's rule for a slab, and the reason this form is its own block. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState standing = context.getLevel().getBlockState(pos);
        if (standing.is(this)) {
            return standing.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false);
        }
        return defaultBlockState()
                .setValue(TYPE, upperHalf(context) ? SlabType.TOP : SlabType.BOTTOM)
                .setValue(WATERLOGGED, flooded(context));
    }

    /**
     * Whether a second one lands in the same cell rather than the next.
     *
     * <p>Vanilla's, by way of {@link PlateBlock#stacksInto}, which the standing form asks
     * as well: a bottom slab takes another from above or from a side click that landed
     * high, a top one from below or a side click that landed low, and a doubled one takes
     * nothing.
     */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack held = context.getItemInHand();
        SlabType type = state.getValue(TYPE);
        if (type == SlabType.DOUBLE || !held.is(asItem())) {
            return false;
        }
        return stacksInto(context, Direction.Axis.Y, type == SlabType.BOTTOM);
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
