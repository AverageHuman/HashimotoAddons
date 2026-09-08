package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Warns when Fast Travel is opened while the player has a valuable item that
 * has not been protected against loss on death.
 */
public final class HaItemContractNotifier {
    private static final String FAST_TRAVEL_TITLE = "ファストトラベル";
    private static final String UNTOUCHABLE_RARITY = "UNTOUCHABLE+";
    private static final String UNIQUE_RARITY = "UNIQUE";
    private static final String DEATH_LOSS_PROTECTION_LORE = "死亡時のロストを防ぎます。";
    private static final String LOSS_PROTECTION_LORE = "ロストから保護されます。";
    private static final int SCREEN_OPEN_WAIT_TICKS = 20;
    private static final int ALERT_SOUND_COUNT = 5;
    private static final int ALERT_SOUND_INTERVAL_TICKS = 2;
    private static final float ALERT_VOLUME = 2.0F;
    private static final float ALERT_PITCH = 1.0F;

    private static int pendingNotifierSyncId = -1;
    private static String pendingNotifierTitle = "";
    private static int notifierScreenWaitTicks;

    private static final AlertSoundSchedule ALERT_SOUND_SCHEDULE = new AlertSoundSchedule();

    private HaItemContractNotifier() {
    }

    /**
     * Records the server-supplied title before the screen handler has been
     * created. Player inventory is scanned only after the matching chest screen
     * has been created on the client.
     */
    public static void onOpenScreen(int syncId, Text title) {
        String normalizedTitle = normalize(title == null ? "" : title.getString());
        HaItemContractNotifierConfig config = HaConfig.get().itemContractNotifier;

        clearPendingNotifier();
        if (config.enabled && isFastTravelTitle(normalizedTitle)) {
            pendingNotifierSyncId = syncId;
            pendingNotifierTitle = normalizedTitle;
            notifierScreenWaitTicks = SCREEN_OPEN_WAIT_TICKS;
        }
    }

    public static void tick(MinecraftClient client) {
        tickAlertSoundSequence(client);

        HaItemContractNotifierConfig config = HaConfig.get().itemContractNotifier;
        if (!config.enabled) {
            clearPendingNotifier();
            return;
        }
        if (pendingNotifierSyncId < 0) {
            return;
        }
        if (client == null || client.player == null) {
            clearPendingNotifier();
            return;
        }
        if (client.currentScreen == null
            || client.player.currentScreenHandler == null
            || client.player.currentScreenHandler.syncId != pendingNotifierSyncId) {
            waitForNotifierScreen();
            return;
        }
        if (!(client.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
            clearPendingNotifier();
            return;
        }

        List<List<String>> itemLores = collectPlayerInventoryLores(client.player.inventory);
        boolean shouldWarn = shouldCloseForFastTravel(pendingNotifierTitle, itemLores);
        clearPendingNotifier();
        if (!shouldWarn) {
            return;
        }

        // This sends CloseHandledScreenC2SPacket, keeping the client and server
        // screen state synchronized.
        client.player.closeHandledScreen();
        showWarning(client);
    }

    public static void showTestWarning(MinecraftClient client) {
        showWarning(client);
    }

    public static void onDisconnected() {
        clearPendingNotifier();
        ALERT_SOUND_SCHEDULE.clear();
    }

    static boolean shouldCloseForFastTravel(String rawTitle, List<List<String>> itemLores) {
        if (!isFastTravelTitle(rawTitle) || itemLores == null) {
            return false;
        }
        for (List<String> lore : itemLores) {
            if (isTargetItem(lore) && !hasLossProtection(lore)) {
                return true;
            }
        }
        return false;
    }

    private static List<List<String>> collectPlayerInventoryLores(PlayerInventory inventory) {
        List<List<String>> result = new ArrayList<List<String>>();
        if (inventory == null) {
            return result;
        }
        // PlayerInventory#size includes the main inventory, armor, and offhand.
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack != null && !stack.isEmpty()) {
                result.add(readLore(stack));
            }
        }
        return result;
    }

    private static void showWarning(MinecraftClient client) {
        if (client == null || client.player == null || client.inGameHud == null) {
            return;
        }
        client.inGameHud.setTitles(new LiteralText("\u00a7cAPPLY CONTRACT!!!"), new LiteralText(""), 5, 40, 10);
        playAlertSound(client);
        ALERT_SOUND_SCHEDULE.start();
    }

    private static void tickAlertSoundSequence(MinecraftClient client) {
        if (!ALERT_SOUND_SCHEDULE.tickAndShouldPlay()) {
            return;
        }
        if (client == null || client.player == null) {
            ALERT_SOUND_SCHEDULE.clear();
            return;
        }
        playAlertSound(client);
    }

    private static void playAlertSound(MinecraftClient client) {
        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, ALERT_VOLUME, ALERT_PITCH);
    }

    private static boolean isFastTravelTitle(String rawTitle) {
        return FAST_TRAVEL_TITLE.equals(normalize(rawTitle));
    }

    private static boolean isTargetItem(List<String> lore) {
        return getTargetRarity(lore) != null;
    }

    private static String getTargetRarity(List<String> lore) {
        if (contains(lore, UNTOUCHABLE_RARITY)) {
            return UNTOUCHABLE_RARITY;
        }
        return contains(lore, UNIQUE_RARITY) ? UNIQUE_RARITY : null;
    }

    private static boolean hasLossProtection(List<String> lore) {
        return contains(lore, DEATH_LOSS_PROTECTION_LORE) || contains(lore, LOSS_PROTECTION_LORE);
    }

    private static boolean contains(List<String> lore, String expectedFragment) {
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            if (normalize(line).contains(expectedFragment)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> readLore(ItemStack stack) {
        List<String> result = new ArrayList<String>();
        NbtCompound tag = stack.getTag();
        if (tag == null || !tag.contains("display", 10)) {
            return result;
        }
        NbtCompound display = tag.getCompound("display");
        if (!display.contains("Lore", 9)) {
            return result;
        }

        NbtList lore = display.getList("Lore", 8);
        for (int i = 0; i < lore.size(); i++) {
            String rawLine = lore.getString(i);
            try {
                Text parsed = Text.Serializer.fromJson(rawLine);
                result.add(normalize(parsed == null ? rawLine : parsed.getString()));
            } catch (RuntimeException ignored) {
                // A malformed lore component is scanned as raw text so a
                // possible warning item is not overlooked.
                result.add(normalize(rawLine));
            }
        }
        return result;
    }

    private static String normalize(String value) {
        String stripped = Formatting.strip(value == null ? "" : value);
        return stripped == null ? "" : stripped.trim();
    }

    private static void clearPendingNotifier() {
        pendingNotifierSyncId = -1;
        pendingNotifierTitle = "";
        notifierScreenWaitTicks = 0;
    }

    private static void waitForNotifierScreen() {
        notifierScreenWaitTicks--;
        if (notifierScreenWaitTicks <= 0) {
            clearPendingNotifier();
        }
    }

    static final class AlertSoundSchedule {
        private int pendingSoundCount;
        private int ticksUntilNextSound;

        void start() {
            pendingSoundCount = ALERT_SOUND_COUNT - 1;
            ticksUntilNextSound = ALERT_SOUND_INTERVAL_TICKS;
        }

        boolean tickAndShouldPlay() {
            if (pendingSoundCount <= 0) {
                return false;
            }
            ticksUntilNextSound--;
            if (ticksUntilNextSound > 0) {
                return false;
            }
            pendingSoundCount--;
            ticksUntilNextSound = ALERT_SOUND_INTERVAL_TICKS;
            return true;
        }

        void clear() {
            pendingSoundCount = 0;
            ticksUntilNextSound = 0;
        }
    }
}
