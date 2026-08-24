package dev.chaosrig.block;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsertionGateBlock implements BlockEntityProvider {
    public static final EnumProperty<InsertionGatePart> PART = EnumProperty.of("part", InsertionGatePart.class);

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    public enum InsertionGatePart implements StringIdentifiable {
        PART_1("part1"),
        PART_2("part2"),
        PART_3("part3"),
        PART_4("part4");

        private final String name;

        InsertionGatePart(@NotNull String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }
}
