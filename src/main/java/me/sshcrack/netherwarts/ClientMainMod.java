package me.sshcrack.netherwarts;

import com.mojang.brigadier.context.CommandContext;
//import me.sshcrack.netherwarts.manager.WartManager;
import me.sshcrack.netherwarts.manager.GeneralTimerAccess;
import me.sshcrack.netherwarts.manager.WartManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

public class ClientMainMod implements ClientModInitializer {
    private static WartManager wartManager;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, b) -> {
            dispatcher
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


            dispatcher
                    .register(
                            ClientCommandManager
                                    .literal("shulker")
                                    .executes(this::checkShulkerBoxes)
                    );
            dispatcher
                    .register(
                            ClientCommandManager
                                    .literal("debug")
                                    .executes(this::toggleDebug)
                    );
        });
    }

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

    public int checkShulkerBoxes(CommandContext<FabricClientCommandSource> context) {
        ((GeneralTimerAccess)MinecraftClient.getInstance().gameRenderer).test();
        return 0;
    }

    public int toggleDebug(CommandContext<FabricClientCommandSource> context) {
        MessageManager.toggleDebug();
        return 0;
    }
}
