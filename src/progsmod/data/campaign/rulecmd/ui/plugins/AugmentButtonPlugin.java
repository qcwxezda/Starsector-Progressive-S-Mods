package progsmod.data.campaign.rulecmd.ui.plugins;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.*;
import com.fs.starfarer.api.input.*;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.*;
import progsmod.data.campaign.rulecmd.PSM_BuildInHullModNew.*;
import progsmod.data.campaign.rulecmd.ui.PanelCreator;
import progsmod.data.campaign.rulecmd.util.XPHelper;
import util.*;

import java.util.*;

public class AugmentButtonPlugin implements CustomUIPanelPlugin, Updatable {
    private int xpCost;
    private int spCost;
    private FleetMemberAPI ship;
    private CustomPanelAPI parentPanel;
    private SelectorContainer container;
    public CustomPanelAPI augmentPanel;

    public ButtonAPI button;

    private LabelAPI spInfoLabel;

    private String spInfoText() {
        return "You have " + Global.getSector().getPlayerStats().getStoryPoints() + " story points available.";
    }

    private String buttonText() {
        return String.format("Increase this ship's S-mod limit from %s to %s  [%s SP; %s ship XP]",
                SModUtils.getMaxSMods(ship),
                SModUtils.getMaxSMods(ship) + 1,
                spCost,
                xpCost
        );
    }

    public AugmentButtonPlugin(CustomPanelAPI parentPanel, FleetMemberAPI ship, SelectorContainer container) {
        this.parentPanel = parentPanel;
        this.ship = ship;
        this.container = container;
        container.register(this);
        this.xpCost = SModUtils.getAugmentXPCost(ship);
        this.spCost = SModUtils.getAugmentSPCost(ship);

        final float width = parentPanel.getPosition().getWidth();
        float textHeight = 20f;
        float buttonHeight = 30f;
        float buttonWidth = width * 0.95f;
        float buttonListHorizontalPadding = 0.5f * (width - buttonWidth - 10f);
        float height = textHeight + buttonHeight;

        augmentPanel = parentPanel.createCustomPanel(width, height, this);

        TooltipMakerAPI augmentElement = augmentPanel.createUIElement(width, height, false);
        spInfoLabel = augmentElement.addPara(spInfoText(), 0f, Misc.getStoryOptionColor());

        String buttonKey = "increase_limit_button";
        String buttonText = buttonText();
        button = augmentElement.addButton(buttonText, buttonKey,
            Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL,
            buttonWidth, buttonHeight, 5f);

        String infoString = "Increasing a ship's S-mod limit with story points grants no bonus experience";
        if (SModUtils.Constants.DEPLOYMENT_COST_PENALTY >= 0f) {
            infoString += " and increases its deployment cost by " + (int) (SModUtils.Constants.DEPLOYMENT_COST_PENALTY*100)
                + "% for each additional S-mod.";
        } else {
            infoString += ".";
        }
        final String finalInfoString = infoString;
        augmentElement.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return width;
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                tooltip.addPara(finalInfoString, 0f);
            }
        }, TooltipMakerAPI.TooltipLocation.ABOVE);

        button.setButtonPressedSound("ui_char_spent_story_point_combat");

        augmentPanel.addUIElement(augmentElement).inTL(buttonListHorizontalPadding, 0);
        parentPanel.addComponent(augmentPanel);
    }

    public boolean buttonEnabled() {
        return (spCost < Global.getSector().getPlayerStats().getStoryPoints()) &&
               (container.xpHelper.canAfford(xpCost) != XPHelper.Affordable.NO);
    }

    public void update() {
        button.setEnabled(buttonEnabled());
        spInfoLabel.setText(spInfoText());
        button.setText(buttonText());
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (buttonEnabled()) {
            Global.getSector().getPlayerStats().spendStoryPoints(spCost, true,
                    Global.getSector().getCampaignUI().getCurrentInteractionDialog().getTextPanel(), true, 0f, "log text");
            // xpLabel is the "tentative" value shared across all three separate panels (augment button,
            //   remove section and buildin section)
            container.xpHelper.decreaseXPLabel(xpCost);
            // Use reserve XP if required
            container.xpHelper.useReserveXPIfNeeded(ship);
            SModUtils.incrementSModLimit(ship);
            container.countLabel.changeVar(1, SModUtils.getMaxSMods(ship));
            xpCost = SModUtils.getAugmentXPCost(ship);
            spCost = SModUtils.getAugmentSPCost(ship);
            container.updateAll();
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {

    }


    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }
}
