package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import net.minecraft.text.LiteralText;

public final class HaMobSpawnCoordinateConfigJsonMapperTest {
    @Test
    public void persistsBothWaypointRenderChoices() {
        HaMobSpawnCoordinateConfig config = new HaMobSpawnCoordinateConfig();
        config.enabled = true;
        config.targetName = "  Boss Name  ";
        config.colorSlotIndex = 3;
        config.renderFullBlocks = true;

        SavedMobSpawnCoordinateConfig saved = HaMobSpawnCoordinateConfigJsonMapper.toSaved(config);

        Assert.assertTrue(saved.enabled);
        Assert.assertEquals("Boss Name", saved.targetName);
        Assert.assertEquals(3, saved.colorSlotIndex);
        Assert.assertTrue(saved.renderFullBlocks);
    }

    @Test
    public void normalizesMissingNameAndOutOfRangeColorSlot() {
        SavedMobSpawnCoordinateConfig saved = new SavedMobSpawnCoordinateConfig();
        saved.targetName = null;
        saved.colorSlotIndex = 99;
        HaMobSpawnCoordinateConfig config = new HaMobSpawnCoordinateConfig();

        HaMobSpawnCoordinateConfigJsonMapper.apply(saved, config);

        Assert.assertEquals("", config.targetName);
        Assert.assertEquals(3, config.colorSlotIndex);
        Assert.assertFalse(config.isActive());
    }

    @Test
    public void matchesOnlyTheExactNameTagAfterFormattingIsRemoved() {
        Assert.assertTrue(HaMobSpawnCoordinate.matchesTargetName("Boss Name", new LiteralText("\u00a7aBoss Name")));
        Assert.assertFalse(HaMobSpawnCoordinate.matchesTargetName("Boss", new LiteralText("Boss Name")));
        Assert.assertFalse(HaMobSpawnCoordinate.matchesTargetName("Boss Name", null));
    }

    @Test
    public void keepsOnlyTheFirstWaypointInTheSameVerticalColumn() {
        List<HaWaypointManager.WaypointEntry> waypoints = new ArrayList<HaWaypointManager.WaypointEntry>();
        HaWaypointManager.WaypointEntry spawnWaypoint = new HaWaypointManager.WaypointEntry("minecraft:overworld", 10, 80, 20, "Boss", 0);
        HaWaypointManager.WaypointEntry fallenWaypoint = new HaWaypointManager.WaypointEntry("minecraft:overworld", 10, 79, 20, "Boss", 0);

        Assert.assertTrue(HaMobSpawnCoordinate.addTemporaryWaypoint(waypoints, spawnWaypoint));
        Assert.assertFalse(HaMobSpawnCoordinate.addTemporaryWaypoint(waypoints, fallenWaypoint));
        Assert.assertEquals(1, waypoints.size());
        Assert.assertEquals(80, waypoints.get(0).y);
    }

    @Test
    public void startsFiveSecondPauseWhenPlayerIsTenBlocksAway() {
        Assert.assertTrue(HaMobSpawnCoordinate.isAtLeastPauseDistanceAway(10.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
        Assert.assertFalse(HaMobSpawnCoordinate.isAtLeastPauseDistanceAway(9.99D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
        Assert.assertTrue(HaMobSpawnCoordinate.isGenerationPaused(1000L, 6000L));
        Assert.assertFalse(HaMobSpawnCoordinate.isGenerationPaused(6000L, 6000L));
    }
}
