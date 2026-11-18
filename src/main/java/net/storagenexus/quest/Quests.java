package net.storagenexus.quest;

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
import net.storagenexus.StorageNexus;
import net.storagenexus.quest.QuestBook.Chapter;
import net.storagenexus.quest.QuestBook.Quest;
import net.storagenexus.registry.ModItems;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The quest system on the server: which quests a player has finished (saved on the player, kept through death), checking
 * the goals once a second, and the rewards: a few random Nexium items for each quest. A chapter is open once every quest of
 * the one before it is done. Quests count what the player has already done (the game's own statistics, or what they hold), so
 * nothing has to be repeated. Recipes are never locked by any of this.
 */
public final class Quests {
    private static final AttachmentType<List<String>> DONE = AttachmentRegistry.create(StorageNexus.id("quests_done"),
            b -> b.persistent(Codec.STRING.listOf()).copyOnDeath());

    /** Random Nexium rewards and how likely each is. */
    private record Prize(Item item, int weight, int min, int max) {}

    private static List<Prize> prizes() {
        return List.of(new Prize(ModItems.NEXIUM_INGOT, 8, 1, 3), new Prize(ModItems.STORAGE_PARTS.get(net.storagenexus.disk.DiskTier.NEXIUM), 2, 1, 1),
                new Prize(ModItems.FLUID_PARTS.get(net.storagenexus.disk.DiskTier.NEXIUM), 2, 1, 1), new Prize(ModItems.NEXIUM_BLOCK, 1, 1, 1));
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

    /** Whether chapter number {@code index} is open: the first always, the rest once the one before is complete. */
    public static boolean open(Set<String> done, int index) {
        if (index <= 0) return true;
        Chapter before = QuestBook.chapters().get(index - 1);
        return before.quests().stream().allMatch(q -> done.contains(q.key()));
    }

    /** How far a player is on a quest, from 0 to its count. */
    public static int progress(ServerPlayer player, Quest quest) {
        int fromStats = player.getStats().getValue(quest.goal() == QuestBook.Goal.GET ? Stats.ITEM_CRAFTED.get(quest.item()) : Stats.ITEM_USED.get(quest.item()));
        int held = quest.goal() == QuestBook.Goal.GET ? player.getInventory().countItem(quest.item()) : 0;
        return Math.min(quest.count(), Math.max(fromStats, held));
    }

    /** Finishes every quest whose goal is met in an open chapter (and any chapter that opens as a result). Returns the quests finished. */
    public static List<Quest> check(ServerPlayer player) {
        List<Quest> finished = new ArrayList<>();
        Set<String> done = done(player);
        boolean again = true;
        while (again) {
            again = false;
            List<Chapter> chapters = QuestBook.chapters();
            for (int i = 0; i < chapters.size(); i++) {
                if (!open(done, i)) continue;
                Chapter chapter = chapters.get(i);
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
                    player.sendSystemMessage(Component.translatable("quest.storagenexus.chapter_completed", Component.translatable(chapter.nameKey())));
                }
            }
        }
        if (!finished.isEmpty()) player.setAttached(DONE, List.copyOf(done));
        return finished;
    }

    /** Gives the random Nexium items for a finished quest and says so in chat. */
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
        player.sendSystemMessage(Component.translatable("quest.storagenexus.completed", Component.translatable(quest.nameKey())));
        player.sendSystemMessage(Component.translatable("quest.storagenexus.reward", list));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    private static ItemStack roll(RandomSource random) {
        List<Prize> prizes = prizes();
        int total = prizes.stream().mapToInt(Prize::weight).sum();
        int pick = random.nextInt(total);
        for (Prize p : prizes) {
            pick -= p.weight();
            if (pick < 0) return new ItemStack(p.item(), p.min() + random.nextInt(p.max() - p.min() + 1));
        }
        return new ItemStack(ModItems.NEXIUM_INGOT);
    }

    /** Sends the player the state of the whole book. {@code open} makes the client open the screen. */
    public static void send(ServerPlayer player, boolean open) {
        Set<String> done = done(player);
        List<QuestStatePayload.Entry> entries = new ArrayList<>();
        List<Chapter> chapters = QuestBook.chapters();
        for (int i = 0; i < chapters.size(); i++) {
            boolean chapterOpen = open(done, i);
            for (Quest q : chapters.get(i).quests()) {
                boolean isDone = done.contains(q.key());
                entries.add(new QuestStatePayload.Entry(q.key(), isDone ? q.count() : chapterOpen ? progress(player, q) : 0, isDone));
            }
        }
        ServerPlayNetworking.send(player, new QuestStatePayload(open, entries));
    }

    private Quests() {}
}
