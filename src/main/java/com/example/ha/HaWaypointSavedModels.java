package com.example.ha;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

final class SavedWaypointState {
    boolean editMode = false;
    boolean renderFullBlocks = false;
    int activeColorSlot = 0;
    int cycleKeyCode = GLFW.GLFW_KEY_UNKNOWN;
    int cycleKeyScanCode = -1;
    String cycleKeyType = "keysym";
    List<String> colorSlots = new ArrayList<String>();
    Map<String, List<SavedWaypointEntry>> waypointsByDimension = new LinkedHashMap<String, List<SavedWaypointEntry>>();
}

final class SavedWaypointEntry {
    int x;
    int y;
    int z;
    String label = "";
    int colorSlotIndex = 0;
}
