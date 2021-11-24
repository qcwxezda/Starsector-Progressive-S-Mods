package data.campaign.rulecmd.util;

import java.util.Iterator;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import data.campaign.rulecmd.util.ProgSModSelectPanelCreator.SelectorData;
import util.SModUtils;
public class ProgSModRemovePlugin implements CustomUIPanelPlugin {

    private LabelAPI xpLabel;
    private List<SelectorData> selectorList;
    private float shipXP;
    private ProgSModSelectPanelCreator panelCreator;
    private MarketAPI market;

    public void setData(LabelAPI xpLabel, List<SelectorData> list, float shipXP, ProgSModSelectPanelCreator panelCreator, MarketAPI market) {
        this.xpLabel = xpLabel;
        selectorList = list;
        this.shipXP = shipXP;
        this.panelCreator = panelCreator;
        this.market = market;

        removeIfStationRequired();
    }

    /** Remove the hull mods that require a station or spaceport,
     *  if fleet is not at either. */
    private void removeIfStationRequired() {
        Iterator<SelectorData> itr = selectorList.iterator();
        while (itr.hasNext()) {
            SelectorData entry = itr.next();
            HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(entry.hullModId);
            if (!SModUtils.canModifyHullMod(hullMod, market)) {
                panelCreator.disableRedAndChangeText(entry, "Requires a non-hostile spaceport or orbital station");
                itr.remove();
            }
        }
    }

    /** Returns the sum of checked buttons' XP costs.*/
    public int tallyCheckedEntries(List<SelectorData> selectorList) {
        int sumChecked = 0;
        for (SelectorData entry : selectorList) {
            if (entry.button.isChecked()) {
                sumChecked += entry.hullModCost;
            }
        }
        return sumChecked;
    }

    @Override
    public void positionChanged(PositionAPI position) {}
    
    @Override
    public void advance(float amount) {}

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (selectorList == null) {
            return;
        }

        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }

            // Ideally this would be a mouse down event, or even better,
            // a callback from a button being clicked, but the first
            // get consumed by the button click and the second doesn't seem to
            // exist.
            if (event.isMouseMoveEvent()) {
                int sum = tallyCheckedEntries(selectorList);
                float xp = shipXP + sum * SModUtils.Constants.XP_REFUND_FACTOR;

                // Update the xp remaining and number selected labels
                panelCreator.setRemainingXPText(xpLabel, xp);
            }
        }
    }

    @Override
    public void render(float alphaMult) {
    }

    @Override
    public void renderBelow(float alphaMult) {}
}
