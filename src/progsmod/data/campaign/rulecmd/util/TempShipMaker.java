package progsmod.data.campaign.rulecmd.util;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import com.fs.starfarer.loading.specs._;
import com.fs.starfarer.rpg.Person;

public class TempShipMaker {
    /** If [variant == fleetMember.getVariant()], then this is a base ship.
     *  If [variant != fleetMember.getVariant()], then this is a module. */
    public static ShipAPI makeShip(ShipVariantAPI variant, FleetMemberAPI fleetMember) {
        // Arguments: variant spec, name, name group (?), should apply HM effects, should fill empty weapons, officer, fleet commander, fleet member
        return _.o00000(
            (HullVariantSpec) variant, 
            null, 
            null, 
            true, 
            false, 
            (Person) fleetMember.getCaptain(), 
            (Person) fleetMember.getFleetCommander(),
            fleetMember.getVariant().getHullVariantId().equals(variant.getHullVariantId()) ? (FleetMember) fleetMember : null
        );
    }
}
