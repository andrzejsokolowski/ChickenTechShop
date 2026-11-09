package chickentechshop.campaign.intel.missions.chicken;

import com.fs.starfarer.api.impl.campaign.missions.DisruptHeavyIndustry;
import chickentechshop.campaign.submarkets.ChickenMarketLevel;

public class CTS_DisruptHeavyIndustry extends DisruptHeavyIndustry {

	@Override
	protected void notifyEnding() {
		super.notifyEnding();

		// Add to Chickens Tech market Level
		ChickenMarketLevel.addCredits(creditReward);
	}
}