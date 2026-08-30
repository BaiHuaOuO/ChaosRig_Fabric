package dev.chaosrig.utils;

import dev.chaosrig.ChaosRigClient;

public class KeyboardManager {

    public static void register() {
        if (ChaosRigClient.isInit()) throw new RuntimeException("重复注册");

    }
}
