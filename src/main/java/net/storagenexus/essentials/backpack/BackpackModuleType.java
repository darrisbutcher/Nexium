package net.storagenexus.essentials.backpack;

/** The Backpack's modules. Each fits a Backpack once, in one of its module slots, and is switched on or off in the Backpack's screen. */
public enum BackpackModuleType {
    /** Places torches from the Backpack on dark spawnable ground around the player, spaced so nothing hostile can spawn. */
    AUTO_TORCH("auto_torch", false),
    /**
     * Retired: every stackable item stacks to {@value BackpackContents#STACK_MAX} in a Backpack now. The item stays registered so a
     * Backpack or chest that still holds one loads, but it does nothing, has no recipe and can no longer be put in a slot.
     */
    TORCH_STACK("torch_stack", true),
    /** Eats food from the Backpack when you are hungry enough that none of it is wasted. */
    FEEDING("feeding", false),
    /** Packs what the Backpack holds into storage blocks (9 iron ingots into an iron block). */
    COMPACTING("compacting", false),
    /** Tops up the stacks you are using (hotbar and offhand) from the Backpack, unpacking storage blocks when it has to. */
    RESTOCKING("restocking", false);

    private final String id;
    private final boolean retired;

    BackpackModuleType(String id, boolean retired) {
        this.id = id;
        this.retired = retired;
    }

    /** Item id prefix, as in {@code nexium:<id>_module}, and lang key part. */
    public String id() {
        return id;
    }

    /** A module that no longer does anything: not in the creative tab, no recipe, and refused by the module slots. */
    public boolean retired() {
        return retired;
    }
}
