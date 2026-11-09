package chickentechshop;

import org.apache.log4j.Logger;

import lunalib.lunaSettings.LunaSettings;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.intel.misc.BreadcrumbIntel;

import chickentechshop.campaign.intel.TechMarketContact;
import chickentechshop.campaign.intel.missions.chicken.ChickenQuestUtils;
import chickentechshop.campaign.submarkets.ChickenMarketLevel;
import chickentechshop.campaign.submarkets.TechMarket;
import chickentechshop.campaign.world.ChickenStation;

public class ChickenTechShop extends BaseModPlugin {

    public static Logger log = Global.getLogger(ChickenTechShop.class);

    final static int LEVEL_REQ = 5;
    public static final String MOD_ID = "chickentechshop";
    public static final String SETTING_STARTING_LEVEL = "cts_startingLevel";
    public static final String INTRO_COMPLETE_KEY = "$CTS_ChickenIntroComplete";

    @Override
    public void onApplicationLoad() throws Exception {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            LunaSettings.SettingsCreator.addInt(
                MOD_ID,
                SETTING_STARTING_LEVEL,
                "Starting Market Level",
                "The level Chicken's Tech Market starts at (1-5). Only applies when the market is first created in a new game.",
                1,
                1,
                5
            );
            LunaSettings.SettingsCreator.refresh(MOD_ID);
        }
    }

    // Inital setup: make sure Chicken's station exists and Chicken lives there.
    // Saves from before 0.3.0 have Chicken and her single Tech Market on another
    // market (usually Prism Freeport), so move both over to the station.
    public void chickenInitialSetup() {
        MarketAPI station = ChickenStation.getOrCreateMarket();
        if (station == null) {
            // If we reach this point, all hope is lost
            log.error("Could not find a valid market to spawn Chicken at!");
            return;
        }

        PersonAPI chicken = Global.getSector().getImportantPeople().getPerson(ChickenQuestUtils.PERSON_CHICKEN);
        if (chicken == null) {
            ChickenQuestUtils.createChicken(station);
            chicken = Global.getSector().getImportantPeople().getPerson(ChickenQuestUtils.PERSON_CHICKEN);
        }

        MarketAPI oldMarket = chicken.getMarket();
        if (oldMarket != station) {
            moveChickenToStation(chicken, oldMarket, station);
        }

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(INTRO_COMPLETE_KEY)) {
            ChickenStation.addShops(station);
        }
    }

    private void moveChickenToStation(PersonAPI chicken, MarketAPI oldMarket, MarketAPI station) {
        if (oldMarket != null) {
            // Carry the old Tech Market's level over, then close it
            SubmarketAPI oldShop = oldMarket.getSubmarket(ChickenStation.SHOP_SPECIALS);
            if (oldShop != null) {
                if (oldShop.getPlugin() instanceof TechMarket && !ChickenMarketLevel.isSet()) {
                    TechMarket legacy = (TechMarket) oldShop.getPlugin();
                    ChickenMarketLevel.setLevel(legacy.getLegacyLevel());
                    ChickenMarketLevel.setCredits(legacy.getLegacyCredits());
                }
                oldMarket.removeSubmarket(ChickenStation.SHOP_SPECIALS);
            }
            // Older versions re-created Chicken on every load, so there can be several copies here
            for (PersonAPI person : oldMarket.getPeopleCopy()) {
                if (ChickenQuestUtils.PERSON_CHICKEN.equals(person.getId())) {
                    oldMarket.removePerson(person);
                    if (oldMarket.getCommDirectory() != null) {
                        oldMarket.getCommDirectory().removePerson(person);
                    }
                }
            }
        }

        TechMarketContact contact = null;
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(TechMarketContact.class)) {
            contact = (TechMarketContact) intel;
        }
        if (contact != null) {
            contact.relocateToMarket(station, false);
        } else {
            chicken.setMarket(station);
            station.addPerson(chicken);
        }
        log.info("Moved Chicken to " + station.getName());
    }

    @Override
    public void onGameLoad(boolean newGame) {
        //I'm adding this to fix the bug since chicken doesn't exist at later-game save
        //Idk if it's the correct fix since this line adds chicken
        chickenInitialSetup();
        
        PersonAPI chicken = Global.getSector().getImportantPeople().getPerson(ChickenQuestUtils.PERSON_CHICKEN);
        if (chicken == null) {
            return;
        }
        MarketAPI market = chicken.getMarket();
        final SectorAPI sector = Global.getSector();

        if (sector != null && sector.getListenerManager() != null) {
            if (market != null) {
                if (!market.hasCondition(Conditions.ABANDONED_STATION)
                        || !market.hasCondition(Conditions.DECIVILIZED)) {
                    sector.addTransientListener(new ChickenIntroCheck());
                }
            }
        }
    }

    @Override
    public void onNewGameAfterTimePass() {
        chickenInitialSetup();
        final SectorAPI sector = Global.getSector();
        PersonAPI chicken = Global.getSector().getImportantPeople().getPerson(ChickenQuestUtils.PERSON_CHICKEN);
        MarketAPI market = chicken == null ? null : chicken.getMarket();
        if (market == null) {
            sector.removeListener(new ChickenIntroCheck());
        }
        if (Global.getSector().getIntelManager().hasIntelOfClass(TechMarketContact.class)) {
            sector.removeListener(new ChickenIntroCheck());
        }
    }

    public class ChickenIntroCheck extends BaseCampaignEventListener {
        public ChickenIntroCheck() {
            super(false);
        }

        @Override
        public void reportPlayerClosedMarket(final MarketAPI market) {
            MarketAPI chickenMarket = ChickenQuestUtils.getChickenMarket().getPrimaryEntity().getMarket();
            if (Global.getSector().getCharacterData().getPerson().getStats().getLevel() >= LEVEL_REQ) {
                if (!Global.getSector().getIntelManager().hasIntelOfClass(TechMarketContact.class)) {
                    BreadcrumbIntel intel = new BreadcrumbIntel(Global.getSector().getPlayerFleet(),
                            ChickenQuestUtils.getChickenMarket().getPrimaryEntity());
                    intel.setTitle("A Message from a friend");
                    intel.setText(
                            "You recieve a message from someone named \"Chicken\" who claims he has something for you on "
                                    + chickenMarket.getName() + " in the " + chickenMarket.getStarSystem().getName());
                    Global.getSector().getIntelManager().addIntel(intel, false);
                    Global.getSector().removeListener(this);
                }
            }
        }
    }
}
