package progsmod.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import progsmod.data.campaign.EngagementListener;
import progsmod.data.campaign.RefitTabListenerAndScript;
import util.SModUtils;

@SuppressWarnings("unused")
public class ProgSMod extends com.fs.starfarer.api.BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        SModUtils.loadConstants("progsmod_settings.json");
    }

    /** Disable only when necessary, i.e. in the refit screen, so that fleet inflaters that rely on s-mods being able
     *  to be built in "normally" can work properly. */
    public static void disableStoryPointBuildIn() {
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
            if (spec.hasTag("no_build_in")) {
                spec.addTag("progsmod_no_build_in");
            }
            spec.addTag("no_build_in");
        }
    }

    public static void enableStoryPointBuildIn() {
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
            if (!spec.hasTag("progsmod_no_build_in")) {
                spec.getTags().remove("no_build_in");
            }
            spec.getTags().remove("progsmod_no_build_in");
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        if (!SModUtils.Constants.DISABLE_MOD) {
            SModUtils.loadData();
            Global.getSettings().getHullModSpec("progsmod_xptracker").setHiddenEverywhere(false);
            Global.getSector().getMemory().set("$progsmodEnabled", true);

            ListenerManagerAPI listeners = Global.getSector().getListenerManager();
            if (!listeners.hasListenerOfClass(RefitTabListenerAndScript.class)) {
                listeners.addListener(new RefitTabListenerAndScript(), true);
            }

            Global.getSector().addTransientListener(new EngagementListener(false));
            Global.getSector().addTransientScript(new RefitTabListenerAndScript());
            Global.getSector().getMemory().set("$progsmodEnableLegacyUI", SModUtils.Constants.ENABLE_LEGACY_UI);
            Global.getSector().getMemory().set("$progsmodEnableNewUI", SModUtils.Constants.ENABLE_NEW_UI);

        }
        else {
            Global.getSettings().getHullModSpec("progsmod_xptracker").setHiddenEverywhere(true);
            Global.getSector().getMemory().set("$progsmodEnabled", false);
            // Reallow building-in hullmods via story points
            enableStoryPointBuildIn();
        }
    }
}
