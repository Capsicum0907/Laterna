package io.github.capsicum0907.laterna;

/**
 * What a lamp does when nothing is powering it, which is the only thing that separates
 * a normal lamp from an inverted one.
 *
 * <p><b>This has to be a block and cannot be a block state.</b> {@code LIT} is a state,
 * and a state does not survive being carried as an item: a lamp broken and put back down
 * comes up in whatever its block says the default is. So "lit until powered" is not a
 * setting a single block can hold on to — it is a second block that looks the same and
 * starts from the other end. Simply Light reaches the same shape by a different route,
 * passing the default into the constructor; the reason is this one.
 */
public enum Wiring {
    /** Dark until a signal reaches it. */
    NORMAL(false, "", ""),
    /** Lit until a signal reaches it, and dark while one does. */
    INVERTED(true, "inverted_", "Inverted ");

    private final boolean litWhenUnpowered;
    private final String prefix;
    private final String namePrefix;

    Wiring(boolean litWhenUnpowered, String prefix, String namePrefix) {
        this.litWhenUnpowered = litWhenUnpowered;
        this.prefix = prefix;
        this.namePrefix = namePrefix;
    }

    public boolean litWhenUnpowered() {
        return litWhenUnpowered;
    }

    /**
     * Put in front of every name this wiring owns - the block, the item, the tag - so
     * that one word decides all three and they cannot drift apart.
     */
    public String prefix() {
        return prefix;
    }

    /** The same distinction as {@link #prefix()}, in the language the player reads. */
    public String namePrefix() {
        return namePrefix;
    }
}
