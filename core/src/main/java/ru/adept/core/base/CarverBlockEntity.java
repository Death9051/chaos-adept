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
import ru.adept.core.config.RuneConfig;
import ru.adept.core.registry.ModBlockEntities;
import ru.adept.core.rune.Rune;
import ru.adept.core.rune.RuneMaterial;
import ru.adept.core.rune.RuneSet;
import ru.adept.core.rune.Runes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Высекатель рун (нулевой уровень): гравирует литые изделия (иглы, болты, пластины) одним материалом.
 * Слот 0 — изделия без рун (вся пачка за раз), слот 2 — материал гравировки, слот 1 — готовая пачка.
 * Руны выбираются кнопками; на литом изделии одна руна, с «Точностью» I (ступень машины в данных базы) — две.
 * Тратит электричество базы за тик и ману базы за каждое изделие (руна запитывается).
 */
public class CarverBlockEntity extends MachineBlockEntity {
    public static final String PRECISION = "carver/precision";
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_MATERIAL = 2;

    private Rune first = Rune.CURRENT;
    /** Вторая руна; null — нет. */
    private Rune second;

    public CarverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARVER.get(), pos, state, 3);
    }

    public Rune first() {
        return first;
    }

    public Rune second() {
        return second;
    }

    /** Кнопки: 0 — следующая первая руна, 1 — следующая вторая (после последней — «нет»). */
    public void button(int id) {
        if (id == 0) {
            first = Rune.byIndex(first.ordinal() + 1);
        } else if (id == 1) {
            second = second == null ? Rune.values()[0] : second.ordinal() + 1 >= Rune.values().length ? null : Rune.byIndex(second.ordinal() + 1);
        }
        progress = 0;
        setChanged();
    }

    public void setRunes(Rune first, Rune second) {
        this.first = first;
        this.second = second;
        progress = 0;
        setChanged();
    }

    /** Сколько рун ставит машина: 1 + ступени «Точности». */
    public int maxRunes() {
        BaseData base = base();
        return 1 + (base == null ? 0 : base.machineStep(PRECISION));
    }

    /** Гравировка, которую получит пачка. */
    public RuneSet planned(String material) {
        List<Rune> runes = new ArrayList<>();
        runes.add(first);
        if (second != null && maxRunes() >= 2) {
            runes.add(second);
        }
        return new RuneSet(runes, material);
    }

    /** Материала на пачку: игл — по {@code needlesPerMaterial} на единицу, болтов и пластин — по единице; «Экономия» вдвое меньше. */
    public static int materialCost(ItemStack input, RuneMaterial material) {
        int n = input.getCount();
        int cost = "needle".equals(Runes.kind(input))
                ? (int) Math.ceil(n / (double) RuneConfig.CARVER_NEEDLES_PER_MATERIAL.get()) : n;
        if (RuneMaterial.LAPIS.equals(material.id())) {
            cost = (int) Math.ceil(cost / 2.0);
        }
        return Math.max(1, cost);
    }

    /** Готова ли пачка к гравировке (без учёта энергии базы). */
    public boolean ready() {
        ItemStack in = items.getItem(SLOT_INPUT);
        if (in.isEmpty() || !items.getItem(SLOT_OUTPUT).isEmpty() || !Runes.engravable(in)) {
            return false;
        }
        Optional<RuneMaterial> m = RuneMaterial.of(items.getItem(SLOT_MATERIAL));
        return m.isPresent() && items.getItem(SLOT_MATERIAL).getCount() >= materialCost(in, m.get());
    }

    private int manaCost() {
        return RuneConfig.CARVER_MANA.get() * items.getItem(SLOT_INPUT).getCount();
    }

    @Override
    public void serverTick() {
        BaseData base = base();
        if (base == null || !ready()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (base.mana() < manaCost() || !base.useElectricity(RuneConfig.CARVER_ELECTRICITY.get())) {
            return;
        }
        if (++progress >= RuneConfig.CARVER_TICKS.get()) {
            progress = 0;
            finish();
        }
        setChanged();
    }

    /** Закончить гравировку сразу (конец цикла и тесты). */
    public boolean finish() {
        BaseData base = base();
        if (base == null || !ready() || !base.useMana(manaCost())) {
            return false;
        }
        ItemStack in = items.getItem(SLOT_INPUT);
        RuneMaterial material = RuneMaterial.of(items.getItem(SLOT_MATERIAL)).orElseThrow();
        items.getItem(SLOT_MATERIAL).shrink(materialCost(in, material));
        ItemStack out = in.copy();
        planned(material.id()).applyTo(out);
        Runes.setFresh(out, true);
        items.setItem(SLOT_INPUT, ItemStack.EMPTY);
        items.setItem(SLOT_OUTPUT, out);
        items.setChanged();
        return true;
    }

    @Override
    protected int progressPercent() {
        return progress * 100 / Math.max(1, RuneConfig.CARVER_TICKS.get());
    }

    @Override
    protected int extra1() {
        return first.ordinal();
    }

    /** Вторая руна (+1, 0 — нет) и число рун машины ×16 — для экрана. */
    @Override
    protected int extra2() {
        return (second == null ? 0 : second.ordinal() + 1) + 16 * maxRunes();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMenu.carver(id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition), this::button);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("first", first.ordinal());
        tag.putInt("second", second == null ? -1 : second.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        first = Rune.byIndex(tag.getInt("first"));
        int s = tag.contains("second") ? tag.getInt("second") : -1;
        second = s < 0 ? null : Rune.byIndex(s);
    }
}
