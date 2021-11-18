package data.campaign.rulecmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc.Token;

import data.campaign.rulecmd.ProgSModSelectPanelCreator.SelectorData;
import util.SModUtils;
import util.SModUtils.ShipData;

/** ProgSModRemove [fleetMember] -- shows the built-in hull mods for [fleetMember].
 *  Remove the selected built-in hull mods. 
 *  Fire [trigger] upon confirmation. */
public class ProgSModRemove extends BaseCommandPlugin {

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) {
            return false;
        }

        final String titleString = "Choose built-in hull mods to remove";
        final List<HullModSpecAPI> builtInMods = new ArrayList<>();
        final FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).getVarNameAndMemory(memoryMap).name);
        final List<SelectorData> selectorList = new ArrayList<>();
        
        Collection<String> builtInIds = fleetMember.getVariant().getSMods();
        for (String id : builtInIds) {
            builtInMods.add(Global.getSettings().getHullModSpec(id));
        }

        dialog.showCustomDialog(500, 500, 
            new CustomDialogDelegate() {
                @Override
                public void createCustomDialog(CustomPanelAPI panel) {
                    selectorList.addAll(
                        ProgSModSelectPanelCreator.createHullModSelectionPanel(
                            panel, 
                            titleString, 
                            builtInMods, 
                            fleetMember, 
                            true
                        )
                    );
                }

                @Override
                public void customDialogCancel() {}
            
                @Override
                public void customDialogConfirm() {
                    boolean removedAtLeastOne = false;
                    ShipData shipData = SModUtils.SHIP_DATA_TABLE.get(fleetMember.getId());
                    if (shipData == null) {
                        shipData = new ShipData(0, 0);
                        SModUtils.SHIP_DATA_TABLE.put(fleetMember.getId(), shipData);
                    }
                    
                    int xpGained = 0;
                    for (SelectorData data : selectorList) {
                        if (data.button.isChecked()) {
                            fleetMember.getVariant().removePermaMod(data.hullModId);
                            xpGained += data.hullModCost * SModUtils.Constants.XP_REFUND_FACTOR;
                            String hullModName = Global.getSettings().getHullModSpec(data.hullModId).getDisplayName();
                            LabelAPI confirmText = dialog.getTextPanel().addPara("Removed " + hullModName);
                            confirmText.setHighlight(hullModName);
                            removedAtLeastOne = true;
                        }
                    }
                    if (removedAtLeastOne) {
                        Global.getSoundPlayer().playUISound("ui_wait_interrupt", 1f, 1f);
                        shipData.xp += xpGained;
                        LabelAPI xpGainText = dialog.getTextPanel().addPara("The " 
                            + fleetMember.getShipName() + " gained " + xpGained);
                        xpGainText.setHighlight(fleetMember.getShipName(), "" + xpGained);
                        SModUtils.writeShipDataToMemory(fleetMember, memoryMap);
                        FireAll.fire(ruleId, dialog, memoryMap, params.get(1).getString(memoryMap));
                    }
                }
            
                @Override
                public String getCancelText() {
                    return "Cancel";
                }
            
                @Override
                public String getConfirmText() {
                    return "Remove";
                }
            
                @Override
                public CustomUIPanelPlugin getCustomPanelPlugin() {
                    return null;
                }
            
                @Override
                public boolean hasCancelButton() {
                    return true;
                }      
            }
        );
        return true;
    }
}
