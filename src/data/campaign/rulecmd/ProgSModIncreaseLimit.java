package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

import util.SModUtils;
import util.SModUtils.ShipData;

/** ProgSModIncreaseLimit [fleetMember] -- increases [fleetMember]'s built-in hull mod limit by one. */
public class ProgSModIncreaseLimit extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) {
            return false;
        }

        FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).getVarNameAndMemory(memoryMap).name);
        String fleetMemberId = fleetMember.getId();
        ShipData data = SModUtils.SHIP_DATA_TABLE.get(fleetMemberId);
        if (data == null) {
            SModUtils.SHIP_DATA_TABLE.put(fleetMemberId, new ShipData(0, 1));
        }
        else {
            data.permaModsOverLimit++;
        }

        SModUtils.writeShipDataToMemory(fleetMember, memoryMap);

        return true;
    }
    
}
