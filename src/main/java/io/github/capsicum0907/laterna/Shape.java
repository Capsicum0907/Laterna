package io.github.capsicum0907.laterna;

import java.util.List;
import java.util.Optional;

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
    LAMP("lamp", SoundType.GLASS, 0.3F, 16.0, Mount.NONE,
            List.of(Wiring.NORMAL, Wiring.INVERTED)),

    /**
     * A round lens in a grey ring, set flush into whatever face it is put on.
     *
     * <p>The form with no thickness at all: what is drawn is a plate, and it carries a
     * shape one pixel deep only because a block with none cannot be pointed at or broken.
     * Always lit, so sixteen blocks and one state.
     */
    SPOTLIGHT("spotlight", SoundType.GLASS, 0.3F, 1.0, Mount.ANY, List.of(Wiring.ALWAYS)),

    /**
     * Half a lamp, laid against any of the six faces. The everyday one: a ceiling of
     * these is a lit ceiling.
     */
    SLAB("lamp_slab", SoundType.GLASS, 0.3F, 8.0, Mount.FLAT, List.of(Wiring.ALWAYS)),

    /**
     * The same slab stood on its edge, against a wall.
     *
     * ⚠ <b>A separate block, and that is the point.</b> One block cannot be placed the
     * way a slab is placed and also the way a wall panel is placed: a click on the side of
     * a block has to mean one of the two. Splitting them lets each be placed the way its
     * own shape is expected to be, and a single item on the bench turns one into the
     * other.
     */
    VERTICAL_SLAB("vertical_lamp_slab", SoundType.GLASS, 0.3F, 8.0, Mount.UPRIGHT,
            List.of(Wiring.ALWAYS)),

    /**
     * A quarter as deep again, for a light that is meant to disappear into the surface
     * it is set in.
     *
     * <p>The name carries {@code lamp} in front of it, as the slab does, because
     * {@code white_panel} and {@code white_slab} read as building blocks rather than as
     * lights - and {@code white_slab} in particular reads as stone.
     */
    PANEL("lamp_panel", SoundType.GLASS, 0.3F, 4.0, Mount.FLAT, List.of(Wiring.ALWAYS)),

    /** The panel stood on its edge; see {@link #VERTICAL_SLAB}. */
    VERTICAL_PANEL("vertical_lamp_panel", SoundType.GLASS, 0.3F, 4.0, Mount.UPRIGHT,
            List.of(Wiring.ALWAYS));

    /**
     * How a form meets the block it is put against, which decides what states it keeps
     * and how a click on it is read.
     *
     * <p>Not a setting on one block but a choice of block: each mounting is a different
     * set of states, so each is its own class under {@code PlateBlock}.
     */
    public enum Mount {
        /** Not a plate at all: the cube, which fills its cell. */
        NONE,
        /** Any of the six faces, taken from the face that was clicked. */
        ANY,
        /** Lying down, top or bottom, placed by vanilla's rule for a slab. */
        FLAT,
        /** Standing on its edge against one of the four walls. */
        UPRIGHT
    }

    private final String id;
    private final SoundType sound;
    private final float strength;
    private final double depth;
    private final Mount mount;
    private final List<Wiring> wirings;

    Shape(String id, SoundType sound, float strength, double depth, Mount mount,
            List<Wiring> wirings) {
        this.id = id;
        this.sound = sound;
        this.strength = strength;
        this.depth = depth;
        this.mount = mount;
        this.wirings = wirings;
    }

    public Mount mount() {
        return mount;
    }

    /**
     * Whether two of these laid together make one whole block.
     *
     * ⚠ <b>A separate question from {@link #mount}.</b> The slab and the panel are
     * mounted identically - both lie down - and differ only here, so folding this into
     * the mounting would make that enum a product of two things and every switch over it
     * grow an arm it does not care about.
     *
     * <p>Only the slab. Two panels are eight pixels and not sixteen, and a pair of them
     * drawn as a whole block would be a lie about how much light is there; reaching a
     * whole block out of quarters is a block of layers, which is a different thing.
     */
    public boolean stacks() {
        return switch (this) {
            case SLAB -> true;
            case LAMP, SPOTLIGHT, VERTICAL_SLAB, PANEL, VERTICAL_PANEL -> false;
        };
    }

    /**
     * The same form turned the other way, where there is one.
     *
     * <p>A pair here is a pair on the bench: one of either makes one of the other, so
     * neither has to be built from materials twice. A switch rather than a field, because
     * an enum cannot name a constant declared after it - and because adding a form then
     * has to say whether it has a turned twin.
     */
    public Optional<Shape> turned() {
        return switch (this) {
            case LAMP, SPOTLIGHT -> Optional.empty();
            case SLAB -> Optional.of(VERTICAL_SLAB);
            case VERTICAL_SLAB -> Optional.of(SLAB);
            case PANEL -> Optional.of(VERTICAL_PANEL);
            case VERTICAL_PANEL -> Optional.of(PANEL);
        };
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
