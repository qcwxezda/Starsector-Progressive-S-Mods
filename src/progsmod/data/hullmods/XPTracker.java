package progsmod.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import util.SModUtils;

import java.util.List;

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
        ShipVariantAPI variant = stats.getVariant();
        if (SModUtils.Constants.DISABLE_MOD || SModUtils.Constants.DEPLOYMENT_COST_PENALTY <= 0f || variant == null) return;

        int sModsOverLimit = SModUtils.getSModsOverLimitIncludeModules(variant, stats);

        if (sModsOverLimit > 0) {
            int dpMod = computeDPModifier(stats, sModsOverLimit);
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, dpMod);
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, final ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null || ship.getMutableStats() == null || ship.getVariant() == null) return;
        int sModsOverLimit = SModUtils.getSModsOverLimitIncludeModules(ship.getVariant(), ship.getMutableStats());
        int dpMod = computeDPModifier(ship.getMutableStats(), sModsOverLimit);
        if (dpMod > 0) {
            tooltip.addPara("Deployment point cost increased by %s due to the presence of additional S-mods over the standard limit.",
                    10f,
                    Misc.getNegativeHighlightColor(),
                    Misc.getHighlightColor(),
                    Integer.toString(dpMod));
        }
    }

    private int computeDPModifier(MutableShipStatsAPI stats, int sModsOverLimit) {
        if (stats == null || stats.getFleetMember() == null) return 0;

        float baseDPCost = stats.getFleetMember().getUnmodifiedDeploymentPointsCost();
        return (int) Math.ceil(baseDPCost * SModUtils.Constants.DEPLOYMENT_COST_PENALTY * sModsOverLimit);
    }
}
