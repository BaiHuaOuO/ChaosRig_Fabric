package dev.chaosrig;

import dev.chaosrig.datagen.ChaosRigEnUKProvider;
import dev.chaosrig.datagen.ChaosRigZhCNProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class ChaosRigDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(ChaosRigEnUKProvider::new);
		pack.addProvider(ChaosRigZhCNProvider::new);
	}
}
