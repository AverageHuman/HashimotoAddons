package com.example.ha;

import net.minecraft.client.util.InputUtil;

/** Both variants; arming and binding are persistent, suspension is session-only. */
public final class HaPanicModeConfig {
    public boolean enabled;
    public int keyCode = -1;
    public int scanCode = -1;
    public String keyType = "keysym";

    public void normalize() {
        if (!"keysym".equals(keyType) && !"scancode".equals(keyType) && !"mouse".equals(keyType)) {
            keyType = "keysym";
        }
        if ("mouse".equals(keyType) && (keyCode < 0 || keyCode > 7)) {
            keyType = "keysym";
            keyCode = -1;
        }
        if ("keysym".equals(keyType) && (keyCode < -1 || keyCode > 348)) keyCode = -1;
        if (scanCode < -1) scanCode = -1;
    }

    public InputUtil.Key getKey() {
        if ("mouse".equals(keyType)) return InputUtil.Type.MOUSE.createFromCode(keyCode);
        if ("scancode".equals(keyType)) return InputUtil.Type.SCANCODE.createFromCode(scanCode);
        return InputUtil.Type.KEYSYM.createFromCode(keyCode);
    }
}
