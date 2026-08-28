package io.github.capsicum0907.laterna.data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import io.github.capsicum0907.laterna.Frame;
import io.github.capsicum0907.laterna.Laterna;
import io.github.capsicum0907.laterna.Shape;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.world.item.DyeColor;

/**
 * The picture that stands for the mod on a listing page: one lit lamp, drawn as a cube,
 * over the dark it exists for.
 *
 * <p><b>This is not a resource.</b> Nothing here reaches the jar. It is written beside
 * the repository, ignored by git, and uploaded by hand - which is why the path arrives as
 * a run property instead of coming from the pack output. The repository still holds no
 * images.
 *
 * <p><b>The faces come from the same master as the block.</b> {@link Masters} is asked
 * for the lamp exactly as {@link LampTextures} asks for it, so the icon cannot drift:
 * change what a lamp looks like and the next data run changes the icon to match. That is
 * the whole reason this is generated rather than drawn once and kept.
 */
public class ModIcon implements DataProvider {
    /** The run property naming the file to write. Set by the {@code data} run. */
    public static final String TARGET = "laterna.icon";

    // --- what is drawn ---------------------------------------------------------------

    private static final Shape SHAPE = Shape.LAMP;
    private static final Frame FRAME = Frame.OWN;
    private static final DyeColor COLOUR = DyeColor.WHITE;

    // --- how big ---------------------------------------------------------------------

    private static final int SIZE = 512;
    /** The cube's height as a share of the canvas; the rest is margin. */
    private static final double CUBE = 0.70;
    /** Samples per axis. A cube seen this way has diagonal edges and large texels. */
    private static final int SAMPLES = 4;

    // --- the dark it sits in ---------------------------------------------------------

    private static final int BACKDROP = 0x090E1B;
    /** How far the backdrop is carried towards the lamp's colour where the spill is full. */
    private static final float GLOW = 0.42F;
    /**
     * How wide the spill is, as a share of the canvas from the middle out.
     *
     * <p>⚠ <b>Wider than it looks like it should be.</b> The obvious falloff is
     * brightest in the middle and gone by the edge - and the middle is exactly where the
     * cube is, so all of it lands behind the block and none of it is ever seen. What has
     * to be lit is the ring just outside the silhouette, so the curve is still near full
     * where the cube ends and only fades over the margin past it.
     */
    private static final double SPREAD = 0.66;

    /**
     * The three faces a cube shows from this angle, and how far each is turned away.
     *
     * <p>The multipliers are vanilla's own - one, four fifths, three fifths going top,
     * south, east - lifted halfway back towards one.
     *
     * <p>⚠ <b>A lamp is not shaded like a stone block.</b> Three fifths dark reads as a
     * face in shadow, which is wrong for something lit from inside; but taking the
     * shading out altogether leaves a flat hexagon rather than a cube. Halfway keeps the
     * ordering that makes it read as a solid without putting a shadow on a light.
     */
    private enum Facet {
        TOP(1.0F), SOUTH(0.8F), EAST(0.6F);

        private static final float LIFT = 0.5F;

        private final float shade;

        Facet(float shade) {
            this.shade = shade;
        }

        float lifted() {
            return 1.0F - (1.0F - shade) * LIFT;
        }

        /**
         * @return where on this face the given point of the canvas lands, as a pair in
         *     the unit square, or null if the point is not on this face
         */
        double[] at(double sx, double sy) {
            switch (this) {
                case TOP -> {
                    // The rhombus on top, spanned by east and south from the far corner.
                    double sum = (sy + 1.0) / SIN30;
                    double difference = sx / COS30;
                    return square((difference + sum) / 2.0, (sum - difference) / 2.0);
                }
                case SOUTH -> {
                    double east = sx / COS30 + 1.0;
                    return square(east, 1.0 - (SIN30 + east * SIN30 - sy));
                }
                default -> {
                    double south = 1.0 - sx / COS30;
                    return square(1.0 - south, 1.0 - (SIN30 + south * SIN30 - sy));
                }
            }
        }

        private static double[] square(double u, double v) {
            if (u < -SLACK || u > 1.0 + SLACK || v < -SLACK || v > 1.0 + SLACK) {
                return null;
            }
            return new double[] { u, v };
        }
    }

    private static final double COS30 = Math.sqrt(3.0) / 2.0;
    private static final double SIN30 = 0.5;
    /** Slack on a face's edge, so the corner all three of them share has no seam. */
    private static final double SLACK = 1.0e-9;

    @Override
    public String getName() {
        return "Mod Icon: " + Laterna.MODID;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        String target = System.getProperty(TARGET);
        if (target == null) {
            throw new IllegalStateException("No " + TARGET + " to write the icon to. The data "
                    + "run sets it in build.gradle; a run that does not is misconfigured.");
        }
        int[][] face = face();
        int[][] pixels = new int[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                pixels[y][x] = 0xFF000000 | sampled(face, x, y);
            }
        }
        write(Path.of(target), pixels);
        return CompletableFuture.completedFuture(null);
    }

    /** The lamp's own texture, its layers laid over one another as the game lays them. */
    private static int[][] face() {
        int[][] pixels = new int[Master.SIZE][Master.SIZE];
        int plain = FRAME.colour().orElse(Masters.plainColour());
        for (Masters.Layer layer : Masters.layers(SHAPE)) {
            int colour = layer.tinted()
                    ? COLOUR.getTextureDiffuseColor() & 0xFFFFFF : Masters.plainColour();
            int[][] over = Masters.of(SHAPE, layer, true, FRAME).tinted(colour, plain);
            for (int y = 0; y < Master.SIZE; y++) {
                for (int x = 0; x < Master.SIZE; x++) {
                    pixels[y][x] = layered(pixels[y][x], over[y][x]) | 0xFF000000;
                }
            }
        }
        return pixels;
    }

    /** One canvas pixel, taken as the mean of a grid of samples across it. */
    private static int sampled(int[][] face, int x, int y) {
        double scale = SIZE * CUBE / 2.0;
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int down = 0; down < SAMPLES; down++) {
            for (int across = 0; across < SAMPLES; across++) {
                int colour = at(face, x + (across + 0.5) / SAMPLES,
                        y + (down + 0.5) / SAMPLES, scale);
                red += (colour >> 16) & 0xFF;
                green += (colour >> 8) & 0xFF;
                blue += colour & 0xFF;
            }
        }
        int samples = SAMPLES * SAMPLES;
        return Math.round((float) red / samples) << 16
                | Math.round((float) green / samples) << 8
                | Math.round((float) blue / samples);
    }

    /** What is at one point of the canvas: a face of the cube, or the dark behind it. */
    private static int at(int[][] face, double px, double py, double scale) {
        int behind = backdrop(px, py);
        double sx = (px - SIZE / 2.0) / scale;
        double sy = (py - SIZE / 2.0) / scale;
        for (Facet facet : Facet.values()) {
            double[] uv = facet.at(sx, sy);
            if (uv == null) {
                continue;
            }
            return layered(behind, shaded(face[texel(uv[1])][texel(uv[0])], facet.lifted()));
        }
        return behind;
    }

    private static int texel(double along) {
        return Math.max(0, Math.min(Master.SIZE - 1, (int) (along * Master.SIZE)));
    }

    /** The dark, lifted towards the lamp's colour by however near the middle it is. */
    private static int backdrop(double px, double py) {
        double away = Math.hypot(px - SIZE / 2.0, py - SIZE / 2.0) / (SIZE / 2.0) / SPREAD;
        return blend(BACKDROP, COLOUR.getTextureDiffuseColor() & 0xFFFFFF,
                (float) (GLOW * Math.exp(-away * away)));
    }

    private static int layered(int under, int over) {
        return blend(under, over & 0xFFFFFF, ((over >>> 24) & 0xFF) / 255.0F);
    }

    private static int shaded(int argb, float multiplier) {
        return (argb & 0xFF000000)
                | channel(((argb >> 16) & 0xFF) * multiplier) << 16
                | channel(((argb >> 8) & 0xFF) * multiplier) << 8
                | channel((argb & 0xFF) * multiplier);
    }

    private static int blend(int under, int over, float towards) {
        if (towards <= 0.0F) {
            return under & 0xFFFFFF;
        }
        return part(under >> 16, over >> 16, towards) << 16
                | part(under >> 8, over >> 8, towards) << 8
                | part(under, over, towards);
    }

    private static int part(int under, int over, float towards) {
        return channel((under & 0xFF) + ((over & 0xFF) - (under & 0xFF)) * towards);
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static void write(Path target, int[][] pixels) {
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, Png.encode(pixels));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
