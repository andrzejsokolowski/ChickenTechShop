package chickentechshop.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;

import chickentechshop.campaign.intel.missions.chicken.ChickenQuestUtils;

import java.util.Arrays;

import org.apache.log4j.Logger;

/**
 * Base of all of Chicken's submarkets: shared level, prices, no sales.
 * The subclasses decide what each submarket stocks.
 *
 * Saves from before 0.3.0 hold a plain TechMarket as the "chicken_market"
 * submarket, so this class keeps its old fields and stays loadable.
 */
public class TechMarket extends BaseSubmarketPlugin {

    public static RepLevel MIN_STANDING = RepLevel.VENGEFUL;
    public static Logger log = Global.getLogger(TechMarket.class);

    // Only read when moving a pre-0.3.0 save over, the level now lives in ChickenMarketLevel
    private int techMarketLevel = 1;
    private int currentCredits = 0;
    private int[] levelCosts = { 100000, 150000, 200000, 250000 };

    // Not in Items on 0.98a
    public static final String MODSPEC_ID = "modspec";

    // Special items that are used through a dialog or the refit screen, so the
    // checks in isUsableSpecialItem() can't see them
    protected static final String[] EXTRA_USABLE_ITEM_IDS = { "wormhole_anchor", "wormhole_scanner",
            "shrouded_substrate", "shrouded_thunderhead", "shrouded_mantle", "shrouded_lens" };

    // Hardcoded for DIY planets, as they dont have any tags
    protected static final String[] DIY_PLANETS_ITEM_IDS = { "atmo_mineralizer", "atmo_sublimator",
            "solar_reflector", "tectonic_attenuator", "weather_core", "climate_sculptor", "gravity_oscillator",
            "rad_remover" };

    // Template items that only work with a target (ship, weapon, hull mod...)
    // Blueprints and modspecs are added on their own
    protected static final String[] TEMPLATE_ITEM_IDS = { MODSPEC_ID, Items.SHIP_BP, Items.WEAPON_BP,
            Items.FIGHTER_BP, Items.INDUSTRY_BP };

    public int getLegacyLevel() {
        return techMarketLevel;
    }

    public int getLegacyCredits() {
        return currentCredits;
    }

    public int getTechMarketLevel() {
        return ChickenMarketLevel.getLevel();
    }

    public void setTechMarketLevel(int newLevel) {
        ChickenMarketLevel.setLevel(newLevel);
    }

    public void addCreditsToTechMarket(int credits) {
        ChickenMarketLevel.addCredits(credits);
    }

    public String ToNextLevelCreditsString() {
        return ChickenMarketLevel.toNextLevelCreditsString();
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
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
        cargo.clear();
        cargo.getMothballedShips().clear();
        cargo.removeEmptyStacks();
        addStock();
        cargo.sort();
    }

    // What this submarket sells, filled in by the subclasses
    protected void addStock() {
    }

    // Quantity for items you use up: 1 to market level, clamp to 3
    protected int randomQuantity() {
        return Math.min(itemGenRandom.nextInt(getTechMarketLevel()) + 1, 3);
    }

    // ==========================================================================
    // Special item checks, shared by the Specials and Blueprints submarkets
    // ==========================================================================

    public static boolean isBlueprintPackage(SpecialItemSpecAPI spec) {
        for (String tag : spec.getTags()) {
            if (tag.endsWith("package_bp")) {
                return true;
            }
        }
        return false;
    }

    // Colony items, blueprint packages, items with a right-click use and the
    // hardcoded lists above. Quest items and template items are left out.
    public static boolean isUsableSpecialItem(SpecialItemSpecAPI spec) {
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
        PersonAPI chicken = Global.getSector().getImportantPeople().getPerson(ChickenQuestUtils.PERSON_CHICKEN);
        if (chicken == null) {
            return 0.5f;
        }
        RepLevel chicken_repLevel = chicken.getRelToPlayer().getLevel();
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
        if (getTechMarketLevel() == ChickenMarketLevel.MAX_LEVEL) {
            return "The Tech market is at the Max Level 5";
        }
        return "The Tech Market is currently at Level " + getTechMarketLevel() + ".\nThe next level will be reach in "
                + ToNextLevelCreditsString() + " credits";
    }

    @Override
    public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
        Highlights highlights = new Highlights();
        highlights.append("Level " + getTechMarketLevel(), Misc.getHighlightColor());
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
