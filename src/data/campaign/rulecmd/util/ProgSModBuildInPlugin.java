package data.campaign.rulecmd.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import org.lwjgl.util.vector.Vector2f;

import data.campaign.rulecmd.util.ProgSModHullModSelector.SelectorData;
import util.SModUtils;
public class ProgSModBuildInPlugin implements CustomUIPanelPlugin {

    private LabelAPI nSelectedLabel, remainingXPLabel;
    // Note: LinkedList for O(n) initialization,
    // ArrayList for faster iteration but O(n^2) initialization
    private List<SelectorData> selectorList;
    private float shipXP, remainingXP;
    private int numCanBuildIn, numChecked, sumChecked;
    private ButtonAPI showAllButton;
    private ProgSModHullModSelector panelCreator;
    private ShipVariantAPI variant;
    private String variantId;
    private FleetMemberAPI fleetMember;
    private BitSet checkedEntries;
    private boolean isShowingAll = false;
    private SectorEntityToken interactionTarget;
    private float mouseY;

    public void setData(
            LabelAPI nSelected, 
            LabelAPI remainingXP, 
            List<SelectorData> list, 
            FleetMemberAPI fleetMember,
            ShipVariantAPI variant, 
            ButtonAPI showAllButton, 
            ProgSModHullModSelector panelCreator,
            SectorEntityToken interactionTarget) {
        nSelectedLabel = nSelected;
        remainingXPLabel = remainingXP;
        selectorList = list;
        this.fleetMember = fleetMember;
        shipXP = SModUtils.getXP(fleetMember.getId());
        this.remainingXP = shipXP;
        numCanBuildIn = SModUtils.getMaxSMods(fleetMember) - variant.getSMods().size();
        numChecked = sumChecked = 0;
        this.variant = variant;
        this.showAllButton = showAllButton;
        this.panelCreator = panelCreator;
        this.interactionTarget = interactionTarget;
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
                showAllButton.setEnabled(false);
            }
        }
        pruneHullMods();
    }

    /** Remove the hull mods that can't be built in (i.e. SO)
     *  from the button list and disable them. */
    private void removeCantBuildIn() {
        Iterator<SelectorData> itr = selectorList.iterator();
        while (itr.hasNext()) {
            SelectorData entry = itr.next();
            HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(entry.hullModId);
            if (hullMod.hasTag("no_build_in")) {
                panelCreator.disableRedAndChangeText(entry, "Cannot be built in");
                itr.remove();
            }
        }
    }

    /** Remove the hull mods that require a station or spaceport,
     *  if fleet is not at either. */
    private void removeIfStationRequired() {
        Iterator<SelectorData> itr = selectorList.iterator();
        while (itr.hasNext()) {
            SelectorData entry = itr.next();
            HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(entry.hullModId);
            if (!SModUtils.canModifyHullMod(hullMod, interactionTarget)) {
                panelCreator.disableRedAndChangeText(entry, "Requires docking at a spaceport or orbital station");
                itr.remove();
            }
        }
    }

    /** Returns [true] iff the checked entries are the same as [selectedHullModIds].
     * If not [checkAll], only checks the two entries closest to the cursor's y-coordinate.
     * Changes [numChecked] and [sumChecked].
     * Changes [selectedHullModIds] to match current checked ids. */ 
    public boolean tallyCheckedEntries(boolean checkAll) {
        boolean noChange = true;
        if (checkedEntries == null) {
            noChange = false;
            checkAll = true;
            checkedEntries = new BitSet(selectorList.size());
        }

        int i = checkAll ? 0 : getLastEntryBeforeMouseY();
        int j = 0;
        Iterator<SelectorData> itr = selectorList.listIterator(i);
        while (itr.hasNext()) {
            SelectorData entry = itr.next();
            if (entry.button.isChecked() && !checkedEntries.get(i)) {
                numChecked++;
                sumChecked += entry.hullModCost;
                noChange = false;
                checkedEntries.set(i);
            }
            else if (!entry.button.isChecked() && checkedEntries.get(i)) {
                numChecked--;
                sumChecked -= entry.hullModCost;
                noChange = false;
                checkedEntries.clear(i);
            }
            if (!checkAll && j >= 1) {
                break;
            }
            i++; 
            j++;
        }
        return noChange;
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
                mouseY = event.getY();

                // If the show all button was checked, disable it
                // and show the list of all hull mods
                if (showAllButton.isChecked()) {
                    showAllButton.setChecked(false);
                    showAllButton.setEnabled(false);

                    List<HullModSpecAPI> allHullMods = new ArrayList<>();
                    for (String id : Global.getSector().getPlayerFaction().getKnownHullMods()) {
                        allHullMods.add(Global.getSettings().getHullModSpec(id));
                    }

                    panelCreator.showAdditionalHullMods(filterHullMods(allHullMods), variant.getHullSize(), fleetMember.getDeploymentPointsCost(), selectorList);
                    pruneHullMods();

                    // Set selectedHullModIds to null to force an update
                    checkedEntries = null;
                    isShowingAll = true;
                }

                // If nothing was changed, return early to avoid unnecessary computation
                if (tallyCheckedEntries(false)) {
                    return;
                }

                remainingXP = shipXP - sumChecked;

                // Disable or enable buttons depending on the remaining XP
                // and hull mod build-in slots
                for (SelectorData entry : selectorList) {
                    if (entry.button.isChecked()) {
                        continue;
                    }
                    if (entry.hullModCost > remainingXP) {
                        if (entry.button.isEnabled()) {
                            panelCreator.disableEntryRed(entry);
                        }
                    }
                    else if (numChecked >= numCanBuildIn) {
                        if (entry.button.isEnabled()) {
                            panelCreator.disableEntryGray(entry);
                        }
                    }
                    else {
                        if (!entry.button.isEnabled()) {
                            panelCreator.enableEntry(entry);
                        }
                    }	
                }

                if (isShowingAll) { 
                    disableUnapplicable();
                }

                // Update the xp remaining and number selected labels
                panelCreator.setNSelectedModsText(nSelectedLabel, numChecked, numCanBuildIn);
                panelCreator.setRemainingXPText(remainingXPLabel, remainingXP);
                // Only care about the first event since this function is the same
                // regardless of what the event is
                return;
            }
        }
    }

    @Override
    public void render(float alphaMult) {
    }

    @Override
    public void renderBelow(float alphaMult) {}

    /* Disable the hull mods that are not applicable to the current ship variant.
       Returns the number of entries that were checked that became unchecked. */
    private void disableUnapplicable() {
        // Temporarily hijack the player shuttle (lol) so we have a ShipAPI for testing
        // ShipAPI tempShip = Global.getCombatEngine().getPlayerShip();
        // ShipVariantAPI originalVariant = tempShip.getVariant();
        // HullSize originalHullSize = tempShip.getHullSize();
        // float originalMaxHP = tempShip.getMaxHitpoints();
        // ShipVariantAPI checkerVariant = variant.clone();
        // tempShip.setVariantForHullmodCheckOnly(checkerVariant);
        // tempShip.setHullSize(variant.getHullSize());
        // tempShip.setMaxHitpoints(variant.getHullSpec().getHitpoints());
        // ShieldSpecAPI variantShields = variant.getHullSpec().getShieldSpec();
        // tempShip.setShield(variantShields.getType(), variantShields.getUpkeepCost(), variantShields.getFluxPerDamageAbsorbed(), variantShields.getArc());
        boolean checkedEntriesChanged = true;
        while (checkedEntriesChanged) {
            //ShipVariantAPI checkerVariant = variant.clone();
            ShipAPI tempShip = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).spawnShipOrWing(variantId, new Vector2f(), 0f);
            //tempShip.setVariantForHullmodCheckOnly(checkerVariant);
            ShipVariantAPI checkerVariant = tempShip.getVariant();
            
            // Add all the mods that are currently checked
            // a s well as all the hullmods already in [variant]
            checkerVariant.getHullMods().clear();
            for (SelectorData entry : selectorList) {
                if (entry.button.isChecked()) {
                    checkerVariant.addPermaMod(entry.hullModId);
                    applyHullModEffectsToShip(tempShip, Global.getSettings().getHullModSpec(entry.hullModId));
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
            int i = 0;
            for (SelectorData entry : selectorList) {
                HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(entry.hullModId);
                if (!hullMod.getEffect().isApplicableToShip(tempShip)) {
                    if (entry.button.isChecked()) {
                        entry.button.setChecked(false);
                        checkedEntriesChanged = true;
                        numChecked--;
                        sumChecked -= entry.hullModCost;
                        checkedEntries.clear(i);
                        remainingXP += entry.hullModCost;
                    }
                    String unapplicableReason = panelCreator.shortenText(hullMod.getEffect().getUnapplicableReason(tempShip), entry.costLabel);
                    if (unapplicableReason == null) {
                        unapplicableReason = "Can't build in (no reason given, default message)";
                    }
                    panelCreator.disableRedAndChangeText(entry, unapplicableReason);
                } else {
                    panelCreator.changeText(entry, entry.hullModCost + " XP");
                    // Enable entry if possible
                    if (remainingXP >= entry.hullModCost && numChecked < numCanBuildIn) {
                        panelCreator.enableEntry(entry);
                    }
                }
                i++;
            }
        }

        // tempShip.setShield(ShieldType.NONE, 0f, 1f, 0f);
        // tempShip.setMaxHitpoints(originalMaxHP);
        // tempShip.setHullSize(originalHullSize);
        // tempShip.setVariantForHullmodCheckOnly(originalVariant);
    }

    private void applyHullModEffectsToShip(ShipAPI ship, HullModSpecAPI hullMod) {
        HullModEffect effect = hullMod.getEffect();
        effect.applyEffectsBeforeShipCreation(ship.getHullSize(), ship.getMutableStats(), hullMod.getId());
        effect.applyEffectsAfterShipCreation(ship, hullMod.getId());
    }

    /** Returns a sorted list of the hull mods that [variant] does not already have. */
    private List<HullModSpecAPI> filterHullMods(List<HullModSpecAPI> hullMods) {
        List<HullModSpecAPI> filteredHullMods = new ArrayList<>();
        for (HullModSpecAPI hullMod : hullMods) {
            if (variant.hasHullMod(hullMod.getId())) {
                continue;
            }
            filteredHullMods.add(hullMod);
        }
        Collections.sort(filteredHullMods, 
            new Comparator<HullModSpecAPI> () {
                @Override
                public int compare(HullModSpecAPI a, HullModSpecAPI b) {
                    return a.getDisplayName().compareTo(b.getDisplayName());
                }
            }
        );
        return filteredHullMods;
    }

    /** Permanently disable and remove hull mods from the selector list
     *  that will never be able to be modified.*/
    private void pruneHullMods() {
        if (!SModUtils.Constants.IGNORE_NO_BUILD_IN) {
            removeCantBuildIn();
        }
        removeIfStationRequired();
    }

    // /** Finds the index of the first visible entry in the selector list, i.e.
    //  *  the top-most button that isn't hidden due to the scroll bar location.
    //  *  Has a fairly generous slack of [50f]. */
    // private int getFirstVisibleEntry() {
    //     if (selectorList.size() <= 1) {
    //         return 0;
    //     }
    //     int left = 0, right = selectorList.size() - 1;
    //     while (left < right) {
    //         int mid = (left + right) / 2;
    //         float yPos = selectorList.get(mid).nameLabel.getPosition().getY();
    //         if (yPos > positionYMax + 50f) {
    //             left = mid + 1;
    //         } else {
    //             right = mid;
    //         }
    //     }
    //     return left;
    // }

    /** Finds the index of the last selector list entry whose y-coordinate is 
     *  greater than the y-coordinate of the mouse.
     *  (The list is descending in y-coordinate)
     *  The selected entry, if any,
     *  must be either this one or the next one. */ 
    private int getLastEntryBeforeMouseY() {
        if (selectorList.size() <= 1) {
            return 0;
        }
        int left = 0, right = selectorList.size() - 1;
        while (left < right) {
            int mid = (left + right + 1) / 2;
            float yPos = selectorList.get(mid).nameLabel.getPosition().getY();
            if (yPos > mouseY) {
                left = mid;
            } 
            else {
                right = mid - 1;
            }
        }
        return left;
    }
}
