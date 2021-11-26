package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class SModUtils {

    public enum GrowthType {LINEAR, EXPONENTIAL};

    /** Lookup key into the sector-persistent data that stores ship data */
    public static final String SHIP_DATA_KEY = "progsmod_ShipData";
    public static ShipDataTable SHIP_DATA_TABLE = new ShipDataTable();

    public static class Constants {
        /** How many story points it costs to unlock the first extra SMod slot. */
        public static int BASE_EXTRA_SMOD_SP_COST_FRIGATE;
        public static int BASE_EXTRA_SMOD_SP_COST_DESTROYER;
        public static int BASE_EXTRA_SMOD_SP_COST_CRUISER;
        public static int BASE_EXTRA_SMOD_SP_COST_CAPITAL;
        /** How much XP it costs to unlock the first extra SMod slot. */
        public static float BASE_EXTRA_SMOD_XP_COST_FRIGATE;
        public static float BASE_EXTRA_SMOD_XP_COST_DESTROYER;
        public static float BASE_EXTRA_SMOD_XP_COST_CRUISER;
        public static float BASE_EXTRA_SMOD_XP_COST_CAPITAL;
        /** Whether the story point increase for unlocking extra SMod slots is linear or exponential. */
        public static GrowthType EXTRA_SMOD_SP_COST_GROWTHTYPE;
        /** If exponential, SP cost is BASE * GROWTH_FACTOR^n, otherwise SP cost is BASE + n*GROWTH_FACTOR. */
        public static float EXTRA_SMOD_SP_COST_GROWTHFACTOR;
        /** Same as above two but for XP cost */
        public static GrowthType EXTRA_SMOD_XP_COST_GROWTHTYPE;
        public static float EXTRA_SMOD_XP_COST_GROWTHFACTOR;
        /** The base amount of XP it costs per OP to build in a hull mod is defined as 
         *  p(x) where x is the OP cost of the mod and p is a polynomial with coefficients
         * in [XP_COST_COEFFS] listed in ascending order. */
        public static float[] XP_COST_COEFF_FRIGATE;
        public static float[] XP_COST_COEFF_DESTROYER;
        public static float[] XP_COST_COEFF_CRUISER;
        public static float[] XP_COST_COEFF_CAPITAL;
        /** The XP cost is proportial to the ship's DP cost; the above represents the cost
         * for a ship with the base DP cost. */
        public static float BASE_DP_FRIGATE;
        public static float BASE_DP_DESTROYER;
        public static float BASE_DP_CRUISER;
        public static float BASE_DP_CAPITAL;
        /** How much XP a ship gets refunded when you remove a built-in mod. 
          * Set to something less than 0 to disable removing built-in mods completely. */
        public static float XP_REFUND_FACTOR;
        /** Whether or not ships that get disabled in battle should still get XP */
        public static boolean GIVE_XP_TO_DISABLED_SHIPS;
        /** Whether or not enemy ships that aren't disabled should still award XP */
        public static boolean ONLY_GIVE_XP_FOR_KILLS;
        /** XP gain multiplier */
        public static float XP_GAIN_MULTIPLIER;
        /** Minimum contribution percentage -- ships that deal any hull damage gain XP as if
         *  they did this fraction of total hull damage. */
        public static float MIN_CONTRIBUTION_FRACTION;
        /** Enemy ships with d-mods give less XP than pristine ships;
         *  however, regardless of the number of D-mods, they will always
         *  give at least this fraction of a pristine ship's XP. */
        public static float TARGET_DMOD_LOWER_BOUND;
        /** XP gained by non-combat ships as a fraction of total XP gain */
        public static float NON_COMBAT_XP_FRACTION;
        /** Ignore the 'no_build_in' tag */
        public static boolean IGNORE_NO_BUILD_IN;
        /** Allows increasing # of built-in hull mods with story points */
        public static boolean ALLOW_INCREASE_SMOD_LIMIT;
        /** Set to true to disable this mod's features */
        public static boolean DISABLE_MOD;

        /** Load constants from a json file */
        private static void load(String filePath) throws IOException, JSONException {
            JSONObject json = Global.getSettings().loadJSON(filePath);
            JSONObject augmentSP = json.getJSONObject("baseExtraSModSPCost");
            BASE_EXTRA_SMOD_SP_COST_FRIGATE = augmentSP.getInt("frigate");
            BASE_EXTRA_SMOD_SP_COST_DESTROYER = augmentSP.getInt("destroyer");
            BASE_EXTRA_SMOD_SP_COST_CRUISER = augmentSP.getInt("cruiser");
            BASE_EXTRA_SMOD_SP_COST_CAPITAL = augmentSP.getInt("capital");
            JSONObject augmentXP = json.getJSONObject("baseExtraSModXPCost");
            BASE_EXTRA_SMOD_XP_COST_FRIGATE = (float) augmentXP.getDouble("frigate");
            BASE_EXTRA_SMOD_XP_COST_DESTROYER = (float) augmentXP.getDouble("destroyer");
            BASE_EXTRA_SMOD_XP_COST_CRUISER = (float) augmentXP.getDouble("cruiser");
            BASE_EXTRA_SMOD_XP_COST_CAPITAL = (float) augmentXP.getDouble("capital");
            EXTRA_SMOD_SP_COST_GROWTHTYPE = 
                json.getInt("extraSModSPCostGrowthType") == 0 ? GrowthType.LINEAR : GrowthType.EXPONENTIAL;
            EXTRA_SMOD_SP_COST_GROWTHFACTOR = (float) json.getDouble("extraSModSPCostGrowthFactor");
            EXTRA_SMOD_XP_COST_GROWTHTYPE = 
                json.getInt("extraSModXPCostGrowthType") == 0 ? GrowthType.LINEAR : GrowthType.EXPONENTIAL;
            EXTRA_SMOD_XP_COST_GROWTHFACTOR = (float) json.getDouble("extraSModXPCostGrowthFactor");
            JSONObject costCoeff = json.getJSONObject("xpCostCoeff");
            XP_COST_COEFF_FRIGATE = loadCoeffsFromJSON(costCoeff, "frigate");
            XP_COST_COEFF_DESTROYER = loadCoeffsFromJSON(costCoeff, "destroyer");
            XP_COST_COEFF_CRUISER = loadCoeffsFromJSON(costCoeff, "cruiser");
            XP_COST_COEFF_CAPITAL = loadCoeffsFromJSON(costCoeff, "capital");
            BASE_DP_FRIGATE = (float) json.getDouble("baseDPFrigate");
            BASE_DP_DESTROYER = (float) json.getDouble("baseDPDestroyer");
            BASE_DP_CRUISER = (float) json.getDouble("baseDPCruiser");
            BASE_DP_CAPITAL = (float) json.getDouble("baseDPCapital");
            XP_REFUND_FACTOR = (float) json.getDouble("xpRefundFactor");
            IGNORE_NO_BUILD_IN = json.getBoolean("ignoreNoBuildIn");
            ALLOW_INCREASE_SMOD_LIMIT = json.getBoolean("allowIncreaseSModLimit");
            DISABLE_MOD = json.getBoolean("disableMod");
            JSONObject combat = json.getJSONObject("combat");
            GIVE_XP_TO_DISABLED_SHIPS = combat.getBoolean("giveXPToDisabledShips");
            ONLY_GIVE_XP_FOR_KILLS = combat.getBoolean("onlyGiveXPForKills");
            XP_GAIN_MULTIPLIER = (float) combat.getDouble("xpGainMultiplier");
            MIN_CONTRIBUTION_FRACTION = (float) combat.getDouble("minContributionFraction");
            NON_COMBAT_XP_FRACTION = (float) combat.getDouble("nonCombatXPFraction");
            TARGET_DMOD_LOWER_BOUND = (float) combat.getDouble("targetDModLowerBound");
        }

        private static float[] loadCoeffsFromJSON(JSONObject json, String name) throws JSONException {
            JSONArray jsonArray = json.getJSONArray(name);
            int length = jsonArray.length();
            float[] coeffs = new float[length];
            for (int i = 0; i < jsonArray.length(); i++) {
                coeffs[i] = (float) jsonArray.getDouble(i);
            }
            return coeffs;
        }
    }

    /** Contains XP and # of max perma mods over the normal limit. */
    public static class ShipData {
        public float xp = 0;
        public int permaModsOverLimit = 0;

        public ShipData(float xp, int pmol) {
            this.xp = xp;
            permaModsOverLimit = pmol;
        }
    }

    /** Wrapper class that maps ships to their ship data. */
    public static class ShipDataTable extends HashMap<String, ShipData> {}

    // /** Wrapper class for ShipAPI for use as hash keys. */
    // public static class HashableShipAPI {
    //     public ShipAPI ship;

    //     public HashableShipAPI(ShipAPI ship) {
    //         this.ship = ship;
    //     }

    //     @Override
    //     public boolean equals(Object o) {
    //         if (!(o instanceof HashableShipAPI)) return false;
    //         return ship.getId().equals(((HashableShipAPI) o).ship.getId());
    //     }

    //     @Override
    //     public int hashCode() {
    //         return ship.getId().hashCode();
    //     }
    // }
        
    public static void loadConstants(String filePath) {
        try {
            Constants.load(filePath);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    /** Retrieve the persistent data for this mod, if it exists. Else create it. */
    public static void loadShipData() {
        if (!Global.getSector().getPersistentData().containsKey(SHIP_DATA_KEY)) {
            SHIP_DATA_TABLE = new ShipDataTable();
            Global.getSector().getPersistentData().put(SHIP_DATA_KEY, SHIP_DATA_TABLE); 
        }
        else {
            SHIP_DATA_TABLE = (ShipDataTable) Global.getSector().getPersistentData().get(SHIP_DATA_KEY);
        }
    }

    /** Add [xp] XP to [fmId]'s entry in the ship data table,
     *  creating the entry if it doesn't exist yet.
     *  Returns whether a new entry was created. */
    public static boolean giveXP(String fmId, float xp) {
        ShipData data = SHIP_DATA_TABLE.get(fmId);
        if (data == null) { 
            SHIP_DATA_TABLE.put(fmId, new ShipData(xp, 0));
            return true;
        }
        else {
            data.xp += xp;
            return false;
        }
    }

    /** Remove [xp] XP from [fmId]'s entry in the ship data table.
     *  Returns [true] if and only if the operation succeeded. */
    public static boolean spendXP(String fmId, float xp) {
        ShipData data = SHIP_DATA_TABLE.get(fmId);
        // Edge case: no ship data (technically 0 XP)
        // should still be able to build in hull mods
        // that cost 0 XP
        if (xp <= 0f) {
            return true;
        }
        if (data == null || data.xp < xp) return false;
        data.xp -= xp;
        return true;
    }

    /** Increases [fleetMember]'s limit of built in hull mods by 1.
     *  Spends the required XP. */
    public static void incrementSModLimit(FleetMemberAPI fleetMember) {
        String fmId = fleetMember.getId();
        ShipData data = SHIP_DATA_TABLE.get(fmId);
        int cost = getAugmentXPCost(fleetMember);
        if (data == null && cost <= 0) {
            SHIP_DATA_TABLE.put(fmId, new ShipData(0, 1));
        }
        else if (spendXP(fmId, cost)) {
            data.permaModsOverLimit++;
        }
    }

    public static float getXP(String fmId) {
        ShipData data = SHIP_DATA_TABLE.get(fmId);
        return data == null ? 0f : data.xp;
    }

    public static int getNumOverLimit(String fmId) {
        ShipData data = SHIP_DATA_TABLE.get(fmId);
        return data == null ? 0 : data.permaModsOverLimit;
    }

    /** Gets the story point cost of increasing the number of built-in hullmods of [ship] by 1. */
    public static int getStoryPointCost(FleetMemberAPI ship) {
        int baseCost;
        switch (ship.getVariant().getHullSize()) {
            case FRIGATE: baseCost = Constants.BASE_EXTRA_SMOD_SP_COST_FRIGATE; break;
            case DESTROYER: baseCost = Constants.BASE_EXTRA_SMOD_SP_COST_DESTROYER; break;
            case CRUISER: baseCost = Constants.BASE_EXTRA_SMOD_SP_COST_CRUISER; break;
            case CAPITAL_SHIP: baseCost = Constants.BASE_EXTRA_SMOD_SP_COST_CAPITAL; break;
            default: baseCost = 0;
        }

        int modsOverLimit = getNumOverLimit(ship.getId());

        return Constants.EXTRA_SMOD_SP_COST_GROWTHTYPE == GrowthType.EXPONENTIAL ? 
            (int) (baseCost * Math.pow(Constants.EXTRA_SMOD_SP_COST_GROWTHFACTOR, modsOverLimit)) : 
            (int) (baseCost + modsOverLimit * Constants.EXTRA_SMOD_SP_COST_GROWTHFACTOR);
    }

    /** Gets the XP cost of increasing the number of built-in hullmods of [ship] by 1. */
    public static int getAugmentXPCost(FleetMemberAPI ship) {
        float baseCost;
        float deploymentCost = ship.getDeploymentPointsCost();
        switch (ship.getVariant().getHullSize()) {
            case FRIGATE: baseCost = Constants.BASE_EXTRA_SMOD_XP_COST_FRIGATE * deploymentCost / Constants.BASE_DP_FRIGATE; break;
            case DESTROYER: baseCost = Constants.BASE_EXTRA_SMOD_XP_COST_DESTROYER * deploymentCost / Constants.BASE_DP_DESTROYER; break;
            case CRUISER: baseCost = Constants.BASE_EXTRA_SMOD_XP_COST_CRUISER * deploymentCost / Constants.BASE_DP_CRUISER; break;
            case CAPITAL_SHIP: baseCost = Constants.BASE_EXTRA_SMOD_XP_COST_CAPITAL * deploymentCost / Constants.BASE_DP_CAPITAL; break;
            default: baseCost = 0;
        }

        int modsOverLimit = getNumOverLimit(ship.getId());

        return Constants.EXTRA_SMOD_XP_COST_GROWTHTYPE == GrowthType.EXPONENTIAL ? 
            (int) (baseCost * Math.pow(Constants.EXTRA_SMOD_XP_COST_GROWTHFACTOR, modsOverLimit)) : 
            (int) (baseCost + modsOverLimit * baseCost * Constants.EXTRA_SMOD_XP_COST_GROWTHFACTOR);
    }

    /** Gets the XP cost of building in a certain hullmod */
    public static int getBuildInCost(HullModSpecAPI hullMod, HullSize size, float deploymentCost) {
        float cost = 0f, mult = 1f;
        switch (size) {
            case FRIGATE: 
                cost = computePolynomial(hullMod.getFrigateCost(), Constants.XP_COST_COEFF_FRIGATE);
                mult = deploymentCost / Constants.BASE_DP_FRIGATE;
                break;
            case DESTROYER:
                cost = computePolynomial(hullMod.getDestroyerCost(), Constants.XP_COST_COEFF_DESTROYER);
                mult = deploymentCost / Constants.BASE_DP_DESTROYER; 
                break;
            case CRUISER:
                cost = computePolynomial(hullMod.getCruiserCost(), Constants.XP_COST_COEFF_CRUISER);
                mult = deploymentCost / Constants.BASE_DP_CRUISER; 
                break;
            case CAPITAL_SHIP:
                cost = computePolynomial(hullMod.getCapitalCost(), Constants.XP_COST_COEFF_CAPITAL); 
                mult = deploymentCost / Constants.BASE_DP_CAPITAL;
                break;
            default: return 0;
        }
        return (int) Math.max(0f, cost * mult);
    }

    // private enum FleetMemberType {DFM, FM, SHIP};

    // public static <T> List<String> getFleetMemberIds(List<? extends T> fleetMembers) {
    //     int n = fleetMembers.size();
    //     List<String> ids = new ArrayList<>(n);
    //     FleetMemberType type = null;
    //     for (T fm : fleetMembers) {
    //         if (type == null) {
    //             if (fm instanceof FleetMemberAPI) {type = FleetMemberType.FM;}
    //             else if (fm instanceof DeployedFleetMemberAPI) {type = FleetMemberType.DFM;}
    //             else if (fm instanceof ShipAPI) {type = FleetMemberType.SHIP;}
    //             else return ids;
    //         }
    //         switch (type) {
    //             case FM: ids.add(((FleetMemberAPI) fm).getId()); break;
    //             case DFM: ids.add(((DeployedFleetMemberAPI) fm).getMember().getId()); break;
    //             case SHIP: ids.add(((ShipAPI) fm).getFleetMemberId()); break;
    //             default: break;
    //         }
    //     }
    //     return ids;
    // }

    /** Given a list of fleetMembers, return a list of their ids */
    public static List<String> getFleetMemberIds(List<FleetMemberAPI> fleetMembers) {
        List<String> ids = new ArrayList<>(fleetMembers.size());
        for (FleetMemberAPI fm : fleetMembers) {
            ids.add(fm.getId());
        }
        return ids; 
    }

    /** Given a list of deployedFleetMembers, return a list of their ids */
    public static List<String> getDeployedFleetMemberIds(List<DeployedFleetMemberAPI> deployedFleetMembers) {
        List<String> ids = new ArrayList<>(deployedFleetMembers.size());
        for (DeployedFleetMemberAPI fm : deployedFleetMembers) {
            ids.add(fm.getMember().getId());
        }
        return ids; 
    }

    /** Given a fleet member, return its S-Mod limit */
    public static int getMaxSMods(FleetMemberAPI fleetMember) {
        return getNumOverLimit(fleetMember.getId()) + 
                    (int) fleetMember.getStats()
                                     .getDynamic()
                                     .getMod(Stats.MAX_PERMANENT_HULLMODS_MOD)
                                     .computeEffective(Global.getSettings().getInt("maxPermanentHullmods"));
    }

    /** Polynomial coefficients are listed in [coeff] lowest order first. */
    public static float computePolynomial(int x, float[] coeff) {
        float result = 0;
        for (int i = coeff.length - 1; i >= 0; i--) {
            result = result*x + coeff[i];
        }
        return result;
    }

    /** Mostly copied from the API */
    public static boolean canModifyHullMod(HullModSpecAPI spec, SectorEntityToken interactionTarget) {
        if (spec == null) return true;
        
        boolean reqSpaceport = spec.hasTag(HullMods.TAG_REQ_SPACEPORT);
        if (!reqSpaceport) return true;
        
        MarketAPI market = interactionTarget.getMarket();
        if (market == null) return false;

        Object tradeMode = interactionTarget.getMemory().get("$tradeMode");
        if (tradeMode == null || tradeMode == CoreUITradeMode.NONE) {
            return false;
        }
        
        for (Industry ind : market.getIndustries()) {
            if (ind.getSpec().hasTag(Industries.TAG_STATION)) return true;
            if (ind.getSpec().hasTag(Industries.TAG_SPACEPORT)) return true;
        }
        
        return false;
    }

    public static void displayXP(InteractionDialogAPI dialog, FleetMemberAPI fleetMember) {
        int xp = (int) SModUtils.getXP(fleetMember.getId());
        dialog.getTextPanel()
            .addPara(String.format("The %s has %s XP.", fleetMember.getShipName(), xp))
            .setHighlight(fleetMember.getShipName(), "" + xp);
    }
    
}
