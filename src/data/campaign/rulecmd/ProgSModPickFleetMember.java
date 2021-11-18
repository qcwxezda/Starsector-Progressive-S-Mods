package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.util.Misc.Token;

import util.SModUtils;

/** ProgSModPickFleetMember [trigger] [menuId] 
 * -- fires [trigger] and changes $menuState to [menuId] 
 *    upon successful selection of a ship */
public class ProgSModPickFleetMember extends BaseCommandPlugin {

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Token> params, final Map<String, MemoryAPI> memoryMap)  {
        if (dialog == null) return false;
        if (params.size() != 2) return false;

        // This function excludes fighters
        List<FleetMemberAPI> playerFleet = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        
        dialog.showFleetMemberPickerDialog("Select a ship", "Ok", "Cancel", 3, 7, 58f, true, false, playerFleet, 
            new FleetMemberPickerListener() {

                @Override
                public void cancelledFleetMemberPicking() {}

                @Override
                public void pickedFleetMembers(List<FleetMemberAPI> fleetMembers) {
                    if (fleetMembers == null || fleetMembers.size() == 0) {
                        return;
                    }

                    FleetMemberAPI picked = fleetMembers.get(0);
                    memoryMap.get(MemKeys.LOCAL).set("$selectedShip", picked, 0f);

                    // Write some data to memory for use in the next menu screen
                    SModUtils.writeShipDataToMemory(picked, memoryMap);
                    
                    // Wait for player to finish picking a ship before messing with
                    // the menu states.
                    dialog.getVisualPanel().showFleetMemberInfo(picked, false);
                    memoryMap.get(MemKeys.LOCAL).set("$menuState", params.get(1).getString(memoryMap), 0f);
                    FireAll.fire(ruleId, dialog, memoryMap, params.get(0).getString(memoryMap));
                }

            }
        );

        return true;
    }
    
}