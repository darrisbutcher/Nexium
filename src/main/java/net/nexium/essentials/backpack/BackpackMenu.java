package net.nexium.essentials.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.Holder;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.nexium.essentials.registry.ModMenus;
import net.nexium.essentials.slots.ExtraSlots;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A Backpack's screen: its {@value BackpackModules#SLOTS} module slots (each with an on/off switch), its
 * {@value BackpackContents#SIZE} slots, and the player's inventory. The slots write straight back to the Backpack's
 * data (on the server) after every change. The Backpack is either the one worn in the backpack slot
 * ({@link Backpacks#EQUIPPED}) or the one in the inventory slot it was opened from, which is locked in place while the
 * screen is open.
 *
 * <p>Every stackable item stacks to {@value BackpackContents#STACK_MAX} in the Backpack's slots.
 *
 * <p>With a Crafting module installed and on, a panel to the right of the screen has a 3x3 crafting grid and its result slot. The
 * grid refills from the Backpack's own slots after every craft, and what is left in it goes back into the Backpack when the module
 * is taken out or switched off, or the screen closes. A trash slot (always there, no module) holds the last item put in it until
 * another goes in or the screen closes.
 */
public class BackpackMenu extends AbstractCraftingMenu {
    /** Button id of the first module's on/off switch; module slot {@code n} is {@code BTN_TOGGLE_MODULE + n}. */
    public static final int BTN_TOGGLE_MODULE = 0;
    /** Button ids of the Feeding module's settings: its level one step lower (or higher), and its never-waste switch. */
    public static final int BTN_FEEDING_LEVEL = 10, BTN_FEEDING_LEVEL_BACK = 11, BTN_FEEDING_WASTE = 12;
    /** Hides or shows the crafting grid (the panel's close button, and the small button that brings it back). */
    public static final int BTN_CRAFTING_HIDE = 13;

    public static final int MODULE_X = 182, MODULE_Y = 18, MODULE_ROW = 20;
    public static final int GRID_X = 8, GRID_Y = 18;
    /** The player's inventory sits below the Backpack's rows. */
    public static final int INVENTORY_Y = 104;
    public static final int WIDTH = 256, HEIGHT = INVENTORY_Y + 82;
    /** The Crafting module's panel, right of the main one: where it starts, how wide it is, its grid and its result slot. */
    public static final int SIDE_X = WIDTH, SIDE_W = 108;
    public static final int CRAFT_X = SIDE_X + 8, CRAFT_Y = 18, RESULT_X = SIDE_X + 84, RESULT_Y = CRAFT_Y + 18;
    /** The trash slot: right under the module column, at its right-hand side. */
    public static final int TRASH_X = MODULE_X + 50, TRASH_Y = MODULE_Y + BackpackModules.SLOTS * MODULE_ROW - 2;

    private final Player player;
    /** How many slots before the player's inventory are the menu's own (modules and storage), and where the player's inventory starts. */
    private int customSlotCount, playerSlotStart = -1;
    private final Slot resultSlot;
    private final List<Slot> gridSlots = new ArrayList<>();
    private final Slot trashSlot;
    /** What the trash slot holds: the last item put in, until another goes in or the screen closes. */
    private final SimpleContainer trash = new SimpleContainer(1);
    /** {@link Backpacks#EQUIPPED}, or the inventory slot the Backpack was opened from. */
    private final int source;
    /** Server only: the Backpack stack the menu was opened for, so it closes if that stack is replaced. */
    private final ItemStack opened;
    private boolean loading;

    private final SimpleContainer modules = new SimpleContainer(BackpackModules.SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) onModulesChanged();
        }
    };
    private final SimpleContainer contents = new SimpleContainer(BackpackContents.SIZE) {
        @Override
        public int getMaxStackSize() {
            // The slots limit each stack; this keeps the container from clamping them
            return BackpackContents.STACK_MAX;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            // The container clamps every stack it is given to this, not to the item's own limit
            return BackpackContents.STACK_MAX;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) saveContents();
        }
    };

    /** Both sides: {@code source} is {@link Backpacks#EQUIPPED} or the inventory slot the Backpack was opened from. */
    public BackpackMenu(int id, Inventory inventory, Integer source) {
        super(ModMenus.BACKPACK, id, 3, 3);
        this.player = inventory.player;
        this.source = source;
        this.opened = player.level().isClientSide() ? ItemStack.EMPTY : backpack();
        if (!player.level().isClientSide()) {
            loading = true;
            List<ItemStack> installed = BackpackModules.slots(opened);
            for (int i = 0; i < BackpackModules.SLOTS; i++) modules.setItem(i, installed.get(i));
            List<ItemStack> stored = BackpackContents.read(opened);
            for (int i = 0; i < BackpackContents.SIZE; i++) contents.setItem(i, stored.get(i));
            loading = false;
        }
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            addCustomSlot(new Slot(modules, i, MODULE_X, MODULE_Y + i * MODULE_ROW) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof BackpackModuleItem module && !module.type().retired() && !hasOtherOfType(module, getContainerSlot());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int i = 0; i < BackpackContents.SIZE; i++) {
            // Four even rows of nine
            addCustomSlot(new Slot(contents, i, GRID_X + (i % 9) * 18, GRID_Y + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // No Backpack inside a Backpack
                    return !(stack.getItem() instanceof BackpackItem);
                }

                @Override
                public int getMaxStackSize() {
                    return BackpackContents.STACK_MAX;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return BackpackContents.maxStack(stack);
                }
            });
        }
        addPlayerSlots(inventory, GRID_X, INVENTORY_Y);
        // After the player's slots, so every slot number above is the same as it always was
        resultSlot = addSlot(new BackpackResultSlot(player, RESULT_X, RESULT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) gridSlots.add(addSlot(new GridSlot(craftSlots, col + row * 3, CRAFT_X + col * 18, CRAFT_Y + row * 18)));
        }
        trashSlot = addSlot(new Slot(trash, 0, TRASH_X, TRASH_Y));
    }

    private Slot addCustomSlot(Slot slot) {
        customSlotCount++;
        return addSlot(slot);
    }

    private void addPlayerSlots(Inventory inventory, int left, int top) {
        playerSlotStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, left + col * 18, top + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, left + col * 18, top + 58));
    }

    // ---- The Crafting module ------------------------------------------------------------------------------------

    /** Whether a Crafting module is installed and switched on, whether or not its grid is hidden. Works on both sides: the module slots are synced. */
    public boolean craftingModuleOn() {
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            ItemStack module = modules.getItem(i);
            if (module.getItem() instanceof BackpackModuleItem item && item.type() == BackpackModuleType.CRAFTING
                    && !module.has(net.nexium.registry.ModComponents.MODULE_OFF)) return true;
        }
        return false;
    }

    /** Whether the Crafting module's grid is hidden (remembered on the module, so per Backpack). */
    public boolean craftingHidden() {
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            ItemStack module = modules.getItem(i);
            if (module.getItem() instanceof BackpackModuleItem item && item.type() == BackpackModuleType.CRAFTING
                    && module.has(net.nexium.registry.ModComponents.CRAFTING_HIDDEN)) return true;
        }
        return false;
    }

    /** Whether the crafting grid is there to use: the Crafting module is on and its grid is not hidden. A hidden grid is empty and takes nothing. */
    public boolean craftingActive() {
        return craftingModuleOn() && !craftingHidden();
    }

    /** A slot of the crafting grid: only there while the Crafting module is. */
    private class GridSlot extends Slot {
        GridSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return craftingActive();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return craftingActive();
        }

        @Override
        public boolean mayPickup(Player player) {
            return craftingActive();
        }
    }

    /** The result slot is never part of a double-click's "pick up all" (as in vanilla's crafting table), so clicking it quickly crafts each time. */
    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return slot != getResultSlot() && super.canTakeItemForPickAll(carried, slot);
    }

    /** The result slot: after a craft it refills the grid's emptied slots from the Backpack's own slots. */
    private class BackpackResultSlot extends ResultSlot {
        BackpackResultSlot(Player player, int x, int y) {
            super(player, craftSlots, resultSlots, 0, x, y);
        }

        @Override
        public boolean isActive() {
            return craftingActive();
        }

        @Override
        public boolean mayPickup(Player player) {
            return craftingActive();
        }

        @Override
        public void onTake(Player p, ItemStack stack) {
            ItemStack[] before = new ItemStack[9];
            for (int i = 0; i < 9; i++) before[i] = craftSlots.getItem(i).copy();
            super.onTake(p, stack);
            if (p.level().isClientSide()) return;
            refillGrid(before);
            slotsChanged(craftSlots);
        }
    }

    /** Puts one of the same item (from the Backpack's slots) back in each grid slot a craft emptied. */
    private void refillGrid(ItemStack[] before) {
        for (int i = 0; i < 9; i++) {
            if (!craftSlots.getItem(i).isEmpty() || before[i].isEmpty()) continue;
            for (int j = 0; j < BackpackContents.SIZE; j++) {
                ItemStack stored = contents.getItem(j);
                if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, before[i])) continue;
                craftSlots.setItem(i, contents.removeItem(j, 1));
                break;
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == craftSlots && player instanceof ServerPlayer sp) {
            ServerLevel level = (ServerLevel) sp.level();
            CraftingInput input = craftSlots.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> recipe = craftingActive() ? level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level) : Optional.empty();
            resultSlots.setItem(0, recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY));
            broadcastChanges();
        }
    }

    /** Server: puts the grid's contents back in the Backpack's slots (what does not fit goes to the inventory, then the ground). */
    private void clearGrid() {
        NonNullList<ItemStack> stored = contentsCopy();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;
            ItemStack rest = BackpackContents.insert(stored, stack);
            if (!rest.isEmpty()) player.getInventory().placeItemBackInInventory(rest, false, Prediction.SERVER_ONLY);
        }
        replaceContents(stored);
        resultSlots.clearContent();
        slotsChanged(craftSlots);
    }

    @Override
    public Slot getResultSlot() {
        return resultSlot;
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return gridSlots;
    }

    @Override
    protected Player owner() {
        return player;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    // ---- The recipe book and recipe viewers: ingredients from the Backpack first, then the inventory -------------------

    /** What the recipe book may craft from: the grid, and the Backpack's own slots (the screen adds the inventory). */
    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stacked) {
        super.fillCraftSlotsStackedContents(stacked);
        for (int i = 0; i < BackpackContents.SIZE; i++) {
            ItemStack stored = contents.getItem(i);
            if (!stored.isEmpty() && Inventory.isUsableForCrafting(stored)) stacked.accountStack(stored);
        }
    }

    /** Takes up to {@code amount} of this item (usable for crafting) out of the Backpack's slots. */
    private ItemStack takeFromBackpack(Item item, int amount) {
        ItemStack taken = ItemStack.EMPTY;
        for (int i = 0; i < BackpackContents.SIZE && taken.getCount() < amount; i++) {
            ItemStack stored = contents.getItem(i);
            if (!stored.is(item) || !Inventory.isUsableForCrafting(stored)) continue;
            ItemStack part = contents.removeItem(i, Math.min(amount - taken.getCount(), stored.getCount()));
            if (taken.isEmpty()) taken = part;
            else taken.grow(part.getCount());
        }
        return taken;
    }

    /**
     * Fills the grid for a recipe picked in the recipe book, like vanilla's but taking the ingredients from the Backpack's slots first
     * and from the player's inventory after them.
     */
    @Override
    public RecipeBookMenu.PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear, RecipeHolder<?> holder,
                                                          ServerLevel level, Inventory inventory) {
        if (!craftingActive() || !(holder.value() instanceof CraftingRecipe recipe)) return RecipeBookMenu.PostPlaceAction.NOTHING;
        StackedItemContents available = new StackedItemContents();
        inventory.fillStackedContents(available);
        craftSlots.fillStackedContents(available);
        for (int i = 0; i < BackpackContents.SIZE; i++) {
            ItemStack stored = contents.getItem(i);
            if (!stored.isEmpty() && Inventory.isUsableForCrafting(stored)) available.accountStack(stored);
        }
        if (!available.canCraft(recipe, null)) {
            clearGrid();
            return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
        }
        boolean alreadyPlaced = recipe.matches(craftSlots.asCraftInput(), level);
        int amount;
        if (useMaxItems) {
            amount = available.getBiggestCraftableStack(recipe, null);
        } else if (alreadyPlaced) {
            int smallest = Integer.MAX_VALUE;
            for (Slot s : gridSlots) if (!s.getItem().isEmpty()) smallest = Math.min(smallest, s.getItem().getCount());
            amount = smallest == Integer.MAX_VALUE ? 1 : smallest + 1;
        } else {
            amount = 1;
        }
        List<Holder<Item>> used = new ArrayList<>();
        if (!available.canCraft(recipe, amount, used::add)) return RecipeBookMenu.PostPlaceAction.NOTHING;
        int clamped = amount;
        for (Holder<Item> item : used) clamped = Math.min(clamped, new ItemStack(item).getMaxStackSize());
        if (clamped != amount) {
            used.clear();
            if (!available.canCraft(recipe, clamped, used::add)) return RecipeBookMenu.PostPlaceAction.NOTHING;
        }
        final int perSlot = clamped;

        clearGrid();
        PlaceRecipeHelper.placeRecipe(3, 3, recipe, recipe.placementInfo().slotsToIngredientIndex(), (ingredientIndex, gridIndex, x, y) -> {
            if (ingredientIndex == -1) return;
            Item item = used.get(ingredientIndex).value();
            ItemStack placed = takeFromBackpack(item, perSlot);
            // Top up from the player's inventory if the Backpack ran short
            for (int i = 0; i < inventory.getContainerSize() && placed.getCount() < perSlot; i++) {
                ItemStack s = inventory.getItem(i);
                if (!s.is(item) || !Inventory.isUsableForCrafting(s)) continue;
                ItemStack taken = s.split(perSlot - placed.getCount());
                if (placed.isEmpty()) placed = taken;
                else placed.grow(taken.getCount());
            }
            gridSlots.get(gridIndex).set(placed);
        });
        inventory.setChanged();
        slotsChanged(craftSlots);
        return RecipeBookMenu.PostPlaceAction.NOTHING;
    }

    /** A recipe picked in JEI or REI: fill the grid the same way the recipe book does. */
    public void fillGrid(ServerPlayer sp, net.nexium.terminal.FillGridPayload payload) {
        if (!craftingActive()) return;
        ServerLevel level = (ServerLevel) sp.level();
        level.recipeAccess().byKey(payload.recipe()).ifPresent(holder -> {
            handlePlacement(payload.maxItems(), true, holder, level, sp.getInventory());
            broadcastChanges();
        });
    }

    /** A copy of what the Backpack's own slots hold, on either side (for the recipe viewers' "can this be made" check). */
    public List<ItemStack> storedStacks() {
        return contentsCopy();
    }

    /** The trash slot, and the crafting grid's and result slot's, for the screen and for tests. */
    public Slot trashSlot() {
        return trashSlot;
    }

    /** The Backpack at this source (or an empty stack): the worn one, or whatever sits in that inventory slot. */
    public static ItemStack stackAt(Player player, int source) {
        return source == Backpacks.EQUIPPED ? ExtraSlots.of(player).backpack() : player.getInventory().getItem(source);
    }

    /** Whether this is one of the Backpack's {@value BackpackContents#SIZE} storage slots (not a module slot or the player's). Works on both sides. */
    public boolean isStorageSlot(Slot slot) {
        return slot.container == contents;
    }

    /** Server: copies of the Backpack's {@value BackpackContents#SIZE} storage slots as this open screen holds them right now. */
    public NonNullList<ItemStack> contentsCopy() {
        NonNullList<ItemStack> list = NonNullList.withSize(BackpackContents.SIZE, ItemStack.EMPTY);
        for (int i = 0; i < BackpackContents.SIZE; i++) list.set(i, contents.getItem(i).copy());
        return list;
    }

    /**
     * Server: puts these stacks in the storage slots (only those that differ), saves them in the Backpack and sends the change to the
     * screen. How the modules (Compacting, Feeding, Auto Torch) edit a Backpack whose screen is open: the screen keeps its own copy of
     * the slots and writes all of it back on every change, so an edit made to the saved data behind its back would be put back over.
     */
    public void replaceContents(List<ItemStack> stacks) {
        boolean changed = false;
        loading = true;
        for (int i = 0; i < BackpackContents.SIZE; i++) {
            ItemStack next = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            if (ItemStack.matches(contents.getItem(i), next)) continue;
            contents.setItem(i, next.copy());
            changed = true;
        }
        loading = false;
        if (changed) {
            saveContents();
            broadcastChanges();
        }
    }

    /** The Backpack this menu is for. */
    public ItemStack backpack() {
        return stackAt(player, source);
    }

    /** {@link Backpacks#EQUIPPED}, or the inventory slot the Backpack was opened from. */
    public int source() {
        return source;
    }

    private boolean hasOtherOfType(BackpackModuleItem module, int exceptSlot) {
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            if (i != exceptSlot && modules.getItem(i).getItem() instanceof BackpackModuleItem other && other.type() == module.type()) return true;
        }
        return false;
    }

    private boolean valid() {
        return !player.level().isClientSide() && backpack() == opened && opened.getItem() instanceof BackpackItem;
    }

    private void saveContents() {
        if (!valid()) return;
        List<ItemStack> stacks = new ArrayList<>(BackpackContents.SIZE);
        for (int i = 0; i < BackpackContents.SIZE; i++) stacks.add(contents.getItem(i));
        BackpackContents.write(opened, stacks);
    }

    private void saveModules() {
        if (!valid()) return;
        List<ItemStack> stacks = new ArrayList<>(BackpackModules.SLOTS);
        for (int i = 0; i < BackpackModules.SLOTS; i++) stacks.add(modules.getItem(i));
        BackpackModules.setSlots(opened, stacks);
    }

    private void onModulesChanged() {
        saveModules();
        // The module was taken out or switched off: its grid goes with it, so what is in it goes back in the Backpack
        if (valid() && !craftingActive()) clearGrid(); // switched off, taken out or hidden
    }

    /** The slot index (in this menu) of the inventory slot the Backpack was opened from, or -1 if it is the worn one (or the offhand). */
    private int lockedMenuSlot() {
        if (source < 0 || source >= 36) return -1;
        return playerSlotStart + (source >= 9 ? source - 9 : 27 + source);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        // The Backpack stays where it is: no clicking it, and no number-key (or offhand) swaps into its slot
        if (slotIndex >= 0 && slotIndex == lockedMenuSlot()) return;
        if (input == ContainerInput.SWAP && (button == source || (button == 40 && source == Inventory.SLOT_OFFHAND))) return;
        // The trash slot keeps the last item put in it: what is carried goes in and replaces (destroys) what was there
        if (input == ContainerInput.PICKUP && slotIndex >= 0 && slotIndex < slots.size() && slots.get(slotIndex) == trashSlot && !getCarried().isEmpty()) {
            trash.setItem(0, getCarried().copy());
            setCarried(ItemStack.EMPTY);
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == lockedMenuSlot()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        int playerEnd = playerSlotStart + 36;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slot == resultSlot) {
            // Craft: the result goes to the player's inventory (the grid refills from the Backpack, so this can go on)
            stack.getItem().onCraftedBy(stack, player);
            if (!moveItemStackTo(stack, playerSlotStart, playerEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (index < playerSlotStart) {
            if (!moveItemStackTo(stack, playerSlotStart, playerEnd, true)) return ItemStack.EMPTY;
        } else if (index < playerEnd) {
            // From the player's slots to the Backpack's (modules and storage), never the grid or the trash
            if (customSlotCount == 0 || !moveItemStackTo(stack, 0, playerSlotStart, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, playerSlotStart, playerEnd, false)) {
            return ItemStack.EMPTY; // the grid and the trash give their items back
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        if (slot == resultSlot) player.drop(stack, false, Prediction.SERVER_ONLY);
        return original;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        if (id >= BTN_FEEDING_LEVEL && id <= BTN_FEEDING_WASTE) {
            changeFeeding(id);
            return true;
        }
        if (id == BTN_CRAFTING_HIDE) {
            toggleCraftingHidden();
            return true;
        }
        int slot = id - BTN_TOGGLE_MODULE;
        if (slot < 0 || slot >= BackpackModules.SLOTS || !valid()) return true;
        ItemStack module = modules.getItem(slot);
        if (!(module.getItem() instanceof BackpackModuleItem)) return true;
        ItemStack changed = module.copy();
        boolean nowOn = changed.has(net.nexium.registry.ModComponents.MODULE_OFF);
        if (nowOn) changed.remove(net.nexium.registry.ModComponents.MODULE_OFF);
        else changed.set(net.nexium.registry.ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
        modules.setItem(slot, changed);
        serverPlayer.sendOverlayMessage(Component.translatable(nowOn ? "message.nexium.backpack_module_on" : "message.nexium.backpack_module_off",
                changed.getHoverName()).withStyle(nowOn ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        return true;
    }

    /** Server: hides the crafting grid (what is in it goes back in the Backpack) or shows it again, on the Crafting module. */
    private void toggleCraftingHidden() {
        if (!valid()) return;
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            ItemStack module = modules.getItem(i);
            if (!(module.getItem() instanceof BackpackModuleItem item) || item.type() != BackpackModuleType.CRAFTING) continue;
            ItemStack changed = module.copy();
            if (changed.has(net.nexium.registry.ModComponents.CRAFTING_HIDDEN)) changed.remove(net.nexium.registry.ModComponents.CRAFTING_HIDDEN);
            else changed.set(net.nexium.registry.ModComponents.CRAFTING_HIDDEN, net.minecraft.util.Unit.INSTANCE);
            modules.setItem(i, changed);
            return;
        }
    }

    /** Server: one click on the Feeding module's level or never-waste button, on the module wherever it sits in the slots. */
    private void changeFeeding(int id) {
        if (!valid()) return;
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            ItemStack module = modules.getItem(i);
            if (!(module.getItem() instanceof BackpackModuleItem item) || item.type() != BackpackModuleType.FEEDING) continue;
            ItemStack changed = switch (id) {
                case BTN_FEEDING_LEVEL -> BackpackFeeding.withLevel(module, BackpackFeeding.nextLevel(BackpackFeeding.level(module), false));
                case BTN_FEEDING_LEVEL_BACK -> BackpackFeeding.withLevel(module, BackpackFeeding.nextLevel(BackpackFeeding.level(module), true));
                default -> BackpackFeeding.withWaste(module, !BackpackFeeding.allowsWaste(module));
            };
            modules.setItem(i, changed);
            return;
        }
    }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide()) {
            clearGrid(); // what is left in the crafting grid goes back in the Backpack
            trash.clearContent(); // and the trash is emptied
        }
        if (valid()) {
            saveContents();
            saveModules();
        }
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        // On the client the Backpack may not have arrived yet; the server decides
        return player.level().isClientSide() || valid();
    }
}
