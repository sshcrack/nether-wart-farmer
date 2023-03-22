package me.sshcrack.netherwarts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.Format;

public class MessageManager {
    private final static String PREFIX = Formatting.GRAY + "[" + Formatting.GOLD + "NetherWarts" + Formatting.GRAY + "]" + " ";
    private static boolean isDebugging = false;

    public static void toggleDebug() {
        isDebugging = !isDebugging;
        if(isDebugging)
            MessageManager.sendMsg(Formatting.GREEN + "Enabled debugging mode.");
        else
            MessageManager.sendMsg(Formatting.RED + "Disabled debugging mode.");
    }

    public static void sendMsg(String text) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(Text.literal(PREFIX + text));
    }

    public static void sendMsgF(String text, Object ...replacement) {
        sendMsg(String.format(text, replacement));
    }


    public static void debugMsg(String text) {
        assert MinecraftClient.getInstance().player != null;
        if(!isDebugging)
            return;

        MinecraftClient.getInstance().player.sendMessage(Text.literal(PREFIX + text));
    }

    public static void debugMsgF(String text, Object ...replacement) {
        debugMsg(String.format(text, replacement));
    }
}
