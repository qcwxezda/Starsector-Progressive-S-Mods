package progsmod.data.campaign;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

/** Tracks every fleet that's generated and adds an XP tracker to any ships that are over the normal S-mod limit, in order
 *  to apply the same DP cost penalty that the player faces. */
public class NPCFleetSModTracker extends BaseCampaignEventListener  {
    public NPCFleetSModTracker(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        SectorEntityToken target = dialog.getInteractionTarget();
        if (target instanceof CampaignFleetAPI) {
            addXPTrackerToSModdedShips((CampaignFleetAPI) target);
        }
    }

    private void addXPTrackerToSModdedShips(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getBattle() == null || fleet.getBattle().getCombinedTwo() == null) {
            return;
        }
        for (FleetMemberAPI fm : fleet.getBattle().getCombinedTwo().getMembersWithFightersCopy()) {
            int baseSMods = Misc.getMaxPermanentMods(fm, fleet.getCommanderStats());
            if (!fm.isFighterWing() && fm.getVariant() != null) {
                int numOverLimit = fm.getVariant().getSMods().size() - baseSMods;
                if (numOverLimit > 0 && !fm.getVariant().hasHullMod("progsmod_xptracker")) {
                    fm.getVariant().addPermaMod("progsmod_xptracker", false);
                }
            }
        }
    }
}
