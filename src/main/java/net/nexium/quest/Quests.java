package net.nexium.quest;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.nexium.Nexium;
import net.nexium.quest.QuestBook.Chapter;
import net.nexium.quest.QuestBook.Quest;
import net.nexium.registry.ModItems;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The quest system on the server: which quests a player has finished (saved on the player, kept through death), checking
 * the goals once a second, and the rewards: a few random items from the mod for each quest. Nothing is locked: every chapter and
 * every quest is open from the start, in any order. Quests count what the player has already done (the game's own statistics, or
 * what they hold), so nothing has to be repeated. Recipes are never locked by any of this (every Nexium recipe is unlocked on joining, see {@link RecipeUnlock}).
 */
public final class Quests {
    private static final AttachmentType<List<String>> DONE = AttachmentRegistry.create(Nexium.id("quests_done"),
            b -> b.persistent(Codec.STRING.listOf()).copyOnDeath());

    /** Items that make no sense as a gift. */
    private static final java.util.Set<String> NOT_REWARDS = java.util.Set.of("guide", "captured_mob", "spent_fuel_rod");

    private static List<Item> pool;

    /** Every item the mod shows in its creative tabs (so not the retired ones), less the few that are not gifts. */
    public static synchronized List<Item> rewardPool() {
        if (pool == null) {
            List<Item> out = new ArrayList<>();
            for (Item item : ModItems.TAB_ORDER) add(out, item);
            for (Item item : net.nexium.essentials.registry.ModItems.TAB_ORDER) add(out, item);
            pool = List.copyOf(out);
        }
        return pool;
    }

    private static void add(List<Item> out, Item item) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (!NOT_REWARDS.contains(id.getPath()) && !out.contains(item)) out.add(item);
    }

    public static void init() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(QuestStatePayload.TYPE, QuestStatePayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(QuestRequestPayload.TYPE, QuestRequestPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(QuestRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            check(player);
            send(player, payload.open());
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!check(player).isEmpty()) send(player, false);
            }
        });
    }

    /** The keys of the quests this player has finished. */
    public static Set<String> done(ServerPlayer player) {
        return new LinkedHashSet<>(player.getAttachedOrElse(DONE, List.of()));
    }

    /** How far a player is on a quest, from 0 to its count. */
    public static int progress(ServerPlayer player, Quest quest) {
        // A quest that takes a whole tag (any Drawer) counts every item in it, added together
        int fromStats = 0, held = 0;
        for (Item item : quest.items()) {
            fromStats += player.getStats().getValue(quest.goal() == QuestBook.Goal.GET ? Stats.ITEM_CRAFTED.get(item) : Stats.ITEM_USED.get(item));
            if (quest.goal() == QuestBook.Goal.GET) held += player.getInventory().countItem(item);
        }
        return Math.min(quest.count(), Math.max(fromStats, held));
    }

    /**
     * Finishes every quest whose goal is met, in any chapter. Returns the quests finished. It goes round again while a reward finishes
     * another quest (a random reward can be the very item a quest asks for), so what it returns leaves nothing more to finish.
     */
    public static List<Quest> check(ServerPlayer player) {
        List<Quest> finished = new ArrayList<>();
        Set<String> done = done(player);
        boolean again = true;
        while (again) {
            again = false;
            for (Chapter chapter : QuestBook.chapters()) {
                boolean wasComplete = chapter.quests().stream().allMatch(q -> done.contains(q.key()));
                for (Quest q : chapter.quests()) {
                    if (done.contains(q.key()) || progress(player, q) < q.count()) continue;
                    done.add(q.key());
                    finished.add(q);
                    reward(player, chapter, q);
                    again = true;
                }
                boolean complete = chapter.quests().stream().allMatch(q -> done.contains(q.key()));
                if (complete && !wasComplete) {
                    player.sendSystemMessage(Component.translatable("quest.nexium.chapter_completed", Component.translatable(chapter.nameKey())));
                }
            }
        }
        if (!finished.isEmpty()) player.setAttached(DONE, List.copyOf(done));
        return finished;
    }

    /** Gives the random items for a finished quest and says so in chat. */
    private static void reward(ServerPlayer player, Chapter chapter, Quest quest) {
        RandomSource random = player.getRandom();
        List<ItemStack> given = new ArrayList<>();
        for (int i = 0; i < chapter.tier(); i++) {
            ItemStack stack = roll(random);
            for (ItemStack have : given) {
                if (ItemStack.isSameItemSameComponents(have, stack)) {
                    have.grow(stack.getCount());
                    stack = ItemStack.EMPTY;
                    break;
                }
            }
            if (!stack.isEmpty()) given.add(stack);
        }
        Component list = Component.empty();
        for (int i = 0; i < given.size(); i++) {
            ItemStack stack = given.get(i);
            if (i > 0) list = list.copy().append(", ");
            list = list.copy().append(stack.getCount() + " ").append(stack.getHoverName());
            ItemStack toGive = stack.copy();
            if (!player.getInventory().add(toGive)) player.spawnAtLocation(player.level(), toGive);
        }
        player.sendSystemMessage(Component.translatable("quest.nexium.completed", Component.translatable(quest.nameKey())));
        player.sendSystemMessage(Component.translatable("quest.nexium.reward", list));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    private static ItemStack roll(RandomSource random) {
        List<Item> items = rewardPool();
        return new ItemStack(items.get(random.nextInt(items.size())));
    }

    /** Sends the player the state of the whole book. {@code open} makes the client open the screen. */
    public static void send(ServerPlayer player, boolean open) {
        Set<String> done = done(player);
        List<QuestStatePayload.Entry> entries = new ArrayList<>();
        for (Chapter chapter : QuestBook.chapters()) {
            for (Quest q : chapter.quests()) {
                boolean isDone = done.contains(q.key());
                entries.add(new QuestStatePayload.Entry(q.key(), isDone ? q.count() : progress(player, q), isDone));
            }
        }
        ServerPlayNetworking.send(player, new QuestStatePayload(open, entries));
    }

    private Quests() {}
}
