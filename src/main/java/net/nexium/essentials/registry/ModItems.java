package net.nexium.essentials.registry;

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
import net.nexium.essentials.NexiumEssentials;
import net.nexium.essentials.backpack.BackpackItem;
import net.nexium.essentials.backpack.BackpackModuleItem;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.bedrock.BedrockBreakerElixirItem;
import net.nexium.essentials.capture.CapturedMobItem;
import net.nexium.essentials.config.NexusConfig;
import net.nexium.essentials.farm.FarmItem;
import net.nexium.essentials.farm.FarmKind;
import net.nexium.essentials.ironfarm.IronFarmItem;
import net.nexium.essentials.magnet.MagnetItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    public static final MagnetItem MAGNET = register("magnet", MagnetItem::new, new Item.Properties()
            .stacksTo(1)
            .component(ModComponents.MAGNET_RANGE, MagnetItem.DEFAULT_RANGE)
            .component(ModComponents.MAGNET_ACTIVE, true));

    /** Worn in the backpack slot above the trinket slot; 36 slots and module slots. */
    public static final BackpackItem BACKPACK = register("backpack", BackpackItem::new, new Item.Properties().stacksTo(1));

    /** Backpack modules, one per type: {@code nexium:<type>_module}. */
    public static final Map<BackpackModuleType, BackpackModuleItem> BACKPACK_MODULES = new EnumMap<>(BackpackModuleType.class);

    static {
        for (BackpackModuleType type : BackpackModuleType.values()) {
            BACKPACK_MODULES.put(type, register(type.id() + "_module", p -> new BackpackModuleItem(p, type), new Item.Properties().stacksTo(1)));
        }
    }

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

    /** A grindstone that pulls enchantments off an item onto books of their own. Made from a Grindstone, a Book and Lapis Lazuli. */
    public static final net.minecraft.world.item.BlockItem ENCHANTED_GRINDSTONE = register("enchanted_grindstone",
            p -> new net.minecraft.world.item.BlockItem(ModBlocks.ENCHANTED_GRINDSTONE, p), new Item.Properties().useBlockDescriptionPrefix());

    /** The 1-block iron farm. Only made at the Farm Assembler, but creative players find it in the Nexium tab. */
    public static final IronFarmItem IRON_FARM = register("iron_farm", p -> new IronFarmItem(ModBlocks.IRON_FARM, p), new Item.Properties()
            .useBlockDescriptionPrefix()
            .rarity(Rarity.UNCOMMON));

    /** The Experience Farm. Only made at the Farm Assembler, but creative players find it in the Farms and Power tab. */
    public static final net.nexium.essentials.xpfarm.XpFarmItem XP_FARM = register("xp_farm",
            p -> new net.nexium.essentials.xpfarm.XpFarmItem(ModBlocks.XP_FARM, p), new Item.Properties()
                    .useBlockDescriptionPrefix()
                    .rarity(Rarity.UNCOMMON));
    /** Switches hostile mob spawning off for the whole world. Only made at the Farm Assembler (from 140 hostile mobs), but creative players find it in the Farms and Power tab. */
    public static final net.minecraft.world.item.BlockItem HOSTILE_MOB_SWITCH = register("hostile_mob_switch",
            p -> new net.minecraft.world.item.BlockItem(ModBlocks.HOSTILE_MOB_SWITCH, p), new Item.Properties()
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
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, NexiumEssentials.id(name));
        T item = factory.apply(props.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    /**
     * What the Nexium creative tab shows after its own items. The Captured Mob is left out: an empty one is
     * meaningless, so it only comes from picking up a mob.
     */
    public static final List<Item> TAB_ORDER = java.util.stream.Stream.of(
            java.util.stream.Stream.of(MAGNET, BACKPACK), BACKPACK_MODULES.entrySet().stream().filter(e -> !e.getKey().retired()).map(java.util.Map.Entry::getValue),
            java.util.stream.Stream.of(BEDROCK_BREAKER_ELIXIR, ENCHANTED_GRINDSTONE, FARM_ASSEMBLER, IRON_FARM, XP_FARM, HOSTILE_MOB_SWITCH), FARMS.values().stream(),
            net.nexium.essentials.compressed.CompressedBlocks.items().stream(), net.nexium.essentials.sponge.BigSponges.items().stream())
            .flatMap(stream -> stream).map(i -> (Item) i).toList();

    public static void init() {}

    private ModItems() {}
}
