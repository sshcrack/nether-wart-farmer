package me.sshcrack.netherwarts.mixin;

import me.sshcrack.netherwarts.manager.HandledScreenAccess;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin implements HandledScreenAccess {

    @Shadow protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Override
    public void onHotbarKeyPressedAccessed(Slot slot, int hotbarSlot) {
        onMouseClick(slot, slot.id, hotbarSlot, SlotActionType.SWAP);
    }
}
