package net.nexium.quest;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.nexium.Nexium;

import java.util.ArrayList;
import java.util.List;

/** Server to client: how far the player is on every quest. {@code open} makes the client open the quest book. */
public record QuestStatePayload(boolean open, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<QuestStatePayload> TYPE = new Type<>(Nexium.id("quest_state"));

    public record Entry(String key, int current, boolean done) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.open);
                buf.writeVarInt(p.entries.size());
                for (Entry e : p.entries) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, e.key);
                    buf.writeVarInt(e.current);
                    buf.writeBoolean(e.done);
                }
            },
            buf -> {
                boolean open = buf.readBoolean();
                int n = Math.min(buf.readVarInt(), 4096);
                List<Entry> entries = new ArrayList<>(n);
                for (int i = 0; i < n; i++) entries.add(new Entry(ByteBufCodecs.STRING_UTF8.decode(buf), buf.readVarInt(), buf.readBoolean()));
                return new QuestStatePayload(open, entries);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
