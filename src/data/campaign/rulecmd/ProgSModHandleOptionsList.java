package data.campaign.rulecmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;

import util.SModUtils;

/** ProgSModFilterOptionsList [$fleetMember] [$selectedVariant] [option1] [option2] [option3] [option4]
 *  [option1] is the option to build in hull mods; 
 *  [option2] is the option to remove them;
 *  [option3] is the option to increase S-Mod limit. 
 *  [option4] is to go back to main menu. */
public class ProgSModHandleOptionsList extends BaseCommandPlugin implements InteractionDialogPlugin {

    private static final String MODULE_OPTION_PREFIX = "module_";
    private static final String MANAGE_MODULE_TEXT = "Manage built-in hull mods for ";
    private static final String BUILD_IN_TEXT = "Build up to %s hull mods into ";
    private static final String REMOVE_TEXT = "Select built-in hull mods to remove from ";
    private static final String MODULE_ID_BASE = "base";
    private static final String THIS_SHIP_TEXT = "this ship";
    private static final boolean CAN_REFUND_SMODS = SModUtils.Constants.XP_REFUND_FACTOR >= 0f;

    private Map<String, MemoryAPI> memoryMap;
    private InteractionDialogAPI dialog;
    private InteractionDialogPlugin originalPlugin;
    private String buildInOption, removeOption, goBackOption;
    private String currentModuleId = MODULE_ID_BASE;
    private List<String> moduleOptionIds = new ArrayList<>();
    private int nRemaining = 0;
    private int nSModsLimit = 0;
    private String selectedVariantKey;
    private FleetMemberAPI fleetMember;

	@Override
	public boolean execute(String ruleId, final InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null || params.isEmpty()) {
            return false;
        }

        this.memoryMap = memoryMap;
        this.dialog = dialog;
        // Adding or removing hull mods triggers this event again,
        // so make sure not to overwrite originalPlugin with
        // a copy of this plugin
        boolean firstTimeOpened;
        if (dialog.getPlugin() instanceof ProgSModHandleOptionsList) {
            ProgSModHandleOptionsList plugin = (ProgSModHandleOptionsList) dialog.getPlugin(); 
            originalPlugin = plugin.originalPlugin;
            currentModuleId = plugin.currentModuleId;
            firstTimeOpened = false;
        } else {
            originalPlugin = dialog.getPlugin();
            firstTimeOpened = true;
        }
        dialog.setPlugin(this);

        fleetMember = (FleetMemberAPI) memoryMap.get(MemKeys.LOCAL).get(params.get(0).string);
        int nSMods = fleetMember.getVariant().getSMods().size();
        nSModsLimit = SModUtils.getMaxSMods(fleetMember);
        nRemaining = nSModsLimit - nSMods;
        selectedVariantKey = params.get(1).string;
        buildInOption = params.get(2).getString(memoryMap);
        removeOption = params.get(3).getString(memoryMap);
        String augmentOption = params.get(4).getString(memoryMap);
        goBackOption = params.get(5).getString(memoryMap);
        float spRefundFraction = 0f;

        dialog.getOptionPanel().clearOptions();

        // The selected variant is the base ship, by default
        if (firstTimeOpened) {
            memoryMap.get(MemKeys.LOCAL).set(selectedVariantKey, fleetMember.getVariant(), 0f);
        }

        // Add build in option
        dialog.getOptionPanel().addOption(String.format(BUILD_IN_TEXT + THIS_SHIP_TEXT, nRemaining), buildInOption);

        // Add in the remove option if that was allowed
        if (CAN_REFUND_SMODS) {
            dialog.getOptionPanel().addOption(REMOVE_TEXT + THIS_SHIP_TEXT, removeOption);
        }

        // Add options to select ship modules
        List<String> moduleIds = fleetMember.getVariant().getModuleSlots();
        if (moduleIds != null) {
            for (String id : moduleIds) { 
                dialog.getOptionPanel().addOption(MANAGE_MODULE_TEXT + "module: " + id, MODULE_OPTION_PREFIX + id);
                moduleOptionIds.add(id);
            }
        }

        if (currentModuleId.equals(MODULE_ID_BASE)) {
            updateOptions(fleetMember.getVariant());
        } else {
            updateOptions(fleetMember.getVariant().getModuleVariant(currentModuleId));
        }

        // Add in the +extra S-Mods option if that was allowed
        if (SModUtils.Constants.ALLOW_INCREASE_SMOD_LIMIT) {
            dialog.getOptionPanel().addOption(
                    String.format("Increase this ship's built-in hull mod limit from %s to %s", nSModsLimit, nSModsLimit + 1), 
                augmentOption);
            int nextSPCost = SModUtils.getStoryPointCost(fleetMember);
            int nextXPCost = SModUtils.getAugmentXPCost(fleetMember);
            List<Token> storyParams = new ArrayList<>();
            storyParams.add(params.get(0));
            storyParams.add(params.get(4));
            storyParams.add(new Token("" + nextSPCost, TokenType.LITERAL));
            storyParams.add(new Token("" + spRefundFraction, TokenType.LITERAL));
            storyParams.add(new Token("" + nextXPCost, TokenType.LITERAL));
            new ProgSModSetStoryOption().execute(ruleId, dialog, storyParams, memoryMap);
        }

        // Add in the back to main menu option 
        dialog.getOptionPanel().addOption("Go back", goBackOption);
        dialog.getOptionPanel().setShortcut(goBackOption, Global.getSettings().getCodeFor("ESCAPE"), false, false, false, true);

        // Add in the text panel
        if (firstTimeOpened) {
            dialog.getTextPanel()
                .addPara(String.format("The %s has %s out of %s built-in hull mods.", fleetMember.getShipName(), nSMods, nSModsLimit))
                .setHighlight(fleetMember.getShipName(), "" + nSMods, "" + nSModsLimit);
            
            if (CAN_REFUND_SMODS) {
                int refundPercent = (int) (SModUtils.Constants.XP_REFUND_FACTOR * 100);
                dialog.getTextPanel()
                    .addPara(String.format("Removing an existing built-in hull mod will refund %s%% of the XP spent.",refundPercent))
                    .setHighlight("" + refundPercent);
            }
            if (SModUtils.Constants.ALLOW_INCREASE_SMOD_LIMIT) {
                int numOverLimit = SModUtils.getNumOverLimit(fleetMember.getId());
                dialog.getTextPanel()
                    .addPara(
                        String.format("You have increased this ship's limit of built-in hull mods a total of %s time%s.",
                            numOverLimit,
                            numOverLimit == 1 ? "" : "s")
                        )
                    .setHighlight("" + numOverLimit);
            }
            SModUtils.displayXP(dialog, fleetMember);
        }

		return true;
	}

    @Override
    public void advance(float amount) {}

    @Override
    public void backFromEngagement(EngagementResultAPI battle) {}

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {}

    @Override
    public void optionMousedOver(String optionText, Object optionId) {}

    @Override
    public void optionSelected(String optionText, Object optionId) {
        String optId = (String) optionId;
        if (optId.startsWith(MODULE_OPTION_PREFIX)) {
            String selectedId = optId.substring(MODULE_OPTION_PREFIX.length());
            // Selected the same option again -- go back to the base ship
            if (selectedId.equals(currentModuleId)) {
                currentModuleId = MODULE_ID_BASE;
                memoryMap.get(MemKeys.LOCAL).set(selectedVariantKey, fleetMember.getVariant(), 0f);
                updateOptions(fleetMember.getVariant());
            }
            // Set the module to the selected one 
            else {
                currentModuleId = selectedId;
                memoryMap.get(MemKeys.LOCAL).set(selectedVariantKey, fleetMember.getVariant().getModuleVariant(selectedId), 0f);
                updateOptions(fleetMember.getVariant().getModuleVariant(currentModuleId));
            }
        }
        else {
            // For some reason optionText always refers to "this ship"
            // even after changing the option's text.
            // For this reason just don't print anything
            // out upon selection.
            if (optId.equals(removeOption) || optId.equals(buildInOption)) {
                optionText = null;
            }
            originalPlugin.optionSelected(optionText, optionId);
        }

        // Reset the dialog plugin if we go back to main menu
        if (optId.equals(goBackOption)) {
            dialog.setPlugin(originalPlugin);
        }
    }

    /** Disables or enables the build in and remove options
     *  based on the S-Mod stats of [variant]. */
    private void updateOptions(ShipVariantAPI variant){ 
        // If the ship does not have any S-Mods to remove, disable the remove option
        if (CAN_REFUND_SMODS) {
            dialog.getOptionPanel().setEnabled(removeOption, !variant.getSMods().isEmpty());
        }

        // Update the module info
        for (String id : moduleOptionIds) {
            if (id.equals(currentModuleId)) {
                // For the currently selected module, set the build in and remove text
                // to reflect this module. Set the module selection text to the base ship.
                dialog.getOptionPanel().setOptionText(MANAGE_MODULE_TEXT + THIS_SHIP_TEXT, MODULE_OPTION_PREFIX + currentModuleId);
                dialog.getOptionPanel().setOptionText(
                    String.format(BUILD_IN_TEXT + "module: %s", nSModsLimit - variant.getSMods().size(), id), 
                    buildInOption);
                if (CAN_REFUND_SMODS) {
                    dialog.getOptionPanel().setOptionText(REMOVE_TEXT + "module: " + id, removeOption);
                }
            }
            else {
                // Selector for a different module
                dialog.getOptionPanel().setOptionText(MANAGE_MODULE_TEXT + "module: " + id, MODULE_OPTION_PREFIX + id);
            }
        }
        // If selected base ship, change the options to show "this ship" instead of a module
        if (currentModuleId.equals(MODULE_ID_BASE)) {
            dialog.getOptionPanel().setOptionText(String.format(BUILD_IN_TEXT + THIS_SHIP_TEXT, nRemaining), buildInOption);
            if (CAN_REFUND_SMODS) {
                dialog.getOptionPanel().setOptionText(REMOVE_TEXT + THIS_SHIP_TEXT, removeOption);
            }
        }
    }
}