package net.nexium.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.nexium.drawer.DrawerBlock;
import net.nexium.drawer.DrawerWood;
import net.nexium.quest.QuestBook;
import net.nexium.quest.QuestBook.Chapter;
import net.nexium.quest.QuestBook.Quest;
import net.nexium.quest.QuestStatePayload;
import net.nexium.quest.Quests;
import net.nexium.quest.RecipeUnlock;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModItems;

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
    public void nothingIsLockedAndQuestsCountWhatIsAlreadyDone(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        List<Chapter> chapters = QuestBook.chapters();
        h.assertTrue(Quests.done(player).isEmpty(), "nothing is done at the start");
        // A quest of the fourth chapter counts at once, with nothing done in the first three
        Quest later = chapters.get(3).quests().getFirst();
        player.getInventory().add(new ItemStack(later.item(), later.count()));
        List<Quest> done = Quests.check(player);
        h.assertTrue(done.stream().anyMatch(q -> q.key().equals(later.key())), "a later chapter's quest counts with nothing done before it: " + done);
        h.assertTrue(Quests.done(player).contains(later.key()), "and is remembered");
        // Any other quest, in any order: one from the last chapter, and one from the second
        Quest last = chapters.getLast().quests().get(2);
        Quest second = chapters.get(1).quests().get(3);
        player.getInventory().add(new ItemStack(last.item(), last.count()));
        player.getInventory().add(new ItemStack(second.item(), second.count()));
        List<Quest> more = Quests.check(player);
        // (The first check paid random rewards, and one of them can be the very item of one of these two quests, which then finished
        // at once and is not in this result: what counts is that both are done.)
        h.assertTrue(Quests.done(player).contains(last.key()) && Quests.done(player).contains(second.key()),
                "quests of the last and the second chapter count too, in any order: " + more + ", done: " + Quests.done(player));
        // A whole chapter, then nothing is done twice (a random reward can finish a quest too, so the check goes round until nothing is left)
        give(player, chapters.get(0));
        Quests.check(player);
        for (Quest q : chapters.get(0).quests()) h.assertTrue(Quests.done(player).contains(q.key()), "the first chapter's quest " + q.key() + " is done");
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
        // A roll can be the very item the quest asks for (it then joins that stack), so count everything held beyond what the test put in
        int held = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() != q.item()) h.assertTrue(allowed.contains(stack.getItem()), "only items from the mod are given, found " + stack);
            held += stack.getCount();
        }
        int rewards = held - q.count();
        h.assertTrue(rewards >= c.tier(), "at least one item per roll was given: " + rewards);
        h.succeed();
    }

    @GameTest
    public void theRewardPoolIsTheModsItemsWithoutTheOddOnes(GameTestHelper h) {
        List<Item> pool = Quests.rewardPool();
        h.assertTrue(pool.size() > 100, "a big pool: " + pool.size());
        for (Item item : pool) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            h.assertTrue(id.getNamespace().equals("nexium"), "only the mod's own items: " + id);
            h.assertTrue(!java.util.Set.of("guide", "captured_mob", "spent_fuel_rod").contains(id.getPath()), "not " + id);
            h.assertTrue(!id.getPath().contains("compressed"), "no Compressed blocks: " + id);
            h.assertTrue(!id.getPath().contains("drawer"), "no drawers: " + id);
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

    @GameTest(maxTicks = 40)
    public void aDrawerOfAnyWoodFinishesTheDrawerQuest(GameTestHelper h) {
        Quest drawer = QuestBook.find("storage.drawer");
        h.assertTrue(drawer != null && drawer.tag() != null, "the drawer quest takes a whole tag of items");
        for (DrawerBlock d : ModBlocks.ALL_DRAWERS) {
            h.assertTrue(drawer.items().contains(d.asItem()), "every drawer counts, not " + d + " left out");
        }
        // Crafted: a poplar drawer, not the oak one the quest names
        ServerPlayer crafted = h.makeMockServerPlayerInLevel();
        crafted.awardStat(Stats.ITEM_CRAFTED.get(ModBlocks.DRAWERS.get(DrawerWood.POPLAR)[0].asItem()));
        h.assertTrue(Quests.check(crafted).stream().anyMatch(q -> q.key().equals("storage.drawer")), "crafting a poplar drawer finishes the quest");
        // Held: a spruce 2x2 drawer
        ServerPlayer held = h.makeMockServerPlayerInLevel();
        held.getInventory().add(new ItemStack(ModBlocks.DRAWERS.get(DrawerWood.SPRUCE)[2]));
        h.assertTrue(Quests.check(held).stream().anyMatch(q -> q.key().equals("storage.drawer")), "so does holding a spruce 2x2 drawer");
        // Anything else does not
        ServerPlayer other = h.makeMockServerPlayerInLevel();
        other.awardStat(Stats.ITEM_CRAFTED.get(Items.STICK));
        other.getInventory().add(new ItemStack(Items.OAK_PLANKS, 8));
        h.assertTrue(Quests.check(other).isEmpty(), "a stick and planks are not a drawer");
        // A quest with no tag still wants exactly its item
        Quest connector = QuestBook.find("storage.connector");
        h.assertTrue(connector.tag() == null && connector.items().size() == 1, "a quest without a tag names one item");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void everyNexiumRecipeIsInTheRecipeBookFromTheStart(GameTestHelper h) {
        // The mock player joins the server like a real one, which unlocks them
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        List<RecipeHolder<?>> mine = h.getLevel().getServer().getRecipeManager().getLearnableRecipes().stream()
                .filter(r -> r.id().identifier().getNamespace().equals("nexium")).toList();
        h.assertTrue(mine.size() > 300, "the mod has plenty of recipes: " + mine.size());
        for (RecipeHolder<?> recipe : mine) {
            h.assertTrue(player.getRecipeBook().contains(recipe.id()), "unlocked when the player joined: " + recipe.id().identifier());
        }
        h.assertTrue(!player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("crafting_table"))),
                "the game's own recipes unlock the way they always do");
        // Unlocking again (after a reload, say) gives back what is missing and nothing else
        List<RecipeHolder<?>> some = mine.subList(0, 3);
        player.resetRecipes(some);
        h.assertTrue(!player.getRecipeBook().contains(some.getFirst().id()), "a recipe taken away is gone");
        h.assertTrue(RecipeUnlock.unlock(player) > 0, "unlocking gives it back");
        for (RecipeHolder<?> recipe : some) h.assertTrue(player.getRecipeBook().contains(recipe.id()), "back: " + recipe.id().identifier());
        h.assertTrue(RecipeUnlock.unlock(player) == 0, "nothing is unlocked twice");
        h.succeed();
    }
}
