package dev.chaosrig;

import dev.chaosrig.block.ChaosRigBlocks;
import dev.chaosrig.command.CraftUsCommands;
import dev.chaosrig.gamefunc.StayTogetherManager;
import dev.chaosrig.item.ChaosRigItems;
import dev.chaosrig.utils.DamageTypes;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChaosRig implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(ChaosRigApi.MAIN_MOD_ID);
	private static boolean init = false;

	@Override
	public void onInitialize() {
		LOGGER.info("ChaosRig init...");
		CraftUsCommands.register();
		ChaosRigItems.register();
		ChaosRigBlocks.register();
		StayTogetherManager.register();
		DamageTypes.register();
		init = true;
	}

	public static boolean isInit() {
		return init;
	}
}