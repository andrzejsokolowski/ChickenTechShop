package chickentechshop.campaign.intel.missions.chicken;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;

import chickentechshop.ChickenTechShop;
import chickentechshop.campaign.intel.TechMarketContact;
import chickentechshop.campaign.submarkets.ChickenMarketLevel;
import chickentechshop.campaign.world.ChickenStation;
import lunalib.lunaSettings.LunaSettings;

public class ChickenQuestUtils {

	public static final String PERSON_CHICKEN = "chicken";

	public static void createChicken(MarketAPI market) {
		PersonAPI person = Global.getFactory().createPerson();
		person.setId(PERSON_CHICKEN);
		person.setImportance(PersonImportance.HIGH);
		person.setVoice(Voices.SPACER);
		person.setFaction(Factions.INDEPENDENT);
		person.setGender(FullName.Gender.FEMALE);
		person.setRankId(Ranks.UNKNOWN);
		person.setPostId(Ranks.POST_CITIZEN);
		person.getName().setFirst("Chicken");
		person.getName().setLast("");
		person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "chicken"));
		person.addTag("chicken");
		// person.addTag(Tags.CONTACT_TRADE);
		// person.addTag(Tags.CONTACT_UNDERWORLD);
		Global.getSector().getImportantPeople().addPerson(person);
		market.addPerson(person);
	}

	// Open all of Chicken's submarkets on her station and add her as a contact.
	// Runs once; also used by the old intro dialog (CTS_CreateSubmarketAndChickenContact).
	public static void openShopAndContact(MarketAPI market, TextPanelAPI textPanel) {
		MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
		boolean firstTime = !sectorMemory.getBoolean(ChickenTechShop.INTRO_COMPLETE_KEY);

		// Apply configured starting level if LunaLib is present
		if (firstTime && !ChickenMarketLevel.isSet()
				&& Global.getSettings().getModManager().isModEnabled("lunalib")) {
			Integer startingLevel = LunaSettings.getInt(ChickenTechShop.MOD_ID,
					ChickenTechShop.SETTING_STARTING_LEVEL);
			if (startingLevel != null && startingLevel > 1) {
				ChickenMarketLevel.setLevel(startingLevel);
			}
		}
		ChickenStation.addShops(market);

		// Add Chicken as a contact
		PersonAPI chicken = Global.getSector().getImportantPeople().getPerson(PERSON_CHICKEN);
		if (chicken != null && !Global.getSector().getIntelManager().hasIntelOfClass(TechMarketContact.class)) {
			market.getCommDirectory().addPerson(chicken);
			BaseMissionHub.set(chicken, new BaseMissionHub(chicken));
			chicken.getMemoryWithoutUpdate().set(BaseMissionHub.NUM_BONUS_MISSIONS, 1);
			TechMarketContact intel = new TechMarketContact(chicken, market);
			if (textPanel != null) {
				Global.getSector().getIntelManager().addIntel(intel, false, textPanel);
			} else {
				Global.getSector().getIntelManager().addIntel(intel, true);
			}
		}

		sectorMemory.set(ChickenTechShop.INTRO_COMPLETE_KEY, true);
	}

	public static MarketAPI getChickenMarket() {
		return Global.getSector().getImportantPeople().getPerson(ChickenQuestUtils.PERSON_CHICKEN).getMarket();
	}
}