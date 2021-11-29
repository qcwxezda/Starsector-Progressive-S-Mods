package progsmod.data.campaign;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;

import progsmod.data.combat.ContributionTracker;
import progsmod.data.combat.ContributionTracker.ContributionType;
import util.SModUtils;

public class EngagementListener extends BaseCampaignEventListener {

    /** Maps ship id to the corresponding fleetMember id. */
    private Map<String, String> shipToFleetMemberMap;
    /** Maps fleetMemberIds to their corresponding fleetMembers. */
    private Map<String, FleetMemberAPI> idToFleetMemberMap;
    /** Maps fleet member ids to their total XP gain, by contribution type */
    private Map<String, Map<ContributionType, Float>> xpGainMap;
    /** Contribution map as populated in the combat plugin */
    private Map<ContributionType, Map<String, Map<String, Float>>> totalContributionMap;
    /** Set of player ships that are eligible to gain XP */
    private Set<String> playerFilter;
    /** Set of enemy ships that are eligible to give XP */
    private Set<String> enemyFilter;
    /** Keep track of the last dialog opened in order to add text to it. */
    private InteractionDialogAPI lastDialog;

    public EngagementListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        lastDialog = dialog;
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        // Populate the required utility mappings
        shipToFleetMemberMap = ContributionTracker.getShipToFleetMemberMap();
        totalContributionMap = ContributionTracker.getContributionTable();
        idToFleetMemberMap = new HashMap<>();
        xpGainMap = new HashMap<>();
        playerFilter = new HashSet<>();
        enemyFilter = new HashSet<>();

        if (totalContributionMap == null) {
            return;
        }

        EngagementResultForFleetAPI playerResult = result.getLoserResult(), enemyResult = result.getWinnerResult();
        if (result.didPlayerWin()) {
            playerResult = result.getWinnerResult();
            enemyResult = result.getLoserResult();
        }

        // If nobody was deployed (second-in-command handles pursuit) no damage data
        if (playerResult.getAllEverDeployedCopy() == null) {
            return;
        }

        for (DeployedFleetMemberAPI dfm : playerResult.getAllEverDeployedCopy()) {
            idToFleetMemberMap.put(dfm.getMember().getId(), dfm.getMember());
        }
        for (DeployedFleetMemberAPI dfm : enemyResult.getAllEverDeployedCopy()) {
            idToFleetMemberMap.put(dfm.getMember().getId(), dfm.getMember());
        }
        // List of ships that are eligible to gain XP
        playerFilter.addAll(SModUtils.getFleetMemberIds(playerResult.getDeployed()));
        playerFilter.addAll(SModUtils.getFleetMemberIds(playerResult.getRetreated()));
        if (SModUtils.Constants.GIVE_XP_TO_DISABLED_SHIPS) {
            playerFilter.addAll(SModUtils.getFleetMemberIds(playerResult.getDestroyed()));
            playerFilter.addAll(SModUtils.getFleetMemberIds(playerResult.getDisabled()));
        }
        // List of ships that can give XP when damaged
        enemyFilter.addAll(SModUtils.getFleetMemberIds(enemyResult.getDestroyed()));
        enemyFilter.addAll(SModUtils.getFleetMemberIds(enemyResult.getDisabled()));
        if (!SModUtils.Constants.ONLY_GIVE_XP_FOR_KILLS) {
            enemyFilter.addAll(SModUtils.getFleetMemberIds(enemyResult.getRetreated()));
            enemyFilter.addAll(SModUtils.getFleetMemberIds(enemyResult.getDeployed()));
        }

        // Convert player ships' contributions into XP gains
        for (ContributionType type : totalContributionMap.keySet()) {
            processContributions(type, totalContributionMap.get(type));
        }

        // Give XP to the ships that earned XP.
        float totalXPGain = 0f;
        for (Map.Entry<String, Map<ContributionType, Float>> xpGainEntry : xpGainMap.entrySet()) {
            for (Map.Entry<ContributionType, Float> xpByType : xpGainEntry.getValue().entrySet()) {
                float xpGain = xpByType.getValue();
                SModUtils.giveXP(xpGainEntry.getKey(), xpGain);
                totalXPGain += xpGain;
            }
        }

        // Give additional XP to non-combat ships in the player's fleet
        // Also add XP tracking hullmod to any ship that has XP
        List<FleetMemberAPI> playerFleet = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        List<FleetMemberAPI> civilianShips = new ArrayList<>();
        for (FleetMemberAPI member : playerFleet) {
            // Show the XP gain in the dialog
            if (xpGainMap.containsKey(member.getId())) {
                SModUtils.addTypedXPGainToDialog(
                    lastDialog, 
                    member, 
                    xpGainMap.get(member.getId()), 
                    "from combat");
            }
            if (member.isCivilian()) {
                SModUtils.giveXP(member.getId(), totalXPGain * SModUtils.Constants.NON_COMBAT_XP_FRACTION);
                civilianShips.add(member);
            }
            if (SModUtils.getXP(member.getId()) > 0 && !member.getVariant().hasHullMod("progsmod_xptracker")) {
                if (member.getVariant().isStockVariant()) {
                    member.setVariant(member.getVariant().clone(), false, false);
                    member.getVariant().setSource(VariantSource.REFIT);
                }
                member.getVariant().addPermaMod("progsmod_xptracker", false);
            }
        }
        if (totalXPGain > 0f && !civilianShips.isEmpty()) {
            SModUtils.addCoalescedXPGainToDialog(
                lastDialog, 
                civilianShips, 
                (int) (totalXPGain * SModUtils.Constants.NON_COMBAT_XP_FRACTION), 
                "due to being civilian ships or having no weapons equipped");
        }
    }

    /** Process contributions for a specific contribution type (ATTACK, DEFENSE, SUPPORT).
     *  Puts the result into [this.xpGainMap].
     *  Note: [contributionTable] uses shipIds, whereas [xpGainMap] uses fleetMemberIds. */
    private void processContributions(ContributionType type,  Map<String, Map<String, Float>> contributionTable) {
        for (Map.Entry<String, Map<String, Float>> contributionByEnemy : contributionTable.entrySet()) {
            String enemyFleetMemberId = shipToFleetMemberMap.get(contributionByEnemy.getKey());
            if (enemyFleetMemberId == null) {
                continue;
            }
            if (!enemyFilter.contains(enemyFleetMemberId)) {
                continue;
            }
            FleetMemberAPI enemyFleetMember = idToFleetMemberMap.get(enemyFleetMemberId);
            if (enemyFleetMember == null) {
                continue;
            }
            float totalContribution = 0f;
            for (float contribution : contributionByEnemy.getValue().values()) {
                totalContribution += contribution;
            }
            if (totalContribution <= 0f) {
                continue;
            }
            float totalXP = 
                SModUtils.Constants.XP_GAIN_MULTIPLIER 
                * getXPFractionForType(type) 
                * enemyFleetMember.getStatus().getHullDamageTaken() 
                * Math.max(
                     enemyFleetMember.getDeploymentCostSupplies(), 
                     SModUtils.Constants.TARGET_DMOD_LOWER_BOUND * enemyFleetMember.getDeploymentPointsCost()
                );
            for (Map.Entry<String, Float> contributionByPlayer : contributionByEnemy.getValue().entrySet()) {
                String playerFleetMemberId = shipToFleetMemberMap.get(contributionByPlayer.getKey());
                if (playerFleetMemberId == null) {
                    continue;
                }
                if (!playerFilter.contains(playerFleetMemberId)) {
                    continue;
                }
                addXPGain(playerFleetMemberId, totalXP * contributionByPlayer.getValue() / totalContribution, type);
            }
        }
    }

    /** Does xpGainMap[fleetMemberId][type] += amount, initializing
     *  maps as needed. */
    private void addXPGain(String fleetMemberId, float amount, ContributionType type) {
        Map<ContributionType, Float> xpByType = xpGainMap.get(fleetMemberId);
        if (xpByType == null) {
            xpByType = new EnumMap<>(ContributionType.class);
            xpGainMap.put(fleetMemberId, xpByType);
        }
        Float xp = xpByType.get(type);
        xpByType.put(type, xp == null ? amount : xp + amount);
    }

    private float getXPFractionForType(ContributionType type) {
        switch (type) {
            case ATTACK: return SModUtils.Constants.XP_FRACTION_ATTACK;
            case DEFENSE: return SModUtils.Constants.XP_FRACTION_DEFENSE;
            case SUPPORT: return SModUtils.Constants.XP_FRACTION_SUPPORT;
            default: return 0f;
        }
    }

    // private void checkContribTable(Map<ContributionType, Map<String, Map<String, Float>>> totalContribution) {
    //     Logger logger = Global.getLogger(getClass());
    //     for (Map.Entry<ContributionType, Map<String, Map<String, Float>>> entry : totalContribution.entrySet()) {
    //         for (Map.Entry<String, Map<String, Float>> entry2 : entry.getValue().entrySet()) {
    //             String enemy = entry2.getKey();
    //             if (!ProgSModCombatPlugin.baseShipTable.containsKey(enemy)) {
    //                 logger.info("Unknown enemy (contrib)");
    //             }
    //             else if (ProgSModCombatPlugin.baseShipTable.get(enemy).getOwner() == 0) {
    //                 logger.info("Player where enemy should be (contrib)");
    //             }
    //             for (Map.Entry<String, Float> entry3 : entry2.getValue().entrySet()) {
    //                 String player = entry3.getKey();
    //                 if (!ProgSModCombatPlugin.baseShipTable.containsKey(player)) {
    //                     logger.info("Unknown player (contrib)");
    //                 }
    //                 else if (ProgSModCombatPlugin.baseShipTable.get(player).getOwner() == 1) {
    //                     logger.info("Enemy where player should be (contrib)");
    //                 }
    //             }
    //         }
    //     }
    // }

    // private void checkContribTable2(Map<ContributionType, Map<String, Map<String, Float>>> totalContribution) {
    //     Logger logger = Global.getLogger(getClass());
    //     for (Map.Entry<ContributionType, Map<String, Map<String, Float>>> entry : totalContribution.entrySet()) {
    //         for (Map.Entry<String, Map<String, Float>> entry2 : entry.getValue().entrySet()) {
    //             String enemy = entry2.getKey();
    //             if (!shipToFleetMemberMap.containsKey(enemy) || !idToFleetMemberMap.containsKey(shipToFleetMemberMap.get(enemy))) {
    //                 logger.info("Unknown enemy (contrib2)");
    //             }
    //             else if (idToFleetMemberMap.get(shipToFleetMemberMap.get(enemy)).getOwner() == 0) {
    //                 logger.info("Player where enemy should be (contrib2)");
    //             }
    //             for (Map.Entry<String, Float> entry3 : entry2.getValue().entrySet()) {
    //                 String player = entry3.getKey();
    //                 if (!shipToFleetMemberMap.containsKey(player) || !idToFleetMemberMap.containsKey(shipToFleetMemberMap.get(player))) {
    //                     logger.info("Unknown player (contrib2)");
    //                 }
    //                 else if (idToFleetMemberMap.get(shipToFleetMemberMap.get(player)).getOwner() == 1) {
    //                     logger.info("Enemy where player should be (contrib2)");
    //                 }
    //             }
    //         }
    //     }
    // }
}
