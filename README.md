# Nexium (Fabric, Minecraft 26.3)

Drawers, a storage network with crafting terminals, cross-dimensional tablets and item links, item and fluid
conduits, all-in-one tools (AIOTs), **Nexium** (a tier above netherite) and a set of quality-of-life **Essentials**
(no creeper or ghast block damage, an item and XP magnet, wings, arrow and totem slots, Fortune on ancient debris,
a 1-block iron farm and more).

Every item is in the **Nexium** creative tab, and every tooltip says what its item does.

The **Nexium Codex** is the in-game manual, a small built-in wiki. Right-click the **Nexium Guide** (given once when you join
a world, or crafted from a book and any drawer) to open it, or press **H** outside a screen. The sidebar lists 11 categories
(Getting Started, Storage, Storage Network, Transport & Links, All-in-one Tools, Nexium & Armor, Equipment Slots, Farms &
Machines, Mobs & Villagers, Enchanting & Effects, World Tweaks) and a search box finds any topic. Each topic is one page
that scrolls: words that name another topic are links, recipes are drawn as crafting grids (click an ingredient to open its
page), and "See also" lists related topics. Back and forward work like a browser. In any inventory, hover over an item and
press **H** to open its page; Escape brings you back. Every item in the mod has a page.

The **Nexium advancement tab** (press **L**) is a quick map of everything in the mod: 33 nodes with an icon and a short
description each, all unlocked from the start. For a bigger, easier-to-read advancements screen, try the optional
[Better Advancements](https://modrinth.com/mod/Q2OqKxDG) mod: the tab works with it automatically, and Nexium doesn't need it.

## Install and build

Get the jar from the repository's **Releases** page, or build it yourself as below. A release is published automatically
whenever a new `mod_version` (in `gradle.properties`) is merged to `main`: it has that version's jar and its notes from
`CHANGELOG.md`.

Put the jar and **Fabric API** for 26.3 in your `mods` folder. It is needed on both the client and the server.

```
# Windows
.\gradlew.bat build
# macOS / Linux
./gradlew build
```

The jar ends up in `build/libs/nexium-<version>.jar` (not the `-sources` one). Any Java 17+ starts the build; Gradle
fetches JDK 25 itself. `./gradlew runGameTest` runs the in-game tests on a headless server and
`./gradlew runClientGameTest` runs the client tests (they need a window or Xvfb). Config lives in
`config/nexusessentials.json`.

## Storage

### Drawers
* 1x1 holds **54 stacks**, 1x2 holds **27 stacks** per slot, 2x2 holds **13.5 stacks** per slot.
* **Recipes** (planks P and slabs S of one wood, in all 13 woods): 1x1 `PSP / S S / PSP` makes 1, 1x2 `PSP / PPP / PSP`
  makes 2, 2x2 `SPS / PPP / SPS` makes 4.
* Right-click the front to insert, double-click to insert everything matching, left-click to take a stack (sneak for
  one item). Break drawers from the side.
* A bar under each count shows how full the section is, counting upgrades: green under 75%, yellow to 90%, red above.
* A **comparator** reads how full a drawer is, like a chest: 0 when empty, 15 when full. A drawer with 2 or 4 sections
  reads their average. Fluid and Compacting Drawers do the same.
* Right-click with an upgrade to install it; right-click the side or top with an empty hand to see or remove
  upgrades. Capacity upgrades add together: Iron x2, Gold x4, Diamond x8, Emerald x16, Netherite x32. A higher
  capacity upgrade swaps out the weakest one. The Lock Upgrade locks the contents (padlock on the front) and the
  Void Upgrade destroys overflow.
* **Fluid Drawers** hold 32 buckets of one fluid. **Compacting Drawers** hold nuggets/ingots/blocks (any reversible
  9:1 or 4:1 recipe), 54 stacks of the largest form.
* Breaking a drawer keeps its contents and upgrades in the item, like a shulker box.
* **Drawer Controller**: drawers touching it or each other within **12 blocks** form a cluster. Right-click to send
  items into the drawer already holding them; double-click dumps your inventory. With an empty hand, right-click shows
  how many drawers it controls; **sneak + right-click** outlines its reach for a minute (again to hide). **Controller
  Slave** extends the reach, and slaves can chain.

### Barrels
**Iron, Gold, Diamond, Emerald and Netherite Barrels** hold 45, 66, 84, 102 and 120 slots. Right-click a barrel with
the matching **Barrel Upgrade** to upgrade it in place (Iron upgrades a vanilla barrel, Gold an Iron Barrel, and so
on). What is inside stays inside.

### Simple network
* **Storage Connector** reads every inventory touching it; **Filtered Connector** reads only the one it faces,
  through a filter. Both have a priority: items go into the highest priority inventory that takes them, even if a lower
  one already holds some, and come out of the lowest first. At the same priority, items join their own kind first. A Drawer
  Controller behind a connector only fills drawers that already hold the item or are locked to it. **Inventory Cable**
  links connectors and terminals. No power.
* **Import/Export Bus**: placed on an inventory or tank, **Import** pulls items and fluids into the network and
  **Export** pushes them out (32 items or a bucket every half second). An optional filter limits what moves.
* **Level Emitter**: a redstone signal while the network holds at least (or fewer than) an amount of one item or fluid.
* **Crafting Terminal**: items and fluids in one grid with search (`@modname` filters by mod) and sorting, a network
  fill bar, the vanilla recipe book, and a red **X** that empties the crafting grid. JEI / REI "+" fills the grid from
  the network; when something is missing, JEI marks the missing slots in red and REI lists them in the button's tooltip.
  **Right-click the search box** (here, on the recipe book's, and in the Codex) to clear it. The terminal's search, the recipe book's and JEI's (or REI's) stay the same: typing in either changes both (`terminalSearchSync`, client,
  turns it off). Neither mod is required.

### Inventory sorting

Press **R** (`Sort Inventory` under Nexium in Controls) with the mouse over a container's slots to sort that container,
or over the main inventory to sort that: only the area under the mouse, never both, and never the hotbar. Chests,
barrels, ender chests, shulker boxes, hoppers, dispensers, droppers and Nexium barrels sort. Stacks are merged first;
shulker boxes and bundles go to the bottom-right slots. Pressing R again over the same area steps through
**A to Z**, **creative menu order** and **biggest stacks first**. With JEI, REI or EMI installed, R over an item still
shows its recipes: sort with the mouse over an empty slot or a gap between slots.

### Disk Drive and storage disks
Digital storage for the network, counted like Refined Storage: a disk holds a number of items, of any kinds.

* **Disk Drive** (`IGI / R R / IRI`: iron, glass, redstone): a network block that holds **8 disks**. Place it against a
  cable, connector or terminal. Right-click it for its screen, or right-click it holding a disk to put the disk in. Each
  bay's light is green (room), yellow (over 75% full) or red (full).
* **Item disks**: 16k (16,000 items), 64k, 256k, 1M and Nexium (4,000,000). **Fluid disks**: 64, 256, 1,024, 4,096 and
  16,384 buckets. What a disk holds is saved with the world under the disk's id, so it stays with the disk when it is
  moved, dropped or put in another drive; the tooltip shows how full it is.
* **Making disks**: a **Disk Casing** (`GRG / G G / III`, makes 2) and a **Storage Part**. A 16k part is
  `RCR / CLC / RCR` (redstone, copper, lapis; a bucket in the middle for a fluid part). Three parts plus a gold ingot,
  diamond, emerald or Nexium ingot make the next size. Sneak + right-click an empty disk to take it apart again.
* **Priority**: drives start at **10**, above chests (0), so new items go to disks first. The screen has a priority
  (shift-click for steps of 10) and a filter slot for a Basic or Advanced Filter.
* **Move to disks** (in the drive's screen) moves everything the network's chests and barrels hold onto the disks, as
  far as they have room. Drawers keep their items.
* No power and no channels, like the rest of the network.

### Tablets
Sneak + right-click a Crafting Terminal with a **Crafting Storage Tablet** to link it, with no range limit and in any
dimension, as long as the terminal's chunk is loaded. A **Dimensional Anchor** keeps the 3x3 chunks around it loaded;
place it or right-click it to outline those chunks for a minute.

### Dimensional Input / Output
* Click an Input with a **Dimensional Link Card**, then click Outputs. Each link has a name (rename the card in an
  anvil first, or rename later in the block's screen).
* Inputs take items and fluids. Outputs push into everything around them on the sides you turn on, and can have a
  filter so one Input splits items by type. A Transfer Speed Upgrade moves 16 items (1) or full stacks (2).

### Conduits
* Item and Fluid conduits (Tier 1 and 2) share one block. Fluid: 100 mB/tick (Tier 1), 2000 mB/tick (Tier 2).
* Every side has an **insert** colour and an **extract** colour; what a side extracts goes to insert sides of the
  same colour.
* **Wrench**: right-click a side to cycle its mode, sneak + right-click for the full settings (colours, redstone
  control, priority, round-robin, filter, speed upgrades). Right-click a conduit with a full block to give it a
  facade.
* **Fluid Intake** pulls from the source blocks touching it. An **Infinite Lava Source** (obsidian, lava buckets and a
  nether star) next to it gives endless lava.
* **Basic Filter** (whitelist/blacklist) and **Advanced Filter** (by item tag).

## Tools and Nexium

### All-in-one tools (AIOT)
* A pickaxe, axe, shovel, hoe and sword in one item: **Stone, Copper, Iron, Golden, Diamond, Netherite and Nexium AIOT**.
* **Crafting**: all five tools of one material anywhere in the crafting grid. Enchantments carry over at their highest
  level, Silk Touch and Fortune both stay (one active, one inactive), the best of Sharpness / Smite / Bane of
  Arthropods stays, curses stay, and the AIOT lasts 1.5 times as long as the five tools together (five fresh tools
  make a full AIOT; worn ones a worn one).
* **Netherite**: at a smithing table, a Diamond AIOT, a netherite upgrade template and a netherite block.
  **Nexium**: a Netherite AIOT, a template and a Block of Nexium.

  | | Stone | Copper | Iron | Golden | Diamond | Netherite | Nexium |
  |---|---|---|---|---|---|---|---|
  | Durability | 983 | 1,425 | 1,875 | 240 | 11,708 | 15,233 | 18,750 |
  | Attack damage | 9 | 9 | 9 | 7 | 9 | 10 | 11 |

* **Combat**: as hard as that material's axe at a sword's speed, with the sword's sweeping attack, and it disables
  shields.
* **Mining**: everything that material's pickaxe, axe, shovel, hoe and sword mine, as fast as the best of them, with
  the same material limits. It takes every tool and sword enchantment.
* **Right-click** strips logs, scrapes or unwaxes copper and makes paths; **sneak + right-click** tills farmland.
* At an **anvil**, an AIOT holding one of Silk Touch and Fortune takes the other from a book, added inactive.

### Tool menu
* Hold an AIOT or Nexium tool and press **G** (rebind under Controls > Key Binds > Nexium).
* **Mining**: on an AIOT with both Silk Touch and Fortune, switches which is active. The gem shows it: **green for
  Fortune, blue for Silk Touch**.
* **Haste II**: turns a Nexium tool's Haste II on or off. The setting is saved on the tool.
* **Vein size**: steps how many blocks one vein mining swing mines with this AIOT (4, 9, 18, 32 ... up to `aiotVeinMaxBlocks`).
  Vein mining needs no module.
* **Tunnel**: steps how many 3x3 layers deep one 3x3 Mining swing digs, away from you: off, 2, 4, 8, 16, 24, 32 (up to
  `aiotTunnelMaxLength`). Press **Z** (rebindable) to step it without opening the menu.
* **Modules** (AIOTs only): two slots. Put a module in like any item; the button beside each slot turns it on or off.
* The **Armor** button goes to the armor menu, and its **Tools** button comes back.

### AIOT modules
| Module | Recipe | What it does |
|---|---|---|
| **Nexus Link** | Dimensional Link Card in the middle, ender pearls on the sides, iron ingots in the corners | What the AIOT mines goes straight into a storage network |
| **3x3 Mining** | Diamond in the middle, redstone on the sides, iron ingots in the corners | Mines the 3x3 around each block |

* **Nexus Link**: sneak + right-click a Crafting Terminal with the module (or an AIOT that has one) to link it. Drops
  go into that terminal's network in any dimension; what the network can't take drops as usual.
* **3x3 Mining**: mines the 3x3 around the block, facing you. Press **X** (rebind under Controls > Key Binds >
  Nexium) to switch it on or off for the AIOT in your hand. With a **tunnel length** set in the tool menu it digs that
  many 3x3 layers going away from you, one swing, with the same rules for every block. Only blocks the AIOT is made for, at most about twice as
  hard as the block you mined, and never blocks with contents such as chests.
* **Vein mining** (no module): mining any block, stone included, mines every block of the same kind touching it, up to
  the size set in the tool menu. Grasses, ferns, bushes, dead bushes, azaleas and leaf litter (the
  `#storagenexus:vein_plants` tag) count as one kind, so a mixed patch clears in one swing, and cost no durability.
  Blocks with contents, such as chests, are never vein mined, and the tool must be right for the block (obsidian needs a
  diamond-grade AIOT). A data pack can list blocks to leave out in `#storagenexus:vein_excluded` (empty by default).
  **Hold the Vein Mine key** (**V**) while mining; without it, you mine one block.
* **The old Vein Mining Module** is retired. It stays in the game only so that AIOTs and chests that hold one still load:
  it can't be crafted, right-clicking it gives back its materials (4 iron ingots, 4 lapis lazuli and a diamond), and so
  does opening the tool menu of an AIOT that still has one installed.
* With an AIOT in hand, a **faint pale green outline** shows the blocks your next swing would take along with the one you
  look at (the rest of a vein while the key is held, the 3x3 or the tunnel). Sneaking mines one block, so nothing is
  outlined. It uses the same rules as the mining, and shows for 20 seconds after you switch to an AIOT or press the 3x3
  (X), tunnel (Z) or vein (V) key, and while V is held, like the readout below. `aiotMiningPreview` (client) turns it off.
* A small readout in the bottom right corner, by the hotbar, shows what the AIOT in your hand is set to: Silk Touch or
  Fortune (when it holds both) the vein size and each module's state (3x3 on, tunnel depth, Nexus Link). It shows for 20
  seconds after you switch to an AIOT or press the 3x3 (X), tunnel (Z) or vein (V) key, stays up while V is held, then fades
  out. `aiotHudSeconds` (client) changes the time, 0 keeps it up; `aiotHud` turns it off.
* Each block costs 1 durability, Fortune, Silk Touch and the Nexus Link apply, and spawn protection is respected.
  **Sneak** to mine just one block.
* If an AIOT breaks, the modules in it drop at your feet (on/off setting and Nexus Link terminal kept).

### Construction Builder
Builds out from the block you right-click: every connected block of that kind gets a new block on the face you clicked.
It places the block in your **offhand**, or else a copy of the one you clicked (facing the same way). Each one is a
freshly placed block: a crop is planted, not grown, and candles or snow layers are placed one at a time. A double slab
is placed as one and uses two slabs.

* **Tiers**: Iron places up to **9** blocks a click, Diamond **27**, Netherite **64**, Nexium **128** (and never breaks).
  Each block costs 1 durability. Iron: `PLP / IGI / I I` (paper, lapis lazuli, iron ingots, a gold nugget). Add a
  **diamond** in a crafting grid for Diamond; at a smithing table with a netherite upgrade template, a **netherite ingot**
  makes Netherite and a **Nexium ingot** makes Nexium. Upgrades keep its settings and module.
* **Modes**: **Extend** (the whole connected face), **Line** (one row, along the way you look) and **Exchange** (swaps
  the clicked surface for your offhand block and gives you what the old blocks drop when broken, so stone comes back
  as cobblestone and glass as nothing; never containers or unbreakable blocks).
* A **pale blue outline** shows where the blocks will go before you click; `builderPreview` (client) turns it off. The
  readout by the hotbar shows its mode, plane and size.
* **Tool menu (G)**: Mode; **Plane** (any, flat or upright: which way it may spread); **Size** (steps down from the tier's
  limit); **Undo**, which takes back your last build and returns its blocks (the last 2 builds, until you log out; blocks
  changed since are left alone); and a slot for a **Nexus Link Module** with its on/off switch.
* Blocks come from your inventory first, then from the storage network of its Nexus Link: sneak + right-click a
  Crafting Terminal with the builder to link it. Creative players need no blocks. Doors, beds and other two-part blocks
  are left alone.
* If a builder breaks, the module in it drops at your feet.

### Nexium
* **Nexium Ingot**: a diamond, an emerald, a netherite ingot and a netherite upgrade template in a crafting table make
  **3 ingots**. Nine make a **Block of Nexium** (a beacon base).
* **Gear**: at a smithing table, a netherite upgrade template, the netherite piece and a Nexium ingot. There is a
  Nexium **sword, pickaxe, axe, shovel, hoe, spear, helmet, chestplate, leggings and boots**, and the Nexium AIOT.
  Everything Nexium survives fire and lava.
* **Tools**: netherite's mining speed and level, 2,500 uses, one more attack damage than netherite.
* **Haste II**: Nexium mining tools give the real **Haste II** effect while held, so with Efficiency V they mine as
  fast as netherite with Efficiency V under a Haste II beacon. It goes as soon as the tool is put away.
* **Armor**: 24 armor and 20 toughness in all, 0.15 knockback resistance per piece. It takes armor trims and keeps
  piglins calm.

### Nexium armor bonuses
| Bonus | Needs | What it gives |
|---|---|---|
| **Nether-forged** | full set | Fire Resistance: no fire or lava damage |
| **Miner's set** | full set | No mining slowdown underwater or off the ground |
| **No knockback** | full set | Hits and explosions don't push you back |
| **Nexus restock** | full set, linked chestplate | Hotbar stacks top up from a storage network; you eat from it when hungry |
| **Night vision** | helmet | Night Vision |

* **Armor menu**: press **G** with no AIOT or Nexium tool in hand, or click **Armor** in the tool menu. Each bonus has
  an on/off switch, saved on the piece that carries it.
* **Nexus restock**: link the chestplate by sneak + right-clicking a Crafting Terminal with it. A hotbar stack you use
  is topped up from the network at 8, or refilled when you use the last one. When your hunger bar is down by 6 or
  more you eat the most filling plain food in the network.

## Essentials

### Explosions and endermen
Creeper and ghast explosions break no blocks and light no fires (they still hurt), and spare item frames, paintings,
armor stands, dropped items, minecarts and boats. TNT, beds, respawn anchors, withers and end crystals work as in
vanilla. Endermen never pick up blocks.

### Trinket, wings, arrow and totem slots
* Four slots that use no inventory space. Above the offhand slot: the **trinket slot** (the magnet) and the **wings
  slot** (an elytra, or any item that glides like one). Under the crafting grid: the **arrow slot** (up to 64 arrows
  of any kind) and the **totem slot** (a Totem of Undying or any item with death protection).
* Shift-click an item to send it to its slot.
* **Arrows**: bows and crossbows shoot from the arrow slot after anything held in a hand and before arrows in your
  inventory. Arrows you pick up fill the slot first.
* **Totem**: if you would die with no totem in either hand, the one in the totem slot saves you.
* An elytra in the wings slot glides like one in the chest slot, so you can wear a chestplate too. Unbreaking and
  Mending work on it, and its protection enchantments add to your armor's.
* The slots are saved with your player and drop on death like the rest of your inventory (unless keepInventory).

### Magnet
* Crafted from 5 iron ingots, 1 redstone and 1 lapis lazuli:
  ```
  R . L
  I . I
  I I I
  ```
* Works while it is in your **trinket slot**. It picks up every item and experience orb in range, through walls.
  **Sneak + right-click** it to change the range (4, 8, 12, 16, up to `magnetMaxRange`). Press **M** to switch it on
  or off.

### Elytra enchanting
At an anvil, elytra take Protection, Fire, Blast and Projectile Protection from books, all four at once (every armor piece does now).

### Anvil: never Too Expensive
An anvil never says **Too Expensive!**: any job can be done, for its **full cost** in levels (the screen shows it, say
"Enchantment Cost: 68"), as long as you have the levels. `anvilNoCostLimit` set to false brings vanilla's 40-level limit back.

### Enchantment levels and exclusions
* **Level V**: Protection, Fire Protection, Blast Protection, Projectile Protection, Unbreaking, Looting, Fortune and Respiration
  go up to V in the enchanting table, at an anvil (two IV books make a V), in loot chests and from villagers.
* **Together**: all four protections on one piece, Mending with Infinity, and Sharpness with Smite. Bane of Arthropods still
  excludes both, and Fortune still excludes Silk Touch.
* This is done with data files (`data/minecraft/enchantment`), so a data pack can change it.

### Master Librarian
At a **Farm Assembler**, pick **Master Librarian**, put in **12 villagers** (spawn eggs or Captured Mobs) and press Build. The
villagers are used up and a **Master Librarian** comes out as a Captured Mob: right-click a block to let him out.

* He sells an **enchanted book of every enchantment at its highest level**: Mending and the other treasure enchantments
  too, level V ones included, but no curses. A trade takes emeralds and a book, at the villagers' own prices: 2 emeralds plus
  up to 5 + 3 per level more, doubled for treasure enchantments, never above 64. Each trade has the usual 12 uses.
* He is level 5 with every trade unlocked. A librarian made this way doesn't count as a villager for any recipe, so it can't
  be fed back in.
* `librarianVillagers` changes how many villagers he takes.

### Elevator

A flat Nexium platform, a quarter of a block tall. **Craft:** polished blackstone across the top, iron ingots at the
sides and bottom corners, a block of redstone in the middle (makes 2).

* Put elevators at different heights **in the same column** (same X and Z), anywhere from the bottom of the world to the top.
* **Right-click** one for the list of floors, highest first (scroll with the mouse wheel), and click a floor to go there.
  You land on the platform, with no fall damage and no leftover speed.
* **Name a floor** in the box at the top of the screen and press Name; an empty name goes back to "Floor 1, 2, 3", counted
  from the bottom. Anyone can rename. Breaking an elevator forgets its name.
* A floor with **no room to stand** (the two blocks above it are blocked) is greyed out and cannot be used.
* The column is always inside one chunk, so every floor is loaded whenever the one you stand on is. Same dimension only.

### Piglins
Any piece of netherite armor counts as gold armor: piglins stay neutral.

### Elixir of Bedrock Breaking
* Crafted from 2 diamonds, 2 obsidian and a glass bottle:
  ```
  . O .
  D B D
  . O .
  ```
* Gives **Bedrock Breaker** for 5 minutes: bedrock mines like obsidian with a diamond or netherite pickaxe, and drops
  nothing. Any bedrock can be broken, including the bottom layer above the void.

### Picking up mobs
* **Sneak + right-click a mob with an empty hand** to pick it up. You get a Captured Mob item (a glinting spawn egg)
  that keeps everything about the mob. **Right-click a block** with it to let the mob out.
* The Ender Dragon and the Wither can't be picked up, and only a pet's owner can pick it up. The **Warden** and the
  **Elder Guardian** cost **10 experience levels** (`bossPickUpLevels`; other mods' bosses in `#c:bosses` too, and a
  data pack can add mobs to `#nexusessentials:pick_up_costs_levels`).

### Right-click harvest
**Right-click a fully grown crop** (wheat, carrots, potatoes, beetroot, nether wart, cocoa) to harvest it: it drops as if
broken, less the one seed it is replanted with. **Hold V** (the vein mining key) to harvest the
whole connected field at once, up to `harvestVeinMax` crops. Sneak to use an item on the crop instead. `rightClickHarvest`
turns it off.

### Ancient debris
Ancient debris drops **netherite scrap** instead of itself: 2 per block, multiplied by Fortune like diamond ore. Silk
Touch still drops the block.

### Farm Assembler
Makes the one-block farms below and the Master Librarian. **Recipe**: a crafting table in the middle, 4 iron ingots on the
sides and 4 lapis lazuli in the corners (`LIL / ICI / LIL`).

* Right-click it, pick what to make from the list on the left, and put in what it needs. The right side shows how much of
  each material is in, with a bar and a tick for each line that is done.
* **Add from inventory** moves everything it still needs out of your inventory. Shift-clicking items, or hoppers and pipes
  above or beside it, add them too. It only takes what is still needed, in any mix of kinds and in any order.
* **Build** makes it once everything is in; take it from the slot next to the button. Picking something else hands back
  what was in, and breaking the Assembler drops it.
* The farms cost what their old renamed double chests did: the materials plus the filler block the chest's other slots
  held, as listed below.

### Iron Farm
* **Making one**: at a Farm Assembler, pick **Iron Farm** and put in:

  | Material | Amount | Counts |
  |---|---|---|
  | Zombies | 4 | Zombie, husk, drowned or zombie villager: spawn eggs or Captured Mobs |
  | Villagers | 12 | Villager spawn eggs or Captured Mob villagers |
  | Torches | 33 | Torch, soul torch or copper torch |
  | Slabs | 20 | Any slab |
  | Signs | 16 | Any sign or hanging sign |
  | Trapdoors | 12 | Any trapdoor |
  | Beds | 12 | Any colour |
  | Smooth Stone | 1,408 | Smooth stone |

  Then press Build.
* **Place it anywhere.** Every 7.5 seconds it makes a golem's drops: 3-5 iron, about **1,900 iron an
  hour**. Hoppers and pipes take the output from below.
* **Bread**: bread in the bread slot makes the farm **10x faster**, eating one loaf every 30 seconds. When the last
  loaf is eaten the slot locks for 10x as long as the boost lasted, and the farm keeps working at normal speed.
* A full output or a **redstone signal** pauses the farm. Breaking it keeps its boost and cooldown in the item.
* A **comparator** reads how full the output is, and 15 once the farm has stopped for lack of room.

### More one-block farms
Made at the Farm Assembler like the Iron Farm (mobs can be spawn eggs or Captured Mobs). Place it anywhere; hoppers and pipes
take the output from below, a full output or a redstone signal pauses it, and breaking it keeps its progress in the item.
A comparator reads how full the output is, and 15 once the farm has stopped for lack of room.

| Farm | Recipe | Makes |
|---|---|---|
| **Villager Breeder** | 2 villagers, 4 beds, 2 composters, 8 hay bales, 2 emerald blocks, 2,880 planks | A baby villager (a Captured Mob) every 10 minutes. Feed it 3 bread, or 12 carrots, potatoes or beetroot, each |
| **Piglin Barter Farm** | 2 piglins, 9 gold blocks, 16 soul sand, 16 planks, 3,136 netherrack | Trades 1 gold ingot every 6 seconds for the vanilla piglin barter loot; hoppers feed the gold from the top or sides |
| **Zombified Piglin Farm** | 4 zombified piglins, 8 magma blocks, 20 nether bricks, 16 gold ingots, 3,008 netherrack | Every 4 seconds: gold nuggets (0-6, about 3) and a 7.5% gold ingot, like Looting V with no player needed. No rotten flesh |
| **Wither Skeleton Farm** | 4 wither skeletons, 16 nether brick fences, 8 soul sand, 4 coal blocks, 4 magma blocks, 2,944 netherrack | Every 6 seconds: 0-1 coal, 0-2 bones and a 7.5% wither skeleton skull |
| **Shulker Farm** | 4 shulkers, 32 purpur blocks, 8 end rods, 8 ender pearls, 3,008 end stone bricks | Every 30 seconds: a shulker shell about 4 times in 5 (81%) |
| **Witch Farm** | 2 witches, 8 cauldrons, 16 spruce planks, 8 lily pads, 768 mossy cobblestone | Every 10 seconds: a witch's own drops, as if a player killed her (no Looting): glowstone dust, redstone, gunpowder, sugar, spider eyes, sticks and glass bottles |
| **Sugar Cane Farm** | 32 sugar cane, 4 water buckets, 8 pistons, 16 observers, 768 sand | 3 sugar cane every 20 seconds |
| **Bamboo Farm** | 32 bamboo, 4 water buckets, 8 pistons, 16 observers, 768 dirt | 4 bamboo every 20 seconds |
| **Nether Wart Farm** | 32 nether wart, 16 soul sand, 4 lava buckets, 8 magma blocks, 768 netherrack | 4 nether wart every 30 seconds |

The Farm Assembler's list scrolls (mouse wheel) now that it has more than seven targets. The newer farms ask a
quarter of the filler block the first five do (768 of one block).

**More farms** (768 of their filler block unless the recipe says otherwise; mobs are spawn eggs or Captured Mobs):

| Farm | Recipe | Makes |
|---|---|---|
| **Monster Farm** | 1 zombie, 1 pillager, 8 hoppers, 16 iron bars, 8 pistons, 4 emerald blocks, cobblestone | Every 10 s: the real loot of a zombie, skeleton, spider, creeper, pillager, vindicator and evoker, as if a player killed each (no Looting, rare player-kill drops included), all mixed. The evoker's Totem of Undying is a 2% find each cycle instead of every time |
| **Nether Mob Farm** | 1 blaze, 1 ghast, 1 magma cube, 16 nether bricks, 4 lava buckets, 8 magma blocks, netherrack | Every 10 s: blaze rods, ghast tears, gunpowder, magma cream |
| **Ocean Farm** | 1 guardian, 1 drowned, 1 cod, 16 prismarine bricks, 8 sea lanterns, 4 water buckets, sand | Every 10 s: prismarine shards and crystals, fish, rotten flesh, copper |
| **Slime Farm** | 2 slimes, 4 slime blocks, 8 sticky pistons, 8 hoppers, mud | Every 10 s: slime balls |
| **Livestock Farm** | 1 each of cow, pig, sheep, chicken, rabbit, 16 hay bales, 16 oak fences, dirt | Every 20 s, for 1 feed item: wheat gives cow and sheep drops (random wool colours), seeds chicken drops and an egg, carrots pig and rabbit drops, potatoes or beetroot pig drops |
| **Crop Farm** | 32 each of wheat, carrots, potatoes, beetroot, 4 water buckets, dirt | Every 20 s: 3 of each crop and seeds. Bone meal in its slot doubles the speed (one lasts 3 cycles) |
| **Gourd and Cactus Farm** | 16 each of melons, pumpkins, cactus, kelp, 4 water buckets, sand | Every 20 s, all at once: 12 melon slices, 3 pumpkins, 4 cactus and 6 kelp |
| **Generator** | 4 lava buckets, 4 water buckets, 8 hoppers, 2 diamond pickaxes, cobblestone | Every 10 s, by its mode button: 8 cobblestone, stone, gravel or sand, 4 clay, 2 basalt; obsidian 1 every 20 s |
| **Bee Farm** | 2 bees, 4 beehives, 4 campfires, 16 flowers, 8 glass bottles, oak planks | Every 20 s: 1 to 3 honeycomb, and 2 bottles of honey, both into its output slots |
| **Froglight Farm** | 3 temperate, 3 warm and 3 cold frogs and 9 slimes (Captured Mobs), 256 glass of any colour | Every 20 s: 1 ochre, 1 verdant and 1 pearlescent froglight |
| **Moss Farm** | 32 moss blocks, 8 azaleas, 4 water buckets, 8 bone blocks, clay | Every 20 s: 3 moss blocks, 2 moss carpet, 3 pale moss blocks, 2 pale moss carpet, 2 pale hanging moss, 6 resin clumps, a 10% azalea. Needs bone meal (one lasts 3 cycles) |

### Freezer
* **Recipe**: 3 packed ice, 5 iron ingots and a cauldron (`PPP / ICI / III`).
* **Fill it with water** with a water bucket or a pipe (fluid conduit, Dimensional Output). The tank holds 8 buckets and
  only takes water. It freezes **ice** (1 bucket, 5 seconds), and its button switches it to **packed ice** (9 buckets,
  45 seconds) or **blue ice** (81 buckets, 90 seconds). The water is used up steadily while it works, so a tank that runs
  dry waits for more. Hoppers and pipes take the ice from below; a full output or a redstone signal pauses it.
* **Dyeable like leather armor**: put it in the crafting grid with dyes (several mix). The body and frame take the colour;
  the frost, glow and water don't. A cauldron of water washes it. It keeps its colour, water and mode when broken.
* **Ice recipes**: 9 ice make packed ice and 9 packed ice make blue ice (vanilla); packed ice also crafts into 9 ice and
  blue ice into 9 packed ice.
* **Soul sand to sand**: a Blast Furnace smelts soul sand into sand, with no experience, at an ore's speed (5 seconds).

## Settings

`config/nexusessentials.json` is created on first launch. Changes take effect after a restart. On a server, the
server's file is the one that counts.

| Setting | Default | What it does |
|---|---|---|
| `creepersBreakBlocks` | `false` | Creeper explosions break blocks (vanilla) |
| `ghastFireballsBreakBlocks` | `false` | Ghast fireballs break blocks and start fires (vanilla) |
| `explosionsSpareDecorations` | `true` | Explosions spare item frames, paintings, armor stands, dropped items, minecarts and boats |
| `endermenPickUpBlocks` | `false` | Endermen pick up blocks (vanilla) |
| `magnetMaxRange` | `16` | Largest magnet range, 4 to 64 blocks |
| `magnetPullsXp` | `true` | Magnets pull experience orbs too |
| `librarianVillagers` | `12` | How many villagers the Master Librarian takes at the Farm Assembler, 1 to 54 |
| `elytraProtectionEnchantments` | `true` | Elytra accept all four protection enchantments |
| `netheriteArmorCalmsPiglins` | `true` | Netherite armor keeps piglins calm |
| `bedrockBreakerSeconds` | `300` | How long the elixir lasts |
| `mobPickUp` | `true` | Sneak + right-click picks up mobs |
| `onlyOwnersPickUpPets` | `true` | Only a pet's owner can pick it up |
| `bossPickUpLevels` | `10` | Experience levels it costs to pick up a boss (the Warden, the Elder Guardian); 0 makes them free |
| `ancientDebrisDropsScrap` | `true` | Ancient debris drops Fortune-multiplied netherite scrap |
| `rightClickHarvest` | `true` | Right-click a grown crop to harvest and replant it |
| `harvestVeinMax` | `128` | Most crops one harvest takes with V held, 1 to 1024 |
| `anvilNoCostLimit` | `true` | An anvil is never Too Expensive: any job, for its full cost in levels; false keeps vanilla's limit |
| `ironFarmGolemTicks` | `150` | Ticks between golems (150 = 7.5 s) |
| `ironFarmBreadSpeedUp` | `10` | How many times faster bread makes the farm |
| `ironFarmBreadSeconds` | `30` | Seconds of boost per loaf |
| `ironFarmCooldownMultiplier` | `10` | Bread cooldown, as a multiple of how long the boost lasted |
| `villagerBreederTicks`, `piglinBarterTicks`, `zombifiedPiglinTicks`, `witherSkeletonTicks`, `shulkerFarmTicks`, `witchFarmTicks`, `sugarCaneFarmTicks`, `bambooFarmTicks`, `netherWartFarmTicks`, `monsterFarmTicks`, `netherMobFarmTicks`, `oceanFarmTicks`, `slimeFarmTicks`, `livestockFarmTicks`, `cropFarmTicks`, `gourdFarmTicks`, `generatorFarmTicks`, `beeFarmTicks`, `mossFarmTicks`, `froglightFarmTicks` | `12000`, `120`, `80`, `120`, `600`, `200`, `400`, `400`, `600`, `200`, `200`, `200`, `200`, `400`, `400`, `400`, `200`, `400`, `400`, `400` | Ticks per cycle of each farm (20 ticks a second), 1 to 1,000,000 |
| `freezerIceTicks`, `freezerPackedIceTicks`, `freezerBlueIceTicks` | `100`, `900`, `1800` | Ticks the Freezer takes for 1 ice, 1 packed ice and 1 blue ice |
| `aiotVeinMining` | `true` | Vein mining works |
| `aiotVeinMaxBlocks` | `256` | Most blocks one vein mining swing takes, 1 to 512 |
| `aiotAreaMining` | `true` | The 3x3 Mining module works |
| `aiotTunnelMaxLength` | `32` | Longest tunnel the 3x3 module can dig, in layers, 1 to 64 |
| `aiotMiningPreview` | `true` | Client: outline the blocks an AIOT swing would mine |
| `builderPreview` | `true` | Client: outline where the Construction Builder in your hand would put blocks |
| `aiotHud` | `true` | Client: show the held AIOT's mode and module states in the bottom right corner |
| `terminalSearchSync` | `true` | Client: the terminals' search, the recipe book's and JEI's or REI's stay the same |
| `aiotHudSeconds` | `20` | Client: seconds that readout and the mining outline stay up after switching to an AIOT or pressing X, Z or V; 0 keeps them up |
| `aiotNexusLink` | `true` | The Nexus Link module works |
| `nexusRestock` | `true` | The Nexium armor's Nexus restock bonus works |
| `restockTopUpAt` | `8` | A hotbar stack being used up is topped up when it gets down to this many |
| `restockEatWhenMissing` | `6` | The wearer eats from the network when the hunger bar is down by this much |
