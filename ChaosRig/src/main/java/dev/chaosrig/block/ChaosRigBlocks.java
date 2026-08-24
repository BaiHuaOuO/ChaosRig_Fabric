package dev.chaosrig.block;

import dev.chaosrig.ChaosRig;
import dev.chaosrig.ChaosRigApi;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ChaosRigBlocks {
    public static final Block CAMERA = registerBlock("camera", new CameraBlock());

    public static void register() {
        if (ChaosRig.isInit()) {
            throw new RuntimeException("不允许重复注册");
        }
    }

    public static Block registerBlock(String id, Block block) {
        registerBlockItem(id, block);
        return Registry.register(Registries.BLOCK, new Identifier(ChaosRigApi.MAIN_MOD_ID, id), block);
    }

    public static void registerBlockItem(String id, Block block) {
        BlockItem item = Registry.register(Registries.ITEM, new Identifier(ChaosRigApi.MAIN_MOD_ID, id), new BlockItem(block, new Item.Settings()));
        item.appendBlocks(Item.BLOCK_ITEMS, item);
    }
}
