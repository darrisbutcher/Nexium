package net.nexium.dog;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.Item;
import net.nexium.Nexium;

/** The dog's entity type. */
public final class ModDogs {
    public static final ResourceKey<EntityType<?>> DOG_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Nexium.id("dog"));

    /** Same size as a wolf. */
    public static final EntityType<DogEntity> DOG = Registry.register(BuiltInRegistries.ENTITY_TYPE, DOG_KEY,
            EntityType.Builder.<DogEntity>of(DogEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).eyeHeight(0.68F).passengerAttachments(0.8375F).ridingOffset(-0.0625F).clientTrackingRange(10)
                    .build(DOG_KEY));

    /** Placeholder until the Nexium Dog Armor item exists (set by the item registry). */
    public static Item NEXIUM_DOG_ARMOR = net.minecraft.world.item.Items.AIR;

    public static void init() {
        FabricDefaultAttributeRegistry.register(DOG, Wolf.createAttributes());
    }

    private ModDogs() {}
}
