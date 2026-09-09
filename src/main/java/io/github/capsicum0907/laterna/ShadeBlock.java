package io.github.capsicum0907.laterna;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A lamp run backwards: switched the same way, and what it switches on is darkness.
 *
 * <p><b>Everything about the redstone is inherited and none of it is repeated.</b> A
 * shade is a {@link LampBlock} whose {@code LIT} means "working" rather than "shining",
 * so both wirings, placing already in the right state and answering a neighbour change at
 * once come from there untouched. A normal one goes dark when a signal reaches it, which
 * is the switch on the wall; an inverted one is dark until one does, which is the material
 * you build a windowless room out of.
 *
 * <p><b>The two overrides below have to move together.</b> Block light dies on opacity
 * and sky light dies on opacity <em>and</em> on the straight-down column the game keeps
 * separately, so a shade that reported one and not the other would stop the light from
 * the sides while daylight fell through it at full strength - which looks like nothing
 * being wrong until you put one under the sky.
 */
public class ShadeBlock extends LampBlock {
    public ShadeBlock(Wiring wiring, Properties properties) {
        super(wiring, properties);
    }

    /**
     * The whole of the mechanism.
     *
     * <p>Light spreads by each cell taking the brightest of its neighbours less its own
     * opacity, so a cell of the full fifteen leaves nothing of whatever reaches it. There
     * is no negative emission to use instead: brightness is unsigned and a source that
     * offered one would be read as no source at all.
     *
     * <p>⚠ <b>Read off the state and nothing else.</b> The game caches this once per
     * block state, with no level and no position, so an answer that depended on either
     * would be computed at startup against an empty world and then never asked again.
     * Because it is a state, the game also notices when it changes and relights the
     * neighbourhood for us.
     */
    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) ? LightEngine.MAX_LEVEL : 0;
    }

    /** @see #getLightBlock the other half, which this has to agree with */
    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.getValue(LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * Nothing to see and nothing to point at - until you are holding one.
     *
     * <p>A block that is invisible and has no outline cannot be broken by anyone who has
     * forgotten where it is. The game's own light block answers this by appearing while
     * its item is in your hand, and that is the answer here: put a wall of these in and
     * they are gone; take one out of your pocket and the wall is there again.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return context.isHoldingItem(asItem()) ? Shapes.block() : Shapes.empty();
    }
}
