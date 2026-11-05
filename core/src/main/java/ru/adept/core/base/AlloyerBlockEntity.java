package ru.adept.core.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModBlockEntities;

/**
 * Сплавщик (нулевой уровень): берёт расплавы из соседних резервуаров и сливает сплав в соседний резервуар.
 * Сплавы среза: красное золото (золото + медь) и сталь (железо + уголь в слоте). Работает на электричестве базы.
 */
public class AlloyerBlockEntity extends MachineBlockEntity {
    public enum Recipe { RED_GOLD, STEEL }

    private Recipe recipe = Recipe.RED_GOLD;

    public AlloyerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOYER.get(), pos, state, 2);
    }

    public Recipe recipe() {
        return recipe;
    }

    public void setRecipe(int index) {
        recipe = Recipe.values()[Math.floorMod(index, Recipe.values().length)];
        progress = 0;
        setChanged();
    }

    /** Можно ли сейчас начать/продолжать сплав (входы есть, выходу есть место). */
    public boolean ready() {
        if (level == null) {
            return false;
        }
        int mb = BalanceConfig.ALLOY_MB.get();
        return switch (recipe) {
            case RED_GOLD -> ReservoirBlockEntity.available(level, worldPosition, Molten.GOLD.id()) >= mb
                    && ReservoirBlockEntity.available(level, worldPosition, Molten.COPPER.id()) >= mb
                    && ReservoirBlockEntity.room(level, worldPosition, Molten.RED_GOLD.id()) >= mb * 2;
            case STEEL -> ReservoirBlockEntity.available(level, worldPosition, Molten.IRON.id()) >= mb
                    && (items.getItem(0).is(Items.COAL) || items.getItem(0).is(Items.CHARCOAL))
                    && ReservoirBlockEntity.room(level, worldPosition, Molten.STEEL.id()) >= mb;
        };
    }

    @Override
    public void serverTick() {
        BaseData base = base();
        if (base == null) {
            return;
        }
        if (!ready()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (!base.useElectricity(BalanceConfig.ALLOY_ELECTRICITY_PER_TICK.get())) {
            return;
        }
        if (++progress >= BalanceConfig.ALLOY_TICKS.get()) {
            progress = 0;
            finish();
        }
        setChanged();
    }

    /** Сплавить одну порцию (без ожидания; для тестов и конца цикла). */
    public boolean finish() {
        if (!ready()) {
            return false;
        }
        int mb = BalanceConfig.ALLOY_MB.get();
        switch (recipe) {
            case RED_GOLD -> {
                ReservoirBlockEntity.take(level, worldPosition, Molten.GOLD.id(), mb);
                ReservoirBlockEntity.take(level, worldPosition, Molten.COPPER.id(), mb);
                ReservoirBlockEntity.put(level, worldPosition, Molten.RED_GOLD.id(), mb * 2);
            }
            case STEEL -> {
                ReservoirBlockEntity.take(level, worldPosition, Molten.IRON.id(), mb);
                items.getItem(0).shrink(1);
                items.setChanged();
                ReservoirBlockEntity.put(level, worldPosition, Molten.STEEL.id(), mb);
            }
        }
        return true;
    }

    @Override
    protected int progressPercent() {
        return progress * 100 / Math.max(1, BalanceConfig.ALLOY_TICKS.get());
    }

    @Override
    protected int extra1() {
        return recipe.ordinal();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMenu.alloyer(id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition), this::setRecipe);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("recipe", recipe.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        recipe = Recipe.values()[Math.floorMod(tag.getInt("recipe"), Recipe.values().length)];
    }

    public static boolean isAdditive(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }
}
