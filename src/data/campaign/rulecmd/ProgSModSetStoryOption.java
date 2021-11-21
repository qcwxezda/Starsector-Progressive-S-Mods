package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI.OptionTooltipCreator;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;


/** ProgSModSetStoryOption [n] [m] makes a story option costing n story points and giving m% bonus XP.
 *  Uses default sound. Modified from SetStoryOption in the base game. */
public class ProgSModSetStoryOption extends SetStoryOption {

    private class ProgSModStoryPointActionDelegate extends BaseOptionStoryPointActionDelegate {
        ProgSModStoryPointActionDelegate(InteractionDialogAPI dialog, String optionId, int cost, int bonusPercent) {
            super(dialog, new StoryOptionParams(optionId, cost, "", "general", null));
            bonusXPFraction = (float) bonusPercent / 100f;
        }

        String getOptionId() {
            return (String) optionId;
        }

        int getCost() {
            return numPoints;
        }

        String getSoundId() {
            return soundId;
        }
    }

    @Override
    public boolean execute(String ruleId, final InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        String optionId = params.get(0).string;
        int numPoints = params.get(1).getInt(memoryMap);
        int bonusXPPercent = params.get(2).getInt(memoryMap);

        return set(dialog, numPoints, optionId, bonusXPPercent);
    }

    private boolean set(InteractionDialogAPI dialog, int numPoints, String optionId, int bonusXPPercent) {
            return set(dialog, new ProgSModStoryPointActionDelegate(dialog, optionId, numPoints, bonusXPPercent));
    }

    private ProgSModStoryPointActionDelegate delegate = null;

    private boolean set(InteractionDialogAPI dialog,
            ProgSModStoryPointActionDelegate del) {
        delegate = del;
        int cost = delegate.getCost();
        String optionId = delegate.getOptionId();
        float bonusXPFraction = delegate.getBonusXPFraction();
        dialog.makeStoryOption(optionId, cost, bonusXPFraction, delegate.getSoundId());
        
        if (cost > Global.getSector().getPlayerStats().getStoryPoints()) {
            dialog.getOptionPanel().setEnabled(optionId, false);
        }
        
        dialog.getOptionPanel().addOptionTooltipAppender(optionId, new OptionTooltipCreator() {
            public void createTooltip(TooltipMakerAPI tooltip, boolean hadOtherText) {
                float opad = 10f;
                float initPad = 0f;
                if (hadOtherText) initPad = opad;
                tooltip.addStoryPointUseInfo(initPad, delegate.getCost(), delegate.getBonusXPFraction(), true);
                int sp = Global.getSector().getPlayerStats().getStoryPoints();
                String points = "points";
                if (sp == 1) points = "point";
                tooltip.addPara("You have %s " + Misc.STORY + " " + points + ".", opad, 
                        Misc.getStoryOptionColor(), "" + sp);
            }
        });
        
        dialog.getOptionPanel().addOptionConfirmation(optionId, delegate);
        
        return true;
    }
}