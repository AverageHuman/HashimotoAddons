package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaSpotifyScreen extends Screen {
    private static final Text TITLE = new LiteralText("Spotify");

    private final Screen parent;

    public HaSpotifyScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int top = 48;
        addButton(new ButtonWidget(centerX - 105, top, 210, 20, new LiteralText("Spotify: " + onOff(config.spotifyEnabled)), button -> {
            config.spotifyEnabled = !config.spotifyEnabled;
            config.save();
            button.setMessage(new LiteralText("Spotify: " + onOff(config.spotifyEnabled)));
        }));

        addButton(new ButtonWidget(centerX - 105, top + 28, 210, 20, new LiteralText("Adjust Overlay Position"), button -> {
            if (client != null) {
                client.openScreen(new HaSpotifyOverlayScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 16, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Shows the current Spotify desktop track in a slim HUD."), this.width / 2, 32, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        HaConfig.get().normalize();
        HaConfig.get().save();
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
    }
}
