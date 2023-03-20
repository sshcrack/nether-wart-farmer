package me.sshcrack.netherwarts.manager.inv;

import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public class ShulkerHelper {
    public static void openShulker(BlockPos pos) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        BlockHitResult blockHit = new BlockHitResult(pos.toCenterPos(), Direction.EAST, pos, false);
        MinecraftClient.getInstance().interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
    }

    public static void closeShulker(ShulkerBoxBlockEntity entity) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        player.currentScreenHandler.close(player);
        entity.onClose(player);

        MinecraftClient.getInstance().setScreen(null);
    }

    public static List<ItemStack> getCurrentItems(ShulkerBoxBlockEntity entity) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        return player.currentScreenHandler.getStacks().subList(0, entity.size());
    }
}
