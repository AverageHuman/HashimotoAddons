package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class HaDangerousFeaturesScreen extends Screen {
    private static final Text TITLE = new LiteralText("Dangerous Features");

    private final Screen parent;
    private boolean waitingForMacroToggleKey;

    public HaDangerousFeaturesScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            onClose();
            return;
        }

        HaConfig config = HaConfig.get();
        config.normalize();

        int centerX = this.width / 2;
        int y = this.height / 4;

        addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText(waitingForMacroToggleKey ? "Press any key..." : "Change Macro Toggle Key"), button -> {
            waitingForMacroToggleKey = true;
            button.setMessage(new LiteralText("Press any key..."));
        }));

        addButton(new ButtonWidget(centerX - 105, y + 24, 210, 20, new LiteralText("Default Weapon Position: " + slotName(config.defaultWeaponHotbarSlot)), button -> {
            config.defaultWeaponHotbarSlot = nextSlot(config.defaultWeaponHotbarSlot);
            config.save();
            button.setMessage(new LiteralText("Default Weapon Position: " + slotName(config.defaultWeaponHotbarSlot)));
        }));

        addButton(new ButtonWidget(centerX - 105, y + 48, 210, 20, new LiteralText("Macro Status HUD: " + onOff(config.macroStatusHudEnabled)), button -> {
            config.macroStatusHudEnabled = !config.macroStatusHudEnabled;
            config.save();
            button.setMessage(new LiteralText("Macro Status HUD: " + onOff(config.macroStatusHudEnabled)));
        }));

        addButton(new ButtonWidget(centerX - 105, y + 72, 210, 20, new LiteralText("Adjust Macro Status HUD"), button -> {
            if (client != null) {
                client.openScreen(new HaMacroStatusOverlayScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, y + 96, 210, 20, new LiteralText("Auto Heal"), button -> {
            if (client != null) {
                client.openScreen(new HaAutoHealScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, y + 120, 210, 20, new LiteralText("Item Macro"), button -> {
            if (client != null) {
                client.openScreen(new HaMacroListScreen(this, 0));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, y + 144, 210, 20, new LiteralText("Chunk Containers"), button -> {
            if (client != null) {
                client.openScreen(new HaChunkChestScreen(this));
            }
        }));

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 18, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Macro key: " + keyName(HaConfig.get().getMacroToggleKey())), this.width / 2, 28, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForMacroToggleKey) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                HaConfig config = HaConfig.get();
                config.macroToggleKeyCode = keyCode;
                config.macroToggleScanCode = scanCode;
                HaClientMod.updateMacroToggleBinding(config.getMacroToggleKey());
                config.save();
            }
            waitingForMacroToggleKey = false;
            if (client != null) {
                client.openScreen(new HaDangerousFeaturesScreen(parent));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return waitingForMacroToggleKey || super.charTyped(chr, modifiers);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private static String keyName(InputUtil.Key key) {
        return key == InputUtil.UNKNOWN_KEY ? "Unbound" : key.getLocalizedText().getString();
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static int nextSlot(int current) {
        return current >= 8 ? 0 : current + 1;
    }

    private static String slotName(int slot) {
        return Integer.toString(slot + 1);
    }
}
