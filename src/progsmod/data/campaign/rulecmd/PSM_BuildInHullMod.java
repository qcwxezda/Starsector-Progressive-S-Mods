package progsmod.data.campaign.rulecmd;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.*;
import com.fs.starfarer.api.loading.*;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc.*;
import progsmod.data.campaign.rulecmd.delegates.*;
import progsmod.data.campaign.rulecmd.ui.Button;
import progsmod.data.campaign.rulecmd.ui.*;
import progsmod.data.campaign.rulecmd.ui.PanelCreator.*;
import progsmod.data.campaign.rulecmd.ui.plugins.*;
import progsmod.data.campaign.rulecmd.util.*;
import util.*;

import java.awt.*;
import java.util.List;
import java.util.*;

/** ProgSModBuildIn [fleetMember] [selectedVariant] [trigger] -- shows the build-in interface for
 *  the module of [fleetMember] whose variant is [selectedVariant].
 *  Build in the selected hull mods.
 *  Fire [trigger] upon confirmation. */
@SuppressWarnings("unused")
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


        Collection<String> enhancedAlready = selectedVariant.getSModdedBuiltIns();

        List<String> toAdd = new ArrayList<>();
        if (selectedVariant.getHullSpec() != null) {
            for (String id : selectedVariant.getHullSpec().getBuiltInMods()) {
                HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(id);
                HullModEffect effect = hullMod.getEffect();
                if (effect.hasSModEffect() && !effect.isSModEffectAPenalty() && !enhancedAlready.contains(id)) {
                    toAdd.add(id);
                }
            }
        }

        final int firstIndexToBeCounted = toAdd.size();

        for (String id : selectedVariant.getNonBuiltInHullmods()) {
            HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(id);
            if (!hullMod.isHidden() && !hullMod.isHiddenEverywhere()) {
                toAdd.add(id);
            }
        }

        for (int i = 0; i < toAdd.size(); i++) {
            String id = toAdd.get(i);
            HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(id);
            int cost = SModUtils.getBuildInCost(hullMod, selectedVariant.getHullSize(), fleetMember.getUnmodifiedDeploymentPointsCost());
            boolean isEnhanceOnly = i < firstIndexToBeCounted;
            buttonData.add(
                    new HullModButtonData(
                            id,
                            hullMod.getDisplayName() + (isEnhanceOnly ? "*" : ""),
                            hullMod.getSpriteName(),
                            cost + " XP",
                            hullMod.getDescription(selectedVariant.getHullSize()),
                            hullMod.getEffect(),
                            selectedVariant.getHullSize(),
                            cost,
                            isEnhanceOnly
                    ));
        }

        final BuildInSelector plugin = new BuildInSelector();
        dialog.showCustomDialog(ManageSMods.getWidth(), ManageSMods.getHeight(),
            new ManageSMods() {
                @Override
                public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
                    PanelCreator.createTitle(panel, titleString, titleHeight);
                    PanelCreatorData<List<HullModButton>> createdButtonsData =
                        PanelCreator.createHullModButtonList(panel, buttonData, 45f, 10f, titleHeight, false);
                    LabelWithVariables<Integer> countLabel =
                        PanelCreator.createLabelWithVariables(panel, "Selected: %s/%s", Color.WHITE, 30f, Alignment.LMID, 0, SModUtils.getMaxSMods(fleetMember) - selectedVariant.getSMods().size()).created;
                    LabelWithVariables<Integer> xpLabel =
                        PanelCreator.createLabelWithVariables(panel, "XP: %s", Color.WHITE, 30f, Alignment.RMID, (int) SModUtils.getXP(fleetMember.getId())).created;
                    Button showAllButton = PanelCreator.createButton(panel, "Show recent", 100f, 25f, 10f, panel.getPosition().getHeight() + 6f).created;
                plugin.init(this, createdButtonsData, xpLabel, countLabel, showAllButton, fleetMember, selectedVariant, firstIndexToBeCounted, new PSM_BuildInHullModNew.SelectorContainer());
                }

                @Override
                public void customDialogCancel() {}

                @Override
                public void customDialogConfirm() {
                    boolean addedAtLeastOne = false;
                    String fmId = fleetMember.getId();

                    List<String> addToRecent = new ArrayList<>();
                    for (HullModButton button: plugin.getSelected()) {
                        if (SModUtils.spendXP(fmId, button.data.cost)) {
                            String hullModName = Global.getSettings().getHullModSpec(button.data.id).getDisplayName();

                            if (button.data.isEnhanceOnly) {
                                selectedVariant.getSModdedBuiltIns().add(button.data.id);
                                dialog.getTextPanel().addPara("Enhanced " + hullModName)
                                        .setHighlight(hullModName);
                            }
                            else {
                                selectedVariant.addPermaMod(button.data.id, true);
                                dialog.getTextPanel().addPara("Built in " + hullModName)
                                        .setHighlight(hullModName);
                                addToRecent.add(button.data.id);

                            }
                            addedAtLeastOne = true;
                        }
                    }

                    // Add in reverse order
                    for (int i = addToRecent.size() - 1; i >= 0 ; i--) {
                        RecentBuildInTracker.addToRecentlyBuiltIn(addToRecent.get(i));
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

                public void showRecentPressed(CustomPanelAPI panel, TooltipMakerAPI tooltipMaker) {
                    List<HullModButtonData> newButtonData = generateNewButtonData(selectedVariant, fleetMember);
                    PanelCreatorData<List<HullModButton>> newButtons =
                            PanelCreator.addToHullModButtonList(panel, tooltipMaker, newButtonData, 45f, 10f, titleHeight, false);
                    tooltipMaker.getExternalScroller().setYOffset(
                            Math.max(0f, tooltipMaker.getHeightSoFar() - panel.getPosition().getHeight()));
                    plugin.addNewItems(newButtons.created);
                }
            }
        );
        return true;
    }

    static List<HullModButtonData> generateNewButtonData(ShipVariantAPI selectedVariant, FleetMemberAPI fleetMember) {
        List<HullModButtonData> newButtonData = new ArrayList<>();
        List<String> recentlyBuiltIn = new ArrayList<>(RecentBuildInTracker.getRecentlyBuiltIn());
        Collections.sort(recentlyBuiltIn, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                HullModSpecAPI spec1 = Global.getSettings().getHullModSpec(s1);
                String name1 = spec1 == null ? "zzzunknown" : spec1.getDisplayName();
                HullModSpecAPI spec2 = Global.getSettings().getHullModSpec(s2);
                String name2 = spec2 == null ? "zzzunknown" : spec2.getDisplayName();
                return name1.compareTo(name2);
            }
        });

        for (String id : recentlyBuiltIn) {
            if (selectedVariant.hasHullMod(id)) {
                continue;
            }
            HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(id);
            int cost = SModUtils.getBuildInCost(hullMod, selectedVariant.getHullSize(), fleetMember.getUnmodifiedDeploymentPointsCost());
            newButtonData.add(
                    new HullModButtonData(
                            hullMod.getId(),
                            hullMod.getDisplayName(),
                            hullMod.getSpriteName(),
                            cost + " XP",
                            hullMod.getDescription(selectedVariant.getHullSize()),
                            hullMod.getEffect(),
                            selectedVariant.getHullSize(),
                            cost,
                            false)
            );
        }
        return newButtonData;
    }
}
