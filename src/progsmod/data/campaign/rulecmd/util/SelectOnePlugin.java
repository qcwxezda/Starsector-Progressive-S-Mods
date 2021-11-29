package progsmod.data.campaign.rulecmd.util;

import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;

/** Plugin that allows only selecting one button from a list of buttons. */
public class SelectOnePlugin implements CustomUIPanelPlugin {

    private ButtonAPI[] buttonList;
    private int checkedIndex = -1;

    public int getCheckedIndex() {
        return checkedIndex;
    }

    public void setData(List<ButtonAPI> buttonList) {
        this.buttonList = buttonList.toArray(new ButtonAPI[0]);
    }

    @Override
    public void advance(float arg0) {}

    @Override
    public void positionChanged(PositionAPI arg0) {}

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (buttonList == null) {
            return;
        }
        // Check every frame if a button has been pressed.
        // Just loop through every button;
        // if your ship has so many modules that doing a binary search
        // for the selected button (as in the build-in plugin) 
        // would improve performance, your ship has issues
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }

            // Ideally this would be a mouse down event, or even better,
            // a callback from a button being clicked, but the first
            // get consumed by the button click and the second doesn't seem to
            // exist.
            if (event.isMouseMoveEvent()) {
                for (int i = 0; i < buttonList.length; i++) {
                    ButtonAPI button = buttonList[i];
                    if (button.isChecked() && i != checkedIndex) {
                        if (checkedIndex >= 0 && checkedIndex < buttonList.length) {
                            buttonList[checkedIndex].setChecked(false);
                        }
                        checkedIndex = i;
                    }
                    if (!button.isChecked() && i == checkedIndex) {
                        checkedIndex = -1;
                    }
                }
            }
        }
    }

    public void disableButton(int index) {
        if (buttonList.length < index + 1) {
            return;
        }
        buttonList[index].setEnabled(false);
    }

    @Override
    public void render(float arg0) {}

    @Override
    public void renderBelow(float arg0) {}
    
}
