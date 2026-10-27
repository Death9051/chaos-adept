package ru.adept.core.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModBlockEntities;
import ru.adept.core.registry.ModItems;

/** Генератор базы: сжигает кристаллы узлов и даёт электричество и ману. При полном запасе не жжёт. */
public class GeneratorBlockEntity extends MachineBlockEntity {
    private int burnLeft;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR.get(), pos, state, 1);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(ModItems.NODE_SHARD.get());
    }

    @Override
    public void serverTick() {
        BaseData base = base();
        if (base == null) {
            return;
        }
        if (burnLeft <= 0) {
            ItemStack fuel = items.getItem(0);
            if (base.isFull() || !isFuel(fuel)) {
                return;
            }
            fuel.shrink(1);
            items.setChanged();
            burnLeft = BalanceConfig.GENERATOR_BURN_TICKS.get();
        }
        burnLeft--;
        base.add(BalanceConfig.GENERATOR_ELECTRICITY_PER_TICK.get(), BalanceConfig.GENERATOR_MANA_PER_TICK.get());
        setChanged();
    }

    @Override
    protected int progressPercent() {
        return burnLeft * 100 / Math.max(1, BalanceConfig.GENERATOR_BURN_TICKS.get());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMenu.generator(id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("burnLeft", burnLeft);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnLeft = tag.getInt("burnLeft");
    }
}
