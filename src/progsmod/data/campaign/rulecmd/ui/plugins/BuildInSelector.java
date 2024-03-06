package progsmod.data.campaign.rulecmd.ui.plugins;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.CampaignUIAPI.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.*;
import com.fs.starfarer.api.loading.*;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.*;
import progsmod.data.campaign.rulecmd.PSM_BuildInHullModNew.*;
import progsmod.data.campaign.rulecmd.delegates.*;
import progsmod.data.campaign.rulecmd.ui.*;
import progsmod.data.campaign.rulecmd.ui.PanelCreator.*;
import progsmod.data.campaign.rulecmd.util.*;
import util.*;

import java.util.*;

public class BuildInSelector extends Selector<HullModButton> {

    // 1 variable: XP
    private LabelWithVariables<Integer> xpLabel;
    // 2 variables: # selected, max allowed
    private LabelWithVariables<Integer> countLabel;
    private int firstIndexToBeCounted;
    private Button showRecentButton;
    private FleetMemberAPI fleetMember;
    private ShipVariantAPI checkerVariant;
    private ShipVariantAPI originalVariant;
    
    private ManageSMods delegate;
    private TooltipMakerAPI tooltipMaker;
    private CustomPanelAPI panel;

    private SelectorContainer container;

    public void init(
            ManageSMods delegate,
            PanelCreatorData<List<HullModButton>> data, 
            LabelWithVariables<Integer> xpLabel, 
            LabelWithVariables<Integer> countLabel,
            Button showRecentButton,
            FleetMemberAPI fleetMember,
            ShipVariantAPI variant,
            int firstIndexToBeCounted,
            SelectorContainer container) {
        super.init(data.created);
        this.delegate = delegate;
        this.panel = data.panel;
        this.tooltipMaker = data.tooltipMaker;
        this.xpLabel = xpLabel;
        this.countLabel = countLabel;
        this.showRecentButton = showRecentButton;
        this.originalVariant = variant;
        checkerVariant = variant.clone();
        this.fleetMember = fleetMember;
        this.firstIndexToBeCounted = firstIndexToBeCounted;
        this.container = container;
        container.register(this);
        updateItems();
    }

    public void addNewItems(List<HullModButton> items) {
        this.items.addAll(items);
        updateItems();
    }

    public void updateItems() {
        container.updateAll();
    }
    @Override
    public void update() {
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
            else if (count >= maxCount && i >= firstIndexToBeCounted) {
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
            for (int i = firstIndexToBeCounted; i < items.size(); i++) {
                HullModButton button = items.get(i);
                HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(button.data.id);
                boolean shouldDisable = false;
                String disableText = null;
                if (!hullMod.getEffect().isApplicableToShip(checkerShip)) {
                    String reason = hullMod.getEffect().getUnapplicableReason(checkerShip);
                    // Can build in any number of logistics hull mods
                    // Don't use s-mods in the check ship as we want to be able to tell when incompatibilities arise
                    // via forcible removing of non s-mods
                    if (reason != null && !reason.startsWith("Maximum of 2 non-built-in \"Logistics\"")) {
                        disableText = SModUtils.shortenText(reason, button.description);
                        shouldDisable = true;
                    }

                }
                else if (interactionTarget != null && interactionTarget.getMarket() != null) {
                    CoreUITradeMode tradeMode = CoreUITradeMode.valueOf(interactionTarget.getMemory().getString("$tradeMode"));
                    if (!hullMod.getEffect().canBeAddedOrRemovedNow(checkerShip, interactionTarget.getMarket(), tradeMode)) {
                        String reason = hullMod.getEffect().getCanNotBeInstalledNowReason(checkerShip, interactionTarget.getMarket(), tradeMode);
                        shouldDisable = true;
                        disableText = SModUtils.shortenText(
                                reason,
                                button.description);
                    }
                }
                if (hullMod.hasTag("no_build_in") && !SModUtils.Constants.IGNORE_NO_BUILD_IN) {
                    shouldDisable = true;
                    disableText = hullMod.getDisplayName() + " can't be built in";
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
        if (showRecentButton != null && showRecentButton.isSelected()) {
            showRecentButton.deselect();
            showRecentButton.disable();
            delegate.showRecentPressed(panel, tooltipMaker);
        }
    }

    @Override
    protected void onSelected(int index) {
        xpLabel.changeVar(0, xpLabel.getVar(0) - items.get(index).data.cost);
        if (index >= firstIndexToBeCounted) {
            countLabel.changeVar(0, countLabel.getVar(0) + 1);
        }
        String hullModId = items.get(index).data.id;
        checkerVariant.addMod(hullModId);
        if (testForDesync()) {
            forceDeselect(index);
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage("Can't build in due to custom hull mod incompatibility", Misc.getNegativeHighlightColor());
        }
        else {
            updateItems();
        }
    }

    /** Tests if a ship made from checkerVariant still has all the hull mods that checkerVariant does.
     *  If not, this likely means that a mod forcibly removed some other hull mod(s) due to mod incompatibilities,
     *  so adding this hull mod would not be safe. */
    private boolean testForDesync() {
        ShipVariantAPI cloneVariant = checkerVariant.clone();
        TempShipMaker.makeShip(cloneVariant, fleetMember);
        if (cloneVariant.hasHullMod("ML_incompatibleHullmodWarning")) {
            return true;
        }
        for (String id : checkerVariant.getHullMods()) {
            if (!cloneVariant.hasHullMod(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDeselected(int index) {
        xpLabel.changeVar(0, xpLabel.getVar(0) + items.get(index).data.cost);
        if (index >= firstIndexToBeCounted) {
            countLabel.changeVar(0, countLabel.getVar(0) - 1);
        }
        String hullModId = items.get(index).data.id;
        // Don't remove hull mods that were already on the ship
        if (!originalVariant.hasHullMod(hullModId)) {
            checkerVariant.removeMod(hullModId);
        }
        updateItems();
    }
    
    @Override
    protected void forceDeselect(int index) {
        super.forceDeselect(index);
        xpLabel.changeVar(0, xpLabel.getVar(0) + items.get(index).data.cost);
        if (index >= firstIndexToBeCounted) {
            countLabel.changeVar(0, countLabel.getVar(0) - 1);
        }
        String hullModId = items.get(index).data.id;
        // Don't remove hull mods that were already on the ship
        if (!originalVariant.hasHullMod(hullModId)) {
            checkerVariant.removeMod(hullModId);
        }
    }
}
