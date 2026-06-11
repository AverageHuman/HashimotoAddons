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
    private static final String TICKET_NAME = "\u8d85\u9ad8\u6027\u80fd\u932c\u91d1\u91dc\u30a2\u30af\u30bb\u30b9\u30c1\u30b1\u30c3\u30c8";
    private static final String KILN_SCREEN_TITLE = "\u8d85\u9ad8\u6027\u80fd\u932c\u91d1\u91dc";
    private static final String STAGE_ONE_TARGET = "\u88fd\u4f5c 10 x \u3068\u3053\u3057\u3048\u306e\u91d1\u584a";
    private static final String STAGE_TWO_TARGET = "\u88fd\u4f5c \u3068\u3053\u3057\u3048\u306e\u91d1\u584a";
    private static final String MATERIALS_MISSING_MESSAGE = "\u3053\u306e\u30a2\u30a4\u30c6\u30e0\u306e\u88fd\u4f5c\u306b\u5fc5\u8981\u306a\u7d20\u6750\u304c\u63c3\u3063\u3066\u3044\u307e\u305b\u3093\u3002";
    private static final String OFFHAND_PROTECT_MARKER = "\u2605[ 2 ]";
    private static final int PREPARE_DELAY_TICKS = 10;
    private static final int GUI_WAIT_TIMEOUT_TICKS = 60;
    private static final int MIN_CLICK_INTERVAL_TICKS = 4;
    private static final int HUD_MARKER_RECENT_TICKS = 80;

    private static State state = State.IDLE;
    private static int originalHotbarSlot = -1;
    private static int ticketHotbarSlot = -1;
    private static long stateStartTick;
    private static long lastCraftClickTick = -1L;
    private static boolean offhandSwapPerformed;
    private static boolean materialsMissingReceived;
    private static String latestHudMessage = "";
    private static long latestHudMessageTick = -1L;

    private HaAlchemyKilnAutomation() {
    }

    public static boolean isRunning() {
        return state != State.IDLE;
    }

    public static void start(MinecraftClient client, HaConfig config) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED
            || client == null
            || client.player == null
            || client.world == null
            || client.interactionManager == null
            || isRunning()) {
            return;
        }
        if (client.currentScreen != null) {
            sendClientMessage(client, "\u00a7cClose the current screen before starting Alchemy Kiln Assist.");
            return;
        }

        originalHotbarSlot = client.player.inventory.selectedSlot;
        ticketHotbarSlot = -1;
        offhandSwapPerformed = false;
        materialsMissingReceived = false;
        lastCraftClickTick = -1L;

        if (recentHudMessageHasOffhandProtectMarker(client)) {
            if (!swapWithOffhand(client)) {
                fail(client, "\u00a7cCould not move the marked item to offhand.");
                return;
            }
            offhandSwapPerformed = true;
        }

        advanceState(client, State.WAITING_BEFORE_TICKET);
        sendClientMessage(client, "\u00a7aAlchemy Kiln Assist started.");
    }

    public static void stop(MinecraftClient client, String reason) {
        if (!isRunning()) {
            return;
        }
        if (reason != null && !reason.isEmpty()) {
            sendClientMessage(client, reason);
        }
        clearState();
    }

    public static void onDisconnected() {
        clearState();
    }

    public static void onGameMessage(Text message) {
        if (!isRunning() || message == null) {
            return;
        }
        if (MATERIALS_MISSING_MESSAGE.equals(normalize(message.getString()))) {
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
            clearState();
            return;
        }
        if (!config.alchemyKilnAutomationEnabled) {
            stop(client, "\u00a7cAlchemy Kiln Assist was disabled in settings.");
            return;
        }

        switch (state) {
            case WAITING_BEFORE_TICKET:
                tickWaitingBeforeTicket(client);
                break;
            case USING_TICKET:
                tickUsingTicket(client);
                break;
            case WAITING_FOR_KILN_GUI:
                tickWaitingForKilnGui(client);
                break;
            case CRAFTING_STAGE_ONE:
                tickCrafting(client, config, STAGE_ONE_TARGET, State.CRAFTING_STAGE_TWO);
                break;
            case CRAFTING_STAGE_TWO:
                tickCrafting(client, config, STAGE_TWO_TARGET, State.FINISHING);
                break;
            case FINISHING:
                finish(client);
                break;
            case IDLE:
            default:
                break;
        }
    }

    private static void tickWaitingBeforeTicket(MinecraftClient client) {
        if (client.world.getTime() - stateStartTick < PREPARE_DELAY_TICKS) {
            return;
        }

        ticketHotbarSlot = findTicketSlot(client.player);
        if (ticketHotbarSlot < 0) {
            fail(client, "\u00a7cSuper Alchemy Kiln access ticket was not found in the hotbar.");
            return;
        }
        selectHotbarSlot(client, ticketHotbarSlot);
        advanceState(client, State.USING_TICKET);
    }

    private static void tickUsingTicket(MinecraftClient client) {
        if (!isTicketInMainHand(client.player)) {
            fail(client, "\u00a7cSuper Alchemy Kiln access ticket was no longer in the selected hotbar slot.");
            return;
        }
        ((MinecraftClientAccessor) client).ha$doItemUse();
        advanceState(client, State.WAITING_FOR_KILN_GUI);
    }

    private static void tickWaitingForKilnGui(MinecraftClient client) {
        if (client.currentScreen == null) {
            if (timedOut(client, GUI_WAIT_TIMEOUT_TICKS)) {
                fail(client, "\u00a7cTimed out waiting for the Super Alchemy Kiln GUI.");
            }
            return;
        }

        GenericContainerScreenHandler handler = getGenericContainerHandler(client);
        if (handler == null) {
            fail(client, "\u00a7cAn unexpected screen opened after using the access ticket.");
            return;
        }
        if (!normalize(client.currentScreen.getTitle().getString()).contains(KILN_SCREEN_TITLE)) {
            fail(client, "\u00a7cThe opened GUI was not the Super Alchemy Kiln GUI.");
            return;
        }
        if (findContainerSlotByExactName(handler, STAGE_ONE_TARGET) < 0) {
            fail(client, "\u00a7cCould not find: " + STAGE_ONE_TARGET);
            return;
        }

        materialsMissingReceived = false;
        lastCraftClickTick = -1L;
        advanceState(client, State.CRAFTING_STAGE_ONE);
    }

    private static void tickCrafting(MinecraftClient client, HaConfig config, String targetName, State nextState) {
        GenericContainerScreenHandler handler = getGenericContainerHandler(client);
        if (handler == null || !normalize(client.currentScreen.getTitle().getString()).contains(KILN_SCREEN_TITLE)) {
            fail(client, "\u00a7cThe Super Alchemy Kiln GUI was closed or changed unexpectedly.");
            return;
        }

        if (materialsMissingReceived) {
            materialsMissingReceived = false;
            lastCraftClickTick = -1L;
            if (nextState == State.CRAFTING_STAGE_TWO
                && findContainerSlotByExactName(handler, STAGE_TWO_TARGET) < 0) {
                fail(client, "\u00a7cCould not find: " + STAGE_TWO_TARGET);
                return;
            }
            advanceState(client, nextState);
            return;
        }

        long now = client.world.getTime();
        int clickInterval = Math.max(MIN_CLICK_INTERVAL_TICKS, config.alchemyKilnAutomationClickIntervalTicks);
        if (lastCraftClickTick >= 0L && now - lastCraftClickTick < clickInterval) {
            return;
        }

        int targetSlot = findContainerSlotByExactName(handler, targetName);
        if (targetSlot < 0) {
            fail(client, "\u00a7cCould not find: " + targetName);
            return;
        }
        Slot slot = handler.slots.get(targetSlot);
        client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
        lastCraftClickTick = now;
    }

    private static void finish(MinecraftClient client) {
        if (client.currentScreen != null) {
            client.player.closeHandledScreen();
        }
        if (offhandSwapPerformed && originalHotbarSlot >= 0 && originalHotbarSlot <= 8) {
            selectHotbarSlot(client, originalHotbarSlot);
            swapWithOffhand(client);
        }
        sendClientMessage(client, "\u00a7aAlchemy Kiln Assist finished.");
        clearState();
    }

    private static int findTicketSlot(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.inventory.getStack(slot);
            if (!stack.isEmpty() && TICKET_NAME.equals(normalize(stack.getName().getString()))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isTicketInMainHand(ClientPlayerEntity player) {
        return player != null
            && ticketHotbarSlot >= 0
            && ticketHotbarSlot <= 8
            && player.inventory.selectedSlot == ticketHotbarSlot
            && TICKET_NAME.equals(normalize(player.getMainHandStack().getName().getString()));
    }

    private static int findContainerSlotByExactName(GenericContainerScreenHandler handler, String expectedName) {
        int containerSlotCount = Math.min(handler.getRows() * 9, handler.slots.size());
        for (int slotIndex = 0; slotIndex < containerSlotCount; slotIndex++) {
            ItemStack stack = handler.slots.get(slotIndex).getStack();
            if (!stack.isEmpty() && expectedName.equals(normalize(stack.getName().getString()))) {
                return slotIndex;
            }
        }
        return -1;
    }

    private static GenericContainerScreenHandler getGenericContainerHandler(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen)
            || !(client.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
            return null;
        }
        return (GenericContainerScreenHandler) client.player.currentScreenHandler;
    }

    private static void selectHotbarSlot(MinecraftClient client, int slot) {
        client.player.inventory.selectedSlot = slot;
        client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private static boolean swapWithOffhand(MinecraftClient client) {
        if (client == null || client.player == null || client.player.networkHandler == null) {
            return false;
        }
        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
            BlockPos.ORIGIN,
            Direction.DOWN
        ));
        return true;
    }

    private static boolean recentHudMessageHasOffhandProtectMarker(MinecraftClient client) {
        return latestHudMessage.contains(OFFHAND_PROTECT_MARKER)
            && latestHudMessageTick >= 0L
            && client.world.getTime() - latestHudMessageTick <= HUD_MARKER_RECENT_TICKS;
    }

    private static void advanceState(MinecraftClient client, State nextState) {
        state = nextState;
        stateStartTick = client.world.getTime();
    }

    private static boolean timedOut(MinecraftClient client, int timeoutTicks) {
        return client.world.getTime() - stateStartTick > timeoutTicks;
    }

    private static void fail(MinecraftClient client, String reason) {
        sendClientMessage(client, reason);
        clearState();
    }

    private static void clearState() {
        state = State.IDLE;
        originalHotbarSlot = -1;
        ticketHotbarSlot = -1;
        stateStartTick = 0L;
        lastCraftClickTick = -1L;
        offhandSwapPerformed = false;
        materialsMissingReceived = false;
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
        WAITING_BEFORE_TICKET,
        USING_TICKET,
        WAITING_FOR_KILN_GUI,
        CRAFTING_STAGE_ONE,
        CRAFTING_STAGE_TWO,
        FINISHING
    }
}
