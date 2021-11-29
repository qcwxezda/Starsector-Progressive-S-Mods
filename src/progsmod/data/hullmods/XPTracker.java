package progsmod.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import util.SModUtils;

public class XPTracker extends BaseHullMod {
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        switch (index) {
            case 0: return ship.getName();
            case 1: return String.valueOf((int) SModUtils.getXP(ship.getFleetMemberId()));
            default: return null;
        }
    }
}
