package me.sshcrack.netherwarts.manager.inv;

import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public class GeneralHelper {
    public final static int MIN_SLEEPTIME = 12540;
    public final static int MAX_SLEEPTIME = 23400;
    public final static int DAY_LENGTH = 24000;

    public static void interactBlock(BlockPos pos) {
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

    public static List<ItemStack> getCurrentItems(Inventory entity) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        return player.currentScreenHandler.getStacks().subList(0, entity.size());
    }

    public static boolean canSleep() {
        long time = MinecraftClient.getInstance().world.getTimeOfDay();
        double curr = time % DAY_LENGTH;

        return curr > MIN_SLEEPTIME && curr < MAX_SLEEPTIME;
    }
}
