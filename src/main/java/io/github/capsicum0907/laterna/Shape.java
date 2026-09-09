package io.github.capsicum0907.laterna;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
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
    ROD("rod", SoundType.GLASS, 0.3F, 16.0, 7.0, Mount.AXIS, List.of(Wiring.ALWAYS)),

    /**
     * A lamp in a case: an opaque core with a clear cover around it, filling its cell.
     *
     * <p>The bulb with its base taken off and grown to a whole block - the same nesting,
     * and the same nesting vanilla's beacon uses. Always lit, and the only cube here that
     * is not the plain one.
     */
    CASED("cased_lamp", SoundType.GLASS, 0.3F, 16.0, 4.0, Mount.NONE, List.of(Wiring.ALWAYS)),

    /**
     * Glass with the light in the glass, rather than a lamp behind glass.
     *
     * <p>The cased lamp is a core inside a cover and reads as a lamp in a case; this is
     * the cover on its own, glowing, filling its cell. So it is the one form here that is
     * a building material first: a window that is its own light, a floor you can see
     * through and stand on in the dark.
     *
     * <p><b>The only form that comes in seventeen.</b> Vanilla has glass and sixteen
     * stained glasses, and this follows it - the clear one is not a colour that was left
     * out, it is what the others are made from.
     *
     * <p>⚠ <b>And the only one offered without a frame.</b> A lamp's border is what stops
     * a wall of them reading as one wall, which is a thing worth having; a window's border
     * is the thing you are looking through, and a wall of frameless glass being one sheet
     * is the whole point of it. {@link Frame#NONE} is offered here and nowhere else.
     */
    GLASS("glowing_glass", SoundType.GLASS, 0.3F, 16.0, Mount.NONE, List.of(Wiring.ALWAYS)),

    /**
     * The one form that is not a lamp: it takes light out of the air around it rather
     * than putting any in.
     *
     * <p><b>There is no such thing as a negative light source.</b> Brightness is an
     * unsigned four-bit number and light spreads by each cell taking the brightest of its
     * neighbours less its own opacity, so a source can only add and a cell can only
     * subtract. The lever that takes light away is therefore opacity and never emission,
     * and this form is a cell whose opacity is the whole fifteen while it is working:
     * every neighbour's light arrives at nought, and a torch inside a room built of these
     * lights its own cell and nothing else.
     *
     * <p><b>Invisible, and so it is glass to look through and a wall to light.</b> One
     * of these over an opening is a room that is dark at noon and still has a view; a cell
     * filled with them is a dark room. Which of the two you get is how it is built and not
     * a setting on it.
     *
     * <p>⚠ <b>Opacity is cached per block state, which is what makes this cost nothing.</b>
     * The game keeps one value per state and compares the two when a block changes, so
     * flipping {@code LIT} relights the neighbourhood by itself - no block entity, no
     * ticking, and nothing filling a radius. An opacity that varied by position instead
     * would be read once at startup and quietly ignored.
     */
    SHADE("shade", SoundType.WOOL, 0.3F, 16.0, Mount.NONE,
            List.of(Wiring.NORMAL, Wiring.INVERTED));

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

    /** The game's sixteen, which is what most forms come in. */
    private static final List<Optional<DyeColor>> DYES =
            Arrays.stream(DyeColor.values()).map(Optional::of).toList();

    /** The same, with the colourless one in front of them, which is what glass comes in. */
    private static final List<Optional<DyeColor>> CLEAR_AND_DYES =
            Stream.concat(Stream.of(Optional.<DyeColor>empty()), DYES.stream()).toList();

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
     * <p>⚠ <b>This is the whole of the form, collar and all.</b> A fitting is eight by
     * four by three of block, of which the pixel nearest the surface is a dark collar and
     * the rest is lamp - the collar is stacked behind the light rather than wrapped around
     * it, which is the only arrangement that keeps both the outline and the light.
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
            case CASED -> throw new IllegalStateException("a cased lamp fills its cell");
            case SHADE -> throw new IllegalStateException("a shade fills its cell");
            case GLASS -> throw new IllegalStateException("glowing glass fills its cell");
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
    /**
     * The frame colours this form comes in.
     *
     * <p>⚠ <b>Only the forms whose border is the lamp's own colour.</b> What edges a
     * spotlight, a bulb, a fitting or a cased lamp is a grey fitting or a case - already
     * not the lamp's colour - so there is nothing there to fix, and offering the choice
     * would be three identical blocks.
     */
    public List<Frame> frames() {
        return switch (this) {
            case LAMP, SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL ->
                    List.of(Frame.OWN, Frame.BLACK, Frame.WHITE);
            case SPOTLIGHT, BULB, FIXTURE, ROD, CASED, SHADE -> List.of(Frame.OWN);
            // A border on a window is the thing you are looking through, so this is
            // the one form where not drawing it is a form of its own.
            case GLASS -> List.of(Frame.OWN, Frame.NONE);
        };
    }

    /**
     * The colours this form comes in.
     *
     * <p><b>The fourth axis, asked for the same way the other two are.</b> A form says
     * which frames and which wirings it has; this is the same question about colour, and
     * it is here rather than in {@link Lamp#all()} so that a form which is not sixteen of
     * anything costs a line in this enum and nothing anywhere else.
     *
     * <p>An empty value is a form with no colour in its name at all - not a colour that
     * has not been chosen. Everything downstream reads it as "leave the colour out": out
     * of the id, out of the name, out of the texture's name.
     */
    public List<Optional<DyeColor>> colours() {
        return switch (this) {
            case LAMP, SPOTLIGHT, SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL, BULB, FIXTURE,
                    ROD, CASED -> DYES;
            // Invisible, so there is nothing for a colour to be.
            case SHADE -> List.of(Optional.empty());
            // ⚠ Seventeen, and the clear one first: it is not a colour that was left
            // out, it is the one the other sixteen are dyed from.
            case GLASS -> CLEAR_AND_DYES;
        };
    }

    /**
     * The one colour of this form that is made out of materials; every other colour is
     * that one with a dye.
     *
     * <p>⚠ <b>An empty value here does not mean "no base".</b> It means the colourless
     * one is the base - the same reading {@link #colours()} gives an empty value - so a
     * form whose plain variant is the thing you craft says so by returning empty. Testing
     * this against a lamp's own colour is therefore {@code equals} on two options and
     * never a comparison of dyes.
     */
    public Optional<DyeColor> base() {
        return switch (this) {
            case LAMP, SPOTLIGHT, SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL, BULB, FIXTURE,
                    ROD, CASED -> Optional.of(DyeColor.WHITE);
            case SHADE, GLASS -> Optional.empty();
        };
    }

    /**
     * The dye a colourless variant of this form is drawn in.
     *
     * <p>⚠ <b>Having no colour in its name is not having no picture.</b> The masters are
     * grayscale and every one of them is given a colour before it is written out, so a
     * form with a colourless variant has to say which - and a form without one has no
     * answer to give rather than a default, or the first form to grow one would be
     * painted whatever the default happened to be and nobody would be told.
     */
    public DyeColor plain() {
        return switch (this) {
            // Clear glass is white glass with nothing added, which is what the game's own
            // dyeing recipes say about it.
            case GLASS -> DyeColor.WHITE;
            case LAMP, SPOTLIGHT, SLAB, VERTICAL_SLAB, PANEL, VERTICAL_PANEL, BULB, FIXTURE,
                    ROD, CASED, SHADE ->
                    throw new IllegalStateException(this + " has no colourless form to draw");
        };
    }

    /**
     * Whether this form is a light.
     *
     * <p>Every form was one until {@link #SHADE}, which is why the brightness a block
     * gives off used to be read straight off its wiring. The two questions are not the
     * same: a shade is switched, and what its switch turns on is the taking away.
     */
    public boolean emits() {
        return this != SHADE;
    }

    public boolean stacks() {
        return switch (this) {
            case SLAB, VERTICAL_SLAB -> true;
            case LAMP, SPOTLIGHT, PANEL, VERTICAL_PANEL, BULB, FIXTURE, ROD, CASED, SHADE,
                    GLASS -> false;
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
            case LAMP, SPOTLIGHT, BULB, FIXTURE, ROD, CASED, SHADE, GLASS ->
                    Optional.empty();
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
