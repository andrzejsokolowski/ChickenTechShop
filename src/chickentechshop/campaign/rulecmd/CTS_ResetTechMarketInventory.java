package chickentechshop.campaign.rulecmd;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.util.Misc;

import chickentechshop.campaign.submarkets.ChickenMarketLevel;

import java.util.List;
import java.util.Map;

/**
 * Checks the player level
 */
public class CTS_ResetTechMarketInventory extends BaseCommandPlugin {

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Misc.Token> params,
            final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) {
            return false;
        }
        ChickenMarketLevel.refreshShops();

        return true;
    }

}