package net.storagenexus.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.storagenexus.StorageNexus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * World-wide registry of Dimensional Input/Output links. Every link has a unique name;
 * one Input can feed several Outputs.
 */
public class LinkRegistry extends SavedData {
    public static final class Link {
        String name;
        @Nullable GlobalPos input;
        final List<GlobalPos> outputs;

        Link(String name, @Nullable GlobalPos input, List<GlobalPos> outputs) {
            this.name = name;
            this.input = input;
            this.outputs = new ArrayList<>(outputs);
        }

        public String name() { return name; }
        public @Nullable GlobalPos input() { return input; }
        public List<GlobalPos> outputs() { return outputs; }

        boolean isEmpty() { return input == null && outputs.isEmpty(); }
    }

    private record Entry(UUID id, String name, Optional<GlobalPos> input, List<GlobalPos> outputs) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                GlobalPos.CODEC.optionalFieldOf("input").forGetter(Entry::input),
                GlobalPos.CODEC.listOf().fieldOf("outputs").forGetter(Entry::outputs)
        ).apply(i, Entry::new));
    }

    private static final Codec<LinkRegistry> CODEC = Entry.CODEC.listOf().xmap(LinkRegistry::new, LinkRegistry::toEntries);

    public static final SavedDataType<LinkRegistry> TYPE =
            new SavedDataType<>(StorageNexus.id("links"), LinkRegistry::new, CODEC, null);

    private final Map<UUID, Link> links = new HashMap<>();

    public LinkRegistry() {}

    private LinkRegistry(List<Entry> entries) {
        for (Entry e : entries) links.put(e.id(), new Link(e.name(), e.input().orElse(null), e.outputs()));
        links.values().removeIf(Link::isEmpty);
    }

    private List<Entry> toEntries() {
        List<Entry> out = new ArrayList<>();
        links.forEach((id, l) -> out.add(new Entry(id, l.name, Optional.ofNullable(l.input), List.copyOf(l.outputs))));
        return out;
    }

    public static LinkRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public @Nullable Link get(@Nullable UUID id) {
        return id == null ? null : links.get(id);
    }

    public boolean isNameTaken(String name) {
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (Link l : links.values()) if (l.name.toLowerCase(Locale.ROOT).equals(n)) return true;
        return false;
    }

    /** Creates a link. Returns null if the requested name is already used. */
    public @Nullable UUID create(@Nullable String requestedName, GlobalPos input, net.minecraft.util.RandomSource random) {
        String name;
        if (requestedName != null && !requestedName.isBlank()) {
            if (isNameTaken(requestedName)) return null;
            name = requestedName.trim();
        } else {
            name = NameGenerator.generate(random, this::isNameTaken);
        }
        UUID id = UUID.randomUUID();
        links.put(id, new Link(name, input, List.of()));
        setDirty();
        return id;
    }

    public void setInput(UUID id, @Nullable GlobalPos input) {
        Link l = links.get(id);
        if (l == null) return;
        l.input = input;
        setDirty();
    }

    /** Adds an output (removing it from any other link). Returns its 1-based number. */
    public int addOutput(UUID id, GlobalPos output) {
        Link l = links.get(id);
        if (l == null) return 0;
        for (Link other : links.values()) other.outputs.remove(output);
        l.outputs.add(output);
        pruneEmpty();
        setDirty();
        return l.outputs.size();
    }

    public void removeOutput(GlobalPos output) {
        for (Link l : links.values()) {
            if (l.outputs.remove(output)) setDirty();
        }
        pruneEmpty();
    }

    public void removeInput(GlobalPos input) {
        for (Link l : links.values()) {
            if (input.equals(l.input)) {
                l.input = null;
                setDirty();
            }
        }
        pruneEmpty();
    }

    /** Forgets links with neither an Input nor any Outputs left, so the save does not grow forever. */
    private void pruneEmpty() {
        if (links.values().removeIf(Link::isEmpty)) setDirty();
    }

    /** Renames a link; false if the name is blank or used by another link. */
    public boolean rename(UUID id, String newName) {
        Link l = links.get(id);
        if (l == null || newName == null || newName.isBlank()) return false;
        String trimmed = newName.trim();
        if (!l.name.equalsIgnoreCase(trimmed) && isNameTaken(trimmed)) return false;
        l.name = trimmed;
        setDirty();
        return true;
    }

    /** "Crimson Falcon", or "Crimson Falcon 2" for the second of several outputs. */
    public String displayName(@Nullable UUID id, @Nullable GlobalPos output) {
        Link l = get(id);
        if (l == null) return "";
        if (output == null || l.outputs.size() <= 1) return l.name;
        int idx = l.outputs.indexOf(output);
        return idx < 0 ? l.name : l.name + " " + (idx + 1);
    }
}
