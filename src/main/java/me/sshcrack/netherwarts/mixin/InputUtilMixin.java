package me.sshcrack.netherwarts.mixin;

import me.sshcrack.netherwarts.manager.KeyOverwrite;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputUtil.class)
public abstract class InputUtilMixin {
    @Inject(method = "isKeyPressed", at=@At("RETURN"), cancellable = true)
    private static void isKeyPressed(long handle, int code, CallbackInfoReturnable<Boolean> cir) {
        if(!KeyOverwrite.overwrites.containsKey(code))
            return;

        boolean newValue = KeyOverwrite.overwrites.get(code);
        cir.setReturnValue(newValue);
    }
}
