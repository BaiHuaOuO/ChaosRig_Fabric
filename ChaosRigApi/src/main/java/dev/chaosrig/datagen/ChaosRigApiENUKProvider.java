package dev.chaosrig.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class ChaosRigApiENUKProvider extends FabricLanguageProvider {

    public ChaosRigApiENUKProvider(FabricDataOutput dataOutput) {
        super(dataOutput, "en_uk");
    }

    @Override
    public void generateTranslations(TranslationBuilder t) {
        t.add("key.chaosrig.api.ping", "Ping");
        t.add("chaosrig.api.ping.camera.tip", "No allow to ping in Free Camera mode.");
        t.add("chaosrig.api.ping.nothing", "No anything can be pinged or too far away.");
    }
}
