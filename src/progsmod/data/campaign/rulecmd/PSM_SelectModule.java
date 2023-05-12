package progsmod.data.campaign.rulecmd;

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
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.util.Misc.Token;

import progsmod.data.campaign.rulecmd.ui.Button;
import progsmod.data.campaign.rulecmd.ui.PanelCreator;
import progsmod.data.campaign.rulecmd.ui.plugins.SelectOne;
import util.SModUtils;

/** ProgSModSelectModule [fleetMember] [selectedVariant] [trigger] --
 *  opens up the module selection screen for [fleetMember],
 *  [variant] is the currently selected variant.
 *  then fires [trigger] once an option is selected. */
@SuppressWarnings("unused")
public class PSM_SelectModule extends BaseCommandPlugin {

    private int currentVariantIndex;

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.size() < 3) {
            return false;
        }

        final FleetMemberAPI fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).string);
        ShipVariantAPI selectedVariant = (ShipVariantAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(1).string);
        final List<ShipVariantAPI> modulesWithOP = SModUtils.getModuleVariantsWithOP(fleetMember.getVariant());
       
        if (modulesWithOP.isEmpty()) {
            return false;
        }

        final List<String> moduleNameStrings = new ArrayList<>();
        moduleNameStrings.add("Base ship");
        if (fleetMember.getVariant() == selectedVariant) {
            currentVariantIndex = 0;
        }

        for (int i = 0; i < modulesWithOP.size(); i++) {
            ShipVariantAPI moduleVariant = modulesWithOP.get(i);
            moduleNameStrings.add("Module: " + moduleVariant.getHullSpec().getHullName());
            if (selectedVariant == moduleVariant) {
                currentVariantIndex = i + 1;
            }
        }

        final SelectOne<Button> plugin = new SelectOne<>();
        dialog.showCustomDialog(500f, 500f, 
            new CustomDialogDelegate() {
                @Override
                public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
                    float titleHeight = 25f;
                    PanelCreator.createTitle(panel, "Select a module", titleHeight);
                    plugin.init(PanelCreator.createButtonList(panel, moduleNameStrings, 45f, 10f, titleHeight).created);
                    plugin.disableItem(currentVariantIndex);
                }

                @Override
                public void customDialogCancel() {}

                @Override
                public void customDialogConfirm() {
                    int index = plugin.getSelectedIndex();
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
