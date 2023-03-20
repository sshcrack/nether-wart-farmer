package me.sshcrack.netherwarts.manager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;

public class GeneralHandler {
    protected ClientPlayerEntity player;
    protected GameOptions options;

    public GeneralHandler(ClientPlayerEntity player) {
        this.player = player;
        this.options = MinecraftClient.getInstance().options;
    }
}
