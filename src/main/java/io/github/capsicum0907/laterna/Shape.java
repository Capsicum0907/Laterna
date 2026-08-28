package io.github.capsicum0907.laterna;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Direction;
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
    SPOTLIGHT("spotlight", SoundType.GLASS, 0.3F, 1.0, 4.0, Mount.ANY, List.of(Wiring.ALWAYS)),

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
            List.of(Wiring.ALWAYS)),

    /**
     * A small bulb on a face, glowing on every side of itself.
     *
     * <p>A base flush with the surface and a narrow bulb standing off it - two parts, and
     * that is what makes it read as a fitting rather than a tile. Four pixels of base with
     * six of nothing around it, and five pixels proud of the wall.
     */
    BULB("bulb", SoundType.GLASS, 0.3F, 5.0, 6.0, Mount.ANY, List.of(Wiring.ALWAYS)),

    /**
     * A shallow fitting: a plate on the surface with a lit face raised out of it. What the
     * spotlight would be if it were mounted on the wall rather than sunk into it.
     *
     * ⚠ <b>Square on every face, where the mod this follows is not.</b> Theirs is a
     * wide bar on a wall and a small disc on a floor - two models. That is a good idea and
     * is not done here yet: it wants a second box per form, one for the walls and one for
     * the floor and ceiling, and the outline has to follow it.
     */
    FIXTURE("fixture", SoundType.GLASS, 0.3F, 3.0, 4.0, Mount.ANY, List.of(Wiring.ALWAYS)),

    /**
     * A thin bar of light running the length of its cell.
     *
     * ⚠ <b>The only form that sits against no face at all.</b> It runs through the
     * middle from one side to the other, so what it keeps is an axis rather than a facing,
     * and a row of them reads as one continuous line - which is the only reason to have a
     * strip light rather than more fittings. Its inset is how thin it is across; its depth
     * is the whole cell, because that is how long it is.
     */
    ROD("rod", SoundType.GLASS, 0.3F, 16.0, 7.0, Mount.AXIS, List.of(Wiring.ALWAYS));

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
        UPRIGHT,
        /** Against no face: running through the cell along one axis, like a chain. */
        AXIS
    }

    private final String id;
    private final SoundType sound;
    private final float strength;
    private final double depth;
    private final double inset;
    private final Mount mount;
    private final List<Wiring> wirings;

    Shape(String id, SoundType sound, float strength, double depth, Mount mount,
            List<Wiring> wirings) {
        this(id, sound, strength, depth, 0.0, mount, wirings);
    }

    Shape(String id, SoundType sound, float strength, double depth, double inset, Mount mount,
            List<Wiring> wirings) {
        this.id = id;
        this.sound = sound;
        this.strength = strength;
        this.depth = depth;
        this.inset = inset;
        this.mount = mount;
        this.wirings = wirings;
    }

    /**
     * How far in from the edges of its face this form sits, in pixels of the sixteen.
     *
     * <p>Nought for everything that covers its face. The spotlight, the bulb and the
     * fitting do not, and the same one number trims the box the model is built from and
     * the outline you point at - so a small lamp cannot end up with a full-face hit box.
     *
     * ⚠ <b>The spotlight was exactly that until it was looked at.</b> Its drawing is
     * eight pixels across and its outline was the whole sixteen, so pointing anywhere near
     * it caught it. The number here is read off the rim the master actually draws.
     */
    public double inset() {
        return inset;
    }

    /**
     * How big this form is on a given face: across it, up it, and out of it, in pixels.
     *
     * <p>⚠ <b>A form may be a different size on a wall than on a floor.</b> A fitting
     * is a wide bar where it is bolted to a wall and a small disc where it is set into a
     * ceiling, which is how the mod this follows builds one - and there is no turning of a
     * single box that gives both. So the size is asked for per face, and the model
     * generator and the outline both ask the same question.
     *
     * <p>⚠ <b>This is the whole of the form, and for a fitting it is the whole of the
     * light too.</b> The fitting has no plate: the mod this follows builds it as one box
     * eight by four by three, and there is no way to keep that outline and add a plate -
     * inside it there is not enough lamp left to take a rim out of, and outside it the
     * outline stops being eight by four. What makes theirs read as a fitting is drawn in
     * the texture, not built out of boxes.
     *
     * <p>Everything else answers the same whichever face it is on, out of its depth and
     * its inset, and says so once here rather than in each of them.
     */
    public Fit fit(Direction facing) {
        boolean flat = facing.getAxis() == Direction.Axis.Y;
        return switch (this) {
            case FIXTURE -> flat ? new Fit(4.0, 4.0, 2.0) : new Fit(8.0, 4.0, 3.0);
            case LAMP, SPOTLIGHT, SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL, BULB ->
                    new Fit(16.0 - 2 * inset, 16.0 - 2 * inset, depth);
            // ⚠ Asking a rod which face it is on has no answer, and quietly making one
            // up would put a rod-shaped hole in whatever asked.
            case ROD -> throw new IllegalStateException("a rod sits against no face");
        };
    }

    /**
     * How much of its cell a form takes up on one face.
     *
     * @param wide across the face - along the wall, or either way on a floor
     * @param tall up the face; the same as {@code wide} on a floor or a ceiling
     * @param deep out of the face
     */
    public record Fit(double wide, double tall, double deep) {
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
     * <p>The slabs and not the panels. Two panels are eight pixels and not sixteen, and a
     * pair of them drawn as a whole block would be a lie about how much light is there;
     * reaching a whole block out of quarters is a block of layers, which is a different
     * thing.
     */
    public boolean stacks() {
        return switch (this) {
            case SLAB, VERTICAL_SLAB -> true;
            case LAMP, SPOTLIGHT, PANEL, VERTICAL_PANEL, BULB, FIXTURE, ROD -> false;
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
            case LAMP, SPOTLIGHT, BULB, FIXTURE, ROD -> Optional.empty();
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
