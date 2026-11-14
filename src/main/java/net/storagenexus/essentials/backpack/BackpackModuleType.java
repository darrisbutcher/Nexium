package net.storagenexus.essentials.backpack;

/** The Backpack's modules. Each fits a Backpack once, in one of its module slots, and is switched on or off in the Backpack's screen. */
public enum BackpackModuleType {
    /** Places torches from the Backpack on dark spawnable ground around the player, spaced so nothing hostile can spawn. */
    AUTO_TORCH("auto_torch"),
    /** Torches stack to 256 in the Backpack. */
    TORCH_STACK("torch_stack");

    private final String id;

    BackpackModuleType(String id) {
        this.id = id;
    }

    /** Item id prefix, as in {@code nexusessentials:<id>_module}, and lang key part. */
    public String id() {
        return id;
    }
}
