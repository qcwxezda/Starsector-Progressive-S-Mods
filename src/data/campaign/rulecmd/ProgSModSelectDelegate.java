package data.campaign.rulecmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Pair;

import util.SModUtils;
import util.SModUtils.ShipData;

import java.awt.Color;

public class ProgSModSelectDelegate implements CustomDialogDelegate {

    @Override
    public void createCustomDialog(CustomPanelAPI arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void customDialogCancel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void customDialogConfirm() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getCancelText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getConfirmText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasCancelButton() {
        // TODO Auto-generated method stub
        return false;
    }

    // static final String TITLE_STRING;
    // static final String NMODS_PREFIX = "Selected: ";
    // static final String XP_PREFIX = "XP Remaining: ";
    // static final float BUTTON_HEIGHT = 50f;
    // static final float TITLE_HEIGHT = 50f;
    // static final float BUTTON_PADDING = 10f;
    // static final float TRACKER_HEIGHT = 20f;

    // private static final Color BASE_COLOR, DARK_COLOR, BRIGHT_COLOR;

    // static {
    //     FactionAPI playerFaction = Global.getSector().getPlayerFaction();
    //     BASE_COLOR = playerFaction.getBaseUIColor();
    //     DARK_COLOR = playerFaction.getDarkUIColor();
    //     BRIGHT_COLOR = playerFaction.getBrightUIColor();
    // }

    // private ProgSModSelectPlugin panelPlugin;
    // private final FleetMemberAPI fleetMember;
    // private final InteractionDialogAPI dialog;
    // private final boolean removeMode;
    // final int shipXP, numCanBuildIn;
    // final List<HullModSpecAPI> availableHullMods;
    // final HullSize hullSize;

    // // Hull mods that are barred from being built-in (i.e. safety overrides)
    // // are not added to this list.
    // final List<EntryData> listEntries;
    // LabelAPI remainingXPText, nSelectedModsText;


    // /** A container for the button, text, etc. in a 
    //  * hull mod entry that needs to be modifiable. */
    // class EntryData {
    //     ButtonAPI button;
    //     LabelAPI nameLabel;
    //     LabelAPI costLabel;
    //     int hullModCost;
    //     String hullModId;

    //     EntryData(ButtonAPI button, LabelAPI nameLabel, LabelAPI costLabel, int hullModCost, String hullModId) {
    //         this.button = button;
    //         this.nameLabel = nameLabel;
    //         this.costLabel = costLabel;
    //         this.hullModCost = hullModCost;
    //         this.hullModId = hullModId;
    //     }
    // }

    // public ProgSModSelectDelegate(
    //         FleetMemberAPI fleetMember, 
    //         InteractionDialogAPI dialog, 
    //         String titleText, 
    //         List<HullModSpecAPI> hullModsToDisplay,
    //         boolean removeMode) {
    //     TITLE_STRING = titleText;
    //     this.fleetMember = fleetMember;
    //     this.dialog = dialog;
    //     this.removeMode = removeMode;
    //     availableHullMods = hullModsToDisplay;
    //     ShipData shipData = SModUtils.SHIP_DATA_TABLE.get(fleetMember.getId());
    //     shipXP = shipData == null ? 0 : shipData.xp;
    //     numCanBuildIn = SModUtils.getMaxSMods(fleetMember) - fleetMember.getVariant().getSMods().size();
    //     hullSize = fleetMember.getVariant().getHullSize();
    //     SettingsAPI settings = Global.getSettings();
    //     // Collection<String> hullModIds = fleetMember.getVariant().getNonBuiltInHullmods();
    //     // for (String hullModId : hullModIds) {
    //     //     availableHullMods.add(settings.getHullModSpec(hullModId));
    //     // }
    //     listEntries = new ArrayList<>(hullModsToDisplay.size());
    // }

    // /** Adds an area checkbox on top of an image-with-text. 
    //  *  Returns the button, title text, and cost info text. */
    // private EntryData addHullModSelector(
    //             TooltipMakerAPI tooltip, HullModSpecAPI hullMod, float width, float height, float pad) {
    //     ButtonAPI button = 
    //         tooltip.addAreaCheckbox("", "BUTTON_" + hullMod.getId(), BASE_COLOR, DARK_COLOR, BRIGHT_COLOR, width, height, pad);
    //     TooltipMakerAPI imageAndText = tooltip.beginImageWithText(hullMod.getSpriteName(), height);
    //     imageAndText.setTitleOrbitronLarge();
    //     LabelAPI nameText = imageAndText.addTitle(hullMod.getDisplayName());
    //     nameText.setHighlight(hullMod.getDisplayName());
    //     nameText.setHighlightColor(Color.WHITE);
    //     imageAndText.setParaFontOrbitron();
    //     int hullModCost = SModUtils.getBuildInCost(hullMod, hullSize);
    //     String costText = hullModCost + " XP";
    //     LabelAPI costTextLabel = imageAndText.addPara(costText, 0f);
    //     costTextLabel.setHighlight(costText);
    //     costTextLabel.setHighlightColor(Color.WHITE);
    //     tooltip.addImageWithText(-height);
    //     tooltip.setForceProcessInput(true);
    //     return new EntryData(button, nameText, costTextLabel, hullModCost, hullMod.getId());
    // }

    // /** Returns the pair (number of checked buttons, sum of checked buttons' XP costs). 
    //  * Populates [checkedEntries] with the checked buttons if a non-null list was passed in.*/
    // Pair<Integer, Integer> tallyCheckedEntries(List<EntryData> checkedEntries) {
    //     if (checkedEntries != null && !checkedEntries.isEmpty()) {
    //         checkedEntries.clear();
    //     }
    //     int numChecked = 0, sumChecked = 0;
    //     for (EntryData entry : listEntries) {
    //         if (entry.button.isChecked()) {
    //             if (checkedEntries != null) {
    //                 checkedEntries.add(entry);
    //             }
    //             numChecked++;
    //             sumChecked += entry.hullModCost;
    //         }
    //     }
    //     return new Pair<>(numChecked, sumChecked);
    // }

    // /** Update the remaining XP label to reflect a new value. */
    // void setRemainingXPText(int remainingXP) {
    //     String newText = XP_PREFIX + remainingXP;
    //     remainingXPText.setText(newText);
    //     remainingXPText.setHighlight(XP_PREFIX.length(), newText.length() - 1);
    // }

    // /** Update the number of selected hull mods text to reflect a new value. */
    // void setNSelectedModsText(int nSelected) {
    //     String newText = NMODS_PREFIX + nSelected + "/" + numCanBuildIn;
    //     nSelectedModsText.setText(newText);
    //     nSelectedModsText.setHighlight(NMODS_PREFIX.length(), NMODS_PREFIX.length() + String.valueOf(nSelected).length() - 1);
    //     nSelectedModsText.setHighlightColor(nSelected < numCanBuildIn ? Color.WHITE : Color.ORANGE);
    // }

    // /** Disable a hull mod entry by disabling its button, 
    //  * darkening the font, and setting the XP color to gray. */
    // void disableEntry(EntryData entry) {
    //     entry.button.setEnabled(false);
    //     entry.costLabel.setHighlightColor(Color.GRAY);
    //     entry.nameLabel.setHighlightColor(Color.GRAY);
    // }

    // /** Enable a hull mod entry by enabling its button,
    //  *  lightening the font, and setting the XP color to white. */
    // void enableEntry(EntryData entry) {
    //     entry.button.setEnabled(true);
    //     entry.costLabel.setHighlightColor(Color.WHITE);
    //     entry.nameLabel.setHighlightColor(Color.WHITE);
    // }

    // @Override
    // public void createCustomDialog(CustomPanelAPI panel) {
    //     float width = panel.getPosition().getWidth();
    //     float height = panel.getPosition().getHeight();

    //     // TITLE
    //     TooltipMakerAPI title = panel.createUIElement(width - 10f, TITLE_HEIGHT, false);
    //     title.setTitleOrbitronLarge();
    //     title.addTitle(TITLE_STRING).setAlignment(Alignment.MID);
    //     panel.addUIElement(title).inTMid(0f);

        
    //     // BUILD IN LIMIT
    //     float trackerWidth = width * 0.85f;
    //     TooltipMakerAPI countTracker = panel.createUIElement(trackerWidth, TRACKER_HEIGHT, false);
    //     nSelectedModsText = countTracker.addTitle("");
    //     setNSelectedModsText(0);
    //     nSelectedModsText.setAlignment(Alignment.LMID);
    //     remainingXPText = countTracker.addTitle("");
    //     setRemainingXPText(shipXP);
    //     remainingXPText.setAlignment(Alignment.RMID);
    //     panel.addUIElement(countTracker).inTMid(30f);


    //     // BUTTON LIST
    //     float buttonWidth = width * 0.95f;
    //     float buttonHeight = BUTTON_HEIGHT;
    //     float buttonPadding = BUTTON_PADDING;
    //     float buttonListHeight = height - TITLE_HEIGHT - BUTTON_PADDING;
    //     // There seems to be a base horizontal padding of 10f, 
    //     // need to account for this for true centering 
    //     float buttonListHorizontalPadding = 0.5f * (width - buttonWidth - 10f);
    //     TooltipMakerAPI buttonsList = panel.createUIElement(width, buttonListHeight, true);
    //     for (HullModSpecAPI hullMod : availableHullMods) {
    //         int cost = SModUtils.getBuildInCost(hullMod, hullSize);
    //         EntryData entry = addHullModSelector(buttonsList, hullMod, buttonWidth, buttonHeight, buttonPadding);
    //         if (removeMode) {
    //             listEntries.add(entry);
    //         }
    //         else if (hullMod.hasTag("no_build_in")) {
    //             disableEntry(entry);
    //             entry.costLabel.setText("Cannot be built in");
    //             entry.costLabel.setHighlight("Cannot be built in");
    //         }
    //         else {
    //             if (cost > shipXP) {
    //                 disableEntry(entry);
    //             }
    //             listEntries.add(entry);
    //         }
    //     }
    //     panel.addUIElement(buttonsList).belowLeft(title, buttonListHorizontalPadding);
    // }

    // @Override
    // public void customDialogCancel() {}

    // @Override
    // public void customDialogConfirm() {
    //     if (removeMode) {

    //     }
    //     else { 
    //         List<EntryData> checkedEntries = new ArrayList<>();
    //         Pair<Integer, Integer> numAndSum = tallyCheckedEntries(checkedEntries);
            
    //         // One last sanity check
    //         if (numAndSum.one > numCanBuildIn || numAndSum.two > shipXP) {
    //             return;
    //         }
    //         ShipData shipData = SModUtils.SHIP_DATA_TABLE.get(fleetMember.getId());
    //         if (shipData == null || shipData.xp < numAndSum.two) {
    //             return;
    //         }

    //         shipData.xp -= numAndSum.two;
    //         for (EntryData entry : checkedEntries) {
    //             fleetMember.getVariant().addPermaMod(entry.hullModId, true);
    //             String hullModName = Global.getSettings().getHullModSpec(entry.hullModId).getDisplayName();
    //             LabelAPI confirmText = dialog.getTextPanel().addPara("Built " + hullModName + " into the " + fleetMember.getShipName());
    //             confirmText.setHighlight(hullModName, fleetMember.getShipName());
    //         }

    //         if (!checkedEntries.isEmpty()) {
    //             Global.getSoundPlayer().playUISound("ui_acquired_hullmod", 1f, 1f);
    //         }
    //     }
    // }

    // @Override
    // public String getCancelText() {
    //     return "Cancel";
    // }

    // @Override
    // public String getConfirmText() {
    //     return "Confirm";
    // }

    // @Override
    // public CustomUIPanelPlugin getCustomPanelPlugin() {
    //     if (removeMode) {
    //         return null;
    //     }
    //     if (panelPlugin != null) {
    //         return panelPlugin;
    //     }
    //     panelPlugin = new ProgSModSelectPlugin(this);
    //     return panelPlugin;
    // }

    // @Override
    // public boolean hasCancelButton() {
    //     return true;
    // }
    
}
