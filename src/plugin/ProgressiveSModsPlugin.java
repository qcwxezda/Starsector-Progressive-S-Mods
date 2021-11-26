package plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import data.campaign.EngagementResultListener;
import util.ProgSModUtils;

public class ProgressiveSModsPlugin extends com.fs.starfarer.api.BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {

        ProgSModUtils.loadConstants("mod_settings.json");

        if (!ProgSModUtils.Constants.DISABLE_MOD) {
            ProgSModUtils.loadShipData();
            Global.getSettings().getHullModSpec("progsmod_xptracker").setHiddenEverywhere(false);
            Global.getSector().getMemory().set("$progsmodEnabled", true);
            Global.getSector().addTransientListener(new EngagementResultListener(false));
            
            // Disallow building-in hullmods via story points
            Misc.MAX_PERMA_MODS = -1;
        }
        else {
            Global.getSettings().getHullModSpec("progsmod_xptracker").setHiddenEverywhere(true);
            Global.getSector().getMemory().set("$progsmodEnabled", false);

            Misc.MAX_PERMA_MODS = Global.getSettings().getInt("maxPermanentHullmods");
        }
    }
}
