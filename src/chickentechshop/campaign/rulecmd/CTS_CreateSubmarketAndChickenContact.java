package chickentechshop.campaign.rulecmd;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import lunalib.lunaSettings.LunaSettings;

import chickentechshop.ChickenTechShop;
import chickentechshop.campaign.intel.TechMarketContact;
import chickentechshop.campaign.intel.missions.chicken.ChickenQuestUtils;
import chickentechshop.campaign.submarkets.ChickenMarketLevel;
import chickentechshop.campaign.world.ChickenStation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.List;
import java.util.Map;

/**
 * Creates the character contact for Chicken
 * Also creates the Submarket wherever he is
 */
public class CTS_CreateSubmarketAndChickenContact extends BaseCommandPlugin {

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Misc.Token> params,
            final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) {
            return false;
        }

        // Open Chicken's submarkets wherever Chicken is (her station)
        MarketAPI market = ChickenQuestUtils.getChickenMarket();

        // Apply configured starting level if LunaLib is present
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            Integer startingLevel = LunaSettings.getInt(ChickenTechShop.MOD_ID, ChickenTechShop.SETTING_STARTING_LEVEL);
            if (startingLevel != null && startingLevel > 1) {
                ChickenMarketLevel.setLevel(startingLevel);
            }
        }
        ChickenStation.addShops(market);

        // Add Chicken as a contact
        PersonAPI chicken = Global.getSector().getImportantPeople().getPerson(ChickenQuestUtils.PERSON_CHICKEN);
        market.getCommDirectory().addPerson(chicken);
        BaseMissionHub.set(chicken, new BaseMissionHub(chicken));
        chicken.getMemoryWithoutUpdate().set(BaseMissionHub.NUM_BONUS_MISSIONS, 1);
        TechMarketContact intel = new TechMarketContact(chicken, market);
        Global.getSector().getIntelManager().addIntel(intel, false, dialog.getTextPanel());

        return true;
    }

}