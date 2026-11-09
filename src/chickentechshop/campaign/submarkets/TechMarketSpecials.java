package chickentechshop.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;

/**
 * Chicken's Tech - Specials: every special item the player can use except
 * blueprint packages (those are in the Blueprints submarket). The market level
 * sets how many of each item are in stock.
 */
public class TechMarketSpecials extends TechMarket {

    @Override
    protected void addStock() {
        CargoAPI cargo = getCargo();
        for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs()) {
            if (isBlueprintPackage(spec) || !isUsableSpecialItem(spec)) {
                continue;
            }
            int quantity = randomQuantity();
            log.info("Adding special item " + spec.getId() + " with quantity " + quantity);
            cargo.addSpecial(new SpecialItemData(spec.getId(), null), quantity);
        }
    }
}
