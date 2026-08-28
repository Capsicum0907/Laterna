package io.github.capsicum0907.laterna;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import org.jetbrains.annotations.Nullable;

/**
 * A lamp that redstone switches.
 *
 * <p><b>No block entity and no ticking.</b> The light is
 * {@code lightLevel(state -> state.getValue(LIT) ? 15 : 0)}, handed in with the
 * properties at registration, so the game reads the brightness off the state it already
 * has. A lamp costs what a stone block costs; there is nothing here to run.
 *
 * <p><b>The wiring decides one thing: which way round the signal reads.</b> Everything
 * else - placing, being told a neighbour changed - goes through {@link #lit} and so is
 * written once for both blocks. Vanilla's own {@code RedstoneLampBlock} asks
 * {@code hasNeighborSignal} directly, which is the same question only for the normal
 * one; copying it as it stands would leave the inverted lamp backwards.
 */
public class LampBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final Wiring wiring;

    public LampBlock(Wiring wiring, Properties properties) {
        super(properties);
        this.wiring = wiring;
        registerDefaultState(stateDefinition.any().setValue(LIT, wiring.litWhenUnpowered()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    /**
     * Placed already in the state it belongs in, in one step.
     *
     * <p>A lamp put down inside a powered region is switched from the moment it exists,
     * rather than arriving in its default and being corrected afterwards - there is no
     * frame in which it shows the wrong face.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LIT, lit(context.getLevel(), context.getClickedPos()));
    }

    /**
     * ⚠ <b>Switched immediately, both ways.</b> Vanilla's redstone lamp waits four ticks
     * before going dark, so that a signal with a one-tick gap in it does not blink. This
     * one does not: a decorative lamp that answers at once is easier to reason about, and
     * the delay would need a scheduled tick whose condition has to be wiring-aware to be
     * correct. If the flicker ever matters, that is where it goes, and it goes through
     * {@link #lit} rather than around it.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour,
            BlockPos from, boolean moving) {
        if (level.isClientSide) {
            return;
        }
        boolean lit = lit(level, pos);
        if (state.getValue(LIT) != lit) {
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    /** Whether this lamp should be shining where it stands, wiring included. */
    private boolean lit(Level level, BlockPos pos) {
        return wiring.litWhenUnpowered() != level.hasNeighborSignal(pos);
    }
}
