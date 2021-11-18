package data.campaign.rulecmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.Misc.Token;

import data.campaign.rulecmd.ProgSModSelectPanelCreator.SelectorData;
import util.SModUtils;
import util.SModUtils.ShipData;

/** ProgSModBuildIn [fleetMember] [trigger] -- shows the build-in interface for [fleetMember].
 *  Build in the selected hull mods.
 *  Fire [trigger] upon confirmation. */
public class ProgSModBuildIn extends BaseCommandPlugin {

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) {
            return false;
        }

        final String titleString = "Choose hull mods to build in";
        final List<HullModSpecAPI> builtInMods = new ArrayList<>();
        final FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).getVarNameAndMemory(memoryMap).name);
        final List<SelectorData> selectorList = new LinkedList<>();
        final ProgSModBuildInPlugin plugin = new ProgSModBuildInPlugin();
        
        Collection<String> builtInIds = fleetMember.getVariant().getNonBuiltInHullmods();
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
                            false
                        )
                    );
                    Pair<LabelAPI, LabelAPI> textPair = ProgSModSelectPanelCreator.addCountAndXPToPanel(panel);
                    plugin.setData(textPair.one, textPair.two, selectorList, fleetMember);
                }

                @Override
                public void customDialogCancel() {}
            
                @Override
                public void customDialogConfirm() {
                    boolean addedAtLeastOne = false;
                    ShipData shipData = SModUtils.SHIP_DATA_TABLE.get(fleetMember.getId());
                    if (shipData == null) {
                        return;
                    }
                    
                    for (SelectorData data : selectorList) {
                        if (data.button.isChecked()) {
                            if (shipData.xp >= data.hullModCost) {
                                fleetMember.getVariant().addPermaMod(data.hullModId, true);
                                shipData.xp -= data.hullModCost;
                                String hullModName = Global.getSettings().getHullModSpec(data.hullModId).getDisplayName();
                                LabelAPI confirmText = dialog.getTextPanel().addPara("Built in " + hullModName);
                                confirmText.setHighlight(hullModName);
                                addedAtLeastOne = true;
                            }
                        }
                    }
                    if (addedAtLeastOne) {
                        Global.getSoundPlayer().playUISound("ui_acquired_hullmod", 1f, 1f);
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
                    return "Confirm";
                }
            
                @Override
                public CustomUIPanelPlugin getCustomPanelPlugin() {
                    return plugin;
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