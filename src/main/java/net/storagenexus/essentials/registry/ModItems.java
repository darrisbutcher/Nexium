package net.storagenexus.essentials.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.bedrock.BedrockBreakerElixirItem;
import net.storagenexus.essentials.capture.CapturedMobItem;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.farm.FarmItem;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.ironfarm.IronFarmItem;
import net.storagenexus.essentials.magnet.MagnetItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    public static final MagnetItem MAGNET = register("magnet", MagnetItem::new, new Item.Properties()
            .stacksTo(1)
            .component(ModComponents.MAGNET_RANGE, MagnetItem.DEFAULT_RANGE)
            .component(ModComponents.MAGNET_ACTIVE, true));

    /** Drink it to mine bedrock for a while. The duration comes from the config. */
    public static final BedrockBreakerElixirItem BEDROCK_BREAKER_ELIXIR = register("bedrock_breaker_elixir", BedrockBreakerElixirItem::new, new Item.Properties()
            .stacksTo(16)
            .rarity(Rarity.RARE)
            .component(DataComponents.CONSUMABLE, Consumables.defaultDrink()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(ModEffects.BEDROCK_BREAKER,
                            NexusConfig.get().bedrockBreakerSeconds * 20)))
                    .build())
            .usingConvertsTo(Items.GLASS_BOTTLE));

    /** A picked-up mob. Only made by sneak + right-clicking a mob, so it isn't in any creative tab. */
    public static final CapturedMobItem CAPTURED_MOB = register("captured_mob", CapturedMobItem::new, new Item.Properties()
            .stacksTo(1)
            .delayedComponent(DataComponents.DAMAGE_RESISTANT, context -> new DamageResistant(context.getOrThrow(ModTags.CAPTURED_MOB_RESISTS))));

    /** Makes the one-block farms and the Master Librarian. */
    public static final net.minecraft.world.item.BlockItem FARM_ASSEMBLER = register("farm_assembler",
            p -> new net.minecraft.world.item.BlockItem(ModBlocks.FARM_ASSEMBLER, p), new Item.Properties().useBlockDescriptionPrefix());

    /** The 1-block iron farm. Only made at the Farm Assembler, but creative players find it in the Nexium tab. */
    public static final IronFarmItem IRON_FARM = register("iron_farm", p -> new IronFarmItem(ModBlocks.IRON_FARM, p), new Item.Properties()
            .useBlockDescriptionPrefix()
            .rarity(Rarity.UNCOMMON));

    /** The Experience Farm. Only made at the Farm Assembler, but creative players find it in the Farms and Power tab. */
    public static final net.storagenexus.essentials.xpfarm.XpFarmItem XP_FARM = register("xp_farm",
            p -> new net.storagenexus.essentials.xpfarm.XpFarmItem(ModBlocks.XP_FARM, p), new Item.Properties()
                    .useBlockDescriptionPrefix()
                    .rarity(Rarity.UNCOMMON));
    /** The other one-block farms. Only made at the Farm Assembler, but creative players find them in the Nexium tab. */
    public static final Map<FarmKind, FarmItem> FARMS = new EnumMap<>(FarmKind.class);

    static {
        for (FarmKind kind : FarmKind.values()) {
            FARMS.put(kind, register(kind.id(), p -> new FarmItem(kind, ModBlocks.FARMS.get(kind), p), new Item.Properties()
                    .useBlockDescriptionPrefix()
                    .rarity(Rarity.UNCOMMON)));
        }
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, NexusEssentials.id(name));
        T item = factory.apply(props.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    /**
     * What the Nexium creative tab shows after its own items. The Captured Mob is left out: an empty one is
     * meaningless, so it only comes from picking up a mob.
     */
    public static final List<Item> TAB_ORDER = java.util.stream.Stream.concat(
            java.util.stream.Stream.of(MAGNET, BEDROCK_BREAKER_ELIXIR, FARM_ASSEMBLER, IRON_FARM, XP_FARM), FARMS.values().stream()).map(i -> (Item) i).toList();

    public static void init() {}

    private ModItems() {}
}
