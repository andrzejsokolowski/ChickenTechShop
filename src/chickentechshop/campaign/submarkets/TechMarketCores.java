package chickentechshop.campaign.submarkets;

import com.fs.starfarer.api.campaign.CargoAPI;

import java.util.Random;

/**
 * Chicken's Tech - AI Cores: Gamma cores, Beta cores from market level 2,
 * Alpha cores from market level 4.
 */
public class TechMarketCores extends TechMarket {

    @Override
    protected void addStock() {
        CargoAPI cargo = getCargo();
        Random random = new Random();
        int level = getTechMarketLevel();

        // Add Gammas
        int quantityGamma = random.nextInt(level) + 1;
        cargo.addCommodity("gamma_core", quantityGamma);

        // Add Betas
        if (level >= 2) {
            int quantityBeta = random.nextInt(level - 1) + 1;
            cargo.addCommodity("beta_core", quantityBeta);
        }

        // Add Alphas
        if (level >= 4) {
            int quantityAlpha = random.nextInt(level - 3) + 1;
            cargo.addCommodity("alpha_core", quantityAlpha);
        }
    }
}
