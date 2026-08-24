package dev.chaosrig.item;

import dev.chaosrig.ChaosRigApi;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ChaosRigItems {

    public static void register() {}

    public static Item register(Item item, String id) {
        return Registry.register(Registries.ITEM, Identifier.of(ChaosRigApi.MAIN_MOD_ID, id), item);
    }

}
