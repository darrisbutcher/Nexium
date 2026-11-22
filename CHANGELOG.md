# Changelog

All notable changes to Nexium (mod id `nexium`), newest first. The version is `mod_version` in `gradle.properties`.
Pull requests are squash merged, so each one is a single commit on `main`.

## 1.25.13

* **The Farm Assembler's From network button uses the network's water and lava.** The farms that ask for water or lava buckets (Sugar Cane,
  Bamboo, Nether Wart, and the others with such a line) only ever looked for bucket items, so water and lava stored in a network as fluid
  (in a Fluid Drawer, a Fluid Tank or a fluid disk) was ignored. Now a line of water or lava buckets takes real buckets from the network
  first and makes up the rest from the network's water or lava, a bucket's worth for each bucket and whole buckets only. The Assembler holds
  it as fluid, not as buckets (so no buckets are made from nothing), and the line counts it (4 / 4). Picking another target gives the fluid
  back to the network, and what the network has no room for, or all of it if no network touches the Assembler any more, comes back as filled
  buckets. Building uses it up, and breaking the Assembler drops it as filled buckets. It is saved with the block.

## 1.25.12

* **The Trash Can destroys items.** Craft it from a **Cauldron and a Hopper** (shapeless). A hopper, a dropper, an Item Conduit or an
  Import/Export Bus pointing at it, from any side, empties into it, and it never fills up. Right-click it for a screen with one slot:
  drop an item on the slot and the whole stack is destroyed (a right click destroys one). A shift-click moves nothing there, so an item is
  only destroyed by dropping it on the slot on purpose. It takes items only, no fluids or energy. It is on `#nexium:machines`, so a Storage
  Connector never adopts it as network storage and nothing on a storage network can reach it. It has a Codex topic and a quest in the
  Storage chapter.
* **The Farm Assembler links to a storage network.** Touch it with a Storage Connector, an Inventory Cable or any other network block
  (the connectors link it without taking it for storage), and its new **From network** button, under Add from inventory, takes what
  the chosen target still needs out of the network: spawn eggs or Captured Mobs, blocks, everything on its list. It pulls only when you
  press the button, only what is still needed, and the finished item stays in the output slot as before. The button is greyed out, with a
  hint, until a network touches the Assembler.

## 1.25.11

* **The inventory key (E) closes every Nexium window,** as Escape does and as it closes vanilla's own windows. The Codex, the quest book,
  the settings, the Conduit Scanner, the Power Meter, the Elevator, the Armor menu, the block list and the tag picker did not close on it
  before. While you are typing in a text box, E types and the window stays. It follows whatever key you have set for the inventory.
* **JEI and REI keep their item lists clear of the Backpack's crafting panel.** The panel sticks out to the right of the Backpack's screen,
  and their lists were drawn over it; they now move aside for it (and come back when the Crafting module is taken out).
* **Nexium tooltips wrap to one narrow width, 170 pixels.** Every Nexium item's tooltip and the hover tips of the machine and settings
  screens now wrap instead of running off the screen. The screens that did wrap used widths of their own (the Backpack's trash hint 160,
  the Enchanted Grindstone's 150, the Conduit Scanner's 230, the settings screen's up to 220); they all use the one width now. A few
  tooltips said things twice and no longer do: the Backpack modules' description, and the link instructions of the Crafting Storage
  Tablet and the Dimensional Link Card (their status line now just says "Not linked"). The Codex line on an item's tooltip no longer
  reads "Press Not Bound" when its key is unbound.
* **The Power Meter shows what a generator really makes.** A generator (and a Battery) hands energy straight to a Battery or machine
  touching it before a conduit can take any, and the meter did not count that: a Combustion Generator burning blaze rods said 20 NE/t on
  its screen and showed 0.0 in the meter when a Battery touched it. A generator's row now shows everything it makes, and a machine's row
  everything it takes in, however it arrives.

## 1.25.10

* **The Enchanted Grindstone: pull enchantments off an item onto books of their own.** Craft it from a Grindstone, a Book and Lapis
  Lazuli (anywhere in the crafting grid). It is a grindstone to look at, with a violet wheel and glowing runes, and places the same
  way, but right-clicking it opens its own menu: an item slot, a slot for plain Books and a list of the enchantments on the item.
  Click one and it comes off onto a new Enchanted Book that holds just that enchantment, using up one Book. It is free (no
  experience), the item keeps its wear, its name and every other enchantment, curses can be pulled off too, and it works on tools,
  armor and enchanted books alike (a book that already holds one enchantment has nothing to separate). An AIOT's waiting Silk Touch or
  Fortune is listed and taken off like the rest. A list of more than five enchantments scrolls with the mouse wheel; a full inventory
  gets the new book dropped at the player's feet; closing the screen gives back what is in the slots. The plain Grindstone is
  unchanged. The Codex has pages for it (in the Enchanting chapter) and a quest in the Tools chapter.

* **The Hostile Mob Switch: turn hostile mob spawning off for the whole world.** Made at the Farm Assembler (pick Hostile Switch) from
  **140 hostile mobs of any kind**, as spawn eggs or Captured Mobs. Right-click it and press Stop hostile spawning (or Allow hostile
  spawning to undo it). It is one setting for the whole world, saved with it, that every switch shows and changes, wherever it stands: while it
  is on, no hostile mob spawns near any player in any dimension. It does what Peaceful does to spawning without removing the mobs
  already there: the usual spawn cycle, phantoms and patrols stop, and so do the spawns the spawn_monsters game rule governs (nether
  portal zombified piglins, reinforcement zombies, endermites from pearls, creakings). Mob spawners and spawn eggs still work. A redstone
  signal on a switch turns spawning off, and losing the power turns it back on. Breaking a switch leaves the setting as it was, and a
  switch drops with any tool. The Codex has pages for it (in the Machines, Mobs & Villagers chapter) and there is a quest in the Farms chapter.

## 1.25.9

* **A Crafting module for the Backpack: a 3x3 crafting grid in its screen.** Make it with iron ingots in the corners, any planks on the four
  sides and a crafting table in the middle. With it installed and switched on, a panel at the right of the Backpack's screen has a 3x3
  grid and a result slot (the screen is centred with it). Put items in the grid by hand, or use the **recipe book** (its button is under
  the grid) or the **+ button of JEI or REI**: the grid fills from what the Backpack holds first and your inventory after it. After every
  craft the emptied grid slots refill from the Backpack, so a **shift-click on the result crafts all there is**. Take the module out or
  switch it off, or close the screen, and what is in the grid goes back into the Backpack. When the screen is too narrow for the recipe book
  beside it, the open book covers the screen, as in vanilla, and its button still closes it. It is the fifth module, so it uses the last
  module slot. The Codex has pages for it, there is a quest for it in the Backpack chapter, and its icon is a crafting table in the
  module frame.
* **A trash slot in the Backpack's screen.** It is under the module column and always there, with no module. Drop an item on it to get rid of it:
  it keeps the last item you put in until you put another in or close the screen, so you can take it back (a shift-click on it moves it
  into your inventory). Shift-clicking an item from your inventory never puts it in the trash or the grid.

## 1.25.8

* **You can set when the Feeding module eats.** With a Feeding module in the Backpack, its screen has a small **Feeding** panel under the
  module slots, with two settings kept on that module (so they stay with it, and each Backpack has its own). **Eats at N** is the hunger
  level (of 20) it eats at or below: it starts at 19, which is how it always worked (as soon as any hunger is missing); each click steps
  it lower (18, 16, 14, 12, 10, 8, 6, 4, 2, then back to 19) and a shift-click steps it higher, so it waits until you are hungrier.
  **No waste** (the default) keeps it eating only food that restores no more than is missing; **May waste** makes it eat once you are at the
  level whatever is missing, the best-fitting food or, if every food is bigger than what is missing, the smallest one. Foods with side
  effects are still never eaten. Hovering a button explains it, and a module that is not at the defaults says so in its tooltip.
  A Feeding module you already have is unchanged: it starts at 19 with No waste. The Codex has a second Feeding page for the settings.

## 1.25.7

* **Left clicks work in the tag picker and in the conduit's Out colour grid.** In this version of the game the left mouse button is
  number 1 (it was 0 in older ones), and both screens only accepted 0, so clicking a tag in the Advanced Filter's picker, or a colour
  in a conduit's Out grid (new in 1.25.5), did nothing. Both take a left click now. A check through the rest of the mod found one more
  case: **Enter in the vein-mining blacklist screen** never added the typed block (it compared against an old key number); it does now.
  The tests for these click and press through the game's real mouse and keyboard, and a new check fails the build if a mouse button or
  key is ever compared with a bare number again.
* **Nexium recipes no longer need the netherite upgrade template.** All 14 smithing recipes (the ten Nexium tools and armor pieces,
  the Nexium AIOT and builder, and the Netherite AIOT and builder) take just the piece and the addition: **leave the template slot
  empty**. A template in the slot gives no result. The Codex cards show the empty slot and the pages and README say so.

## 1.25.6

* **The Advanced Filter can match item tags you choose, anything smeltable, or furnace fuel.** Its editor has two new buttons. **Tags** opens a
  tag picker: every item tag in the game in a list you can search by name (`minecraft:logs`, `c:ores`), with a click to choose or remove one
  (up to 32 tags; hovering shows how many items a tag holds). **Match** cycles **Items & tags**, **Smeltable** (anything a furnace, blast
  furnace or smoker takes) and **Furnace fuel**. Whitelist/Blacklist applies to all of it, so an insert side can allow or refuse a tag, or all
  smeltables, or all fuel. The Basic Filter and the filters you already have are unchanged. One extractor can now fill a furnace without
  clogging it: a Smeltable filter on the conduit's top side and a Fuel filter on another side. The filter's tooltip lists its Match and tags,
  and the Codex Filters page explains them.
* **A conduit arm shows an arrow for what it does.** A flat arrow on the arm points out to the block when the side inserts, in to the conduit
  when it extracts, and is a double arrow when it does both, whichever conduit types the arm carries. The coloured rings stay as they were.
  The arrow goes in the stretch of arm the rings leave free (past them, or between them and the core), shorter if the stretch is short,
  and is left out only in the one case with no room: item, fluid and energy conduits all inserting and extracting on one arm.

## 1.25.5

* **A conduit side can extract on several colours at once.** In a conduit's settings the **Out** row is now a grid of the 16 colours:
  click a colour to switch it on or off (at least one always stays on). What the side takes out goes to the insert sides of **any** of its
  colours, so one extract side can feed receivers on different colours, and a side saved before this keeps the one colour it had. A side
  still has **one filter**, shared by all of its colours, and an insert side still has one colour. Because the grid needs room, the settings
  screen is 18 pixels taller. On the conduit itself the extract side shows one ring for each colour that is on (as many as fit on the arm).
  The Conduit Scanner shows every Out colour of a side (a link appears under each colour's heading), names them all in its tooltip, and
  warns of a dead end only when none of a side's colours has a receiver.

## 1.25.4

* **Nothing in the quest book is locked.** A chapter used to open only once every quest of the one before it was done, and quests in a closed
  chapter did not count, even if you already held the item. Now every chapter and quest is open from the start, in any order, and a quest
  counts as soon as you have done it. The quests inside a chapter were never locked behind each other. **Quests you have already met in
  the later chapters are finished the first time the book checks (about a second after you join), each with its reward**, so on an existing
  world you may get a pile of random items at once (what does not fit in your inventory drops at your feet). A random reward can also be the
  very item another quest asks for, which finishes that quest too. The Drawer quest's text is shorter so it fits its line.

## 1.25.3

* **Using a trade never makes it dearer.** A villager's prices used to climb with demand: each time it restocked, the game added to a trade's
  demand by how much it had been used, and since trades never run out that kept growing, so a trade you used a lot ended up at the
  top price. Demand no longer grows, and the price ignores any demand a trade already has, so **a trade that had gone up goes back to its
  base price** (the discounts and surcharges for how the village sees you still apply). It applies to villagers and wandering traders
  alike, and the trading screen shows the same price the server charges.

## 1.25.2

* **Night vision in the free camera.** While the free camera is away from the player, the world is drawn with the Night Vision effect at full
  strength: caves, nights and unlit rooms are as bright as day. Only how it looks changes (your player is not given the effect, and it
  ends the moment you press K again). The free camera already had no fog and no cave culling, so you can now look through a whole
  mountain at night.

## 1.25.1

* **The Drawer quest takes any drawer.** "A drawer for everything" only counted the Oak Drawer (1x1), so crafting a Poplar Drawer, or
  any other wood or size, did not finish it. A quest can now name an item tag as well as an item (`"tag"` in `quests.json`), and any item
  in it counts: the Drawer quest uses `#nexium:drawers`, so a drawer of any wood and any size, crafted or held, finishes it.
* **Every Nexium recipe is in the recipe book from the start.** You are given all of them when you join a world (and again after a
  `/reload`, which also picks up recipes added by an update), instead of finding them one at a time. Recipes of the game and other mods
  unlock the way they always did.
* **The free camera no longer hides caves and terrain.** The game only draws the parts of the world it can reach from the camera through
  open space, so with the free camera inside rock, or under the world, most caves and terrain went undrawn until you turned or moved. The
  free camera now has that culling off (as the game already does for a spectator inside a block), so everything in view stays drawn.

## 1.25.0

* **Conduit Scanner.** A new item for seeing what a conduit network connects to, so you don't have to remember it. Craft it from a
  spyglass, a comparator, 2 iron ingots and redstone, then right-click any conduit with it. The window shows everything on that
  conduit network: for each block touching a conduit, which side it's on, whether it takes out (**Out**), puts in (**In**) or both
  (**In+Out**), and its channel colours, with priority and filter tags. There is a tab for each conduit type the network
  carries (Item, Fluid, Energy), and a button switches between a list **grouped by channel colour** and a flat list. Hover a row for the details in words.
  * **Warnings.** A red `!` marks a dead end: a side that takes out on a channel nothing inserts on, or inserts on a channel nothing takes out
    of. The tooltip also says when a side is paused or held off by redstone right now.
  * **Outlines.** Click a row to outline that block, and the conduit it hangs off, in the world for about 10 seconds; click again to remove it.
    **Clear outlines** removes them all.
  * The window refreshes while it's open, and a very large network is cut short (it says so). Energy has no channels, so that tab is a single list.

## 1.24.4

* **Storage connectors no longer connect to machines.** The Storage Connector and the Filtered Connector add an inventory to the network
  as storage, which suits chests and barrels but not machines with a different slot behind each face: a connector on a furnace saw
  only one face (the input), so fuel and output were out of reach. They now skip furnaces, blast furnaces, smokers, brewing stands,
  hoppers, dispensers, droppers, crafters and Nexium's own machines (farms, the Farm Assembler, generators, batteries, the Redstone
  Reactor, the Freezer, the Nexus Furnace, the Power Meter, the Lava Spring). Use an **Import/Export Bus** on the face you want to feed or empty
  a machine. **Existing connectors touching a machine stop connecting to it.** The list is a new setting, `connectorBlacklist`
  (Nexium Settings > Items and world): it starts with those blocks and the tag `#nexium:machines` (a data pack can add to it), and you can take
  an entry off to connect to that block again.

## 1.24.3

A maintenance release: fixes found by reviewing the code, each with a test.

* **Energy conduits no longer leak memory.** The energy network cache was a map that was never emptied, so every conduit, and the whole
  level with them, stayed in memory after a chunk unloaded or you left a world (in singleplayer, until the game was closed). A conduit
  now holds its own network, which goes with it, and the conduits beside a removed or unloaded one find their network again without it.
* **Storage networks never load chunks.** Scanning a network looked at blocks in the next chunk without checking it was loaded, which
  loads it. The scan now stops at the edge of what is loaded, and picks the rest up once it loads.
* **Builder Undo asks first.** It now respects spawn protection and the world border, as building does: blocks where you may not change
  blocks are left as they are.
* **A settings file with a mistake is kept.** If `config/nexium.json` can't be read, say after a typo, it is no longer replaced by the
  defaults and lost: it is moved to `nexium.json.broken-<date>-<time>` (and the log says so) before the defaults are written.
* **Dimensional Anchors drop their old-world migration.** It could release chunks someone else had forced with `/forceload`, and only
  applied to saves from before 1.1.4, which can't be loaded since 1.23.0 anyway. Anchors never touch the forced-chunk flag now.

## 1.24.2

* **Zoom**: hold the new Zoom key (unbound until you pick one under Key Binds, in Nexium) and the view closes in smoothly. While it is
  held the scroll wheel zooms in more or less, from 1.25x to 20x (4x to start; the hotbar does not change), the level is remembered,
  and letting go eases back out. Mouse turning slows down by the same amount so aiming stays steady. It works from the free camera too.
* **No fog in the free camera**: while the camera is away from your player, all fog is gone (the render distance's haze, and the
  water, lava, powder snow, blindness and darkness fog), so it sees clearly to the edge of the loaded world, even underwater or
  underground. Normal play is unchanged.

## 1.24.1

* **Reinforced Glass**: tinted glass that nothing but a player can break. It blocks light like tinted glass and drops itself with any
  tool, but the Wither, the Ender Dragon and every explosion (creepers, TNT, ghast fireballs, Wither skulls, end crystals) leave it
  standing. Craft 8 from 8 glass around an obsidian block (Tools tab; the Codex has a page on it).
* **Vein mining with any tool**: hold the Vein Mine key and mine with a pickaxe, axe, shovel, hoe, any other item or an empty
  hand, not just an AIOT. The block must be one the tool is right for (a bare hand takes what it can break, such as dirt, sand
  and logs; obsidian still needs a diamond-grade tool); every block costs the tool 1 durability and a tool that breaks stops the
  vein. An AIOT keeps the vein size from its tool menu; any other tool takes up to the `aiotVeinMaxBlocks` limit (256).
  The outline preview shows for any tool while the key is held. The setting `aiotVeinMining` now covers all tools.
* **Vein mining blacklist**: a new setting, `veinBlacklist` (Nexium Settings > All-in-one Tools, which opens a list you add to by
  typing a block id such as `diamond_ore` or a tag such as `#minecraft:logs`, or with one click for the block you last looked at),
  keeps those blocks out of vein mining for every tool, AIOTs included. The first block you mine still breaks.
* **Zoe bites 1.5 times as hard** as a wolf (6 damage instead of 4). It is a permanent attribute bonus, so it is saved with her and in
  her egg, and a Zoe saved before this version gets it too.
* **Zoe's skin**: the back of her head had an eye painted on it by mistake; it is plain fur now.
* **The Vein Mine key is now Left Control by default** (it was V). Players who already have the game's saved controls keep
  the key they have (Minecraft saved V for them); change it under Key Binds. Left Control is also the default Sprint key: both
  work while it is held, so change one of them in Key Binds if that gets in the way.

## 1.24.0

* **Zoe, the dog**: a new follower that behaves like a tamed wolf, with her own model and skin (a black and white speckled
  cattle dog with a green collar). She follows you, comes to you when you get far away, attacks what attacks you or what you
  attack, and lies down when you right-click her to sit. **Nothing can kill her**: mobs, falls, fire, lava, drowning and
  explosions do nothing (your own hits hurt but never kill; only the kill command and the void take her away). **Creepers run from
  her and cannot blow up while she is near.**
* **Dog Egg**: craft it from 5 bones and 4 leather (a Captured Mob called "Zoe's Egg"). Right-click a block to let her out, tame
  and with a green collar; sneak + right-click her with an empty hand to put her back. She can wear **Wolf Armor** or the new
  **Nexium Dog Armor** (6 Nexium Ingots, shaped like Wolf Armor), and her collar can be dyed like a wolf's. The Dog Egg and the
  armor are in the Tools tab, and the Codex has a page on her.

## 1.23.1

* **Key Binds screen**: two key names were too long and ran under their button. "Sort Inventory (over a container or your inventory)" is now
  "Sort Inventory" and "Cycle Villager Trades (in the trading screen)" is now "Cycle Villager Trades". The Codex's key page still says
  where each one works.

## 1.23.0

* **One mod id**: the two ids `storagenexus` and `nexusessentials` are now one, `nexium`. Every block, item, block entity, menu,
  recipe, advancement, loot table, tag, translation key and resource now lives under `nexium:`, and the mod loads as one mod called
  Nexium (nothing "provides" Nexus Essentials any more). **This is a clean break**: a world saved with an older version does not load
  correctly, because its blocks and items have the old ids; no aliases or conversion are included. The settings file is now
  `config/nexium.json`; an old `config/nexusessentials.json` is left alone and ignored, so settings start from the defaults. Key
  binds that were saved are reset for the same reason. The jar was already called `nexium-<version>.jar`. The Java packages moved from `net.storagenexus` to `net.nexium` (and `maven_group` with them) and the classes named after the old mods were renamed (`StorageNexus` is `Nexium`, `NexusEssentials` is `NexiumEssentials`, and so on; the armor and tool code in `net.nexium.nexium` is now `net.nexium.gear`), which matters only to anyone building against or alongside the mod.
* **Quest rewards**: every quest now gives a **random item from the mod** (any item shown in its creative tabs, each equally likely,
  except the Codex, Captured Mob and spent fuel rods), one for each roll: one in the first two chapters, two in the next three, three in
  the last.

## 1.22.6

* **Settings screen**: all 56 settings of `config/nexusessentials.json` can now be changed in the game, on **Nexium Settings**: open it from
  the **Settings** button in the Codex or in the armor menu, from **Mod Menu**'s Configure button (if you use it), or with the
  *Open Nexium Settings* key (unbound until you pick one). They are grouped by feature (Mobs, Items and world, AIOT tools and screens,
  Nexium armor, One-block farms, Freezer). A switch is an On/Off button; a number is a box held to its limits. Each setting has its own
  **Reset**; **Reset page** and **Reset all** do a group or everything. Hover a setting for what it does, its limits and default, and whether
  it is for your own game or for the world you host. Changed settings are gold, and changes are saved as you make them. Settings are read
  as they are used, so they apply at once in a world you host; a dedicated server still uses its own file.
* **Power Meter**: a new block (`IGI / RCR / III`: iron ingots, glass, redstone, a comparator) you put against any **Energy Conduit** and
  right-click for a **dashboard of that whole network**: how much it makes and uses each tick, the stored energy in the conduits and
  batteries with a bar and how long it lasts at the present rate, a minute of history (green made, orange used), a warning when machines
  go without, and every generator, machine and battery on it with its rate. **Control** (optional): each machine has a **priority**
  (High, Normal, Low) and an **On/Off switch**. When power runs short the Low ones go without first, and Off takes a block off the network
  (a machine stops taking power, a generator or battery stops giving it). They set the conduit sides that feed the block (the same priority
  the Conduit Wrench sets, -5 / 0 / +5), and the switch is saved on the side as well. The numbers update once a second and start again
  whenever any conduit is placed or broken.
* **Quests**: a separate quest book of **six chapters** (Storage, Power, Farms, Tools, Backpack, Nexium; 41 quests), opened with the
  **Quests** button in the Codex or the *Open Quests* key (unbound until you pick one). Each quest asks you to make or have an item; it
  counts what you have already done (the game's own crafting statistics, or what you hold), so nothing is repeated. A chapter opens
  once every quest in the one before it is done (a locked one says which). Every quest gives **random Nexium items** (ingots, a Nexium
  block or a Nexium disk part; one roll in the first two chapters, two in the next three, three in the last), with a message and sound.
  Progress is saved on the player and kept through death. **Recipes are never locked.** Quests are listed in
  `data/storagenexus/quests.json` inside the mod, with their text in the language file.
* **Faster storage networks**: a scan of a storage network is now shared by every block on it (buses, terminals, emitters, intakes...) instead
  of each block scanning on its own, and the cache holds many more networks. With 20 buses on one network at its size limit and storage changing
  every tick, a tick's work dropped from 24.8 ms to 2.9 ms. Nothing changes in how networks behave.
* **Stress tests** (not shipped in the mod, run in the build): 300 overclocked farms, a network at its size limit, 100 conduits with 20 machines
  and 20 players with Restocking all stay far under 1% of a server tick, apart from the network case above, now 6%.

## 1.22.5

* **Water breathing**: the full Nexium set now also gives **Water Breathing**, like the helmet's Night Vision: an infinite effect with a steady
  icon while all four pieces are worn, so you can't drown. It has its own on/off switch in the armor menu (G with no tool in hand), saved
  on the chestplate like the other set bonuses. Like the other armor effects it is taken off when a piece comes off or it is switched off,
  and a potion's or beacon's Water Breathing is never taken away.

## 1.22.4

* **Placement straight across with a gap**: with the Diagonal Placement key held, aim at the **middle of a face** (the middle half of it each
  way) and the block goes **straight across from that face with one block of gap**, two out from it and nothing between; aim nearer an edge
  and it is diagonal as before. Same key: where you aim decides. It follows the same rules as the diagonal one (uses the item up, aimed
  half of a slab, the face for logs, only into a free or replaceable cell, build protection).
* **The preview shows what is used**: on the block you aim at, the face is drawn in **zones**, as Tweakeroo does: a **middle square**
  (straight across with the gap) and a **trapezoid toward each edge** (diagonal). The zone you are aiming at is **filled in gold**, the
  others are faint lines. The outline of the cell the block would go in (white, or red when taken) stays, and is more see-through now.
  `diagonalPreview` (client) still turns it all off.

## 1.22.3

* **Restocking Module** (a new Backpack module; `IHI / CDC / IHI`: iron ingots, 2 hoppers, 2 chests, a diamond): a stack you are using up in
  the **hotbar or the offhand** (blocks placed, arrows shot...) is topped up to a full stack from the Backpack when it gets down to **8**,
  or refilled when the last one is used. Only use counts: a stack you move or throw away whole is left alone, and it does nothing in
  creative mode. If the Backpack has none of the item left but holds a **bigger form** of it, it **unpacks** just enough: Triple Compressed
  stone becomes Double Compressed, then Compressed, then stone (an iron block becomes ingots, and so on, from the crafting recipes, as
  for the Compacting Module), and what was not needed stays in the Backpack. Renamed or enchanted items are never unpacked.
* **Every stackable item stacks to 1,024 in the Backpack**, not only torches: cobblestone, ender pearls, anything that stacks at all. Tools and
  other items that do not stack still hold one. Sorting merges to 1,024.
* **Torch Stack Module retired**: with all items stacking to 1,024 it has nothing left to do. The item stays in the game so a Backpack or chest that
  holds one still loads (nothing is lost), but it does nothing, has no recipe, is not in the creative tab and no longer fits a module slot.
* **Fluid Intake** pulls **8 buckets a second** (it was 1) and holds **32 buckets** (it was 4).

## 1.22.2

* **Drawer Key**: the Lock Upgrade is now a **key**. Right-click a drawer, a compacting drawer or a fluid drawer with it to **lock** it, and
  right-click again to **unlock** it. The key is **never used up**, any key fits any drawer, and it works wherever you click on the
  drawer. A locked drawer is as before: each slot stays reserved for its item, even when empty, with the padlock mark top right on the
  front; it keeps its lock when you pick it up. The upgrade screen has four slots now, with no lock slot. It has a new look, a gold key with
  a purple gem.
* **Old locks carry over**: a Lock Upgrade in your inventory or a chest is a Drawer Key now (same item), and a drawer that had one in its
  lock slot stays locked and drops the key beside it the next time it is loaded.

## 1.22.1

* **Item conduits reach every face**: an item conduit (or any other pipe that uses Fabric's item storage, such as a Network Bus or
  Connector) can pull the output of a **one-block farm, the Iron Farm and the Freezer from any side**, not only the bottom. They still
  take nothing in. Hoppers are as before: they take the output from below only.
* **Freezer overclock**: the Freezer's screen has the same **OC** button as the farms (off, x2, x4, x8; Shift-click goes back), with the
  same rules: that many times the speed for that many times the NE/t, a bigger energy buffer, and a slower run, never a stop, when
  energy or water is short. The setting is saved and kept in the dropped item.
* **Packed ice and blue ice take half as long**: packed ice 22.5 seconds (450 ticks, was 900) and blue ice 45 seconds (900 ticks, was
  1800). The water per item is the same, so the water is used twice as fast, and so is the energy per item halved. A config file that is
  still on the old numbers moves to the new ones once; one you changed yourself is left alone.

## 1.22.0

* **Overclock**: every one-block farm, the Iron Farm and the Experience Farm has an **OC** button at the top right of its screen. Click
  it to step the farm through **off, x2, x4 and x8** (Shift-click steps back). At x4 the farm does 4 ticks of work every tick, so it
  cycles four times as fast, and draws 4 times the NE/t, so a cycle costs the same energy at any speed: you pay for the speed in
  power draw, not in extra energy per item. The energy buffer grows with the setting to keep holding ten seconds of work. With less
  energy than the setting needs the farm does not stop: it runs as fast as the energy it holds can pay for (a farm set to x8 with
  25 NE left, on 10 NE/t, still does 2 ticks of work that tick). The setting is saved with the farm and kept in the dropped item.

## 1.21.25

* **Nexus Link**: a shulker box (of any colour) or an ender chest mined with an AIOT whose Nexus Link is on is no longer sent to the
  storage network: it drops on the ground as it would without the module (an ender chest drops what it normally drops, obsidian
  unless Silk Touch). It holds in a single swing and in a vein or a 3x3 alike, where everything else mined still goes to the network.

## 1.21.24

* **Move up**: a new button in the Disk Drive's screen, beside Move to disks. It moves what the **lower tier disks in that drive** hold
  onto its **higher tier disks**, item disks onto higher item disks and fluid disks onto higher fluid disks (never one kind onto the
  other). Each disk's contents go to the biggest disk first, then the next biggest, as far as there is room, so swapping a 16k disk
  for a 64k one takes one click; what does not fit stays where it was, and disks of the same tier are left alone.

## 1.21.23

* **Free camera**: the speed now changes with **Left Ctrl and the scroll wheel** (the key is `Free Camera Speed` under Controls > Key Binds,
  in Nexium, so you can change it); the plain scroll wheel does nothing while the camera is away, not even the hotbar. The experience
  bar and its level number are hidden while it is on. Your own body is drawn once the camera is outside it, so you no longer look at the
  inside of your head right after switching it on.

## 1.21.22

* **Free camera**: press **K** (Controls > Key Binds, in Nexium) to take the camera off your player. You stay where you are while the
  movement keys and the mouse fly the camera through everything (forward goes where it looks, jump up, sneak down, sprint 3 times
  faster, the scroll wheel changes the speed). Nothing you click reaches the world while it is away, and the hand is not drawn. K
  again brings it back, and it ends by itself if your player is hurt or dies. All on the client.
* **Villagers' trades never run out**: every trade of a villager or wandering trader can be made as often as you like and none is
  ever locked. Prices still follow demand.
* **Trade cycling**: in a villager's trading screen press **C** (Controls > Key Binds, in Nexium) to give a villager nobody has
  traded with (a Novice with no experience and no trade used) a new set of trades, as often as you like. Once you have traded with a
  villager its trades stay. The new trades keep your discounts.
* **Mending anywhere**: experience orbs repair any damaged item with Mending in your inventory, not only what you hold or wear: the
  hotbar, the main inventory and the extra slots too. One at random per orb, at the usual rate.
* **Combined sponges**: nine sponges craft a **Large Sponge** (reach 14, soaks up 585 water blocks), nine of those a **Huge Sponge**
  (reach 32, 5,265) and nine of those a **Giant Sponge** (reach 64, 47,385); each crafts back into nine of the one below. They work
  like a sponge, turn wet, and a wet one dries in a furnace (10, 20 or 40 seconds) or at once in the Nether. A Backpack's Compacting
  Module packs sponges by itself.

## 1.21.21

* **More compressed blocks**: 13 more bulk blocks have *Compressed*, *Double Compressed* and *Triple Compressed* versions that hold
  **9, 81 and 729** of them, 39 new blocks in all: **calcite, dripstone block, smooth basalt, terracotta, sandstone, red sandstone,
  obsidian, magma block, mud, clay, soul sand, soul soil and snow block**. They work like the others (nine make one, each crafts
  back into nine of the one below, mined like the block they stand for, a Backpack's Compacting Module packs them). Compressed
  obsidian still takes a diamond pickaxe, and a Compressed Magma Block gives off light like a magma block. Ice has no compressed
  block: 9 ice already make packed ice and 9 packed ice make blue ice, each with a way back.
* **Way back for one-way packs**: a block that packed but could not unpack now crafts back into exactly what made it, so the
  Compacting Module packs them too: **snow block** (4 snowballs), **clay** (4 clay balls), **amethyst block** (4 amethyst shards),
  **glowstone** (4 glowstone dust), **honeycomb block** (4 honeycomb), **magma block** (4 magma cream), **melon** (9 melon slices),
  **dripstone block** (4 pointed dripstone) and **resin bricks** (4 resin bricks). Quartz, nether bricks, prismarine bricks and
  wool are left out because those blocks already craft into slabs in this version, and planks, coral, bamboo, honey bottles,
  flint and rabbit hide are not storage packs. Loose glowstone dust, honeycomb, magma cream and amethyst shards in a Backpack
  with the Compacting Module are packed 4 at a time until you unpack them.

## 1.21.20

* **Backpack modules work while its screen is open**: the Compacting, Feeding and Auto Torch modules used to wait until you
  closed the Backpack. Now they keep working with it open, on the slots you are looking at: Compacting packs the blocks on the
  screen (9 ingots you just put in become a block a moment later), Feeding eats the food shown there, and Auto Torch takes the
  torches from the screen. They used to wait because the open screen keeps its own copy of the slots and saves all of it on every
  change, so anything a module saved behind its back would have been put back over; the modules now go through the open screen.
  A Backpack opened from your inventory does not stop the worn one's modules, and is left alone.

## 1.21.19

* **Sorting the Backpack**: press the Sort key (**O**, `Sort Inventory` under Nexium in Controls) with the mouse over the
  Backpack's 36 slots and they sort **A to Z** like a chest: stacks merge first, shulker boxes and bundles go to the bottom-right
  slots, and only that area sorts (never the module slots, your inventory or the hotbar). Torches merge into stacks of **512**
  while the Torch Stack module is on, and into 64 without it. The same key over the inventory below sorts the inventory.
* **Fix**: sorting the main inventory while a Backpack screen was open could move the Backpack you opened it from out of its
  slot and close the screen. That Backpack now stays where it is while the slots around it sort.

## 1.21.18

* **Diagonal placement**: hold a key (**Left Alt** by default, rebindable under Controls > Key Binds, in the Nexium group) and
  right-click with a block in your hand to place it diagonally, off the edge of the block face you aim at: one step out from the
  face and one step over toward the nearest edge of that face. The block can float with nothing to hold it. It is placed as if
  you had clicked the cell itself, so stairs, slabs, logs and the like face the way they do for a normal click, and one block is
  used up (none in creative). The cell must be free or replaceable (grass, water) and within your reach, otherwise nothing is
  placed. Without the key, right-click works as before. The server checks every request (reach, build permission, loaded
  chunks, the block in your hand).
* **Diagonal preview**: while the key is held with a block in your hand, an outline shows the cell the block would go in, white
  when it is free and red when it is taken. `diagonalPreview` (client setting, on by default) turns it off.

## 1.21.17

* **Compressed blocks**: 48 new blocks, a *Compressed*, *Double Compressed* and *Triple Compressed* version of 16 bulk blocks that
  hold **9, 81 and 729** of them: stone, cobblestone, deepslate, cobbled deepslate, tuff, granite, diorite, andesite, netherrack,
  blackstone, basalt, end stone, dirt, sand, red sand and gravel. Nine make one (3 x 3 in a crafting grid) and each crafts back
  into nine of the one below. Each is mined like the block it stands for (same hardness, blast resistance, sound and tool), drops
  itself, and has an iron, gold or purple frame by level; its tooltip says how many of the plain block it stands for. A
  Backpack's Compacting Module packs them by itself (729 cobblestone become one Triple Compressed Cobblestone, 9 sand one
  Compressed Sand, not sandstone), and a Compacting Drawer holds a block and its compressed versions as tiers. They are in the
  Nexium Storage creative tab.
* **Mending**: what you collect from the Experience Farm (Collect all, +1 level, +10 levels) and the Nexus Furnace (Collect XP)
  now repairs first, as an experience orb does. A random damaged item you carry or wear with Mending, the elytra in the wings slot
  too, is mended at 2 durability a point, points it does not use go on to another damaged item, and the rest becomes levels. The
  level buttons take the same points as before, so a repair can leave you short of that level.
* **Shulker box preview**: a shulker box with something in it shows a 9 x 3 grid of its contents under its name, in slots
  the colour of the box, with every item and count. The normal item list under the name ("Iron Ingot x64 ... and 12 more")
  is hidden when it shows. An empty box and other containers keep their normal tooltip.
* **Backpack screen**: its four rows of nine slots are evenly spaced now (the last row sat a little apart), and your inventory
  below moved up to match.

## 1.21.16

* **Feeding Module** for the Backpack (4 bread around a golden carrot, with iron ingots in the corners): eats for you while
  the Backpack is worn. Once a second it checks how much of your hunger bar is missing, and eats the food in the Backpack that
  restores the most without any of it being wasted, so a missing 2 points eats nothing and a missing 8 eats a cooked steak
  rather than bread. It never eats foods with side effects (rotten flesh, pufferfish, golden apples, chorus fruit, suspicious
  stew and the like), gives back a bowl from stew, and does nothing in creative mode or while the Backpack's screen is open.
* **Compacting Module** for the Backpack (4 pistons around a crafting table, with iron ingots in the corners): packs what the
  Backpack holds into storage blocks once a second, in its own free slots: 9 iron ingots into an iron block, 9 nuggets into an
  ingot and on into a block, 9 wheat into a hay bale. It finds the packs from the crafting recipes, as the Compacting Drawer
  does, and only uses those that craft straight back, so nothing is lost or changed (sand never becomes sandstone) and modded
  blocks work too. Renamed and enchanted items are left alone, and nothing is packed that has no room for its block.
* **Torch Stack Module** now stacks torches to **512**, double the 256 of 1.21.15 (its icon says 512). Torches already in a
  Backpack stay as they are.

## 1.21.15

* **Backpack**: a new **backpack slot** above the trinket (magnet) slot in your inventory, for a Backpack crafted from
  **8 leather around a chest**. It holds **36 stacks**, as much as your whole inventory including the hotbar, and uses no
  inventory space. Wear it with **sneak + right-click** (or shift-click it into the slot). Press **B** (rebindable under
  Controls > Key Binds) to open the worn one, or **right-click** a Backpack in your hand to open that one. What is in it stays
  with the item: it drops with everything in it when you die (unless keepInventory is on) and can sit in a chest. A Backpack
  can't hold another Backpack. It also has **5 module slots**, each with an on/off button, and modules only work while it is
  worn.
* **Auto Torch Module** (iron ingots in the corners, 4 torches on the edges, a redstone lamp in the middle): places torches
  taken straight from the Backpack wherever a hostile mob could still spawn within **32 blocks** of you in every direction,
  in the open, in caves behind walls and in caves nobody has seen. It reads the real block light, so lit ground is left
  alone, and it spaces the torches as far apart as it can while leaving no dark block (a torch stops spawns 13 blocks away,
  so on open ground they form a staggered grid about 19 blocks apart). Up to 8 per sweep, only as many as the Backpack holds,
  and nothing in the Nether, in adventure or spectator mode or while the Backpack's screen is open.
* **Torch Stack Module** (iron ingots in the corners, 4 torches on the edges, a chest in the middle): torches stack to **256**
  in the Backpack's slots. Switched off or taken out, torches over a normal stack go back to the Backpack's other free slots
  and then to your inventory, so none are lost.
* In the creative inventory the backpack slot ends the lower row of extra slots.
* **Tunnel steps** for the 3x3 Mining module are now **off, 2, 3, 8, 16, 24 and 32** layers (up to `aiotTunnelMaxLength`).
  Hold the tunnel key (**Z**, rebindable) and **scroll the wheel** with an AIOT in hand to step up (deeper) or down
  (shallower); it stops at off and at the deepest, and the hotbar doesn't scroll while the key is held. A plain press of the
  key now only shows the readout, and the button in the tool menu still steps on and wraps round to off.
* **Crafting Terminal**: shift-clicking the crafting result crafts at most **2 stacks** (twice the result's stack size) per
  click, so one click can't flood the system. Click again for more; a plain click still crafts once.

## 1.21.14

* **Captured Mobs tell you more.** The tooltip always shows what the mob looks like: a frog, cat, wolf, cow, pig or chicken
  **variant** (so a warm frog is easy to tell from a cold one for the Froglight Farm), a horse's coat and markings, a sheep's
  wool colour, a villager's job, level and village type, and the variants of llamas, parrots, axolotls, rabbits, foxes,
  mooshrooms, pandas and tropical fish. **Hold Shift** for the rest: how long a baby has to grow up or an adult has to wait to
  breed again, whether it is in love, who tamed it (by name when they are nearby), its collar colour and whether it sits,
  what it wears and holds, its potion effects with the time left, and traits such as a slime's size, a charged creeper, a
  sheared sheep or a zombie villager being cured. A mob with nothing more to say shows no hint. The "Right-click a block to
  let it out" and Codex lines now come last.

## 1.21.13

* **Experience Farm**: a one-block farm that turns Nexus Energy into experience. Made at the Farm Assembler from **64 Bottles
  o' Enchanting** and nothing else. On **10 NE/t** it banks **40 XP a second** (2 a tick) while it has power, up to **200 levels'
  worth** (149,720 points), and starts again once you collect some. Its screen shows the bank (points and level) and the
  energy, with **Collect all**, **+1 level** and **+10 levels**: the level buttons give just enough to reach the start of that
  level, or everything banked if there is less. A redstone signal pauses it, a comparator reads how full the bank is, and
  broken it gives the bank back as experience orbs. XP is only collected by hand; it is not carried by conduits.

## 1.21.12

* **Import/Export Bus: choose the side it faces.** Its screen has a row of six buttons (D, U, N, S, W, E). Only sides with
  an inventory or tank can be chosen, the others are greyed out, the side it faces now is framed, and hovering a button
  names what is on that side. A new chest beside the bus becomes selectable at once. The old side gets an ordinary arm
  and the new one the snout.
* **Crafting Terminal** opens sorted **A to Z** instead of by count (the sort button still cycles name, count and mod).
* **Fix: the scroll wheel pulled items out of a bundle.** The wheel lifted the bundle onto the cursor and a click then
  poured its contents out. A bundle is never touched now, and over one the wheel keeps its vanilla job.
* **Nexus Furnace gives ten times the experience** the recipes do (raw iron 7, a steak 3.5).
* **Fix: Nexus Furnace experience came up short.** The bank was a sum of floats, and 0.7 is really 0.69999999, so ten raw
  iron made 6.9999999 and Collect XP handed over 6 instead of 7 (a hundred made 69 instead of 70). The bank is now kept to a
  thousandth of a point. The number on the Collect XP button was also capped at 32,767 while Collect took the whole bank; it
  now shows every point, so the button shows exactly what you receive.

## 1.21.11

* **Scroll wheel transfer**: with an empty cursor over a stack in a chest, barrel or machine screen, **scroll down** sends one
  item of it to the other inventory and **scroll up** pulls one matching item in from it. The player's main inventory and
  hotbar are one side, the chest or machine the other. It is done with ordinary slot clicks, so each slot's own rules
  decide what fits. Not in your own inventory screen, the creative menu or the terminals.
* **Nexus Furnace** is back to **30 NE/t** for every working slot (240 NE/t for all eight); its buffer is 48,000 NE again.
* **Redstone Fuel Rod** lasts **96 hours** at the default depth (was 8). A rod already in use keeps its wear, so it gets
  proportionally more time left.
* **Tier 1 Item and Fluid Conduits** now make **8** per craft (was 6), like the Energy Conduit.
* **Infinite Lava Source** is made with an **eye of ender** instead of a nether star.
* **Elevator** is made with **chiseled deepslate** instead of polished blackstone.
* **Nexium Ingot** no longer needs a netherite upgrade template: a diamond, an emerald and a netherite ingot make 3.

## 1.21.10

* **Fix: a Battery on an Energy Conduit now charges.** The conduit used to pull a Battery's charge into the cable at 500 NE a
  tick and only give back what was above half of the cable's pool, so a Battery next to one or two Solar Panels never rose
  past a few hundred NE (a second panel only made it look better by pushing the pool over half). Now **generators fill the
  pool first**, the machines are served from it, and a **Battery gives only what is short** of what the machines want and
  **takes whatever is left over**. Generators still never take energy in: a Solar Panel, Thermal or Combustion Generator or
  the Reactor only stores and sends.
* **Energy Conduit rates**: Tier 1 now moves **2,500 NE/t** a side (was 500) and Tier 2 **50,000 NE/t** (was 4,000). A
  segment's share of the pool is four ticks of that, **10,000 NE** (Tier 1) and **200,000 NE** (Tier 2). The Tier 1
  recipe now makes **8** conduits (was 6).
* **Nexus Furnace** uses **10 NE/t** for every working slot (was 30), 80 NE/t for all eight. Its buffer is still ten seconds'
  worth, so it now holds 16,000 NE (was 48,000).
* **Solar Panel**: makes **30 NE/t** (was 20), **8 NE/t** in rain (was 5) and stores **5,000 NE** (was 400).
* **Shift + click and drag now also works in your own inventory screen**: drag across the hotbar and the inventory to move
  everything you cross to the other part. (Space + click stays in chests and machines only.)

## 1.21.9

* **Nexus Furnace**: a furnace on Nexus Energy with **eight smelting slots running at once**, each with an output slot of its
  own. It smelts, blasts and smokes in one (anything a furnace, blast furnace or smoker cooks), 2.5 seconds an item, 30 NE/t
  for every working slot. With less energy it runs as many slots as it can pay for, taking turns. A stack put in is shared
  out evenly over the input slots; hoppers and pipes go in from the top and sides and out from below. The experience is kept
  until you press Collect XP. Recipe: `III / FBS / RIC` with iron ingots, a furnace, a blast furnace, a smoker, a redstone
  block and a copper block.

## 1.21.8

* **Moving every stack of an item is now Space + left click** (it was shift + left click in 1.21.6). Plain shift + click is
  vanilla's again: just the stack you clicked. Shift + left click and drag still moves everything the cursor passes over.

## 1.21.7

* **Stonecutter: a block of quartz cuts back into 4 quartz.**

## 1.21.6

* **Inventory sorting has its own key, O**, separate from JEI, REI and EMI (whose R shows recipes), and **it works with the
  mouse over an item** as well as over an empty slot. The key is a new `Sort Inventory` entry under Nexium in Controls, so
  it starts at O even if you had bound the old one; rebind it as you like.
* **Shift-click tweaks**: with an empty cursor, shift + left click on an item sends **every stack of that item** from that
  inventory to the other, and shift + left click and drag moves **every item the cursor passes over**. Not in the survival
  inventory screen, the creative menu or on crafting results.
* **Sorting is just A to Z.** The creative menu order and biggest-stacks-first modes are gone, and pressing the key again
  does nothing new.
* **The Honey fluid is gone.** Nothing has made it since the Bee Farm's tank was removed in 1.20.1, and it still showed up
  in recipe viewers under the mod. A fluid drawer or disk that still holds some now loads that slot empty instead of failing.

## 1.21.5

* **Three creative tabs** instead of one:
  * **Nexium Storage**: the guide, drawers, barrels, the network blocks, terminals, disks and the Disk Drive, tablets,
    upgrades, filters, and the item and fluid conduits.
  * **Nexium Tools**: AIOTs, modules, Construction Builders, Nexium ingot, block, tools and armor, the magnet, the elixir,
    the Elevator and the Dimensional Anchor.
  * **Nexium Farms & Power**: every farm, the Farm Assembler, the Freezer, generators, Batteries, the Redstone Reactor and its
    rods, and Energy Conduits.

## 1.21.4

Recipe and loot tweaks, written from the end results of a combined datapack. Anything the mod already does (elevators, the
creeper, ghast and enderman grief rules, elytra protection, netherite calming piglins) was left out.

* **Bigger batches**: stairs make 8 instead of 4, trapdoors 12 instead of 2, wood and hyphae 4 instead of 3, and bricks make 4
  brick blocks from 4 bricks. Chiseled and cracked stone bricks make stone brick stairs too.
* **Slabs and stairs back to blocks**: 2 slabs make their block, 4 stairs make 3 blocks, and in the crafting grid 1 block makes
  2 slabs.
* **Universal dyeing** of candles, concrete powder, glazed terracotta, stained glass, panes and terracotta (ice washes the
  colour out), plus craftable coral blocks, blackstone and blackstone redstone parts, notch apples, horse armor, copper
  powered rails, gravel from flint, red sand and sandstone from dye, ice and nether wart from their blocks, black dye from
  coal, shapeless bread, paper and shulker box, rotten flesh to leather and wool to string in a furnace, and the Armadillo Crate.
* **Loot**: shulkers drop 2 shells instead of 1 (same chance), and budding amethyst drops itself with Silk Touch.

## 1.21.3

* **Tree Farm: Poplar.** The new Poplar tree is now one of the Tree Farm's modes (13 in all: ten woods, the two nether
  stems and chorus).
* **Froglight Farm makes 5 of each colour** (ochre, verdant and pearlescent) every 20 seconds, up from 1.
* **Redstone Reactor stores 1,000,000 NE** (it was 100,000).
* **Redstone Reactor makes twice the power.** A rod makes 200 NE/t at the default depth (1,800 with nine), and 100 to 300
  NE/t a rod from the shallowest depth to the deepest, up to 2,700 NE/t. Heat and rod wear are unchanged.

## 1.21.2

Energy Conduits share a pool.

* **Shared energy pool, like Mekanism's cables**: touching Energy Conduits form one network that holds energy, 2,000 NE per
  Tier 1 segment and 16,000 per Tier 2 segment, added together. Generators and Batteries fill the pool, machines draw from
  it, so a long run of conduit keeps a reserve and evens out the supply. The energy is kept in the conduits and saved with
  them; a broken conduit loses its share. Energy ignores colour channels.
* **Machines before Batteries.** A conduit used to hand its energy to the nearest block first, so a Battery beside it
  could soak up everything and starve a farm. Now machines are served in priority order, equal priorities share evenly,
  and Batteries take only what is above half of the pool.
* **Jade support**: with Jade installed, looking at a farm, the Freezer, a generator, a Reactor, a Battery or an Energy
  Conduit shows its Nexus Energy (a conduit shows its whole network's pool). Jade is optional.

## 1.21.1

Farms run on power alone.

* **No more inputs or boosts.** Every one-block farm, and the Iron Farm, now needs only Nexus Energy: the Villager Breeder's
  bread, the Piglin Barter Farm's gold, the Livestock Farm's feed, the Moss and Crop Farms' bone meal and the Iron Farm's
  bread are gone, with their slots, boosts and cooldowns.
* The farms that needed an input now just work: the Breeder makes a baby villager every 10 minutes, the Barter Farm
  trades every 6 seconds, the Moss Farm and Crop Farm run at their normal rate, and the **Livestock Farm** makes the drops
  of a cow, sheep, chicken (and an egg), pig and rabbit every 20 seconds.
* Items an older farm still holds in its input slot are dropped; the Iron Farm's bread too.
* The `ironFarmBreadSpeedUp`, `ironFarmBreadSeconds` and `ironFarmCooldownMultiplier` settings are gone.

## 1.21.0

Power. Every one-block farm and the Freezer now run on Nexus Energy.

* **Nexus Energy (NE)**: farms use 10 to 40 NE a tick, the Freezer 20; the Disk Drive needs none. A farm without power waits
  and says so.
* **Generators**: Combustion Generator (20 NE/t, anything a furnace burns), Solar Panel (20 NE/t in daylight, 5 in rain),
  Thermal Generator (80 NE/t on lava) and the **Redstone Reactor**.
* **Redstone Reactor**: nine Redstone Fuel Rod slots (100 NE/t each at the default depth, any one to nine work) and three ice
  coolant slots. One depth for all rods trades power (50 to 150 NE/t a rod) for heat and wear; output falls above 60% heat
  and the reactor shuts off at 90% until it is under 50%. Rods last 8 hours at the default depth. No explosions. New items: Redstone Fuel Rod and Spent Fuel Rod.
* **Energy Conduit** (500 NE/t) and **Ender Energy Conduit** (4,000 NE/t), a third conduit kind with its own colours.
* **Tree Farm**: 18 stacks of logs at the Farm Assembler. A mode button picks the tree (every wood, the nether stems and
  chorus); every 20 s it makes 8 logs of that tree, or 4 chorus fruit and a 5% chorus flower. 10 NE/t.
* **Batteries** in iron, gold and diamond: 250,000, 1,000,000 and 10,000,000 NE. A broken Battery keeps its charge.
* **Every farm's filler is now 576** (nine stacks), the Iron Farm's smooth stone included (the Tree Farm takes 18 stacks of logs).
* Fix: a Solar Panel with a block directly on top of it no longer works.

## 1.20.3

Fixes from a review of the whole mod, two balance changes, and comparator output.

* **Construction Builder: no more free items.** A copy of the clicked block is now a freshly placed block facing the
  same way, so it no longer copies a crop's growth (seeds placed ripe wheat), a stack of candles or snow layers, and the
  like. A double slab is still placed as one, and now uses two slabs (it used one, doubling them).
* **Exchange gives what the old blocks drop when broken**, not the blocks themselves: cobblestone for stone, nothing for
  glass, and no more budding amethyst or reinforced deepslate. Undo takes those drops back.
* **A Construction Builder that breaks drops its Nexus Link Module**, as an AIOT does.
* **Farms no longer lose output when nearly full.** The Gourd and Cactus, Bee, Froglight and Moss farms made more kinds
  of item than they left room for, and the rest vanished; they now wait for enough free slots.
* **Crafting Terminal**: shift-clicking a craft your inventory has no room for all of no longer loses the rest; it goes
  into the network, else onto the ground.
* Fences, panes and walls placed by the builder join their neighbours, and Undo takes all of them back (it used to
  skip the ones whose neighbours it had just removed).
* **Monster Farm**: the Totem of Undying is a 2% find each cycle, not one every cycle.
* **Comparators read drawers and farms.** A drawer (item, fluid or compacting) gives 0 when empty and 15 when full,
  like a chest; one with 2 or 4 sections gives their average. A farm gives how full its output is, and 15 once it has
  stopped for lack of room.
* **Picking up bosses**: the Warden and the Elder Guardian cost 10 experience levels (`bossPickUpLevels`, 0 for free).
  The Ender Dragon and the Wither still can't be picked up.

## 1.20.2

* **Froglight Farm**: a new one-block farm at the Farm Assembler. It takes 3 temperate, 3 warm and 3 cold frogs and 9
  slimes (all Captured Mobs) and 256 glass of any colour, and makes 1 ochre, 1 verdant and 1 pearlescent froglight
  every 20 seconds (`froglightFarmTicks`).

## 1.20.1

* **The Bee Farm makes honey bottles**: every cycle it puts 1 to 3 honeycomb and 2 honey bottles in its output slots,
  where hoppers and pipes take them. The internal tank and the glass bottle right-click are gone; honey a 1.19 farm
  had stored comes out as bottles when the world loads.
* **The Gourd and Cactus Farm makes everything at once** (12 melon slices, 3 pumpkins, 4 cactus and 6 kelp a cycle);
  its mode button is gone.
* **The Iron Farm no longer makes poppies** (the `ironFarmPoppies` setting is gone).
* **The Moss Farm also makes resin and the pale variants**: 6 resin clumps, 3 pale moss blocks, 2 pale moss carpet and 2
  pale hanging moss a cycle, on top of the usual moss.
* **The Nexium Spear's textures lose two stray corner fragments** (floating pixels at the edges).
* **The sort key's message is drawn on top of the screen**, at the top centre, so it can be seen over a tall barrel.

## 1.20.0

* **Inventory sorting**: press **R** over a container or over your main inventory to sort just that area, never the
  hotbar. Works on chests, barrels, ender chests, shulker boxes, hoppers, dispensers, droppers and Nexium barrels. Stacks
  merge, and shulker boxes and bundles go to the bottom right. Press again to step through A to Z, creative menu order
  and biggest stacks first. With a recipe viewer installed, R over an item belongs to the viewer. The key is `Sort
  Inventory` in Controls.

## 1.19.0

* **Ten more one-block farms** at the Farm Assembler, 768 of a filler block each: **Monster** (zombie, skeleton, spider,
  creeper and raider loot, mixed), **Nether Mob** (blaze, ghast, magma cube), **Ocean** (guardian, drowned, cod),
  **Slime**, **Livestock** (the feed picks the animals), **Crop** (bone meal speeds it up), **Gourd and Cactus** and
  **Generator** (a mode button picks what they make), **Bee** (honeycomb, and honey in an internal tank) and **Moss**
  (needs bone meal). Mob farms roll the real loot tables as if a player killed the mob, with no Looting.
* **Honey**: a new fluid that only lives in tanks and pipes. The Bee Farm's 16-bucket tank gives honey bottles to a glass
  bottle (right-click) or honey to pipes, and takes nothing in.
* Farm settings: `monsterFarmTicks`, `netherMobFarmTicks`, `oceanFarmTicks`, `slimeFarmTicks`, `livestockFarmTicks`,
  `cropFarmTicks`, `gourdFarmTicks`, `generatorFarmTicks`, `beeFarmTicks`, `mossFarmTicks`.

## 1.18.1

* **The recipe book's search follows the terminal's**: in a Crafting Terminal, typing in the recipe book's search box
  changes the terminal's, and typing in the terminal's changes the recipe book's (and JEI's or REI's, as before). A
  right-click on the recipe book's search box clears both. The client setting `terminalSearchSync` turns the linking off.

## 1.18.0

* **Four more one-block farms**, made at the Farm Assembler: the **Witch Farm** (2 captured witches; a witch's own drops
  every 10 seconds, as if a player killed her), the **Sugar Cane Farm** (3 sugar cane every 20 s), the **Bamboo Farm**
  (4 bamboo every 20 s) and the **Nether Wart Farm** (4 nether wart every 30 s). They need no input and ask 768 of their
  filler block instead of the 2,900 to 3,100 the first five do. Times are settings: `witchFarmTicks`, `sugarCaneFarmTicks`,
  `bambooFarmTicks`, `netherWartFarmTicks`.
* **The Assembler's list scrolls** (mouse wheel, with a scroll bar) now that it has eleven entries, and keeps the chosen
  one in view.
* **The Villager Breeder takes 10 minutes** instead of 20 (`villagerBreederTicks` 12000). A config file still on the old
  24000 is moved to 12000 once.

## 1.17.1

* **Right-click clears a search**: a right-click on the search box of a terminal or of the Codex clears it, with the cursor
  left in the box ready to type. In a terminal, JEI's or REI's search clears with it.

## 1.17.0

* **Vein mining takes stone and plants**: holding V now vein mines stone, cobblestone, deepslate, netherrack and the rest
  of the stone family too (without V, the 3x3 module mines as before). Grasses, ferns, bushes, dead bushes, azaleas and
  leaf litter (`#storagenexus:vein_plants`) vein together as one kind, with any AIOT and at no durability, so a mixed
  patch clears in one swing. `#storagenexus:vein_excluded` is now empty, for data packs to fill.

## 1.16.2

* **Conduit Wrench texture**: removed two stray pixels that floated beside the wrench at its top and right edges.

## 1.16.1

* **The AIOT's 3x3 and vein outline no longer stays on**: like the readout by the hotbar, it shows for 20 seconds after
  you switch to an AIOT or press the 3x3 (X), tunnel (Z) or vein (V) key, and while V is held. `aiotHudSeconds` sets the
  time for both (0 keeps them up).

## 1.16.0

* **Digital storage**: the **Disk Drive** holds 8 storage disks on a storage network. Item disks hold 16k, 64k, 256k,
  1M or 4M (Nexium) items of any kinds, fluid disks 64 to 16,384 buckets. Disks are a Disk Casing plus a Storage Part;
  three parts and gold, a diamond, an emerald or a Nexium ingot make the next size. Contents are saved with the world,
  so a disk keeps them wherever it goes. Drives start at priority 10 so new items go to disks first, take a filter,
  and their **Move to disks** button empties the network's chests and barrels onto the disks (drawers are left alone).
  Lights on the drive show each disk's fill.

## 1.15.0

* **Construction Builder**: right-click a block to build out from it, with the block in your offhand or a copy of the one
  you click. Iron (9 blocks a click), Diamond (27), Netherite (64) and Nexium (128, unbreakable). Modes Extend, Line and
  Exchange; a plane lock (any, flat, upright); a size setting; a pale blue outline of where the blocks will go; and Undo
  for the last 2 builds, all in its tool menu (G). A Nexus Link Module in it takes blocks from a storage network when the
  inventory runs out.
* **Right-click harvest**: right-click a grown crop to harvest it and replant it. With V held, the whole connected field.
  Settings `rightClickHarvest` and `harvestVeinMax`.
* **Nexium Ingot recipe**: a diamond, an emerald, a netherite ingot and a netherite upgrade smithing template make 3
  ingots (it used blocks of each before).

## 1.14.5

* **Cheaper drawers, no chests**: drawers are now made from planks and slabs of one wood, like a barrel. 1x1:
  `PSP / S S / PSP` (makes 1), 1x2: `PSP / PPP / PSP` (makes 2), 2x2: `SPS / PPP / SPS` (makes 4). A 1x1 now costs about
  6 planks' worth instead of 16. A game test makes sure no other recipe uses the same grids.

## 1.14.4

* **The anvil is never Too Expensive**: a job that costs 40 levels or more can now be done for its full cost, as long as you
  have the levels, instead of the result disappearing. The screen shows the real cost instead of "Too Expensive!". The
  setting `anvilNoCostLimit` (on by default) set to false brings back vanilla's limit.

## 1.14.3

* **Terminal search follows JEI and REI**: typing in a Storage or Crafting Terminal's search also searches JEI's (or REI's)
  item list, and typing in theirs searches the terminal; a terminal opened with an empty search takes theirs. The client
  setting `terminalSearchSync` turns it off.
* **Missing ingredients are marked**: when the network, your inventory and the grid can't make a recipe, JEI's "+" now marks
  the exact slots that are missing in red, and REI's lists the missing items in its tooltip, instead of only saying
  something is missing.

## 1.14.2

* **Connector priority works as set**: items go into the highest priority inventory that takes them. Before, any
  inventory that already held some of the item was filled first whatever its priority, so a lower priority barrel holding
  a few could take everything ahead of a higher priority Drawer Controller. Within one priority, items still join their
  own kind first. An inventory touched by two connectors now uses the higher of their priorities.
* **Drawer Controller reach outline needs sneak + right-click**: a plain empty-hand right-click now only says how many
  drawers it controls, so checking it (or double-clicking to dump your inventory) no longer covers the area in particles.
* The Farm Assembler's game tests now run (they were missing from the test list), and a check makes sure every game test
  class is listed.

## 1.14.1

* **AIOT readout by the hotbar shows for 20 seconds**: after you switch to an AIOT, press the 3x3 (X), tunnel (Z) or vein
  (V) key, or change something in the tool menu, instead of the whole time you hold one. It stays up while V is held and
  fades out over its last second. The new client setting `aiotHudSeconds` changes the time (0 keeps it up).

## 1.14.0

* **Farm Assembler**: a new block that makes the one-block farms and the Master Librarian, replacing the renamed chests.
  Pick what to make from its list, put in what it needs (Add from inventory, shift-click, or hoppers and pipes), watch
  each material fill up with a bar and a tick, and press Build. It only takes what is still needed; picking something else
  hands back what was in, and breaking it drops everything. Recipe: a crafting table, 4 iron ingots and 4 lapis lazuli.
* **Same costs**: each farm takes its old recipe plus the filler block its double chest held (for example 1,408 smooth
  stone for the Iron Farm, 3,008 end stone bricks for the Shulker Farm); the Master Librarian takes 12 villagers.
* **Renamed chests no longer turn into farms or the Master Librarian.** The chest-name settings are gone from the config;
  `librarianVillagers` stays.

## 1.13.1

* **AIOTs redrawn at 32x32**: the curved pick-and-axe head is chunkier, each tier is back in its original colours with
  flat, Minecraft-style shading, and the gold-framed gem, wrapped grip and jewelled pommel stay. The Fortune and Silk
  Touch gem colours (green, pale blue) now recolour the gem itself instead of sitting beside it.

## 1.13.0

* **The Nexium Codex replaces the guide book.** The guide item now opens a built-in wiki: a sidebar with 11 categories
  (Nexium and Nexium Armor are one category now, and the old Essentials and Mobs chapters are split by subject), a search
  box, and one scrolling page per topic with no page or line limits. Words that name another topic are links, recipes are
  crafting grids (click an ingredient to open its page), "See also" lists related topics, and back, forward and Home work
  like a browser. The Codex reopens where you left it. All the old guide text carries over, plus "Using the Codex" and
  "What's new" pages.
* **Look up any item**: in any inventory, hover over an item and press H (Look up in the Codex, in Controls) to open its
  page; Escape comes back to the inventory. Outside a screen, H opens the Codex. Items with a page say so in their tooltip.
  A game test makes sure every item in the mod has a page.
* **Every texture redrawn at 64x64**: new AIOTs (a crystal pick-and-scythe head with a gold-framed purple gem, a wrapped
  grip, and the blade in each tier's material), new Nexium armor (crystal plates, gold trim and purple gems, on the items
  and on the armor you wear), Nexium tools with wrapped grips, gold rivets on upgrades, modules and filters, and every
  block, item, GUI sprite and the mod icon at four times the detail. Blocks keep their layouts, so cables, connectors and
  dyed Freezers look right.
* **Dimensional Anchor always loads**: the On-demand mode is gone, so an anchor keeps its 3x3 chunks loaded all the time.
  Anchors that were set to On-demand now always load too. Placing an anchor, or right-clicking it, outlines the chunks it
  loads for 1 minute (the chunk borders as a grid at its height, with posts at the corners); right-click again to hide it.
  The anchor is now teal-green instead of purple.

## 1.12.2

* **The Vein Mining Module is retired.** Vein mining never needed it: hold the Vein Mine key (V) with an AIOT. The vein
  size is now set on the AIOT itself, with the Vein button in the tool menu, and an AIOT has two module slots (Nexus Link
  and 3x3 Mining). The module can't be crafted any more. It stays in the game only so that worlds don't lose anything:
  an AIOT, chest or shulker box that holds one still loads, right-clicking it gives back its materials (4 iron ingots,
  4 lapis lazuli, a diamond), and so does opening the tool menu of an AIOT that still has one installed. AIOTs saved with
  three module slots keep their Nexus Link and 3x3 Mining modules.

## 1.12.1

* **Guide**: the Essentials contents now link the Higher levels, Elevator and Master Librarian pages; the Mobs chapter points
  to the Nexus Librarian chest; the AIOT chapter has a page on the mining outline and the readout.
* **Vein mining needs no module**: hold the Vein Mine key (V) with an AIOT and it mines the whole vein of any block the tool is
  right for, including obsidian, crying obsidian, hay bales, melons, pumpkins and every kind of leaves (not the stone family).
  A Vein Mining module is now optional: it sets the size and can switch vein mining off. Obsidian needs a tool strong enough
  to drop it.
* **Five more one-block farms**, each made from a named double chest like the Iron Farm: Villager Breeder (a baby villager
  every 20 minutes), Piglin Barter Farm (gold ingots for the vanilla barter loot), Zombified Piglin Farm (gold nuggets and
  rare ingots at Looting V rates, no rotten flesh), Wither Skeleton Farm (coal, bones, skulls) and Shulker Farm (shulker
  shells). Chest names, recipes and cycle times are in the README and the config.
* **Freezer**: fill it with water and it freezes ice, packed ice or blue ice (a button switches; blue ice takes 90
  seconds). Dyeable like leather armor, washed in a cauldron. Packed ice now crafts back into 9 ice and blue ice into 9
  packed ice.
* **Soul sand to sand** in a Blast Furnace, no experience, at an ore's speed.
* Guide pages for all of it, with contents links.

## 1.12.0

* **Higher enchantment levels**: Protection (all four kinds), Unbreaking, Looting, Fortune and Respiration go up to V, in the
  enchanting table, at an anvil, in loot chests and from villagers.
* **Fewer exclusions**: all four protections on one piece, Mending with Infinity, Sharpness with Smite. Bane of Arthropods
  still excludes both.
* **Master Librarian**: rename a chest "Nexus Librarian" and put in exactly 12 villagers (spawn eggs or Captured Mobs) to get a
  level 5 librarian as a Captured Mob. He sells a book of every enchantment at its highest level (Mending too, no curses) at
  the villagers' prices. Config `librarianChestName` and `librarianVillagers`.

## 1.11.0

* **Elevator**: a flat Nexium platform. Elevators in the same column (same X and Z) link from the bottom of the world to the
  top. Right-click one for a floor list (highest first, mouse wheel scrolls), type a floor name in the box, and click a floor
  to teleport there with no fall damage. Unnamed floors read "Floor 1, 2, 3" from the bottom; floors with no room to stand
  are greyed out. Recipe: 3 polished blackstone, 4 iron ingots, 1 redstone block (makes 2). Includes a guide page.

## 1.10.0

* **Poplar drawers** (1x1, 1x2, 2x2): every Minecraft 26.3 wood type now has drawers.
* **AIOT readout**: while an AIOT is held, a small panel in the bottom right shows Silk Touch or Fortune (when it holds both)
  and each module's state (3x3 on or off, tunnel depth, vein size, Nexus Link). Turn it off with the client setting `aiotHud`.

## 1.9.0 (includes 1.8.0 and 1.8.1)

* **Mining outline**: with an AIOT in hand, the blocks the next swing would also take (3x3, tunnel, or vein while the key is
  held) get a faint pale green glow. Client setting `aiotMiningPreview`.
* **Tunnel mode** for the 3x3 Mining module: dig 2, 4, 8, 16, 24 or 32 layers deep. Step through the lengths with **Z** or the
  tool menu's Tunnel button. Config `aiotTunnelMaxLength`.
* **Vein mining works on every block except stone varieties** (stone, deepslate, netherrack-type base stones, cobblestone).
  It still skips blocks with contents, such as chests.

## 1.7.0

* **Stone AIOT**: a new first tier, crafted from the five stone tools.
* **X key** switches the 3x3 Mining module on and off.
* **Modules drop** at your feet when an AIOT breaks.
* **AIOT durability is 1.5 times higher** for every tier (merging worn tools scales to match).

## 1.6.1

* Version bump only, so builds and the jar name match the release.

## 1.6.0

* Renamed to **Nexium**.
* Vein mining controls (hold **V**), armor and tool menu switching, arrow and totem slots, and new Nexium armor and AIOT art.

## 1.5.1

* Faster AIOT and Nexium code paths, config switches, and Abyss Conduit art.

## 1.5.0

* **AIOTs** (all-in-one tools), **Nexium** gear, the tool menu with module slots (Nexus Link, 3x3 Mining, Vein Mining), and
  Nexium armor bonuses.

## 1.1.4

* Dimensional Anchors use their own chunk ticket instead of the shared forced flag.

## 1.1.3

* The Magnet scans less often; a save/load test now covers every block entity; client tests run in CI.

## 1.1.2

* Barrels keep their contents when the world is reopened.

## 1.1.1

* The Magnet pulls back what you drop yourself.

## 1.1.0

* **Nexus Essentials merged in**: the Magnet, Elixir of Bedrock Breaking, Iron Farm and the extra slots now ship in this mod,
  in its creative tab.

## 1.0.x (1.0.1 to 1.0.26)

* **1.0.26**: fill bar under each drawer section's count.
* **1.0.25**: separate insert and extract conduit channels, faster drawer clicks, cable-grey network blocks.
* **1.0.24**: barrels fill their grids (Gold 6x11, Diamond 6x14, Emerald 6x17, Netherite 6x20).
* **1.0.20**: capacity upgrades x2 to x32 with no total cap; emerald upgrade and barrel.
* **1.0.19**: Level Emitter counts fluids; bigger barrels; network capacity shown in the terminal.
* **1.0.18**: JEI and REI recipe transfer; fixed swapped terminal mouse buttons.
* **1.0.17**: Import/Export Bus, Level Emitter, tiered barrels, fluid links, intake filter.
* **1.0.16**: terminal sort direction and exact counts; Void Upgrade warning.
* **1.0.15**: link names linger 3 seconds, one-minute controller outline, channel rings on conduit arms.
* **1.0.14**: lock slot and padlock on fronts, upgrade fit hints.
* **1.0.13**: Escape closes terminals while typing in search.
* **1.0.12**: Output side toggles; screens name what is on each side.
* **1.0.11**: Dimensional Outputs push into inventories, networks and item conduits on every side.
* **1.0.9 and 1.0.10**: all screens use the dark inventory colours.
* **1.0.8**: Compacting Drawer holds 54 stacks of the largest form.
* **1.0.3 to 1.0.7**: terminal grid, recipe book and colours; conduit tier names; link name display.
* **1.0.2**: item descriptions and the in-game guide book.
* **1.0.1**: first release on Minecraft 26.3 and Fabric API 0.165 (drawers, controller, fluids, storage network,
  Dimensional Input and Output, conduits).
