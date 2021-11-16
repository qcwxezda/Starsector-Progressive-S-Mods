package plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import campaign.EngagementResultListener;
import util.SModUtils;
import util.SModUtils.ShipDataTable;

public class RevampedSModsPlugin extends com.fs.starfarer.api.BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        if (!Global.getSector().getPersistentData().containsKey(SModUtils.SHIPDATA_KEY)) {
            resetShipData();
        }

        Global.getSector().addTransientListener(new EngagementResultListener(false));

        // Disallow building-in hullmods via story points
        Misc.MAX_PERMA_MODS = -1;
    }

    public static void resetShipData() {
        Global.getSector().getPersistentData().put(SModUtils.SHIPDATA_KEY, new ShipDataTable()); 
    }
}
