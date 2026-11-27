package net.nexium.autocraft;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.nexium.Nexium;
import net.nexium.network.NetworkBlock;
import net.nexium.network.StorageNetwork;
import net.nexium.util.StorageEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The messages of the craft request screen and the Autocrafting Monitor. Both work on a network block (a Crafting Terminal or a
 * Monitor, the {@code anchor}) the player is near, so neither needs a menu open on the server.
 */
public final class AutocraftPayloads {
    /** How far from its network block a player may still make requests (blocks). */
    private static final double REACH = 24;

    /** An item with an amount that may be over a stack. */
    public record Entry(ItemStack stack, long count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::stack, ByteBufCodecs.VAR_LONG, Entry::count, Entry::new);
    }

    /** What the monitor lists of a job. */
    public record JobEntry(UUID id, ItemStack target, long done, long total, String status, boolean finished) {
        public static final StreamCodec<RegistryFriendlyByteBuf, JobEntry> STREAM_CODEC = StreamCodec.composite(
                net.minecraft.core.UUIDUtil.STREAM_CODEC, JobEntry::id, ItemStack.STREAM_CODEC, JobEntry::target,
                ByteBufCodecs.VAR_LONG, JobEntry::done, ByteBufCodecs.VAR_LONG, JobEntry::total,
                ByteBufCodecs.STRING_UTF8, JobEntry::status, ByteBufCodecs.BOOL, JobEntry::finished, JobEntry::new);
    }

    // ---- server to client ----

    public record OpenMonitor(BlockPos anchor) implements CustomPacketPayload {
        public static final Type<OpenMonitor> TYPE = new Type<>(Nexium.id("autocraft_open_monitor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenMonitor> STREAM_CODEC = BlockPos.STREAM_CODEC.map(OpenMonitor::new, OpenMonitor::anchor).cast();
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OpenRequest(BlockPos anchor, List<ItemStack> craftables) implements CustomPacketPayload {
        public static final Type<OpenRequest> TYPE = new Type<>(Nexium.id("autocraft_open_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenRequest> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenRequest::anchor, ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenRequest::craftables, OpenRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The answer to a plan or start request: what would be crafted, taken from storage, and what is missing. */
    public record PlanResult(boolean ok, boolean started, boolean tooBig, List<Entry> craft, List<Entry> use, List<Entry> missing) implements CustomPacketPayload {
        public static final Type<PlanResult> TYPE = new Type<>(Nexium.id("autocraft_plan_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlanResult> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, PlanResult::ok, ByteBufCodecs.BOOL, PlanResult::started, ByteBufCodecs.BOOL, PlanResult::tooBig,
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), PlanResult::craft,
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), PlanResult::use,
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), PlanResult::missing, PlanResult::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Jobs(List<JobEntry> jobs) implements CustomPacketPayload {
        public static final Type<Jobs> TYPE = new Type<>(Nexium.id("autocraft_jobs"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Jobs> STREAM_CODEC = JobEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Jobs::new, Jobs::jobs).cast();
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---- client to server ----

    public record RequestCraftables(BlockPos anchor) implements CustomPacketPayload {
        public static final Type<RequestCraftables> TYPE = new Type<>(Nexium.id("autocraft_request_craftables"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestCraftables> STREAM_CODEC = BlockPos.STREAM_CODEC.map(RequestCraftables::new, RequestCraftables::anchor).cast();
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Plan(BlockPos anchor, ItemStack item, long count, boolean start) implements CustomPacketPayload {
        public static final Type<Plan> TYPE = new Type<>(Nexium.id("autocraft_plan"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Plan> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Plan::anchor, ItemStack.STREAM_CODEC, Plan::item, ByteBufCodecs.VAR_LONG, Plan::count,
                ByteBufCodecs.BOOL, Plan::start, Plan::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestJobs(BlockPos anchor) implements CustomPacketPayload {
        public static final Type<RequestJobs> TYPE = new Type<>(Nexium.id("autocraft_request_jobs"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestJobs> STREAM_CODEC = BlockPos.STREAM_CODEC.map(RequestJobs::new, RequestJobs::anchor).cast();
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Cancel(BlockPos anchor, UUID job) implements CustomPacketPayload {
        public static final Type<Cancel> TYPE = new Type<>(Nexium.id("autocraft_cancel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cancel> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Cancel::anchor, net.minecraft.core.UUIDUtil.STREAM_CODEC, Cancel::job, Cancel::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(OpenMonitor.TYPE, OpenMonitor.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenRequest.TYPE, OpenRequest.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlanResult.TYPE, PlanResult.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Jobs.TYPE, Jobs.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestCraftables.TYPE, RequestCraftables.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Plan.TYPE, Plan.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestJobs.TYPE, RequestJobs.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Cancel.TYPE, Cancel.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestCraftables.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            // A client with a terminal open does not know its block: it sends no position and the open menu supplies it.
            BlockPos anchor = player.containerMenu instanceof net.nexium.terminal.TerminalMenu tm && tm.terminalPos() != null ? tm.terminalPos() : p.anchor();
            StorageNetwork net = network(player, anchor);
            if (net != null) ServerPlayNetworking.send(player, new OpenRequest(anchor, AutocraftingService.craftables((ServerLevel) player.level(), net)));
        });
        ServerPlayNetworking.registerGlobalReceiver(Plan.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            StorageNetwork net = network(player, p.anchor());
            if (net != null && !p.item().isEmpty()) ServerPlayNetworking.send(player, plan((ServerLevel) player.level(), net, p, player));
        });
        ServerPlayNetworking.registerGlobalReceiver(RequestJobs.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            StorageNetwork net = network(player, p.anchor());
            if (net != null) ServerPlayNetworking.send(player, new Jobs(jobs((ServerLevel) player.level(), net)));
        });
        ServerPlayNetworking.registerGlobalReceiver(Cancel.TYPE, (p, ctx) -> {
            ServerPlayer player = ctx.player();
            StorageNetwork net = network(player, p.anchor());
            if (net != null) AutocraftingService.cancel((ServerLevel) player.level(), net, p.job());
        });
        AutocraftingService.init();
    }

    /** The network of the block at {@code anchor}, if the player is near it and it is a network block. */
    public static StorageNetwork network(ServerPlayer player, BlockPos anchor) {
        if (!(player.level() instanceof ServerLevel level) || !level.hasChunkAt(anchor)) return null;
        if (!(level.getBlockState(anchor).getBlock() instanceof NetworkBlock)) return null;
        if (player.distanceToSqr(Vec3.atCenterOf(anchor)) > REACH * REACH) return null;
        return StorageNetwork.find(level, anchor);
    }

    public static PlanResult plan(ServerLevel level, StorageNetwork net, Plan request, ServerPlayer player) {
        ItemVariant target = ItemVariant.of(request.item());
        long count = Math.max(1, Math.min(request.count(), 1_000_000));
        Planner.Plan plan = AutocraftingService.plan(level, net, target, count);
        boolean started = false;
        if (request.start() && plan.ok()) {
            AutocraftingService.start(level, request.anchor(), player, target, count, plan);
            started = true;
        }
        List<Entry> craft = new ArrayList<>();
        for (Planner.Step step : plan.steps()) {
            ItemStack primary = step.ref().pattern().primary();
            craft.add(new Entry(primary.copyWithCount(1), step.crafts() * primary.getCount()));
        }
        return new PlanResult(plan.ok(), started, plan.tooBig(), craft, entries(plan.use()), entries(plan.missing()));
    }

    private static List<Entry> entries(Map<ItemVariant, Long> map) {
        List<Entry> out = new ArrayList<>();
        for (var e : map.entrySet()) out.add(new Entry(e.getKey().toStack(1), e.getValue()));
        return out;
    }

    public static List<JobEntry> jobs(ServerLevel level, StorageNetwork net) {
        List<JobEntry> out = new ArrayList<>();
        for (AutocraftingService.Info info : AutocraftingService.infos(level, net)) {
            out.add(new JobEntry(info.id(), info.target(), info.done(), info.total(), info.status(), info.finished()));
        }
        return out;
    }

    private AutocraftPayloads() {}
}
