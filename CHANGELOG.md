# Changelog

All notable changes to Nexium (mod id `storagenexus`), newest first. The version is `mod_version` in `gradle.properties`.
Pull requests are squash merged, so each one is a single commit on `main`.

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
