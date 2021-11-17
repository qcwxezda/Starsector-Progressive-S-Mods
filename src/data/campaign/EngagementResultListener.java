package data.campaign;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CombatDamageData;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.CombatDamageData.DamageToFleetMember;
import com.fs.starfarer.api.campaign.CombatDamageData.DealtByFleetMember;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import util.SModUtils;
import util.SModUtils.Constants;
import util.SModUtils.ShipData;
import util.SModUtils.ShipDataTable;

public class EngagementResultListener extends BaseCampaignEventListener {

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

        // Populate player ships, including fighters
        List<DeployedFleetMemberAPI> playerFleet = playerResult.getAllEverDeployedCopy();

        // carrierTable[id] points to carrier that owns id
        Map<String, String> carrierTable = new HashMap<>();
        populateCarrierTable(playerFleet, carrierTable);

        // List of ships that are eligible to gain XP
        Set<String> eligibleReceivers = new HashSet<>();
        eligibleReceivers.addAll(SModUtils.getDeployedFleetMemberIds(playerFleet));
        if (!Constants.GIVE_XP_TO_DISABLED_SHIPS) {
            eligibleReceivers.removeAll(SModUtils.getFleetMemberIds(playerResult.getDestroyed()));
            eligibleReceivers.removeAll(SModUtils.getFleetMemberIds(playerResult.getDisabled()));
        }

        // List of ships that can give XP when damaged
        Set<String> eligibleTargets = new HashSet<>();
        eligibleTargets.addAll(SModUtils.getFleetMemberIds(enemyResult.getDestroyed()));
        eligibleTargets.addAll(SModUtils.getFleetMemberIds(enemyResult.getDisabled()));
        if (!Constants.ONLY_GIVE_XP_FOR_KILLS) {
            eligibleTargets.addAll(SModUtils.getFleetMemberIds(enemyResult.getRetreated()));
            eligibleTargets.addAll(SModUtils.getFleetMemberIds(enemyResult.getDeployed()));
        }
        
        // Table that maps each ship's id to the total weighted damage that it caused to eligible targets
        Map<String, Float> weightedDamageTable = new HashMap<>();
        populateWeightedDamageTable(result.getLastCombatDamageData(), eligibleReceivers, eligibleTargets, weightedDamageTable,
            new WeightedDamageFn() {
                public float compute(float damage, FleetMemberAPI target) {
                    float hp = target.getStats().getHullBonus().computeEffective(target.getHullSpec().getHitpoints());
                    return damage / hp * target.getDeploymentPointsCost();
                }
            }
        );

        // Transfer damage from fighters to their carriers
        transferWeightedDamage(carrierTable, weightedDamageTable);

        // Use the weighted damage table to update ship data
        ShipDataTable shipDataTable = ((ShipDataTable) Global.getSector().getPersistentData().get(SModUtils.SHIP_DATA_KEY));
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
            }
            else {
                shipData.xp += xpGain;
            }
        }

        // Add an XP tracking hullmod to any ship that has XP
        for (DeployedFleetMemberAPI deployedMember : playerFleet) {
            FleetMemberAPI member = deployedMember.getMember();
            ShipData data = shipDataTable.get(member.getId());
            if (data != null && data.xp > 0 && !member.getVariant().hasHullMod("progsmod_xptracker")) {
                member.getVariant().addPermaMod("progsmod_xptracker", false);
            }
        }



        // for (Map.Entry<String, FleetMemberAPI> entry : carrierTable.entrySet()) {
        //     logger.info("The wing with id " + entry.getKey() + " has owner " + entry.getValue() + " with id " + entry.getValue().getId());
        // }
        
        // // For a ship s, weightedDamageTable[s] is the damage dealt by s and its fighters,
        // // weighted by the DP cost of damaged targets.
        // Map<String, Float> weightedDamageTable = new HashMap<>();

        // Set<String> playerShipIds = new HashSet<>();
        // for (DeployedFleetMemberAPI member : playerFleet) {
        //     if (member.getMember() != null) {
        //         String memberId = member.getMember().getId();
        //         playerShipIds.add(memberId);
        //         weightedDamageTable.put(memberId, 0f);
        //         logger.info("Added the ship " + member + " with id " + member.getMember().getId() + " to the list of ships");
        //         logger.info("Trying to lookup with global: " + Global.getCombatEngine().getFleetManager(0).getShipFor(member.getMember()));
        //     }
        // }

        // // for (FleetMemberAPI member : playerShips) {
        // //     playerShipIds.add(member.getId());
        // //     ShipAPI ship = playerFleetManager.getShipFor(member);
        // //     if (ship != null) {
        // //         weightedDamageTable.put(new HashableShipAPI(ship), 0f);
        // //         logger.info("Successfully added the ship " + member);
        // //     }
        // //     else {
        // //         logger.info("No info for the ship " + member);
        // //     }
        // // }

        // // Populate disabled and destroyed ships for enemy and player fleets
        // Set<String> disabledPlayerShips = new HashSet<>();
        // Set<String> disabledEnemyShips = new HashSet<>();
        // for (FleetMemberAPI ship : playerResult.getDestroyed()) {
        //     disabledPlayerShips.add(ship.getId());
        // }
        // for (FleetMemberAPI ship : playerResult.getDisabled()) {
        //     disabledPlayerShips.add(ship.getId());
        // }
        // for (FleetMemberAPI ship : enemyResult.getDestroyed()) {
        //     disabledEnemyShips.add(ship.getId());
        // }
        // for (FleetMemberAPI ship : enemyResult.getDisabled()) { 
        //     disabledEnemyShips.add(ship.getId());
        // }

        // // // Populate the weighted damage table with info from the last combat
        // // for (Map.Entry<FleetMemberAPI, DealtByFleetMember> dealt : result.getLastCombatDamageData().getDealt().entrySet()) {
        // //     String memberId = dealt.getKey().getId();

        // //     logger.info("The damage dealer is " + dealt.getKey() + ", " + memberId);

        // //     // Ignore ships not in player's fleet
        // //     if (!playerShipIds.contains(memberId)) {
        // //         continue;
        // //     }

        // //     // Ignore disabled player ships if applicable
        // //     if (!SModUtils.Constants.GIVE_XP_TO_DISABLED_SHIPS && disabledPlayerShips.contains(memberId)) {
        // //         continue;
        // //     }

        // //     float weightedDamage = 0f;
        // //     //logger.info("For the fleet member, " + dealt.getKey() + " with id " + dealt.getKey().getId() + ": ");
        // //     for (Map.Entry<FleetMemberAPI, DamageToFleetMember> dealtTo : dealt.getValue().getDamage().entrySet()) {
        // //         //logger.info("  - damage dealt to " + dealtTo.getKey() + " is " + dealtTo.getValue().hullDamage);
        // //         FleetMemberAPI target = dealtTo.getKey();

        // //         // Ignore non-disabled enemy ships if applicable
        // //         if (SModUtils.Constants.ONLY_GIVE_XP_FOR_KILLS && !disabledEnemyShips.contains(target.getId())) {
        // //             continue;
        // //         }

        // //         float hp = target.getStats().getHullBonus().computeEffective(target.getHullSpec().getHitpoints());
        // //         weightedDamage += dealtTo.getValue().hullDamage / hp * target.getDeploymentPointsCost();
        // //     }

        // //     // If the fleet member is a wing, give the damage to the carrier
        // //     if (carrierTable.containsKey(memberId)) {
        // //         logger.info("The ship " + dealt.getKey() + " with id " + memberId+ " is a wing owned by " + carrierTable.get(memberId) + " with id " + carrierTable.get(memberId).getId());
        // //         String carrierId = carrierTable.get(memberId).getId();
        // //         //logger.info(dealt.getKey() + " is a wing owned by " + wingLeaders.get(dealt.getKey().getId()));
        // //         Float totalDamage = weightedDamageTable.get(carrierId);
        // //         weightedDamageTable.put(carrierId, totalDamage == null ? weightedDamage : totalDamage + weightedDamage);
        // //         logger.info("Adding damage to the carrier with id " + carrierId);
        // //         logger.info("The total weighted damage dealt is " + weightedDamageTable.get(carrierId));
        // //     } 
        // //     else {
        // //         Float totalDamage = weightedDamageTable.get(memberId);
        // //         weightedDamageTable.put(memberId, totalDamage == null ? weightedDamage : totalDamage + weightedDamage);
        // //         logger.info("Adding damage to the ship with id " + memberId);
        // //         logger.info("The total weighted damage dealt is " + weightedDamageTable.get(memberId));
        // //     }
        // // }


    }

    /** Populates the [carrierTable] map with mapping from wing -> leader ship. Modifies carrierTable. */
    private void populateCarrierTable(List<DeployedFleetMemberAPI> playerFleet, Map<String, String> carrierTable) {
        for (DeployedFleetMemberAPI member : playerFleet) {
            if (member.isFighterWing() && member.getShip() != null) {
                FighterWingAPI wing = member.getShip().getWing();
                if (wing.getSourceShip() != null && wing.getSourceShip().getFleetMember() != null)
                    carrierTable.put(member.getMember().getId(), wing.getSourceShip().getFleetMemberId());
            }
        }
    }

    /** Uses the combat data to generate, for each ship in keyFilter and combatDamageData.getDealt().keySet(), 
     *  the sum of its weighted damage to each ship in targetFilter. Modifies weightedDamageTable. */
    private void populateWeightedDamageTable(CombatDamageData combatDamageData, Set<String> keyFilter, Set<String> targetFilter,
                                             Map<String, Float> weightedDamageTable, WeightedDamageFn damageFn) {
        for (Map.Entry<FleetMemberAPI, DealtByFleetMember> dealt : combatDamageData.getDealt().entrySet()) {
            String memberId = dealt.getKey().getId();

            // Add only ships in the keyFilter to the table
            if (!keyFilter.contains(memberId)) {
                continue;
            }

            float weightedDamage = 0f;
            for (Map.Entry<FleetMemberAPI, DamageToFleetMember> dealtTo : dealt.getValue().getDamage().entrySet()) {
                FleetMemberAPI target = dealtTo.getKey();

                // Only consider damage to ships in the targetFilter
                if (!targetFilter.contains(target.getId())) {
                    continue;
                }

                weightedDamage += damageFn.compute(dealtTo.getValue().hullDamage, target);
            }

            Float totalDamage = weightedDamageTable.get(memberId);
            weightedDamageTable.put(memberId, totalDamage == null ? weightedDamage : totalDamage + weightedDamage);
        }
    }

    /** For each <k, v> pair in transferMap, adds k's weighted damage to v's weighted damage. Modifies weightedDamageTable. */
    private void transferWeightedDamage(Map<String, String> transferMap, Map<String, Float> weightedDamageTable) {
        for (Map.Entry<String, String> transfer : transferMap.entrySet()) {
            Float transferDamage = weightedDamageTable.get(transfer.getKey());
            if (transferDamage != null && transferDamage > 0) {
                String receiver = transfer.getValue();
                Float totalDamage = weightedDamageTable.get(receiver);
                weightedDamageTable.put(receiver, totalDamage == null ? transferDamage : transferDamage + totalDamage);
            }
        }
    }

    /** Classes implementing this interface provide a function that takes raw hull damage on a target and
     *  weights it according to the target's statistics.*/
    private interface WeightedDamageFn {
        public float compute(float damage, FleetMemberAPI target);
    }
}
