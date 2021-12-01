package progsmod.data.campaign.rulecmd.ui;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import progsmod.data.campaign.rulecmd.util.HullModButtonData;

import java.awt.Color;

public class PanelCreator {

    private static float width, height, buttonWidth, buttonListHorizontalPadding, buttonListHeight;

    public static class PanelCreatorData<T> {
        // The panel that was acted on
        public CustomPanelAPI panel;
        // The TooltipMakerAPI that was used to create the element
        public TooltipMakerAPI tooltipMaker;
        // Whatever [tooltipMaker] created
        public T created;

        private PanelCreatorData(CustomPanelAPI panel, TooltipMakerAPI tooltipMaker, T created) {
            this.panel = panel;
            this.tooltipMaker = tooltipMaker;
            this.created = created;
        }
    }

    public static PanelCreatorData<Title> createTitle(CustomPanelAPI panel, String titleText, float titleHeight) {
        float width = panel.getPosition().getWidth();
        TooltipMakerAPI titleElement = panel.createUIElement(width - 10f, titleHeight, false);
        Title title = new Title(titleText);
        title.init(titleElement);
        panel.addUIElement(titleElement).inTMid(0f);
        return new PanelCreatorData<>(panel, titleElement, title);
    }

    public static PanelCreatorData<List<Button>> createButtonList(CustomPanelAPI panel, List<String> buttonText, float buttonHeight, float buttonPadding, float distanceFromTop) {
        initVars(panel, distanceFromTop);
        List<Button> buttons = new ArrayList<>();
        TooltipMakerAPI buttonsElement = panel.createUIElement(width, buttonListHeight, true);
        for (String text : buttonText) { 
            Button button = 
                new Button(
                    text, 
                    Misc.getBasePlayerColor(), 
                    Misc.getDarkPlayerColor(), 
                    Misc.getBrightPlayerColor(), 
                    buttonWidth, 
                    buttonHeight, 
                    buttonPadding);
            button.init(buttonsElement);
            buttons.add(button);
        }
        panel.addUIElement(buttonsElement).inTL(buttonListHorizontalPadding, distanceFromTop);
        return new PanelCreatorData<List<Button>>(panel, buttonsElement, buttons);
    }

    public static PanelCreatorData<Button> createButton(CustomPanelAPI panel, String text, float width, float height, float distanceFromLeft, float distanceFromTop) {
        TooltipMakerAPI buttonElement = panel.createUIElement(width, height, false);
        Button button = 
            new Button(
                text, 
                Misc.getBasePlayerColor(), 
                Misc.getBasePlayerColor(), 
                Misc.getBrightPlayerColor(), 
                width, 
                height, 
                0f
            );
        button.init(buttonElement);
        panel.addUIElement(buttonElement).inTL(distanceFromLeft, distanceFromTop);
        return new PanelCreatorData<>(panel, buttonElement, button);
    }

    @SafeVarargs
    public static <T> PanelCreatorData<LabelWithVariables<T>> createLabelWithVariables(CustomPanelAPI panel, String text, Color highlightColor, float pad, Alignment align, T... vars) {
        float width = panel.getPosition().getWidth();
        float labelWidth = width * 0.85f;
        TooltipMakerAPI labelElement = panel.createUIElement(labelWidth, 20f, false);
        labelElement.setParaFontOrbitron();
        LabelWithVariables<T> label = new LabelWithVariables<>(text, highlightColor, align, vars);
        label.init(labelElement);
        panel.addUIElement(labelElement).inTMid(pad);
        return new PanelCreatorData<>(panel, labelElement, label);
    }

    public static PanelCreatorData<List<HullModButton>> createHullModButtonList(
            CustomPanelAPI panel, 
            List<HullModButtonData> buttonData,
            float buttonHeight, 
            float buttonPadding, 
            float distanceFromTop,
            boolean useStoryColor) {
        initVars(panel, distanceFromTop);
        TooltipMakerAPI buttonsElement = panel.createUIElement(width, buttonListHeight, true);
        return addToHullModButtonList(
            panel, 
            buttonsElement, 
            buttonData,
            buttonHeight, 
            buttonPadding, 
            distanceFromTop, 
            useStoryColor);
    }

    /** Returns only the buttons that were added */
    public static PanelCreatorData<List<HullModButton>> addToHullModButtonList(
            CustomPanelAPI panel,
            TooltipMakerAPI tooltipMaker,
            List<HullModButtonData> buttonData,
            float buttonHeight, 
            float buttonPadding, 
            float distanceFromTop,
            boolean useStoryColor) {
        initVars(panel, distanceFromTop);
        List<HullModButton> buttons = new ArrayList<>();
        for (int i = 0; i < buttonData.size(); i++) {
            HullModButton button =
                new HullModButton(
                    buttonData.get(i),
                    useStoryColor ? Misc.getStoryOptionColor() : Misc.getBasePlayerColor(), 
                    useStoryColor ? Misc.getStoryDarkColor() : Misc.getDarkPlayerColor(), 
                    Misc.getBrightPlayerColor(), 
                    useStoryColor ? Misc.getStoryOptionColor() : Color.WHITE, 
                    buttonWidth, 
                    buttonHeight, 
                    buttonPadding 
                );
            button.init(tooltipMaker);
            buttons.add(button);
        }
        panel.addUIElement(tooltipMaker).inTL(buttonListHorizontalPadding, distanceFromTop);
        return new PanelCreatorData<>(panel, tooltipMaker, buttons);
    }

    private static void initVars(CustomPanelAPI panel, float distanceFromTop) {
        width = panel.getPosition().getWidth();
        height = panel.getPosition().getHeight();
        buttonWidth = width * 0.95f;
        // There seems to be a base horizontal padding of 10f, 
        // need to account for this for true centering 
        buttonListHorizontalPadding = 0.5f * (width - buttonWidth - 10f);
        buttonListHeight = height - distanceFromTop /* - buttonPadding */;
    }
}
