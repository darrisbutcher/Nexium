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

The mod jar ends up in `build/libs/storage-nexus-1.0.1.jar` (not the `-sources` one). Put it in
your `mods` folder next to **Fabric API** for 26.3.

Built and tested against Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3 and
Loom 1.19.3. To move to a newer version, update `gradle.properties` from
https://fabricmc.net/develop.

## How it works in game

### Drawers
* 1x1 holds **54 stacks**, 1x2 holds **27 stacks** per slot, 2x2 holds **13.5 stacks** per slot.
* Right-click the front to insert, double-click to insert everything matching, left-click to take
  a stack (sneak for one item). Break drawers from the side.
* Right-click with an upgrade to install it; sneak + empty hand opens the 4 upgrade slots.
* Capacity upgrades add together: Iron x2, Gold x4, Diamond x8, Netherite x16.
* Breaking a drawer, fluid drawer or fluid tank keeps its contents and upgrades in the dropped item
  (like a shulker box); the tooltip shows what is inside. Place it to get everything back.
* **Compacting Drawer**: nuggets/ingots/blocks (any reversible 9:1 or 4:1 recipe), 54 stacks of the
  smallest form.
* **Drawer Controller**: drawers touching it or each other within **16 blocks** form a cluster.
  Right-click to send items into the drawer already holding them; double-click dumps your inventory.
  **Controller Slave** restarts the 16-block reach.

### Simple network
* **Storage Connector** reads every inventory touching it. **Filtered Connector** reads only the one it
  faces, through a filter. Both have a priority (click them).
* **Inventory Cable** links connectors and terminals. No power.
* **Storage Terminal / Crafting Terminal**: search (`@modname` to filter by mod), sort, Items and
  Fluids tabs. Click with a bucket on the Fluids tab to fill/empty.

### Tablets
* Sneak + right-click a terminal with a **Storage Tablet** to link it. No range limit, any dimension,
  as long as the terminal's chunk is loaded.
* **Dimensional Anchor** loads a 3x3 chunk area. Right-click to switch between Always and On-demand
  (loads only while a tablet is open).

### Dimensional Input / Output
* Click an Input with a **Dimensional Link Card**, then click Outputs. Each link gets a unique name
  (random, or rename the card in an anvil first). Rename later in either block's screen.
* To move an Input, break it and click the new one with the same card: it takes over the link and
  all its Outputs.
* Outputs push into the block they face, including any storage-network block. Outputs can have a filter
  so one Input can split items by type. If nothing can take an item, the Input's buffer backs up.
* Transfer Speed Upgrade in the Input: 1 = 16 items per move, 2 = full stacks.

### Conduits
* Item (basic, ender) and Fluid (basic, pressurized, ender) conduits share one block.
* **Wrench**: right-click a side to cycle its mode, sneak + right-click for the full settings
  (mode, colour channel, redstone control, priority, round-robin, filter, speed upgrades).
* Right-click a conduit with any full block to give it a **facade**; wrench it to take it off.
* **Fluid Intake** pulls from source blocks (infinite water is never drained).
* **Basic Filter** (whitelist/blacklist, exact match) and **Advanced Filter** (matches by item tag).
  Fluids are filtered by putting their bucket in the filter.
