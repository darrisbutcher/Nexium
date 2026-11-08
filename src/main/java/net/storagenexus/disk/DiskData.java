package net.storagenexus.disk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.storagenexus.StorageNexus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The contents of every storage disk in the world, by disk id. Saved with the overworld, like Refined Storage's disk
 * data, so the disk items themselves stay small; moving, dropping or re-inserting a disk keeps what it holds.
 */
public final class DiskData extends SavedData {
    private record Amount<T>(T variant, long amount) {}

    private record Disk<T>(UUID id, List<Amount<T>> contents) {}

    private static <T> Codec<Disk<T>> diskCodec(Codec<T> variant) {
        Codec<Amount<T>> amount = RecordCodecBuilder.create(i -> i.group(
                variant.fieldOf("variant").forGetter(Amount::variant),
                Codec.LONG.fieldOf("amount").forGetter(Amount::amount)
        ).apply(i, Amount::new));
        return RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Disk::id),
                amount.listOf().fieldOf("contents").forGetter(Disk::contents)
        ).apply(i, Disk::new));
    }

    private static final Codec<DiskData> CODEC = RecordCodecBuilder.create(i -> i.group(
            diskCodec(ItemVariant.CODEC).listOf().optionalFieldOf("items", List.of()).forGetter(d -> d.save(d.items)),
            diskCodec(FluidVariant.CODEC).listOf().optionalFieldOf("fluids", List.of()).forGetter(d -> d.save(d.fluids))
    ).apply(i, DiskData::new));

    public static final SavedDataType<DiskData> TYPE = new SavedDataType<>(StorageNexus.id("storage_disks"), DiskData::new, CODEC, null);

    private final Map<UUID, DiskContents<ItemVariant>> items = new HashMap<>();
    private final Map<UUID, DiskContents<FluidVariant>> fluids = new HashMap<>();

    public DiskData() {}

    private DiskData(List<Disk<ItemVariant>> itemDisks, List<Disk<FluidVariant>> fluidDisks) {
        load(itemDisks, items);
        load(fluidDisks, fluids);
    }

    private <T extends net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant<?>> void load(List<Disk<T>> disks, Map<UUID, DiskContents<T>> into) {
        for (Disk<T> disk : disks) {
            DiskContents<T> contents = new DiskContents<>(this::setDirty);
            for (Amount<T> a : disk.contents()) contents.load(a.variant(), a.amount());
            into.put(disk.id(), contents);
        }
    }

    private <T extends net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant<?>> List<Disk<T>> save(Map<UUID, DiskContents<T>> from) {
        List<Disk<T>> out = new ArrayList<>();
        for (Map.Entry<UUID, DiskContents<T>> e : from.entrySet()) {
            List<Amount<T>> contents = new ArrayList<>();
            e.getValue().snapshot().forEach((v, n) -> contents.add(new Amount<>(v, n)));
            out.add(new Disk<>(e.getKey(), contents));
        }
        return out;
    }

    public static DiskData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public DiskContents<ItemVariant> items(UUID id) {
        return items.computeIfAbsent(id, k -> {
            setDirty();
            return new DiskContents<>(this::setDirty);
        });
    }

    public DiskContents<FluidVariant> fluids(UUID id) {
        return fluids.computeIfAbsent(id, k -> {
            setDirty();
            return new DiskContents<>(this::setDirty);
        });
    }

    /** Whether a disk with this id holds anything (a disk never used holds nothing). */
    public boolean isEmpty(UUID id) {
        DiskContents<ItemVariant> i = items.get(id);
        DiskContents<FluidVariant> f = fluids.get(id);
        return (i == null || i.isEmpty()) && (f == null || f.isEmpty());
    }

    /** Forgets a disk (it was taken apart). */
    public void remove(UUID id) {
        if (items.remove(id) != null | fluids.remove(id) != null) setDirty();
    }
}
