package ru.adept.core.base;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.NodeEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModItems;

/** Выход выкачки: кристаллы узла (топливо генератора базы) выпадают у кристалла после зачистки. */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class NodeShardDrops {
    private NodeShardDrops() {
    }

    @SubscribeEvent
    public static void onCleansed(NodeEvent.Cleansed event) {
        int count = BalanceConfig.NODE_SHARDS_PER_LEVEL.get() * event.getLevel();
        ServerLevel level = event.getServer().overworld();
        BlockPos pos = event.getNode();
        while (count > 0) {
            int stack = Math.min(64, count);
            count -= stack;
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(ModItems.NODE_SHARD.get(), stack));
            drop.setUnlimitedLifetime();
            level.addFreshEntity(drop);
        }
    }
}
