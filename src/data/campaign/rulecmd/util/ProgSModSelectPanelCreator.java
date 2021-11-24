package data.campaign.rulecmd.util;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
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

    private TooltipMakerAPI buttonsList, title;
    private final CustomPanelAPI panel;
    private float width, height;
    private float buttonWidth, buttonListHorizontalPadding;
    private boolean removeMode;

    public ProgSModSelectPanelCreator(CustomPanelAPI panel, boolean removeMode) {
        this.panel = panel;
        this.removeMode = removeMode;

        width = panel.getPosition().getWidth();
        height = panel.getPosition().getHeight();
        buttonWidth = width * 0.95f;
        // There seems to be a base horizontal padding of 10f, 
        // need to account for this for true centering 
        buttonListHorizontalPadding = 0.5f * (width - buttonWidth - 10f);
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
    private SelectorData addHullModSelector(
                TooltipMakerAPI tooltip, 
                HullModSpecAPI hullMod,
                int hullModCost, 
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
        LabelAPI costTextLabel = null;
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
    public LabelAPI setNSelectedModsText(LabelAPI textLabel, int left, int right) {
        String newText = String.format("%s%s/%s", NMODS_PREFIX, left, right);
        textLabel.setText(newText);
        textLabel.setHighlight(NMODS_PREFIX.length(), NMODS_PREFIX.length() + String.valueOf(left).length() - 1);
        textLabel.setHighlightColor(left < right ? Color.WHITE : YELLOW);
        return textLabel;
    }

    /** Update the remaining XP label to show the following:
     *  "XP remaining: [xp]"
     *  Modifies and returns [textLabel]. */
    public LabelAPI setRemainingXPText(LabelAPI textLabel, float xp) {
        String newText = XP_PREFIX + (int) xp;
        textLabel.setText(newText);
        textLabel.setHighlight(XP_PREFIX.length(), newText.length() - 1);
        return textLabel;
    }

    /** Disable a hull mod entry by disabling its button, 
     * darkening the font, and setting the XP color to red. */
    public void disableEntryRed(SelectorData entry) {
        entry.button.setEnabled(false);
        entry.costLabel.setHighlightColor(RED);
        entry.nameLabel.setHighlightColor(Color.GRAY);
    }

    /** Disable a hull mod entry by disabling its button, 
     * darkening the font, and setting the XP color to red. 
     * Also changes the cost text to show [newText]. */
    public void disableRedAndChangeText(SelectorData entry, String newText) {
        entry.costLabel.setText(newText);
        entry.costLabel.setHighlight(newText);
        disableEntryRed(entry);
    }

    /** Disable a hull mod entry by disabling its button, 
     * darkening the font, and setting the XP color to gray. */
    public void disableEntryGray(SelectorData entry) {
        entry.button.setEnabled(false);
        entry.costLabel.setHighlightColor(Color.GRAY);
        entry.nameLabel.setHighlightColor(Color.GRAY);
    }

    /** Enable a hull mod entry by enabling its button,
     *  lightening the font, and setting the XP color to white. */
    public void enableEntry(SelectorData entry) {
        entry.button.setEnabled(true);
        entry.costLabel.setHighlightColor(Color.WHITE);
        entry.nameLabel.setHighlightColor(Color.WHITE);
    }

    /** Changes the cost text to show [newText]. */
    public void changeText(SelectorData entry, String newText) {
        entry.costLabel.setText(newText);
        entry.costLabel.setHighlight(newText);
    }

    /** Adds the "Selected: x/y" and "XP: z" counters
     *  to the hull mod selection interface.
     *  Returns both LabelAPI objects. */
    public Pair<LabelAPI, LabelAPI> addCountAndXPToPanel() {
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
    public LabelAPI addXPToPanel() {
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
     *  will be based on [hullSize] and [dpCost]. All hull mods from [hullModList] are
     *  shown. 
     *  Fills in the [selectorList] argument.
     *  Returns the "show all" button in add mode or null in remove mode. */
    public ButtonAPI createHullModSelectionPanel(
            String titleString, 
            List<HullModSpecAPI> hullModList,
            HullSize hullSize,
            float dpCost,
            List<SelectorData> selectorList) {
        

        // TITLE
        title = panel.createUIElement(width - 10f, TITLE_HEIGHT, false);
        title.setTitleOrbitronLarge();
        title.addTitle(titleString).setAlignment(Alignment.MID);
        panel.addUIElement(title).inTMid(0f);

        // BUTTON LIST
        float buttonListHeight = height - TITLE_HEIGHT - BUTTON_PADDING;

        buttonsList = panel.createUIElement(width, buttonListHeight, true);
        for (HullModSpecAPI hullMod : hullModList) {
            selectorList.add(
                addHullModSelector(
                    buttonsList, 
                    hullMod, 
                    SModUtils.getBuildInCost(hullMod, hullSize, dpCost),
                    buttonWidth, 
                    BUTTON_HEIGHT, 
                    BUTTON_PADDING,
                    removeMode
                )
            );
        }
        panel.addUIElement(buttonsList).belowLeft(title, buttonListHorizontalPadding);

        if (!removeMode) {
            float showAllWidth = 100f;
            float showAllHeight = 25f;
            TooltipMakerAPI showAllElement = panel.createUIElement(showAllWidth, showAllHeight, false);
            ButtonAPI showAllButton = 
                showAllElement.addAreaCheckbox(
                    "Show all", 
                    "SHOWALLBUTTON", 
                    BASE_COLOR, 
                    DARK_COLOR,
                    BRIGHT_COLOR, 
                    showAllWidth, 
                    25f, 
                    showAllHeight
                );
            panel.addUIElement(showAllElement).inBL(5f, -35f);
            return showAllButton;
        }

        return null;
    }

    /** Adds the given hull mods to the UI component with the hull mod selector buttons.
     *  Appends the list of corresponding SelectorData to [selectorList]. */
    public void showAdditionalHullMods(List<HullModSpecAPI> hullModList, HullSize hullSize, float dpCost, List<SelectorData> selectorList) {
        if (buttonsList == null) {
            return;
        }
        
        for (HullModSpecAPI hullMod : hullModList) {
            selectorList.add(
                addHullModSelector(
                    buttonsList, 
                    hullMod, 
                    SModUtils.getBuildInCost(hullMod, hullSize, dpCost), 
                    buttonWidth, 
                    BUTTON_HEIGHT, 
                    BUTTON_PADDING, 
                    removeMode
                )
            );
        }

        panel.addUIElement(buttonsList).belowLeft(title, buttonListHorizontalPadding);
    }

    public String shortenText(String text, LabelAPI label) {
        if (text == null) {
            return null;
        }
        float ellipsesWidth = label.computeTextWidth("...");
        float maxWidth = buttonWidth * 0.95f - BUTTON_HEIGHT - ellipsesWidth;
        if (label.computeTextWidth(text) <= maxWidth) {
            return text;
        }
        int left = 0, right = text.length();

        String newText = text;
        while (right > left) {
            int mid = (left + right) / 2;
            newText = text.substring(0, mid);
            float curWidth = label.computeTextWidth(newText);
            if (curWidth > maxWidth) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return newText + "...";
    }
}
