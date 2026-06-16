package com.example.ha;

import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

public final class HaItemProtect {
    private static final String MESSAGE_PREFIX = "[\u00a7l\u00a7bHashimotoAddons\u00a7r]: ";
    private static final String PROTECT_TAG_KEY = "ha_item_protect";
    private static final String PROTECT_ID_KEY = "id";
    private static final String PROTECTION_KEY_SEPARATOR = "\u001f";
    private static final String MAGICAL_BOMB_NAME_JA = "繝槭ず繧ｫ繝ｫ繝懊Β";
    private static final String MAGICAL_BOMB_NAME_EN = "magical bomb";

    private HaItemProtect() {
    }

    public static boolean toggleHeldItemProtection() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        ItemStack stack = client.player.getMainHandStack();
        if (stack == null || stack.isEmpty()) {
            sendMessage(client, "\u00a7cPlease hold the item you want to protect in your main hand.");
            return false;
        }

        String protectionKey = createProtectionKey(stack);
        if (protectionKey == null) {
            sendMessage(client, "\u00a7cThis item could not be assigned a protection key.");
            return false;
        }

        HaConfig config = HaConfig.get();
        boolean enabledNow;
        String legacyProtectId = getProtectId(stack);
        boolean alreadyProtected = legacyProtectId != null
            ? config.protectedItemIds.contains(legacyProtectId)
            : config.protectedItemIds.contains(protectionKey);
        if (alreadyProtected) {
            if (legacyProtectId != null) {
                config.protectedItemIds.remove(legacyProtectId);
            } else {
                config.protectedItemIds.remove(protectionKey);
            }
            enabledNow = false;
        } else {
            if (legacyProtectId != null) {
                config.protectedItemIds.add(legacyProtectId);
            } else {
                config.protectedItemIds.add(protectionKey);
            }
            enabledNow = true;
        }

        config.save();
        playFeedback(client, enabledNow);
        sendMessage(client, enabledNow ? "\u00a7aHashimotoAddons protected this item!" : "\u00a7eHashimotoAddons protection has been removed.");
        return true;
    }

    public static void notifyBlockedProtection() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        playFeedback(client, false);
        sendMessage(client, "\u00a7cThis item is protected by HashimotoAddons.");
    }

    public static boolean isProtected(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        HaConfig config = HaConfig.get();
        String protectId = getProtectId(stack);
        if (protectId != null && config.protectedItemIds.contains(protectId)) {
            return true;
        }

        String protectionKey = createProtectionKey(stack);
        return protectionKey != null && config.protectedItemIds.contains(protectionKey);
    }

    public static boolean isMagicalBombCursor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() == Items.TNT) {
            return true;
        }

        String name = normalizeName(stack.getName().getString());
        if (name.isEmpty()) {
            return false;
        }

        return name.contains(MAGICAL_BOMB_NAME_JA.toLowerCase(Locale.ROOT))
            || name.contains(MAGICAL_BOMB_NAME_EN)
            || name.contains("tnt");
    }

    public static boolean isMagicalBombCursor(MinecraftClient client) {
        if (client == null || client.player == null || client.player.inventory == null) {
            return false;
        }

        return isMagicalBombCursor(((com.example.ha.mixin.PlayerInventoryAccessor) client.player.inventory).ha$getCursorStack());
    }

    public static String getProtectId(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return null;
        }

        NbtCompound tag = stack.getTag();
        if (tag == null || !tag.contains(PROTECT_TAG_KEY, 10)) {
            return null;
        }

        NbtCompound protectTag = tag.getCompound(PROTECT_TAG_KEY);
        if (protectTag == null || !protectTag.contains(PROTECT_ID_KEY, 8)) {
            return null;
        }

        String value = protectTag.getString(PROTECT_ID_KEY);
        return normalizeProtectId(value);
    }

    public static String ensureProtectId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        String existing = getProtectId(stack);
        if (existing != null) {
            return existing;
        }

        NbtCompound tag = stack.getOrCreateTag();
        NbtCompound protectTag = tag.contains(PROTECT_TAG_KEY, 10) ? tag.getCompound(PROTECT_TAG_KEY) : new NbtCompound();
        String protectId = UUID.randomUUID().toString();
        protectTag.putString(PROTECT_ID_KEY, protectId);
        tag.put(PROTECT_TAG_KEY, protectTag);
        stack.setTag(tag);
        return protectId;
    }

    public static String createProtectionKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        net.minecraft.util.Identifier itemId = net.minecraft.util.registry.Registry.ITEM.getId(stack.getItem());
        String baseId = itemId == null ? stack.getItem().toString() : itemId.toString();
        String tagKey = createTagKey(stack.getTag());
        return baseId + PROTECTION_KEY_SEPARATOR + stack.getDamage() + PROTECTION_KEY_SEPARATOR + tagKey;
    }

    private static void playFeedback(MinecraftClient client, boolean enabledNow) {
        if (client.player == null) {
            return;
        }

        float pitch = enabledNow ? 1.25F : 0.85F;
        client.player.playSound(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.7F, pitch);
    }

    private static void sendMessage(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(new LiteralText(MESSAGE_PREFIX + message), false);
        }
    }

    private static String normalizeName(String value) {
        String stripped = Formatting.strip(value == null ? "" : value);
        return (stripped == null ? "" : stripped).trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeProtectId(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String createTagKey(NbtCompound tag) {
        if (tag == null) {
            return "";
        }

        NbtCompound copied = tag.copy();
        copied.remove(PROTECT_TAG_KEY);
        return copied.isEmpty() ? "" : copied.toString();
    }
}
