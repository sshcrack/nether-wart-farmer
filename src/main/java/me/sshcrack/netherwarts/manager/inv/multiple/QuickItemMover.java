package me.sshcrack.netherwarts.manager.inv.multiple;

import me.sshcrack.netherwarts.manager.KeyOverwrite;
import me.sshcrack.netherwarts.manager.inv.baic.InventoryManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.Arrays;
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
        for(int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if(stack.isEmpty())
                return true;
        }

        return false;
    }

    private List<Slot> getMatchingSlots(Item kind, boolean fromPlayerToChest, Inventory block) {
        ScreenHandler handler = player.currentScreenHandler;
        List<Slot> outList = new ArrayList<>();

        if(fromPlayerToChest) {
            PlayerInventory inv = player.getInventory();
            for (ItemStack stack : inv.main) {
                if(!stack.isOf(kind))
                    continue;

                int iSlot = inv.getSlotWithStack(stack);
                OptionalInt handlerSlot = handler.getSlotIndex(inv, iSlot);

                if(handlerSlot.isEmpty())
                    continue;

                outList.add(handler.getSlot(handlerSlot.getAsInt()));
            }
        } else {
            for(int i = 0; i < block.size(); i++) {
                ItemStack stack = block.getStack(i);
                if(!stack.isOf(kind))
                    continue;

                OptionalInt handlerSlot = handler.getSlotIndex(block, i);

                if(handlerSlot.isEmpty())
                    continue;

                outList.add(handler.getSlot(handlerSlot.getAsInt()));
            }
        }

        return outList;
    }

    public MultipleReturnState moveSingle(List<Slot> slots, Inventory block, Runnable openShulker) {
        return  moveSingle(slots, block, openShulker, true);
    }

    public MultipleReturnState moveSingle(List<Slot> slots, Inventory block, Runnable openShulker, boolean shouldClose) {
        if(slots.size() == 0) {
            sampleSlots = null;
            state = State.INITIALIZING;
            return MultipleReturnState.ITEM_NOT_FOUND;
        }

        if(state == State.INITIALIZING) {
            openShulker.run();
            block.onOpen(player);
            state = State.WAIT_FOR_SCREEN;
            return MultipleReturnState.WAIT;
        }

        if(state == State.WAIT_FOR_SCREEN) {
            currTick++;
            if(currTick % 20 == 0) {
                state = State.SHIFTING_ITEMS;
            }
            return MultipleReturnState.WAIT;
        }

        Screen screen = MinecraftClient.getInstance().currentScreen;
        if(screen == null) {
            state = State.INITIALIZING;
            return MultipleReturnState.SCREEN_NULL;
        }

        Slot toClick = slots.get(0);
        if(state == State.SHIFTING_ITEMS) {
            Vec2f clickPos = getCoordinatesAt(screen, toClick);
            KeyOverwrite.press(SHIFT_KEY);
            screen.mouseClicked(clickPos.x, clickPos.y, 0);
            screen.mouseReleased(clickPos.x, clickPos.y, 0);

            KeyOverwrite.unset(SHIFT_KEY);
            state = State.CLOSING;

            currTick = 1;
        } else if(state == State.CLOSING) {
            currTick++;
            if(currTick % 5 == 0) {
                if(shouldClose) {
                    block.onClose(player);
                    MinecraftClient.getInstance().setScreen(null);
                    sampleSlots = null;
                }

                state = State.INITIALIZING;
                return MultipleReturnState.OK;
            }
        }

        return MultipleReturnState.WAIT;
    }

    public MultipleReturnState moveAll(Item kind, boolean fromPlayerToChest, Inventory blockInv, Runnable openShulker) {
        if(sampleSlots == null)
            sampleSlots = getMatchingSlots(kind, fromPlayerToChest, blockInv);

        if(sampleSlots.size() <= 1)
            return moveSingle(sampleSlots, blockInv, openShulker);


        Slot initialSlot = sampleSlots.get(0);
        Slot secondary = sampleSlots.get(1);

        if(secondary == null)
            return moveSingle(sampleSlots, blockInv, openShulker);

        if(state == State.INITIALIZING) {
            openShulker.run();
            blockInv.onOpen(player);
            state = State.WAIT_FOR_SCREEN;

            return MultipleReturnState.WAIT;
        }

        if(state == State.WAIT_FOR_SCREEN) {
            currTick++;
            if(currTick % 20 == 0) {
                state = State.CLICKING_SAMPLE;
            }
            return MultipleReturnState.WAIT;
        }

        Screen screen = MinecraftClient.getInstance().currentScreen;
        if(screen == null) {
            state = State.INITIALIZING;
            sampleSlots = null;
            return MultipleReturnState.SCREEN_NULL;
        }

        if(state == State.CLICKING_SAMPLE) {
            currTick++;
            if(currTick % 5 == 0) {
                Vec2f pos = getCoordinatesAt(screen, initialSlot);
                screen.mouseClicked(pos.x, pos.y, 0);
                screen.mouseReleased(pos.x, pos.y, 0);

                state = State.SHIFTING_ITEMS;
                currTick = 1;
            }
        } else if(state == State.SHIFTING_ITEMS) {
            currTick++;
            if(currTick % 5 == 0) {
                Vec2f pos = getCoordinatesAt(screen, secondary);

                KeyOverwrite.press(SHIFT_KEY);
                screen.mouseClicked(pos.x, pos.y, 0);
                screen.mouseReleased(pos.x, pos.y, 0);
                state = State.SHIFTING_ITEMS_DOUBLE;
                currTick = 1;
            }
        } else if (state == State.SHIFTING_ITEMS_DOUBLE) {
            currTick++;
            if(currTick % 3 == 0) {
                Vec2f pos = getCoordinatesAt(screen, secondary);

                screen.mouseClicked(pos.x, pos.y, 0);
                screen.mouseReleased(pos.x, pos.y, 0);

                KeyOverwrite.unset(SHIFT_KEY);
                state = State.CLOSING;
            }
        }else if(state == State.CLOSING) {
            currTick++;
            if(currTick % 5 == 0) {
                blockInv.onClose(player);
                MinecraftClient.getInstance().setScreen(null);

                state = State.INITIALIZING;

                sampleSlots = null;
                return MultipleReturnState.OK;
            }
        }


        return MultipleReturnState.WAIT;
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
