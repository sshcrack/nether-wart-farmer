package me.sshcrack.netherwarts.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyOverwrite {
    public static Map<Integer, Boolean> overwrites = new HashMap<>();

    public static void press(int code) { overwrites.put(code, true); }
    public static void release(int code) { overwrites.put(code, false); }
    public static void unset(int code) { overwrites.remove(code); }
}
