package plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import data.campaign.EngagementResultListener;
import util.SModUtils;

public class ProgressiveSModsPlugin extends com.fs.starfarer.api.BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().addTransientListener(new EngagementResultListener(false));

        SModUtils.loadShipData();
        SModUtils.loadConstants("mod_settings.json");

        // Disallow building-in hullmods via story points
        Misc.MAX_PERMA_MODS = -1;
    }
}
