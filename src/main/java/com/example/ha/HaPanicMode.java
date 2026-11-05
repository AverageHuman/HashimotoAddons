package com.example.ha;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Global runtime gate shared by tick, render, input and packet integration. */
public final class HaPanicMode {
    private static final HaPanicModeState STATE = new HaPanicModeState();

    private HaPanicMode() { }

    public static boolean isActive() { return STATE.isActive(); }

    static boolean tick(MinecraftClient client, HaTickHandler handler) {
        HaPanicModeConfig config = HaConfig.get().panicMode;
        boolean down = false;
        if (client.getWindow() != null) {
            long window = client.getWindow().getHandle();
            if ("mouse".equals(config.keyType) && config.keyCode >= 0) {
                down = GLFW.glfwGetMouseButton(window, config.keyCode) == GLFW.GLFW_PRESS;
            } else if ("scancode".equals(config.keyType) && config.scanCode >= 0) {
                for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
                    if (GLFW.glfwGetKeyScancode(key) == config.scanCode && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                        down = true;
                        break;
                    }
                }
            } else if (config.keyCode >= GLFW.GLFW_KEY_SPACE) {
                down = GLFW.glfwGetKey(window, config.keyCode) == GLFW.GLFW_PRESS;
            }
        }
        // Text entry and key capture must never trigger panic. Track held keys there too.
        boolean allowed = client.player != null && client.world != null && client.currentScreen == null;
        if (!STATE.update(config.enabled, down, allowed)) return false;
        handler.suspendForPanic();
        if (isActive()) {
            if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
                HaAfkFarming.suspendForPanic();
                HaAlchemyKilnAutomation.stop(client, null);
                HaAutoThrowTrashVerse.onDisconnected();
                HaTriggerBot.onDisconnected();
                HaGhostWall.suspendForPanic(client);
            }
            HaItemContractNotifier.onDisconnected();
            client.inGameHud.setTitles(null, null, -1, -1, -1);
            client.getSoundManager().stopSounds(HaSounds.RITUAL_TIMER_READY.getId(), null);
            if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED && HaSounds.CRITICAL_SOUND != null) {
                client.getSoundManager().stopSounds(HaSounds.CRITICAL_SOUND.getId(), null);
            }
            client.inGameHud.getChatHud().clear(false);
        } else {
            restore(client);
        }
        return true;
    }

    public static void resume(MinecraftClient client) {
        if (!isActive()) return;
        STATE.resume();
        restore(client);
    }

    private static void restore(MinecraftClient client) {
        HaDropTracker.resumeAfterPanic();
        HaExpTracker.resumeAfterPanic();
        HaElementTracker.resumeAfterPanic();
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) HaGhostWall.resumeAfterPanic(client);
    }
}
