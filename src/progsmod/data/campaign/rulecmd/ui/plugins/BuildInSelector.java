package progsmod.data.campaign.rulecmd.ui.plugins;

import java.util.BitSet;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import org.lwjgl.util.vector.Vector2f;

import progsmod.data.campaign.rulecmd.delegates.BuildInSModDelegate;
import progsmod.data.campaign.rulecmd.ui.Button;
import progsmod.data.campaign.rulecmd.ui.HullModButton;
import progsmod.data.campaign.rulecmd.ui.LabelWithVariables;
import progsmod.data.campaign.rulecmd.ui.PanelCreator.PanelCreatorData;
import util.SModUtils;

public class BuildInSelector extends Selector<HullModButton> {

    // 1 variable: XP
    private LabelWithVariables<Integer> xpLabel;
    // 2 variables: # selected, max allowed
    private LabelWithVariables<Integer> countLabel;
    private BitSet permanentlyDisabled;
    private Button showAllButton;
    private ShipVariantAPI variant;
    private String variantId;
    
    private BuildInSModDelegate delegate;
    private TooltipMakerAPI tooltipMaker;
    private CustomPanelAPI panel;

    public void init(
            BuildInSModDelegate delegate,
            PanelCreatorData<List<HullModButton>> data, 
            LabelWithVariables<Integer> xpLabel, 
            LabelWithVariables<Integer> countLabel,
            Button showAllButton, 
            ShipVariantAPI variant) {
        super.init(data.created);
        this.delegate = delegate;
        this.panel = data.panel;
        this.tooltipMaker = data.tooltipMaker;
        this.xpLabel = xpLabel;
        this.countLabel = countLabel;
        this.showAllButton = showAllButton;
        this.variant = variant;
        permanentlyDisabled = new BitSet();
        // Get the variant id for [variant]
        // If it doesnt exist, find one from the settings.
        // If that doesn't exist, just disable the show all button
        variantId = variant.getHullVariantId();
        if (!Global.getSettings().doesVariantExist(variantId)) {
            List<String> possibleIds = Global.getSettings().getHullIdToVariantListMap().get(variant.getHullSpec().getHullId());
            if (!possibleIds.isEmpty()) {
                variantId = possibleIds.get(0);
            } 
            else {
                showAllButton.disable();
            }
        }
        updateItems();
    }

    public void addNewItems(List<HullModButton> items) {
        this.items.addAll(items);
        updateItems();
    }

    private void updateItems() {
        int xp = xpLabel.getVar(0);
        int count = countLabel.getVar(0);
        int maxCount = countLabel.getVar(1);
        // Disable all of the unapplicable entries
        BitSet unapplicable = disableUnapplicable();
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

    /** Disable an item, but only if it isn't already selected and it hasn't 
     *  already been permanently disabled with a different message. */
    private void disable(int index, String reason, boolean highlight) {
        HullModButton item = items.get(index);
        // Ignore already disabled items
        if (permanentlyDisabled.get(index)) {
            return;
        }
        // Ignore already selected items
        if (item.isSelected()) {
            return;
        }
        item.disable(reason, highlight);
    }

    /** Enable an item, but only if it hasn't already been permanently disabled. */
    private void enable(int index, String reason) {
        if (permanentlyDisabled.get(index)) {
            return;
        }
        items.get(index).enable(reason);
    }

    public void permaDisable(int index, String reason, boolean highlight) {
        permanentlyDisabled.set(index);
        items.get(index).disable(reason, highlight);
    }

    /** Returns a set of indices of buttons that were disabled by this
     *  function. */
    private BitSet disableUnapplicable() {
        boolean checkedEntriesChanged = true;
        BitSet disabledIndices = new BitSet();
        while (checkedEntriesChanged) {
            ShipAPI tempShip = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).spawnShipOrWing(variantId, new Vector2f(), 0f);
            ShipVariantAPI checkerVariant = tempShip.getVariant().clone();
            tempShip.setVariantForHullmodCheckOnly(checkerVariant);
            checkerVariant.getHullMods().clear();
            
            // Add all the mods that are currently checked
            for (int i = 0; i < items.size(); i++) {
                HullModButton button = items.get(i);
                if (button.isSelected()) {
                    checkerVariant.addPermaMod(button.data.id);
                    applyHullModEffectsToShip(tempShip, Global.getSettings().getHullModSpec(button.data.id));
                }
            }

            for (String id : variant.getHullMods()) {
                if (!checkerVariant.hasHullMod(id)) {
                    checkerVariant.addPermaMod(id);
                    applyHullModEffectsToShip(tempShip, Global.getSettings().getHullModSpec(id));
                }
            }

            // Now check which mods are no longer applicable
            // Since hull mods may have dependencies, some checked entries may
            // need to be unchecked.
            // Since dependencies can be chained, we need to do this in a loop.
            // (# of loops is bounded by # of checked entries as well as
            // longest hull mod dependency chain)
            checkedEntriesChanged = false;
            // Hull mods already on the ship should always be able to be built in
            for (int i = variant.getHullMods().size(); i < items.size(); i++) {
                HullModButton button = items.get(i);
                HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(button.data.id);
                if (!hullMod.getEffect().isApplicableToShip(tempShip)) {
                    if (button.isSelected()) {
                        forceDeselect(i);
                        checkedEntriesChanged = true;
                    }
                    String unapplicableReason = SModUtils.shortenText(hullMod.getEffect().getUnapplicableReason(tempShip), button.description);
                    if (unapplicableReason == null) {
                        unapplicableReason = "Can't build in (no reason given, default message)";
                    }
                    disable(i, unapplicableReason, true);
                    disabledIndices.set(i);
                }
            }
        }
        return disabledIndices;
    }

    private void applyHullModEffectsToShip(ShipAPI ship, HullModSpecAPI hullMod) {
        HullModEffect effect = hullMod.getEffect();
        effect.applyEffectsBeforeShipCreation(ship.getHullSize(), ship.getMutableStats(), hullMod.getId());
        effect.applyEffectsAfterShipCreation(ship, hullMod.getId());
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
        updateItems();
    }

    @Override
    protected void onDeselected(int index) {
        xpLabel.changeVar(0, xpLabel.getVar(0) + items.get(index).data.cost);
        countLabel.changeVar(0, countLabel.getVar(0) - 1);
        updateItems();
    }
    
    @Override
    protected void forceDeselect(int index) {
        super.forceDeselect(index);
        xpLabel.changeVar(0, xpLabel.getVar(0) + items.get(index).data.cost);
        countLabel.changeVar(0, countLabel.getVar(0) - 1);
    }
}
