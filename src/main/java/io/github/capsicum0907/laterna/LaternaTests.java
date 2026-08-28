package io.github.capsicum0907.laterna;

import io.github.capsicum0907.laterna.data.TestStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the two wirings are supposed to be, checked in a world rather than by reading.
 *
 * <p>Run with {@code gradlew runGameTestServer}.
 */
@GameTestHolder(Laterna.MODID)
@PrefixGameTestTemplate(false)
public final class LaternaTests {
    private static final BlockPos WHERE = new BlockPos(4, 1, 4);
    private static final BlockPos SOURCE = WHERE.north();

    private static final Lamp NORMAL = new Lamp(Shape.LAMP, Wiring.NORMAL, DyeColor.WHITE);
    private static final Lamp INVERTED = new Lamp(Shape.LAMP, Wiring.INVERTED, DyeColor.WHITE);

    private LaternaTests() {
    }

    /** Put down with nothing near it, a normal lamp is dark and gives off nothing. */
    @GameTest(template = TestStructures.FLOOR)
    public static void normalStartsDark(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(NORMAL).get());
        helper.succeedWhen(() -> {
            lit(helper, WHERE, false);
            brightness(helper, WHERE, 0);
        });
    }

    /**
     * The same, inverted: lit from the moment it is placed.
     *
     * <p>This is the whole of what the second block is for, and the reason it has to be a
     * second block rather than a state on the first.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void invertedStartsLit(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(INVERTED).get());
        helper.succeedWhen(() -> {
            lit(helper, WHERE, true);
            brightness(helper, WHERE, 15);
        });
    }

    /** A signal lights a normal lamp. */
    @GameTest(template = TestStructures.FLOOR)
    public static void powerLightsNormal(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(NORMAL).get());
        helper.setBlock(SOURCE, Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            lit(helper, WHERE, true);
            brightness(helper, WHERE, 15);
        });
    }

    /** The same signal puts an inverted one out, which is the only difference between them. */
    @GameTest(template = TestStructures.FLOOR)
    public static void powerDarkensInverted(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(INVERTED).get());
        helper.setBlock(SOURCE, Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            lit(helper, WHERE, false);
            brightness(helper, WHERE, 0);
        });
    }

    /**
     * ⚠ <b>Being switched is not something a lamp can carry.</b> An inverted lamp held
     * dark by a signal, taken away and put back with nothing near it, comes up lit again:
     * the state went with the block, not with the item. That is why "inverted" is a
     * property of which block it is and cannot be a property of a lamp's state.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void breakingForgetsBeingSwitched(GameTestHelper helper) {
        helper.setBlock(WHERE, LaternaRegistry.block(INVERTED).get());
        helper.setBlock(SOURCE, Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenWaitUntil(() -> lit(helper, WHERE, false))
                .thenExecute(() -> {
                    helper.setBlock(SOURCE, Blocks.AIR);
                    helper.setBlock(WHERE, Blocks.AIR);
                    helper.setBlock(WHERE, LaternaRegistry.block(INVERTED).get());
                })
                .thenWaitUntil(() -> {
                    lit(helper, WHERE, true);
                    brightness(helper, WHERE, 15);
                })
                .thenSucceed();
    }

    /**
     * All sixteen, because the product is the claim.
     *
     * <p>A colour that failed to register, or registered under a name something else
     * already had, would show up here and nowhere else - the four tests above would pass
     * on white alone.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void everyColourLights(GameTestHelper helper) {
        for (DyeColor colour : DyeColor.values()) {
            BlockPos at = new BlockPos(colour.getId() % 8, 1 + colour.getId() / 8, 0);
            helper.setBlock(at, LaternaRegistry.block(
                    new Lamp(Shape.LAMP, Wiring.INVERTED, colour)).get());
        }
        helper.succeedWhen(() -> {
            for (DyeColor colour : DyeColor.values()) {
                BlockPos at = new BlockPos(colour.getId() % 8, 1 + colour.getId() / 8, 0);
                lit(helper, at, true);
            }
        });
    }

    private static void lit(GameTestHelper helper, BlockPos pos, boolean expected) {
        BlockState state = helper.getBlockState(pos);
        if (!state.hasProperty(LampBlock.LIT)) {
            throw new GameTestAssertException("no lamp at " + pos + ", found " + state);
        }
        if (state.getValue(LampBlock.LIT) != expected) {
            throw new GameTestAssertException(
                    "lamp at " + pos + " should be " + (expected ? "lit" : "dark"));
        }
    }

    private static void brightness(GameTestHelper helper, BlockPos pos, int expected) {
        int light = helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(pos));
        if (light != expected) {
            throw new GameTestAssertException(
                    "light at " + pos + " is " + light + ", wanted " + expected);
        }
    }
}
