package com.example.ha;

import com.example.ha.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class HaAlchemyKilnAutomation {
    private static final String TICKET_NAME = "便利系ショートカットアクセスチケット";
    private static final String KILN_ENTRY_NAME = "錬金釜";
    private static final String KILN_SCREEN_TITLE = "錬金釜 (1/1)";
    private static final String GOLD_NAME = "とこしえの金塊";
    private static final String MATERIALS_MISSING_MESSAGE = "このアイテムの製作に必要な素材が揃っていません。";
    private static final String SUPER_KILN_TICKET_NAME = "超高性能錬金釜アクセスチケット";
    private static final String SUPER_KILN_SCREEN_TITLE = "超高性能錬金釜";
    private static final int SHORTCUT_WAIT_TIMEOUT_TICKS = 60;
    private static final int KILN_WAIT_TIMEOUT_TICKS = 60;
    private static final int CRAFT_RESULT_TIMEOUT_TICKS = 200;
    private static final int KILN_ENTRY_CLICK_DELAY_TICKS = 20;
    private static final int MIN_CLICK_INTERVAL_TICKS = 4;
    private static final int TICKET_USE_DELAY_TICKS = 4;
    private static final int TICKET_USE_RETRY_INTERVAL_TICKS = 4;
    private static final int OFFHAND_SWAP_SETTLE_TICKS = 2;
    private static final int HUD_MARKER_RECENT_TICKS = 80;
    private static final String OFFHAND_PROTECT_MARKER = "\u2605[ 2 ]";
    private static final String STAGE_ONE_TARGET_PARTIAL = "\u88fd\u4f5c 10 x \u3068\u3053\u3057\u3048\u306e\u91d1\u584a";
    private static final String STAGE_TWO_TARGET_PARTIAL = "\u88fd\u4f5c \u3068\u3053\u3057\u3048\u306e\u91d1\u584a";

    private static State state = State.IDLE;
    private static int originalHotbarSlot = -1;
    private static int ticketHotbarSlot = -1;
    private static int kilnEntrySlotIndex = -1;
    private static long stateStartTick;
    private static long lastCraftClickTick = -1L;
    private static long lastTicketUseTick = -1L;
    private static long nextCraftAllowedTick = 0L;
    private static int lastGoldCount;
    private static int ticketUseAttempts;
    private static boolean waitingForGoldIncrease;
    private static boolean materialsMissingReceived;
    private static boolean protectMarkedItemWithOffhand;
    private static boolean markedItemMovedToOffhand;
    private static boolean restoreOriginalHotbarOnReset = true;
    private static String latestHudMessage = "";
    private static long latestHudMessageTick = -1L;

    private HaAlchemyKilnAutomation() {
    }

    public static boolean isRunning() {
        return state != State.IDLE;
    }

    public static void start(MinecraftClient client, HaConfig config) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED || client == null || client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }
        if (isRunning()) {
            return;
        }
        if (client.currentScreen != null) {
            sendClientMessage(client, "\u00a7cClose the current screen before starting Alchemy Kiln Assist.");
            return;
        }

        originalHotbarSlot = client.player.inventory.selectedSlot;
        ticketHotbarSlot = -1;
        kilnEntrySlotIndex = -1;
        lastGoldCount = countGold(client.player);
        protectMarkedItemWithOffhand = recentHudMessageHasOffhandProtectMarker(client);
        markedItemMovedToOffhand = false;
        state = State.SELECTING_TICKET;
        stateStartTick = client.world.getTime();
        lastCraftClickTick = -1L;
        lastTicketUseTick = -1L;
        nextCraftAllowedTick = 0L;
        ticketUseAttempts = 0;
        waitingForGoldIncrease = false;
        materialsMissingReceived = false;
        restoreOriginalHotbarOnReset = true;
        sendClientMessage(client, "\u00a7aAlchemy Kiln Assist started.");
        tick(client, config);
    }

    public static void stop(MinecraftClient client, String reason) {
        if (!isRunning()) {
            return;
        }
        if (reason != null && !reason.isEmpty()) {
            sendClientMessage(client, reason);
        }
        reset(client);
    }

    public static void onDisconnected() {
        reset(MinecraftClient.getInstance());
    }

    public static void onGameMessage(Text message) {
        if (!isRunning() || message == null) {
            return;
        }
        String normalized = normalize(message.getString());
        if (MATERIALS_MISSING_MESSAGE.equals(normalized)) {
            materialsMissingReceived = true;
        }
    }

    public static void onHudMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        latestHudMessage = message == null ? "" : normalize(message.getString());
        latestHudMessageTick = client != null && client.world != null ? client.world.getTime() : -1L;
    }

    public static void tick(MinecraftClient client, HaConfig config) {
        if (!isRunning()) {
            return;
        }
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
            reset(client);
            return;
        }
        if (!config.alchemyKilnAutomationEnabled) {
            stop(client, "\u00a7cAlchemy Kiln Assist was disabled in settings.");
            return;
        }

        switch (state) {
            case SELECTING_TICKET:
                tickSelectingTicket(client);
                break;
            case MOVING_MARKED_ITEM_TO_OFFHAND:
                tickMovingMarkedItemToOffhand(client);
                break;
            case USING_TICKET:
                tickUsingTicket(client);
                break;
            case WAITING_SHORTCUT_GUI:
                tickWaitingShortcutGui(client);
                break;
            case CLICKING_KILN_ENTRY:
                tickClickingKilnEntry(client);
                break;
            case WAITING_KILN_GUI:
                tickWaitingKilnGui(client, config);
                break;
            case CRAFTING_STAGE_ONE:
                tickCrafting(client, config, STAGE_ONE_TARGET_PARTIAL, true);
                break;
            case CRAFTING_STAGE_TWO:
                tickCrafting(client, config, STAGE_TWO_TARGET_PARTIAL, false);
                break;
            case STOPPED:
                reset(client);
                break;
            case IDLE:
            default:
                break;
        }
    }

    private static void tickSelectingTicket(MinecraftClient client) {
        int foundSlot = findTicketSlot(client.player);
        if (foundSlot < 0) {
            fail(client, "\u00a7cShortcut ticket not found in hotbar.");
            return;
        }
        ticketHotbarSlot = foundSlot;
        if (protectMarkedItemWithOffhand && !markedItemMovedToOffhand) {
            if (!swapWithOffhand(client)) {
                fail(client, "\u00a7cCould not move the marked item to offhand.");
                return;
            }
            markedItemMovedToOffhand = true;
            advanceState(client, State.MOVING_MARKED_ITEM_TO_OFFHAND);
            return;
        }
        selectHotbarSlot(client, ticketHotbarSlot);
        advanceState(client, State.USING_TICKET);
    }

    private static void tickMovingMarkedItemToOffhand(MinecraftClient client) {
        if (client.world == null || client.world.getTime() - stateStartTick < OFFHAND_SWAP_SETTLE_TICKS) {
            return;
        }
        if (ticketHotbarSlot < 0) {
            fail(client, "\u00a7cShortcut ticket was no longer available.");
            return;
        }
        selectHotbarSlot(client, ticketHotbarSlot);
        advanceState(client, State.USING_TICKET);
    }

    private static void tickUsingTicket(MinecraftClient client) {
        if (client.currentScreen != null) {
            advanceState(client, State.WAITING_SHORTCUT_GUI);
            return;
        }
        if (ticketHotbarSlot < 0 || ticketHotbarSlot > 8) {
            fail(client, "\u00a7cShortcut ticket was no longer available.");
            return;
        }
        if (!isTicketInMainHand(client.player, ticketHotbarSlot)) {
            selectHotbarSlot(client, ticketHotbarSlot);
            advanceState(client, State.USING_TICKET);
            return;
        }
        if (client.player.inventory.selectedSlot != ticketHotbarSlot) {
            selectHotbarSlot(client, ticketHotbarSlot);
            advanceState(client, State.USING_TICKET);
            return;
        }
        if (client.world == null) {
            return;
        }
        long now = client.world.getTime();
        if (now - stateStartTick < TICKET_USE_DELAY_TICKS) {
            return;
        }
        if (timedOut(client, SHORTCUT_WAIT_TIMEOUT_TICKS)) {
            fail(client, "\u00a7cTimed out trying to use the shortcut ticket.");
            return;
        }
        if (lastTicketUseTick >= 0L && now - lastTicketUseTick < TICKET_USE_RETRY_INTERVAL_TICKS) {
            return;
        }
        ((MinecraftClientAccessor) client).ha$doItemUse();
        lastTicketUseTick = now;
        ticketUseAttempts++;
    }

    private static void tickWaitingShortcutGui(MinecraftClient client) {
        if (timedOut(client, SHORTCUT_WAIT_TIMEOUT_TICKS)) {
            fail(client, "\u00a7cTimed out waiting for the shortcut GUI.");
            return;
        }
        if (client.currentScreen == null) {
            return;
        }
        GenericContainerScreenHandler handler = getGenericContainerHandler(client);
        if (handler == null) {
            fail(client, "\u00a7cUnexpected screen opened after using the shortcut ticket.");
            return;
        }
        int superKilnTicketSlotIndex = findContainerSlotByNameAnywhere(handler, SUPER_KILN_TICKET_NAME);
        if (superKilnTicketSlotIndex >= 0) {
            kilnEntrySlotIndex = superKilnTicketSlotIndex;
        } else {
            kilnEntrySlotIndex = findContainerSlotByName(handler, KILN_ENTRY_NAME);
        }
        if (kilnEntrySlotIndex < 0) {
            fail(client, "\u00a7cShortcut GUI did not contain the expected 錬金釜 item.");
            return;
        }
        advanceState(client, State.CLICKING_KILN_ENTRY);
    }

    private static void tickClickingKilnEntry(MinecraftClient client) {
        if (client.world == null || client.world.getTime() - stateStartTick < KILN_ENTRY_CLICK_DELAY_TICKS) {
            return;
        }
        if (!clickContainerSlotByIndex(client, kilnEntrySlotIndex, KILN_ENTRY_NAME)) {
            return;
        }
        advanceState(client, State.WAITING_KILN_GUI);
    }

    private static void tickWaitingKilnGui(MinecraftClient client, HaConfig config) {
        if (timedOut(client, KILN_WAIT_TIMEOUT_TICKS)) {
            failWithoutRestoring(client, "\u00a7cTimed out waiting for the 錬金釜 GUI.");
            return;
        }
        if (client.currentScreen == null) {
            return;
        }
        GenericContainerScreenHandler handler = getGenericContainerHandler(client);
        if (handler == null) {
            failWithoutRestoring(client, "\u00a7cUnexpected screen opened after clicking 錬金釜.");
            return;
        }
        if (!isKilnScreenTitle(client.currentScreen.getTitle().getString())) {
            failWithoutRestoring(client, "\u00a7cDid not reach the 錬金釜 (1/1) GUI.");
            return;
        }
        enterCraftStage(client, config, State.CRAFTING_STAGE_ONE, getClickIntervalTicks(config) * 3);
    }

    private static void tickCrafting(MinecraftClient client, HaConfig config, String expectedPartialName, boolean stageOne) {
        GenericContainerScreenHandler handler = getGenericContainerHandler(client);
        if (handler == null) {
            failWithoutRestoring(client, "\u00a7cAlchemy Kiln GUI was closed.");
            return;
        }
        if (!isKilnScreenTitle(client.currentScreen.getTitle().getString())) {
            failWithoutRestoring(client, "\u00a7cUnexpected GUI while crafting.");
            return;
        }
        if (materialsMissingReceived) {
            materialsMissingReceived = false;
            if (stageOne) {
                enterCraftStage(client, config, State.CRAFTING_STAGE_TWO, getClickIntervalTicks(config) * 2);
            } else {
                finish(client);
            }
            return;
        }

        long now = client.world.getTime();
        int currentGoldCount = countGold(client.player);
        if (waitingForGoldIncrease) {
            if (currentGoldCount > lastGoldCount) {
                waitingForGoldIncrease = false;
                lastGoldCount = currentGoldCount;
            } else if (now - stateStartTick > CRAFT_RESULT_TIMEOUT_TICKS) {
                failWithoutRestoring(client, "\u00a7cTimed out waiting for " + GOLD_NAME + " to increase.");
            }
            return;
        }
        int clickIntervalTicks = getClickIntervalTicks(config);
        if (lastCraftClickTick >= 0L && now - lastCraftClickTick < clickIntervalTicks) {
            return;
        }
        if (now < nextCraftAllowedTick) {
            return;
        }
        int targetSlotIndex = findContainerSlotContaining(handler, expectedPartialName);
        if (targetSlotIndex < 0) {
            failWithoutRestoring(client, "\u00a7cCould not find a crafting option containing " + expectedPartialName + ".");
            return;
        }
        if (!clickContainerSlotByIndex(client, targetSlotIndex, expectedPartialName)) {
            return;
        }
        lastCraftClickTick = now;
        nextCraftAllowedTick = now + clickIntervalTicks;
        stateStartTick = now;
        waitingForGoldIncrease = true;
    }

    private static void enterCraftStage(MinecraftClient client, HaConfig config, State nextState, int extraDelayTicks) {
        long now = client != null && client.world != null ? client.world.getTime() : 0L;
        lastGoldCount = client != null && client.player != null ? countGold(client.player) : 0;
        lastCraftClickTick = -1L;
        nextCraftAllowedTick = now + Math.max(0, extraDelayTicks);
        waitingForGoldIncrease = false;
        materialsMissingReceived = false;
        advanceState(client, nextState);
    }

    private static void finish(MinecraftClient client) {
        if (client != null && client.player != null && client.currentScreen != null) {
            client.player.closeHandledScreen();
        }
        stop(client, "\u00a7aAlchemy Kiln Assist finished.");
    }

    private static boolean clickContainerSlotByIndex(MinecraftClient client, int slotIndex, String expectedLabel) {
        GenericContainerScreenHandler handler = getGenericContainerHandler(client);
        if (handler == null) {
            fail(client, "\u00a7cExpected a container GUI, but none was open.");
            return false;
        }
        if (slotIndex < 0 || slotIndex >= handler.getRows() * 9 || slotIndex >= handler.slots.size()) {
            fail(client, "\u00a7cExpected slot for " + expectedLabel + " was not available.");
            return false;
        }
        Slot slot = handler.slots.get(slotIndex);
        if (slot == null || slot.getStack().isEmpty()) {
            fail(client, "\u00a7cExpected slot for " + expectedLabel + " was empty.");
            return false;
        }
        client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
        return true;
    }

    private static int findContainerSlotByName(GenericContainerScreenHandler handler, String expectedName) {
        if (handler == null || expectedName == null || expectedName.isEmpty()) {
            return -1;
        }
        int containerSlotCount = handler.getRows() * 9;
        for (int slotIndex = 0; slotIndex < containerSlotCount && slotIndex < handler.slots.size(); slotIndex++) {
            ItemStack stack = handler.slots.get(slotIndex).getStack();
            if (!stack.isEmpty() && expectedName.equals(normalize(stack.getName().getString()))) {
                return slotIndex;
            }
        }
        return -1;
    }

    private static int findContainerSlotContaining(GenericContainerScreenHandler handler, String expectedPartialName) {
        if (handler == null || expectedPartialName == null || expectedPartialName.isEmpty()) {
            return -1;
        }
        int containerSlotCount = handler.getRows() * 9;
        for (int slotIndex = 0; slotIndex < containerSlotCount && slotIndex < handler.slots.size(); slotIndex++) {
            ItemStack stack = handler.slots.get(slotIndex).getStack();
            if (!stack.isEmpty() && normalize(stack.getName().getString()).contains(expectedPartialName)) {
                return slotIndex;
            }
        }
        return -1;
    }

    private static int findContainerSlotByNameAnywhere(GenericContainerScreenHandler handler, String expectedName) {
        if (handler == null || expectedName == null || expectedName.isEmpty()) {
            return -1;
        }
        for (int slotIndex = 0; slotIndex < handler.slots.size(); slotIndex++) {
            ItemStack stack = handler.slots.get(slotIndex).getStack();
            if (!stack.isEmpty() && expectedName.equals(normalize(stack.getName().getString()))) {
                return slotIndex;
            }
        }
        return -1;
    }

    private static boolean isKilnScreenTitle(String title) {
        String normalized = normalize(title);
        return normalized.contains(KILN_SCREEN_TITLE) || normalized.contains(SUPER_KILN_SCREEN_TITLE);
    }

    private static GenericContainerScreenHandler getGenericContainerHandler(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen)) {
            return null;
        }
        if (!(client.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
            return null;
        }
        return (GenericContainerScreenHandler) client.player.currentScreenHandler;
    }

    private static int findTicketSlot(ClientPlayerEntity player) {
        if (player == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.inventory.getStack(slot);
            if (!stack.isEmpty() && TICKET_NAME.equals(normalize(stack.getName().getString()))) {
                return slot;
            }
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.inventory.getStack(slot);
            if (!stack.isEmpty() && SUPER_KILN_TICKET_NAME.equals(normalize(stack.getName().getString()))) {
                return slot;
            }
        }
        return -1;
    }

    private static int countGold(ClientPlayerEntity player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < player.inventory.size(); slot++) {
            ItemStack stack = player.inventory.getStack(slot);
            if (!stack.isEmpty() && GOLD_NAME.equals(normalize(stack.getName().getString()))) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean isTicketInMainHand(ClientPlayerEntity player, int expectedSlot) {
        if (player == null || expectedSlot < 0 || expectedSlot > 8) {
            return false;
        }
        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack expectedStack = player.inventory.getStack(expectedSlot);
        if (mainHandStack == null || mainHandStack.isEmpty() || expectedStack == null || expectedStack.isEmpty()) {
            return false;
        }
        String normalizedName = normalize(mainHandStack.getName().getString());
        return (TICKET_NAME.equals(normalizedName) || SUPER_KILN_TICKET_NAME.equals(normalizedName))
            && mainHandStack == expectedStack;
    }

    private static void selectHotbarSlot(MinecraftClient client, int slot) {
        if (client == null || client.player == null || slot < 0 || slot > 8) {
            return;
        }
        client.player.inventory.selectedSlot = slot;
        client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private static void advanceState(MinecraftClient client, State nextState) {
        state = nextState;
        stateStartTick = client != null && client.world != null ? client.world.getTime() : 0L;
    }

    private static boolean timedOut(MinecraftClient client, int timeoutTicks) {
        return client != null
            && client.world != null
            && timeoutTicks > 0
            && client.world.getTime() - stateStartTick > timeoutTicks;
    }

    private static void fail(MinecraftClient client, String reason) {
        state = State.STOPPED;
        stop(client, reason);
    }

    private static void failWithoutRestoring(MinecraftClient client, String reason) {
        restoreOriginalHotbarOnReset = false;
        fail(client, reason);
    }

    private static void reset(MinecraftClient client) {
        if (restoreOriginalHotbarOnReset && client != null && client.player != null && markedItemMovedToOffhand && originalHotbarSlot >= 0 && originalHotbarSlot <= 8) {
            selectHotbarSlot(client, originalHotbarSlot);
            swapWithOffhand(client);
        } else if (restoreOriginalHotbarOnReset && client != null && client.player != null && originalHotbarSlot >= 0 && originalHotbarSlot <= 8) {
            selectHotbarSlot(client, originalHotbarSlot);
        }
        state = State.IDLE;
        originalHotbarSlot = -1;
        ticketHotbarSlot = -1;
        kilnEntrySlotIndex = -1;
        stateStartTick = 0L;
        lastCraftClickTick = -1L;
        lastTicketUseTick = -1L;
        nextCraftAllowedTick = 0L;
        lastGoldCount = 0;
        ticketUseAttempts = 0;
        waitingForGoldIncrease = false;
        materialsMissingReceived = false;
        protectMarkedItemWithOffhand = false;
        markedItemMovedToOffhand = false;
        restoreOriginalHotbarOnReset = true;
    }

    private static int getClickIntervalTicks(HaConfig config) {
        return Math.max(MIN_CLICK_INTERVAL_TICKS, config.alchemyKilnAutomationClickIntervalTicks);
    }

    private static boolean swapWithOffhand(MinecraftClient client) {
        if (client == null || client.player == null || client.player.networkHandler == null) {
            return false;
        }
        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        return true;
    }

    private static boolean recentHudMessageHasOffhandProtectMarker(MinecraftClient client) {
        if (client == null || client.world == null || latestHudMessage == null || !latestHudMessage.contains(OFFHAND_PROTECT_MARKER)) {
            return false;
        }
        return latestHudMessageTick >= 0L && client.world.getTime() - latestHudMessageTick <= HUD_MARKER_RECENT_TICKS;
    }

    private static void sendClientMessage(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:" + message), false);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Formatting.strip(value);
        return stripped == null ? value.trim() : stripped.trim();
    }

    private enum State {
        IDLE,
        SELECTING_TICKET,
        MOVING_MARKED_ITEM_TO_OFFHAND,
        USING_TICKET,
        WAITING_SHORTCUT_GUI,
        CLICKING_KILN_ENTRY,
        WAITING_KILN_GUI,
        CRAFTING_STAGE_ONE,
        CRAFTING_STAGE_TWO,
        STOPPED
    }
}
