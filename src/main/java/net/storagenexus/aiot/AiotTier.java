package net.storagenexus.aiot;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.storagenexus.nexium.NexiumMaterials;
import net.storagenexus.registry.ModItems;

import java.util.List;
import java.util.function.Supplier;

/**
 * The AIOT materials: the six vanilla tool materials from stone up, and Nexium. Each AIOT mines like that
 * material's tools, hits as hard as its axe at a sword's speed, and holds one and a half times every use of the five
 * tools it is crafted from.
 */
public enum AiotTier {
    STONE("stone", ToolMaterial.STONE, 7.0F, () -> List.of(
            Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL, Items.STONE_HOE, Items.STONE_SWORD)),
    COPPER("copper", ToolMaterial.COPPER, 7.0F, () -> List.of(
            Items.COPPER_PICKAXE, Items.COPPER_AXE, Items.COPPER_SHOVEL, Items.COPPER_HOE, Items.COPPER_SWORD)),
    IRON("iron", ToolMaterial.IRON, 6.0F, () -> List.of(
            Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD)),
    GOLDEN("golden", ToolMaterial.GOLD, 6.0F, () -> List.of(
            Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE, Items.GOLDEN_SWORD)),
    DIAMOND("diamond", ToolMaterial.DIAMOND, 5.0F, () -> List.of(
            Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_SWORD)),
    NETHERITE("netherite", ToolMaterial.NETHERITE, 5.0F, () -> List.of(
            Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_SWORD)),
    /** The mod's own tier above netherite; its tools are registered in {@link ModItems}, hence the supplier. */
    NEXIUM("nexium", NexiumMaterials.TOOL, 5.0F, () -> List.of(
            ModItems.NEXIUM_PICKAXE, ModItems.NEXIUM_AXE, ModItems.NEXIUM_SHOVEL, ModItems.NEXIUM_HOE, ModItems.NEXIUM_SWORD));

    /** How many vanilla tools an AIOT is made from, and so how many times a single tool's durability it holds. */
    public static final int TOOLS_PER_AIOT = 5;
    /** An AIOT lasts this many times the durability of its five tools together (rounded to a whole number of uses). */
    public static final float DURABILITY_FACTOR = 1.5F;
    /** Attack speed modifier of a sword (4.0 base - 2.4 = 1.6 attacks a second). */
    public static final float SWORD_ATTACK_SPEED = -2.4F;

    private final String id;
    private final ToolMaterial material;
    private final float axeAttackDamageBaseline;
    private final Supplier<List<Item>> tools;

    /** @param tools pickaxe, axe, shovel, hoe and sword, in that order */
    AiotTier(String id, ToolMaterial material, float axeAttackDamageBaseline, Supplier<List<Item>> tools) {
        this.id = id;
        this.material = material;
        this.axeAttackDamageBaseline = axeAttackDamageBaseline;
        this.tools = tools;
    }

    /** Item id prefix, as in {@code storagenexus:<id>_aiot}. Matches vanilla's tool names (golden, not gold). */
    public String id() {
        return id;
    }

    public ToolMaterial material() {
        return material;
    }

    /** The same baseline vanilla gives this material's axe, so the AIOT hits exactly as hard as that axe. */
    public float axeAttackDamageBaseline() {
        return axeAttackDamageBaseline;
    }

    /** Total attack damage shown in the tooltip: 1 (hand) + the material bonus + the axe baseline. */
    public float attackDamage() {
        return 1.0F + material.attackDamageBonus() + axeAttackDamageBaseline;
    }

    public int durability() {
        return Math.round(TOOLS_PER_AIOT * material.durability() * DURABILITY_FACTOR);
    }

    /** Pickaxe, axe, shovel, hoe and sword of this material: the merge recipe's ingredients. */
    public List<Item> tools() {
        return tools.get();
    }

    /** Netherite and Nexium AIOTs survive fire and lava, like netherite gear. */
    public boolean fireResistant() {
        return this == NETHERITE || this == NEXIUM;
    }
}
