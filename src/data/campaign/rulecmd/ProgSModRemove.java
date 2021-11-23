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
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc.Token;

import data.campaign.rulecmd.util.ProgSModRemovePlugin;
import data.campaign.rulecmd.util.ProgSModSelectPanelCreator;
import data.campaign.rulecmd.util.ProgSModSelectPanelCreator.SelectorData;
import util.SModUtils;

/** ProgSModRemove [fleetMember] [selectedVariant] [trigger] -- shows the built-in hull mods for 
 *  the module of [fleetMember] whose variant is [selectedVariant].
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
        final FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).string);
        final ShipVariantAPI selectedVariant = (ShipVariantAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(1).string);
        final List<SelectorData> selectorList = new ArrayList<>();
        final ProgSModRemovePlugin plugin = new ProgSModRemovePlugin();
        
        Collection<String> builtInIds = selectedVariant.getSMods();
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
                            selectedVariant.getHullSize(),
                            fleetMember.getDeploymentPointsCost(), 
                            true
                        )
                    );
                    LabelAPI xpLabel = ProgSModSelectPanelCreator.addXPToPanel(panel);
                    plugin.setData(xpLabel, selectorList, SModUtils.getXP(fleetMember.getId()));
                }

                @Override
                public void customDialogCancel() {}
            
                @Override
                public void customDialogConfirm() {
                    boolean removedAtLeastOne = false;
                    
                    int xpGained = 0;
                    for (SelectorData data : selectorList) {
                        if (data.button.isChecked()) {
                            selectedVariant.removePermaMod(data.hullModId);
                            xpGained += data.hullModCost * SModUtils.Constants.XP_REFUND_FACTOR;
                            String hullModName = Global.getSettings().getHullModSpec(data.hullModId).getDisplayName();
                            dialog.getTextPanel().addPara("Removed " + hullModName).setHighlight(hullModName);
                            removedAtLeastOne = true;
                        }
                    }
                    if (removedAtLeastOne) {
                        Global.getSoundPlayer().playUISound("ui_objective_constructed", 1f, 1f);
                        SModUtils.giveXP(fleetMember.getId(), xpGained);
                        dialog.getTextPanel()
                            .addPara(String.format("The %s gained %s XP", fleetMember.getShipName(), xpGained))
                            .setHighlight(fleetMember.getShipName(), "" + xpGained);
                        FireAll.fire(ruleId, dialog, memoryMap, params.get(2).getString(memoryMap));
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
