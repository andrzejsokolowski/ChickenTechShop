package chickentechshop.campaign.intel.missions.chicken;

import com.fs.starfarer.api.impl.campaign.missions.DeadDropMission;
import chickentechshop.campaign.submarkets.ChickenMarketLevel;

public class CTS_DeadDropMission extends DeadDropMission {

	@Override
	protected void notifyEnding() {
		super.notifyEnding();

		// Add to Chickens Tech market Level
		ChickenMarketLevel.addCredits(creditReward);
	}
}
