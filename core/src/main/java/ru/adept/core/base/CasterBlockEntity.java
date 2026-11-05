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

import java.util.Optional;

/**
 * Литейная (нулевой уровень): отливает из расплава в соседнем резервуаре слитки, иглы гаусс-пушки
 * (материал иглы задаёт урон) и пластины тяжёлой брони. Быстро и массово; рун в срезе 2 нет.
 */
public class CasterBlockEntity extends MachineBlockEntity {
    public enum Form { INGOT, NEEDLE, PLATE }

    private Form form = Form.NEEDLE;

    public CasterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASTER.get(), pos, state, 2);
    }

    public Form form() {
        return form;
    }

    public void setForm(int index) {
        form = Form.values()[Math.floorMod(index, Form.values().length)];
        progress = 0;
        setChanged();
    }

    public static int cost(Form form) {
        return switch (form) {
            case INGOT -> BalanceConfig.SMELTER_MB_PER_INGOT.get();
            case NEEDLE -> BalanceConfig.CAST_NEEDLE_MB.get();
            case PLATE -> BalanceConfig.CAST_PLATE_MB.get();
        };
    }

    public static ItemStack product(Form form, Molten m) {
        return switch (form) {
            case INGOT -> new ItemStack(m.ingot());
            case NEEDLE -> new ItemStack(m.needle(), BalanceConfig.CAST_NEEDLES_PER_CAST.get());
            case PLATE -> new ItemStack(m.plate());
        };
    }

    /** Материал для отливки: тот, которого в соседних резервуарах больше всего (и хватает на форму). */
    public Optional<Molten> material() {
        if (level == null) {
            return Optional.empty();
        }
        Molten best = null;
        int bestAmount = cost(form) - 1;
        for (Molten m : Molten.values()) {
            int a = ReservoirBlockEntity.available(level, worldPosition, m.id());
            if (a > bestAmount) {
                best = m;
                bestAmount = a;
            }
        }
        return Optional.ofNullable(best);
    }

    @Override
    public void serverTick() {
        BaseData base = base();
        Optional<Molten> m = material();
        if (base == null || m.isEmpty() || !canOutput(1, product(form, m.get()))) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (!base.useElectricity(BalanceConfig.CAST_ELECTRICITY_PER_TICK.get())) {
            return;
        }
        if (++progress >= BalanceConfig.CAST_TICKS.get()) {
            progress = 0;
            cast();
        }
        setChanged();
    }

    /** Отлить одну порцию (конец цикла и тесты). */
    public boolean cast() {
        Optional<Molten> m = material();
        if (m.isEmpty()) {
            return false;
        }
        ItemStack out = product(form, m.get());
        if (!canOutput(1, out) || !ReservoirBlockEntity.take(level, worldPosition, m.get().id(), cost(form))) {
            return false;
        }
        output(1, out);
        return true;
    }

    @Override
    protected int progressPercent() {
        return progress * 100 / Math.max(1, BalanceConfig.CAST_TICKS.get());
    }

    @Override
    protected int extra1() {
        return form.ordinal();
    }

    @Override
    protected int extra2() {
        return material().map(x -> x.ordinal() + 1).orElse(0);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMenu.caster(id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition), this::setForm);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("form", form.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        form = Form.values()[Math.floorMod(tag.getInt("form"), Form.values().length)];
    }
}
