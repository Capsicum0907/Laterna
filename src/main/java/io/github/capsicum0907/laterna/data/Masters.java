package io.github.capsicum0907.laterna.data;

import io.github.capsicum0907.laterna.Shape;

/**
 * What each form looks like, as arithmetic.
 *
 * <p>Two numbers do the work for the cube: the frame and the face. Lit, the face sits
 * well above the halfway mark and so is carried towards white, which is what makes it
 * read as a light rather than as a coloured block; unlit, both sit below it and the same
 * block reads as the thing switched off. The falloff towards the edges is slight - enough
 * that the middle looks like the source of the light, not so much that the face looks
 * domed.
 *
 * <p>⚠ <b>The colours these are tinted with are the game's dye values</b>
 * ({@code DyeColor#getTextureDiffuseColor}), which are the pure dyes and are brighter
 * than the shaded pixels of the wool textures.
 */
public final class Masters {
    private static final float FRAME_LIT = 0.34F;
    private static final float FACE_LIT = 0.80F;
    private static final float FALLOFF_LIT = 0.14F;

    private static final float FRAME_UNLIT = 0.20F;
    private static final float FACE_UNLIT = 0.34F;
    private static final float FALLOFF_UNLIT = 0.05F;

    private Masters() {
    }

    /**
     * @param shape which form to draw
     * @param lit whether it is shining
     * @return the grayscale picture of that form, ready to be given a colour
     */
    public static Master of(Shape shape, boolean lit) {
        return switch (shape) {
            case LAMP -> face(lit);
        };
    }

    /**
     * A flat panel inside a one-pixel frame, which is every side of the cube.
     *
     * <p>The frame is what stops sixteen lamps side by side reading as one wall: a lit
     * face alone has no edge, and a row of them merges.
     */
    private static Master face(boolean lit) {
        float frame = lit ? FRAME_LIT : FRAME_UNLIT;
        float centre = lit ? FACE_LIT : FACE_UNLIT;
        float falloff = lit ? FALLOFF_LIT : FALLOFF_UNLIT;

        Master master = Master.blank();
        int last = Master.SIZE - 1;
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                master.alpha()[y][x] = 1.0F;
                if (x == 0 || y == 0 || x == last || y == last) {
                    master.level()[y][x] = frame;
                } else {
                    master.level()[y][x] = centre - falloff * distance(x, y);
                }
            }
        }
        return master;
    }

    /** How far a pixel is from the middle of the face, squared, as a fraction of the half-width. */
    private static float distance(int x, int y) {
        float half = (Master.SIZE - 1) / 2.0F;
        float dx = (x - half) / half;
        float dy = (y - half) / half;
        return Math.min(1.0F, dx * dx + dy * dy);
    }
}
