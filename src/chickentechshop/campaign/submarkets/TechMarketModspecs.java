package chickentechshop.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;

/**
 * Chicken's Tech - Modspecs: one modspec for every hull mod the player can
 * learn and doesn't know yet.
 */
public class TechMarketModspecs extends TechMarket {

    @Override
    protected void addStock() {
        CargoAPI cargo = getCargo();
        FactionAPI player = Global.getSector().getPlayerFaction();
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
            if (spec.isHidden() || spec.isHiddenEverywhere() || spec.isAlwaysUnlocked()
                    || spec.hasTag(Tags.HULLMOD_NO_DROP) || spec.hasTag(Tags.HULLMOD_DMOD)
                    || player.knowsHullMod(spec.getId())) {
                continue;
            }
            cargo.addSpecial(new SpecialItemData(MODSPEC_ID, spec.getId()), 1);
        }
    }

    // Learning a modspec takes it off the shelf for good, so restock on every visit
    @Override
    public void updateCargoPrePlayerInteraction() {
        sinceLastCargoUpdate = 0f;
        updateCargo();
    }
}
