package fi.dy.masa.itemscroller.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Slot;
import fi.dy.masa.itemscroller.LiteModItemScroller;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.gui.ConfigScreen;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.CraftingRecipe;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.util.InputUtils;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import fi.dy.masa.itemscroller.util.MoveAction;
import fi.dy.masa.malilib.config.option.HotkeyConfig;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.input.ActionResult;
import fi.dy.masa.malilib.input.KeyAction;
import fi.dy.masa.malilib.input.KeyBind;
import fi.dy.masa.malilib.input.callback.HotkeyCallback;
import fi.dy.masa.malilib.util.inventory.InventoryScreenUtils;

public class KeybindCallbacks implements HotkeyCallback
{
    private static final KeybindCallbacks INSTANCE = new KeybindCallbacks();

    public static KeybindCallbacks getInstance()
    {
        return INSTANCE;
    }

    private KeybindCallbacks()
    {
    }

    public void setCallbacks()
    {
        for (HotkeyConfig hotkey : Hotkeys.HOTKEY_LIST)
        {
            hotkey.getKeyBind().setCallback(this);
        }
    }

    @Override
    public ActionResult onKeyAction(KeyAction action, KeyBind key)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.player == null)
        {
            return ActionResult.FAIL;
        }

        if (key == Hotkeys.KEY_OPEN_CONFIG_GUI.getKeyBind())
        {
            BaseScreen.openScreen(ConfigScreen.create(null));
            return ActionResult.SUCCESS;
        }

        if ((GuiUtils.getCurrentScreen() instanceof GuiContainer) == false ||
            Configs.Toggles.MAIN_TOGGLE.getBooleanValue() == false)
        {
            return ActionResult.FAIL;
        }

        GuiContainer gui = (GuiContainer) GuiUtils.getCurrentScreen();
        Slot slot = InventoryScreenUtils.getSlotUnderMouse(gui);
        RecipeStorage recipes = RecipeStorage.getInstance();
        MoveAction moveAction = InputUtils.getDragMoveAction(key);

        if (slot != null)
        {
            if (moveAction != MoveAction.NONE)
            {
                return InventoryUtils.dragMoveItems(gui, mc, moveAction, true) ? ActionResult.SUCCESS : ActionResult.FAIL;
            }
            else if (key == Hotkeys.KEY_MOVE_EVERYTHING.getKeyBind())
            {
                InventoryUtils.tryMoveStacks(slot, gui, false, true, false);
                return ActionResult.SUCCESS;
            }
            else if (key == Hotkeys.KEY_DROP_ALL_MATCHING.getKeyBind())
            {
                if (Configs.Toggles.DROP_MATCHING.getBooleanValue() &&
                    Configs.Lists.GUI_BLACKLIST.getValue().contains(gui.getClass().getName()) == false &&
                    slot.getHasStack())
                {
                    InventoryUtils.dropStacks(gui, slot.getStack(), slot, true);
                    return ActionResult.SUCCESS;
                }
            }
            else if (key == Hotkeys.KEY_MOVE_STACK_TO_OFFHAND.getKeyBind())
            {
                // Swap the hovered stack to the Offhand
                if ((gui instanceof GuiInventory) && slot != null)
                {
                    InventoryUtils.swapSlots(gui, slot.slotNumber, 45);
                    return ActionResult.SUCCESS;
                }
            }
        }

        if (key == Hotkeys.KEY_CRAFT_EVERYTHING.getKeyBind())
        {
            return InventoryUtils.craftEverythingPossibleWithCurrentRecipe(recipes.getSelectedRecipe(), gui) ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
        else if (key == Hotkeys.KEY_THROW_CRAFT_RESULTS.getKeyBind())
        {
            InventoryUtils.throwAllCraftingResultsToGround(recipes.getSelectedRecipe(), gui);
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.KEY_MOVE_CRAFT_RESULTS.getKeyBind())
        {
            InventoryUtils.moveAllCraftingResultsToOtherInventory(recipes.getSelectedRecipe(), gui);
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.KEY_STORE_RECIPE.getKeyBind())
        {
            return recipes.storeCraftingRecipeToCurrentSelection(slot, gui, true) ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
        else if (key == Hotkeys.KEY_VILLAGER_TRADE_FAVORITES.getKeyBind())
        {
            return InventoryUtils.villagerTradeEverythingPossibleWithAllFavoritedTrades() ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
        else if (key == Hotkeys.KEY_SLOT_DEBUG.getKeyBind())
        {
            if (slot != null)
            {
                InventoryUtils.debugPrintSlotInfo(gui, slot);
            }
            else
            {
                LiteModItemScroller.logger.info("GUI class: {}", gui.getClass().getName());
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    public void onTick(Minecraft mc)
    {
        if (mc.player != null &&
            Configs.Toggles.MAIN_TOGGLE.getBooleanValue() &&
            GuiUtils.getCurrentScreen() instanceof GuiContainer &&
            (GuiUtils.getCurrentScreen() instanceof GuiContainerCreative) == false &&
            Configs.Lists.GUI_BLACKLIST.getValue().contains(GuiUtils.getCurrentScreen().getClass().getName()) == false &&
            Hotkeys.KEY_MASS_CRAFT.getKeyBind().isKeyBindHeld())
        {
            GuiScreen guiScreen = GuiUtils.getCurrentScreen();
            GuiContainer gui = (GuiContainer) guiScreen;
            Slot outputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

            if (outputSlot != null)
            {
                CraftingRecipe recipe = RecipeStorage.getInstance().getSelectedRecipe();

                InventoryUtils.tryClearCursor(gui, mc);
                InventoryUtils.throwAllCraftingResultsToGround(recipe, gui);
                InventoryUtils.tryMoveItemsToFirstCraftingGrid(recipe, gui, true);

                int failsafe = 0;

                while (++failsafe < 40 && InventoryUtils.areStacksEqual(outputSlot.getStack(), recipe.getResult()))
                {
                    if (Configs.Generic.CARPET_CTRL_Q_CRAFTING.getBooleanValue())
                    {
                        InventoryUtils.dropStack(gui, outputSlot.slotNumber);
                    }
                    else
                    {
                        InventoryUtils.dropStacksWhileHasItem(gui, outputSlot.slotNumber, recipe.getResult());
                    }

                    InventoryUtils.tryClearCursor(gui, mc);
                    InventoryUtils.throwAllCraftingResultsToGround(recipe, gui);
                    InventoryUtils.tryMoveItemsToFirstCraftingGrid(recipe, gui, true);
                }
            }
        }
    }
}
