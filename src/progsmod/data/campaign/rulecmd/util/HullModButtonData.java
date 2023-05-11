package progsmod.data.campaign.rulecmd.util;

import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class HullModButtonData {
    public String id;
    public String name;
    public String spriteName;
    public String defaultDescription;
    public String tooltipDescription;
    public HullModEffect hullModEffect;
    public HullSize hullSize;
    public int cost;
    public boolean isEnhanceOnly;

    public HullModButtonData(String id, String name, String spriteName, String defaultDescription, String tooltipDescription, HullModEffect hullModEffect, HullSize hullSize, int cost, boolean isEnhanceOnly) {
        this.id = id;
        this.name = name;
        this.spriteName = spriteName;
        this.defaultDescription = defaultDescription;
        this.tooltipDescription = tooltipDescription;
        this.hullModEffect = hullModEffect;
        this.hullSize = hullSize;
        this.cost = cost;
        this.isEnhanceOnly = isEnhanceOnly;
    }
}
