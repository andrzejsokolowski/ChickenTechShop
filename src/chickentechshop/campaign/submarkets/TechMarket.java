package chickentechshop.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import chickentechshop.campaign.intel.missions.chicken.ChickenQuestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

public class TechMarket extends BaseSubmarketPlugin {

    public static RepLevel MIN_STANDING = RepLevel.VENGEFUL;
    public static Logger log = Global.getLogger(TechMarket.class);

    private int techMarketLevel = 1;
    private int currentCredits = 0;
    private int[] levelCosts = { 100000, 150000, 200000, 250000 };

    public int getTechMarketLevel() {
        return techMarketLevel;
    }

    // Tech Market can be 1 to 5 inclusive
    public void setTechMarketLevel(int newLevel) {
        if (newLevel < 1) {
            techMarketLevel = 1;
        } else if (newLevel > 5) {
            techMarketLevel = 5;
        } else {
            techMarketLevel = newLevel;
        }
    }

    // Adding credits is the main way we increase our TechLevel
    public void addCreditsToTechMarket(int credits) {
        // The max level is 5
        if (techMarketLevel >= 5) {
            return;
        }

        currentCredits += credits;
        if (currentCredits >= levelCosts[getTechMarketLevel() - 1]) {
            currentCredits -= levelCosts[getTechMarketLevel() - 1];
            setTechMarketLevel(getTechMarketLevel() + 1);
            updateCargoForce();
        }
    }

    public String ToNextLevelCreditsString() {
        if (getTechMarketLevel() == 5) {
            return "";
        }
        return currentCredits + "/" + levelCosts[getTechMarketLevel() - 1];
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        // log.info("Days since update: " + sinceLastCargoUpdate);
        if (sinceLastCargoUpdate < 30)
            return;
        sinceLastCargoUpdate = 0f;
        updateCargo();
    }

    // Force update the Marketplace
    public void updateCargoForce() {
        sinceLastCargoUpdate = 0f;
        updateCargo();
    }

    public void updateCargo() {
        CargoAPI cargo = getCargo();

        // clear inventory
        for (CargoStackAPI s : cargo.getStacksCopy()) {
            float qty = s.getSize();
            cargo.removeItems(s.getType(), s.getData(), qty);
        }
        cargo.removeEmptyStacks();
        addSpecialTech();
        addAICores();
        addBlueprints();
        cargo.sort();
    }

    // Special items that are used through a dialog or the refit screen, so the
    // checks in isUsableSpecialItem() can't see them
    protected static final String[] EXTRA_USABLE_ITEM_IDS = { "wormhole_anchor", "wormhole_scanner",
            "shrouded_substrate", "shrouded_thunderhead", "shrouded_mantle", "shrouded_lens" };

    // Hardcoded for DIY planets, as they dont have any tags
    protected static final String[] DIY_PLANETS_ITEM_IDS = { "atmo_mineralizer", "atmo_sublimator",
            "solar_reflector", "tectonic_attenuator", "weather_core", "climate_sculptor", "gravity_oscillator",
            "rad_remover" };

    // Not in Items on 0.98a
    protected static final String MODSPEC_ID = "modspec";

    // Template items that only work with a target (ship, weapon, hull mod...)
    // Blueprints and modspecs are added on their own
    protected static final String[] TEMPLATE_ITEM_IDS = { MODSPEC_ID, Items.SHIP_BP, Items.WEAPON_BP,
            Items.FIGHTER_BP, Items.INDUSTRY_BP };

    // addSpecialTech stocks every special item the player can use: colony items,
    // blueprint packages, items with a right-click use, DIY planet items and
    // modspecs for every hull mod the player doesn't know yet.
    // The market level sets how many of each item are in stock.
    protected void addSpecialTech() {
        CargoAPI cargo = getCargo();

        for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs()) {
            if (!isUsableSpecialItem(spec)) {
                continue;
            }
            // Items you learn from only need 1, the rest get 1 to market level, clamp to 3
            int quantity = 1;
            if (!isBlueprintPackage(spec)) {
                quantity = Math.min(itemGenRandom.nextInt(techMarketLevel) + 1, 3);
            }
            log.info("Adding special item " + spec.getId() + " with quantity " + quantity);
            cargo.addSpecial(new SpecialItemData(spec.getId(), null), quantity);
        }

        addModSpecs();
    }

    protected boolean isUsableSpecialItem(SpecialItemSpecAPI spec) {
        String id = spec.getId();
        if (Arrays.asList(TEMPLATE_ITEM_IDS).contains(id) || spec.hasTag("single_bp")) {
            return false;
        }
        if (Arrays.asList(EXTRA_USABLE_ITEM_IDS).contains(id) || Arrays.asList(DIY_PLANETS_ITEM_IDS).contains(id)) {
            return true;
        }
        // Quest items (Janus Device, the inert planetkiller...) stay with their quests
        if (spec.hasTag("mission_item")) {
            return false;
        }
        // Colony items, including modded ones that only register an effect
        boolean colonyItem = spec.hasTag("colony_item") || ItemEffectsRepo.ITEM_EFFECTS.containsKey(id);
        boolean usable = colonyItem || isBlueprintPackage(spec);

        // Check that the item works without a target, and look for a right-click use
        try {
            CargoStackAPI stack = Global.getFactory().createCargoStack(CargoItemType.SPECIAL,
                    new SpecialItemData(id, null), null);
            SpecialItemPlugin plugin = spec.getNewPluginInstance(stack);
            if (plugin == null || plugin.getName() == null) {
                return false;
            }
            if (plugin.hasRightClickAction()) {
                usable = true;
            }
        } catch (Throwable t) {
            log.info("Skipping special item " + id + ", it doesn't work without a target");
            return false;
        }
        return usable;
    }

    protected boolean isBlueprintPackage(SpecialItemSpecAPI spec) {
        for (String tag : spec.getTags()) {
            if (tag.endsWith("package_bp")) {
                return true;
            }
        }
        return false;
    }

    // One modspec for every hull mod the player can learn and doesn't know yet
    protected void addModSpecs() {
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

    // Add a number of Gamma/Beta/Alpha Cores
    // Beta Cores "unlock" at Market Level 2
    // Alpha Cores "unlock" at Market Level 4
    protected void addAICores() {
        CargoAPI cargo = getCargo();
        Random random = new Random();

        // Add Gammas
        int quantityGamma = random.nextInt(techMarketLevel) + 1;
        cargo.addCommodity("gamma_core", quantityGamma);

        // Add Betas
        if (techMarketLevel >= 2) {
            int quantityBeta = random.nextInt(techMarketLevel - 1) + 1;
            cargo.addCommodity("beta_core", quantityBeta);
        }

        // Add Alphas
        if (techMarketLevel >= 4) {
            int quantityAlpha = random.nextInt(techMarketLevel - 3) + 1;
            cargo.addCommodity("alpha_core", quantityAlpha);
        }

    }

    protected void addBlueprints() {
        addWeaponBlueprints();
        addWingsBlueprints();
        addShipsBlueprints();
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
            // Check if player already knows this weapon?
            // if (Global.getSector().getPlayerFaction().knowsWeapon(spec.getWeaponId())) {
            // continue;
            // }
            // Add the data to our picker
            randomWeaponPicker.add(spec.getWeaponId());
        }

        // Now make our Blueprints
        int itemPickerNum = Math.round((((float) randomWeaponPicker.getItems().size() / 5f) * (float) techMarketLevel));
        // log.info("randomWeaponPicker has " + randomWeaponPicker.getItems().size());
        // log.info("num picked for weapons is " + itemPickerNum);
        for (int i = 0; i < itemPickerNum; i++) {
            if (!randomWeaponPicker.isEmpty()) {
                String itemID = randomWeaponPicker.pickAndRemove();
                // Only need 1 of each Blueprint
                // log.info("Trying to add Weapon blueprint for " + itemID);
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
            // Check if player already knows this weapon?
            // if (Global.getSector().getPlayerFaction().knowsWeapon(spec.getWeaponId())) {
            // continue;
            // }
            // Add the data to our picker
            randomFighterPicker.add(spec.getId());
        }

        // Now make our Blueprints
        int itemPickerNum = Math
                .round((((float) randomFighterPicker.getItems().size() / 5f) * (float) techMarketLevel));
        // log.info("randomFighterPicker has " + randomFighterPicker.getItems().size());
        // log.info("num picked for fighters is " + itemPickerNum);
        for (int i = 0; i < itemPickerNum; i++) {
            if (!randomFighterPicker.isEmpty()) {
                String itemID = randomFighterPicker.pickAndRemove();
                // Only need 1 of each Blueprint
                // log.info("Trying to add Fighter blueprint for " + itemID);
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
            // Check if player already knows this weapon?
            // if (Global.getSector().getPlayerFaction().knowsWeapon(spec.getWeaponId())) {
            // continue;
            // }
            // Add the data to our picker
            randomHullPicker.add(spec.getHullId());
        }

        // Now make our Blueprints
        int itemPickerNum = Math.round((((float) randomHullPicker.getItems().size() / 5f) * (float) techMarketLevel));
        // log.info("randomHullPicker has " + randomHullPicker.getItems().size());
        // log.info("num picked for hulls is " + itemPickerNum);
        for (int i = 0; i < itemPickerNum; i++) {
            if (!randomHullPicker.isEmpty()) {
                String itemID = randomHullPicker.pickAndRemove();
                // Only need 1 of each Blueprint
                // log.info("Trying to add Hull blueprint for " + itemID);
                cargo.addSpecial(new SpecialItemData(Items.SHIP_BP, itemID), 1);
            }
        }

    }

    // ==========================================================================
    // ==========================================================================

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL)
            return true;
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL)
            return true;
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL)
            return true;
        return false;
    }

    @Override
    public float getTariff() {
        RepLevel chicken_repLevel = Global.getSector().getImportantPeople().getPerson(ChickenQuestUtils.PERSON_CHICKEN)
                .getRelToPlayer().getLevel();
        float mult;
        switch (chicken_repLevel) {
            case NEUTRAL:
                mult = 0.5f;
                break;
            case FAVORABLE:
                mult = 0.4f;
                break;
            case WELCOMING:
                mult = 0.3f;
                break;
            case FRIENDLY:
                mult = 0.2f;
                break;
            case COOPERATIVE:
                mult = 0.1f;
                break;
            default:
                mult = 0.5f;
        }
        return mult;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "No sales/returns";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "No sales/returns";
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        if (getTechMarketLevel() == 5) {
            return "The Tech market is at the Max Level 5";
        }
        return "The Tech Market is currently at Level " + techMarketLevel + ".\nThe next level will be reach in "
                + ToNextLevelCreditsString() + " credits";
    }

    @Override
    public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
        Highlights highlights = new Highlights();
        highlights.append("Level " + techMarketLevel, Misc.getHighlightColor());
        highlights.append(ToNextLevelCreditsString() + " Credits" + "", Misc.getHighlightColor());
        return highlights;
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        RepLevel level = submarket.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        return level.isAtWorst(MIN_STANDING);
    }

    @Override
    public boolean isBlackMarket() {
        return false;
    }

}