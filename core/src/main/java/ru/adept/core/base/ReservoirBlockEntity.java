package ru.adept.core.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModBlockEntities;

/**
 * Простой резервуар расплава (первый уровень объёма): хранит один материал.
 * Туда сливает плавильня, оттуда берут сплавщик и литейная — через соседство (вплотную).
 * Сломанный резервуар сохраняет расплав в предмете.
 */
public class ReservoirBlockEntity extends BlockEntity {
    private String material = "";
    private int amount;

    public ReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESERVOIR.get(), pos, state);
    }

    public static int capacity() {
        return BalanceConfig.RESERVOIR_CAPACITY_MB.get();
    }

    public String material() {
        return amount <= 0 ? "" : material;
    }

    public int amount() {
        return amount;
    }

    /** Принять расплав; возвращает, сколько принято. */
    public int insert(String id, int mb) {
        if (mb <= 0 || (amount > 0 && !material.equals(id))) {
            return 0;
        }
        int accepted = Math.min(mb, capacity() - amount);
        if (accepted > 0) {
            material = id;
            amount += accepted;
            setChanged();
        }
        return accepted;
    }

    /** Забрать расплав; возвращает, сколько отдано. */
    public int extract(String id, int mb) {
        if (amount <= 0 || !material.equals(id)) {
            return 0;
        }
        int taken = Math.min(mb, amount);
        amount -= taken;
        if (amount <= 0) {
            amount = 0;
            material = "";
        }
        setChanged();
        return taken;
    }

    public void set(String id, int mb) {
        material = mb > 0 ? id : "";
        amount = Math.max(0, Math.min(mb, capacity()));
        setChanged();
    }

    /** Соседние резервуары блока (6 сторон). */
    public static java.util.List<ReservoirBlockEntity> around(Level level, BlockPos pos) {
        java.util.List<ReservoirBlockEntity> out = new java.util.ArrayList<>();
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(pos.relative(d)) instanceof ReservoirBlockEntity r) {
                out.add(r);
            }
        }
        return out;
    }

    /** Сколько материала во всех соседних резервуарах. */
    public static int available(Level level, BlockPos pos, String id) {
        return around(level, pos).stream().filter(r -> r.material().equals(id)).mapToInt(ReservoirBlockEntity::amount).sum();
    }

    /** Забрать из соседних; true — если набрано всё. Сначала проверяет, потом списывает. */
    public static boolean take(Level level, BlockPos pos, String id, int mb) {
        if (available(level, pos, id) < mb) {
            return false;
        }
        int left = mb;
        for (ReservoirBlockEntity r : around(level, pos)) {
            left -= r.extract(id, left);
            if (left <= 0) {
                break;
            }
        }
        return true;
    }

    /** Сколько можно положить в соседние; не меняет их. */
    public static int room(Level level, BlockPos pos, String id) {
        int room = 0;
        for (ReservoirBlockEntity r : around(level, pos)) {
            if (r.amount() == 0 || r.material().equals(id)) {
                room += capacity() - r.amount();
            }
        }
        return room;
    }

    /** Разлить по соседним; возвращает, сколько разлито. Резервуары с этим материалом — первыми. */
    public static int put(Level level, BlockPos pos, String id, int mb) {
        int left = mb;
        var list = around(level, pos);
        list.sort(java.util.Comparator.comparingInt(r -> r.material().equals(id) ? 0 : 1));
        for (ReservoirBlockEntity r : list) {
            left -= r.insert(id, left);
            if (left <= 0) {
                break;
            }
        }
        return mb - left;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("material", material);
        tag.putInt("amount", amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        material = tag.getString("material");
        amount = tag.getInt("amount");
    }
}
