package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
    private TextFieldWidget searchField;
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
        rebuildUi("", true);
    }

    @Override
    public void tick() {
        if (searchField != null) {
            searchField.tick();
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return searchField != null && searchField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return searchField != null && searchField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return searchField != null && searchField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, TITLE, this.width / 2, 12, 0xFFFFFF);
        if (searchField != null) {
            searchField.render(matrices, mouseX, mouseY, delta);
            if (searchField.getText().isEmpty() && !searchField.isFocused()) {
                this.textRenderer.draw(matrices, "Search Feature", this.width / 2 - 100, 42, 0x808080);
            }
        }
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

    private void rebuildUi(String query, boolean focusSearch) {
        this.buttons.clear();
        this.children.clear();
        cameraButtonX = -1;
        cameraButtonY = -1;
        cameraButtonWidth = 0;
        cameraButtonHeight = 0;

        int centerX = this.width / 2;
        searchField = new TextFieldWidget(this.textRenderer, centerX - 105, 36, 210, 20, new LiteralText("Search Feature"));
        searchField.setMaxLength(64);
        searchField.setText(query);
        searchField.setChangedListener(value -> rebuildUi(value, true));
        this.children.add(searchField);
        if (focusSearch) {
            setInitialFocus(searchField);
        }

        HaConfig config = HaConfig.get();
        List<MenuEntry> visibleEntries = getVisibleEntries(config);
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        int y = 64;

        if (normalizedQuery.isEmpty()) {
            int totalItems = visibleEntries.size();
            int maxPage = Math.max(0, (totalItems - 1) / ITEMS_PER_PAGE);
            page = Math.min(page, maxPage);

            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, totalItems);
            for (int i = start; i < end; i++) {
                visibleEntries.get(i).addButton(this, centerX, y, config);
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
        } else {
            List<MenuEntry> matches = filterEntries(visibleEntries, normalizedQuery);
            for (int i = 0; i < matches.size(); i++) {
                matches.get(i).addButton(this, centerX, y, config);
                y += 24;
            }
        }

        addButton(new ButtonWidget(centerX - 105, this.height - 28, 210, 20, new LiteralText("Done"), button -> onClose()));
    }

    private List<MenuEntry> getVisibleEntries(HaConfig config) {
        List<MenuEntry> entries = new ArrayList<MenuEntry>();
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            entries.add(new OpenScreenEntry("\u00a7cDangerous Features") {
                @Override
                protected Screen createScreen(Screen parent) {
                    return new HaDangerousFeaturesScreen(parent);
                }
            });
        }

        entries.add(new MenuEntry("Item Lock") {
            @Override
            void addButton(HaConfigScreen screen, int centerX, int y, HaConfig currentConfig) {
                screen.addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Item Lock: " + onOff(currentConfig.itemLockEnabled)), button -> {
                    currentConfig.itemLockEnabled = !currentConfig.itemLockEnabled;
                    currentConfig.save();
                    button.setMessage(new LiteralText("Item Lock: " + onOff(currentConfig.itemLockEnabled)));
                }));
            }
        });
        entries.add(new OpenScreenEntry("HP Alert") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaHpAlertListScreen(parent, 0);
            }
        });
        entries.add(new OpenScreenEntry("Mana Alert") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaManaAlertListScreen(parent, 0);
            }
        });
        entries.add(new OpenScreenEntry("Camera") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaCameraScreen(parent);
            }

            @Override
            void addButton(HaConfigScreen screen, int centerX, int y, HaConfig currentConfig) {
                screen.cameraButtonX = centerX - 105;
                screen.cameraButtonY = y;
                screen.cameraButtonWidth = 210;
                screen.cameraButtonHeight = 20;
                super.addButton(screen, centerX, y, currentConfig);
            }
        });
        entries.add(new MenuEntry("Soulbind Protection") {
            @Override
            void addButton(HaConfigScreen screen, int centerX, int y, HaConfig currentConfig) {
                screen.addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText("Soulbind Protection: " + onOff(currentConfig.soulbindProtectionEnabled)), button -> {
                    currentConfig.soulbindProtectionEnabled = !currentConfig.soulbindProtectionEnabled;
                    currentConfig.save();
                    button.setMessage(new LiteralText("Soulbind Protection: " + onOff(currentConfig.soulbindProtectionEnabled)));
                }));
            }
        });
        entries.add(new OpenScreenEntry("Chest Search") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaChestSearchScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Damage Truncation") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaDamageTruncationScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Element Rarity") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaElementRarityScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Gear View") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaGearViewScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Waypoint") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaWaypointScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Drop Tracker") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaDropTrackerScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Exp Tracker") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaExpTrackerScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Element Tracker") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaElementTrackerScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Chat Filter") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaChatFilterListScreen(parent, 0);
            }
        });
        entries.add(new OpenScreenEntry("Evolution Forge Helper") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaEvolutionForgeScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Mob HP Display") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaMobHpDisplayScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Sub Skill Timer") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaSubSkillTimerScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Ritual Book Timer") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaRitualBookTimerScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Spotify") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaSpotifyScreen(parent);
            }
        });
        entries.add(new OpenScreenEntry("Drop Notifier") {
            @Override
            protected Screen createScreen(Screen parent) {
                return new HaDropNotifierScreen(parent);
            }
        });
        return entries;
    }

    private static List<MenuEntry> filterEntries(List<MenuEntry> entries, String query) {
        List<MenuEntry> matches = new ArrayList<MenuEntry>();
        for (int i = 0; i < entries.size(); i++) {
            MenuEntry entry = entries.get(i);
            if (entry.searchLabel.toLowerCase(Locale.ROOT).contains(query)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    private boolean isMouseOverCameraButton(int mouseX, int mouseY) {
        return cameraButtonX >= 0
            && mouseX >= cameraButtonX
            && mouseX < cameraButtonX + cameraButtonWidth
            && mouseY >= cameraButtonY
            && mouseY < cameraButtonY + cameraButtonHeight;
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aEnabled" : "\u00a7cDisabled";
    }

    private static List<Text> toTextList(String[] lines) {
        List<Text> result = new ArrayList<Text>();
        for (int i = 0; i < lines.length; i++) {
            result.add(new LiteralText(lines[i]));
        }
        return result;
    }

    private abstract static class MenuEntry {
        protected final String searchLabel;

        MenuEntry(String searchLabel) {
            this.searchLabel = searchLabel;
        }

        abstract void addButton(HaConfigScreen screen, int centerX, int y, HaConfig config);
    }

    private abstract static class OpenScreenEntry extends MenuEntry {
        OpenScreenEntry(String searchLabel) {
            super(searchLabel);
        }

        @Override
        void addButton(HaConfigScreen screen, int centerX, int y, HaConfig config) {
            screen.addButton(new ButtonWidget(centerX - 105, y, 210, 20, new LiteralText(searchLabel), button -> {
                if (screen.client != null) {
                    screen.client.openScreen(createScreen(screen));
                }
            }));
        }

        protected abstract Screen createScreen(Screen parent);
    }
}
