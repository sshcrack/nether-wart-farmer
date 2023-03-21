package me.sshcrack.netherwarts.manager;

import net.minecraft.screen.slot.Slot;

public interface HandledScreenAccess {
    void onHotbarKeyPressedAccessed(Slot slot, int hotbarSlot);
}
