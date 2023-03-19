package me.sshcrack.netherwarts;

import com.mojang.brigadier.context.CommandContext;
//import me.sshcrack.netherwarts.manager.WartManager;
import me.sshcrack.netherwarts.manager.GeneralTimerAccess;
import me.sshcrack.netherwarts.manager.inv.StorageManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ClientMainMod implements ClientModInitializer {
    //private static WartManager wartManager;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, b) -> {
            /*dispatcher
                    .register(
                            ClientCommandManager.literal("wartsstop")
                                    .executes(this::stopFarming)
                    );

            dispatcher
                    .register(
                            ClientCommandManager
                                    .literal("netherwarts")
                                    .executes(this::startFarming)
                    );
*/

            dispatcher
                    .register(
                            ClientCommandManager
                                    .literal("shulker")
                                    .executes(this::checkShulkerBoxes)
                    );
        });
    }

/*
    public int stopFarming(CommandContext<FabricClientCommandSource> context) {
        if(wartManager == null)
            wartManager = new WartManager();

        return wartManager.stop();
    }

    public int startFarming(CommandContext<FabricClientCommandSource> context) {
        if(wartManager == null)
            wartManager = new WartManager();

        return wartManager.start();
    }
*/
    public int checkShulkerBoxes(CommandContext<FabricClientCommandSource> context) {
        /*ClientPlayerEntity player = context.getSource().getPlayer();

        ClientWorld world = player.clientWorld;
        BlockPos pos = player.getBlockPos();

        Pair<BlockPos, BlockPos> rect = Rect.detectRect(pos, world);
        if (rect == null) {
            MessageManager.sendMsg(Formatting.RED + "Could not find matching field to farm.");
            return -1;
        }

        List<BlockPos> shulkerBoxes = StorageManager.getOuterBlocks(context.getSource().getWorld(), rect, Blocks.RED_SHULKER_BOX);
        for (BlockPos shulkerBox : shulkerBoxes) {
            MessageManager.sendMsgF("Shulker Box at: %s", shulkerBox);
        }
*/
        ((GeneralTimerAccess)MinecraftClient.getInstance().gameRenderer).test();
        return 0;
    }
}
