package io.github.capsicum0907.laterna.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import com.google.common.hash.Hashing;

import io.github.capsicum0907.laterna.Lamp;
import io.github.capsicum0907.laterna.Laterna;
import io.github.capsicum0907.laterna.Shape;

import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * Every texture the mod has, written out from {@link Masters}.
 *
 * <p><b>Two files per form and colour, not per lamp.</b> A normal lamp and an inverted
 * one are the same two pictures - lit and unlit - and both blocks point at them.
 *
 * <p><b>The colour is asked for, never written down.</b> {@code getTextureDiffuseColor}
 * is the game's own value for a dye, so these match wool and concrete without a table to
 * keep in step, and a resource pack can still replace any single file because what comes
 * out is an ordinary PNG rather than a tint applied at runtime.
 */
public class LampTextures implements DataProvider {
    private final PackOutput.PathProvider textures;

    public LampTextures(PackOutput output) {
        this.textures = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "textures/block");
    }

    /** The names this provider will write, for anything that has to know in advance. */
    public static List<String> skins() {
        List<String> names = new ArrayList<>();
        for (Shape shape : Shape.values()) {
            for (DyeColor colour : DyeColor.values()) {
                names.add(Lamp.skin(shape, colour, true));
                names.add(Lamp.skin(shape, colour, false));
            }
        }
        return names;
    }

    @Override
    public String getName() {
        return "Lamp Textures: " + Laterna.MODID;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writing = new ArrayList<>();
        for (Shape shape : Shape.values()) {
            for (boolean lit : new boolean[] { true, false }) {
                Master master = Masters.of(shape, lit);
                for (DyeColor colour : DyeColor.values()) {
                    int[][] pixels = master.tinted(colour.getTextureDiffuseColor() & 0xFFFFFF);
                    Path target = textures.file(ResourceLocation.fromNamespaceAndPath(
                            Laterna.MODID, Lamp.skin(shape, colour, lit)), "png");
                    writing.add(CompletableFuture.runAsync(() -> write(output, pixels, target),
                            Util.backgroundExecutor()));
                }
            }
        }
        return CompletableFuture.allOf(writing.toArray(CompletableFuture[]::new));
    }

    @SuppressWarnings("deprecation") // Hashing.sha1 is what CachedOutput expects
    private static void write(CachedOutput output, int[][] pixels, Path target) {
        try {
            byte[] bytes = png(pixels);
            output.writeIfNeeded(target, bytes, Hashing.sha1().hashBytes(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // --- png -------------------------------------------------------------------------

    /** Eight-bit RGBA, one filter byte of nought per row, which is all this needs. */
    private static byte[] png(int[][] pixels) throws IOException {
        int size = pixels.length;
        ByteBuffer raw = ByteBuffer.allocate(size * (size * 4 + 1));
        for (int[] row : pixels) {
            raw.put((byte) 0);
            for (int pixel : row) {
                raw.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel)
                        .put((byte) (pixel >>> 24));
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' });
        chunk(out, "IHDR", ByteBuffer.allocate(13)
                .putInt(size).putInt(size)
                .put((byte) 8).put((byte) 6).put((byte) 0).put((byte) 0).put((byte) 0).array());
        chunk(out, "IDAT", deflate(raw.array()));
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String kind, byte[] data)
            throws IOException {
        byte[] name = kind.getBytes(StandardCharsets.US_ASCII);
        out.write(ByteBuffer.allocate(4).putInt(data.length).array());
        out.write(name);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
