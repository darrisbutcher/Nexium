package net.storagenexus.aiot;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.registry.ModComponents;

import java.util.List;
import java.util.function.Consumer;

/**
 * An all-in-one tool: pickaxe, axe, shovel, hoe and sword in one item.
 * <p>
 * Mining, combat and durability are plain data components, so the rest of the game treats it like any tool; the
 * vanilla tool tags it is in ({@code minecraft:pickaxes}, {@code axes}, {@code shovels}, {@code hoes} and
 * {@code swords}) give it every tool's enchantments, the sword's sweeping attack and the shovel's campfire dousing.
 * Right-click runs the axe, shovel and hoe block transformers in turn (sneaking puts the hoe first).
 */
public class AiotItem extends Item {
    /** Right-click: strip, scrape or unwax first, then make a path, then till (a path or anything a shovel can't flatten). */
    private static final List<ResourceKey<BlockTransformer>> USE_ORDER =
            List.of(BlockTransformers.AXE, BlockTransformers.SHOVEL, BlockTransformers.HOE);
    /** Sneak + right-click: till first, so grass and dirt become farmland instead of a path. */
    private static final List<ResourceKey<BlockTransformer>> SNEAK_USE_ORDER =
            List.of(BlockTransformers.HOE, BlockTransformers.AXE, BlockTransformers.SHOVEL);

    private final AiotTier tier;

    public AiotItem(Properties properties, AiotTier tier) {
        super(properties);
        this.tier = tier;
    }

    public AiotTier tier() {
        return tier;
    }

    /** Durability, mining, combat, enchantability and repair material of an AIOT of this tier. */
    public static Properties properties(AiotTier tier) {
        ToolMaterial material = tier.material();
        HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        // The first rule that matches a block decides its speed, and the first with a drop setting decides drops.
        Tool tool = new Tool(List.of(
                // Too weak a material: no drops (and slow), as with that material's pickaxe
                Tool.Rule.deniesDrops(blocks.getOrThrow(material.incorrectBlocksForDrops())),
                // Sword: cobwebs drop string, bamboo breaks instantly
                Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
                Tool.Rule.overrideSpeed(blocks.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE),
                Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), material.speed()),
                Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_AXE), material.speed()),
                Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), material.speed()),
                Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_HOE), material.speed()),
                // Blocks no tool above covers that a sword cuts a little faster (vines, glow lichen, cocoa...)
                Tool.Rule.overrideSpeed(blocks.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F)),
                1.0F, 1, true);
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        tier.axeAttackDamageBaseline() + material.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        AiotTier.SWORD_ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
        Properties properties = new Properties()
                .durability(tier.durability())
                .repairable(material.repairItems())
                .enchantable(material.enchantmentValue())
                .component(DataComponents.TOOL, tool)
                .attributes(attributes)
                // 1 durability a hit, like a sword; disables shields like an axe
                .component(DataComponents.WEAPON, new Weapon(1, Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS));
        return tier.fireResistant() ? properties.fireResistant() : properties;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var transformers = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER);
        for (ResourceKey<BlockTransformer> key : context.isSecondaryUseActive() ? SNEAK_USE_ORDER : USE_ORDER) {
            var transformer = transformers.get(key);
            if (transformer.isEmpty()) continue;
            InteractionResult result = transformer.get().value().transformBlock(context);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        // Silk Touch or Fortune, whichever isn't active, so the player knows the AIOT still has it
        ItemEnchantments inactive = stack.getOrDefault(ModComponents.INACTIVE_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : inactive.entrySet()) {
            tooltip.accept(Component.translatable("tooltip.storagenexus.aiot_inactive",
                    Enchantment.getFullname(entry.getKey(), entry.getIntValue())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
