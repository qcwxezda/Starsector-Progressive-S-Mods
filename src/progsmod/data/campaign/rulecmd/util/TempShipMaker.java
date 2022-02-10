package progsmod.data.campaign.rulecmd.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.combat.CombatFleetManager;
import com.fs.starfarer.loading.specs.HullVariantSpec;

public class TempShipMaker {
    /** If [variant == fleetMember.getVariant()], then this is a base ship.
     *  If [variant != fleetMember.getVariant()], then this is a module. */
    public static ShipAPI makeShip(ShipVariantAPI variant, FleetMemberAPI fleetMember) {
        // Create a temporary fleet member
        FleetMember tempFleetMember = new FleetMember(0, (HullVariantSpec) variant, FleetMemberType.SHIP);
        return tempFleetMember.instantiateForCombat(null, 0, (CombatFleetManager) Global.getCombatEngine().getFleetManager(FleetSide.PLAYER));
    }
}
