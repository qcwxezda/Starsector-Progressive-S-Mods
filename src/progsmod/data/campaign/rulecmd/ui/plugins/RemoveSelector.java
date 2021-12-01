package progsmod.data.campaign.rulecmd.ui.plugins;

import java.util.List;

import progsmod.data.campaign.rulecmd.ui.HullModButton;
import progsmod.data.campaign.rulecmd.ui.LabelWithVariables;

public class RemoveSelector extends Selector<HullModButton> {

    // 1 variable: XP
    private LabelWithVariables<Integer> xpLabel;

    public void disableItem(int index, String reason, boolean highlight) {
        items.get(index).disable(reason, highlight);
    }

    public void init(List<HullModButton> items, LabelWithVariables<Integer> label) {
        super.init(items);
        xpLabel = label;
    }

    @Override
    protected void onSelected(int index) {
        xpLabel.changeVar(0, xpLabel.getVar(0) + items.get(index).data.cost);
    }

    @Override
    protected void onDeselected(int index) {
        xpLabel.changeVar(0, xpLabel.getVar(0) - items.get(index).data.cost);
    }
}
