# Storage Nexus (Fabric, Minecraft 26.3)

Drawers + a Tom's Simple Storage-style network + cross-dimensional tablets and item links +
Ender IO-style item and fluid conduits.

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

The mod jar ends up in `build/libs/storage-nexus-1.0.22.jar` (not the `-sources` one). Put it in
your `mods` folder next to **Fabric API** for 26.3.

Built and tested against Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3 and
Loom 1.19.3. To move to a newer version, update `gradle.properties` from
https://fabricmc.net/develop.

### Tests

`./gradlew build` also runs the unit tests and the in-game tests in `src/gametest` on a headless
server (about 40 tests covering drawers, controllers, terminals, the bus, level emitter, barrels,
conduits and Dimensional links). Run just those with `./gradlew runGameTest`.

There is also a client test that opens a real Minecraft client, opens every screen and clicks the
terminal grid: `./gradlew runClientGameTest`. It needs a window (or a virtual display such as Xvfb),
so it is not part of `build`. Add `-PwithJei` to run it with JEI installed, which also presses JEI's "+". The tests live in their own test mod and are never part of the release jar.

## How it works in game

Every item's tooltip says what it does, and the **Storage Nexus Guide** (given once when you join a
world, or crafted from a book and any drawer) explains everything in game.

### Drawers
* 1x1 holds **54 stacks**, 1x2 holds **27 stacks** per slot, 2x2 holds **13.5 stacks** per slot.
* Right-click the front to insert, double-click to insert everything matching, left-click to take
  a stack (sneak for one item). Break drawers from the side.
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
* **Iron, Gold, Diamond, Emerald and Netherite Barrels** hold 45, 63, 81, 99 and 117 slots (a vanilla barrel has 27;
  every tier adds 18). Bigger barrels get a wider screen, never taller than a double chest, each in its tier's colour. Right-click a barrel with the matching **Barrel
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
  (loads only while a tablet is open).

### Dimensional Input / Output
* Click an Input with a **Dimensional Link Card**, then click Outputs. Each link gets a unique name
  (random, or rename the card in an anvil first). Rename later in either block's screen.
* To move an Input, break it and click the new one with the same card: it takes over the link and
  all its Outputs.
* Look straight at an Input or Output to see its link name for 3 seconds.
* Inputs take fluids too (pipes or fluid conduits can feed them). Outputs push into everything around them, on any side: inventories, fluid storage, storage-network blocks and
  item conduits (which carry the items to their Insert sides on the same colour). Inputs are
  skipped. In the Output's screen, the D/U/N/S/W/E buttons turn sides on and off (green = on);
  hover one to see what is on that side. Outputs can have a filter so one Input can split items by type. If nothing can take an
  item, the Input's buffer backs up.
* Transfer Speed Upgrade in the Input: 1 = 16 items per move, 2 = full stacks.

### Conduits
* Item and Fluid conduits (Tier 1 and 2) share one block. Tier 2 conduits show a green trim when
  placed. Fluid: 100 mB/tick (Tier 1), 2000 mB/tick (Tier 2).
* Each connected conduit arm shows a small ring in its side's colour channel (item rings nearer the
  core, fluid rings further out).
* **Wrench**: right-click a side to cycle its mode, sneak + right-click for the full settings
  (mode, colour channel, redstone control, priority, round-robin, filter, speed upgrades). The
  settings screen also names what is on the selected side.
* Right-click a conduit with any full block to give it a **facade**; wrench it to take it off.
* **Fluid Intake** pulls a bucket a second from the source blocks touching it (infinite water is never
  drained) into its own 4-bucket buffer. It doesn't push anywhere: an **Import/Export Bus** in import mode
  facing it moves the fluid into a network (fluid conduits can pull from it too). Right-click it
  with a bucket to make it take only that fluid (sneak + empty hand to clear). Put an **Infinite Lava
  Source** (obsidian, lava buckets and a nether star) next to it for endless lava.
* **Basic Filter** (whitelist/blacklist, exact match) and **Advanced Filter** (matches by item tag).
  Fluids are filtered by putting their bucket in the filter.
