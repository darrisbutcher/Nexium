# Changelog

All notable changes to Nexium (mod id `storagenexus`), newest first. The version is `mod_version` in `gradle.properties`.
Pull requests are squash merged, so each one is a single commit on `main`.

## 1.12.1

* **Guide**: the Essentials contents now link the Higher levels, Elevator and Master Librarian pages; the Mobs chapter points
  to the Nexus Librarian chest; the AIOT chapter has a page on the mining outline and the readout.

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
