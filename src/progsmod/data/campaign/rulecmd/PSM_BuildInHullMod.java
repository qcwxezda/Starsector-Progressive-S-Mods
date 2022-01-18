package progsmod.data.campaign.rulecmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc.Token;

import progsmod.data.campaign.rulecmd.delegates.BuildInSModDelegate;
import progsmod.data.campaign.rulecmd.ui.Button;
import progsmod.data.campaign.rulecmd.ui.HullModButton;
import progsmod.data.campaign.rulecmd.ui.LabelWithVariables;
import progsmod.data.campaign.rulecmd.ui.PanelCreator;
import progsmod.data.campaign.rulecmd.ui.PanelCreator.PanelCreatorData;
import progsmod.data.campaign.rulecmd.ui.plugins.BuildInSelector;
import progsmod.data.campaign.rulecmd.util.HullModButtonData;
import util.SModUtils;

/** ProgSModBuildIn [fleetMember] [selectedVariant] [trigger] -- shows the build-in interface for
 *  the module of [fleetMember] whose variant is [selectedVariant].
 *  Build in the selected hull mods.
 *  Fire [trigger] upon confirmation. */
public class PSM_BuildInHullMod extends BaseCommandPlugin {

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) {
            return false;
        }

        final String titleString = "Choose hull mods to build in";
        final float titleHeight = 50f;
        final List<HullModButtonData> buttonData = new ArrayList<>();
        final FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).string);
        final ShipVariantAPI selectedVariant = (ShipVariantAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(1).string);
        
        for (String id : selectedVariant.getNonBuiltInHullmods()) {
            HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(id);
            if (!hullMod.isHidden() && !hullMod.isHiddenEverywhere()) {
                int cost = SModUtils.getBuildInCost(hullMod, selectedVariant.getHullSize(), fleetMember.getDeploymentPointsCost());
                buttonData.add(
                    new HullModButtonData(
                        id, 
                        hullMod.getDisplayName(), 
                        hullMod.getSpriteName(), 
                        cost + " XP", 
                        hullMod.getDescription(selectedVariant.getHullSize()),
                        hullMod.getEffect(), 
                        selectedVariant.getHullSize(), 
                        cost
                    ));
            }
        }

        final BuildInSelector plugin = new BuildInSelector();
        dialog.showCustomDialog(500f, 500f, 
            new BuildInSModDelegate() {
                @Override
                public void createCustomDialog(CustomPanelAPI panel) {
                    PanelCreator.createTitle(panel, titleString, titleHeight);
                    PanelCreatorData<List<HullModButton>> createdButtonsData = 
                        PanelCreator.createHullModButtonList(panel, buttonData, 45f, 10f, titleHeight, false);
                    LabelWithVariables<Integer> countLabel =
                        PanelCreator.createLabelWithVariables(panel, "Selected: %s/%s", Color.WHITE, 30f, Alignment.LMID, 0, SModUtils.getMaxSMods(fleetMember) - selectedVariant.getSMods().size()).created;
                    LabelWithVariables<Integer> xpLabel = 
                        PanelCreator.createLabelWithVariables(panel, "XP: %s", Color.WHITE, 30f, Alignment.RMID, (int) SModUtils.getXP(fleetMember.getId())).created;
                    Button showAllButton = PanelCreator.createButton(panel, "Show all", 100f, 25f, 10f, panel.getPosition().getHeight() + 6f).created;
                    plugin.init(this, createdButtonsData, xpLabel, countLabel, showAllButton, fleetMember, selectedVariant);
                }

                @Override
                public void customDialogCancel() {}

                @Override
                public void customDialogConfirm() {
                    boolean addedAtLeastOne = false;
                    String fmId = fleetMember.getId();
                    
                    for (HullModButton button: plugin.getSelected()) {
                        if (SModUtils.spendXP(fmId, button.data.cost)) {
                            selectedVariant.addPermaMod(button.data.id, true);
                            String hullModName = Global.getSettings().getHullModSpec(button.data.id).getDisplayName();
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
                    return "Build in";
                }

                @Override
                public CustomUIPanelPlugin getCustomPanelPlugin() {
                    return plugin;
                }

                @Override
                public boolean hasCancelButton() {
                    return true;
                }

                @Override
                public void showAllPressed(CustomPanelAPI panel, TooltipMakerAPI tooltipMaker) {
                    List<HullModButtonData> newButtonData = new ArrayList<>();
                    for (String id : Global.getSector().getPlayerFaction().getKnownHullMods()) {
                        if (selectedVariant.hasHullMod(id)) {
                            continue;
                        }
                        HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(id);
                        int cost = SModUtils.getBuildInCost(hullMod, selectedVariant.getHullSize(), fleetMember.getDeploymentPointsCost());
                        newButtonData.add(
                            new HullModButtonData(
                                id, 
                                hullMod.getDisplayName(), 
                                hullMod.getSpriteName(), 
                                cost + " XP",
                                hullMod.getDescription(selectedVariant.getHullSize()),
                                hullMod.getEffect(), 
                                selectedVariant.getHullSize(), 
                                cost)
                            );
                    }
                    Collections.sort(newButtonData, new Comparator<HullModButtonData>() {
                        @Override
                        public int compare(HullModButtonData a, HullModButtonData b) {
                            return a.name.compareTo(b.name);
                        }
                    });
                    PanelCreatorData<List<HullModButton>> newButtons = 
                        PanelCreator.addToHullModButtonList(panel, tooltipMaker, newButtonData, 45f, 10f, titleHeight, false);
                    plugin.addNewItems(newButtons.created);
                }
            }
        );
        return true;
    }
}
