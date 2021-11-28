package plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import data.campaign.ProgSModEngagementListener;
import data.campaign.ProgSModEngagementListenerOld;
import util.SModUtils;

public class ProgSModBasePlugin extends com.fs.starfarer.api.BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {

        SModUtils.loadConstants("mod_settings.json");

        if (!SModUtils.Constants.DISABLE_MOD) {
            SModUtils.loadShipData();
            Global.getSettings().getHullModSpec("progsmod_xptracker").setHiddenEverywhere(false);
            Global.getSector().getMemory().set("$progsmodEnabled", true);

            if (SModUtils.Constants.USE_LEGACY_XP_TRACKER) {
                Global.getSector().addTransientListener(new ProgSModEngagementListenerOld(false));
            }
            else {
                Global.getSector().addTransientListener(new ProgSModEngagementListener(false));
            }
            
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
