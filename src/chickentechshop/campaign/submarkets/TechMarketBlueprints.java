package chickentechshop.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.List;

/**
 * Chicken's Tech - Blueprints: every blueprint package, plus 20% per market
 * level of the rare ship, weapon and fighter blueprints.
 */
public class TechMarketBlueprints extends TechMarket {

    @Override
    protected void addStock() {
        addBlueprintPackages();
        addWeaponBlueprints();
        addWingsBlueprints();
        addShipsBlueprints();
    }

    // One of every blueprint package, they only need learning once
    protected void addBlueprintPackages() {
        CargoAPI cargo = getCargo();
        for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs()) {
            if (!isBlueprintPackage(spec) || !isUsableSpecialItem(spec)) {
                continue;
            }
            cargo.addSpecial(new SpecialItemData(spec.getId(), null), 1);
        }
    }

    protected void addWeaponBlueprints() {
        CargoAPI cargo = getCargo();
        List<WeaponSpecAPI> weaponSpecs = Global.getSettings().getAllWeaponSpecs();
        WeightedRandomPicker<String> randomWeaponPicker = new WeightedRandomPicker<>(itemGenRandom);

        for (WeaponSpecAPI spec : weaponSpecs) {
            // Check if this is not a blueprint that should drop etc
            if (!spec.hasTag("rare_bp") || spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.NO_BP_DROP)) {
                continue;
            }
            randomWeaponPicker.add(spec.getWeaponId());
        }

        // Now make our Blueprints
        int itemPickerNum = Math
                .round((((float) randomWeaponPicker.getItems().size() / 5f) * (float) getTechMarketLevel()));
        for (int i = 0; i < itemPickerNum; i++) {
            if (!randomWeaponPicker.isEmpty()) {
                String itemID = randomWeaponPicker.pickAndRemove();
                // Only need 1 of each Blueprint
                cargo.addSpecial(new SpecialItemData(Items.WEAPON_BP, itemID), 1);
            }
        }
    }

    protected void addWingsBlueprints() {
        CargoAPI cargo = getCargo();
        List<FighterWingSpecAPI> fighterSpecs = Global.getSettings().getAllFighterWingSpecs();
        WeightedRandomPicker<String> randomFighterPicker = new WeightedRandomPicker<>(itemGenRandom);

        for (FighterWingSpecAPI spec : fighterSpecs) {
            // Check if this is not a blueprint that should drop etc
            if (!spec.hasTag("rare_bp") || spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.NO_BP_DROP)) {
                continue;
            }
            randomFighterPicker.add(spec.getId());
        }

        // Now make our Blueprints
        int itemPickerNum = Math
                .round((((float) randomFighterPicker.getItems().size() / 5f) * (float) getTechMarketLevel()));
        for (int i = 0; i < itemPickerNum; i++) {
            if (!randomFighterPicker.isEmpty()) {
                String itemID = randomFighterPicker.pickAndRemove();
                // Only need 1 of each Blueprint
                cargo.addSpecial(new SpecialItemData(Items.FIGHTER_BP, itemID), 1);
            }
        }
    }

    protected void addShipsBlueprints() {
        CargoAPI cargo = getCargo();
        List<ShipHullSpecAPI> hullSpecs = Global.getSettings().getAllShipHullSpecs();
        WeightedRandomPicker<String> randomHullPicker = new WeightedRandomPicker<>(itemGenRandom);

        for (ShipHullSpecAPI spec : hullSpecs) {
            // Check if this is not a blueprint that should drop etc
            if (!spec.hasTag("rare_bp") || spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.NO_BP_DROP)) {
                continue;
            }
            randomHullPicker.add(spec.getHullId());
        }

        // Now make our Blueprints
        int itemPickerNum = Math
                .round((((float) randomHullPicker.getItems().size() / 5f) * (float) getTechMarketLevel()));
        for (int i = 0; i < itemPickerNum; i++) {
            if (!randomHullPicker.isEmpty()) {
                String itemID = randomHullPicker.pickAndRemove();
                // Only need 1 of each Blueprint
                cargo.addSpecial(new SpecialItemData(Items.SHIP_BP, itemID), 1);
            }
        }
    }
}
