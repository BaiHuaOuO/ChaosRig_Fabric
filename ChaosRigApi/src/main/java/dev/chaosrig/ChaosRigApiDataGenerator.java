package dev.chaosrig;

import dev.chaosrig.datagen.ChaosRigApiENUKProvider;
import dev.chaosrig.datagen.ChaosRigApiZHCNProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class ChaosRigApiDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ChaosRigApiZHCNProvider::new);
        pack.addProvider(ChaosRigApiENUKProvider::new);
    }
}
