package me.sshcrack.netherwarts.manager.inv.baic;

import me.sshcrack.netherwarts.MainMod;
import me.sshcrack.netherwarts.MessageManager;
import me.sshcrack.netherwarts.manager.GeneralHandler;
import me.sshcrack.netherwarts.manager.HandledScreenAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec2f;

import java.util.function.Supplier;

public class InventoryManager extends GeneralHandler {
    private int currTick = 1;

    protected InventoryScreen screen;
    protected HandledScreenAccess screenAccess;
    protected InvState state = InvState.Opening;

    public InventoryManager(ClientPlayerEntity player) {
        super(player);
    }


    // Constants taken from net.minecraft.client.gui.screen.ingame.HandledScreen.backgroundWidth
    protected int backgroundWidth = 176;
    // Constants taken from net.minecraft.client.gui.screen.ingame.HandledScreen.backgroundHeight
    protected int backgroundHeight = 166;


    protected Vec2f getCoordinatesAt(Screen screen, Slot slot) {
        int i = (screen.width - backgroundWidth) / 2;
        int j = (screen.height - backgroundHeight) / 2;

        // Width/Height straight up copied too.
        int width = 16;
        int height = 16;

        float mouseXOut = slot.x + ((float) width) / 2 + i;
        float mouseYOut = slot.y + ((float) height) / 2 + j;

        return new Vec2f(mouseXOut, mouseYOut);
    }


    public int getSlotsFree() {
        DefaultedList<ItemStack> main = player.getInventory().main;
        int slotsFree = 0;

        for (ItemStack stack : main) {
            if(stack.isOf(Items.AIR))
                slotsFree++;
        }

        return slotsFree;
    }

    protected boolean basicInvOpener(Supplier<Boolean> processor) {
        if(state == InvState.Opening) {
            screen = new InventoryScreen(player);
            screenAccess = (HandledScreenAccess) screen;
            MinecraftClient.getInstance().setScreen(screen);

            state = InvState.SUSPEND;
            return false;
        }

        if(state == InvState.SUSPEND) {
            if(!processor.get())
                return false;

            state = InvState.Closing;
            currTick = 1;
            return false;
        }

        if(state == InvState.Closing) {
            currTick++;

            if(currTick % 20 == 0) {
                screen.close();
                player.currentScreenHandler.close(player);
                MinecraftClient.getInstance().setScreen(null);
                state = InvState.Wait;
                currTick = 1;

                MessageManager.sendMsg(Formatting.GREEN + "Closing inventory / waiting...");
            }

            return false;
        }

        if(state == InvState.Wait) {
            currTick++;
            if(currTick % 20 == 0) {
                MessageManager.sendMsg(Formatting.GREEN + "Done. Continuing...");
                state = InvState.Opening;
                return true;
            }
            return false;
        }

        MainMod.LOGGER.error("Invalid State {}", state);
        return false;
    }

    public static int getSlotsFree(Inventory inv) {
        int free = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack item = inv.getStack(i);
            if(item.isEmpty())
                free++;
        }

        return free;
    }

    public static int getSlotsWithItem(Inventory inv, Item kind) {
        int withItem = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack item = inv.getStack(i);
            if(item.isOf(kind))
                withItem++;
        }

        return withItem;
    }
}
