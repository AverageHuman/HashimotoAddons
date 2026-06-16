package com.example.ha;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.ha.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaTickHandler {
    private static final int TICKS_PER_SECOND = 20;
    private static final Pattern MANA_FRACTION_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*/\\s*([0-9]+(?:\\.[0-9]+)?)");
    private boolean openConfigScreenRequested;
    private boolean openBlockGalleryScreenRequested;
    private boolean openHudEditScreenRequested;
    private int healCooldownTicks;
    private final KeyBinding macroToggleKeyBinding;
    private final KeyBinding alchemyKilnAutomationKeyBinding;
    private final KeyBinding cameraToggleKeyBinding;
    private final KeyBinding chestSearchKeyBinding;
    private final KeyBinding gearViewKeyBinding;
    private final KeyBinding waypointCycleKeyBinding;
    private long swapHoldEndWorldTick = -1L;
    private InputUtil.Key simulatedHotbarKey = InputUtil.UNKNOWN_KEY;
    private boolean manaMissingNotified;

    public HaTickHandler(KeyBinding macroToggleKeyBinding, KeyBinding alchemyKilnAutomationKeyBinding, KeyBinding cameraToggleKeyBinding, KeyBinding chestSearchKeyBinding, KeyBinding gearViewKeyBinding, KeyBinding waypointCycleKeyBinding) {
        this.macroToggleKeyBinding = macroToggleKeyBinding;
        this.alchemyKilnAutomationKeyBinding = alchemyKilnAutomationKeyBinding;
        this.cameraToggleKeyBinding = cameraToggleKeyBinding;
        this.chestSearchKeyBinding = chestSearchKeyBinding;
        this.gearViewKeyBinding = gearViewKeyBinding;
        this.waypointCycleKeyBinding = waypointCycleKeyBinding;
    }

    public void requestOpenConfigScreen() {
        openConfigScreenRequested = true;
    }

    public void requestOpenBlockGalleryScreen() {
        openBlockGalleryScreenRequested = true;
    }

    public void requestOpenHudEditScreen() {
        openHudEditScreenRequested = true;
    }

    public void onEndClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        HaConfig config = HaConfig.get();
        config.normalize();
        tickSimulatedHotbarKey(client);
        tickCameraToggle(client);
        tickChestSearchShortcut(client);
        tickGearView(client, config);
        tickWaypointCycle(client);
        HaDropTracker.tick(client);
        HaDropNotifier.tick(client);
        HaChestSearchIndex.get().tick(client);
        HaEvolutionForgeHelper.tick(client);
        HaExpTracker.tick(client);
        HaElementTracker.tick(client);
        HaSubSkillTimer.tick(client, config);
        HaRitualBookTimer.tick(client, config);
        HaSpotify.tick(client, config);
        HaGhostWall.tick(client);
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            tickMacroToggle(client, config);
            tickAlchemyKilnAutomationToggle(client, config);
            HaAlchemyKilnAutomation.tick(client, config);
            HaAfkFarming.tick(client, this);
        }
        HaConfigPersistence.tick(config);

        if (openConfigScreenRequested && client.currentScreen == null) {
            openConfigScreenRequested = false;
            client.openScreen(new HaConfigScreen(null));
            return;
        }
        if (openBlockGalleryScreenRequested && client.currentScreen == null) {
            openBlockGalleryScreenRequested = false;
            client.openScreen(new HaBlockGalleryScreen(null, 0));
            return;
        }
        if (openHudEditScreenRequested && client.currentScreen == null) {
            openHudEditScreenRequested = false;
            client.openScreen(new HaHudEditScreen(null));
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        tickHpAlerts(client, config);
        tickManaAlerts(client, config);
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }
        if (!config.macroEnabled) {
            return;
        }
        if (HaAlchemyKilnAutomation.isRunning()) {
            return;
        }

        tickAutoSwap(client, config);
        tickAutoHeal(client, config);
    }

    private void tickMacroToggle(MinecraftClient client, HaConfig config) {
        if (macroToggleKeyBinding == null) {
            return;
        }
        if (isMacroToggleBlocked(client)) {
            while (macroToggleKeyBinding.wasPressed()) {
                // Consume presses while the player is in inventory or HashimotoAddons screens.
            }
            return;
        }

        while (macroToggleKeyBinding.wasPressed()) {
            config.macroEnabled = !config.macroEnabled;
            if (!config.macroEnabled) {
                healCooldownTicks = 0;
                resetSwapTimers(config);
            } else {
                pressHotbarSlot(client, config.defaultWeaponHotbarSlot);
            }
            config.save();

            ClientPlayerEntity player = client.player;
            if (player != null) {
                String suffix = config.macroEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled";
                player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:Macro " + suffix), false);
            }
        }
    }

    private void tickAutoSwap(MinecraftClient client, HaConfig config) {
        if (isSwapHolding(client)) {
            return;
        }

        for (HaConfig.SwapEntry entry : config.swapEntries) {
            int intervalTicks = Math.max(1, (int) Math.round(entry.intervalSeconds * TICKS_PER_SECOND));
            entry.elapsedTicks++;
            if (entry.elapsedTicks >= intervalTicks) {
                entry.elapsedTicks = 0;
                simulateHotbarKeyPress(client, entry.hotbarSlot, entry.holdTicks);
            }
        }
    }

    private void tickCameraToggle(MinecraftClient client) {
        if (isCameraToggleBlocked(client)) {
            while (cameraToggleKeyBinding.wasPressed()) {
                // Consume presses while the player is in inventory or HashimotoAddons screens.
            }
            return;
        }

        while (cameraToggleKeyBinding.wasPressed()) {
            Perspective current = client.options.getPerspective();
            Perspective next = current == Perspective.FIRST_PERSON ? Perspective.THIRD_PERSON_BACK : Perspective.FIRST_PERSON;
            client.options.setPerspective(next);
        }
    }

    private void tickAlchemyKilnAutomationToggle(MinecraftClient client, HaConfig config) {
        if (alchemyKilnAutomationKeyBinding == null) {
            return;
        }
        while (alchemyKilnAutomationKeyBinding.wasPressed()) {
            if (!config.alchemyKilnAutomationEnabled) {
                if (client.player != null) {
                    client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:\u00a7cAlchemy Kiln Assist is OFF."), false);
                }
                continue;
            }
            if (HaAlchemyKilnAutomation.isRunning()) {
                HaAlchemyKilnAutomation.stop(client, "Alchemy Kiln Assist stopped.");
            } else {
                HaAlchemyKilnAutomation.start(client, config);
            }
        }
    }

    private void tickChestSearchShortcut(MinecraftClient client) {
        if (isChestSearchShortcutBlocked(client)) {
            while (chestSearchKeyBinding.wasPressed()) {
                // Consume presses while another screen is open.
            }
            return;
        }

        while (chestSearchKeyBinding.wasPressed()) {
            client.openScreen(new HaChestSearchScreen(null));
        }
    }

    private void tickGearView(MinecraftClient client, HaConfig config) {
        if (gearViewKeyBinding == null) {
            return;
        }
        if (client.currentScreen != null) {
            while (gearViewKeyBinding.wasPressed()) {
                // Consume presses while another screen is open.
            }
            return;
        }
        while (gearViewKeyBinding.wasPressed()) {
            if (!config.gearViewEnabled) {
                if (client.player != null) {
                    client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:\u00a7cGear View is OFF."), false);
                }
                continue;
            }
            if (client.crosshairTarget == null || client.crosshairTarget.getType() != net.minecraft.util.hit.HitResult.Type.ENTITY) {
                if (client.player != null) {
                    client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:\u00a7cAim at a player first."), false);
                }
                continue;
            }
            if (!(client.targetedEntity instanceof PlayerEntity)) {
                if (client.player != null) {
                    client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:\u00a7cAim at another player first."), false);
                }
                continue;
            }
            HaGearView.showTargetGear(client);
        }
    }

    private void tickWaypointCycle(MinecraftClient client) {
        if (waypointCycleKeyBinding == null) {
            return;
        }
        if (client.currentScreen != null) {
            while (waypointCycleKeyBinding.wasPressed()) {
                // Consume presses while another screen is open.
            }
            return;
        }

        while (waypointCycleKeyBinding.wasPressed()) {
            int slot = HaWaypointManager.cycleActiveColorSlot();
            if (client.player != null) {
                client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:Waypoint color slot -> " + (slot + 1)), false);
            }
        }
    }

    private void tickAutoHeal(MinecraftClient client, HaConfig config) {
        if (!config.autoHealEnabled) {
            healCooldownTicks = 0;
            return;
        }

        if (healCooldownTicks > 0) {
            healCooldownTicks--;
            return;
        }

        ClientPlayerEntity player = client.player;
        float maxHealth = player.getMaxHealth();
        float ratio = maxHealth > 0.0F ? (player.getHealth() / maxHealth) : 1.0F;
        if (ratio <= config.autoHealHealthRatioThreshold) {
            simulateHotbarKeyPress(client, config.autoHealHotbarSlot, 0);
            healCooldownTicks = Math.max(1, (int) Math.round(config.autoHealCooldownSeconds * TICKS_PER_SECOND));
        }
    }

    private void tickHpAlerts(MinecraftClient client, HaConfig config) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        float maxHealth = player.getMaxHealth();
        float ratio = maxHealth > 0.0F ? (player.getHealth() / maxHealth) : 1.0F;
        int percentage = Math.round(ratio * 100.0F);

        for (HaConfig.HpAlertEntry entry : config.hpAlertEntries) {
            if (!entry.enabled) {
                entry.triggered = false;
                continue;
            }

            if (percentage <= entry.healthPercentage) {
                if (!entry.triggered) {
                    entry.triggered = true;
                    client.inGameHud.setTitles(
                        new LiteralText(HaConfig.TITLE_COLORS[entry.colorIndex] + entry.titleText),
                        new LiteralText(HaConfig.TITLE_COLORS[entry.colorIndex] + percentage + "%"),
                        5,
                        30,
                        10
                    );
                    player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.9F, 0.7F);
                }
            } else {
                entry.triggered = false;
            }
        }
    }

    private void tickManaAlerts(MinecraftClient client, HaConfig config) {
        if (!hasEnabledManaAlerts(config)) {
            resetManaAlertTriggers(config);
            manaMissingNotified = false;
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        ManaStatus manaStatus = readManaStatus(client);
        if (manaStatus == null) {
            resetManaAlertTriggers(config);
            if (!manaMissingNotified) {
                player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:\u00a7cMana\u304c\u691c\u77e5\u3055\u308c\u307e\u305b\u3093\u3067\u3057\u305f\u3002\u958b\u767a\u8005\u306b\u5831\u544a\u3057\u3066\u304f\u308c\u3002"), false);
                manaMissingNotified = true;
            }
            return;
        }

        manaMissingNotified = false;
        for (HaConfig.ManaAlertEntry entry : config.manaAlertEntries) {
            if (!entry.enabled) {
                entry.triggered = false;
                continue;
            }

            if (manaStatus.percentage <= entry.manaPercentage) {
                if (!entry.triggered) {
                    entry.triggered = true;
                    client.inGameHud.setTitles(
                        new LiteralText(HaConfig.TITLE_COLORS[entry.colorIndex] + entry.titleText),
                        new LiteralText(HaConfig.TITLE_COLORS[entry.colorIndex] + manaStatus.percentage + "%"),
                        5,
                        30,
                        10
                    );
                    player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.9F, 1.0F);
                }
            } else {
                entry.triggered = false;
            }
        }
    }

    private void pressHotbarSlot(MinecraftClient client, int slot) {
        pressHotbarSlot(client, slot, 0);
    }

    private void pressHotbarSlot(MinecraftClient client, int slot, int holdTicks) {
        ClientPlayerEntity player = client.player;
        if (player == null || slot < 0 || slot > 8) {
            return;
        }

        if (holdTicks > 0 && client.world != null) {
            swapHoldEndWorldTick = client.world.getTime() + holdTicks;
        }

        player.inventory.selectedSlot = slot;
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void simulateHotbarKeyPress(MinecraftClient client, int slot, int holdTicks) {
        if (client.player == null || client.world == null || slot < 0 || slot > 8) {
            return;
        }

        KeyBinding hotbarKeyBinding = client.options.keysHotbar[slot];
        InputUtil.Key key = ((KeyBindingAccessor) hotbarKeyBinding).ha$getBoundKey();
        if (key == InputUtil.UNKNOWN_KEY) {
            pressHotbarSlot(client, slot, holdTicks);
            return;
        }

        releaseSimulatedHotbarKey();
        int effectiveHoldTicks = Math.max(1, holdTicks);
        swapHoldEndWorldTick = client.world.getTime() + effectiveHoldTicks;
        simulatedHotbarKey = key;
        KeyBinding.setKeyPressed(key, true);
        KeyBinding.onKeyPressed(key);
    }

    boolean triggerSwapEntry(MinecraftClient client, HaConfig.SwapEntry entry) {
        if (entry == null || isSwapHolding(client)) {
            return false;
        }
        simulateHotbarKeyPress(client, entry.hotbarSlot, entry.holdTicks);
        return true;
    }

    private void tickSimulatedHotbarKey(MinecraftClient client) {
        if (simulatedHotbarKey == InputUtil.UNKNOWN_KEY || client.world == null) {
            return;
        }

        if (!isSwapHolding(client)) {
            releaseSimulatedHotbarKey();
        }
    }

    private void releaseSimulatedHotbarKey() {
        if (simulatedHotbarKey != InputUtil.UNKNOWN_KEY) {
            KeyBinding.setKeyPressed(simulatedHotbarKey, false);
            simulatedHotbarKey = InputUtil.UNKNOWN_KEY;
        }
        swapHoldEndWorldTick = -1L;
    }

    private void resetSwapTimers(HaConfig config) {
        for (HaConfig.SwapEntry entry : config.swapEntries) {
            entry.elapsedTicks = 0;
        }
        releaseSimulatedHotbarKey();
    }

    private boolean isSwapHolding(MinecraftClient client) {
        return swapHoldEndWorldTick >= 0L && client.world != null && client.world.getTime() < swapHoldEndWorldTick;
    }

    private boolean isMacroToggleBlocked(MinecraftClient client) {
        return client.currentScreen instanceof HandledScreen
            || client.currentScreen instanceof HaConfigScreen
            || client.currentScreen instanceof HaHudEditScreen
            || client.currentScreen instanceof HaDangerousFeaturesScreen
            || client.currentScreen instanceof HaMacroStatusOverlayScreen
            || client.currentScreen instanceof HaExtrasScreen
            || client.currentScreen instanceof HaBlockGalleryScreen
            || client.currentScreen instanceof HaExtrasOverlayScreen
            || client.currentScreen instanceof HaAutoHealScreen
            || client.currentScreen instanceof HaMacroListScreen
            || client.currentScreen instanceof HaMacroEditScreen
            || client.currentScreen instanceof HaHpAlertListScreen
            || client.currentScreen instanceof HaHpAlertEditScreen
            || client.currentScreen instanceof HaChunkChestScreen
            || client.currentScreen instanceof HaChunkChestOverlayScreen
            || client.currentScreen instanceof HaChestSearchScreen
            || client.currentScreen instanceof HaGearViewScreen
            || client.currentScreen instanceof HaEvolutionForgeScreen
            || client.currentScreen instanceof HaManaAlertListScreen
            || client.currentScreen instanceof HaManaAlertEditScreen
            || client.currentScreen instanceof HaChatFilterListScreen
            || client.currentScreen instanceof HaChatFilterManageScreen
            || client.currentScreen instanceof HaChatFilterEditScreen
            || client.currentScreen instanceof HaDropTrackerScreen
            || client.currentScreen instanceof HaDropTrackerRegisteredListScreen
            || client.currentScreen instanceof HaDropTrackerRegisteredEditScreen
            || client.currentScreen instanceof HaDropTrackerOverlayScreen
            || client.currentScreen instanceof HaDropNotifierScreen
            || client.currentScreen instanceof HaDropNotifierManageScreen
            || client.currentScreen instanceof HaDropNotifierEditScreen
            || client.currentScreen instanceof HaAfkFarmingScreen
            || client.currentScreen instanceof HaAlchemyKilnAutomationScreen
            || client.currentScreen instanceof HaExpTrackerScreen
            || client.currentScreen instanceof HaExpTrackerOverlayScreen
            || client.currentScreen instanceof HaElementTrackerScreen
            || client.currentScreen instanceof HaElementTrackerTargetScreen
            || client.currentScreen instanceof HaElementTrackerOverlayScreen
            || client.currentScreen instanceof HaMobEspScreen
            || client.currentScreen instanceof HaSpotifyScreen
            || client.currentScreen instanceof HaSpotifyOverlayScreen
            || client.currentScreen instanceof HaWaypointScreen
            || client.currentScreen instanceof HaWaypointListScreen
            || client.currentScreen instanceof HaWaypointLabelScreen
            || client.currentScreen instanceof HaWaypointColorSelectScreen;
    }

    private boolean isCameraToggleBlocked(MinecraftClient client) {
        return isMacroToggleBlocked(client)
            || client.currentScreen instanceof HaCameraScreen;
    }

    private boolean isChestSearchShortcutBlocked(MinecraftClient client) {
        return client.currentScreen != null;
    }

    private static boolean hasEnabledManaAlerts(HaConfig config) {
        for (HaConfig.ManaAlertEntry entry : config.manaAlertEntries) {
            if (entry.enabled) {
                return true;
            }
        }
        return false;
    }

    private static void resetManaAlertTriggers(HaConfig config) {
        for (HaConfig.ManaAlertEntry entry : config.manaAlertEntries) {
            entry.triggered = false;
        }
    }

    private static ManaStatus readManaStatus(MinecraftClient client) {
        if (client.world == null) {
            return null;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        if (scoreboard == null) {
            return null;
        }

        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(1);
        if (objective == null) {
            return null;
        }

        List<String> lines = getSidebarLines(scoreboard, objective);
        for (int i = 0; i < lines.size(); i++) {
            if (!isManaLabelLine(lines.get(i))) {
                continue;
            }

            ManaStatus inlineStatus = parseManaFraction(lines.get(i));
            if (inlineStatus != null) {
                return inlineStatus;
            }

            int start = Math.max(0, i - 1);
            int end = Math.min(lines.size() - 1, i + 2);
            for (int candidateIndex = start; candidateIndex <= end; candidateIndex++) {
                if (candidateIndex == i) {
                    continue;
                }

                ManaStatus nearbyStatus = parseManaFraction(lines.get(candidateIndex));
                if (nearbyStatus != null) {
                    return nearbyStatus;
                }
            }
        }

        return null;
    }

    private static List<String> getSidebarLines(Scoreboard scoreboard, ScoreboardObjective objective) {
        Collection<ScoreboardPlayerScore> scores = scoreboard.getAllPlayerScores(objective);
        List<ScoreboardPlayerScore> visibleScores = new ArrayList<ScoreboardPlayerScore>();
        for (ScoreboardPlayerScore score : scores) {
            if (score != null && score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                visibleScores.add(score);
            }
        }

        Collections.sort(visibleScores, new Comparator<ScoreboardPlayerScore>() {
            @Override
            public int compare(ScoreboardPlayerScore left, ScoreboardPlayerScore right) {
                int scoreCompare = Integer.compare(left.getScore(), right.getScore());
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return right.getPlayerName().compareToIgnoreCase(left.getPlayerName());
            }
        });

        if (visibleScores.size() > 15) {
            visibleScores = new ArrayList<ScoreboardPlayerScore>(visibleScores.subList(visibleScores.size() - 15, visibleScores.size()));
        }

        List<String> lines = new ArrayList<String>();
        for (int i = visibleScores.size() - 1; i >= 0; i--) {
            ScoreboardPlayerScore score = visibleScores.get(i);
            Team team = scoreboard.getPlayerTeam(score.getPlayerName());
            lines.add(Team.decorateName(team, new LiteralText(score.getPlayerName())).getString());
        }
        return lines;
    }

    private static String normalizeScoreboardLine(String value) {
        if (value == null) {
            return "";
        }

        String stripped = Formatting.strip(toAsciiWidth(value));
        if (stripped == null) {
            stripped = value;
        }
        return stripped.replace('\u3000', ' ').trim().toUpperCase();
    }

    private static boolean isManaLabelLine(String value) {
        String normalized = normalizeScoreboardLine(value);
        return normalized.contains("MANA") || normalized.replace(" ", "").contains("MP");
    }

    private static ManaStatus parseManaFraction(String value) {
        String normalized = normalizeScoreboardLine(value);
        Matcher matcher = MANA_FRACTION_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }

        try {
            double current = Double.parseDouble(matcher.group(1));
            double max = Double.parseDouble(matcher.group(2));
            if (max <= 0.0D) {
                return null;
            }
            int percentage = (int) Math.round((current / max) * 100.0D);
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 100) {
                percentage = 100;
            }
            return new ManaStatus(current, max, percentage);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String toAsciiWidth(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\uFF10' && ch <= '\uFF19') {
                result.append((char) ('0' + (ch - '\uFF10')));
            } else if (ch == '\uFF0E') {
                result.append('.');
            } else if (ch == '\uFF0F') {
                result.append('/');
            } else if (ch == '\u2215') {
                result.append('/');
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static final class ManaStatus {
        final double current;
        final double max;
        final int percentage;

        ManaStatus(double current, double max, int percentage) {
            this.current = current;
            this.max = max;
            this.percentage = percentage;
        }
    }
}
