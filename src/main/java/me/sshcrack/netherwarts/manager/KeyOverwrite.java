package me.sshcrack.netherwarts.manager;

import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyOverwrite {
    public static Map<Integer, Boolean> overwrites = new HashMap<>();
    public static Map<KeyBinding, Boolean> keyBindings = new HashMap<>();

    public static void press(int code) { overwrites.put(code, true); }
    public static void release(int code) { overwrites.put(code, false); }
    public static void unset(int code) { overwrites.remove(code); }


    public static void reset() {
        keyBindings = new HashMap<>();
        overwrites = new HashMap<>();
    }

    public static void press(KeyBinding key) {
        keyBindings.put(key, true);
        key.setPressed(true);
    }
    public static void release(KeyBinding key) {
        keyBindings.put(key, false);
        key.setPressed(false);
    }
    public static void unset(KeyBinding key) {
        keyBindings.remove(key);
        key.setPressed(false);
    }
}
