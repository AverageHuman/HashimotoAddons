package com.example.ha;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaSoulbindProtection {
    private static final String SOULBIND_NAME = "ヴェルサリオンの呪縛";
    private static final String SOULBIND_START_KEYWORD = "魂が縛られました";
    private static final String SOULBIND_RELEASE_KEYWORD = "呪縛が解けました";
    private static final Text WARNING_TITLE = new LiteralText("Soulbind Protection");
    private static final Text WARNING_BODY = new LiteralText("ほんとにいいの？ まだヴェルサリオンの呪縛があるよ？\nこのまま切断すると死亡する可能性があります。");
    private static final Text DISCONNECT_TEXT = new LiteralText("Disconnect");
    private static final Text GO_BACK_TEXT = new LiteralText("Go Back");

    private static boolean soulbound;
    private static boolean allowDisconnect;

    private HaSoulbindProtection() {
    }

    public static void onGameMessage(Text message) {
        String normalized = normalize(message);
        if (isSoulbindStartMessage(normalized)) {
            soulbound = true;
            allowDisconnect = false;
            return;
        }

        if (isSoulbindReleaseMessage(normalized)) {
            reset();
        }
    }

    public static boolean interceptDisconnect(MinecraftClient client, Screen nextScreen) {
        if (!shouldBlockDisconnect(client)) {
            return false;
        }

        Screen returnScreen = client.currentScreen;
        openConfirmation(client, returnScreen, new Runnable() {
            @Override
            public void run() {
                if (nextScreen != null) {
                    client.disconnect(nextScreen);
                } else {
                    client.disconnect(new MultiplayerScreen(new TitleScreen()));
                }
            }
        });
        return true;
    }

    public static boolean interceptPauseMenuDisconnect(MinecraftClient client, Screen returnScreen, Runnable disconnectAction) {
        if (!shouldBlockDisconnect(client)) {
            return false;
        }

        openConfirmation(client, returnScreen, disconnectAction);
        return true;
    }

    public static void onDisconnected() {
        reset();
    }

    public static boolean isSoulbound() {
        return soulbound;
    }

    private static boolean shouldBlockDisconnect(MinecraftClient client) {
        return client != null
            && client.world != null
            && client.player != null
            && client.getNetworkHandler() != null
            && !client.isInSingleplayer()
            && HaConfig.get().soulbindProtectionEnabled
            && soulbound
            && !allowDisconnect;
    }

    private static void openConfirmation(MinecraftClient client, Screen returnScreen, Runnable disconnectAction) {
        client.openScreen(new ConfirmScreen(result -> {
            if (result) {
                allowDisconnect = true;
                disconnectAction.run();
            } else {
                client.openScreen(returnScreen);
            }
        }, WARNING_TITLE, WARNING_BODY, DISCONNECT_TEXT, GO_BACK_TEXT));
    }

    private static String normalize(Text message) {
        if (message == null) {
            return "";
        }

        String value = message.getString();
        String stripped = Formatting.strip(value);
        return stripped == null ? value : stripped.trim();
    }

    private static boolean isSoulbindStartMessage(String message) {
        return message.contains(SOULBIND_NAME) && message.contains(SOULBIND_START_KEYWORD);
    }

    private static boolean isSoulbindReleaseMessage(String message) {
        return message.contains(SOULBIND_NAME) && message.contains(SOULBIND_RELEASE_KEYWORD);
    }

    private static void reset() {
        soulbound = false;
        allowDisconnect = false;
    }
}
