package com.example.ha;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class HaConfigScreen extends Screen {
    private static final Text TITLE = new LiteralText("HashimotoAddons");
    private static final int ITEMS_PER_PAGE = 8;
    private static final String[] CAMERA_TOOLTIP = new String[] {
        "F5 \u306e\u4ee3\u308f\u308a\u306b\u3001\u8a2d\u5b9a\u3057\u305f\u30ad\u30fc\u3067",
        "1\u4eba\u79f0\u3068 3\u4eba\u79f0\u5f8c\u8996\u70b9\u3060\u3051\u3092\u5207\u308a\u66ff\u3048\u307e\u3059\u3002"
    };

    private final Screen parent;
    private int page;
    private int cameraButtonX = -1;
    private int cameraButtonY = -1;
    private int cameraButtonWidth;
    private int cameraButtonHeight;

    public HaConfigScreen(Screen parent) {
        this(parent, 0);
    }

    public HaConfigScreen(Screen parent, int page) {
        super(TITLE);
        this.parent = parent;
        this.page = Math.max(0, page);
    }

    @Override
    protected void init() {
        HaConfig config = HaConfig.get();
        config.normalize();

        int totalItems = HaBuildFlags.DANGEROUS_FEATURES_ENABLED ? 12 : 11;
        int maxPage = Math.max(0, (totalItems - 1) / ITEMS_PER_PAGE);
        page = Math.min(page, maxPage);

        int centerX = this.width / 2;
        int top = 36;
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

        int y = top;
        for (int i = start; i < end; i++) {
            addMenuItem(i, centerX, y, config);
            y += 24;
        }

        if (page > 0) {
            addButton(new ButtonWidget(centerX - 105, this.height - 54, 100, 20, new LiteralText("< Back"), button -> {
                if (client != null) {
                    client.openScreen(new HaConfigScreen(parent, page - 1));
                }
            }));
        }
        if (page < maxPage) {
            addButton(new ButtonWidget(centerX + 5, this.height - 54, 100, 20, new LiteralText("Next >"), button -> {
                if (client != null) {
                    client.openScreen(new HaConfigScreen(parent, page + 1));
                }
            }));
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Done"), button -> onClose()));
    }

    private void addMenuItem(int index, int centerX, int y, HaConfig config) {
        int actualIndex = HaBuildFlags.DANGEROUS_FEATURES_ENABLED ? index : index + 1;
        switch (actualIndex) {
            case 0:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("\u00a7cDangerous Features"), button -> {
                    if (client != null) {
                        client.openScreen(new HaDangerousFeaturesScreen(this));
                    }
                }));
                break;
            case 1:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Item Lock: " + onOff(config.itemLockEnabled)), button -> {
                    config.itemLockEnabled = !config.itemLockEnabled;
                    config.save();
                    button.setMessage(new LiteralText("Item Lock: " + onOff(config.itemLockEnabled)));
                }));
                break;
            case 2:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("HP Alert"), button -> {
                    if (client != null) {
                        client.openScreen(new HaHpAlertListScreen(this, 0));
                    }
                }));
                break;
            case 3:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Mana Alert"), button -> {
                    if (client != null) {
                        client.openScreen(new HaManaAlertListScreen(this, 0));
                    }
                }));
                break;
            case 4:
                cameraButtonX = centerX - 105;
                cameraButtonY = y;
                cameraButtonWidth = 210;
                cameraButtonHeight = 20;
                addButton(new ButtonWidget(cameraButtonX, cameraButtonY, cameraButtonWidth, cameraButtonHeight, new LiteralText("Camera"), button -> {
                    if (client != null) {
                        client.openScreen(new HaCameraScreen(this));
                    }
                }));
                break;
            case 5:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Soulbind Protection: " + onOff(config.soulbindProtectionEnabled)), button -> {
                    config.soulbindProtectionEnabled = !config.soulbindProtectionEnabled;
                    config.save();
                    button.setMessage(new LiteralText("Soulbind Protection: " + onOff(config.soulbindProtectionEnabled)));
                }));
                break;
            case 6:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Chest Search"), button -> {
                    if (client != null) {
                        client.openScreen(new HaChestSearchScreen(this));
                    }
                }));
                break;
            case 7:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Drop Tracker"), button -> {
                    if (client != null) {
                        client.openScreen(new HaDropTrackerScreen(this));
                    }
                }));
                break;
            case 8:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Exp Tracker"), button -> {
                    if (client != null) {
                        client.openScreen(new HaExpTrackerScreen(this));
                    }
                }));
                break;
            case 9:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Chat Filter"), button -> {
                    if (client != null) {
                        client.openScreen(new HaChatFilterListScreen(this, 0));
                    }
                }));
                break;
            case 10:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Evolution Forge Helper"), button -> {
                    if (client != null) {
                        client.openScreen(new HaEvolutionForgeScreen(this));
                    }
                }));
                break;
            case 11:
                addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Mob HP Display"), button -> {
                    if (client != null) {
                        client.openScreen(new HaMobHpDisplayScreen(this));
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
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
        if (isMouseOverCameraButton(mouseX, mouseY)) {
            renderTooltip(matrices, toTextList(CAMERA_TOOLTIP), mouseX, mouseY);
        }
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
        return value ? "ON" : "OFF";
    }

    private boolean isMouseOverCameraButton(int mouseX, int mouseY) {
        return cameraButtonX >= 0
            && mouseX >= cameraButtonX
            && mouseX < cameraButtonX + cameraButtonWidth
            && mouseY >= cameraButtonY
            && mouseY < cameraButtonY + cameraButtonHeight;
    }

    private static java.util.List<Text> toTextList(String[] lines) {
        java.util.List<Text> result = new java.util.ArrayList<Text>();
        for (String line : lines) {
            result.add(new LiteralText(line));
        }
        return result;
    }
}
