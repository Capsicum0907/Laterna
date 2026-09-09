package io.github.capsicum0907.laterna;

import net.minecraft.world.level.block.HalfTransparentBlock;

/**
 * Glass that is its own light.
 *
 * <p><b>Everything it is comes from its properties, and the one thing that does not is
 * here.</b> Two panes of the same glass hide the face between them, so a wall is a sheet
 * rather than a stack of boxes with their insides drawn - which the game already does for
 * its own glass, and which is the whole of what {@link HalfTransparentBlock} is.
 *
 * <p>⚠ <b>Against the same block only, as vanilla has it.</b> Blue glass beside white
 * glass shows both faces, because there are two panes there and you can see that there
 * are. Frameless glass of one colour is the case where it matters, and that is the case
 * this covers.
 */
public class GlowingGlassBlock extends HalfTransparentBlock {
    public GlowingGlassBlock(Properties properties) {
        super(properties);
    }
}
