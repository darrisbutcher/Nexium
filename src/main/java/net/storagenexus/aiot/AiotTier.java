package net.storagenexus.aiot;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;

import java.util.List;

/**
 * The five AIOT materials. Each AIOT mines like that material's tools, hits as hard as its axe at a sword's speed,
 * and holds every use of the five vanilla tools it is crafted from.
 */
public enum AiotTier {
    COPPER("copper", ToolMaterial.COPPER, 7.0F,
            Items.COPPER_PICKAXE, Items.COPPER_AXE, Items.COPPER_SHOVEL, Items.COPPER_HOE, Items.COPPER_SWORD),
    IRON("iron", ToolMaterial.IRON, 6.0F,
            Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD),
    GOLDEN("golden", ToolMaterial.GOLD, 6.0F,
            Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE, Items.GOLDEN_SWORD),
    DIAMOND("diamond", ToolMaterial.DIAMOND, 5.0F,
            Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_SWORD),
    NETHERITE("netherite", ToolMaterial.NETHERITE, 5.0F,
            Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_SWORD);

    /** How many vanilla tools an AIOT is made from, and so how many times a single tool's durability it holds. */
    public static final int TOOLS_PER_AIOT = 5;
    /** Attack speed modifier of a sword (4.0 base - 2.4 = 1.6 attacks a second). */
    public static final float SWORD_ATTACK_SPEED = -2.4F;

    private final String id;
    private final ToolMaterial material;
    private final float axeAttackDamageBaseline;
    private final List<Item> tools;

    AiotTier(String id, ToolMaterial material, float axeAttackDamageBaseline, Item pickaxe, Item axe, Item shovel, Item hoe, Item sword) {
        this.id = id;
        this.material = material;
        this.axeAttackDamageBaseline = axeAttackDamageBaseline;
        this.tools = List.of(pickaxe, axe, shovel, hoe, sword);
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
        return TOOLS_PER_AIOT * material.durability();
    }

    /** Pickaxe, axe, shovel, hoe and sword of this material: the merge recipe's ingredients. */
    public List<Item> tools() {
        return tools;
    }
}
