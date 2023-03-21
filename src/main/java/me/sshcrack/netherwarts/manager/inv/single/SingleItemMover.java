package me.sshcrack.netherwarts.manager.inv.single;

import me.sshcrack.netherwarts.MessageManager;
import me.sshcrack.netherwarts.manager.inv.baic.InvState;
import me.sshcrack.netherwarts.manager.inv.baic.InventoryManager;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec2f;

import java.awt.*;
import java.util.OptionalInt;

public class SingleItemMover extends InventoryManager {
    private SingleInvState singleState = SingleInvState.MOVING_SOURCE;
    private int currTick = 0;

    public SingleItemMover(ClientPlayerEntity player) {
        super(player);
    }

    public void reset() {
        singleState = SingleInvState.MOVING_SOURCE;
    }

    public boolean scrollToHoe(int hotbarSlot) {
        return moveItemsWrapper(Items.DIAMOND_HOE, hotbarSlot);
    }

    //Returns if done
    public boolean tickScrollWart(int hotbarSlot) {
        return moveItemsWrapper(Items.NETHER_WART, hotbarSlot);
    }

    //Returns if done
    public boolean moveBreadTick(int hotbarSlot) {
        return moveItemsWrapper(Items.BREAD, hotbarSlot);
    }

    private boolean moveItemsWrapper(Item kind, int hotbarSlot) {
        ItemStack initialStack = player.getInventory().main.get(hotbarSlot);
        if(state == InvState.Opening && initialStack.isOf(kind)) {
            player.getInventory().selectedSlot = hotbarSlot;
            return true;
        }

        return basicInvOpener(() -> moveItems(kind, hotbarSlot));
    }

    private boolean moveItems(Item kind, int hotbarSlot) {
        int destinationSlot = 36 + hotbarSlot;
        ScreenHandler handler = player.currentScreenHandler;

        currTick++;
        if(singleState == SingleInvState.MOVING_SOURCE) {
            if(currTick % 30 == 0) {
                PlayerInventory inv = player.getInventory();

                ItemStack lowestStack = null;
                Slot lowestSlot = null;

                for (ItemStack stack : inv.main) {
                    if (!stack.isOf(kind))
                        continue;

                    if (lowestStack != null && lowestStack.getCount() < stack.getCount())
                        continue;


                    OptionalInt optSlot = handler.getSlotIndex(inv, inv.getSlotWithStack(stack));
                    if (optSlot.isEmpty()) {
                        MessageManager.sendMsgF(Formatting.YELLOW + "Could not find slot with stack %s", stack);
                        continue;
                    }

                    lowestStack = stack;
                    lowestSlot = handler.getSlot(optSlot.getAsInt());
                }

                if (lowestStack == null)
                    return false;

                Vec2f start = getCoordinatesAt(screen, lowestSlot);

                screen.mouseClicked(start.x, start.y, 0);
                screen.mouseReleased(start.x, start.y, 0);
                singleState = SingleInvState.MOVING_DEST;
                MessageManager.sendMsgF("Moving dest");
                currTick = 1;
            }
        } else if(singleState == SingleInvState.MOVING_DEST) {
            if(currTick % 30 == 0) {
                Slot dest = handler.getSlot(destinationSlot);
                Vec2f end = getCoordinatesAt(screen, dest);

                screen.mouseClicked(end.x, end.y, 0);
                screen.mouseReleased(end.x, end.y, 0);

                MessageManager.sendMsgF("Selecting slot %s (checking)", hotbarSlot);
                player.getInventory().selectedSlot = hotbarSlot;
                currTick = 1;

                singleState = SingleInvState.MOVING_SOURCE;
                state = InvState.Closing;
                return true;
            }
        }

        return false;
    }
}
