package com.example.ha;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Persisted Full-only state owned by the Command Key feature. */
public final class HaCommandKeyConfig {
    public boolean enabled = false;
    public String command = "";
    public int keyCode = GLFW.GLFW_KEY_UNKNOWN;
    public int scanCode = -1;
    public String keyType = "keysym";

    public void normalize() {
        command = command == null ? "" : command.trim();
        if (keyType == null || (!"keysym".equals(keyType) && !"scancode".equals(keyType) && !"mouse".equals(keyType))) {
            keyType = "keysym";
        }
    }

    public InputUtil.Key getKey() {
        if ("mouse".equals(keyType)) {
            return InputUtil.Type.MOUSE.createFromCode(keyCode);
        }
        if ("scancode".equals(keyType)) {
            return InputUtil.Type.SCANCODE.createFromCode(scanCode);
        }
        return InputUtil.Type.KEYSYM.createFromCode(keyCode);
    }

    public boolean hasExecutableCommand() {
        return command.length() > 1 && command.charAt(0) == '/';
    }

    public void reset() {
        enabled = false;
        command = "";
        keyCode = GLFW.GLFW_KEY_UNKNOWN;
        scanCode = -1;
        keyType = "keysym";
    }
}
