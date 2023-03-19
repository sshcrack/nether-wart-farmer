package me.sshcrack.netherwarts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MessageManager {
    private final static String PREFIX = Formatting.GRAY + "[" + Formatting.GOLD + "NetherWarts" + Formatting.GRAY + "]" + " ";

    public static void sendMsg(String text) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(Text.literal(PREFIX + text));
    }

    public static void sendMsgF(String text, Object ...replacement) {
        sendMsg(String.format(text, replacement));
    }
}
