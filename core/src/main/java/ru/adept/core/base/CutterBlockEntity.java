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
import ru.adept.core.config.FocusConfig;
import ru.adept.core.focus.FocusCrystal;
import ru.adept.core.focus.FocusCut;
import ru.adept.core.focus.FocusItem;
import ru.adept.core.registry.ModBlockEntities;

import java.util.Optional;

/**
 * Ограночная машина (нулевой уровень): огранивает кристаллы в фокусы (огранка — кнопкой экрана) и чинит фокусы.
 * Слот 0 — кристаллы или фокус на починку, слот 1 — готовый фокус. Огранка тратит электричество базы,
 * починка — ещё и ману базы (в конце цикла). Починка возвращает полную прочность и снимает трещину.
 */
public class CutterBlockEntity extends MachineBlockEntity {
    public enum Job { NONE, CUT, REPAIR }

    private FocusCut cut = FocusCut.KNOCKBACK;

    public CutterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUTTER.get(), pos, state, 2);
    }

    public FocusCut cut() {
        return cut;
    }

    public void setCut(int index) {
        cut = FocusCut.byIndex(index);
        progress = 0;
        setChanged();
    }

    /** Что машина сделает с содержимым входа. */
    public Job job() {
        ItemStack in = items.getItem(0);
        if (!items.getItem(1).isEmpty() || in.isEmpty()) {
            return Job.NONE;
        }
        if (FocusItem.is(in)) {
            return FocusItem.worn(in) ? Job.REPAIR : Job.NONE;
        }
        Optional<FocusCrystal> crystal = FocusCrystal.of(in);
        return crystal.isPresent() && in.getCount() >= FocusConfig.CUTTER_CRYSTALS.get() ? Job.CUT : Job.NONE;
    }

    private int jobTicks(Job job) {
        return job == Job.REPAIR ? FocusConfig.CUTTER_REPAIR_TICKS.get() : FocusConfig.CUTTER_CUT_TICKS.get();
    }

    @Override
    public void serverTick() {
        BaseData base = base();
        Job job = job();
        if (base == null || job == Job.NONE) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (job == Job.REPAIR && base.mana() < FocusConfig.CUTTER_REPAIR_MANA.get()) {
            return;
        }
        if (!base.useElectricity(FocusConfig.CUTTER_ELECTRICITY.get())) {
            return;
        }
        if (++progress >= jobTicks(job)) {
            progress = 0;
            finish();
        }
        setChanged();
    }

    /** Закончить цикл сразу (конец работы и тесты): огранить фокус или починить. */
    public boolean finish() {
        Job job = job();
        ItemStack in = items.getItem(0);
        if (job == Job.CUT) {
            FocusCrystal crystal = FocusCrystal.of(in).orElseThrow();
            ItemStack focus = FocusItem.create(cut, crystal);
            FocusItem.setFresh(focus, true);
            in.shrink(FocusConfig.CUTTER_CRYSTALS.get());
            items.setItem(1, focus);
            items.setChanged();
            return true;
        }
        if (job == Job.REPAIR) {
            BaseData base = base();
            if (base == null || !base.useMana(FocusConfig.CUTTER_REPAIR_MANA.get())) {
                return false;
            }
            ItemStack focus = in.copy();
            FocusItem.repair(focus, FocusConfig.CUTTER_REPAIR_RESTORE.get());
            items.setItem(0, ItemStack.EMPTY);
            items.setItem(1, focus);
            items.setChanged();
            return true;
        }
        return false;
    }

    @Override
    protected int progressPercent() {
        Job job = job();
        return job == Job.NONE ? 0 : progress * 100 / Math.max(1, jobTicks(job));
    }

    @Override
    protected int extra1() {
        return cut.ordinal();
    }

    @Override
    protected int extra2() {
        return job().ordinal();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMenu.cutter(id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition), this::setCut);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("cut", cut.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cut = FocusCut.byIndex(tag.getInt("cut"));
    }
}
