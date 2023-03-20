package me.sshcrack.netherwarts.manager;

import net.minecraft.client.MinecraftClient;

public class WartManager {
    private final GeneralTimerAccess timer;
    public WartManager() {
        this.timer = (GeneralTimerAccess) MinecraftClient.getInstance().gameRenderer;
    }

    public int start() {
        timer.start();
        return 0;
    }

    public int stop() {
        timer.stop();
        return 0;
    }

}
