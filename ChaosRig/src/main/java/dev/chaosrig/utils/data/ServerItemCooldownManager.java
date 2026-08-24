package dev.chaosrig.utils.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServerItemCooldownManager extends PersistentConsumer {
    public static Codec<ServerItemCooldownManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Timer.CODEC.listOf().optionalFieldOf("timers", Collections.synchronizedList(new ArrayList<>())).forGetter(ServerItemCooldownManager::getTimers)
    ).apply(instance, ServerItemCooldownManager::new));
    protected final List<Timer> timers;

    public ServerItemCooldownManager() {
        this(Collections.synchronizedList(new ArrayList<>()));
    }

    public ServerItemCooldownManager(List<Timer> timers) {
        super(false);
        this.timers = timers;
    }

    private List<Timer> getTimers() {
        return timers;
    }

    @Override
    public void tickUpdate(@Nullable World world) {
        Iterator<Timer> timerIterator = this.timers.iterator();
        while (timerIterator.hasNext()) {
            Timer timer = timerIterator.next();
            if (timer.isCanceled()) {
                timerIterator.remove();
                return;
            }
            timer.tick();
        }
    }

    @Override
    public void syncData(@Nullable World world) {
        DataResult<JsonElement> result = CODEC.encodeStart(JsonOps.INSTANCE, this);
        this.markSynced(world);
    }

    @Override
    public void secondUpdate() {
        this.markShouldSync();
    }

    @Override
    public boolean shouldSecondUpdate() {
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return null;
    }

    public static class Timer {
        public static final Codec<Timer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Uuids.CODEC.fieldOf("item_uuid").forGetter(Timer::getItemUuid),
                Codec.LONG.fieldOf("max_tick").forGetter(Timer::getMaxTick),
                Codec.LONG.optionalFieldOf("now_tick", 0L).forGetter(Timer::getNowTick)).apply(instance, Timer::new));
        public final UUID itemUuid;
        public final long maxTick;
        protected long nowTick = 0;
        protected boolean canceled = false;

        public Timer(@NotNull UUID itemUuid, long maxTick, long nowTick) {
            this(itemUuid, maxTick);
            this.nowTick = nowTick;
        }

        private UUID getItemUuid() {
            return this.itemUuid;
        }

        private long getMaxTick() {
            return maxTick;
        }

        public long getNowTick() {
            return nowTick;
        }

        public Timer(@NotNull UUID itemUuid, long maxTick) {
            if (maxTick <= 0) {
                maxTick = 10;
            }
            this.maxTick = maxTick;
            this.itemUuid = itemUuid;
        }

        public void tick() {
            nowTick++;
            if (nowTick > maxTick) {
                this.setCancel();
                return;
            }
        }

        public void setCancel() {
            this.canceled = true;
            this.nowTick = this.maxTick + 1;
        }

        public boolean isCanceled() {
            if (this.maxTick <= this.nowTick) {
                this.canceled = true;
            }
            return this.canceled;
        }
    }
}
