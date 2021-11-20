package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI.OptionTooltipCreator;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc.Token;

import util.SModUtils;

/** ProgSModFilterOptionsList [$fleetMember] [option1] [option2] [option3] [option4]
 *  [option1] is the option to build in hull mods; 
 *  [option2] is the option to remove them;
 *  [option3] is the option to increase S-Mod limit. 
 *  [option4] is to go back to main menu. */
public class ProgSModHandleOptionsList extends BaseCommandPlugin {

	@Override
	public boolean execute(String ruleId, final InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null || params.isEmpty()) {
            return false;
        }

        FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).string);
        int nSMods = fleetMember.getVariant().getSMods().size();
        int nSModsLimit = SModUtils.getMaxSMods(fleetMember);
        int nRemaining = nSModsLimit - nSMods;
        String buildInOption = params.get(1).getString(memoryMap);
        String removeOption = params.get(2).getString(memoryMap);
        String augmentOption = params.get(3).getString(memoryMap);
        String goBackOption = params.get(4).getString(memoryMap);

        dialog.getOptionPanel().clearOptions();

        // Add build in option
        dialog.getOptionPanel().addOption(
                String.format("Build up to %s hull mods into this ship", nRemaining)
            , buildInOption);

        // If the ship does not have any hull mods that can be built in,
        // disable the build-in option
        if (fleetMember.getVariant().getNonBuiltInHullmods().isEmpty()) {
            dialog.getOptionPanel().setEnabled(buildInOption, false);
            dialog.getOptionPanel().addOptionTooltipAppender(buildInOption, 
                new OptionTooltipCreator() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean hadOtherText) {
                        tooltip.addPara("This ship does not have any active hull mods that can be built in.", 0f);
                    }
                }
            );
        }

        // Add in the remove option if that was allowed
        if (SModUtils.Constants.XP_REFUND_FACTOR >= 0) {
            dialog.getOptionPanel().addOption("Select built-in hull mods to remove from this ship", removeOption);

            // If the ship does not have any S-Mods to remove, disable the remove option
            if (nSMods == 0) {
                dialog.getOptionPanel().setEnabled(removeOption, false);
                dialog.getOptionPanel().addOptionTooltipAppender(removeOption, 
                    new OptionTooltipCreator() {
                        @Override
                        public void createTooltip(TooltipMakerAPI tooltip, boolean hadOtherText) {
                            tooltip.addPara("This ship does not have any built-in hull mods that can be removed.", 0f);
                        }
                        
                    }
                );
            }
        }

        // Add in the augment option if that was allowed
        if (SModUtils.Constants.ALLOW_INCREASE_SMOD_LIMIT) {
            dialog.getOptionPanel().addOption(
                    String.format("Increase this ship's built-in hull mod limit from %s to %s", nSModsLimit, nSModsLimit + 1), 
                augmentOption);
        }

        // Add in the back to main menu option
        dialog.getOptionPanel().addOption("Go back", goBackOption);

        // Add in the text panel
        dialog.getTextPanel()
            .addPara(String.format("The %s has %s out of %s built-in hull mods.", fleetMember.getShipName(), nSMods, nSModsLimit))
            .setHighlight("" + fleetMember.getShipName(), "" + nSMods, "" + nSModsLimit);
        
        if (SModUtils.Constants.XP_REFUND_FACTOR >= 0) {
            int refundPercent = (int) (SModUtils.Constants.XP_REFUND_FACTOR * 100);
            dialog.getTextPanel()
                .addPara(String.format("Removing an existing built-in hull mod will refund %s%% of the XP spent.",refundPercent))
                .setHighlight("" + refundPercent);
        }
        if (SModUtils.Constants.ALLOW_INCREASE_SMOD_LIMIT) {
            int numOverLimit = SModUtils.getNumOverLimit(fleetMember.getId());
            dialog.getTextPanel()
                .addPara(
                    String.format("You have increased this ship's limit of built-in hull mods a total of %s time%s.",
                        numOverLimit,
                        numOverLimit == 1 ? "" : "s")
                    )
                .setHighlight("" + numOverLimit);
        }

		return true;
	}
}