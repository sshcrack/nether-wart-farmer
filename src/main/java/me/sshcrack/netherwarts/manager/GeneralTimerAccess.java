package me.sshcrack.netherwarts.manager;

public interface GeneralTimerAccess {
    boolean start();
    boolean stop();

    void innerTick();

    void test();
}
