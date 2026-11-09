package chickentechshop.campaign.rulecmd;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import chickentechshop.campaign.intel.missions.chicken.ChickenQuestUtils;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;

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

        ChickenQuestUtils.openShopAndContact(ChickenQuestUtils.getChickenMarket(), dialog.getTextPanel());

        return true;
    }

}