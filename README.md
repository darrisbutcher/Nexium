# Nexium (Fabric, Minecraft 26.3)

Drawers, a storage network with crafting terminals, cross-dimensional tablets and item links, item and fluid
conduits, all-in-one tools (AIOTs), **Nexium** (a tier above netherite) and a set of quality-of-life **Essentials**
(no creeper or ghast block damage, an item and XP magnet, wings, arrow and totem slots, Fortune on ancient debris,
a 1-block iron farm and more).

Every item is in the **Nexium** creative tab, and every tooltip says what its item does.

The **Nexium Guide** (given once when you join a world, or crafted from a book and any drawer) is the in-game manual.
Right-click it to pick one of nine chapters (Start Here, Drawers & Barrels, Storage Network, Links & Conduits, All-in-one
Tools, Nexium, Nexium Armor, Essentials, and Mobs, Elixir & Iron Farm). Each opens as a book that describes every item,
how to use it and, at the end, its crafting recipes drawn as crafting grids. Closing a book comes back to the chapter list.

The **Nexium advancement tab** (press **L**) is a quick map of everything in the mod: 33 nodes with an icon and a short
description each, all unlocked from the start. For a bigger, easier-to-read advancements screen, try the optional
[Better Advancements](https://modrinth.com/mod/Q2OqKxDG) mod: the tab works with it automatically, and Nexium doesn't need it.

## Install and build

Put the jar and **Fabric API** for 26.3 in your `mods` folder. It is needed on both the client and the server.

```
# Windows
.\gradlew.bat build
# macOS / Linux
./gradlew build
```

The jar ends up in `build/libs/nexium-1.12.2.jar` (not the `-sources` one). Any Java 17+ starts the build; Gradle
fetches JDK 25 itself. `./gradlew runGameTest` runs the in-game tests on a headless server and
`./gradlew runClientGameTest` runs the client tests (they need a window or Xvfb). Config lives in
`config/nexusessentials.json`.

### Releasing

Add a `## X.Y.Z` section to `CHANGELOG.md`, set `mod_version` to match, merge to `main`, then push a tag:
`git tag vX.Y.Z && git push origin vX.Y.Z`. The Release workflow builds the jar and publishes a GitHub Release with it, using
that changelog section as the notes. To release without pushing a tag, or for an older version, run the workflow by hand (Actions > Release > Run workflow)
with the tag name and the commit that version shipped in; it creates the tag on that commit.

## Storage

### Drawers
* 1x1 holds **54 stacks**, 1x2 holds **27 stacks** per slot, 2x2 holds **13.5 stacks** per slot.
* Right-click the front to insert, double-click to insert everything matching, left-click to take a stack (sneak for
  one item). Break drawers from the side.
* A bar under each count shows how full the section is, counting upgrades: green under 75%, yellow to 90%, red above.
* Right-click with an upgrade to install it; right-click the side or top with an empty hand to see or remove
  upgrades. Capacity upgrades add together: Iron x2, Gold x4, Diamond x8, Emerald x16, Netherite x32. A higher
  capacity upgrade swaps out the weakest one. The Lock Upgrade locks the contents (padlock on the front) and the
  Void Upgrade destroys overflow.
* **Fluid Drawers** hold 32 buckets of one fluid. **Compacting Drawers** hold nuggets/ingots/blocks (any reversible
  9:1 or 4:1 recipe), 54 stacks of the largest form.
* Breaking a drawer keeps its contents and upgrades in the item, like a shulker box.
* **Drawer Controller**: drawers touching it or each other within **12 blocks** form a cluster. Right-click to send
  items into the drawer already holding them; double-click dumps your inventory. **Controller Slave** extends the
  reach, and slaves can chain.

### Barrels
**Iron, Gold, Diamond, Emerald and Netherite Barrels** hold 45, 66, 84, 102 and 120 slots. Right-click a barrel with
the matching **Barrel Upgrade** to upgrade it in place (Iron upgrades a vanilla barrel, Gold an Iron Barrel, and so
on). What is inside stays inside.

### Simple network
* **Storage Connector** reads every inventory touching it; **Filtered Connector** reads only the one it faces,
  through a filter. Both have a priority. **Inventory Cable** links connectors and terminals. No power.
* **Import/Export Bus**: placed on an inventory or tank, **Import** pulls items and fluids into the network and
  **Export** pushes them out (32 items or a bucket every half second). An optional filter limits what moves.
* **Level Emitter**: a redstone signal while the network holds at least (or fewer than) an amount of one item or fluid.
* **Crafting Terminal**: items and fluids in one grid with search (`@modname` filters by mod) and sorting, a network
  fill bar, the vanilla recipe book, and a red **X** that empties the crafting grid. JEI / REI "+" fills the grid from
  the network (neither mod is required).

### Tablets
Sneak + right-click a Crafting Terminal with a **Crafting Storage Tablet** to link it, with no range limit and in any
dimension, as long as the terminal's chunk is loaded. A **Dimensional Anchor** loads a 3x3 chunk area, always or only
while a tablet is open.

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
* **Vein mining** (no module): mining any block except the stone family (stone, granite, cobblestone, deepslate, netherrack ... the
  `#storagenexus:vein_excluded` block tag, which a data pack can change) mines every block of the same kind touching
  it, up to the size set in the tool menu. Blocks with contents, such as chests, are never vein mined, and the tool must be right for the block (obsidian needs a diamond-grade AIOT).
  **Hold the Vein Mine key** (**V**) while mining; without it, you mine one block.
* **The old Vein Mining Module** is retired. It stays in the game only so that AIOTs and chests that hold one still load:
  it can't be crafted, right-clicking it gives back its materials (4 iron ingots, 4 lapis lazuli and a diamond), and so
  does opening the tool menu of an AIOT that still has one installed.
* With an AIOT in hand, a **faint pale green outline** shows the blocks your next swing would take along with the one you
  look at (the rest of a vein while the key is held, the 3x3 or the tunnel). Sneaking mines one block, so nothing is
  outlined. It uses the same rules as the mining, and `aiotMiningPreview` (client) turns it off.
* A small readout in the bottom right corner shows what the AIOT in your hand is set to: Silk Touch or Fortune (when it
  holds both) the vein size and each module's state (3x3 on, tunnel depth, Nexus Link). `aiotHud` (client) turns it off.
* Each block costs 1 durability, Fortune, Silk Touch and the Nexus Link apply, and spawn protection is respected.
  **Sneak** to mine just one block.
* If an AIOT breaks, the modules in it drop at your feet (on/off setting and Nexus Link terminal kept).

### Nexium
* **Nexium Ingot**: a block of diamond, a block of emerald, a block of netherite and a netherite upgrade template in a
  crafting table make **3 ingots**. Nine make a **Block of Nexium** (a beacon base).
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

### Enchantment levels and exclusions
* **Level V**: Protection, Fire Protection, Blast Protection, Projectile Protection, Unbreaking, Looting, Fortune and Respiration
  go up to V in the enchanting table, at an anvil (two IV books make a V), in loot chests and from villagers.
* **Together**: all four protections on one piece, Mending with Infinity, and Sharpness with Smite. Bane of Arthropods still
  excludes both, and Fortune still excludes Silk Touch.
* This is done with data files (`data/minecraft/enchantment`), so a data pack can change it.

### Master Librarian
Rename a chest **Nexus Librarian** in an anvil (single or double) and put in exactly **12 villagers**, spawn eggs or Captured
Mobs and nothing else. The chest and the villagers are used up and a **Master Librarian** pops out as a Captured Mob: right-click
a block to let him out.

* He sells an **enchanted book of every enchantment at its highest level**: Mending and the other treasure enchantments
  too, level V ones included, but no curses. A trade takes emeralds and a book, at the villagers' own prices: 2 emeralds plus
  up to 5 + 3 per level more, doubled for treasure enchantments, never above 64. Each trade has the usual 12 uses.
* He is level 5 with every trade unlocked. A librarian made this way is not one of the villagers a chest wants, so it can't be
  fed back in.
* A chest that is not ready tells you what is missing when you close it. `librarianChestName` and `librarianVillagers` change
  the name and the number.

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
* The Ender Dragon and the Wither can't be picked up, and only a pet's owner can pick it up.

### Ancient debris
Ancient debris drops **netherite scrap** instead of itself: 2 per block, multiplied by Fortune like diamond ore. Silk
Touch still drops the block.

### Iron Farm
* **Making one**: place a chest, rename another chest **Nexus Iron Farm** in an anvil and put it next to the first to
  make a double chest. Fill it with exactly:

  | Material | Amount | Counts |
  |---|---|---|
  | Zombies | 4 | Zombie, husk, drowned or zombie villager: spawn eggs or Captured Mobs |
  | Villagers | 12 | Villager spawn eggs or Captured Mob villagers |
  | Torches | 33 | Torch, soul torch or copper torch |
  | Slabs | 20 | Any slab |
  | Signs | 16 | Any sign or hanging sign |
  | Trapdoors | 12 | Any trapdoor |
  | Beds | 12 | Any colour |
  | Smooth Stone | every other slot | Full stacks of 64 |

  The moment the chest holds exactly this, it and everything in it disappear and the **Iron Farm** pops out. Close
  the chest early and the chat says what's missing.
* **Place it anywhere.** Every 7.5 seconds it makes a golem's drops: 3-5 iron and 0-2 poppies, about **1,900 iron an
  hour**. Hoppers and pipes take the output from below.
* **Bread**: bread in the bread slot makes the farm **10x faster**, eating one loaf every 30 seconds. When the last
  loaf is eaten the slot locks for 10x as long as the boost lasted, and the farm keeps working at normal speed.
* A full output or a **redstone signal** pauses the farm. Breaking it keeps its boost and cooldown in the item.

### More one-block farms
Made like the Iron Farm: rename a **double chest** in an anvil, fill it with exactly the recipe (mobs can be spawn eggs or
Captured Mobs, and a full stack of the filler item goes in every other slot), and it turns into the farm. Place it anywhere;
hoppers and pipes take the output from below, a full output or a redstone signal pauses it, and breaking it keeps its
progress in the item. Close the chest early and the chat says what is missing.

| Farm | Chest name | Recipe | Makes |
|---|---|---|---|
| **Villager Breeder** | `Nexus Villager Breeder` | 2 villagers, 4 beds, 2 composters, 8 hay bales, 2 emerald blocks; planks fill | A baby villager (a Captured Mob) every 20 minutes. Feed it 3 bread, or 12 carrots, potatoes or beetroot, each |
| **Piglin Barter Farm** | `Nexus Piglin Barter` | 2 piglins, 9 gold blocks, 16 soul sand, 16 planks; netherrack fills | Trades 1 gold ingot every 6 seconds for the vanilla piglin barter loot; hoppers feed the gold from the top or sides |
| **Zombified Piglin Farm** | `Nexus Zombified Piglin` | 4 zombified piglins, 8 magma blocks, 20 nether bricks, 16 gold ingots; netherrack fills | Every 4 seconds: gold nuggets (0-6, about 3) and a 7.5% gold ingot, like Looting V with no player needed. No rotten flesh |
| **Wither Skeleton Farm** | `Nexus Wither Skeleton Farm` | 4 wither skeletons, 16 nether brick fences, 8 soul sand, 4 coal blocks, 4 magma blocks; netherrack fills | Every 6 seconds: 0-1 coal, 0-2 bones and a 7.5% wither skeleton skull |
| **Shulker Farm** | `Nexus Shulker Farm` | 4 shulkers, 32 purpur blocks, 8 end rods, 8 ender pearls; end stone bricks fill | Every 30 seconds: a shulker shell about 4 times in 5 (81%) |

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
| `librarianChestName` | `Nexus Librarian` | The name a chest needs to make a Master Librarian |
| `librarianVillagers` | `12` | How many villagers that chest takes, 1 to 54 |
| `elytraProtectionEnchantments` | `true` | Elytra accept all four protection enchantments |
| `netheriteArmorCalmsPiglins` | `true` | Netherite armor keeps piglins calm |
| `bedrockBreakerSeconds` | `300` | How long the elixir lasts |
| `mobPickUp` | `true` | Sneak + right-click picks up mobs |
| `onlyOwnersPickUpPets` | `true` | Only a pet's owner can pick it up |
| `ancientDebrisDropsScrap` | `true` | Ancient debris drops Fortune-multiplied netherite scrap |
| `ironFarmChestName` | `"Nexus Iron Farm"` | The name a double chest needs to become an Iron Farm |
| `ironFarmGolemTicks` | `150` | Ticks between golems (150 = 7.5 s) |
| `ironFarmBreadSpeedUp` | `10` | How many times faster bread makes the farm |
| `ironFarmBreadSeconds` | `30` | Seconds of boost per loaf |
| `ironFarmCooldownMultiplier` | `10` | Bread cooldown, as a multiple of how long the boost lasted |
| `ironFarmPoppies` | `true` | Golems drop 0-2 poppies as well as iron |
| `villagerBreederChestName`, `piglinBarterChestName`, `zombifiedPiglinChestName`, `witherSkeletonChestName`, `shulkerFarmChestName` | `"Nexus Villager Breeder"`, `"Nexus Piglin Barter"`, `"Nexus Zombified Piglin"`, `"Nexus Wither Skeleton Farm"`, `"Nexus Shulker Farm"` | The name a double chest needs to become each farm |
| `villagerBreederTicks`, `piglinBarterTicks`, `zombifiedPiglinTicks`, `witherSkeletonTicks`, `shulkerFarmTicks` | `24000`, `120`, `80`, `120`, `600` | Ticks per cycle of each farm (20 ticks a second), 1 to 1,000,000 |
| `freezerIceTicks`, `freezerPackedIceTicks`, `freezerBlueIceTicks` | `100`, `900`, `1800` | Ticks the Freezer takes for 1 ice, 1 packed ice and 1 blue ice |
| `aiotVeinMining` | `true` | Vein mining works |
| `aiotVeinMaxBlocks` | `256` | Most blocks one vein mining swing takes, 1 to 512 |
| `aiotAreaMining` | `true` | The 3x3 Mining module works |
| `aiotTunnelMaxLength` | `32` | Longest tunnel the 3x3 module can dig, in layers, 1 to 64 |
| `aiotMiningPreview` | `true` | Client: outline the blocks an AIOT swing would mine |
| `aiotHud` | `true` | Client: show the held AIOT's mode and module states in the bottom right corner |
| `aiotNexusLink` | `true` | The Nexus Link module works |
| `nexusRestock` | `true` | The Nexium armor's Nexus restock bonus works |
| `restockTopUpAt` | `8` | A hotbar stack being used up is topped up when it gets down to this many |
| `restockEatWhenMissing` | `6` | The wearer eats from the network when the hunger bar is down by this much |
