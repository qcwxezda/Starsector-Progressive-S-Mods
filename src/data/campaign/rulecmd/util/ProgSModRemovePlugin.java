package data.campaign.rulecmd.util;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import data.campaign.rulecmd.util.ProgSModSelectPanelCreator.SelectorData;
import util.SModUtils;
public class ProgSModRemovePlugin implements CustomUIPanelPlugin {

    private LabelAPI xpLabel;
    private List<SelectorData> selectorList;
    private float shipXP;

    public void setData(LabelAPI xpLabel, List<SelectorData> list, float shipXP) {
        this.xpLabel = xpLabel;
        selectorList = list;
        this.shipXP = shipXP;
    }

    /** Returns the sum of checked buttons' XP costs. 
     * Populates [checkedEntries] with the checked buttons.*/
    public int tallyCheckedEntries(List<SelectorData> selectorList, List<SelectorData> checkedEntries) {
        if (!checkedEntries.isEmpty()) {
            checkedEntries.clear();
        }
        int sumChecked = 0;
        for (SelectorData entry : selectorList) {
            if (entry.button.isChecked()) {
                checkedEntries.add(entry);
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
                int sum = tallyCheckedEntries(selectorList, new ArrayList<SelectorData>());
                float xp = shipXP + sum * SModUtils.Constants.XP_REFUND_FACTOR;

                // Update the xp remaining and number selected labels
                ProgSModSelectPanelCreator.setRemainingXPText(xpLabel, xp);
            }
        }
    }

    @Override
    public void render(float alphaMult) {
    }

    @Override
    public void renderBelow(float alphaMult) {}
}
