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

/**
 * Плавильня (нулевой уровень): пыль плавится в расплав во внутреннем баке на мане базы,
 * из бака отливаются слитки. В срезе нет отдельных резервуаров и литейной — бак встроенный.
 */
public class SmelterBlockEntity extends MachineBlockEntity {
    private ItemStack tankIngot = ItemStack.EMPTY;
    private int tank;

    public SmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMELTER.get(), pos, state, 2);
    }

    @Override
    public void serverTick() {
        BaseData base = base();
        if (base == null) {
            return;
        }
        pour();
        ItemStack dust = items.getItem(0);
        ItemStack ingot = MachineRecipes.ingotFor(dust);
        int perDust = BalanceConfig.SMELTER_MB_PER_DUST.get();
        boolean fits = !ingot.isEmpty() && (tank == 0 || ItemStack.isSameItem(ingot, tankIngot))
                && tank + perDust <= BalanceConfig.SMELTER_TANK_MB.get();
        if (!fits) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (!base.useMana(BalanceConfig.SMELTER_MANA_PER_TICK.get())) {
            return;
        }
        if (++progress >= BalanceConfig.SMELTER_TICKS.get()) {
            progress = 0;
            dust.shrink(1);
            items.setChanged();
            tankIngot = ingot;
            tank += perDust;
        }
        setChanged();
    }

    private void pour() {
        int perIngot = BalanceConfig.SMELTER_MB_PER_INGOT.get();
        if (tank >= perIngot && !tankIngot.isEmpty() && output(1, tankIngot.copyWithCount(1))) {
            tank -= perIngot;
            if (tank <= 0) {
                tank = 0;
                tankIngot = ItemStack.EMPTY;
            }
            setChanged();
        }
    }

    public int tank() {
        return tank;
    }

    @Override
    protected int progressPercent() {
        return progress * 100 / Math.max(1, BalanceConfig.SMELTER_TICKS.get());
    }

    @Override
    protected int tankAmount() {
        return tank;
    }

    @Override
    protected int tankCapacity() {
        return BalanceConfig.SMELTER_TANK_MB.get();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMenu.smelter(id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("tank", tank);
        if (!tankIngot.isEmpty()) {
            tag.put("tankIngot", tankIngot.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank = tag.getInt("tank");
        tankIngot = tag.contains("tankIngot") ? ItemStack.parseOptional(registries, tag.getCompound("tankIngot")) : ItemStack.EMPTY;
    }
}
