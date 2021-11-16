package campaign;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.CombatDamageData.DamageToFleetMember;
import com.fs.starfarer.api.campaign.CombatDamageData.DealtByFleetMember;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;

import org.apache.log4j.Logger;

import util.SModUtils;
import util.SModUtils.ShipData;
import util.SModUtils.ShipDataTable;

public class EngagementResultListener extends BaseCampaignEventListener {

    private final static Logger logger = Global.getLogger(EngagementResultListener.class);

    public EngagementResultListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        EngagementResultForFleetAPI playerResult = result.getLoserResult(), enemyResult = result.getWinnerResult();
        if (result.didPlayerWin()) {
            playerResult = result.getWinnerResult();
            enemyResult = result.getLoserResult();
        }

        // Populate player ships
        List<DeployedFleetMemberAPI> playerFleet = playerResult.getAllEverDeployedCopy();//Global.getSector().getPlayerFleet().getMembersWithFightersCopy();

        // carrierTable[id] points to carrier that owns id
        Map<String, FleetMemberAPI> carrierTable = new HashMap<>();
        populateFighterMap(playerFleet, carrierTable);

        for (Map.Entry<String, FleetMemberAPI> entry : carrierTable.entrySet()) {
            logger.info("The wing with id " + entry.getKey() + " has owner " + entry.getValue() + " with id " + entry.getValue().getId());
        }
        
        // For a ship s, weightedDamageTable[s] is the damage dealt by s and its fighters,
        // weighted by the DP cost of damaged targets.
        Map<String, Float> weightedDamageTable = new HashMap<>();

        Set<String> playerShipIds = new HashSet<>();
        for (DeployedFleetMemberAPI member : playerFleet) {
            if (member.getMember() != null) {
                String memberId = member.getMember().getId();
                playerShipIds.add(memberId);
                weightedDamageTable.put(memberId, 0f);
                logger.info("Added the ship " + member + " with id " + member.getMember().getId() + " to the list of ships");
                logger.info("Trying to lookup with global: " + Global.getCombatEngine().getFleetManager(0).getShipFor(member.getMember()));
            }
        }

        // for (FleetMemberAPI member : playerShips) {
        //     playerShipIds.add(member.getId());
        //     ShipAPI ship = playerFleetManager.getShipFor(member);
        //     if (ship != null) {
        //         weightedDamageTable.put(new HashableShipAPI(ship), 0f);
        //         logger.info("Successfully added the ship " + member);
        //     }
        //     else {
        //         logger.info("No info for the ship " + member);
        //     }
        // }

        // Populate disabled and destroyed ships for enemy and player fleets
        Set<String> disabledPlayerShips = new HashSet<>();
        Set<String> disabledEnemyShips = new HashSet<>();
        for (FleetMemberAPI ship : playerResult.getDestroyed()) {
            disabledPlayerShips.add(ship.getId());
        }
        for (FleetMemberAPI ship : playerResult.getDisabled()) {
            disabledPlayerShips.add(ship.getId());
        }
        for (FleetMemberAPI ship : enemyResult.getDestroyed()) {
            disabledEnemyShips.add(ship.getId());
        }
        for (FleetMemberAPI ship : enemyResult.getDisabled()) { 
            disabledEnemyShips.add(ship.getId());
        }

        // Populate the weighted damage table with info from the last combat
        for (Map.Entry<FleetMemberAPI, DealtByFleetMember> dealt : result.getLastCombatDamageData().getDealt().entrySet()) {
            String memberId = dealt.getKey().getId();

            logger.info("The damage dealer is " + dealt.getKey() + ", " + memberId);

            // Ignore ships not in player's fleet
            if (!playerShipIds.contains(memberId)) {
                continue;
            }

            // Ignore disabled player ships if applicable
            if (!SModUtils.Constants.GIVE_XP_TO_DISABLED_SHIPS && disabledPlayerShips.contains(memberId)) {
                continue;
            }

            float weightedDamage = 0f;
            //logger.info("For the fleet member, " + dealt.getKey() + " with id " + dealt.getKey().getId() + ": ");
            for (Map.Entry<FleetMemberAPI, DamageToFleetMember> dealtTo : dealt.getValue().getDamage().entrySet()) {
                //logger.info("  - damage dealt to " + dealtTo.getKey() + " is " + dealtTo.getValue().hullDamage);
                FleetMemberAPI target = dealtTo.getKey();

                // Ignore non-disabled enemy ships if applicable
                if (SModUtils.Constants.ONLY_GIVE_XP_FOR_KILLS && !disabledEnemyShips.contains(target.getId())) {
                    continue;
                }

                float hp = target.getStats().getHullBonus().computeEffective(target.getHullSpec().getHitpoints());
                weightedDamage += dealtTo.getValue().hullDamage / hp * target.getDeploymentPointsCost();
            }

            // If the fleet member is a wing, give the damage to the carrier
            if (carrierTable.containsKey(memberId)) {
                logger.info("The ship " + dealt.getKey() + " with id " + memberId+ " is a wing owned by " + carrierTable.get(memberId) + " with id " + carrierTable.get(memberId).getId());
                String carrierId = carrierTable.get(memberId).getId();
                //logger.info(dealt.getKey() + " is a wing owned by " + wingLeaders.get(dealt.getKey().getId()));
                Float totalDamage = weightedDamageTable.get(carrierId);
                weightedDamageTable.put(carrierId, totalDamage == null ? weightedDamage : totalDamage + weightedDamage);
                logger.info("Adding damage to the carrier with id " + carrierId);
                logger.info("The total weighted damage dealt is " + weightedDamageTable.get(carrierId));
            } 
            else {
                Float totalDamage = weightedDamageTable.get(memberId);
                weightedDamageTable.put(memberId, totalDamage == null ? weightedDamage : totalDamage + weightedDamage);
                logger.info("Adding damage to the ship with id " + memberId);
                logger.info("The total weighted damage dealt is " + weightedDamageTable.get(memberId));
            }
        }

        // Use the weighted damage table to update ship data
        ShipDataTable shipDataTable = ((ShipDataTable) Global.getSector().getPersistentData().get(SModUtils.SHIPDATA_KEY));
        for (Map.Entry<String, Float> weightedDamageEntry : weightedDamageTable.entrySet()) {
            String shipId = weightedDamageEntry.getKey();
            int xpGain = (int) (weightedDamageEntry.getValue() * SModUtils.Constants.XP_GAIN_MULTIPLIER);
            
            if (xpGain == 0) {
                continue;
            }

            ShipData shipData = shipDataTable.get(shipId);
            if (shipData == null) { 
                shipData = new ShipData(xpGain, 0);
                shipDataTable.put(shipId, shipData);
                logger.info("Placing new ship data: " + shipId + ", xp: " + shipData.xp);
            }
            else {
                shipData.xp += xpGain;
                logger.info("Updating ship data: " + shipId + ", xp: " + shipData.xp);
            }

            // // Add an XP tracking hullmod to any ship that has XP 
            // if (shipData.xp > 0 && !ship.ship.getVariant().hasHullMod("progsmod_xptracker")) {
            //     ship.ship.getVariant().addPermaMod("progsmod_xptracker", false);
            //     logger.info("Added hullmod to ship " + ship.ship);
            //     for (String s : ship.ship.getVariant().getHullMods()) {
            //         logger.info("Hullmod: " + s);
            //     }
            //     logger.info("Ship id is: " + ship.ship.getId());
            //     logger.info("Ship fleet member is: " + ship.ship.getFleetMemberId());
            //     logger.info("Variant info: ");
            //     logger.info(ship.ship.getVariant().getDesignation());
            //     logger.info(ship.ship.getVariant().getHullVariantId());
            //     logger.info(ship.ship.getVariant().getOriginalVariant());
            //     logger.info(ship.ship.getVariant().toString());
            // }
        }

        // Add an XP tracking hullmod to any ship that has XP
        for (DeployedFleetMemberAPI deployedMember : playerFleet) {
            FleetMemberAPI member = deployedMember.getMember();
            ShipData data = shipDataTable.get(member.getId());
            if (data != null && data.xp > 0 && !member.getVariant().hasHullMod("progsmod_xptracker")) {
                member.getVariant().addPermaMod("progsmod_xptracker", false);
                logger.info("Added hullmod to ship " + member.getId());
                for (String s : member.getVariant().getHullMods()) {
                    logger.info("Hullmod: " + s);
                }
                logger.info("Ship id is: " + member.getId());
                logger.info("Variant info: ");
                logger.info(member.getVariant().getDesignation());
                logger.info(member.getVariant().getHullVariantId());
                logger.info(member.getVariant().getOriginalVariant());
                logger.info(member.getVariant().toString());

            }
        }
    }

    /** Populates the [carrierTable] map with mapping from wing -> leader ship. */
    private void populateFighterMap(List<DeployedFleetMemberAPI> playerFleet, Map<String, FleetMemberAPI> carrierTable) {
        List<DeployedFleetMemberAPI> deployed = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedCopyDFM();//result.getLoserResult().getAllEverDeployedCopy();
        //deployed.addAll(result.getWinnerResult().getAllEverDeployedCopy());
        List<DeployedFleetMemberAPI> deployed2 = playerFleet;
        List<ShipAPI> ships = Global.getCombatEngine().getShips();
        List<FleetMemberAPI> fm = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();


        for (ShipAPI s : ships) { 
            logger.info("Ship : " + s.getId() + ", " + s.getFleetMemberId() + ", " + s.getFleetMember());
        }
        for (DeployedFleetMemberAPI d : deployed) {
            logger.info("Deployed: " + d.getMember() + ", " + d.getMember().getId() + ", " + d.getShip().getFleetMemberId() + ", " + (d.getShip().getWing() == null ? "null" : d.getShip().getWing().getWingId()) + ", " + (d.getShip().getWingLeader() == null ? "null" : d.getShip().getWingLeader().getFleetMemberId()));
        }
        for (DeployedFleetMemberAPI d : deployed2) {
            logger.info("Deployed (second version): " + d.getShip().getId()+ ", " + d.getMember().getId() + ", " + d.getShip().getFleetMemberId() + ", " +(d.getShip().getWing() == null ? "null" : d.getShip().getWing().getWingId())  + ", " + (d.getShip().getWingLeader() == null ? "null" : d.getShip().getWingLeader().getFleetMemberId()));
            // if (d.getMember().isCarrier()) {
            //     for (FighterWingAPI f : d.getShip().getAllWings()) {
            //         logger.info("Carrier info:" + f.getLeader().getFleetMemberId() + ", " + f.getWingMembers().get(0).getFleetMemberId()); 
            //     }
            // }
        }

        for (FleetMemberAPI d : fm) { 
            logger.info("Deployed (third version): " + d + ", " + d.getId() + ", " + (d.isCarrier() ? d.getVariant().getWingId(0) : "none"));
        }

        for (DeployedFleetMemberAPI member : playerFleet) {
            if (member.isFighterWing()) {
                FighterWingAPI wing = member.getShip().getWing();
                if (wing.getSourceShip() != null && wing.getSourceShip().getFleetMember() != null)
                    carrierTable.put(member.getMember().getId(), wing.getSourceShip().getFleetMember());
            }
        }
    }
}
