package net.nexium.autocraft;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.nexium.autocraft.Planner.PatternRef;
import net.nexium.autocraft.Planner.Plan;
import net.nexium.autocraft.Planner.Step;
import net.nexium.network.StorageNetwork;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plans craft requests and runs the jobs they become. A job is a list of pattern runs, ingredients first; each tick it asks the
 * Autocrafter that holds the pattern to do the next piece. Jobs are kept in memory (a restart forgets them; whatever an
 * Autocrafter was in the middle of still finishes). Everything is looked up through the network a request was made on.
 */
public final class AutocraftingService {
    /** Ticks a job may make no progress before it gives up (five minutes). */
    public static final int TIMEOUT = 6000;
    /** How long a finished job stays listed in the monitor (ticks). */
    private static final int LINGER = 600;
    private static final Map<UUID, Job> JOBS = new LinkedHashMap<>();

    /** One pattern run being worked through. */
    private static final class StepState {
        final Step step;
        long started, done, baseline = -1;

        StepState(Step step) { this.step = step; }
    }

    /** What the monitor shows of a job. */
    public record Info(UUID id, ItemStack target, long done, long total, String status, boolean finished) {}

    public static final class Job {
        final UUID id = UUID.randomUUID();
        final ResourceKey<Level> dimension;
        final BlockPos anchor;
        final ItemStack target;
        final UUID owner;
        final List<StepState> steps = new ArrayList<>();
        int index, idle, finishedFor;
        String status = "crafting";
        boolean finished;

        Job(ResourceKey<Level> dimension, BlockPos anchor, ItemStack target, UUID owner) {
            this.dimension = dimension;
            this.anchor = anchor;
            this.target = target;
            this.owner = owner;
        }

        long total() {
            long n = 0;
            for (StepState s : steps) n += s.step.crafts();
            return n;
        }

        long done() {
            long n = 0;
            for (StepState s : steps) n += Math.min(s.done, s.step.crafts());
            return n;
        }

        public UUID id() { return id; }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(AutocraftingService::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> JOBS.clear());
    }

    // ---------------------------------------------------------------- patterns and plans

    /** Every valid pattern in every Autocrafter on the network, in a steady order. */
    public static List<PatternRef> patterns(ServerLevel level, StorageNetwork net) {
        List<BlockPos> nodes = new ArrayList<>(net.nodes());
        nodes.sort(Comparator.comparingLong(BlockPos::asLong));
        List<PatternRef> refs = new ArrayList<>();
        for (BlockPos pos : nodes) {
            if (!(level.getBlockEntity(pos) instanceof AutocrafterBlockEntity crafter)) continue;
            for (int slot = 0; slot < AutocrafterBlockEntity.SLOTS; slot++) {
                Pattern pattern = crafter.pattern(slot);
                if (pattern != null && pattern.valid()) refs.add(new PatternRef(pos, slot, pattern));
            }
        }
        return refs;
    }

    /** What the patterns on the network can make: one of each distinct output, for the request list. */
    public static List<ItemStack> craftables(ServerLevel level, StorageNetwork net) {
        Map<ItemVariant, ItemStack> out = new LinkedHashMap<>();
        for (PatternRef ref : patterns(level, net)) {
            for (ItemVariant variant : ref.pattern().yields().keySet()) out.putIfAbsent(variant, variant.toStack(1));
        }
        return new ArrayList<>(out.values());
    }

    public static Plan plan(ServerLevel level, StorageNetwork net, ItemVariant target, long count) {
        return Planner.plan(net.listItems(), patterns(level, net), target, count);
    }

    /** Starts a job for an OK plan. */
    public static Job start(ServerLevel level, BlockPos anchor, ServerPlayer player, ItemVariant target, long count, Plan plan) {
        ItemStack shown = target.toStack((int) Math.min(count, 99));
        Job job = new Job(level.dimension(), anchor.immutable(), shown, player == null ? new UUID(0, 0) : player.getUUID());
        for (Step step : plan.steps()) job.steps.add(new StepState(step));
        JOBS.put(job.id, job);
        return job;
    }

    // ---------------------------------------------------------------- jobs

    public static List<Info> infos(ServerLevel level, StorageNetwork net) {
        List<Info> out = new ArrayList<>();
        for (Job job : JOBS.values()) {
            if (job.dimension.equals(level.dimension()) && (net.nodes().contains(job.anchor))) {
                out.add(new Info(job.id, job.target, job.done(), job.total(), job.status, job.finished));
            }
        }
        return out;
    }

    public static boolean cancel(ServerLevel level, StorageNetwork net, UUID id) {
        Job job = JOBS.get(id);
        if (job == null || job.finished || !job.dimension.equals(level.dimension()) || !net.nodes().contains(job.anchor)) return false;
        finish(job, "cancelled");
        return true;
    }

    /** The number of jobs not yet finished (for tests). */
    public static int running() {
        int n = 0;
        for (Job job : JOBS.values()) if (!job.finished) n++;
        return n;
    }

    public static Job job(UUID id) { return JOBS.get(id); }

    private static void finish(Job job, String status) {
        job.finished = true;
        job.status = status;
        job.finishedFor = 0;
    }

    private static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        var it = JOBS.values().iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (job.finished) {
                if (++job.finishedFor > LINGER) it.remove();
                continue;
            }
            ServerLevel level = server.getLevel(job.dimension);
            if (level == null) continue;
            run(server, level, job);
        }
    }

    private static void run(MinecraftServer server, ServerLevel level, Job job) {
        StorageNetwork net = level.hasChunkAt(job.anchor) ? StorageNetwork.find(level, job.anchor) : null;
        if (net == null) {
            job.status = "no_network";
            if (++job.idle > TIMEOUT) finish(job, "failed_network");
            return;
        }
        if (job.index >= job.steps.size()) {
            finish(job, "done");
            ServerPlayer owner = server.getPlayerList().getPlayer(job.owner);
            if (owner != null) owner.sendSystemMessage(Component.translatable("message.nexium.autocraft.done", job.target.getHoverName()));
            return;
        }
        StepState state = job.steps.get(job.index);
        PatternRef ref = state.step.ref();
        if (!(level.getBlockEntity(ref.crafter()) instanceof AutocrafterBlockEntity crafter) || crafter.pattern(ref.slot()) == null
                || !crafter.pattern(ref.slot()).equals(ref.pattern()) && !samePattern(crafter.pattern(ref.slot()), ref.pattern())) {
            finish(job, "failed_pattern");
            return;
        }
        Pattern pattern = ref.pattern();
        long before = state.done;
        long remaining = state.step.crafts() - state.started;
        String waiting = null;
        if (pattern.processing()) {
            if (remaining > 0) {
                if (state.baseline < 0) state.baseline = net.countItem(ItemVariant.of(pattern.primary()));
                var result = crafter.push(level, net, pattern, remaining);
                if (result == AutocrafterBlockEntity.Start.STARTED) state.started += crafter.started();
                else waiting = waitingFor(result);
            }
            if (state.started > 0) {
                long per = Math.max(1, pattern.primary().getCount());
                long arrived = (net.countItem(ItemVariant.of(pattern.primary())) - state.baseline) / per;
                state.done = Math.max(state.done, Math.min(arrived, state.started));
            }
            if (waiting == null && state.done < state.started) waiting = "waiting_output";
        } else {
            state.done += crafter.takeCompleted(job.id);
            if (crafter.job() != null && !job.id.equals(crafter.job()) && crafter.busy()) {
                waiting = "waiting_crafter";
            } else if (remaining > 0 && !crafter.busy()) {
                var result = crafter.startCrafting(level, net, pattern, remaining, job.id);
                if (result == AutocrafterBlockEntity.Start.STARTED) state.started += crafter.started();
                else waiting = waitingFor(result);
            }
            if (state.done > before || crafter.busy()) waiting = null;
        }
        if (state.done > before || state.started > 0 && waiting == null) job.idle = 0;
        else if (waiting != null) job.idle++;
        job.status = waiting != null ? waiting : "crafting";
        if ("failed_pattern".equals(waiting)) {
            finish(job, "failed_pattern");
            return;
        }
        if (job.idle > TIMEOUT) {
            finish(job, "failed_timeout");
            return;
        }
        if (state.done >= state.step.crafts()) job.index++;
    }

    private static boolean samePattern(Pattern a, Pattern b) {
        return a.processing() == b.processing() && a.needs().equals(b.needs()) && a.yields().equals(b.yields());
    }

    private static String waitingFor(AutocrafterBlockEntity.Start start) {
        return switch (start) {
            case NO_ENERGY -> "waiting_energy";
            case NO_ITEMS -> "waiting_items";
            case NO_MACHINE -> "waiting_machine";
            case BAD_PATTERN -> "failed_pattern";
            default -> "waiting_crafter";
        };
    }

    private AutocraftingService() {}
}
