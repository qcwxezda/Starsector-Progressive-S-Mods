package data.campaign.rulecmd.util;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import util.SModUtils;

import java.awt.Color;

public class ProgSModSelectPanelCreator {

    static final float BUTTON_HEIGHT = 50f;
    static final float TITLE_HEIGHT = 50f;
    static final float BUTTON_PADDING = 10f;
    static final float TRACKER_HEIGHT = 20f;
    static final String NMODS_PREFIX = "Selected: ";
    static final String XP_PREFIX = "XP: ";
    static final Color RED = Global.getSettings().getColor("progressBarDangerColor");
    static final Color YELLOW = Global.getSettings().getColor("progressBarWarningColor");

    private static final Color BASE_COLOR, DARK_COLOR, BRIGHT_COLOR;

    static {
        FactionAPI playerFaction = Global.getSector().getPlayerFaction();
        BASE_COLOR = playerFaction.getBaseUIColor();
        DARK_COLOR = playerFaction.getDarkUIColor();
        BRIGHT_COLOR = playerFaction.getBrightUIColor();
    }

    /** A container for the button, text, etc. in a 
     * hull mod entry that needs to be modifiable. */
    public static class SelectorData {
        public ButtonAPI button;
        public LabelAPI nameLabel;
        public LabelAPI costLabel;
        public int hullModCost;
        public String hullModId;

        public SelectorData(ButtonAPI button, LabelAPI nameLabel, LabelAPI costLabel, int hullModCost, String hullModId) {
            this.button = button;
            this.nameLabel = nameLabel;
            this.costLabel = costLabel;
            this.hullModCost = hullModCost;
            this.hullModId = hullModId;
        }
    }

    /** Adds an area checkbox on top of an image-with-text. 
     *  Returns the button, title text, and cost info text. */
    private static SelectorData addHullModSelector(
                TooltipMakerAPI tooltip, 
                HullModSpecAPI hullMod,
                HullSize hullSize,
                float deploymentCost, 
                float width, 
                float height, 
                float pad,
                boolean removeMode) {
        ButtonAPI button = 
            tooltip.addAreaCheckbox(
                "", 
                "BUTTON_" + hullMod.getId(), 
                removeMode ? Misc.getStoryOptionColor() : BASE_COLOR, 
                removeMode ? Misc.getStoryDarkColor() : DARK_COLOR,
                BRIGHT_COLOR, 
                width, 
                height, 
                pad
            );
        TooltipMakerAPI imageAndText = tooltip.beginImageWithText(hullMod.getSpriteName(), height);
        imageAndText.setTitleOrbitronLarge();
        LabelAPI nameText = imageAndText.addTitle(hullMod.getDisplayName());
        nameText.setHighlight(hullMod.getDisplayName());
        nameText.setHighlightColor(removeMode ? Misc.getStoryOptionColor() : Color.WHITE);
        imageAndText.setParaFontOrbitron();
        int hullModCost = 0;
        LabelAPI costTextLabel = null;
        hullModCost = SModUtils.getBuildInCost(hullMod, hullSize, deploymentCost);
        String costText = 
            removeMode 
                ? String.format("refunds %s XP", (int) (hullModCost * SModUtils.Constants.XP_REFUND_FACTOR)) 
                : hullModCost + " XP";
        costTextLabel = imageAndText.addPara(costText, 0f);
        costTextLabel.setHighlight(costText);
        costTextLabel.setHighlightColor(Color.WHITE);
        tooltip.addImageWithText(-height);
        return new SelectorData(button, nameText, costTextLabel, hullModCost, hullMod.getId());
    }

    /** Update the number of selected hull mods text to show the following:
     *  "Selected: [left]/[right]".
     *  Modifies and returns [textLabel]. */
    public static LabelAPI setNSelectedModsText(LabelAPI textLabel, int left, int right) {
        String newText = String.format("%s%s/%s", NMODS_PREFIX, left, right);
        textLabel.setText(newText);
        textLabel.setHighlight(NMODS_PREFIX.length(), NMODS_PREFIX.length() + String.valueOf(left).length() - 1);
        textLabel.setHighlightColor(left < right ? Color.WHITE : YELLOW);
        return textLabel;
    }

    /** Update the remaining XP label to show the following:
     *  "XP remaining: [xp]"
     *  Modifies and returns [textLabel]. */
    public static LabelAPI setRemainingXPText(LabelAPI textLabel, float xp) {
        String newText = XP_PREFIX + (int) xp;
        textLabel.setText(newText);
        textLabel.setHighlight(XP_PREFIX.length(), newText.length() - 1);
        return textLabel;
    }

    /** Disable a hull mod entry by disabling its button, 
     * darkening the font, and setting the XP color to red. */
    public static void disableEntryRed(SelectorData entry) {
        entry.button.setEnabled(false);
        entry.costLabel.setHighlightColor(RED);
        entry.nameLabel.setHighlightColor(Color.GRAY);
    }

    /** Disable a hull mod entry by disabling its button, 
     * darkening the font, and setting the XP color to gray. */
    public static void disableEntryGray(SelectorData entry) {
        entry.button.setEnabled(false);
        entry.costLabel.setHighlightColor(Color.GRAY);
        entry.nameLabel.setHighlightColor(Color.GRAY);
    }

    /** Enable a hull mod entry by enabling its button,
     *  lightening the font, and setting the XP color to white. */
    public static void enableEntry(SelectorData entry) {
        entry.button.setEnabled(true);
        entry.costLabel.setHighlightColor(Color.WHITE);
        entry.nameLabel.setHighlightColor(Color.WHITE);
    }

    /** Adds the "Selected: x/y" and "XP: z" counters
     *  to the hull mod selection interface.
     *  Returns both LabelAPI objects. */
    public static Pair<LabelAPI, LabelAPI> addCountAndXPToPanel(CustomPanelAPI panel) {
        float width = panel.getPosition().getWidth();
        float trackerWidth = width * 0.85f;
        TooltipMakerAPI countTracker = panel.createUIElement(trackerWidth, TRACKER_HEIGHT, false);
        LabelAPI nSelectedText = setNSelectedModsText(countTracker.addTitle(""), 0, 0);
        nSelectedText.setAlignment(Alignment.LMID);
        LabelAPI remainingXPText = setRemainingXPText(countTracker.addTitle(""), 0);
        remainingXPText.setAlignment(Alignment.RMID);
        panel.addUIElement(countTracker).inTMid(30f);
        return new Pair<>(nSelectedText, remainingXPText);
    }

    /** Adds "XP: x" counter to the hull mod selection interface. */
    public static LabelAPI addXPToPanel(CustomPanelAPI panel) {
        float width = panel.getPosition().getWidth();
        float trackerWidth = width * 0.85f;
        TooltipMakerAPI xpTracker = panel.createUIElement(trackerWidth, TRACKER_HEIGHT, false);
        LabelAPI xpText = setRemainingXPText(xpTracker.addTitle(""), 0);
        xpText.setAlignment(Alignment.MID);
        panel.addUIElement(xpTracker).inTMid(30f);
        return xpText;
    }

    /** Creates the interface used for selecting hull mods, both when 
     *  building them in and removing them, just with minor differences.
     *  The panel will have title [titleString] and the XP costs, if applicable,
     *  will be based on [fleetMember]. All hull mods from [hullModList] are
     *  shown. */
    public static List<SelectorData> createHullModSelectionPanel(
            CustomPanelAPI panel, 
            String titleString, 
            List<HullModSpecAPI> hullModList,
            FleetMemberAPI fleetMember,
            boolean removeMode) {
        float width = panel.getPosition().getWidth();
        float height = panel.getPosition().getHeight();

        // TITLE
        TooltipMakerAPI title = panel.createUIElement(width - 10f, TITLE_HEIGHT, false);
        title.setTitleOrbitronLarge();
        title.addTitle(titleString).setAlignment(Alignment.MID);
        panel.addUIElement(title).inTMid(0f);

        // BUTTON LIST
        float buttonWidth = width * 0.95f;
        float buttonHeight = BUTTON_HEIGHT;
        float buttonPadding = BUTTON_PADDING;
        float buttonListHeight = height - TITLE_HEIGHT - BUTTON_PADDING;
        // There seems to be a base horizontal padding of 10f, 
        // need to account for this for true centering 
        float buttonListHorizontalPadding = 0.5f * (width - buttonWidth - 10f);

        List<SelectorData> selectorList = new ArrayList<>();
        TooltipMakerAPI buttonsList = panel.createUIElement(width, buttonListHeight, true);
        for (HullModSpecAPI hullMod : hullModList) {
            SelectorData entry = addHullModSelector(
                buttonsList, 
                hullMod, 
                fleetMember.getVariant().getHullSize(), 
                fleetMember.getDeploymentPointsCost(),
                buttonWidth, 
                buttonHeight, 
                buttonPadding,
                removeMode
            );
            selectorList.add(entry);
        }
        panel.addUIElement(buttonsList).belowLeft(title, buttonListHorizontalPadding);
        return selectorList;
    }
}
