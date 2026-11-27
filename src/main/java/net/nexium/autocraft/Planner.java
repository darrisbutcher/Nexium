package net.nexium.autocraft;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out what a craft request needs: what to take from storage, which patterns to run (and how many times, ingredients first),
 * and what is missing. It is a plain function of the stock and the patterns, so it can be tested without a world.
 */
public final class Planner {
    /** Most levels of patterns inside patterns; deeper than this (or a pattern that needs itself) counts as missing. */
    private static final int MAX_DEPTH = 16;
    /** Most pattern runs one request may plan: a request for a huge amount is refused instead of freezing the server. */
    public static final long MAX_CRAFTS = 20_000;

    /** One pattern in one Autocrafter slot. */
    public record PatternRef(BlockPos crafter, int slot, Pattern pattern) {}

    /** Run {@code ref} {@code crafts} times. */
    public record Step(PatternRef ref, long crafts) {}

    /**
     * @param steps   the pattern runs, ingredients first
     * @param use     what is taken from storage
     * @param missing what neither storage nor a pattern can make
     * @param tooBig  the request would take more than {@link #MAX_CRAFTS} runs
     */
    public record Plan(List<Step> steps, Map<ItemVariant, Long> use, Map<ItemVariant, Long> missing, boolean tooBig) {
        public boolean ok() { return missing.isEmpty() && !tooBig && !steps.isEmpty(); }

        public long totalCrafts() {
            long n = 0;
            for (Step s : steps) n += s.crafts;
            return n;
        }
    }

    private static final class State {
        final Map<ItemVariant, Long> stock;
        final List<PatternRef> refs;
        final List<Step> steps = new ArrayList<>();
        final Map<ItemVariant, Long> use = new LinkedHashMap<>(), missing = new LinkedHashMap<>();
        long crafts;
        boolean tooBig;

        State(Map<ItemVariant, Long> stock, List<PatternRef> refs) {
            this.stock = new HashMap<>(stock);
            this.refs = refs;
        }
    }

    public static Plan plan(Map<ItemVariant, Long> stock, List<PatternRef> refs, ItemVariant target, long count) {
        State s = new State(stock, refs);
        if (count > 0) need(s, target, count, 0);
        return new Plan(s.steps, s.use, s.missing, s.tooBig);
    }

    private static void need(State s, ItemVariant variant, long amount, int depth) {
        long have = Math.max(0, s.stock.getOrDefault(variant, 0L));
        long take = Math.min(have, amount);
        if (take > 0) {
            s.stock.put(variant, have - take);
            s.use.merge(variant, take, Long::sum);
        }
        long left = amount - take;
        if (left <= 0) return;
        PatternRef ref = depth >= MAX_DEPTH ? null : find(s.refs, variant);
        if (ref == null) {
            s.missing.merge(variant, left, Long::sum);
            return;
        }
        long perCraft = ref.pattern().yieldOf(variant);
        long crafts = (left + perCraft - 1) / perCraft;
        s.crafts += crafts;
        if (s.crafts > MAX_CRAFTS) {
            s.tooBig = true;
            return;
        }
        // The ingredients first, so the steps come out in the order they must run
        for (var input : ref.pattern().needs().entrySet()) {
            need(s, input.getKey(), input.getValue() * crafts, depth + 1);
            if (s.tooBig) return;
        }
        s.steps.add(new Step(ref, crafts));
        // What the runs make beyond what was asked for is there for the steps after
        for (var out : ref.pattern().yields().entrySet()) {
            long made = out.getValue() * crafts - (out.getKey().equals(variant) ? left : 0);
            if (made > 0) s.stock.merge(out.getKey(), made, Long::sum);
        }
    }

    private static PatternRef find(List<PatternRef> refs, ItemVariant variant) {
        for (PatternRef ref : refs) if (ref.pattern().yieldOf(variant) > 0) return ref;
        return null;
    }
}
