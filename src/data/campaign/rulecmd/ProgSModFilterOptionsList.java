package data.campaign.rulecmd;

import java.util.Collection;
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


/** ProgSModFilterOptionsList [$fleetMember] [option1] (option2) disables options based on $fleetMember's 
 *  data. [option1] is the option to build in hull mods; [option2] is the option to remove them. */
public class ProgSModFilterOptionsList extends BaseCommandPlugin {

	@Override
	public boolean execute(String ruleId, final InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null || params.isEmpty()) {
            return false;
        }

        FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).getVarNameAndMemory(memoryMap).name);
        Collection<String> sMods = fleetMember.getVariant().getSMods();

        // If the ship does not have any hull mods that can be built in,
        // disable the build-in option
        final int numRemaining = SModUtils.getMaxSMods(fleetMember) - sMods.size();
        if (fleetMember.getVariant().getNonBuiltInHullmods().isEmpty()) {
            String buildInOption = params.get(1).getString(memoryMap);
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

        // If the ship does not have any S-Mods to remove, disable the remove option
        if (sMods.isEmpty() && params.size() > 2) {
            String removeOption = params.get(2).getString(memoryMap);
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

		return true;
	}
}