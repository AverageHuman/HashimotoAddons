package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.LiteralText;

/** Executes a configured command exactly as entered by the user. */
public final class HaCommandKey {
    private static final String PREFIX = "[\u00a7l\u00a7bHashimotoAddons\u00a7r]:";

    private HaCommandKey() {
    }

    public static void execute(MinecraftClient client, HaCommandKeyConfig config) {
        if (client == null || config == null || client.player == null) {
            return;
        }
        if (!config.enabled) {
            showMessage(client.player, "\u00a7cCommand Key is OFF.");
            return;
        }
        if (!config.hasExecutableCommand()) {
            showMessage(client.player, "\u00a7cSet a command beginning with / (example: /bp).");
            return;
        }
        client.player.sendChatMessage(config.command);
    }

    private static void showMessage(ClientPlayerEntity player, String message) {
        player.sendMessage(new LiteralText(PREFIX + message), false);
    }
}
