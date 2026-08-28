package io.github.capsicum0907.laterna;

import java.util.OptionalInt;

/**
 * What colour the border of a lamp is.
 *
 * <p>By default a lamp's frame is its own colour darkened, which is what keeps sixteen
 * colours looking like sixteen of one thing. Fixed to black or to white it becomes a
 * border that does not move with the lamp - so a wall of eight colours can share one
 * outline, which is the whole reason to want it.
 *
 * <p>⚠ <b>Another axis of blocks, not another state.</b> Like the wiring, a frame colour
 * does not survive being carried as an item, so each is its own block: the framed forms go
 * from ninety-six blocks to two hundred and eighty-eight.
 *
 * <p>⚠ <b>Only the forms that have a frame.</b> A spotlight, a bulb, a fitting, a rod and
 * a cased lamp have none - what edges them is a grey fitting or a case, which is not the
 * lamp's colour to begin with - so they come in this one alone.
 */
public enum Frame {
    /** The lamp's own colour, darkened: the frame every lamp had before there was a choice. */
    OWN("", ""),
    /** Fixed black, whatever colour the lamp is. */
    BLACK("black_framed_", "Black-framed ", 0x191919),
    /** Fixed white. */
    WHITE("white_framed_", "White-framed ", 0xE9ECEC);

    private final String prefix;
    private final String namePrefix;
    private final OptionalInt colour;

    Frame(String prefix, String namePrefix) {
        this.prefix = prefix;
        this.namePrefix = namePrefix;
        this.colour = OptionalInt.empty();
    }

    Frame(String prefix, String namePrefix, int colour) {
        this.prefix = prefix;
        this.namePrefix = namePrefix;
        this.colour = OptionalInt.of(colour);
    }

    /** Put in front of every name this frame owns - the block, the item, the texture, the tag. */
    public String prefix() {
        return prefix;
    }

    /** The same distinction in the language the player reads. */
    public String namePrefix() {
        return namePrefix;
    }

    /**
     * The colour the border is painted, where it is fixed.
     *
     * <p>Empty for {@link #OWN}, whose border is worked out from the lamp's own colour and
     * so has no value of its own to give.
     */
    public OptionalInt colour() {
        return colour;
    }
}
