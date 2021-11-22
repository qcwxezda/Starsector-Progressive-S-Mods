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
import com.fs.starfarer.api.loading.VariantSource;

import util.SModUtils;
import util.SModUtils.Constants;

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
        // If nobody was deployed (second-in-command handles pursuit) no damage data
        if (playerFleet == null) {
            return;
        }

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
        
        // Table that maps each ship's id to the total weighted damage that it caused to
        // each eligible target.
        Map<String, Map<String, Float>> weightedDamageTable = new HashMap<>();
        WeightedDamageFn damageFn = new WeightedDamageFn() {
            @Override
            public float compute(float damageFrac, FleetMemberAPI target) {
                return Math.min(damageFrac, 1f)
                        * Math.max(
                            target.getDeploymentCostSupplies(), 
                            SModUtils.Constants.TARGET_DMOD_LOWER_BOUND * target.getDeploymentPointsCost()
                        );
            }
        };
        populateWeightedDamageTable(result.getLastCombatDamageData(), eligibleReceivers, eligibleTargets, weightedDamageTable, damageFn);

        // Transfer damage from fighters to their carriers
        transferWeightedDamage(carrierTable, weightedDamageTable);

        // Flatten the damage table into total weighted damage per ship, 
        // taking minimum contribution into account
        Map<String, Float> minContributionMap = new HashMap<>();
        for (DeployedFleetMemberAPI dfm : enemyResult.getAllEverDeployedCopy()) {
            FleetMemberAPI target = dfm.getMember();
            minContributionMap.put(target.getId(), damageFn.compute(SModUtils.Constants.MIN_CONTRIBUTION_FRACTION, target));
        }
        Map<String, Float> totalWeightedDamage = new HashMap<>();
        flattenWeightedDamage(weightedDamageTable, minContributionMap, totalWeightedDamage);

        // Use the weighted damage table to update ship data
        float totalXPGain = 0f;
        for (Map.Entry<String, Float> weightedDamageEntry : totalWeightedDamage.entrySet()) {
            float xpGain = weightedDamageEntry.getValue() * SModUtils.Constants.XP_GAIN_MULTIPLIER;
            SModUtils.giveXP(weightedDamageEntry.getKey(), xpGain);
            totalXPGain += xpGain;
        }

        // Give additional XP to non-combat ships in the player's fleet
        // Also add XP tracking hullmod to any ship that has XP
        List<FleetMemberAPI> playerEntireFleet = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : playerEntireFleet) {
            if (member.isCivilian()) {
                SModUtils.giveXP(member.getId(), totalXPGain * SModUtils.Constants.NON_COMBAT_XP_FRACTION);
            }
            if (SModUtils.getXP(member.getId()) > 0 && !member.getVariant().hasHullMod("progsmod_xptracker")) {
                if (member.getVariant().isStockVariant()) {
                    member.setVariant(member.getVariant().clone(), false, false);
                    member.getVariant().setSource(VariantSource.REFIT);
                }
                member.getVariant().addPermaMod("progsmod_xptracker", false);
            }
        }
    }

    /** Populates the [carrierTable] map with mapping from wing -> leader ship. Modifies carrierTable. */
    private void populateCarrierTable(List<DeployedFleetMemberAPI> playerFleet, Map<String, String> carrierTable) {
        for (DeployedFleetMemberAPI member : playerFleet) {
            if (member.isFighterWing() && member.getShip() != null) {
                FighterWingAPI wing = member.getShip().getWing();
                if (wing != null
                     && wing.getSourceShip() != null 
                     && wing.getSourceShip().getFleetMember() != null)
                    carrierTable.put(member.getMember().getId(), wing.getSourceShip().getFleetMemberId());
            }
        }
    }

    /** Uses the combat data to generate, for each ship in keyFilter and combatDamageData.getDealt().keySet(), 
     *  the sum of its weighted damage to each ship in targetFilter. Modifies weightedDamageTable. */
    private void populateWeightedDamageTable(CombatDamageData combatDamageData, Set<String> keyFilter, Set<String> targetFilter,
                                             Map<String, Map<String, Float>> weightedDamageTable, WeightedDamageFn damageFn) {
        for (Map.Entry<FleetMemberAPI, DealtByFleetMember> dealt : combatDamageData.getDealt().entrySet()) {
            String memberId = dealt.getKey().getId();

            // Add only ships in the keyFilter to the table
            if (!keyFilter.contains(memberId)) {
                continue;
            }

            weightedDamageTable.put(memberId, new HashMap<String, Float>());

            for (Map.Entry<FleetMemberAPI, DamageToFleetMember> dealtTo : dealt.getValue().getDamage().entrySet()) {
                FleetMemberAPI target = dealtTo.getKey();
                String targetId = target.getId();

                // Only consider damage to ships in the targetFilter
                if (!targetFilter.contains(targetId)) {
                    continue;
                }

                float hp = target.getStats().getHullBonus().computeEffective(target.getHullSpec().getHitpoints());
                weightedDamageTable.get(memberId).put(targetId, damageFn.compute(dealtTo.getValue().hullDamage / hp, target));
            }
        }
    }

    /** For each <k, v> pair in transferMap, transfers k's weighted damage to v's weighted damage. Modifies weightedDamageTable. */
    private void transferWeightedDamage(Map<String, String> transferMap, Map<String, Map<String, Float>> weightedDamageTable) {
        for (Map.Entry<String, String> transfer : transferMap.entrySet()) {
            String giver = transfer.getKey();
            String receiver = transfer.getValue();

            if (!weightedDamageTable.containsKey(receiver)) {
                weightedDamageTable.put(receiver, new HashMap<String, Float>());
            }
            
            Map<String, Float> dmgData = weightedDamageTable.get(giver);
            if (dmgData != null) {
                for (Map.Entry<String, Float> dmgEntry : dmgData.entrySet()) {
                    String targetId = dmgEntry.getKey();
                    float newDmg = dmgEntry.getValue();
                    Float curDmg = weightedDamageTable.get(receiver).get(targetId);
                    weightedDamageTable.get(receiver).put(targetId, curDmg == null ? newDmg : newDmg + curDmg);
                }
            }

            weightedDamageTable.remove(giver);
        }
    }

    /** Sums up the weighted damage entries for each player ship, using the minimum contribution table as a lower bound
     *  for each of that ship's targets. Modifies [totalWeightedDamage] */
    private void flattenWeightedDamage(
            Map<String, Map<String, Float>> weightedDamageTable, 
            Map<String, Float> minContributionMap, 
            Map<String, Float> totalWeightedDamage
        ) {
        for (Map.Entry<String, Map<String, Float>> dmgEntry : weightedDamageTable.entrySet()) {
            String receiver = dmgEntry.getKey();
            float total = 0f;
            for (Map.Entry<String, Float> targetEntry : dmgEntry.getValue().entrySet()) {
                float damage = targetEntry.getValue();
                if (damage <= 0) {
                    continue;
                }
                Float minContribution = minContributionMap.get(targetEntry.getKey());
                total += Math.max(damage, minContribution == null ? 0f : minContribution);
            }
            totalWeightedDamage.put(receiver, total);
        }
    }

    /** Classes implementing this interface provide a function that takes percent hull damage on a target and
     *  weights it according to the target's statistics.*/
    private interface WeightedDamageFn {
        public float compute(float damageFrac, FleetMemberAPI target);
    }
}
