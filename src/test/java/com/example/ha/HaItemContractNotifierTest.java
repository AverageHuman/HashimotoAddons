package com.example.ha;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public final class HaItemContractNotifierTest {
    @Test
    public void closesForColoredAndPaddedFastTravelTitleAndUnprotectedTargetItem() {
        Assert.assertTrue(HaItemContractNotifier.shouldCloseForFastTravel(
            "\u00a7a ファストトラベル ",
            Collections.singletonList(Arrays.asList("\u00a76UNTOUCHABLE+", "装備アイテム"))
        ));
    }

    @Test
    public void doesNotCloseWhenTargetItemHasEitherLossProtectionLore() {
        for (String protectionLore : Arrays.asList("\u00a7b死亡時のロストを防ぎます。", "\u00a7aロストから保護されます。")) {
            Assert.assertFalse(HaItemContractNotifier.shouldCloseForFastTravel(
                "ファストトラベル",
                Collections.singletonList(Arrays.asList("UNIQUE", protectionLore))
            ));
        }
    }

    @Test
    public void doesNotCloseForOtherScreensOrNonTargetItems() {
        Assert.assertFalse(HaItemContractNotifier.shouldCloseForFastTravel(
            "\u00a7cエボリューションフォージ",
            Collections.singletonList(Collections.singletonList("UNIQUE"))
        ));
        Assert.assertFalse(HaItemContractNotifier.shouldCloseForFastTravel(
            "ファストトラベル",
            Collections.singletonList(Collections.singletonList("LEGENDARY"))
        ));
    }

    @Test
    public void closesWhenAnyTargetItemIsUnprotected() {
        Assert.assertTrue(HaItemContractNotifier.shouldCloseForFastTravel(
            "ファストトラベル",
            Arrays.asList(
                Arrays.asList("UNTOUCHABLE+", "死亡時のロストを防ぎます。"),
                Collections.singletonList("UNIQUE")
            )
        ));
    }

    @Test
    public void schedulesFourFollowUpWarningSoundsAtTwoTickIntervals() {
        HaItemContractNotifier.AlertSoundSchedule schedule = new HaItemContractNotifier.AlertSoundSchedule();
        schedule.start();

        for (int soundIndex = 0; soundIndex < 4; soundIndex++) {
            Assert.assertFalse(schedule.tickAndShouldPlay());
            Assert.assertTrue(schedule.tickAndShouldPlay());
        }
        Assert.assertFalse(schedule.tickAndShouldPlay());

        schedule.start();
        schedule.clear();
        Assert.assertFalse(schedule.tickAndShouldPlay());
    }
}
