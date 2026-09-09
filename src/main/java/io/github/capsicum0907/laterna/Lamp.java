package io.github.capsicum0907.laterna;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.DyeColor;

/**
 * One lamp: a {@link Shape}, a {@link Wiring} and a colour.
 *
 * <p><b>The product is the mod.</b> {@link #all()} is the whole list of blocks that
 * exist, built once from the three axes, and every generator walks it rather than
 * keeping a list of its own. The reference implementation of this idea writes its
 * hundred and sixty blocks out one constant at a time and repeats a switch over the
 * colour names once per form; none of that is here, because none of it is a decision -
 * it is the product, spelled out by hand.
 */
public record Lamp(Shape shape, Wiring wiring, Frame frame, Optional<DyeColor> colour) {
    private static final List<Lamp> ALL = product();

    /**
     * The same lamp, named by a colour it certainly has.
     *
     * <p>Most forms are sixteen of a thing and every one of them has a colour, so
     * asking each caller to wrap it would put an {@code Optional} in front of a
     * question that is not open. The canonical constructor is the one that takes the
     * option, and it is the one the product uses.
     */
    public Lamp(Shape shape, Wiring wiring, Frame frame, DyeColor colour) {
        this(shape, wiring, frame, Optional.of(colour));
    }

    /** Every lamp there is, in the order the creative tab shows them: form, wiring, colour. */
    public static List<Lamp> all() {
        return ALL;
    }

    private static List<Lamp> product() {
        List<Lamp> lamps = new ArrayList<>();
        for (Shape shape : Shape.values()) {
            for (Frame frame : shape.frames()) {
                for (Wiring wiring : shape.wirings()) {
                    for (Optional<DyeColor> colour : shape.colours()) {
                        lamps.add(new Lamp(shape, wiring, frame, colour));
                    }
                }
            }
        }
        return List.copyOf(lamps);
    }

    /** What the block and its item are registered as: {@code inverted_light_blue_lamp}. */
    public String id() {
        return frame.prefix() + wiring.prefix() + named(colour) + shape.id();
    }

    /**
     * The colour's part of a name, with the underscore that joins it on, or nothing.
     *
     * <p>One place, because the id, the texture's name and the tag are all built the
     * same way and a colourless form has to fall out of all three identically. Written
     * separately in each, {@code glowing_glass} would have come out as
     * {@code _glowing_glass} in whichever one was missed.
     */
    private static String named(Optional<DyeColor> colour) {
        return colour.map(dye -> dye.getName() + "_").orElse("");
    }

    /**
     * The state this lamp is in when it is placed with nothing powering it, which is also
     * the picture its item shows. An inverted lamp in the hand looks lit because that is
     * what putting it down gives you.
     */
    public boolean litByDefault() {
        return wiring.litWhenUnpowered();
    }

    /**
     * The texture a form wears in a colour, lit or not.
     *
     * <p><b>Not per lamp.</b> A normal lamp and an inverted one are the same picture -
     * that is the whole of what "they look identical" means - so the skin is keyed on
     * the form and the colour only, and the two blocks point at the same two files.
     */
    public static String skin(Shape shape, Frame frame, Optional<DyeColor> colour,
            boolean lit) {
        String state = shape.switched() ? (lit ? "_on" : "_off") : "";
        return frame.prefix() + named(colour) + shape.id() + state;
    }

    public String skin(boolean lit) {
        return skin(shape, frame, colour, lit);
    }

    /** Whether this lamp has a {@code LIT} state for redstone to flip. */
    public boolean switched() {
        return wiring.switched();
    }

    /**
     * The tag that gathers every colour of one form and wiring, which is what the dyeing
     * recipes take in: dyeing a lamp gives a lamp, and dyeing an inverted one gives an
     * inverted one, without either recipe naming a colour.
     */
    public static String tag(Shape shape, Wiring wiring, Frame frame) {
        String id = shape.id();
        // ⚠ Glass is already plural-looking. Left to add an "s" like everything else it
        // came out as glowing_glasss, which nothing would have complained about.
        return frame.prefix() + wiring.prefix() + id + (id.endsWith("s") ? "es" : "s");
    }

    /**
     * What the player sees: {@code Inverted Light Blue Lamp}.
     *
     * <p><b>Built out of the same three parts the name is built from</b>, so there is no
     * table of English words anywhere and no way for one to fall out of step with the id.
     * Simply Light keeps a sixteen-case switch over the colours in every block class;
     * this is the whole of what that switch was for.
     */
    public String displayName() {
        return frame.namePrefix() + wiring.namePrefix()
                + colour.map(dye -> titleCase(dye.getName()) + " ").orElse("")
                + titleCase(shape.id());
    }

    /** {@code light_blue} to {@code Light Blue}. */
    public static String titleCase(String underscored) {
        StringBuilder name = new StringBuilder();
        for (String word : underscored.split("_")) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }
        return name.toString();
    }
}
