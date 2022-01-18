package progsmod.data.campaign.rulecmd.ui.plugins;

import java.util.BitSet;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import progsmod.data.campaign.rulecmd.delegates.BuildInSModDelegate;
import progsmod.data.campaign.rulecmd.ui.Button;
import progsmod.data.campaign.rulecmd.ui.HullModButton;
import progsmod.data.campaign.rulecmd.ui.LabelWithVariables;
import progsmod.data.campaign.rulecmd.ui.PanelCreator.PanelCreatorData;
import progsmod.data.campaign.rulecmd.util.TempShipMaker;
import util.SModUtils;

public class BuildInSelector extends Selector<HullModButton> {

    // 1 variable: XP
    private LabelWithVariables<Integer> xpLabel;
    // 2 variables: # selected, max allowed
    private LabelWithVariables<Integer> countLabel;
    private Button showAllButton;
    private FleetMemberAPI fleetMember;
    private ShipVariantAPI checkerVariant;
    private ShipVariantAPI originalVariant;
    
    private BuildInSModDelegate delegate;
    private TooltipMakerAPI tooltipMaker;
    private CustomPanelAPI panel;

    public void init(
            BuildInSModDelegate delegate,
            PanelCreatorData<List<HullModButton>> data, 
            LabelWithVariables<Integer> xpLabel, 
            LabelWithVariables<Integer> countLabel,
            Button showAllButton, 
            FleetMemberAPI fleetMember,
            ShipVariantAPI variant) {
        super.init(data.created);
        this.delegate = delegate;
        this.panel = data.panel;
        this.tooltipMaker = data.tooltipMaker;
        this.xpLabel = xpLabel;
        this.countLabel = countLabel;
        this.showAllButton = showAllButton;
        this.originalVariant = variant;
        checkerVariant = variant.clone();
        this.fleetMember = fleetMember;
        updateItems();
    }

    public void addNewItems(List<HullModButton> items) {
        this.items.addAll(items);
        updateItems();
    }

    private void updateItems() {
        // Disable all of the unapplicable entries
        BitSet unapplicable = disableUnapplicable();
        int xp = xpLabel.getVar(0);
        int count = countLabel.getVar(0);
        int maxCount = countLabel.getVar(1);
        for (int i = 0; i < items.size(); i++) {
            HullModButton button = items.get(i);
            if (unapplicable.get(i)) {
                continue;
            }
            // Costs too much
            if (button.data.cost > xp) {
                disable(i, button.getDefaultDescription(), true);
            }
            // Already at limit
            else if (count >= maxCount) {
                disable(i, button.getDefaultDescription(), false);
            }
            // No reason to be disabled; enable it
            else {
                enable(i, button.getDefaultDescription());
            }
        }
    }

    /** Disable an item, but only if it isn't already selected. */
    private void disable(int index, String reason, boolean highlight) {
        HullModButton item = items.get(index);
        // Ignore already selected items
        if (item.isSelected()) {
            return;
        }
        item.disable(reason, highlight);
    }

    /** Enable an item. */
    private void enable(int index, String reason) {
        items.get(index).enable(reason);
    }

    /** Returns a set of indices of buttons that were disabled by this
     *  function. */
    private BitSet disableUnapplicable() {
        boolean checkedEntriesChanged = true;
        BitSet disabledIndices = new BitSet();
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        SectorEntityToken interactionTarget = dialog == null ? null : dialog.getInteractionTarget();
        while (checkedEntriesChanged) {
            ShipAPI checkerShip = TempShipMaker.makeShip(checkerVariant, fleetMember);
            // Since hull mods may have dependencies, some checked entries may
            // need to be unchecked.
            // Since dependencies can be chained, we need to do this in a loop.
            // (# of loops is bounded by # of checked entries as well as
            // longest hull mod dependency chain)
            checkedEntriesChanged = false;
            for (int i = 0; i < items.size(); i++) {
                HullModButton button = items.get(i);
                HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(button.data.id);
                boolean shouldDisable = false;
                String disableText = null;
                if (!hullMod.getEffect().isApplicableToShip(checkerShip)) {
                    shouldDisable = true;
                    disableText = SModUtils.shortenText(hullMod.getEffect().getUnapplicableReason(checkerShip), button.description);
                }
                if (interactionTarget != null && interactionTarget.getMarket() != null) {
                    CoreUITradeMode tradeMode = CoreUITradeMode.valueOf(interactionTarget.getMemory().getString("$tradeMode"));
                    if (!hullMod.getEffect().canBeAddedOrRemovedNow(checkerShip, interactionTarget.getMarket(), tradeMode)) {
                        shouldDisable = true;
                        disableText = SModUtils.shortenText(
                            hullMod.getEffect().getCanNotBeInstalledNowReason(checkerShip, interactionTarget.getMarket(), tradeMode),
                            button.description);
                    }
                }
                if (shouldDisable) {
                    if (button.isSelected()) {
                        forceDeselect(i);
                        checkedEntriesChanged = true;
                    }
                    if (disableText == null) {
                        disableText = "Can't build in (no reason given, default message)";
                    }
                    disable(i, disableText, true);
                    disabledIndices.set(i);
                }
            }
        }
        return disabledIndices;
    }

    @Override
    public void advance(float amount) {
        // Check if the show all button has been pressed
        if (showAllButton.isSelected()) {
            showAllButton.deselect();
            showAllButton.disable();
            delegate.showAllPressed(panel, tooltipMaker);
        }
    }

    @Override
    protected void onSelected(int index) {
        xpLabel.changeVar(0, xpLabel.getVar(0) - items.get(index).data.cost);
        countLabel.changeVar(0, countLabel.getVar(0) + 1);
        checkerVariant.addMod(items.get(index).data.id);
        updateItems();
    }

    @Override
    protected void onDeselected(int index) {
        xpLabel.changeVar(0, xpLabel.getVar(0) + items.get(index).data.cost);
        countLabel.changeVar(0, countLabel.getVar(0) - 1);
        // Don't remove mods that are already on the ship
        if (!originalVariant.hasHullMod(items.get(index).data.id))
            checkerVariant.removeMod(items.get(index).data.id);
        updateItems();
    }
    
    @Override
    protected void forceDeselect(int index) {
        super.forceDeselect(index);
        xpLabel.changeVar(0, xpLabel.getVar(0) + items.get(index).data.cost);
        countLabel.changeVar(0, countLabel.getVar(0) - 1);
        // Don't remove mods that are already on the ship
        if (!originalVariant.hasHullMod(items.get(index).data.id))
            checkerVariant.removeMod(items.get(index).data.id);
    }
}
