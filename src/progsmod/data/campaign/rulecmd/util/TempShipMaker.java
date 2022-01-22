package progsmod.data.campaign.rulecmd.util;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import com.fs.starfarer.loading.specs._;
import com.fs.starfarer.rpg.Person;

public class TempShipMaker {

    private static final boolean isLinux;

    static {
        isLinux = System.getProperty("os.name").startsWith("Linux");
    }

    /** If [variant == fleetMember.getVariant()], then this is a base ship.
     *  If [variant != fleetMember.getVariant()], then this is a module. */
    public static ShipAPI makeShip(ShipVariantAPI variant, FleetMemberAPI fleetMember) {
        // Arguments: variant spec, name, name group (?), should apply HM effects, should fill empty weapons, officer, fleet commander, fleet member
        if (!isLinux) {
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
        
        String variantId = variant.getHullVariantId();
        if (!Global.getSettings().doesVariantExist(variantId)) {
            List<String> variantIdList = Global.getSettings().getHullIdToVariantListMap().get(variant.getHullSpec().getBaseHullId());
            if (variantIdList != null && !variantIdList.isEmpty()) {
                variantId = variantIdList.get(0);
            }
            else {
                variantId = "This will be a nebula";
            }
        }
        ShipAPI ship = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).spawnShipOrWing(variantId, null, 0f);
        ship.setVariantForHullmodCheckOnly(ship.getVariant().clone());
        for (String hullModId : variant.getHullMods()) {
            ship.getVariant().addMod(hullModId);
            HullModEffect effect = Global.getSettings().getHullModSpec(hullModId).getEffect();
            effect.applyEffectsBeforeShipCreation(ship.getHullSize(), ship.getMutableStats(), hullModId);
            effect.applyEffectsAfterShipCreation(ship, hullModId);
        }
        return ship;
    }
}
