package net.storagenexus.drawer;

/** Wood types drawers come in. Textures reuse the vanilla planks and stripped logs. */
public enum DrawerWood {
    OAK("oak", "Oak", "oak_planks", "stripped_oak_log"),
    SPRUCE("spruce", "Spruce", "spruce_planks", "stripped_spruce_log"),
    BIRCH("birch", "Birch", "birch_planks", "stripped_birch_log"),
    JUNGLE("jungle", "Jungle", "jungle_planks", "stripped_jungle_log"),
    ACACIA("acacia", "Acacia", "acacia_planks", "stripped_acacia_log"),
    DARK_OAK("dark_oak", "Dark Oak", "dark_oak_planks", "stripped_dark_oak_log"),
    MANGROVE("mangrove", "Mangrove", "mangrove_planks", "stripped_mangrove_log"),
    CHERRY("cherry", "Cherry", "cherry_planks", "stripped_cherry_log"),
    PALE_OAK("pale_oak", "Pale Oak", "pale_oak_planks", "stripped_pale_oak_log"),
    POPLAR("poplar", "Poplar", "poplar_planks", "stripped_poplar_log"),
    BAMBOO("bamboo", "Bamboo", "bamboo_planks", "stripped_bamboo_block"),
    CRIMSON("crimson", "Crimson", "crimson_planks", "stripped_crimson_stem"),
    WARPED("warped", "Warped", "warped_planks", "stripped_warped_stem");

    private final String id;
    private final String displayName;
    private final String planks;
    private final String trim;

    DrawerWood(String id, String displayName, String planks, String trim) {
        this.id = id;
        this.displayName = displayName;
        this.planks = planks;
        this.trim = trim;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String planks() { return planks; }
    public String trim() { return trim; }
}
