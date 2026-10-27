package ru.adept.core.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Общая основа машин базы: инвентарь, сохранение, данные для экрана
 * (0 — электричество, 1 — мана, 2 — ёмкость запаса, 3 — прогресс %, 4 — бак, 5 — объём бака).
 * Энергия общая на всю базу ({@link BaseData}).
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DATA_SIZE = 6;

    protected final SimpleContainer items;
    protected int progress;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            BaseData base = base();
            return switch (index) {
                case 0 -> base == null ? 0 : base.electricity();
                case 1 -> base == null ? 0 : base.mana();
                case 2 -> BaseData.capacity();
                case 3 -> progressPercent();
                case 4 -> tankAmount();
                case 5 -> tankCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_SIZE;
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        this.items = new SimpleContainer(slots) {
            @Override
            public void setChanged() {
                super.setChanged();
                MachineBlockEntity.this.setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return Container.stillValidBlockEntity(MachineBlockEntity.this, player);
            }
        };
    }

    public SimpleContainer items() {
        return items;
    }

    protected BaseData base() {
        return level == null || level.getServer() == null ? null : BaseData.get(level.getServer());
    }

    public abstract void serverTick();

    protected int progressPercent() {
        return 0;
    }

    protected int tankAmount() {
        return 0;
    }

    protected int tankCapacity() {
        return 0;
    }

    /** Положить результат в слот вывода; false — не помещается. */
    protected boolean output(int slot, ItemStack result) {
        ItemStack out = items.getItem(slot);
        if (out.isEmpty()) {
            items.setItem(slot, result.copy());
            return true;
        }
        if (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize()) {
            out.grow(result.getCount());
            items.setChanged();
            return true;
        }
        return false;
    }

    protected boolean canOutput(int slot, ItemStack result) {
        ItemStack out = items.getItem(slot);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items.getItems(), registries);
        progress = tag.getInt("progress");
    }
}
