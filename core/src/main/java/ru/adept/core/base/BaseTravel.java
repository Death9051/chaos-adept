package ru.adept.core.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.BaseEvent;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModRegistries;

/** Переходы между миром и базой-измерением; постройка зачатка базы при первом входе. */
public final class BaseTravel {
    public static final ResourceKey<Level> BASE =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "base"));
    public static final int FLOOR_Y = 63;
    private static final int RADIUS = 10;

    private BaseTravel() {
    }

    public static boolean isBase(Level level) {
        return level.dimension() == BASE;
    }

    /** Уйти на базу; {@code returnTo} — куда вернёт ключ. */
    public static boolean enter(ServerPlayer player, Vec3 returnTo, float yaw) {
        MinecraftServer server = player.getServer();
        ServerLevel base = server == null ? null : server.getLevel(BASE);
        if (base == null) {
            player.displayClientMessage(Component.literal("Измерение базы не найдено"), true);
            return false;
        }
        BaseData data = BaseData.get(server);
        boolean first = !data.isBuilt();
        if (first) {
            build(base);
            data.markBuilt();
        }
        player.setData(ModRegistries.RETURN_POINT, new ReturnPoint(player.level().dimension().location().toString(),
                returnTo.x, returnTo.y, returnTo.z, yaw));
        player.teleportTo(base, 0.5, FLOOR_Y + 1, 0.5, player.getYRot(), 0.0f);
        AdeptEvents.post(new BaseEvent.Entered(player, first));
        return true;
    }

    /** Вернуться с базы в точку возврата (или к точке появления мира). */
    public static void leave(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ReturnPoint point = player.getData(ModRegistries.RETURN_POINT);
        ServerLevel target = null;
        if (point.isSet()) {
            ResourceLocation id = ResourceLocation.tryParse(point.dimension());
            if (id != null) {
                target = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            }
        }
        if (target == null || target.dimension() == BASE) {
            target = server.overworld();
            BlockPos spawn = target.getSharedSpawnPos();
            player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), 0.0f);
        } else {
            player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), 0.0f);
        }
        AdeptEvents.post(new BaseEvent.Left(player));
    }

    /** Зачаток базы: площадка и генератор, который стоит в базе с самого начала. */
    static void build(ServerLevel base) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                boolean edge = Math.abs(x) == RADIUS || Math.abs(z) == RADIUS;
                base.setBlockAndUpdate(new BlockPos(x, FLOOR_Y, z),
                        (edge ? Blocks.POLISHED_DEEPSLATE : Blocks.SMOOTH_STONE).defaultBlockState());
            }
        }
        base.setBlockAndUpdate(new BlockPos(0, FLOOR_Y + 1, -4), ModBlocks.GENERATOR.get().defaultBlockState());
    }
}
