package ru.adept.core.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModBlockEntities;

/** Переработка руды (нулевой уровень): кусок или блок руды → рудная пыль, на электричестве базы. */
public class CrusherBlockEntity extends MachineBlockEntity {
    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state, 2);
    }

    @Override
    public void serverTick() {
        BaseData base = base();
        ItemStack result = MachineRecipes.crush(items.getItem(0));
        if (base == null || result.isEmpty() || !canOutput(1, result)) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (!base.useElectricity(BalanceConfig.CRUSHER_ELECTRICITY_PER_TICK.get())) {
            return;
        }
        if (++progress >= BalanceConfig.CRUSHER_TICKS.get()) {
            progress = 0;
            items.getItem(0).shrink(1);
            output(1, result);
        }
        setChanged();
    }

    @Override
    protected int progressPercent() {
        return progress * 100 / Math.max(1, BalanceConfig.CRUSHER_TICKS.get());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMenu.crusher(id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
