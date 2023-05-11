package progsmod.data.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import progsmod.plugin.ProgSMod;

public class RefitTabListenerAndScript implements CoreUITabListener, EveryFrameScript {
    private static boolean insideRefitScreen = false;

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId id, Object param) {
        if (CoreUITabId.REFIT.equals(id) && !insideRefitScreen) {
            insideRefitScreen = true;
            ProgSMod.disableStoryPointBuildIn();
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float v) {
        if (!insideRefitScreen) return;
        if (Global.getSector() == null || Global.getSector().getCampaignUI() == null) return;

        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (!CoreUITabId.REFIT.equals(ui.getCurrentCoreTab())) {
            insideRefitScreen = false;
            ProgSMod.enableStoryPointBuildIn();
        }

        // Due to a bug, if the player ESCs out of the refit screen in a market, the core tab is still shown as REFIT
        // even though it's been closed. To combat this, check if the savedOptionList is empty. If it is, we're still
        // in the refit screen; otherwise, we've ESCed out of the refit screen.
        else if (ui.getCurrentInteractionDialog() != null
                && ui.getCurrentInteractionDialog().getOptionPanel() != null
                && !ui.getCurrentInteractionDialog().getOptionPanel().getSavedOptionList().isEmpty()) {
            insideRefitScreen = false;
            ProgSMod.enableStoryPointBuildIn();
        }
    }
}
