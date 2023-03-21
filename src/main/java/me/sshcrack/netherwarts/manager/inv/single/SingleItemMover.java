package me.sshcrack.netherwarts.manager.inv.single;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.sshcrack.netherwarts.MessageManager;
import me.sshcrack.netherwarts.manager.inv.baic.InvState;
import me.sshcrack.netherwarts.manager.inv.baic.InventoryManager;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec2f;

import java.awt.*;
import java.util.OptionalInt;

public class SingleItemMover extends InventoryManager {
    private int currTick = 0;
    private Slot lowestSlot;

    public SingleItemMover(ClientPlayerEntity player) {
        super(player);
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
        ScreenHandler handler = player.currentScreenHandler;

        currTick++;
        if(currTick % 30 == 0) {
            PlayerInventory inv = player.getInventory();

            ItemStack lowestStack = null;
            lowestSlot = null;

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

            screenAccess.onHotbarKeyPressedAccessed(lowestSlot, hotbarSlot);

            player.getInventory().selectedSlot = hotbarSlot;
            state = InvState.Closing;
            currTick = 1;
            return true;
        }

        return false;
    }
}
