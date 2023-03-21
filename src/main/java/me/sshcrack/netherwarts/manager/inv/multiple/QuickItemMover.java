package me.sshcrack.netherwarts.manager.inv.multiple;

import me.sshcrack.netherwarts.MessageManager;
import me.sshcrack.netherwarts.manager.KeyOverwrite;
import me.sshcrack.netherwarts.manager.inv.GeneralHelper;
import me.sshcrack.netherwarts.manager.inv.baic.InventoryManager;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class QuickItemMover extends InventoryManager {
    private State state = State.INITIALIZING;
    private static final int SHIFT_KEY = 340;
    private int currTick = 0;

    private QuickItemMover mover;

    private List<Slot> sampleSlots;

    public QuickItemMover(ClientPlayerEntity player) {
        super(player);
    }

    private boolean hasSpaceLeft(Inventory inv) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty())
                return true;
        }

        return false;
    }

    private List<Slot> getMatchingSlots(Item kind, boolean fromPlayerToChest) {
        ScreenHandler handler = player.currentScreenHandler;
        List<Slot> outList = new ArrayList<>();

        if (fromPlayerToChest) {
            PlayerInventory inv = player.getInventory();

            for (int i = 0; i < inv.main.size(); i++) {
                ItemStack stack = inv.main.get(i);
                if (!stack.isOf(kind))
                    continue;

                OptionalInt handlerSlot = handler.getSlotIndex(inv, i);
                if (handlerSlot.isEmpty())
                    continue;

                outList.add(handler.getSlot(handlerSlot.getAsInt()));
            }
        } else {
            if(!(handler instanceof ShulkerBoxScreenHandler))
                return new ArrayList<>();

            ShulkerBoxScreenHandler h = (ShulkerBoxScreenHandler) handler;
            List<ItemStack> matching = h.getStacks().subList(0, ShulkerBoxBlockEntity.INVENTORY_SIZE);
            Inventory shulkerInv = h.slots.get(0).inventory;
            for (int i = 0; i < matching.size(); i++) {
                ItemStack stack = matching.get(i);
                if (!stack.isOf(kind))
                    continue;

                OptionalInt handlerSlot = handler.getSlotIndex(shulkerInv, i);

                if (handlerSlot.isEmpty())
                    continue;

                outList.add(handler.getSlot(handlerSlot.getAsInt()));
            }
        }

        return outList;
    }

    public MultipleReturnState moveSingle(Item kind, boolean fromPlayerToChest, ShulkerBoxBlockEntity entity) {
        return moveSingle(kind, fromPlayerToChest, entity, () -> {
        });
    }

    public MultipleReturnState moveSingle(Item kind, boolean fromPlayerToChest, ShulkerBoxBlockEntity entity, Runnable beforeClosingScreen) {
        if (state == State.INITIALIZING) {
            GeneralHelper.interactBlock(entity.getPos());
            entity.onOpen(player);
            state = State.WAIT_FOR_SCREEN;
            return MultipleReturnState.WAIT;
        }

        if (state == State.WAIT_FOR_SCREEN) {
            currTick++;
            if (currTick % 20 == 0) {
                state = State.SHIFTING_ITEMS;
            }
            return MultipleReturnState.WAIT;
        }

        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null) {
            state = State.INITIALIZING;
            return MultipleReturnState.SCREEN_NULL;
        }

        if (state == State.SHIFTING_ITEMS) {
            List<Slot> slots = getMatchingSlots(kind, fromPlayerToChest);
            if (slots.size() == 0) {
                state = State.INITIALIZING;
                GeneralHelper.closeShulker(entity);
                return MultipleReturnState.ITEM_NOT_FOUND;
            }

            Slot toClick = slots.get(0);


            Vec2f clickPos = getCoordinatesAt(screen, toClick);
            KeyOverwrite.press(SHIFT_KEY);
            screen.mouseClicked(clickPos.x, clickPos.y, 0);
            screen.mouseReleased(clickPos.x, clickPos.y, 0);

            KeyOverwrite.unset(SHIFT_KEY);
            state = State.CLOSING;

            currTick = 1;
        } else if (state == State.CLOSING) {
            currTick++;
            if (currTick % 10 == 0) {
                GeneralHelper.closeShulker(entity);
                sampleSlots = null;

                state = State.INITIALIZING;
                return MultipleReturnState.OK;
            }
        }

        return MultipleReturnState.WAIT;
    }

    public MultipleReturnState moveAll(Item kind, boolean fromPlayerToChest, ShulkerBoxBlockEntity entity) {
        return moveAll(kind, fromPlayerToChest, entity, () -> {});
    }

    public MultipleReturnState moveAll(Item kind, boolean fromPlayerToChest, ShulkerBoxBlockEntity entity, Runnable beforeClosingScreen) {
        if (state == State.INITIALIZING) {
            GeneralHelper.interactBlock(entity.getPos());
            entity.onOpen(player);
            state = State.WAIT_FOR_SCREEN;

            return MultipleReturnState.WAIT;
        }

        if (state == State.WAIT_FOR_SCREEN) {
            currTick++;
            if (currTick % 20 == 0) {
                state = State.CLICKING_SAMPLE;
            }
            return MultipleReturnState.WAIT;
        }

        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null) {
            state = State.INITIALIZING;
            sampleSlots = null;
            return MultipleReturnState.SCREEN_NULL;
        }

        if (state == State.CLICKING_SAMPLE) {
            sampleSlots = getMatchingSlots(kind, fromPlayerToChest);
            if(sampleSlots.size() < 2) {
                GeneralHelper.closeShulker(entity);
                return MultipleReturnState.ITEM_NOT_FOUND;
            }

            Slot initialSlot = sampleSlots.get(0);

            currTick++;
            if (currTick % 10 == 0) {
                Vec2f pos = getCoordinatesAt(screen, initialSlot);
                screen.mouseClicked(pos.x, pos.y, 0);
                screen.mouseReleased(pos.x, pos.y, 0);

                state = State.SHIFTING_ITEMS;
                currTick = 1;
            }

            return MultipleReturnState.WAIT;
        }


        Slot initialSlot = sampleSlots.get(0);
        Slot secondary = sampleSlots.get(1);
        if (state == State.SHIFTING_ITEMS) {
            currTick++;
            if (currTick % 10 == 0) {
                Vec2f pos = getCoordinatesAt(screen, secondary);

                KeyOverwrite.press(SHIFT_KEY);
                screen.mouseClicked(pos.x, pos.y, 0);
                screen.mouseReleased(pos.x, pos.y, 0);
                state = State.SHIFTING_ITEMS_DOUBLE;
                currTick = 1;
            }
        } else if (state == State.SHIFTING_ITEMS_DOUBLE) {
            currTick++;
            if (currTick % 3 == 0) {
                Vec2f pos = getCoordinatesAt(screen, secondary);

                screen.mouseClicked(pos.x, pos.y, 0);
                screen.mouseReleased(pos.x, pos.y, 0);

                KeyOverwrite.unset(SHIFT_KEY);
                beforeClosingScreen.run();
                state = State.CLOSING;
                currTick = 1;
            }
        } else if (state == State.CLOSING) {
            currTick++;
            if (currTick % 5 == 0) {
                Vec2f pos = getCoordinatesAt(screen, initialSlot);
                screen.mouseClicked(pos.x, pos.y, 0);
                screen.mouseReleased(pos.x, pos.y, 0);

                GeneralHelper.closeShulker(entity);
                MessageManager.sendMsg(Formatting.RED + "Closing thingy");
                state = State.INITIALIZING;

                sampleSlots = null;
                return MultipleReturnState.OK;
            }
        }


        return MultipleReturnState.WAIT;
    }

    public boolean moveAllBool(Item kind, boolean fromPlayerToChest, ShulkerBoxBlockEntity entity) {
        return moveAllBool(kind, fromPlayerToChest, entity, () -> {
        });
    }

    public boolean moveAllBool(Item kind, boolean fromPlayerToChest, ShulkerBoxBlockEntity entity, Runnable beforeClosingScreen) {
        MultipleReturnState state = moveAll(kind, fromPlayerToChest, entity, beforeClosingScreen);
        if (state == MultipleReturnState.ITEM_NOT_FOUND) {
            MessageManager.sendMsgF(Formatting.RED + "Item could not be found %s pos: %s playerToChest:", kind.asItem().getTranslationKey(), entity.getPos(), fromPlayerToChest);
        } else if (state == MultipleReturnState.SCREEN_NULL) {
            MessageManager.sendMsgF(Formatting.RED + "Could not open screen item: %s pos: %s playerToChest: %s", kind.asItem().getTranslationKey(), entity.getPos(), fromPlayerToChest);
        } else if (state == MultipleReturnState.NO_SPACE_LEFT) {
            MessageManager.sendMsgF(Formatting.RED + "No space left on inventory item: %s pos: %s playerToChest: %s", kind.asItem().getTranslationKey(), entity.getPos(), fromPlayerToChest);
        }

        return state == MultipleReturnState.OK;
    }

    public boolean moveSingleBool(Item kind, boolean fromPlayerToChest, ShulkerBoxBlockEntity entity) {
        MultipleReturnState state = moveSingle(kind, fromPlayerToChest, entity);
        if (state == MultipleReturnState.ITEM_NOT_FOUND) {
            MessageManager.sendMsgF(Formatting.RED + "Item could not be found %s pos: %s playerToChest:", kind.asItem().getTranslationKey(), entity.getPos(), fromPlayerToChest);
        } else if (state == MultipleReturnState.SCREEN_NULL) {
            MessageManager.sendMsgF(Formatting.RED + "Could not open screen item: %s pos: %s playerToChest: %s", kind.asItem().getTranslationKey(), entity.getPos(), fromPlayerToChest);
        } else if (state == MultipleReturnState.NO_SPACE_LEFT) {
            MessageManager.sendMsgF(Formatting.RED + "No space left on inventory item: %s pos: %s playerToChest: %s", kind.asItem().getTranslationKey(), entity.getPos(), fromPlayerToChest);
        }

        return state == MultipleReturnState.OK;
    }


    enum State {
        INITIALIZING,
        WAIT_FOR_SCREEN,
        CLICKING_SAMPLE,
        SHIFTING_ITEMS,
        SHIFTING_ITEMS_DOUBLE,
        CLOSING
    }
}
