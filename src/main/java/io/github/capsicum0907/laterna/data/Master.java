package io.github.capsicum0907.laterna.data;

/**
 * A picture before it has a colour: how bright each pixel is, and how opaque.
 *
 * <p><b>One convention, and it is the whole of how colour works in this mod.</b> A level
 * of {@code 0.5} is the dye colour untouched. Below that darkens it towards black, above
 * that carries it towards white. So a frame is written as {@code 0.34} and a glowing face
 * as {@code 0.80}, and the same two numbers give a sensible frame and a sensible glow in
 * all sixteen colours - including black, where a plain multiply would have left nothing
 * to see.
 *
 * <p>Nothing here is a file. Masters are drawn from formulas in {@link Masters}, which is
 * why the repository holds no images: there is no source picture to lose track of, and a
 * change to a shape is a change to an expression.
 */
public record Master(float[][] level, float[][] alpha) {
    public static final int SIZE = 16;

    /** An empty master, fully transparent, for a formula to fill in. */
    public static Master blank() {
        return new Master(new float[SIZE][SIZE], new float[SIZE][SIZE]);
    }

    /** @return the master in one colour, as ARGB, ready to be written out */
    public int[][] tinted(int rgb) {
        int[][] pixels = new int[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int opacity = Math.round(clamp(alpha[y][x]) * 255.0F);
                pixels[y][x] = opacity == 0 ? 0 : opacity << 24 | shade(rgb, level[y][x]);
            }
        }
        return pixels;
    }

    /** @see Master the convention this implements */
    private static int shade(int rgb, float level) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        if (level <= 0.5F) {
            float weight = clamp(level * 2.0F);
            return channel(red * weight) << 16 | channel(green * weight) << 8 | channel(blue * weight);
        }
        float towards = clamp((level - 0.5F) * 2.0F);
        return towards(red, towards) << 16 | towards(green, towards) << 8 | towards(blue, towards);
    }

    private static int towards(int value, float white) {
        return channel(value + (255 - value) * white);
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
