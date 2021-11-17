package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import util.SModUtils;
import util.SModUtils.ShipData;

public class XPTracker extends BaseHullMod {
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        ShipData data = SModUtils.SHIP_DATA_TABLE.get(ship.getFleetMemberId());
        switch (index) {
            case 0: return ship.getName();
            case 1: return String.valueOf(data == null ? 0 : data.xp);
            default: return null;
        }
    }
}
