# Nexium (Fabric, Minecraft 26.3)

Drawers, a storage network with crafting terminals, cross-dimensional tablets and item links, item and fluid
conduits, all-in-one tools (AIOTs), **Nexium** (a tier above netherite) and a set of quality-of-life **Essentials**
(no creeper or ghast block damage, an item and XP magnet, wings, arrow and totem slots, Fortune on ancient debris,
a 1-block iron farm and more).

Every item is in one of three **Nexium** creative tabs (Storage; Tools; Farms & Power), and every tooltip says what its item does.

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
`./gradlew runClientGameTest` runs the client tests (they need a window or Xvfb); set `NEXIUM_SHARD=i/n` to run only every n-th
test starting at i (CI runs three shares side by side with `0/3`, `1/3` and `2/3`). Config lives in
`config/nexium.json`.

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
  capacity upgrade swaps out the weakest one. The Void Upgrade destroys overflow. The **Drawer Key** (`SGS / GTG / SGS`:
  sticks, gold nuggets, a tripwire hook) locks a drawer: right-click any drawer, compacting drawer or fluid drawer with it to lock it
  and again to unlock it. It is never used up, any key fits any drawer, and a locked drawer keeps each slot reserved for its item
  (padlock on the front). An older Lock Upgrade is a Drawer Key now: a drawer that held one stays locked and drops the key.
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
* **Storage Connector** reads every storage inventory touching it; **Filtered Connector** reads only the one it faces,
  through a filter. **They do not connect to machines**: furnaces, blast furnaces, smokers, brewing stands, hoppers, dispensers,
  droppers, crafters and Nexium's own machines (farms, the Farm Assembler, generators, batteries, the Reactor, the Freezer, the
  Nexus Furnace...) are skipped, because a machine has a different slot behind each face and a connector would see only one of them.
  Feed a machine with an **Import/Export Bus** on the face you want instead. What is refused is the `connectorBlacklist` setting
  (block ids and `#tags`; it starts with the machines above and `#nexium:machines`): take an entry off to connect to that block.
  Both have a priority: items go into the highest priority inventory that takes them, even if a lower
  one already holds some, and come out of the lowest first. At the same priority, items join their own kind first. A Drawer
  Controller behind a connector only fills drawers that already hold the item or are locked to it. **Inventory Cable**
  links connectors and terminals. No power.
* **Import/Export Bus**: placed on an inventory or tank, **Import** pulls items and fluids into the network and
  **Export** pushes them out (32 items or a bucket every half second). An optional filter limits what moves. It faces the
  block it was placed against; to change that, open its screen and press one of the six side buttons (D, U, N, S, W, E).
  Only sides with an inventory or tank can be chosen, the others are greyed out, and the side it faces now is framed.
* **Level Emitter**: a redstone signal while the network holds at least (or fewer than) an amount of one item or fluid.
* **Crafting Terminal**: items and fluids in one grid with search (`@modname` filters by mod) and sorting (A to Z to start with; the button cycles name, count and mod), a network
  fill bar, the vanilla recipe book, and a red **X** that empties the crafting grid. **Shift-clicking the result** crafts
  until the network's ingredients run out, but at most **2 stacks of the result per click** (click again for more), so one
  click cannot empty the network. JEI / REI "+" fills the grid from
  the network; when something is missing, JEI marks the missing slots in red and REI lists them in the button's tooltip.
  **Right-click the search box** (here, on the recipe book's, and in the Codex) to clear it. The terminal's search, the recipe book's and JEI's (or REI's) stay the same: typing in either changes both (`terminalSearchSync`, client,
  turns it off). Neither mod is required.

### Inventory sorting

Press **O** (`Sort Inventory` under Nexium in Controls) with the mouse over a container's slots to sort that container,
or over the main inventory to sort that: only the area under the mouse, never both, and never the hotbar. Chests,
barrels, ender chests, shulker boxes, hoppers, dispensers, droppers, Nexium barrels and the **Backpack's 36 slots** sort (its
module slots never do; stacks merge to 1,024; a Backpack opened from an inventory slot stays in
that slot when the inventory sorts). Stacks are merged first;
shulker boxes and bundles go to the bottom-right slots. Everything sorts **A to Z** (there are no other orders). It works with
the mouse over an item or an empty slot, and its key is separate from JEI's, REI's and EMI's (R shows recipes there as usual);
rebind it in Controls if you like.

### Transfer tweaks
With an empty cursor, in any chest-like screen (chests, barrels, hoppers, shulker boxes, Nexium barrels and drawers, farms, and
so on):

* **Hold Space and left click** an item and **every stack of that same item** in that inventory goes to the other one (the
  player's main inventory and hotbar count as one inventory), not just the stack you clicked.
* **Hold Shift, left click and drag** across slots and **every item the cursor passes over** goes to the other inventory. The
  first slot you press is the ordinary shift-click. In your own inventory screen this works between the **hotbar and the
  main inventory**, in both directions.
* **Scroll the wheel over a stack** to move **one item** at a time: **scroll down** sends one item of that stack to the
  other inventory, **scroll up** pulls one matching item in from it (the player's main inventory and hotbar are one
  side, the chest or machine the other). It works in chests, barrels and machines, not in your own inventory screen, the
  creative menu or the terminals, and each slot's own rules decide what fits. A **bundle** is never touched: over one the
  wheel keeps its vanilla job (choosing which item in it is shown).

Each stack moves with the ordinary shift-click, so where it lands is the screen's own rule. Plain shift + click keeps vanilla's
single stack. In the survival inventory screen only the shift + click and drag is added (Space + click is not); the creative
menu and crafting results keep vanilla's clicks.

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
* **Move up** (beside it) moves what the **lower tier disks in that drive** hold onto its **higher tier disks**: item disks onto
  higher item disks and fluid disks onto higher fluid disks, never one kind onto the other. Each disk's contents go to the biggest
  disk first and then the next biggest, as far as there is room, so swapping a 16k disk for a 64k one is one click; disks of the same
  tier are left alone, and what does not fit stays where it was.
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
* Every side has an **insert** colour and one or more **extract** colours; what a side extracts goes to insert sides
  of any of its extract colours. In the side's settings the **Out** row is a grid of the 16 colours: click one to switch
  it on or off (at least one stays on). A side has one filter, shared by all its colours.
* A conduit arm shows an **arrow** for what its side does: out to the block when it inserts, in to the conduit when it
  extracts, and a double arrow when it does both (whichever conduit types the arm carries). It sits in the room the colour
  rings leave on the arm, and is left out only when item, fluid and energy conduits all insert and extract on that one arm.
* **Wrench**: right-click a side to cycle its mode, sneak + right-click for the full settings (colours, redstone
  control, priority, round-robin, filter, speed upgrades). Right-click a conduit with a full block to give it a
  facade.
* **Fluid Intake** pulls from the source blocks touching it, up to **8 buckets a second**, and holds **32 buckets**. An **Infinite Lava Source** (obsidian, lava buckets and an
  eye of ender) next to it gives endless lava.
* **Basic Filter** (whitelist/blacklist) and **Advanced Filter** (by item tag). The Advanced Filter's **Tags** button opens
  a tag picker: search every item tag by name and tick any of them (up to 32), and its **Match** button adds
  **Smeltable** (anything a furnace, blast furnace or smoker takes) or **Furnace fuel**. Whitelist/blacklist applies to all of
  it. On a furnace, a Smeltable filter on the conduit's top side and a Fuel filter on another side let one extractor fill
  both the input and the fuel slot without clogging either.
* **Conduit Scanner** (spyglass, comparator, 2 iron ingots, redstone): right-click any conduit with it to see everything on
  that conduit network. For each block touching a conduit it shows the side, whether that side takes **Out**, puts **In** or
  both, its channel colours, and its priority and filter. There is a tab for each conduit type the network carries (Item,
  Fluid, Energy), and a button switches between a list grouped by channel colour and a flat list. A red `!` marks a dead end
  (taking out on a channel nothing inserts on, or the other way round); paused and redstone-disabled sides are flagged in the
  tooltip. Click a row to outline that block and its conduit in the world for about 10 seconds, and click again to remove it.

## Tools and Nexium

### All-in-one tools (AIOT)
* A pickaxe, axe, shovel, hoe and sword in one item: **Stone, Copper, Iron, Golden, Diamond, Netherite and Nexium AIOT**.
* **Crafting**: all five tools of one material anywhere in the crafting grid. Enchantments carry over at their highest
  level, Silk Touch and Fortune both stay (one active, one inactive), the best of Sharpness / Smite / Bane of
  Arthropods stays, curses stay, and the AIOT lasts 1.5 times as long as the five tools together (five fresh tools
  make a full AIOT; worn ones a worn one).
* **Netherite**: at a smithing table, a Diamond AIOT and a netherite block (leave the template slot empty).
  **Nexium**: a Netherite AIOT and a Block of Nexium.

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
* **Tunnel**: how many 3x3 layers deep one 3x3 Mining swing digs, away from you: off, 2, 3, 8, 16, 24, 32 (up to
  `aiotTunnelMaxLength`). Click it in the menu to step on (round to off after the deepest), or without opening the menu
  **hold Z** (rebindable) and **scroll the wheel**: up for a deeper tunnel, down for a shallower one. It stops at off and at
  the deepest, and while Z is held with an AIOT in your hand the wheel does not change the hotbar slot. A plain press of Z
  shows the readout.
* **Modules** (AIOTs only): two slots. Put a module in like any item; the button beside each slot turns it on or off.
* The **Armor** button goes to the armor menu, and its **Tools** button comes back.

### AIOT modules
| Module | Recipe | What it does |
|---|---|---|
| **Nexus Link** | Dimensional Link Card in the middle, ender pearls on the sides, iron ingots in the corners | What the AIOT mines goes straight into a storage network |
| **3x3 Mining** | Diamond in the middle, redstone on the sides, iron ingots in the corners | Mines the 3x3 around each block |

* **Nexus Link**: sneak + right-click a Crafting Terminal with the module (or an AIOT that has one) to link it. Drops
  go into that terminal's network in any dimension; what the network can't take drops as usual. **Shulker boxes (any colour) and
  ender chests are never taken**: they drop where they were, in a single swing and in a vein or 3x3 alike.
* **3x3 Mining**: mines the 3x3 around the block, facing you. Press **X** (rebind under Controls > Key Binds >
  Nexium) to switch it on or off for the AIOT in your hand. With a **tunnel length** set in the tool menu it digs that
  many 3x3 layers going away from you, one swing, with the same rules for every block. Only blocks the AIOT is made for, at most about twice as
  hard as the block you mined, and never blocks with contents such as chests.
* **Vein mining** (no module, **any tool or an empty hand**): mining any block, stone included, mines every block of the same
  kind touching it, up to the size set in an AIOT's tool menu (any other tool or a bare hand takes up to `aiotVeinMaxBlocks`).
  Grasses, ferns, bushes, dead bushes, azaleas and leaf litter (the `#nexium:vein_plants` tag) count as one kind, so a mixed
  patch clears in one swing, and cost no durability. Blocks with contents, such as chests, are never vein mined, and the tool
  must be right for the block (obsidian needs a diamond-grade pickaxe or AIOT; a bare hand takes only what it can break, such as
  dirt, sand or logs). Other tools and AIOTs pay 1 durability per block, and a tool that breaks stops the vein.
  **Hold the Vein Mine key** (**Left Control** by default; change it under Key Binds) while mining; without it, you mine one
  block. **Blacklist**: the `veinBlacklist` setting (Nexium Settings > All-in-one Tools > Vein mining blacklist, or `config/nexium.json`)
  lists blocks vein mining never takes, with any tool: block ids such as `minecraft:diamond_ore` or tags such as `#minecraft:logs`.
  The screen can add the block you last looked at. A data pack can also list blocks in `#nexium:vein_excluded` (empty by default).
* **The old Vein Mining Module** is retired. It stays in the game only so that AIOTs and chests that hold one still load:
  it can't be crafted, right-clicking it gives back its materials (4 iron ingots, 4 lapis lazuli and a diamond), and so
  does opening the tool menu of an AIOT that still has one installed.
* With an AIOT in hand, a **faint pale green outline** shows the blocks your next swing would take along with the one you
  look at (the rest of a vein while the key is held, the 3x3 or the tunnel). Sneaking mines one block, so nothing is
  outlined. It uses the same rules as the mining, and shows for 20 seconds after you switch to an AIOT or press the 3x3
  (X), tunnel (Z) or vein key, and while the vein key is held, like the readout below. `aiotMiningPreview` (client) turns it off.
* A small readout in the bottom right corner, by the hotbar, shows what the AIOT in your hand is set to: Silk Touch or
  Fortune (when it holds both) the vein size and each module's state (3x3 on, tunnel depth, Nexus Link). It shows for 20
  seconds after you switch to an AIOT or press the 3x3 (X), tunnel (Z) or vein key, stays up while the vein key is held, then fades
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
  **diamond** in a crafting grid for Diamond; at a smithing table (template slot empty), a **netherite ingot**
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
* **Nexium Ingot**: a diamond, an emerald and a netherite ingot in a crafting table make
  **3 ingots**. Nine make a **Block of Nexium** (a beacon base).
* **Gear**: at a smithing table, the netherite piece and a Nexium ingot (leave the template slot empty). There is a
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
| **Water breathing** | full set | Water Breathing: you can't drown |
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

### Backpack, trinket, wings, arrow and totem slots
* Five slots that use no inventory space. Above the offhand slot: the **backpack slot** (a Backpack), the **trinket
  slot** (the magnet) and the **wings slot** (an elytra, or any item that glides like one). Under the crafting grid:
  the **arrow slot** (up to 64 arrows of any kind) and the **totem slot** (a Totem of Undying or any item with death
  protection).
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

### Backpack
* Crafted from 8 leather around a chest:
  ```
  L L L
  L C L
  L L L
  ```
* Wear it in the **backpack slot** (shift-click it, or **sneak + right-click** it in your hand). It holds **36 stacks**,
  as much as your whole inventory including the hotbar, and uses no inventory space. Press **B** to open the worn one
  (change the key under Controls > Key Binds), or **right-click** a Backpack in your hand to open that one. A Backpack
  can't hold another Backpack. Its contents are saved in the item: it drops with everything in it when you die (unless
  keepInventory is on), and can be kept in a chest.
* It has **5 module slots** at the right of its screen. Each module fits once, and the button beside it switches it
  on or off. Modules only work while the Backpack is worn.
* **Auto Torch Module** (4 torches around a redstone lamp, with iron ingots in the corners): places torches taken
  straight from the Backpack wherever a hostile mob could still spawn within **32 blocks** of you in every direction:
  in the open, in caves behind walls, even in caves nobody has seen. It reads the real block light and only lights
  ground that is dark, and the torches are spaced as far apart as they can be and still leave no dark block (a torch
  stops spawns 13 blocks away, so on open ground they sit in a staggered grid about 19 blocks apart). It places up
  to 8 per sweep, takes no more than the Backpack holds, and does nothing in the Nether (light can't stop spawns
  there) or in adventure or spectator mode. It works while the Backpack's screen is open too: the torches on the screen are the
  ones placed.
* **Torch Stack Module** (retired): every stackable item stacks to **1,024** in the Backpack's slots now, with no module. The item
  stays in the game so a Backpack that holds one still loads, but it does nothing, has no recipe and no longer fits a slot.
* **Feeding Module** (4 bread around a golden carrot, with iron ingots in the corners): eats for you. Once a second it
  checks how much of your hunger bar is missing and eats the food in the Backpack that restores the most without any of it
  being wasted. It never eats foods with side effects (rotten flesh, pufferfish, golden apples, chorus fruit and the like),
  and does nothing in creative mode. It works while the Backpack's screen is open too. A **Feeding** panel under the module
  slots in the Backpack's screen sets when it eats, for that module: **Eats at N** is the hunger level (of 20) it eats at or
  below, 19 at first (as soon as any hunger is missing); a click steps it lower (18, 16, 14 ... 2, then back to 19) and a
  shift-click steps it higher. **No waste** (the default) eats only food that restores no more than is missing; **May waste**
  eats at that level whatever is missing: the best-fitting food, or the smallest if every food is bigger than what is missing.
* **Compacting Module** (4 pistons around a crafting table, with iron ingots in the corners): packs what the Backpack
  holds into storage blocks once a second: 9 iron ingots into an iron block, 9 nuggets into an ingot, 9 wheat into a hay
  bale. It finds what packs into what from the crafting recipes, the way the Compacting Drawer does, and only uses packs that
  craft straight back, so nothing is lost or changed (sand never becomes sandstone) and modded blocks work too. The blocks
  go in the Backpack's own free slots, and renamed or enchanted items are left alone. It packs while the Backpack's screen is
  open too: the blocks appear on the screen a moment after the last ingot goes in.
* **Restocking Module** (iron ingots in the corners, 2 hoppers, 2 chests and a diamond in the middle: `IHI / CDC / IHI`): tops up
  what you are using. A stack in the hotbar or the offhand that is being used up (blocks you place, arrows you shoot) is refilled
  to a full stack from the Backpack when it gets down to 8 (the same setting as the Nexus Restock armor bonus), or when the last
  one is used. Only use counts: a stack you move or throw away whole is left alone, and it does nothing in creative mode. When the
  Backpack has none of the item left but holds a **bigger form** of it, it unpacks just enough: Triple Compressed stone becomes
  Double Compressed, then Compressed, then stone, an iron block becomes ingots, and what was not needed stays in the Backpack.
  Which block unpacks into what comes from the crafting recipes, as for the Compacting Module (only packs that craft straight
  back), and renamed or enchanted items are never unpacked. It works while the Backpack's screen is open too.
* **Crafting Module** (iron ingots in the corners, any planks on the four sides, a crafting table in the middle: `IPI / PTP / IPI`):
  gives the Backpack's screen a **3x3 crafting grid** with a result slot, in a panel at its right (the screen is centred with it).
  Put items in the grid by hand or open the **recipe book** (its button is under the grid): the book lists what the Backpack's
  slots and your inventory can make, and a recipe you click, or a recipe viewer's JEI or REI "+", fills the grid from the Backpack
  first and your inventory after it. After every craft the emptied grid slots refill from the Backpack, so a shift-click on the
  result crafts all there is. Take the module out or switch it off, or close the screen, and what is in the grid goes back into the
  Backpack. When the screen is too narrow for the recipe book beside it, the open book covers the screen, as in vanilla.
* **Trash**: the Backpack's screen always has a trash slot, under the module column (no module needed). Drop an item on it to get
  rid of it. It keeps the last item you put in until you put another in or close the screen, so you can take it back; shift-click
  empties it into your inventory.

### Shulker box preview
* A shulker box with something in it shows a **9 x 3 grid of what is inside** under its name, in slots the colour of the box, with
  every item and count. It replaces the normal item list ("Iron Ingot x64 ... and 12 more"), which is hidden; an empty box and
  other containers keep their normal tooltip.

### Diagonal placement
* Hold **Left Alt** (change it under Controls > Key Binds, in the Nexium group) and **right-click a block with a block in your
  hand** to place it **diagonally**: one step out from the face you clicked and one step over toward the **nearest edge of that
  face**, the edge your crosshair is closest to. Aim near the top of a wall and the block lands diagonally up and out; near its
  left edge, diagonally left; on a top face, toward the edge you are nearest.
  ```
  aiming at the top part of the face:        aiming at its left part:

              [ NEW ]                        [ NEW ]
     [BLOCK]                                          [BLOCK]
  ```
* **Aim at the middle of the face instead** (the middle half of it each way) and the block goes **straight across from it with a
  one block gap**: two out from the face, nothing between, with no sideways step. Same key, the area you aim at decides.
  ```
  aiming at the middle of the face:

  [BLOCK]    gap    [ NEW ]
  ```
* The new block touches the one you clicked only along an edge, and it **may float**, so staircases, roofs, diagonal lines and
  pillars need no support block first. It uses up the item like any placement (not in creative), follows the aimed half of a slab or
  stair and the face for logs, never replaces a block (tall grass and water it does, as in vanilla), and refuses where you may not
  build (adventure and spectator mode, spawn protection, out of reach, or where another mod's protection says no).
* With the key held, a **white outline** shows the cell the block would go in before you click, and a **red** one where that cell is
  already taken (so nothing would be placed). On the block you aim at, the face is drawn in **zones** as in Tweakeroo: a **middle square**
  (straight across, with the gap) and a **trapezoid toward each edge** (diagonal). The zone you are aiming at, and so the one used, is
  **filled in gold**; the others are faint lines. `diagonalPreview` (client) turns it all off.
* Without the key, right-click places the normal way. The block must be in your main hand or, failing that, your offhand.

### Free camera
* Press **K** (change it under Controls > Key Binds, in Nexium) to take the camera off your player. You stay where you are while the
  movement keys and the mouse **fly the camera through everything**: forward goes where it looks, jump goes up, sneak goes down,
  sprint is 3 times faster, and **Left Ctrl + the scroll wheel change the speed** (10 blocks a second to start; the speed key is
  `Free Camera Speed` under Controls > Key Binds, in Nexium). The plain scroll wheel does nothing. Press K again to come back.
* **No fog** while the camera is away: the render distance's haze, water, lava, powder snow, blindness and darkness fog are all
  removed, so you see clearly to the edge of the loaded world, underwater or underground.
* **Night vision:** the world is drawn with the Night Vision effect at full strength while the camera is away, so caves, nights and unlit
  rooms are as bright as day. It only changes how things look; your player is not given the effect.
* **Nothing is culled:** the game normally draws only what it can reach from the camera through open space, which leaves caves and
  terrain undrawn when the camera is inside rock or under the world. That culling is off while the free camera is away.
* While it is away nothing you click reaches the world, and the hand, the experience bar and its level number are not drawn. Your own
  body is drawn once the camera is outside it. It ends by itself if your player is hurt or dies, or you change world. It is all on the
  client: the server is not told.

### Zoom
* **Hold the Zoom key** (none until you pick one: Controls > Key Binds, in Nexium) and the view closes in smoothly; **scroll the wheel
  while it is held** to zoom in more or less, from 1.25x to 20x (4x to start; the level is remembered, and the hotbar does not change
  while you do). Let go and it eases back out. **Mouse turning slows down by the same amount** so aiming stays steady. It also works
  from the free camera. All on the client.

### Villagers
* **Trades never run out, and never get dearer.** Every trade can be made as often as you like and none is ever locked after a few uses,
  and using a trade does not raise its price: prices ignore **demand** (a trade that had gone up goes back to its base price). The
  discounts and surcharges for how the village sees you still apply. It applies to villagers and wandering traders alike.
* **Trade cycling.** In a villager's trading screen press **C** (change it under Controls > Key Binds, in Nexium) to give a villager
  **nobody has traded with** a new set of trades, again and again until you like them, the way breaking and replacing its job site
  block does, without the block. It only works on a Novice with no experience and no trade used: once you trade with a villager, or
  it has experience, its trades stay. The new trades keep your discounts (reputation, Hero of the Village).

### Mending anywhere
* Experience orbs repair **any damaged item with Mending in your inventory**, not only what you hold or wear: the hotbar, the main
  inventory and the extra slots too. Each orb picks one at random, at the usual rate (2 durability for each point). It is the same
  for the XP you collect from the Experience Farm and the Nexus Furnace.

### Combined sponges
* **Nine sponges craft a Large Sponge, nine of those a Huge Sponge, nine of those a Giant Sponge**, and each crafts back into nine of the
  one below.
  | Sponge | Reach | Soaks up |
  | --- | --- | --- |
  | Sponge | 6 blocks | 65 water blocks |
  | **Large Sponge** | 14 blocks | 585 |
  | **Huge Sponge** | 32 blocks | 5,265 |
  | **Giant Sponge** | 64 blocks | 47,385 |
* They work like a sponge: placed next to water (or when water comes next to them) they soak up the water they can reach through the
  water, kelp and seagrass included, and turn **wet**. A wet one **dries in a furnace** (10, 20 or 40 seconds for a Large, Huge or
  Giant one) or at once in the Nether, and can soak again. Each is mined with a hoe, drops itself and shows its reach in its tooltip.
* A Backpack's Compacting Module packs sponges by itself (nine become a Large Sponge). They are in the Nexium Storage creative tab.

### Compressed blocks
* Twenty-nine bulk blocks have compressed versions that hold **9, 81 and 729** of them: **stone, cobblestone, deepslate, cobbled
  deepslate, tuff, granite, diorite, andesite, calcite, dripstone block, smooth basalt, terracotta, sandstone, red sandstone,
  obsidian, netherrack, blackstone, basalt, magma block, end stone, dirt, mud, clay, sand, red sand, gravel, soul sand, soul soil
  and snow block**. That is 87 new blocks: *Compressed*, *Double Compressed* and *Triple Compressed* of each. (Ice has none: 9 ice
  make packed ice and 9 packed ice make blue ice already, each with a way back.)
  ```
  B B B
  B B B      9 of a block make 1 Compressed block, 9 of those a Double Compressed, 9 of those a Triple Compressed;
  B B B      each one crafts back into 9 of the one below
  ```
* Each is mined like the block it stands for (same hardness, blast resistance, sound, light and tool; compressed obsidian takes a
  diamond pickaxe) and drops itself. Its tooltip says how many of the plain block it stands for. They are in the Nexium Storage
  creative tab.
* A Backpack's **Compacting Module** packs them by itself (729 cobblestone become one Triple Compressed Cobblestone), because it finds
  what packs into what from the crafting recipes, and a **Compacting Drawer** holds a block and its compressed versions as tiers.
* **Way back for one-way packs.** The Compacting Module only packs what crafts straight back, so these blocks, which packed but
  could not unpack, now craft back into exactly what made them (one block in the crafting grid): **snow block** into 4 snowballs,
  **clay** into 4 clay balls, **amethyst block** into 4 amethyst shards, **glowstone** into 4 glowstone dust, **honeycomb block**
  into 4 honeycomb, **magma block** into 4 magma cream, **melon** into 9 melon slices, **dripstone block** into 4 pointed
  dripstone and **resin bricks** into 4 resin bricks. Quartz, nether bricks, prismarine bricks and wool are left out (this version
  already crafts those blocks into slabs), and so are planks, coral, bamboo, honey bottles, flint and rabbit hide. Loose glowstone
  dust, honeycomb, magma cream and amethyst shards in a Backpack with the Compacting Module are packed 4 at a time until you
  unpack them.

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

### Enchanted Grindstone
Craft it from a **Grindstone, a Book and Lapis Lazuli** (shapeless: anywhere in the crafting grid). It is a grindstone to look at
(same shape, same ways of being placed, with a violet wheel) but its menu does something else: it **pulls enchantments off an item
one at a time, each onto a book of its own**, for free.

* Right-click it. Put an enchanted tool, armor piece or **enchanted book** in the top slot and plain **Books** in the Book slot. The
  list shows every enchantment on the item (curses in red); **click one** and it comes off onto a new Enchanted Book that holds just
  that enchantment, using up one Book. The item keeps its wear, its name and every other enchantment.
* It costs no experience and gives none back. Curses come off like any other enchantment. A book that already holds a single
  enchantment has nothing to separate. An AIOT's inactive Silk Touch or Fortune is listed and taken off like the rest.
* A list longer than five scrolls with the mouse wheel. If your inventory is full the new book drops at your feet; closing the screen
  gives back what is in the slots. The plain Grindstone is unchanged: it still wipes enchantments for experience.

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

A flat Nexium platform, a quarter of a block tall. **Craft:** chiseled deepslate across the top, iron ingots at the
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
* Its tooltip tells you what is in it. Always: the name, health, and what it looks like (a frog, cat, wolf, cow, pig or
  chicken variant, a horse's coat, a sheep's wool colour, a villager's job, level and village type, and so on). **Hold
  Shift** for the rest: how long a baby has to grow up or an adult to wait before breeding, whether it is in love, who
  tamed it and its collar, what it wears and holds, its potion effects, and traits such as a slime's size, a charged
  creeper or a sheared sheep.
* The Ender Dragon and the Wither can't be picked up, and only a pet's owner can pick it up. The **Warden** and the
  **Elder Guardian** cost **10 experience levels** (`bossPickUpLevels`; other mods' bosses in `#c:bosses` too, and a
  data pack can add mobs to `#nexium:pick_up_costs_levels`).

### Reinforced Glass
Tinted glass that nothing but you can break. It **blocks light** like tinted glass and **drops itself with any tool**, but the
**Wither**, the **Ender Dragon** and **every explosion** (creepers, TNT, ghast fireballs, Wither skulls, end crystals) leave it
standing. Craft **8** from **8 glass around 1 obsidian** (glass on the top and bottom rows, glass, obsidian, glass in the middle
row). It is in the Tools tab.

### Zoe, the dog
A follower that behaves like a tamed wolf, with her own model and skin (a black and white speckled cattle dog with a green
collar).
* **Dog Egg**: craft it from **5 bones and 4 leather** (bones in the corners and the middle, leather between). It is a Captured
  Mob called *Zoe's Egg*. **Right-click a block** to let her out, tame and with a green collar. **Sneak + right-click her with an
  empty hand** to put her back in the egg.
* She **follows you**, **comes to you** when you get far away, **attacks** what attacks you or what you attack, and **lies down**
  when you right-click her to sit. Dye her collar as you would a wolf's.
* **She bites 1.5 times as hard as a wolf** (6 damage instead of 4).
* **Nothing can kill her**: mobs, falls, fire, lava, drowning and explosions do nothing. Your own hits hurt her but never kill
  her; only the kill command and the void take her away.
* **Creepers run from her** (like from cats) and **cannot blow up** while she is within 8 blocks.
* **Armor**: she can wear **Wolf Armor** or the **Nexium Dog Armor** (6 Nexium Ingots, shaped like Wolf Armor; the same
  protection value of 20 for the body slot): right-click her with it, and shear it off. Both are in the Tools tab with the egg.

### Right-click harvest
**Right-click a fully grown crop** (wheat, carrots, potatoes, beetroot, nether wart, cocoa) to harvest it: it drops as if
broken, less the one seed it is replanted with. **Hold the vein mining key** (Left Control) to harvest the
whole connected field at once, up to `harvestVeinMax` crops. Sneak to use an item on the crop instead. `rightClickHarvest`
turns it off.

### Ancient debris
Ancient debris drops **netherite scrap** instead of itself: 2 per block, multiplied by Fortune like diamond ore. Silk
Touch still drops the block.

### Recipe tweaks
Crafting and loot changes that need no setup. They add recipes (to the vanilla ones, never taking one away) under the
`nexium:tweaks/` ids, and a few vanilla ids make more; each new recipe unlocks in the recipe book when you hold what it uses.

* **Bigger batches**: stairs make 8 (not 4), trapdoors 12 (not 2), wood and hyphae 4 (not 3), and 4 bricks make 4 brick blocks.
  Chiseled and cracked stone bricks count for stone brick stairs.
* **Slabs and stairs back to blocks**: 2 slabs make their block, 4 stairs make 3 blocks (4 smooth stone slabs make 2 smooth stone);
  and in the crafting grid 1 block makes 2 slabs. No recipe makes a profit.
* **Universal dyeing**: 8 of any colour of candle, concrete powder, glazed terracotta, stained glass, stained glass pane or
  terracotta around one dye (ink sac, lapis, cocoa beans and bone meal count as dyes) make 8 of that colour. Ice in the middle
  washes them back to plain candle, terracotta, glass or glass panes.
* **Small ones**: 9 coral or coral fans of one kind make that coral block; basalt and coal or charcoal make blackstone (polished
  basalt makes polished blackstone); a dropper and a bow make a dispenser, and a dropper, 3 sticks and 3 string do too; blackstone
  versions of the brewing stand, dispenser, dropper, lever, observer and piston; iron, gold and diamond horse armor from a saddle
  and 6 of the metal; 8 gold blocks and an apple make an enchanted golden apple; 6 powered rails from copper ingots; 4 flint
  make gravel; sand and a red dye make red sand, and sandstone and red dye make red sandstone and smooth red sandstone; packed
  ice makes 9 ice and blue ice 9 packed ice; a nether wart block makes 9 nether wart; coal or charcoal makes black dye; 3 wheat
  make bread and 3 sugar cane make paper without a row; 2 shulker shells and a chest make a shulker box; a furnace turns rotten
  flesh into leather and wool into 4 string.
* **Stonecutter**: a block of quartz cuts back into 4 quartz.
* **Armadillo Crate**: 8 armadillo scutes around a chest make a shulker box named Armadillo Crate (shulker boxes already keep
  their contents).
* **Loot**: a shulker drops 2 shells (not 1) when it drops one, at the same chance, and budding amethyst drops itself with
  Silk Touch (the AIOT's Silk Touch too).

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
  | Smooth Stone | 576 | Smooth stone |

  Then press Build.
* **Place it anywhere.** Every 7.5 seconds it makes a golem's drops: 3-5 iron, about **1,900 iron an
  hour**. Hoppers take the output from below; item conduits take it from any side.
* **Power only**: the farm needs Nexus Energy and nothing else (no bread or other boost).
* A full output or a **redstone signal** pauses the farm. Breaking it keeps its progress in the item.
* A **comparator** reads how full the output is, and 15 once the farm has stopped for lack of room.

### More one-block farms
Made at the Farm Assembler like the Iron Farm (mobs can be spawn eggs or Captured Mobs). Every farm needs **power** (see Power below). Place it anywhere; hoppers take the output from below (item conduits from any side), a full output or a redstone signal pauses it, and breaking it keeps its progress in the item.
A comparator reads how full the output is, and 15 once the farm has stopped for lack of room.

| Farm | Recipe | Makes |
|---|---|---|
| **Villager Breeder** | 2 villagers, 4 beds, 2 composters, 8 hay bales, 2 emerald blocks, 576 planks | A baby villager (a Captured Mob) every 10 minutes. |
| **Piglin Barter Farm** | 2 piglins, 9 gold blocks, 16 soul sand, 16 planks, 576 netherrack | Every 6 seconds: the vanilla piglin barter loot |
| **Zombified Piglin Farm** | 4 zombified piglins, 8 magma blocks, 20 nether bricks, 16 gold ingots, 576 netherrack | Every 4 seconds: gold nuggets (0-6, about 3) and a 7.5% gold ingot, like Looting V with no player needed. No rotten flesh |
| **Wither Skeleton Farm** | 4 wither skeletons, 16 nether brick fences, 8 soul sand, 4 coal blocks, 4 magma blocks, 576 netherrack | Every 6 seconds: 0-1 coal, 0-2 bones and a 7.5% wither skeleton skull |
| **Shulker Farm** | 4 shulkers, 32 purpur blocks, 8 end rods, 8 ender pearls, 576 end stone bricks | Every 30 seconds: a shulker shell about 4 times in 5 (81%) |
| **Witch Farm** | 2 witches, 8 cauldrons, 16 spruce planks, 8 lily pads, 576 mossy cobblestone | Every 10 seconds: a witch's own drops, as if a player killed her (no Looting): glowstone dust, redstone, gunpowder, sugar, spider eyes, sticks and glass bottles |
| **Sugar Cane Farm** | 32 sugar cane, 4 water buckets, 8 pistons, 16 observers, 576 sand | 3 sugar cane every 20 seconds |
| **Bamboo Farm** | 32 bamboo, 4 water buckets, 8 pistons, 16 observers, 576 dirt | 4 bamboo every 20 seconds |
| **Nether Wart Farm** | 32 nether wart, 16 soul sand, 4 lava buckets, 8 magma blocks, 576 netherrack | 4 nether wart every 30 seconds |

The Farm Assembler's list scrolls (mouse wheel) now that it has more than seven targets. Every farm asks for the same filler: 576 (nine stacks) of one block, except the Tree Farm (18 stacks of logs) and the Experience Farm (one stack of Bottles o' Enchanting).

**More farms** (576 of their filler block; mobs are spawn eggs or Captured Mobs):

| Farm | Recipe | Makes |
|---|---|---|
| **Monster Farm** | 1 zombie, 1 pillager, 8 hoppers, 16 iron bars, 8 pistons, 4 emerald blocks, cobblestone | Every 10 s: the real loot of a zombie, skeleton, spider, creeper, pillager, vindicator and evoker, as if a player killed each (no Looting, rare player-kill drops included), all mixed. The evoker's Totem of Undying is a 2% find each cycle instead of every time |
| **Nether Mob Farm** | 1 blaze, 1 ghast, 1 magma cube, 16 nether bricks, 4 lava buckets, 8 magma blocks, netherrack | Every 10 s: blaze rods, ghast tears, gunpowder, magma cream |
| **Ocean Farm** | 1 guardian, 1 drowned, 1 cod, 16 prismarine bricks, 8 sea lanterns, 4 water buckets, sand | Every 10 s: prismarine shards and crystals, fish, rotten flesh, copper |
| **Slime Farm** | 2 slimes, 4 slime blocks, 8 sticky pistons, 8 hoppers, mud | Every 10 s: slime balls |
| **Livestock Farm** | 1 each of cow, pig, sheep, chicken, rabbit, 16 hay bales, 16 oak fences, dirt | Every 20 s: the drops of a cow, a sheep (random wool colour), a chicken (and an egg), a pig and a rabbit |
| **Crop Farm** | 32 each of wheat, carrots, potatoes, beetroot, 4 water buckets, dirt | Every 20 s: 3 of each crop and seeds. |
| **Gourd and Cactus Farm** | 16 each of melons, pumpkins, cactus, kelp, 4 water buckets, sand | Every 20 s, all at once: 12 melon slices, 3 pumpkins, 4 cactus and 6 kelp |
| **Generator** | 4 lava buckets, 4 water buckets, 8 hoppers, 2 diamond pickaxes, cobblestone | Every 10 s, by its mode button: 8 cobblestone, stone, gravel or sand, 4 clay, 2 basalt; obsidian 1 every 20 s |
| **Bee Farm** | 2 bees, 4 beehives, 4 campfires, 16 flowers, 8 glass bottles, oak planks | Every 20 s: 1 to 3 honeycomb, and 2 bottles of honey, both into its output slots |
| **Froglight Farm** | 3 temperate, 3 warm and 3 cold frogs and 9 slimes (Captured Mobs), 576 glass of any colour | Every 20 s: 5 ochre, 5 verdant and 5 pearlescent froglights |
| **Tree Farm** | 18 stacks of logs (any kinds); no other parts | Every 20 s, 8 logs of the tree its mode button picks (oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, pale oak, poplar, crimson or warped stems); on chorus, 4 chorus fruit and a 5% chorus flower. Shift-click the button to go back |
| **Experience Farm** | 64 Bottles o' Enchanting; no other parts | Makes **40 XP a second** (2 a tick) on **10 NE/t** and banks it up to **200 levels' worth** (149,720 points). Right-click it: **Collect all** gives you everything banked, **+1 level** and **+10 levels** give just enough to rise that many levels (or all that is banked if less). Every point taken **mends first**, as an experience orb does: a random damaged Mending item you carry or wear (the wings slot included) is repaired before the rest becomes levels, so a press can leave you short of a level when something needed mending. It stops when full and starts again once you collect; a redstone signal pauses it, a comparator reads how full the bank is, and broken it gives the bank back as experience orbs. XP is only collected by hand: it cannot be moved by conduits |
| **Moss Farm** | 32 moss blocks, 8 azaleas, 4 water buckets, 8 bone blocks, clay | Every 20 s: 3 moss blocks, 2 moss carpet, 3 pale moss blocks, 2 pale moss carpet, 2 pale hanging moss, 6 resin clumps, a 10% azalea. |

### Power (Nexus Energy)
Every one-block farm and the Freezer runs on **Nexus Energy (NE)**; the Disk Drive and the rest of the network need none.
A farm uses 10 NE a tick (Sugar Cane, Bamboo, Nether Wart, Crop, Gourd and Cactus, Generator, Moss, Tree), 20 (every other
farm and the Freezer) or 40 (Iron Farm), and keeps 10 seconds' worth in a buffer. Without power it waits and its screen says so.

* **Overclock**: every one-block farm, the Iron Farm, the Experience Farm and the Freezer has an **OC** button at the top right of its screen.
  Click it to step through **off, x2, x4 and x8** (Shift-click goes back). At x4 the farm does 4 ticks of work a tick, so it cycles four
  times as fast, and draws 4 times the NE/t, so **a cycle costs the same energy at any speed**. The buffer grows with the setting to
  keep holding ten seconds' worth. Short of energy it does not stop: it runs as fast as the energy it holds can pay for. The setting is
  saved with the farm and kept in the dropped item.

Generators give energy to the machines touching them; **Energy Conduits** carry it further.

| Block | Makes | Notes |
|---|---|---|
| **Combustion Generator** | 20 NE/t | Burns anything a furnace burns, for as long as the furnace would (a piece of coal: 80 s) |
| **Solar Panel** | 30 NE/t | Daylight with open sky above; 8 NE/t in rain; nothing at night or under a roof; stores 5,000 NE |
| **Thermal Generator** | 80 NE/t | Burns lava from a bucket, a Fluid Intake or a fluid conduit; a bucket lasts about 8 minutes |
| **Redstone Reactor** | up to 2,700 NE/t | Stores 1,000,000 NE; see below |

* **Energy Conduit**: Tier 1 moves 2,500 NE/t a side, the Ender Energy Conduit (Tier 2) 50,000 NE/t. Touching Energy Conduits
  form one network with a **shared pool**: each segment holds 10,000 NE (Tier 1) or 200,000 (Tier 2), added together, so a
  long run keeps a reserve. Generators on its sides fill the pool and machines draw from it, highest priority first
  and equal priorities evenly. **Batteries are the backup**: they give only what the generators leave short of what the machines
  want, and they take whatever is left over, so a Battery neither starves a farm nor drains itself into the cable. Sides can be set to extract or insert as with the other conduits; energy ignores colour
  channels. A broken conduit loses its share of the pool.
* **Jade** (optional): with Jade installed, looking at any of these blocks, or an Energy Conduit (its whole network), shows
  the energy it holds.
* **Batteries** (iron, gold, diamond) hold 250,000, 1,000,000 and 10,000,000 NE. Six cells on the side show the charge and a
  comparator reads it. A broken Battery keeps its charge in the item.
* **Power Meter** (`IGI / RCR / III`: iron ingots, glass, redstone, a comparator): put it against an Energy Conduit and right-click it
  for a dashboard of that conduit's whole network: what it makes and uses, what is stored and how long it lasts, a minute of history,
  and every generator, machine and battery on it with its rate. Each machine has a **priority** (High, Normal, Low: Low goes without
  first when power runs short) and an **On/Off** switch, which set the conduit sides that feed it.
* **Redstone Reactor**: nine Redstone Fuel Rod slots, three coolant slots (ice, packed ice, blue ice). Each rod makes 200 NE/t at
  the default depth (1,800 with nine); it runs on any one to nine rods. The **In** and **Out** buttons set one depth for all
  the rods: -2, -1, 0 (the default), +1 and +2 make 100, 150, 200, 250 and 300 NE/t a rod, with
  0.25x, 0.5x, 1x, 1.6x and 2.25x the heat and 0.5x to 1.5x the rod wear. Ice takes the heat away once it passes 25% (blue
  ice best; three slots of blue ice hold nine default rods). Output is full to 60% heat, falls steadily to 10% at 90%, and
  at 90% the reactor shuts off until it is under 50%. It never explodes. A rod lasts 96 hours at the default depth and
  leaves a **Spent Fuel Rod**.
* **Recipes**: Redstone Fuel Rod: 8 redstone blocks around a Nexium ingot; a Spent Fuel Rod with 8 redstone blocks around it
  gives a fresh one. Reactor: nexium ingots in the corners, iron blocks above and below the middle, redstone blocks
  beside it and a diamond block in the centre.

### Nexus Furnace
A furnace for Nexus Energy with **eight smelting slots that run at once**. **Recipe**: 3 iron ingots on top, then a furnace,
a blast furnace and a smoker, then a redstone block, an iron ingot and a copper block (`III / FBS / RIC`).

* **Eight inputs, eight outputs**: each input slot has an output slot of its own. It cooks anything a furnace, a blast
  furnace or a smoker does, each item in **2.5 seconds**.
* **30 NE/t for every working slot** (240 NE/t for all eight), with a buffer of ten seconds' worth. With less energy than the
  working slots need, it runs as many slots as it can pay for and they take turns, so all of them cook the same amount over
  time. Its screen shows how many are cooking.
* **A stack you put in is shared out evenly** over the input slots, by hand, shift-click, hopper or pipe: 64 iron ore become
  8 in each slot, topping up the slots that already hold iron ore.
* **Hoppers and pipes go in from the top and sides and out from the bottom.** A full output only stops its own slot.
* **Experience** is **ten times** what the recipes give (raw iron 7, a steak 3.5) and is kept in the furnace; the **Collect XP** button
  on its screen shows the whole points banked and gives you exactly that many (the fraction stays). Like an experience orb, it
  **mends first**: a random damaged item you carry or wear with Mending (the elytra in the wings slot too) is repaired, 2 durability
  a point, and what it does not use is yours.
* No redstone control and no comparator output. It shows up in Jade with its energy.

### Freezer
* **Recipe**: 3 packed ice, 5 iron ingots and a cauldron (`PPP / ICI / III`).
* **Fill it with water** with a water bucket or a pipe (fluid conduit, Dimensional Output). The tank holds 8 buckets and
  only takes water. It freezes **ice** (1 bucket, 5 seconds), and its button switches it to **packed ice** (9 buckets,
  22.5 seconds) or **blue ice** (81 buckets, 45 seconds). The water is used up steadily while it works, so a tank that runs
  dry waits for more. Hoppers take the ice from below and item conduits from any side; a full output or a redstone signal pauses it.
* **Dyeable like leather armor**: put it in the crafting grid with dyes (several mix). The body and frame take the colour;
  the frost, glow and water don't. A cauldron of water washes it. It keeps its colour, water and mode when broken.
* **Ice recipes**: 9 ice make packed ice and 9 packed ice make blue ice (vanilla); packed ice also crafts into 9 ice and
  blue ice into 9 packed ice.
* **Soul sand to sand**: a Blast Furnace smelts soul sand into sand, with no experience, at an ore's speed (5 seconds).

## Quests

A quest book with six chapters (Storage, Power, Farms, Tools, Backpack, Nexium), opened with the **Quests** button at the top of the
Codex or the *Open Quests* key (no key until you pick one). Each quest asks you to make or have an item and counts what you have
already done (crafting statistics, or what you hold). Nothing is locked: every chapter and quest is open from the start, in any order. Every quest gives
a random item from the mod (more of them in later chapters). A quest can take a whole item tag instead of one item (the Drawer
quest takes a drawer of any wood or size). No recipe is ever locked, and **every Nexium recipe is put in your recipe book when you join
a world** (and after a `/reload`). Progress is saved on the player. The quests are in `data/nexium/quests.json` in the mod.

## Settings

`config/nexium.json` is created on first launch. Every setting can also be changed in the game, on **Nexium Settings**:
the **Settings** button in the Codex or the armor menu, **Mod Menu**'s Configure button, or the *Open Nexium Settings* key (unbound
by default). They are grouped by feature, each has a Reset, and hovering one says what it does and its limits. Changes are saved as
you make them and apply at once in a world you host. On a server, the server's file is the one that counts. If the file can't be
read (a typo, say), the game starts with the defaults and keeps what you had as `nexium.json.broken-<date>-<time>` beside it, so
nothing you typed is lost; fix it there and copy it back.

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
| `harvestVeinMax` | `128` | Most crops one harvest takes with the vein key held, 1 to 1024 |
| `anvilNoCostLimit` | `true` | An anvil is never Too Expensive: any job, for its full cost in levels; false keeps vanilla's limit |
| `ironFarmGolemTicks` | `150` | Ticks between golems (150 = 7.5 s) |
| `villagerBreederTicks`, `piglinBarterTicks`, `zombifiedPiglinTicks`, `witherSkeletonTicks`, `shulkerFarmTicks`, `witchFarmTicks`, `sugarCaneFarmTicks`, `bambooFarmTicks`, `netherWartFarmTicks`, `monsterFarmTicks`, `netherMobFarmTicks`, `oceanFarmTicks`, `slimeFarmTicks`, `livestockFarmTicks`, `cropFarmTicks`, `gourdFarmTicks`, `generatorFarmTicks`, `beeFarmTicks`, `mossFarmTicks`, `froglightFarmTicks` | `12000`, `120`, `80`, `120`, `600`, `200`, `400`, `400`, `600`, `200`, `200`, `200`, `200`, `400`, `400`, `400`, `200`, `400`, `400`, `400` | Ticks per cycle of each farm (20 ticks a second), 1 to 1,000,000 |
| `freezerIceTicks`, `freezerPackedIceTicks`, `freezerBlueIceTicks` | `100`, `900`, `1800` | Ticks the Freezer takes for 1 ice, 1 packed ice and 1 blue ice |
| `aiotVeinMining` | `true` | Vein mining works (with any tool or an empty hand) |
| `veinBlacklist` | `[]` | Blocks vein mining never takes: block ids and `#tags`, edited in the settings screen |
| `connectorBlacklist` | the vanilla machines and `#nexium:machines` | Blocks the Storage Connector and the Filtered Connector never connect to: block ids and `#tags`, edited in the settings screen |
| `aiotVeinMaxBlocks` | `256` | Most blocks one vein mining swing takes (with any tool), 1 to 512 |
| `aiotAreaMining` | `true` | The 3x3 Mining module works |
| `aiotTunnelMaxLength` | `32` | Longest tunnel the 3x3 module can dig, in layers, 1 to 64 |
| `aiotMiningPreview` | `true` | Client: outline the blocks an AIOT swing would mine |
| `builderPreview` | `true` | Client: outline where the Construction Builder in your hand would put blocks |
| `diagonalPreview` | `true` | Client: outline the cell a diagonal or straight-across placement would put the block in, and mark the edge or face used, while the Diagonal Placement key is held |
| `aiotHud` | `true` | Client: show the held AIOT's mode and module states in the bottom right corner |
| `terminalSearchSync` | `true` | Client: the terminals' search, the recipe book's and JEI's or REI's stay the same |
| `aiotHudSeconds` | `20` | Client: seconds that readout and the mining outline stay up after switching to an AIOT or pressing X, Z or the vein key; 0 keeps them up |
| `aiotNexusLink` | `true` | The Nexus Link module works |
| `nexusRestock` | `true` | The Nexium armor's Nexus restock bonus works |
| `restockTopUpAt` | `8` | A hotbar stack being used up is topped up when it gets down to this many |
| `restockEatWhenMissing` | `6` | The wearer eats from the network when the hunger bar is down by this much |
