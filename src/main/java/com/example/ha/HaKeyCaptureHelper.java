package com.example.ha;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class HaKeyCaptureHelper {
    private HaKeyCaptureHelper() {
    }

    public static String keyName(InputUtil.Key key) {
        return key == InputUtil.UNKNOWN_KEY ? "Unbound" : key.getLocalizedText().getString();
    }

    public static boolean shouldIgnoreKeyCapture(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_ESCAPE;
    }

    public static InputBinding keyboard(int keyCode, int scanCode) {
        return new InputBinding(keyCode, scanCode, "keysym");
    }

    public static InputBinding mouse(int button) {
        return new InputBinding(button, -1, "mouse");
    }

    public static final class InputBinding {
        public final int keyCode;
        public final int scanCode;
        public final String type;

        private InputBinding(int keyCode, int scanCode, String type) {
            this.keyCode = keyCode;
            this.scanCode = scanCode;
            this.type = type;
        }
    }
}
