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
     * <p>⚠ <b>Softening is done in colour, never in alpha.</b> The render type these are
 * drawn with keeps or discards a pixel and does not blend, so a part-transparent edge
 * comes back as a fatter hard one. Where an edge has a layer of its own behind it, mixing
 * towards that layer gives the smooth circle instead; where it borders the world, the edge
 * stays hard, as every round thing in the game does.
 *
 * <p>⚠ <b>The colours these are tinted with are the game's dye values</b>
 * ({@code DyeColor#getTextureDiffuseColor}), which are the pure dyes and are brighter
 * than the shaded pixels of the wool textures.
 */
public final class Masters {
    private static final float FRAME_LIT = 0.34F;
    private static final float FACE_LIT = 0.80F;
    private static final float FALLOFF_LIT = 0.14F;

    /** The cut side of a slab: brighter than the frame, duller than the face. */
    private static final float EDGE_FACE = 0.46F;

    /** The shell around a bulb: whiter than the body it covers, and mostly not there. */
    private static final float HALO_LEVEL = 0.86F;
    private static final float HALO_ALPHA = 0.45F;

    private static final float FRAME_UNLIT = 0.20F;
    private static final float FACE_UNLIT = 0.34F;
    private static final float FALLOFF_UNLIT = 0.05F;

    /**
     * How big the fitting is, in pixels of the sixteen.
     *
     * <p>⚠ <b>Smaller than it was.</b> The first pass filled almost the whole face, which
     * read as a coloured block with a grey border rather than as a fitting set into a
     * surface. Eight pixels across - half the face - leaves the surface plainly around it,
     * which is what makes it look recessed.
     *
     * <p>⚠ <b>Not every radius rasterises to a circle.</b> At 4.5 and at 5.6 the outline
     * grows a two-pixel nub at each of the four compass points and reads as a cog. The
     * radii here were picked by drawing every candidate and looking at the outline, not
     * by choosing round numbers.
     */
    private static final float LENS = 2.8F;
    private static final float RIM = 4.2F;

    private static final float LENS_CENTRE = 0.72F;
    private static final float LENS_EDGE = 0.30F;

    /** The fitting: level 0.5 is its own grey untouched, and the bevel is a step each way. */
    private static final int RING_GREY = 0x4A4E52;
    private static final float BEVEL_LIT = 0.66F;
    private static final float BEVEL_DARK = 0.34F;
    private static final float BEVEL_EDGE = 0.30F;
    private static final float BEVEL_DEPTH = 1.3F;

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
        public static final Layer EDGE = new Layer("_edge", true);
        public static final Layer HALO = new Layer("_halo", true);
    }

    public static List<Layer> layers(Shape shape) {
        return switch (shape) {
            case LAMP, ROD -> List.of(Layer.BODY);
            case BULB -> List.of(Layer.BODY, Layer.EDGE, Layer.HALO);
            case SPOTLIGHT -> List.of(Layer.BODY, Layer.RING);
            case FIXTURE -> List.of(Layer.BODY, Layer.EDGE);
            case SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL -> List.of(Layer.BODY, Layer.EDGE);
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
            // ⚠ A flat colour, with none of the falloff the other faces have. The
            // bar's faces are two pixels by sixteen, and a glow drawn round on a square
            // texture comes out of that as a long white ellipse - which is what it looked
            // like. Nothing shaped survives being stretched that far.
            case ROD -> flat();
            case SPOTLIGHT -> layer.equals(Layer.RING) ? ring() : lens();
            // ⚠ Frameless, because these are shown small. The face of a cube is drawn
            // at sixteen pixels and its one-pixel border reads; stretched onto a face six
            // pixels across the same border is a third of a pixel, which is a smudge. A
            // plain glow has nothing to lose at that size.
            case BULB -> layer.equals(Layer.EDGE) ? edge()
                    : layer.equals(Layer.HALO) ? halo() : glow();
            case FIXTURE -> layer.equals(Layer.EDGE) ? edge() : glow();
            // The slab and the panel wear the cube's lit face on the two broad sides, and
            // a rim of their own on the four cut ones. They get files of their own rather
            // than borrowing the cube's, so that a resource pack can retexture a panel
            // without touching every lamp in the world.
            case SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL ->
                    layer.equals(Layer.EDGE) ? edge() : face(lit);
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
     * The lens: a disc, brightest in the middle, with a softened edge.
     *
     * <p>Well above the halfway mark everywhere, so every colour is carried some way
     * towards white - but not as far as the cube's face goes, or the darker dyes stop
     * being told apart.
     *
     * <p>⭐ <b>The edge is softened onto the ring, not into transparency.</b> A disc eight
     * pixels across drawn with hard pixels is a staircase, and the render type these are
     * drawn with turns a soft alpha back into a hard edge anyway. But the lens never
     * borders the world - the ring is a solid disc behind it - so a part-covered pixel can
     * simply be mixed towards the grey, which reads as a smooth circle and stays fully
     * opaque. That is the whole trick, and it is why the ring is a disc and not an annulus.
     */
    private static Master lens() {
        Master master = Master.blank();
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                float covered = coverage(x, y, LENS);
                if (covered <= 0.0F) {
                    continue;
                }
                float across = Math.min(1.0F, radius(x, y) / LENS);
                master.alpha()[y][x] = 1.0F;
                master.level()[y][x] = LENS_CENTRE - LENS_EDGE * across * across;
                master.plain()[y][x] = 1.0F - covered;
            }
        }
        return master;
    }

    /**
     * The fitting: a solid grey disc, bevelled around its rim.
     *
     * <p>Solid rather than a ring with a hole in it, so that the lens has something to
     * soften onto - see {@link #lens}. The middle of it is never seen.
     *
     * <p>⚠ <b>Its own outline is hard, and has to be.</b> This edge borders the block
     * behind, so softening it means alpha, and alpha is what the render type discards.
     * A hard circle is also what every round thing in the game is.
     *
     * <p>The bevel is the only thing that says this is a rim set into a surface rather
     * than a painted circle, and it is why the blockstate keeps {@code uvlock} on - a
     * rotated model without it would turn the highlight with the block.
     */
    private static Master ring() {
        Master master = Master.blank();
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                if (coverage(x, y, RIM) < 0.5F) {
                    continue;
                }
                master.alpha()[y][x] = 1.0F;
                float radius = radius(x, y);
                if (radius < RIM - BEVEL_DEPTH) {
                    master.level()[y][x] = 0.5F;
                    continue;
                }
                float half = Master.SIZE / 2.0F;
                float lean = -((x + 0.5F - half) + (y + 0.5F - half)) / (radius * 2.0F);
                master.level()[y][x] = lean > BEVEL_EDGE ? BEVEL_LIT
                        : lean < -BEVEL_EDGE ? BEVEL_DARK : 0.5F;
            }
        }
        return master;
    }

    /**
     * The envelope around a bulb: the same light, mostly see-through.
     *
     * <p>⚠ <b>Drawn as a second skin rather than a brighter core.</b> A bulb in the mod
     * this follows is an opaque coloured body inside a translucent shell a little larger
     * than it - two boxes and two render types, not one. Painting the body brighter would
     * not do it: what reads is the shell standing off the body, so that the body is seen
     * through something.
     */
    private static Master halo() {
        Master master = Master.blank();
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                master.alpha()[y][x] = HALO_ALPHA;
                master.level()[y][x] = HALO_LEVEL;
            }
        }
        return master;
    }

    /** One shade, for a face too long and thin for anything drawn on it to survive. */
    private static Master flat() {
        Master master = Master.blank();
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                master.alpha()[y][x] = 1.0F;
                master.level()[y][x] = FACE_LIT;
            }
        }
        return master;
    }

    /** A face of light with no border, for the forms that are shown too small for one. */
    private static Master glow() {
        Master master = Master.blank();
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                master.alpha()[y][x] = 1.0F;
                master.level()[y][x] = FACE_LIT - FALLOFF_LIT * squared(x, y);
            }
        }
        return master;
    }

    /**
     * The cut side of a slab or a panel: one flat shade, and nothing else.
     *
     * <p>⚠ <b>Flat because the crop cannot be predicted.</b> A side face is four pixels
     * deep or eight, and the game works its texture coordinates out from where the
     * element is - so a strip is taken across the texture on two of the four sides and
     * down it on the other two. Anything drawn here with a top or a left would come out
     * turned on half of them. What was there before was the cube's own face, cropped, and
     * it read as exactly what it was: a block sliced through.
     *
     * <p>A shade above the frame, so the cut edge reads as part of a lit thing rather
     * than as the dark border around one.
     */
    private static Master edge() {
        Master master = Master.blank();
        for (int y = 0; y < Master.SIZE; y++) {
            for (int x = 0; x < Master.SIZE; x++) {
                master.alpha()[y][x] = 1.0F;
                master.level()[y][x] = EDGE_FACE;
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

    /** How much of a pixel falls inside a circle, sampled on a grid within it. */
    private static float coverage(int x, int y, float radius) {
        int grid = 4;
        float half = Master.SIZE / 2.0F;
        int inside = 0;
        for (int j = 0; j < grid; j++) {
            for (int i = 0; i < grid; i++) {
                float dx = x + (i + 0.5F) / grid - half;
                float dy = y + (j + 0.5F) / grid - half;
                if (dx * dx + dy * dy <= radius * radius) {
                    inside++;
                }
            }
        }
        return (float) inside / (grid * grid);
    }

    /** The same distance as a fraction of the half-width, squared, for the cube's falloff. */
    private static float squared(int x, int y) {
        float half = (Master.SIZE - 1) / 2.0F;
        float dx = (x - half) / half;
        float dy = (y - half) / half;
        return Math.min(1.0F, dx * dx + dy * dy);
    }
}
