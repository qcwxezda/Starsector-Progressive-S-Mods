package plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import data.campaign.EngagementResultListener;
import util.SModUtils;
import util.SModUtils.ShipDataTable;

public class ProgressiveSModsPlugin extends com.fs.starfarer.api.BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        if (!Global.getSector().getPersistentData().containsKey(SModUtils.SHIP_DATA_KEY)) {
            resetShipData();
        }
        else {
            SModUtils.SHIP_DATA_TABLE = (ShipDataTable) Global.getSector().getPersistentData().get(SModUtils.SHIP_DATA_KEY);
        }

        Global.getSector().addTransientListener(new EngagementResultListener(false));

        // Disallow building-in hullmods via story points
        Misc.MAX_PERMA_MODS = -1;
    }

    public static void resetShipData() {
        SModUtils.SHIP_DATA_TABLE = new ShipDataTable();
        Global.getSector().getPersistentData().put(SModUtils.SHIP_DATA_KEY, SModUtils.SHIP_DATA_TABLE); 
    }
}
