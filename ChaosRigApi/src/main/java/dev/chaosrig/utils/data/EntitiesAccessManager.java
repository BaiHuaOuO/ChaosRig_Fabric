package dev.chaosrig.utils.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.chaosrig.ChaosRigApi;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class EntitiesAccessManager extends PersistentConsumer {
    protected final List<EntityAccessData> entityAccesses = Collections.synchronizedList(new ArrayList<>());

    public EntitiesAccessManager() {
        super(false);
    }

    public static EntitiesAccessManager createFromNbt(NbtCompound nbt) {
        EntitiesAccessManager e = new EntitiesAccessManager();
        e.readNbt(nbt);
        return e;
    }

    public static EntitiesAccessManager getOrCreate(@NotNull MinecraftServer server,
                                                    @NotNull String name) {
        PersistentStateManager stateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return stateManager.getOrCreate(EntitiesAccessManager::createFromNbt, EntitiesAccessManager::new, name);
    }

    @Nullable
    protected EntityAccessData getEntity(@NotNull UUID owner) {
        return entityAccesses.stream().filter(d -> d.isOwner(owner)).findFirst().orElse(null);
    }

    @Nullable
    protected EntityAccessData tryWork(@NotNull UUID owner, @NotNull Consumer<EntityAccessData> process) {
        EntityAccessData data = this.getEntity(owner);
        if (data == null) {
            return null;
        }
        process.accept(data);
        this.markDirty();
        this.markShouldSync();
        return data;
    }

    /**
     * <p>创建一个{@link EntityAccessData}</p>
     * @param owner 目标拥有者
     * @apiNote 调用此方法, 调用方函数内容结束后必须调用一次{@link PersistentConsumer#markUpdate()}
     */
    public void create(@NotNull UUID owner, @Nullable Consumer<EntityAccessData> process) {
        EntityAccessData data = this.getEntity(owner);
        if (data == null) {
            data = new EntityAccessData(owner);
            this.entityAccesses.add(data);
        }
        if (process != null) process.accept(data);
        this.markDirty();
        this.markShouldSync();
    }

    public boolean contains(@NotNull UUID owner, @NotNull UUID target) {
        EntityAccessData data = this.getEntity(owner);
        return data != null && data.contains(target);
    }

    public void add(@NotNull UUID owner, @NotNull UUID target) {
        this.tryWork(owner, data -> data.add(target));
    }

    public void set(@NotNull UUID owner, @NotNull List<UUID> targets) {
        this.tryWork(owner, data -> data.set(targets));
    }

    public void remove(@NotNull UUID owner) {
        this.entityAccesses.removeIf(data -> data.isOwner(owner));
    }

    public void remove(@NotNull UUID owner, @NotNull UUID target) {
        this.tryWork(owner, data -> data.remove(target));
    }

    public void readNbt(NbtCompound nbt) {
        String name = "data";
        if (!nbt.contains(name)) {
            return;
        }
        NbtElement instance = nbt.get(name);
        if (instance instanceof NbtList nbtList) {
            for (NbtElement dataNbt : nbtList.stream().toList()) {
                Pair<EntityAccessData, NbtElement> pair = EntityAccessData.CODEC.decode(NbtOps.INSTANCE, dataNbt).resultOrPartial(ChaosRigApi.LOGGER::error).orElse(null);
                if (pair == null) {
                    continue;
                }
                EntityAccessData data = pair.getFirst();
                this.entityAccesses.add(data);
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList dataList = new NbtList();
        for (EntityAccessData data : this.entityAccesses) {
            NbtElement dataNbt = EntityAccessData.CODEC.encodeStart(NbtOps.INSTANCE, data).resultOrPartial(ChaosRigApi.LOGGER::error).orElse(null);
            dataList.add(dataNbt);
        }
        nbt.put("data", dataList);
        return nbt;
    }

    @Override
    public void syncData(@Nullable World world) {
        if (world instanceof ServerWorld serverWorld) {
            PacketByteBuf packet = PacketByteBufs.create();
            packet.encodeAsJson(EntityAccessData.CODEC.listOf(), this.entityAccesses);

        }
    }

    @Override
    public void tickUpdate(@Nullable World world) {}

    @Override
    public boolean shouldTickUpdate() {
        return false;
    }
}
