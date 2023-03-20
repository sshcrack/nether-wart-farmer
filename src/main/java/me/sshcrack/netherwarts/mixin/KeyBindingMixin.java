package me.sshcrack.netherwarts.mixin;

import me.sshcrack.netherwarts.manager.KeyOverwrite;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public class KeyBindingMixin {
    @Shadow private boolean pressed;

    @Inject(method="setPressed",at=@At("TAIL"))
    public void setPressed(boolean pressed, CallbackInfo ci) {
        boolean has = KeyOverwrite.keyBindings.containsKey(this);
        if(!has)
            return;

        this.pressed = KeyOverwrite.keyBindings.get(this);
    }
}
