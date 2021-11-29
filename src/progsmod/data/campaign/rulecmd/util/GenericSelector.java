package progsmod.data.campaign.rulecmd.util;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class GenericSelector {
    public static List<ButtonAPI> createSelector(
            CustomPanelAPI panel,
            String titleString, 
            float titleHeight,
            List<String> buttonText,
            float buttonHeight,
            float buttonPadding) {
        float width = panel.getPosition().getWidth();
        float height = panel.getPosition().getHeight();
        float buttonWidth = width * 0.95f;
        // There seems to be a base horizontal padding of 10f, 
        // need to account for this for true centering 
        float buttonListHorizontalPadding = 0.5f * (width - buttonWidth - 10f);
        // TITLE
        TooltipMakerAPI title = panel.createUIElement(width - 10f, titleHeight, false);
        title.setTitleOrbitronLarge();
        title.addTitle(titleString).setAlignment(Alignment.MID);
        panel.addUIElement(title).inTMid(0f);
        // BUTTON LIST
        float buttonListHeight = height - titleHeight - buttonPadding;
        List<ButtonAPI> buttons = new ArrayList<>();
        TooltipMakerAPI buttonsElement = panel.createUIElement(width, buttonListHeight, true);
        for (String text : buttonText) { 
            buttons.add(
                buttonsElement.addAreaCheckbox(
                    text, 
                    "temp",
                    Misc.getBasePlayerColor(), 
                    Misc.getDarkPlayerColor(), 
                    Misc.getBrightPlayerColor(),
                    buttonWidth,
                    buttonHeight,
                    buttonPadding
                )
            );
        }
        panel.addUIElement(buttonsElement).belowLeft(title, buttonListHorizontalPadding);
        return buttons;
    }
}
