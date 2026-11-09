package chickentechshop.campaign.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 * Chicken's own station, next to Nexerelin's Prism Freeport in Sitnalta.
 * Without Prism Freeport it goes next to Nova Maxios, then next to a random
 * independent market. Built the same way as the Extra Submarkets stations.
 */
public class ChickenStation {

    public static Logger log = Global.getLogger(ChickenStation.class);

    public static final String STATION_ID = "cts_chicken_station";
    public static final String MARKET_ID = "cts_chicken_station_market";
    public static final String STATION_NAME = "Chicken's Roost";
    public static final String STATION_TYPE = "station_side05";

    // "chicken_market" is the old single Tech Market id, kept for the Specials
    public static final String SHOP_SPECIALS = "chicken_market";
    public static final String SHOP_BLUEPRINTS = "chicken_market_blueprints";
    public static final String SHOP_MODSPECS = "chicken_market_modspecs";
    public static final String SHOP_CORES = "chicken_market_cores";
    public static final String[] SHOP_IDS = { SHOP_SPECIALS, SHOP_BLUEPRINTS, SHOP_MODSPECS, SHOP_CORES };

    // Distance kept from the station or planet the Roost is placed next to
    private static final float GAP = 350f;

    public static MarketAPI getMarket() {
        SectorEntityToken station = Global.getSector().getEntityById(STATION_ID);
        if (station == null) {
            return null;
        }
        return station.getMarket();
    }

    // The station market, created on first call
    public static MarketAPI getOrCreateMarket() {
        MarketAPI market = getMarket();
        if (market != null) {
            return market;
        }

        SectorEntityToken anchor = pickAnchor();
        if (anchor == null || anchor.getContainingLocation() == null) {
            log.error("Could not find a place for Chicken's station!");
            return null;
        }
        return createStation(anchor);
    }

    // Add every Chicken submarket the station doesn't have yet
    public static void addShops(MarketAPI market) {
        for (String id : SHOP_IDS) {
            if (!market.hasSubmarket(id)) {
                market.addSubmarket(id);
            }
        }
    }

    // Choices are (in order)
    // 1. Prism Freeport (Nexerelin, in Sitnalta)
    // 2. Nova Maxios
    // 3. Random Independent market
    // 4. Any market
    private static SectorEntityToken pickAnchor() {
        SectorEntityToken entity = Global.getSector().getEntityById("nex_prismFreeport");
        if (entity != null) {
            return entity;
        }
        entity = Global.getSector().getEntityById("new_maxios");
        if (entity != null) {
            return entity;
        }

        List<MarketAPI> allMarkets = Global.getSector().getEconomy().getMarketsCopy();
        List<MarketAPI> candidates = new ArrayList<MarketAPI>();
        for (MarketAPI m : allMarkets) {
            if (Factions.INDEPENDENT.equals(m.getFactionId()) && m.getPrimaryEntity() != null && !m.isHidden()) {
                candidates.add(m);
            }
        }
        if (candidates.isEmpty()) {
            for (MarketAPI m : allMarkets) {
                if (m.getPrimaryEntity() != null && !m.isHidden()) {
                    candidates.add(m);
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(new Random().nextInt(candidates.size())).getPrimaryEntity();
    }

    private static MarketAPI createStation(SectorEntityToken anchor) {
        LocationAPI location = anchor.getContainingLocation();
        SectorEntityToken station = location.addCustomEntity(STATION_ID, STATION_NAME, STATION_TYPE,
                Factions.INDEPENDENT);

        // A station on an orbit (like Prism Freeport) gets the same orbit, a little ahead of it.
        // A planet gets the station in its orbit.
        SectorEntityToken focus = anchor.getOrbitFocus();
        float radius = anchor.getCircularOrbitRadius();
        boolean anchorIsPlanet = anchor instanceof PlanetAPI;
        if (!anchorIsPlanet && focus != null && radius > GAP) {
            float angleOffset = (float) Math.toDegrees(GAP / radius);
            float period = anchor.getCircularOrbitPeriod();
            if (period <= 0f) {
                period = radius / 10f;
            }
            station.setCircularOrbitPointingDown(focus, anchor.getCircularOrbitAngle() + angleOffset, radius,
                    period);
        } else {
            float orbitRadius = anchor.getRadius() + GAP;
            station.setCircularOrbitPointingDown(anchor, new Random().nextFloat() * 360f, orbitRadius,
                    orbitRadius / 10f);
        }

        // Market Setup
        MarketAPI market = Global.getFactory().createMarket(MARKET_ID, STATION_NAME, 3);
        market.setSize(3);
        market.setHidden(false);

        market.setFactionId(Factions.INDEPENDENT);
        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);

        market.setEconGroup(null);
        market.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true);
        market.getMemoryWithoutUpdate().set("$nex_uninvadable", true);

        market.addCondition(Conditions.POPULATION_3);
        market.addIndustry(Industries.POPULATION);
        market.addIndustry(Industries.SPACEPORT);

        market.addSubmarket(Submarkets.SUBMARKET_OPEN);
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE);

        market.setInvalidMissionTarget(true);
        market.addTag(Tags.MARKET_NO_OFFICER_SPAWN);

        market.getTariff().modifyFlat("default_tariff", market.getFaction().getTariffFraction());

        market.setPrimaryEntity(station);
        station.setMarket(market);
        station.setFaction(Factions.INDEPENDENT);

        market.reapplyIndustries();
        Global.getSector().getEconomy().addMarket(market, true);

        log.info("Created Chicken's station in " + location.getName() + " next to " + anchor.getName());
        return market;
    }
}
