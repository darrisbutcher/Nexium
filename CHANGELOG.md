# Changelog

All notable changes to Nexium (mod id `storagenexus`), newest first. The version is `mod_version` in `gradle.properties`.
Pull requests are squash merged, so each one is a single commit on `main`.

## 1.14.2

* **Connector priority works as set**: items go into the highest priority inventory that takes them. Before, any
  inventory that already held some of the item was filled first whatever its priority, so a lower priority barrel holding
  a few could take everything ahead of a higher priority Drawer Controller. Within one priority, items still join their
  own kind first. An inventory touched by two connectors now uses the higher of their priorities.
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
