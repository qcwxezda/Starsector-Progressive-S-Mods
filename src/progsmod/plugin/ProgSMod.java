package progsmod.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import progsmod.data.campaign.EngagementListener;
import util.SModUtils;

public class ProgSMod extends com.fs.starfarer.api.BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        SModUtils.loadConstants("progsmod_settings.json");
        // Disallow building-in hullmods via story points
        if (!SModUtils.Constants.DISABLE_MOD) {
            for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
                if (spec.hasTag("no_build_in")) {
                    spec.addTag("progsmod_no_build_in");
                }
                spec.addTag("no_build_in");
            }
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        if (!SModUtils.Constants.DISABLE_MOD) {
            SModUtils.loadData();
            Global.getSettings().getHullModSpec("progsmod_xptracker").setHiddenEverywhere(false);
            Global.getSector().getMemory().set("$progsmodEnabled", true);
            Global.getSector().addTransientListener(new EngagementListener(false));
        }
        else {
            Global.getSettings().getHullModSpec("progsmod_xptracker").setHiddenEverywhere(true);
            Global.getSector().getMemory().set("$progsmodEnabled", false);
            // Reallow building-in hullmods via story points
            for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
                if (!spec.hasTag("progsmod_no_build_in")) {
                    spec.getTags().remove("no_build_in");
                }
                spec.getTags().remove("progsmod_no_build_in");
            }
        }
    }
}
