package data.campaign.rulecmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.util.Misc.Token;

import data.campaign.rulecmd.util.ProgSModGenericSelector;
import data.campaign.rulecmd.util.ProgSModSelectOnePlugin;
import util.SModUtils;

/** ProgSModSelectModule [fleetMember] [selectedVariant] [trigger] --
 *  opens up the module selection screen for [fleetMember],
 *  [variant] is the currently selected variant.
 *  then fires [trigger] once an option is selected. */
public class ProgSModSelectModule extends BaseCommandPlugin {

    private int currentVariantIndex;

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.size() < 3) {
            return false;
        }

        final FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).string);
        ShipVariantAPI selectedVariant = (ShipVariantAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(1).string);
        final List<ShipVariantAPI> modulesWithOP = SModUtils.getModuleVariantsWithOP(fleetMember.getVariant());
       
        if (modulesWithOP == null) {
            return false;
        }

        final List<String> moduleNameStrings = new ArrayList<>();
        moduleNameStrings.add("Base ship");
        if (fleetMember.getVariant().equals(selectedVariant)) {
            currentVariantIndex = 0;
        }

        for (ShipVariantAPI moduleVariant : modulesWithOP) {
            moduleNameStrings.add("Module: " + moduleVariant.getHullSpec().getHullName());
        }

        final ProgSModSelectOnePlugin plugin = new ProgSModSelectOnePlugin();
        dialog.showCustomDialog(500f, 500f, 
            new CustomDialogDelegate() {
                @Override
                public void createCustomDialog(CustomPanelAPI panel) {
                    List<ButtonAPI> buttons = 
                        ProgSModGenericSelector.createSelector(
                            panel,
                            "Select a module",
                            25f,
                            moduleNameStrings,
                            50f,
                            10f
                        );
                    plugin.setData(buttons);
                    plugin.disableButton(currentVariantIndex);
                }

                @Override
                public void customDialogCancel() {}

                @Override
                public void customDialogConfirm() {
                    int index = plugin.getCheckedIndex();
                    if (index == -1) {
                        return;
                    }
                    ShipVariantAPI variant = fleetMember.getVariant();
                    if (index > 0 && modulesWithOP.size() >= index) {
                        variant = modulesWithOP.get(index - 1);
                    }
                    memoryMap.get(MemKeys.LOCAL).set(params.get(1).string, variant, 0f);
                    FireAll.fire(ruleId, dialog, memoryMap, params.get(2).getString(memoryMap));
                }

                @Override
                public String getCancelText() {
                    return "Cancel";
                }

                @Override
                public String getConfirmText() {
                    return null;
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
