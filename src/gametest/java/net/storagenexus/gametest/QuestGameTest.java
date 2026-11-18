package net.storagenexus.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.quest.QuestBook;
import net.storagenexus.quest.QuestBook.Chapter;
import net.storagenexus.quest.QuestBook.Quest;
import net.storagenexus.quest.QuestStatePayload;
import net.storagenexus.quest.Quests;
import net.storagenexus.registry.ModItems;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The quest system: its data is complete, chapters open one after another, progress counts what the player already has, and rewards are Nexium items. */
public class QuestGameTest {
    private static JsonObject lang() {
        return new Gson().fromJson(new InputStreamReader(QuestGameTest.class.getResourceAsStream("/assets/nexium/lang/en_us.json"), StandardCharsets.UTF_8), JsonObject.class);
    }

    @GameTest
    public void theQuestBookIsCompleteAndHasText(GameTestHelper h) {
        List<Chapter> chapters = QuestBook.chapters();
        h.assertTrue(chapters.size() == 6, "six chapters, found " + chapters.size());
        JsonObject lang = lang();
        Set<String> keys = new HashSet<>();
        for (Chapter c : chapters) {
            h.assertTrue(c.icon() != Items.AIR, "chapter " + c.id() + " has a real icon");
            h.assertTrue(c.tier() >= 1 && c.tier() <= 3, "chapter " + c.id() + " has a reward tier of 1 to 3");
            h.assertTrue(lang.has(c.nameKey()) && lang.has(c.descKey()), "chapter " + c.id() + " has its text");
            h.assertTrue(!c.quests().isEmpty(), "chapter " + c.id() + " has quests");
            for (Quest q : c.quests()) {
                h.assertTrue(q.item() != Items.AIR, "quest " + q.key() + " names a real item");
                h.assertTrue(q.count() >= 1 && q.count() <= 64, "quest " + q.key() + " has a sensible count");
                h.assertTrue(keys.add(q.key()), "quest " + q.key() + " is listed once");
                h.assertTrue(lang.has(q.nameKey()) && lang.has(q.descKey()), "quest " + q.key() + " has its text");
            }
        }
        h.assertTrue(QuestBook.totalQuests() >= 30, "there are plenty of quests: " + QuestBook.totalQuests());
        h.succeed();
    }

    private static void give(ServerPlayer p, Chapter c) {
        for (Quest q : c.quests()) p.getInventory().add(new ItemStack(q.item(), q.count()));
    }

    @GameTest(maxTicks = 40)
    public void chaptersOpenOneAfterAnotherAndCountWhatIsAlreadyDone(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        List<Chapter> chapters = QuestBook.chapters();
        h.assertTrue(Quests.done(player).isEmpty(), "nothing is done at the start");
        // A quest of the second chapter, with nothing done in the first: it waits
        Quest later = chapters.get(1).quests().getFirst();
        player.getInventory().add(new ItemStack(later.item(), later.count()));
        h.assertTrue(Quests.check(player).isEmpty(), "a later chapter's quest does not count while it is closed");
        h.assertTrue(!Quests.open(Quests.done(player), 1), "the second chapter is closed");
        // One quest of the first chapter
        Quest first = chapters.get(0).quests().getFirst();
        player.getInventory().add(new ItemStack(first.item(), first.count()));
        List<Quest> done = Quests.check(player);
        h.assertTrue(done.size() == 1 && done.getFirst().key().equals(first.key()), "the first quest is done: " + done);
        h.assertTrue(Quests.done(player).contains(first.key()), "and remembered");
        // Everything in the first chapter: it completes, the second opens, and what was already held counts at once
        give(player, chapters.get(0));
        List<Quest> more = Quests.check(player);
        h.assertTrue(more.stream().anyMatch(q -> q.key().equals(later.key())), "the second chapter opens and its held item counts at once: " + more);
        h.assertTrue(Quests.open(Quests.done(player), 1), "the second chapter is open");
        h.assertTrue(!Quests.open(Quests.done(player), 3), "the fourth is still closed");
        h.assertTrue(Quests.check(player).isEmpty(), "nothing is done twice");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void rewardsAreRandomItemsFromTheMod(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Chapter c = QuestBook.chapters().getFirst();
        Quest q = c.quests().getFirst();
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(q.item(), q.count()));
        Quests.check(player);
        Set<Item> allowed = new java.util.HashSet<>(Quests.rewardPool());
        int rewards = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || stack.getItem() == q.item()) continue;
            h.assertTrue(allowed.contains(stack.getItem()), "only items from the mod are given, found " + stack);
            rewards += stack.getCount();
        }
        h.assertTrue(rewards >= c.tier(), "at least one item per roll was given: " + rewards);
        h.succeed();
    }

    @GameTest
    public void theRewardPoolIsTheModsItemsWithoutTheOddOnes(GameTestHelper h) {
        List<Item> pool = Quests.rewardPool();
        h.assertTrue(pool.size() > 150, "a big pool: " + pool.size());
        for (Item item : pool) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            h.assertTrue(id.getNamespace().equals("nexium"), "only the mod's own items: " + id);
            h.assertTrue(!java.util.Set.of("guide", "captured_mob", "spent_fuel_rod").contains(id.getPath()), "not " + id);
        }
        h.assertTrue(pool.contains(ModItems.NEXIUM_INGOT) && pool.contains(ModItems.NEXIUM_BLOCK), "Nexium items are among them");
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void theStateTravelsToTheScreen(GameTestHelper h) {
        QuestStatePayload payload = new QuestStatePayload(true, List.of(new QuestStatePayload.Entry("storage.connector", 1, true),
                new QuestStatePayload.Entry("power.conduit", 3, false)));
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        QuestStatePayload.STREAM_CODEC.encode(buf, payload);
        QuestStatePayload back = QuestStatePayload.STREAM_CODEC.decode(buf);
        h.assertTrue(back.open(), "the open flag");
        h.assertValueEqual(back.entries(), payload.entries(), "the entries");
        h.succeed();
    }
}
