package dev.chaosrig.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class ChaosRigApiZHCNProvider extends FabricLanguageProvider {

    public ChaosRigApiZHCNProvider(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generateTranslations(TranslationBuilder t) {
        t.add("key.chaosrig.api.ping", "标记");
        t.add("chaosrig.api.ping.camera.tip", "不允许自由视角下进行Ping标记");
        t.add("chaosrig.api.ping.nothing", "无事可报或距离过远");
    }
}
