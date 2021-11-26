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
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.Misc.Token;

import data.campaign.rulecmd.util.ProgSModBuildInPlugin;
import data.campaign.rulecmd.util.ProgSModSelectPanelCreator;
import data.campaign.rulecmd.util.ProgSModSelectPanelCreator.SelectorData;
import util.SModUtils;

/** ProgSModBuildIn [fleetMember] [selectedVariant] [trigger] -- shows the build-in interface for
 *  the module of [fleetMember] whose variant is [selectedVariant].
 *  Build in the selected hull mods.
 *  Fire [trigger] upon confirmation. */
public class ProgSModBuildIn extends BaseCommandPlugin {

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) {
            return false;
        }

        final String titleString = "Choose hull mods to build in";
        final List<HullModSpecAPI> nonBuiltInMods = new ArrayList<>();
        final FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).string);
        final ShipVariantAPI selectedVariant = (ShipVariantAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(1).string);
        final List<SelectorData> selectorList = new LinkedList<>();
        final ProgSModBuildInPlugin plugin = new ProgSModBuildInPlugin();
        
        Collection<String> nonBuiltInIds = selectedVariant.getNonBuiltInHullmods();
        for (String id : nonBuiltInIds) {
            HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(id);
            if (!hullMod.isHidden() && !hullMod.isHiddenEverywhere()) {
                nonBuiltInMods.add(hullMod);
            }
        }

        dialog.showCustomDialog(500, 500, 
            new CustomDialogDelegate() {
                @Override
                public void createCustomDialog(CustomPanelAPI panel) {
                    ProgSModSelectPanelCreator panelCreator = new ProgSModSelectPanelCreator(panel, false);
                    ButtonAPI showAllButton = 
                        panelCreator.createHullModSelectionPanel(
                            titleString, 
                            nonBuiltInMods, 
                            selectedVariant.getHullSize(),
                            fleetMember.getDeploymentPointsCost(), 
                            selectorList
                        );
                    Pair<LabelAPI, LabelAPI> textPair = panelCreator.addCountAndXPToPanel();
                    plugin.setData(
                        textPair.one, 
                        textPair.two, 
                        selectorList,
                        fleetMember,
                        selectedVariant,
                        showAllButton,
                        panelCreator,
                        dialog.getInteractionTarget()
                    );
                }

                @Override
                public void customDialogCancel() {}
            
                @Override
                public void customDialogConfirm() {
                    boolean addedAtLeastOne = false;
                    String fmId = fleetMember.getId();
                    
                    for (SelectorData data : selectorList) {
                        if (data.button.isChecked() && SModUtils.spendXP(fmId, data.hullModCost)) {
                            selectedVariant.addPermaMod(data.hullModId, true);
                            String hullModName = Global.getSettings().getHullModSpec(data.hullModId).getDisplayName();
                            dialog.getTextPanel().addPara("Built in " + hullModName)
                                .setHighlight(hullModName);
                            addedAtLeastOne = true;
                        }
                    }
                    if (addedAtLeastOne) {
                        Global.getSoundPlayer().playUISound("ui_acquired_hullmod", 1f, 1f);
                        SModUtils.displayXP(dialog, fleetMember);
                        FireAll.fire(ruleId, dialog, memoryMap, params.get(2).getString(memoryMap));
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
