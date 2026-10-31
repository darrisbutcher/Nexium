# Storage Nexus (Fabric, Minecraft 26.3)

Drawers + a Tom's Simple Storage-style network + cross-dimensional tablets and item links +
Ender IO-style item, fluid and redstone conduits.

## Building

You need **JDK 25** installed (Minecraft 26.x requires Java 25).

```
# Windows
gradlew.bat build
# macOS / Linux
./gradlew build
```

The mod jar ends up in `build/libs/storage-nexus-1.0.0.jar`. Put it in your `mods` folder next to
**Fabric API** for 26.3.

If Gradle can't find a version, update the numbers in `gradle.properties` from
https://fabricmc.net/develop (Loom, loader and Fabric API for 26.3). If Loom says it needs a newer
Gradle, change the version in `gradle/wrapper/gradle-wrapper.properties`.

### First-build notes

This code was written without access to the 26.3 jars, so a few API names may have moved.
The most likely spots, in order:

1. `client/render/*` — the block entity renderers (drawer front display, link labels, facades).
   If they won't compile and you just want to play, delete the `client/render` folder and the
   `BlockEntityRenderers.register(...)` lines in `StorageNexusClient`. Everything still works,
   you just lose the item icons on drawer fronts.
2. `appendHoverText(...)` tooltip signatures and `GuiGraphics` tooltip calls in the screens.
3. `SavedDataType` constructor in `LinkRegistry` / `AnchorRegistry`.
4. `ResourceKey#identifier()` in `StorageTabletItem`.

## How it works in game

### Drawers
* 1x1 holds **54 stacks**, 1x2 holds **27 stacks** per slot, 2x2 holds **13.5 stacks** per slot.
* Right-click the front to insert, double-click to insert everything matching, left-click to take
  a stack (sneak for one item). Break drawers from the side.
* Right-click with an upgrade to install it; sneak + empty hand opens the 4 upgrade slots.
* Capacity upgrades add together: Iron x2, Gold x4, Diamond x8, Netherite x16.
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
* Outputs push into the block they face, including any storage-network block. Outputs can have a filter
  so one Input can split items by type. If nothing can take an item, the Input's buffer backs up.
* Transfer Speed Upgrade in the Input: 1 = 16 items per move, 2 = full stacks.

### Conduits
* Item (basic, ender), Fluid (basic, pressurized, ender) and Redstone conduits share one block.
* **Wrench**: right-click a side to cycle its mode, sneak + right-click for the full settings
  (mode, colour channel, redstone control, priority, round-robin, filter, speed upgrades).
* Right-click a conduit with any full block to give it a **facade**; wrench it to take it off.
* **Fluid Intake** pulls from source blocks (infinite water is never drained).
* **Basic Filter** (whitelist/blacklist, exact match) and **Advanced Filter** (matches by item tag).
  Fluids are filtered by putting their bucket in the filter.
