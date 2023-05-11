package progsmod.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import util.SModUtils;

public class XPTracker extends BaseHullMod {
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        switch (index) {
            case 0: return ship.getName();
            case 1: return Misc.getFormat().format((int) SModUtils.getXP(ship.getFleetMemberId()));
            default: return null;
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        int sModsOverLimit = getSModsOverLimit(stats);

        if (sModsOverLimit > 0) {
            int dpMod = computeDPModifier(stats, sModsOverLimit);
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, dpMod);
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, final ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null || ship.getMutableStats() == null) return;
        int sModsOverLimit = getSModsOverLimit(ship.getMutableStats());
        int dpMod = computeDPModifier(ship.getMutableStats(), sModsOverLimit);
        if (dpMod > 0) {
            tooltip.addPara("Deployment point cost increased by %s due to the presence of additional S-Mods over the standard limit.",
                    10f,
                    Misc.getNegativeHighlightColor(),
                    Misc.getHighlightColor(),
                    Integer.toString(dpMod));
        }
    }

    private int getSModsOverLimit(MutableShipStatsAPI stats) {
        if (stats == null || stats.getVariant() == null || spec == null) return 0;
        return Math.max(0, stats.getVariant().getSMods().size() - SModUtils.getBaseSMods(stats));
    }

    private int computeDPModifier(MutableShipStatsAPI stats, int sModsOverLimit) {
        if (stats == null || stats.getFleetMember() == null) return 0;

        float baseDPCost = stats.getFleetMember().getUnmodifiedDeploymentPointsCost();
        return (int) Math.ceil(baseDPCost * SModUtils.Constants.DEPLOYMENT_COST_PENALTY * sModsOverLimit);
    }
}
