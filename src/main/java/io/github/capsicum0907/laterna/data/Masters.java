package io.github.capsicum0907.laterna.data;

import java.util.List;

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
 * <p><b>A form can have more than one layer.</b> The spotlight has two: a lens, which
 * takes the colour, and a ring, which never does. Keeping them apart means the ring is
 * <em>one</em> file for all sixteen colours rather than sixteen composites, and it is
 * what lets the lens be drawn at full brightness while the fitting around it is not.
 *
 * <p>⚠ <b>Nothing here is anti-aliased.</b> A circle sixteen pixels across is a staircase
 * and is supposed to be; softening the edge with part-transparent pixels gives a blur at
 * the size these are actually seen.
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

    /** Where the lens ends and the ring begins, and where the ring ends, in pixels. */
    private static final float LENS = 5.6F;
    private static final float RIM = 7.5F;

    private static final float LENS_CENTRE = 0.72F;
    private static final float LENS_EDGE = 0.30F;

    /** The fitting: level 0.5 is its own grey untouched, and the bevel is a step each way. */
    private static final int RING_GREY = 0x4A4E52;
    private static final float BEVEL_LIT = 0.62F;
    private static final float BEVEL_DARK = 0.38F;
    private static final float BEVEL_EDGE = 0.30F;

    private Masters() {
    }

    /**
     * A layer of a form: what it is called, and whether it takes the lamp's colour.
     *
     * @param suffix added to the texture's name, empty for the layer that carries the form
     * @param tinted whether there is one of these per colour, or one in total
     */
    public record Layer(String suffix, boolean tinted) {
        public static final Layer BODY = new Layer("", true);
        public static final Layer RING = new Layer("_ring", false);
    }

    public static List<Layer> layers(Shape shape) {
        return switch (shape) {
            case LAMP -> List.of(Layer.BODY);
            case SPOTLIGHT -> List.of(Layer.BODY, Layer.RING);
        };
    }

    /**
     * The colour a layer that takes no colour is drawn in.
     *
     * <p>One value, because there is one such layer. A second would make this a switch,
     * which is the point at which it stops being a constant and starts being a decision.
     */
    public static int plainColour() {
        return RING_GREY;
    }

    /**
     * @param shape which form to draw
     * @param layer which of its layers
     * @param lit whether it is shining; forms that are always lit are only asked for true
     * @return the grayscale picture, ready to be given a colour
     */
    public static Master of(Shape shape, Layer layer, boolean lit) {
        return switch (shape) {
            case LAMP -> face(lit);
            case SPOTLIGHT -> layer.equals(Layer.RING) ? ring() : lens();
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
                    master.level()[y][x] = centre - falloff * squared(x, y);
                }
            }
        }
        return master;
    }

    /**
     * The lens: a disc, brightest in the middle, and nothing outside it.
     *
     * <p>Well above the halfway mark everywhere, so every colour is carried some way
     * towards white - but not as far as the cube's face goes, or the darker dyes stop
     * being told apart.
     */
    private static Master lens() {
        Master master = Master.blank();
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                float radius = radius(x, y);
                if (radius > LENS) {
                    continue;
                }
                float across = radius / LENS;
                master.alpha()[y][x] = 1.0F;
                master.level()[y][x] = LENS_CENTRE - LENS_EDGE * across * across;
            }
        }
        return master;
    }

    /**
     * The fitting: a ring two pixels thick, lighter towards the top left.
     *
     * <p>The bevel is the only thing that says this is a rim set into a surface rather
     * than a painted circle, and it is why the blockstate keeps {@code uvlock} on - a
     * rotated model without it would turn the highlight with the block.
     */
    private static Master ring() {
        Master master = Master.blank();
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                float radius = radius(x, y);
                if (radius <= LENS || radius > RIM) {
                    continue;
                }
                float half = (Master.SIZE - 1) / 2.0F;
                float lean = -((x - half) + (y - half)) / (radius * 2.0F);
                master.alpha()[y][x] = 1.0F;
                master.level()[y][x] = lean > BEVEL_EDGE ? BEVEL_LIT
                        : lean < -BEVEL_EDGE ? BEVEL_DARK : 0.5F;
            }
        }
        return master;
    }

    /** How far the middle of a pixel is from the middle of the texture, in pixels. */
    private static float radius(int x, int y) {
        float half = Master.SIZE / 2.0F;
        float dx = x + 0.5F - half;
        float dy = y + 0.5F - half;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** The same distance as a fraction of the half-width, squared, for the cube's falloff. */
    private static float squared(int x, int y) {
        float half = (Master.SIZE - 1) / 2.0F;
        float dx = (x - half) / half;
        float dy = (y - half) / half;
        return Math.min(1.0F, dx * dx + dy * dy);
    }
}
