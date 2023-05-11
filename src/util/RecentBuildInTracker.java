package util;

import com.fs.starfarer.api.Global;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RecentBuildInTracker {
    public static final int recentlyBuiltInMaxSize = 10;
    public static final String DATA_KEY = "progsmod_RecentlyBuiltIn";

    public static void addToRecentlyBuiltIn(String id) {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        if (!persistentData.containsKey(DATA_KEY)) {
            persistentData.put(DATA_KEY, new LinkedList<>());
        }

        //noinspection unchecked
        LinkedList<String> recentlyBuiltIn = (LinkedList<String>) persistentData.get(DATA_KEY);

        recentlyBuiltIn.remove(id);
        recentlyBuiltIn.addFirst(id);

        if (recentlyBuiltIn.size() > recentlyBuiltInMaxSize) {
            recentlyBuiltIn.removeLast();
        }
    }

    public static List<String> getRecentlyBuiltIn() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        if (!persistentData.containsKey(DATA_KEY)) {
            persistentData.put(DATA_KEY, new LinkedList<>());
        }

        //noinspection unchecked
        return (List<String>) persistentData.get(DATA_KEY);
    }
}
