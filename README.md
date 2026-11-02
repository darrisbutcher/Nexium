# Storage Nexus (Fabric, Minecraft 26.3)

Drawers + a Tom's Simple Storage-style network + cross-dimensional tablets and item links +
Ender IO-style item and fluid conduits + **all-in-one tools** (AIOTs) + **Nexium**, a tier above netherite + the
**Essentials**: the quality-of-life features that used to be the
separate Nexus Essentials mod (no creeper or ghast block damage, endermen leave blocks alone, an item and XP magnet
with its own trinket slot, a wings slot for an elytra, protection enchantments on elytra, netherite calms piglins,
a bedrock-breaking elixir, picking up mobs, Fortune on ancient debris and a 1-block iron farm).

Every item, the Essentials ones included (Magnet, Elixir of Bedrock Breaking, Iron Farm), is in the
**Storage Nexus** creative tab.

## Coming from Nexus Essentials

Nexus Essentials is now part of Storage Nexus. **Delete `nexus-essentials-*.jar` from your `mods` folder** when you
install this version. If one is left behind, Fabric skips it without a word (Storage Nexus provides the
`nexusessentials` mod id now), so it does no harm, but it is dead weight.

Nothing in your worlds changes: the Essentials items and blocks keep their `nexusessentials:` ids (so
`/give @s nexusessentials:magnet` still works), magnets and elytra stay in the trinket and wings slots, placed Iron
Farms keep running, Captured Mobs keep their mob, and `config/nexusessentials.json` is still the Essentials config.
The only visible changes: the Essentials items moved from the vanilla creative tabs to the Storage Nexus tab, and
the magnet key bind is listed under Storage Nexus in Controls (it stays on the key you chose).

## Building

Any Java 17+ is enough to start the build: Gradle runs itself on JDK 25 and compiles with JDK 25,
using one you have installed or downloading one automatically (see
`gradle/gradle-daemon-jvm.properties`). The first build also downloads Minecraft and Fabric, so it
takes several minutes.

```
# Windows
gradlew.bat build
# macOS / Linux
./gradlew build
```

The mod jar ends up in `build/libs/storage-nexus-1.3.0.jar` (not the `-sources` one). Put it in
your `mods` folder next to **Fabric API** for 26.3. It is needed on both the client and the server.

Built and tested against Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3 and
Loom 1.19.3. To move to a newer version, update `gradle.properties` from
https://fabricmc.net/develop.

### Tests

`./gradlew build` also runs the unit tests and the in-game tests in `src/gametest` on a headless
server (about 110 tests covering drawers, controllers, terminals, the bus, level emitter, barrels,
conduits, Dimensional links, the AIOTs, Nexium and every Essentials feature). Run just those with `./gradlew runGameTest`.
The Essentials tests run as their own batch (test environment `storagenexus:essentials`), because one sets off TNT
and the magnet tests pull items from 16 blocks away.

`SaveLoadGameTest` saves and reloads every block entity in the mod, the way reopening a world does, changing one
thing it keeps at a time: each must change what is saved and load back exactly. A new block entity type with no case
there fails the test.

There are also client tests that open a real Minecraft client (`./gradlew runClientGameTest`): one opens every
screen and clicks the terminal grid; one fills the trinket and wings slots and checks where they sit in the survival
and creative inventories; one checks the magnet's key bind is on the Key Binds screen under Storage Nexus; one puts the
AIOTs and Nexium gear on the hotbar, checks a diamond AIOT's tooltip, opens the tool menu with its key and clicks it
to Silk Touch, checks a Nexium pickaxe gives Haste II and shows the Nexium armor worn; and one saves and quits a world with filled barrels
and reopens it. They take screenshots as they go. They need a window
(or a virtual display such as Xvfb), so they are not part of `build`; CI runs them in a separate `client-test` job
on Xvfb and keeps the screenshots as an artifact. Add `-PwithJei` to run them with JEI installed, which also presses
JEI's "+". The tests live in their own test mod and are never part of the release jar.

### IDs

The storage features use the `storagenexus:` namespace. Everything in the Essentials (code in
`net.storagenexus.essentials`) uses `nexusessentials:`, including anything added to it later, so old Nexus
Essentials worlds keep loading and the two never mix.

## How it works in game

Every item's tooltip says what it does, and the **Storage Nexus Guide** (given once when you join a
world, or crafted from a book and any drawer) explains everything in game.

### Drawers
* 1x1 holds **54 stacks**, 1x2 holds **27 stacks** per slot, 2x2 holds **13.5 stacks** per slot.
* Right-click the front to insert, double-click to insert everything matching, left-click to take
  a stack (sneak for one item). Every click counts, however fast you click; holding the button
  takes just one. Break drawers from the side.
* A bar under each section's count shows how full it is, counting upgrades: green under 75%, yellow
  to 90%, red from 90%. Empty sections have no bar (a locked empty one shows an empty track). Fluid
  drawers have the same bar; a compacting drawer has one bar for its shared pool, under the top form.
* Right-click with an upgrade to install it. To see or remove upgrades, right-click the side or top
  of a drawer with an empty hand (or sneak + right-click it). The
  Lock Upgrade has its own slot, and a locked drawer shows a padlock on its front. A drawer with a
  Void Upgrade shows a marker in the opposite corner, and the upgrade's tooltip warns it destroys overflow.
* Capacity upgrades add together: Iron x2, Gold x4, Diamond x8, Emerald x16, Netherite x32.
  The upgrade screen shows the multiplier, what each section holds and the whole drawer's total
  (stacks, or buckets for fluid drawers). Sections share the drawer: a 1x1 holds 54 stacks, a 1x2
  27 per section and a 2x2 13.5 per section, all times the multiplier.
* With all four slots taken, even on a full drawer, a higher capacity upgrade swaps out the weakest one:
  right-click the drawer with it (the old upgrade drops), click a slot with a stack of them, or
  shift-click one in (the old upgrade goes to your inventory, or drops if it is full).
* **Fluid Drawers** hold 32 buckets of one fluid.
* Breaking a drawer or fluid drawer keeps its contents and upgrades in the dropped item
  (like a shulker box); the tooltip shows what is inside. Place it to get everything back.
* **Compacting Drawer**: nuggets/ingots/blocks (any reversible 9:1 or 4:1 recipe), 54 stacks of the
  largest form (e.g. 54 stacks of iron blocks).
* **Drawer Controller**: drawers touching it or each other within **12 blocks** form a cluster.
  Right-click to send items into the drawer already holding them; double-click dumps your inventory.
  Right-click with an empty hand to see how many drawers it controls and outline its reach for a
  minute (click again to hide).
  **Controller Slave**: place it touching the drawers within reach; drawers within 12 blocks of
  it join too, and slaves can chain.

### Barrels
* **Iron, Gold, Diamond, Emerald and Netherite Barrels** hold 45, 66, 84, 102 and 120 slots: Iron is
  5 rows of 9, and from Gold on every slot grid is full, 6 rows of 11, 14, 17 and 20. Bigger barrels get a wider screen, never taller than a double chest, each in its tier's colour. Right-click a barrel with the matching **Barrel
  Upgrade** to upgrade it in place: Iron upgrades a vanilla barrel, Gold an Iron Barrel, Diamond a Gold
  Barrel, Emerald a Diamond Barrel, Netherite an Emerald Barrel. What is inside stays inside.

### Simple network
* **Storage Connector** reads every inventory touching it. **Filtered Connector** reads only the one it
  faces, through a filter. Both have a priority (click them) and, like the cable, arms towards
  whatever they link to.
* **Inventory Cable** links connectors and terminals. No power.
* **Import/Export Bus**: one block with a mode switch. Placed on an inventory or tank, **Import** pulls its
  items and fluids into the network and **Export** pushes them out (32 items or a bucket every half
  second). An optional filter limits what moves; no filter means everything.
* **Level Emitter**: gives a redstone signal while the network holds at least (or fewer than) an
  amount of one item. Right-click it to pick the item, the amount and the mode. Pick a bucket of
  something to count that fluid in buckets (plus any filled buckets).
* **Crafting Terminal**: fluids and items in one grid, as many rows as fit your
  screen; search (`@modname` to filter by mod) and sort. Left-click takes a stack, right-click one
  item, shift-click to your inventory; holding items, left puts them all in and right puts one in.
  Empty bucket on a fluid fills it; a filled bucket is poured in with left-click.
  The arrow button flips the sort direction, your sort is remembered for the session, and an item's
  tooltip shows its exact amount in the network.
* Next to the result slot the terminal shows how full the network is: items above, fluids below,
  with exact numbers on hover. A compacting drawer counts once, in its smallest form.
* The red **X** next to the crafting grid empties it: items go into the network, else your inventory,
  else onto the floor.
* **JEI / REI**: their "+" button on a crafting recipe fills the Crafting Terminal's grid from the
  network (then your inventory), and turns red when the ingredients aren't there. Neither mod is required.
* The **Crafting Terminal** has the vanilla recipe book: recipes you can make from the network show
  as craftable, and clicking one fills the grid from the network (then your inventory).

### Tablets
* Sneak + right-click a Crafting Terminal with a **Crafting Storage Tablet** to link it. No range limit, any dimension,
  as long as the terminal's chunk is loaded.
* **Dimensional Anchor** loads a 3x3 chunk area. Right-click to switch between Always and On-demand
  (loads only while a tablet is open). It keeps them loaded with a chunk ticket of its own, so it never unloads
  chunks you forced with `/forceload` or that another mod keeps loaded. Worlds from before 1.1.4 switch over by
  themselves the first time they start.

### Dimensional Input / Output
* Click an Input with a **Dimensional Link Card**, then click Outputs. Each link gets a unique name
  (random, or rename the card in an anvil first). Rename later in either block's screen.
* To move an Input, break it and click the new one with the same card: it takes over the link and
  all its Outputs.
* Look straight at an Input or Output to see its link name for 3 seconds.
* Inputs take fluids too (pipes, conduits or a Fluid Intake can feed them). Outputs push into everything around them, on any side: inventories, fluid storage, storage-network blocks and
  item conduits (which carry the items to their Insert sides on the same colour). Inputs are
  skipped. In the Output's screen, the D/U/N/S/W/E buttons turn sides on and off (green = on);
  hover one to see what is on that side. Outputs can have a filter so one Input can split items by type. If nothing can take an
  item, the Input's buffer backs up.
* Transfer Speed Upgrade in the Input: 1 = 16 items per move, 2 = full stacks.

### Conduits
* Item and Fluid conduits (Tier 1 and 2) share one block. Tier 2 conduits show a green trim when
  placed. Fluid: 100 mB/tick (Tier 1), 2000 mB/tick (Tier 2).
* Every side has two colour channels: an **insert** colour and an **extract** colour. What a side
  extracts goes to insert sides with the same colour, so on a side set to Both, in and out can use
  different colours and stay separate.
* Each arm to an inventory shows its channels as rings: the inner ring is the insert colour, the outer
  ring (nearer the block) the extract colour; item rings come before fluid rings. Arms between
  conduits have no rings.
* **Wrench**: right-click a side to cycle its mode, sneak + right-click for the full settings
  (mode, insert and extract colour channels, redstone control, priority, round-robin, filter, speed upgrades). The
  settings screen also names what is on the selected side.
* Right-click a conduit with any full block to give it a **facade**; wrench it to take it off.
* **Fluid Intake** pulls from the source blocks touching it (infinite water is never drained). Right-click it
  with a bucket to make it take only that fluid (sneak + empty hand to clear). Put an **Infinite Lava
  Source** (obsidian, lava buckets and a nether star) next to it for endless lava.
* **Basic Filter** (whitelist/blacklist, exact match) and **Advanced Filter** (matches by item tag).
  Fluids are filtered by putting their bucket in the filter.

### All-in-one tools (AIOT)
* A pickaxe, axe, shovel, hoe and sword in one item: **Copper, Iron, Golden, Diamond, Netherite and Nexium AIOT**
  (`storagenexus:<material>_aiot`).
* **Crafting**: all five tools of one material (pickaxe, axe, shovel, hoe and sword) anywhere in the crafting
  grid. What you put in carries over:
  * every enchantment, at the highest level any of the five tools had it;
  * **Silk Touch and Fortune both stay**: Fortune is active and Silk Touch is kept inactive (the tooltip lists it
    as "Silk Touch (inactive)"). Switch between them in the **tool menu**;
  * of Sharpness, Smite and Bane of Arthropods, the highest level stays (Sharpness wins a tie);
  * curses stay, so crafting never gets rid of one;
  * durability: the AIOT has exactly the uses the five tools had left, so five fresh tools make a full AIOT;
  * the anvil repair cost of the most expensive tool.
* **Netherite**: at a smithing table, a Diamond AIOT, a netherite upgrade template and a **netherite block**. Its
  enchantments (the inactive one too) and wear stay, as with vanilla's netherite upgrade. Five netherite tools also
  make one in the crafting grid.
* **Nexium**: at a smithing table, a Netherite AIOT, a netherite upgrade template and a **Block of Nexium**; or five
  Nexium tools in the crafting grid.
* **Durability**: five times the material's tool durability, the five tools' uses added together.

  | | Copper | Iron | Golden | Diamond | Netherite | Nexium |
  |---|---|---|---|---|---|---|
  | Durability | 950 | 1,250 | 160 | 7,805 | 10,155 | 12,500 |
  | Attack damage | 9 | 9 | 7 | 9 | 10 | 11 |

* **Combat**: as hard as that material's axe at a sword's speed (1.6 attacks a second), with the sword's sweeping
  attack, and it disables shields like an axe. It loses 1 durability per hit and per block.
* **Mining**: everything that material's pickaxe, axe, shovel, hoe and sword mine, as fast as the best of them, with
  the same material limits (an iron AIOT can't mine obsidian, a diamond one can). It is in the vanilla `pickaxes`,
  `axes`, `shovels`, `hoes` and `swords` item tags, so it takes every tool and sword enchantment, and other mods
  treat it as each tool. A Diamond or Netherite AIOT mines bedrock under the Elixir of Bedrock Breaking.
* **Right-click** strips logs, scrapes or unwaxes copper and makes paths (or tills a path into farmland);
  **sneak + right-click** tills grass and dirt into farmland instead. Right-click a lit campfire to put it out.
* At an **anvil**, an AIOT holding one of Silk Touch and Fortune takes the other from a book (or another AIOT): the
  one it was using stays active and the new one is added inactive. Vanilla tools keep the vanilla rule.
* A grindstone takes the inactive enchantment off with the others. Piglins love the Golden AIOT.

### Tool menu
* Hold an AIOT or a Nexium tool and press **G** (rebind it under Controls > Key Binds > Storage Nexus, next to the
  magnet). Press it again or Esc to close; it doesn't pause the game.
* **Mining**: on an AIOT holding both Silk Touch and Fortune, switches which one is active. The AIOT's gem shows it on
  the hotbar: **green for Fortune, blue for Silk Touch** (purple when it doesn't hold both). Whichever is active
  decides the drops, exactly as on a vanilla pickaxe.
* **Haste II**: on a Nexium pickaxe, axe, shovel, hoe or AIOT, turns its Haste II on or off. The setting is saved on
  the tool, and its tooltip says whether it is on.
* A message above the hotbar confirms each change. Switches that don't apply to the held tool are greyed out, with a
  tooltip saying why.
* Every AIOT has the Silk Touch / Fortune switch built in and every Nexium mining tool its Haste II: neither is an
  add-on. The menu is where optional modules (area mining, Nexus Link and the like) will go if they are added later.

### Nexium
* The tier above netherite. **Nexium Ingot**: a block of diamond, a block of emerald, a block of netherite and a
  netherite upgrade smithing template in a crafting table make **3 ingots** (the template is used up). Nine make a **Block of Nexium**
  (a beacon base, mined like a netherite block); beacons take the ingots as payment.
* **Gear**: at a smithing table, a netherite upgrade template, the netherite piece and a Nexium ingot. Enchantments
  and wear stay. There is a Nexium **sword, pickaxe, axe, shovel, hoe, spear, helmet, chestplate, leggings and
  boots**, and the Nexium AIOT. Everything Nexium survives fire and lava.
* **Tools**: netherite's mining speed and level, 2,500 uses (netherite 2,031), one more attack damage than
  netherite (sword 9, axe 11, spear 6), enchantability 18. The spear charges like vanilla spears, one step past
  netherite on each spear stat.
* **Haste II**: the Nexium pickaxe, axe, shovel, hoe and AIOT give the real **Haste II** effect while held in the main
  hand, so with Efficiency V they mine exactly as fast as netherite with Efficiency V under a Haste II beacon (stone
  and netherrack break instantly). Like the beacon's, it also speeds up attacks by 20%, and it doesn't stack with a
  beacon's Haste II. The effect shows a steady infinity sign and goes as soon as the tool is put away; a beacon's or a
  command's Haste is never taken off. Turn it off in the tool menu.
* **Armor**: 4 / 9 / 7 / 4 armor (helmet, chestplate, leggings, boots: 24 in all, netherite 20), 5 toughness per
  piece (20 in all, the game's maximum), 0.15 knockback resistance per piece and durability 484 / 704 / 660 / 572.
  It takes armor trims, and like netherite it keeps piglins calm (Essentials).

## Essentials

The features that used to be the Nexus Essentials mod. The Guide's last pages cover them too.

### Explosions and endermen
* Creeper and ghast explosions break no blocks, and ghast fireballs light no fires. They still hurt and push
  players and mobs, and still look like full-size explosions.
* They also leave item frames, paintings, armor stands, items on the ground, minecarts and boats alone.
* TNT, beds, respawn anchors, withers and end crystals work as in vanilla.
* Endermen never pick up blocks. One that was already carrying a block before you added the mod can still put it down.

### Trinket and wings slots
* Two new slots sit in a column above the offhand slot in your inventory (and right of the armor in the
  creative inventory tab): the **trinket slot** (top) takes the magnet, and the **wings slot** (bottom) takes an
  elytra (or any modded item that glides like one).
* Shift-click a magnet or elytra to send it there, or right-click it in your hand to put it on.
* An elytra in the wings slot glides exactly like one in the chest slot, so you can wear a chestplate too.
  It wears down the same way, Unbreaking and Mending work on it, and its protection enchantments add to your
  armor's (vanilla's 80% total protection cap still applies). Other players see it on you, and it hides your cape like a normal elytra.
* Curse of Binding keeps an item in the slot, as it does in armor slots.
* The slots are saved with your player. On death they drop like the rest of your inventory (Curse of Vanishing
  destroys), unless keepInventory is on.

### Magnet
* Crafted from 5 iron ingots, 1 redstone and 1 lapis lazuli in a U shape:
  ```
  R . L
  I . I
  I I I
  ```
* Works only while it is in your **trinket slot**. It picks up every item and experience orb within range,
  through walls, using the normal pickup, so items fly into you and full inventories, pickup delays and
  item owners still apply. It pulls back what you drop yourself too, once the usual 2-second pickup delay is
  over, so press **M** to switch it off before throwing something away.
* **Sneak + right-click** it (in your hand) to change the range: 4, 8, 12, 16 blocks (up to `magnetMaxRange`).
* Press **M** to switch it on or off (rebind under Controls > Key Binds > Storage Nexus). It glints while it is on.

### Elytra enchanting
* At an anvil, elytra take Protection, Fire Protection, Blast Protection and Projectile Protection from enchanted
  books, and can hold all four at once. Chestplates keep the vanilla rule of one protection type.

### Piglins
* Wearing any piece of netherite armor counts as wearing gold armor: piglins stay neutral.
  (Bartering still uses gold ingots; the item tag `nexusessentials:calms_piglins` lists the armor.)

### Elixir of Bedrock Breaking
* Crafted from 2 diamonds, 2 obsidian and a glass bottle:
  ```
  . O .
  D B D
  . O .
  ```
* Drinking it gives **Bedrock Breaker** for 5 minutes (`bedrockBreakerSeconds`). While it lasts, bedrock mines
  like obsidian with a diamond or netherite pickaxe (Efficiency and Haste help). Bedrock drops nothing, and any
  bedrock can be broken, including the bottom layer above the void, so be careful.
* Which blocks it affects is the block tag `nexusessentials:bedrock_breaker_mineable`.

### Picking up mobs
* **Sneak + right-click a mob with an empty hand** to pick it up. You get a Captured Mob item that looks like its
  spawn egg (with a glint) and keeps everything about the mob: health, name, equipment, saddle and chests,
  villager profession and trades, horse stats, owner, age and so on.
* **Right-click a block** with the item to let the mob out there. A released mob never despawns.
* The Ender Dragon and the Wither can't be picked up (entity tag `nexusessentials:cannot_be_picked_up`).
* Only a pet's owner can pick it up (`onlyOwnersPickUpPets`). Players in Adventure mode and non-operators
  inside spawn protection can't pick up mobs.
* Sneak + right-clicking a tamed horse, donkey or llama with an **item** in your hand still opens its inventory.
  With an empty hand it picks the animal up.
* A picked-up villager gives up its bed and workstation so other villagers can use them; it finds new ones when let out.
* Riders get off and a lead drops when a mob is picked up.
* Monsters can't be let out on Peaceful (the item stays in your hand).
* A dropped Captured Mob item doesn't despawn and can't be destroyed by fire, lava, explosions or cactus.

### Ancient debris
* Ancient debris drops **netherite scrap** instead of itself: 2 per block, multiplied by Fortune the same way as
  diamond ore, so Fortune I/II/III average 2.67 / 3.5 / 4.4 scrap (up to 8). Silk Touch still drops the block.

### Iron Farm
* **Making one**: put a chest down, rename another chest **Nexus Iron Farm** in an anvil and place it next to the
  first to make a double chest (only one half needs the name; upper and lower case don't matter). Fill it with exactly:

  | Material | Amount | Counts |
  |---|---|---|
  | Zombies | 4 | Zombie, husk, drowned or zombie villager: spawn eggs or Captured Mobs, any mix |
  | Villagers | 12 | Villager spawn eggs or Captured Mob villagers |
  | Torches | 33 | Torch, soul torch or copper torch |
  | Slabs | 20 | Any slab |
  | Signs | 16 | Any sign or hanging sign |
  | Trapdoors | 12 | Any trapdoor |
  | Beds | 12 | Any colour |
  | Smooth Stone | every other slot | Full stacks of 64 |

  Amounts are totals, so a material can be split over slots. Every slot of the double chest must be used: with spawn
  eggs that is 36 stacks of smooth stone, with Captured Mobs (which don't stack) 22. The moment the chest holds exactly
  this, the chest and everything in it disappear and the **Iron Farm** pops out. Close the chest before it's ready
  and the chat says what's missing, what's too much and what doesn't belong (an empty named chest lists the whole recipe).
* **Place it anywhere.** Every 7.5 seconds it makes a golem's drops: 3-5 iron and 0-2 poppies, about **1,900 iron an
  hour** (four 3-villager farms). Right-click it to see the output, a bar to the next golem and the bread slot.
* **Hoppers and pipes take the output from below**, and only from below. Nothing can be put in by hopper.
* **Bread**: put bread in the bread slot and the farm works **10x faster** (about 19,000 iron an hour), eating one loaf
  every 30 seconds. When the last loaf is eaten, the bread slot locks for **10x as long as the boost lasted** (a full
  stack of 64 is 32 minutes of boost, then 5 h 20 min of cooldown). The farm keeps working at normal speed during the
  cooldown. Hover the empty bread slot to see how long is left.
* When the output is full, or a **redstone signal** reaches it, the farm waits and doesn't eat bread (a bread cooldown
  keeps counting down). The screen says which.
* Breaking it drops the Iron Farm with its boost and cooldown kept (the tooltip shows them), so moving it doesn't skip a
  cooldown; the output and bread drop on the ground. A pickaxe breaks it faster, but it drops whatever breaks it.
* The materials are item and entity tags (`nexusessentials:iron_farm_zombies`, `iron_farm_villagers`, `iron_farm_torches`,
  `iron_farm_slabs`, `iron_farm_signs`, `iron_farm_trapdoors`, `iron_farm_beds`, `iron_farm_filler`), so a data pack can change them.

### Essentials settings

`config/nexusessentials.json` is created on first launch (the same file the old Nexus Essentials mod used, so
existing settings carry over). Changes take effect after a restart. On a server, the server's file is the one that
counts.

| Setting | Default | What it does |
|---|---|---|
| `creepersBreakBlocks` | `false` | Creeper explosions break blocks (vanilla) |
| `ghastFireballsBreakBlocks` | `false` | Ghast fireballs break blocks and start fires (vanilla) |
| `explosionsSpareDecorations` | `true` | Creeper and ghast explosions spare item frames, paintings, armor stands, dropped items, minecarts and boats |
| `endermenPickUpBlocks` | `false` | Endermen pick up blocks (vanilla) |
| `magnetMaxRange` | `16` | Largest magnet range, 4 to 64 blocks |
| `magnetPullsXp` | `true` | Magnets pull experience orbs too |
| `elytraProtectionEnchantments` | `true` | Elytra accept all four protection enchantments |
| `netheriteArmorCalmsPiglins` | `true` | Netherite armor keeps piglins calm |
| `bedrockBreakerSeconds` | `300` | How long the elixir lasts |
| `mobPickUp` | `true` | Sneak + right-click picks up mobs |
| `onlyOwnersPickUpPets` | `true` | Only a pet's owner can pick it up |
| `ancientDebrisDropsScrap` | `true` | Ancient debris drops Fortune-multiplied netherite scrap |
| `ironFarmChestName` | `"Nexus Iron Farm"` | The name a double chest needs to become an Iron Farm |
| `ironFarmGolemTicks` | `150` | Ticks between golems (150 = 7.5 s, about 1,900 iron an hour) |
| `ironFarmBreadSpeedUp` | `10` | How many times faster bread makes the farm |
| `ironFarmBreadSeconds` | `30` | Seconds of boost per loaf |
| `ironFarmCooldownMultiplier` | `10` | Bread cooldown, as a multiple of how long the boost lasted |
| `ironFarmPoppies` | `true` | Golems drop 0-2 poppies as well as iron |
