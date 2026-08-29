package dev.chaosrig.utils;

import dev.chaosrig.ChaosRigApi;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class DamageTypes {
    public static final RegistryKey<DamageType> ELECTRIC_SHOCK = register("electric_shock");

    public static void register() {}

    public static RegistryKey<DamageType> register(String name) {
        return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(ChaosRigApi.MAIN_MOD_ID, name));
    }
}
