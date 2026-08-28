package io.github.capsicum0907.laterna;

import java.util.List;

import net.minecraft.world.level.block.SoundType;

/**
 * The forms a lamp comes in.
 *
 * <p><b>One entry here is sixteen blocks, or thirty-two where both wirings apply.</b>
 * Nothing else in the mod holds a list of lamps: registration, models, textures, loot,
 * recipes, language and tags are all read off this enum crossed with the game's sixteen
 * dye colours. Adding a form is one line; adding a colour is not possible, which is the
 * point - the sixteen are the game's and are not ours to extend.
 *
 * <p>What lives on a shape is everything that differs <em>between</em> forms and not
 * between colours: how it sounds, how long it takes to break, and which wirings it comes
 * in. A form that is always lit will simply return one wiring, and everything downstream
 * follows without being told.
 */
public enum Shape {
    /**
     * A full cube. The only form that reacts to redstone, and so the only one with two
     * wirings.
     *
     * <p>Hardness is glowstone's, and so is the sound: it is a lamp made of the stuff,
     * and a player who has broken glowstone already knows how long this takes.
     */
    LAMP("lamp", SoundType.GLASS, 0.3F, 16.0, List.of(Wiring.NORMAL, Wiring.INVERTED)),

    /**
     * A round lens in a grey ring, set flush into whatever face it is put on.
     *
     * <p>The form with no thickness at all: what is drawn is a plate, and it carries a
     * shape one pixel deep only because a block with none cannot be pointed at or broken.
     * Always lit, so sixteen blocks and one state.
     */
    SPOTLIGHT("spotlight", SoundType.GLASS, 0.3F, 1.0, List.of(Wiring.ALWAYS)),

    /**
     * Half a lamp, laid against any of the six faces. The everyday one: a ceiling of
     * these is a lit ceiling.
     */
    SLAB("lamp_slab", SoundType.GLASS, 0.3F, 8.0, List.of(Wiring.ALWAYS)),

    /**
     * A quarter as deep again, for a light that is meant to disappear into the surface
     * it is set in.
     *
     * <p>The name carries {@code lamp} in front of it, as the slab does, because
     * {@code white_panel} and {@code white_slab} read as building blocks rather than as
     * lights - and {@code white_slab} in particular reads as stone.
     */
    PANEL("lamp_panel", SoundType.GLASS, 0.3F, 4.0, List.of(Wiring.ALWAYS));

    private final String id;
    private final SoundType sound;
    private final float strength;
    private final double depth;
    private final List<Wiring> wirings;

    Shape(String id, SoundType sound, float strength, double depth, List<Wiring> wirings) {
        this.id = id;
        this.sound = sound;
        this.strength = strength;
        this.depth = depth;
        this.wirings = wirings;
    }

    /** The word that ends every name this form owns. */
    public String id() {
        return id;
    }

    public SoundType sound() {
        return sound;
    }

    public float strength() {
        return strength;
    }

    public List<Wiring> wirings() {
        return wirings;
    }

    /**
     * How deep this form is, in pixels of the sixteen.
     *
     * <p><b>One number, read by three things</b>: the block's outline, the box the model
     * is built from, and nothing else. The cube fills its cell and so is sixteen.
     *
     * <p>⚠ <b>The spotlight is one here and nought in the model.</b> Its drawing is a
     * plate with no thickness; the pixel is what makes it possible to point at. That gap
     * is deliberate, and it is the only place the two numbers differ.
     */
    public double depth() {
        return depth;
    }

    /**
     * Whether this form has a {@code LIT} state, and so two textures and two models
     * rather than one. Read off the wirings, because that is the same question.
     */
    public boolean switched() {
        return wirings.stream().anyMatch(Wiring::switched);
    }
}
