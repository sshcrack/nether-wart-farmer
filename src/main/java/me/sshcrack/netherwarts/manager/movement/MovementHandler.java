package me.sshcrack.netherwarts.manager.movement;

import me.sshcrack.netherwarts.manager.FarmState;
import me.sshcrack.netherwarts.manager.GeneralHandler;
import me.sshcrack.netherwarts.manager.KeyOverwrite;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.search.IdentifierSearchableIterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.function.Function;

public class MovementHandler extends GeneralHandler {
    public MovementHandler(ClientPlayerEntity player) {
        super(player);
    }

    private void lookAtYawOnly(Vec3d target) {
        Vec3d pos = player.getPos();
        double d = target.x - pos.x;
        double f = target.z - pos.z;

        player.setYaw(MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875D) - 90.0F));
        player.setHeadYaw(player.getYaw());
        player.prevYaw = player.getYaw();
    }

    public boolean walk(BlockPos pos) {
        return walk(pos, p -> p.compareTo(pos) == 0);
    }

    public boolean walk(BlockPos desiredPos, Function<BlockPos, Boolean> reachedEnd) {
        KeyOverwrite.press(options.forwardKey);
        Vec3d currCenter = Vec3d.ofCenter(desiredPos, 0);

        lookAtYawOnly(currCenter);
        BlockPos playerPos = player.getBlockPos().up();
        if (!reachedEnd.apply(playerPos))
            return false;

        KeyOverwrite.unset(options.forwardKey);
        return true;
    }
}
