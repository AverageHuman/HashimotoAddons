package com.example.ha;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public final class HaClientMod implements ClientModInitializer {
    private static KeyBinding macroToggleKeyBinding;
    private static KeyBinding alchemyKilnAutomationKeyBinding;
    private static KeyBinding cameraToggleKeyBinding;
    private static KeyBinding chestSearchKeyBinding;
    private static KeyBinding gearViewKeyBinding;
    private HaTickHandler tickHandler;

    @Override
    public void onInitializeClient() {
        HaConfig.get().load();
        KeyBinding macroBinding = HaBuildFlags.DANGEROUS_FEATURES_ENABLED ? getOrCreateMacroToggleKeyBinding() : null;
        KeyBinding alchemyBinding = HaBuildFlags.DANGEROUS_FEATURES_ENABLED ? getOrCreateAlchemyKilnAutomationKeyBinding() : null;
        tickHandler = new HaTickHandler(macroBinding, alchemyBinding, getOrCreateCameraToggleKeyBinding(), getOrCreateChestSearchKeyBinding(), getOrCreateGearViewKeyBinding());
        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            updateMacroToggleBinding(HaConfig.get().getMacroToggleKey());
            updateAlchemyKilnAutomationBinding(HaConfig.get().getAlchemyKilnAutomationKey());
        }
        updateCameraToggleBinding(HaConfig.get().getCameraToggleKey());
        updateChestSearchBinding(HaConfig.get().getChestSearchKey());
        updateGearViewBinding(HaConfig.get().getGearViewKey());
        registerCommand();
        ClientTickEvents.END_CLIENT_TICK.register(tickHandler::onEndClientTick);
        HudRenderCallback.EVENT.register(HaMacroStatusOverlay::render);
        HudRenderCallback.EVENT.register(HaExtrasOverlay::render);
        HudRenderCallback.EVENT.register(HaChunkChestOverlay::render);
        HudRenderCallback.EVENT.register(HaDropTrackerOverlay::render);
        HudRenderCallback.EVENT.register(HaExpTrackerOverlay::render);
        HudRenderCallback.EVENT.register(HaMobHpDisplayOverlay::render);
        HudRenderCallback.EVENT.register(HaSubSkillTimerOverlay::render);
        HudRenderCallback.EVENT.register(HaMobEspTracerOverlay::render);
        HudRenderCallback.EVENT.register(HaAfkFarmingDebugOverlay::render);
        HudRenderCallback.EVENT.register(HaAfkFarmingAutoMoveOverlay::render);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(HaChestSearchOverlay::render);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(HaMobEspOverlay::render);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(HaAfkFarmingCircleOverlay::render);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommandManager.literal("ha")
            .executes(context -> {
                tickHandler.requestOpenConfigScreen();
                return 1;
            })
            .then(ClientCommandManager.literal("edithud")
                .executes(context -> openHudEditor()))
            .then(ClientCommandManager.literal("expdebug")
                .then(ClientCommandManager.literal("clear")
                    .executes(context -> clearExpDebugLog()))
                .then(ClientCommandManager.literal("mark")
                    .then(ClientCommandManager.argument("label", StringArgumentType.greedyString())
                        .executes(context -> markExpDebugLog(StringArgumentType.getString(context, "label")))))
                .then(ClientCommandManager.literal("copy")
                    .executes(context -> copyExpDebugLog())))
            .then(ClientCommandManager.literal("tracker")
                .then(ClientCommandManager.literal("add")
                    .executes(context -> registerHeldTrackerItem(0L))
                    .then(ClientCommandManager.argument("price", LongArgumentType.longArg(0L))
                        .executes(context -> registerHeldTrackerItem(LongArgumentType.getLong(context, "price"))))));

        if (HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            command.then(ClientCommandManager.literal("extras")
                .executes(context -> toggleExtras()));
            command.then(ClientCommandManager.literal("em")
                .executes(context -> toggleEditMode()));
            command.then(ClientCommandManager.literal("bg")
                .executes(context -> openBlockGallery()));
        }

        ClientCommandManager.DISPATCHER.register(command);
    }

    private int toggleExtras() {
        HaConfig config = HaConfig.get();
        HaGhostWall.setExtrasEnabled(!config.extrasEnabled);
        sendMessage("Extras Visibility " + (HaConfig.get().extrasEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        return 1;
    }

    private int toggleEditMode() {
        HaConfig config = HaConfig.get();
        config.ghostWallEditMode = !config.ghostWallEditMode;
        config.save();
        sendMessage("Edit Mode " + (config.ghostWallEditMode ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        return 1;
    }

    private int openBlockGallery() {
        tickHandler.requestOpenBlockGalleryScreen();
        return 1;
    }

    private int openHudEditor() {
        tickHandler.requestOpenHudEditScreen();
        return 1;
    }

    private int copyExpDebugLog() {
        if (HaExpTracker.copyDebugLogToClipboard()) {
            sendMessage("Copied Exp Tracker debug log to clipboard.");
            return 1;
        }
        sendMessage("\u00a7cCould not copy Exp Tracker debug log.");
        return 0;
    }

    private int clearExpDebugLog() {
        HaExpTracker.clearDebugLog();
        sendMessage("Cleared Exp Tracker debug log and inserted a reset marker.");
        return 1;
    }

    private int markExpDebugLog(String label) {
        HaExpTracker.addDebugMarker(label);
        sendMessage("Added Exp Tracker debug marker: " + (label == null || label.trim().isEmpty() ? "(no label)" : label.trim()));
        return 1;
    }

    private int registerHeldTrackerItem(long price) {
        ItemStack stack = net.minecraft.client.MinecraftClient.getInstance().player == null
            ? ItemStack.EMPTY
            : net.minecraft.client.MinecraftClient.getInstance().player.getMainHandStack();
        if (stack.isEmpty()) {
            sendMessage("\u00a7cHold an item in your main hand before using /ha tracker add.");
            return 0;
        }

        HaDropTracker.RegisteredItem item = HaDropTracker.registerHeldItem(price);
        if (item == null) {
            sendMessage("\u00a7cCould not register the held item.");
            return 0;
        }

        sendMessage("Registered " + item.displayName + " for " + item.unitPrice + " Intercoins.");
        return 1;
    }

    private void sendMessage(String message) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(new LiteralText("[\u00a7l\u00a7bHashimotoAddons\u00a7r]:" + message), false);
        }
    }

    public static void updateMacroToggleBinding(InputUtil.Key key) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }
        getOrCreateMacroToggleKeyBinding().setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }

    public static void updateAlchemyKilnAutomationBinding(InputUtil.Key key) {
        if (!HaBuildFlags.DANGEROUS_FEATURES_ENABLED) {
            return;
        }
        getOrCreateAlchemyKilnAutomationKeyBinding().setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }

    public static void updateCameraToggleBinding(InputUtil.Key key) {
        getOrCreateCameraToggleKeyBinding().setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }

    public static void updateChestSearchBinding(InputUtil.Key key) {
        getOrCreateChestSearchKeyBinding().setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }

    public static void updateGearViewBinding(InputUtil.Key key) {
        getOrCreateGearViewKeyBinding().setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }

    private static KeyBinding getOrCreateMacroToggleKeyBinding() {
        if (macroToggleKeyBinding == null) {
            macroToggleKeyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                    "key.hashimotoaddons.toggle_macro",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    "category.hashimotoaddons"
                )
            );
        }
        return macroToggleKeyBinding;
    }

    private static KeyBinding getOrCreateAlchemyKilnAutomationKeyBinding() {
        if (alchemyKilnAutomationKeyBinding == null) {
            alchemyKilnAutomationKeyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                    "key.hashimotoaddons.alchemy_kiln_automation",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "category.hashimotoaddons"
                )
            );
        }
        return alchemyKilnAutomationKeyBinding;
    }

    private static KeyBinding getOrCreateCameraToggleKeyBinding() {
        if (cameraToggleKeyBinding == null) {
            cameraToggleKeyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                    "key.hashimotoaddons.toggle_camera",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "category.hashimotoaddons"
                )
            );
        }
        return cameraToggleKeyBinding;
    }

    private static KeyBinding getOrCreateChestSearchKeyBinding() {
        if (chestSearchKeyBinding == null) {
            chestSearchKeyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                    "key.hashimotoaddons.open_chest_search",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "category.hashimotoaddons"
                )
            );
        }
        return chestSearchKeyBinding;
    }

    private static KeyBinding getOrCreateGearViewKeyBinding() {
        if (gearViewKeyBinding == null) {
            gearViewKeyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                    "key.hashimotoaddons.gear_view",
                    InputUtil.Type.MOUSE,
                    GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                    "category.hashimotoaddons"
                )
            );
        }
        return gearViewKeyBinding;
    }
}
