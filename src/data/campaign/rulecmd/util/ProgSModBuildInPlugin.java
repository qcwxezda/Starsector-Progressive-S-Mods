package data.campaign.rulecmd.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Pair;

import data.campaign.rulecmd.util.ProgSModSelectPanelCreator.SelectorData;
import util.SModUtils;
public class ProgSModBuildInPlugin implements CustomUIPanelPlugin {

	private LabelAPI nSelectedLabel, remainingXPLabel;
	private List<SelectorData> selectorList;
	private FleetMemberAPI fleetMember;
	private float shipXP;
	private int numCanBuildIn;

	public void setData(LabelAPI nSelected, LabelAPI remainingXP, List<SelectorData> list, FleetMemberAPI member) {
		nSelectedLabel = nSelected;
		remainingXPLabel = remainingXP;
		selectorList = list;
		fleetMember = member;
		shipXP = SModUtils.getXP(fleetMember.getId());
		numCanBuildIn = SModUtils.getMaxSMods(fleetMember) - fleetMember.getVariant().getSMods().size();

		if (!SModUtils.Constants.IGNORE_NO_BUILD_IN) {
			removeCantBuildIn();
		}
	}

	/** Remove the hull mods that can't be built in (i.e. SO)
	 *  from the button list and disable them. */
	private void removeCantBuildIn() {
		Iterator<SelectorData> itr = selectorList.iterator();
		while (itr.hasNext()) {
			SelectorData entry = itr.next();
			HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(entry.hullModId);
			if (hullMod.hasTag("no_build_in")) {
				ProgSModSelectPanelCreator.disableEntryRed(entry);
				entry.costLabel.setText("Cannot be built in");
				entry.costLabel.setHighlight("Cannot be built in");
				itr.remove();
			}
		}
	}

	/** Returns the pair (number of checked buttons, sum of checked buttons' XP costs). 
     * Populates [checkedEntries] with the checked buttons if a non-null list was passed in.*/
    public Pair<Integer, Integer> tallyCheckedEntries(List<SelectorData> selectorList, List<SelectorData> checkedEntries) {
        if (!checkedEntries.isEmpty()) {
            checkedEntries.clear();
        }
        int numChecked = 0, sumChecked = 0;
        for (SelectorData entry : selectorList) {
            if (entry.button.isChecked()) {
                checkedEntries.add(entry);
                numChecked++;
                sumChecked += entry.hullModCost;
            }
        }
        return new Pair<>(numChecked, sumChecked);
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
				Pair<Integer, Integer> numAndSum = tallyCheckedEntries(selectorList, new ArrayList<SelectorData>());
				float remainingXP = shipXP - numAndSum.two;

				// Update the xp remaining and number selected labels
				ProgSModSelectPanelCreator.setNSelectedModsText(nSelectedLabel, numAndSum.one, numCanBuildIn);
				ProgSModSelectPanelCreator.setRemainingXPText(remainingXPLabel, remainingXP);

				// Disable or enable buttons depending on the remaining XP
				// and hull mod build-in slots
				for (SelectorData entry : selectorList) {
					if (entry.button.isChecked()) {
						continue;
					}
					if (entry.hullModCost > remainingXP) {
						if (entry.button.isEnabled()) {
							ProgSModSelectPanelCreator.disableEntryRed(entry);
						}
					}
					else if (numAndSum.one >= numCanBuildIn) {
						if (entry.button.isEnabled()) {
							ProgSModSelectPanelCreator.disableEntryGray(entry);
						}
					}
					else {
						if (!entry.button.isEnabled()) {
							ProgSModSelectPanelCreator.enableEntry(entry);
						}
					}	
				}
			}
		}
	}

	@Override
	public void render(float alphaMult) {
	}

	@Override
	public void renderBelow(float alphaMult) {}
}
