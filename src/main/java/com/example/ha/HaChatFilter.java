package com.example.ha;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaChatFilter {
    private HaChatFilter() {
    }

    public static boolean shouldHide(Text message) {
        HaConfig config = HaConfig.get();
        config.normalize();
        if (!config.chatFilterEnabled || config.chatFilterEntries.isEmpty()) {
            return false;
        }

        String raw = message == null ? "" : message.getString();
        String stripped = Formatting.strip(raw);
        if (stripped == null) {
            stripped = raw;
        }

        for (HaConfig.ChatFilterEntry entry : config.chatFilterEntries) {
            if (!entry.enabled || entry.matchText.isEmpty()) {
                continue;
            }
            if (raw.contains(entry.matchText) || stripped.contains(entry.matchText)) {
                return true;
            }
        }
        return false;
    }
}
