package chickentechshop.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import chickentechshop.campaign.world.ChickenStation;

/**
 * The Tech Market level and the credits towards the next level.
 * Shared by all of Chicken's submarkets, kept in the sector memory.
 */
public class ChickenMarketLevel {

    public static final String LEVEL_KEY = "$cts_techMarketLevel";
    public static final String CREDITS_KEY = "$cts_techMarketCredits";
    public static final int MAX_LEVEL = 5;
    private static final int[] LEVEL_COSTS = { 100000, 150000, 200000, 250000 };

    private static MemoryAPI memory() {
        return Global.getSector().getMemoryWithoutUpdate();
    }

    public static boolean isSet() {
        return memory().contains(LEVEL_KEY);
    }

    public static int getLevel() {
        if (!memory().contains(LEVEL_KEY)) {
            return 1;
        }
        return clamp(memory().getInt(LEVEL_KEY));
    }

    // Tech Market can be 1 to 5 inclusive
    public static void setLevel(int newLevel) {
        memory().set(LEVEL_KEY, clamp(newLevel));
    }

    public static int getCredits() {
        if (!memory().contains(CREDITS_KEY)) {
            return 0;
        }
        return memory().getInt(CREDITS_KEY);
    }

    public static void setCredits(int credits) {
        memory().set(CREDITS_KEY, Math.max(0, credits));
    }

    // Adding credits is the main way we increase our TechLevel
    public static void addCredits(int credits) {
        int level = getLevel();
        // The max level is 5
        if (level >= MAX_LEVEL) {
            return;
        }

        int current = getCredits() + credits;
        if (current >= LEVEL_COSTS[level - 1]) {
            current -= LEVEL_COSTS[level - 1];
            setLevel(level + 1);
            setCredits(current);
            refreshShops();
        } else {
            setCredits(current);
        }
    }

    public static String toNextLevelCreditsString() {
        int level = getLevel();
        if (level >= MAX_LEVEL) {
            return "";
        }
        return getCredits() + "/" + LEVEL_COSTS[level - 1];
    }

    // Restock every one of Chicken's submarkets now
    public static void refreshShops() {
        MarketAPI market = ChickenStation.getMarket();
        if (market == null) {
            return;
        }
        for (String id : ChickenStation.SHOP_IDS) {
            SubmarketAPI submarket = market.getSubmarket(id);
            if (submarket != null && submarket.getPlugin() instanceof TechMarket) {
                ((TechMarket) submarket.getPlugin()).updateCargoForce();
            }
        }
    }

    // Take items that are no longer sold off Chicken's shelves (run on every load)
    public static void removeUnlistedItems() {
        MarketAPI market = ChickenStation.getMarket();
        if (market == null) {
            return;
        }
        for (String id : ChickenStation.SHOP_IDS) {
            SubmarketAPI submarket = market.getSubmarket(id);
            if (submarket != null && submarket.getPlugin() instanceof TechMarket) {
                ((TechMarket) submarket.getPlugin()).removeUnlistedItems();
            }
        }
    }

    private static int clamp(int level) {
        return Math.max(1, Math.min(MAX_LEVEL, level));
    }
}
