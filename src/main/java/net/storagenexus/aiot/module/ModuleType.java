package net.storagenexus.aiot.module;

/** The AIOT modules. Each fits any AIOT once, in one of its module slots, and is switched on or off in the tool menu. */
public enum ModuleType {
    /** Mined drops go straight into the storage network of a linked Crafting Terminal. */
    NEXUS_LINK("nexus_link"),
    /** Mines the 3x3 around the block, facing the player. */
    AREA_MINING("area_mining"),
    /** Mining an ore or a log mines every connected block of the same kind. */
    VEIN_MINING("vein_mining");

    private final String id;

    ModuleType(String id) {
        this.id = id;
    }

    /** Item id prefix, as in {@code storagenexus:<id>_module}, and lang key part. */
    public String id() {
        return id;
    }
}
