package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.model.space.SpaceCheck;
import java.util.HashMap;
import java.util.Map;

/**
 * Space capacity
 */
public class SpaceSizeControl {

    private static Map<String, SpaceCheck> spaceMap = null;

    /**
     * Initialize
     */
    public static void addSpace(String spaceId, long capacity, long actual) {
        if (null == spaceMap) {
            spaceMap = new HashMap<>();
        }
        // Statistical spatial validation information
        SpaceCheck spaceCheck = new SpaceCheck();
        spaceCheck.setActual(actual);
        spaceCheck.setCapacity(capacity);
        spaceMap.put(spaceId, spaceCheck);
    }

    /**
     * Clear
     */
    public static void clear() {
        if (null != spaceMap) {
            spaceMap.clear();
        }
    }

    /**
     * Obtain
     */
    public static SpaceCheck getSpaceCheck(String spaceId) {
        return spaceMap.get(spaceId);
    }

    /**
     * Update space capacity
     */
    public synchronized static void updateCapacity(String spaceId, long capacity) {
        spaceMap.get(spaceId).setCapacity(capacity);
    }

    /**
     * Update the actual storage space
     */
    public synchronized static void updateActual(String spaceId, long actual) {
        spaceMap.get(spaceId).updateActual(actual);
    }

    /**
     * Compare capacity with actual storage (storage is full)
     */
    public static boolean validation(String spaceId) {
        SpaceCheck spaceCheck = spaceMap.get(spaceId);
        if (spaceCheck.getCapacity() <= spaceCheck.getActual()) {
            return true;
        }
        return false;
    }

    /**
     * Compare the capacity with the size of the file to be stored and the actual storage
     */
    public static boolean validation(String spaceId, long data) {
        SpaceCheck spaceCheck = spaceMap.get(spaceId);
        if (spaceCheck.getCapacity() <= (spaceCheck.getActual() + data)) {
            return true;
        }
        return false;
    }
}
