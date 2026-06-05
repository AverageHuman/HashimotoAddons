package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaDangerousFeaturesScreen extends Screen {
    private static final Text TITLE = new LiteralText("Dangerous Features");
    private static final int ITEMS_PER_PAGE = 7;
    private static final int TOTAL_ITEMS = 11;

    private final Screen parent;
    private final int page;
    private boolean waitingForMacroToggleKey;

    public HaDangerousFeaturesScreen(Screen parent) {
        this(parent, 0);
    }

    private HaDangerousFeaturesScreen(Screen parent, int page) {
        super(TITLE);
        this.parent = parent;
        this.page = Math.max(0, page);
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
        int top = 44;
        int maxPage = Math.max(0, (TOTAL_ITEMS - 1) / ITEMS_PER_PAGE);
        int currentPage = Math.min(page, maxPage);
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, TOTAL_ITEMS);

        int y = top;
        for (int i = start; i < end; i++) {
            addMenuItem(i, centerX, y, config);
            y += 24;
        }

        if (currentPage > 0) {
            addButton(new ButtonWidget(centerX - 105, this.height - 54, 100, 20, new LiteralText("< Back"), button -> {
                if (client != null) {
                    client.openScreen(new HaDangerousFeaturesScreen(parent, currentPage - 1));
                }
            }));
        }
        if (currentPage < maxPage) {
            addButton(new ButtonWidget(centerX + 5, this.height - 54, 100, 20, new LiteralText("Next >"), button -> {
                if (client != null) {
                    client.openScreen(new HaDangerousFeaturesScreen(parent, currentPage + 1));
                }
            }));
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Go Back"), button -> onClose()));
    }

    private void addMenuItem(int index, int centerX, int y, HaConfig config) {
        switch (index) {
            case 0:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText(waitingForMacroToggleKey ? "Press any key or mouse button..." : "Change Macro Toggle Key"), button -> {
                    waitingForMacroToggleKey = true;
                    button.setMessage(new LiteralText("Press any key or mouse button..."));
                }));
                break;
            case 1:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Default Weapon Position: " + slotName(config.defaultWeaponHotbarSlot)), button -> {
                    config.defaultWeaponHotbarSlot = nextSlot(config.defaultWeaponHotbarSlot);
                    config.save();
                    button.setMessage(new LiteralText("Default Weapon Position: " + slotName(config.defaultWeaponHotbarSlot)));
                }));
                break;
            case 2:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Macro Status HUD: " + onOff(config.macroStatusHudEnabled)), button -> {
                    config.macroStatusHudEnabled = !config.macroStatusHudEnabled;
                    config.save();
                    button.setMessage(new LiteralText("Macro Status HUD: " + onOff(config.macroStatusHudEnabled)));
                }));
                break;
            case 3:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Adjust Macro Status HUD"), button -> {
                    if (client != null) {
                        client.openScreen(new HaMacroStatusOverlayScreen(this));
                    }
                }));
                break;
            case 4:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Extras"), button -> {
                    if (client != null) {
                        client.openScreen(new HaExtrasScreen(this));
                    }
                }));
                break;
            case 5:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Auto Heal"), button -> {
                    if (client != null) {
                        client.openScreen(new HaAutoHealScreen(this));
                    }
                }));
                break;
            case 6:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Item Macro"), button -> {
                    if (client != null) {
                        client.openScreen(new HaMacroListScreen(this, 0));
                    }
                }));
                break;
            case 7:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Chunk Containers"), button -> {
                    if (client != null) {
                        client.openScreen(new HaChunkChestScreen(this));
                    }
                }));
                break;
            case 8:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Mob ESP"), button -> {
                    if (client != null) {
                        client.openScreen(new HaMobEspScreen(this));
                    }
                }));
                break;
            case 9:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("AFK Farming"), button -> {
                    if (client != null) {
                        client.openScreen(new HaAfkFarmingScreen(this));
                    }
                }));
                break;
            case 10:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Alchemy Kiln Assist"), button -> {
                    if (client != null) {
                        client.openScreen(new HaAlchemyKilnAutomationScreen(this));
                    }
                }));
                break;
            default:
                break;
        }
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
            if (!HaKeyCaptureHelper.shouldIgnoreKeyCapture(keyCode)) {
                applyBinding(HaKeyCaptureHelper.keyboard(keyCode, scanCode));
            }
            waitingForMacroToggleKey = false;
            if (client != null) {
                client.openScreen(new HaDangerousFeaturesScreen(parent, page));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForMacroToggleKey) {
            applyBinding(HaKeyCaptureHelper.mouse(button));
            waitingForMacroToggleKey = false;
            if (client != null) {
                client.openScreen(new HaDangerousFeaturesScreen(parent, page));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        return HaKeyCaptureHelper.keyName(key);
    }

    private static String onOff(boolean value) {
        return value ? "§aEnabled" : "§cDisabled";
    }

    private static int nextSlot(int current) {
        return current >= 8 ? 0 : current + 1;
    }

    private static String slotName(int slot) {
        return Integer.toString(slot + 1);
    }

    private static void applyBinding(HaKeyCaptureHelper.InputBinding binding) {
        HaConfig config = HaConfig.get();
        config.macroToggleKeyCode = binding.keyCode;
        config.macroToggleScanCode = binding.scanCode;
        config.macroToggleKeyType = binding.type;
        HaClientMod.updateMacroToggleBinding(config.getMacroToggleKey());
        config.save();
    }
}
