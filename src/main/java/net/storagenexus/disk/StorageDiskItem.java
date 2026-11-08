package net.storagenexus.disk;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.util.Prediction;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A storage disk: holds up to its size in items (any number of kinds), or a fluid disk up to its size in buckets, while
 * it is in a Disk Drive on a storage network. What it holds is saved with the world under the disk's id
 * ({@link DiskData}). Sneak + right-click an empty one to take it apart into its casing and storage part.
 */
public class StorageDiskItem extends Item {
    private final DiskTier tier;
    private final boolean fluid;

    public StorageDiskItem(Properties properties, DiskTier tier, boolean fluid) {
        super(properties);
        this.tier = tier;
        this.fluid = fluid;
    }

    public DiskTier tier() { return tier; }

    public boolean isFluid() { return fluid; }

    /** How much it holds: items, or droplets for a fluid disk. */
    public long capacity() {
        return fluid ? tier.buckets() * FluidConstants.BUCKET : tier.items();
    }

    /** The disk's id, given one the first time it is needed (server side). */
    public static UUID ensureId(ItemStack disk) {
        UUID id = disk.get(ModComponents.DISK_ID);
        if (id == null) {
            id = UUID.randomUUID();
            disk.set(ModComponents.DISK_ID, id);
        }
        return id;
    }

    /** The contents of an item disk, sized to it. */
    public static DiskContents<ItemVariant> items(MinecraftServer server, ItemStack disk) {
        StorageDiskItem item = (StorageDiskItem) disk.getItem();
        return DiskData.get(server).items(ensureId(disk)).withCapacity(item.capacity());
    }

    /** The contents of a fluid disk, sized to it. */
    public static DiskContents<FluidVariant> fluids(MinecraftServer server, ItemStack disk) {
        StorageDiskItem item = (StorageDiskItem) disk.getItem();
        return DiskData.get(server).fluids(ensureId(disk)).withCapacity(item.capacity());
    }

    /** How full the disk is, 0 to 1, from the usage the drive last wrote on it. */
    public static float fill(ItemStack disk) {
        if (!(disk.getItem() instanceof StorageDiskItem item)) return 0;
        DiskUsage usage = disk.getOrDefault(ModComponents.DISK_USAGE, DiskUsage.EMPTY);
        return (float) Math.min(1.0, (double) usage.stored() / item.capacity());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            ItemStack disk = player.getItemInHand(hand);
            UUID id = disk.get(ModComponents.DISK_ID);
            DiskData data = DiskData.get(sp.level().getServer());
            if (id != null && !data.isEmpty(id)) {
                sp.sendOverlayMessage(Component.translatable("message.storagenexus.disk.not_empty").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (id != null) data.remove(id);
            player.setItemInHand(hand, new ItemStack(ModItems.DISK_CASING));
            sp.getInventory().placeItemBackInInventory(new ItemStack(part(tier, fluid)), false, Prediction.SERVER_ONLY);
            sp.sendOverlayMessage(Component.translatable("message.storagenexus.disk.taken_apart"));
        }
        return InteractionResult.SUCCESS;
    }

    /** The storage part a disk of this size and kind is made with. */
    public static Item part(DiskTier tier, boolean fluid) {
        return (fluid ? ModItems.FLUID_PARTS : ModItems.STORAGE_PARTS).get(tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        DiskUsage usage = stack.getOrDefault(ModComponents.DISK_USAGE, DiskUsage.EMPTY);
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
        if (fluid) {
            tooltip.accept(Component.translatable("tooltip.storagenexus.disk.buckets",
                    f.format(usage.stored() / FluidConstants.BUCKET), f.format(tier.buckets()), usage.types()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.accept(Component.translatable("tooltip.storagenexus.disk.items",
                    f.format(usage.stored()), f.format(tier.items()), usage.types()).withStyle(ChatFormatting.AQUA));
        }
    }
}
