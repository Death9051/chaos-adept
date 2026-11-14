package ru.adept.core.table;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModMenus;

/**
 * Меню стола артефактов. Слотов нет: состояние игрока приходит на клиент через DataSlot,
 * действия — через стандартный {@link #clickMenuButton} (номер кнопки).
 */
public class ArtifactTableMenu extends AbstractContainerMenu {
    public static final int BTN_CHOOSE_BLADE = 0;
    public static final int BTN_CHOOSE_HAMMER = 1;
    public static final int BTN_CRUDE_FIRST = 10;
    public static final int BTN_RESONATOR = 15;
    public static final int BTN_CAMP = 16;
    public static final int BTN_KEY = 17;
    public static final int BTN_BLADE_PROTOTYPE = 20;
    public static final int BTN_BLADE_EDGE = 30;
    public static final int BTN_CHOOSE_LIGHT = 2;
    public static final int BTN_CHOOSE_HEAVY = 3;
    public static final int BTN_CHOOSE_GAUSS = 4;
    public static final int BTN_CHOOSE_RAIL = 5;
    public static final int BTN_HEAVY_PROTOTYPE = 21;
    public static final int BTN_GAUSS_PROTOTYPE = 22;
    public static final int BTN_HEAVY_BASTION = 31;
    public static final int BTN_GAUSS_TEMPO = 32;
    // Срез 3: одиночные артефакты
    public static final int BTN_ACCUMULATOR = 23;
    public static final int BTN_CAULDRON = 24;
    public static final int BTN_ACCUMULATOR_CAPACITY = 33;
    public static final int BTN_CAULDRON_YIELD = 34;

    /** 0 — пара не выбрана, 1 — клинок, 2 — молот. */
    public final DataSlot choice = DataSlot.standalone();
    public final DataSlot bladeBuilt = DataSlot.standalone();
    public final DataSlot bladeEdge = DataSlot.standalone();
    /** 0 — броня не выбрана, 1 — лёгкая, 2 — тяжёлая. */
    public final DataSlot armorChoice = DataSlot.standalone();
    public final DataSlot heavyBuilt = DataSlot.standalone();
    public final DataSlot heavyBastion = DataSlot.standalone();
    /** 0 — дальний бой не выбран, 1 — гаусс, 2 — рельса. */
    public final DataSlot rangedChoice = DataSlot.standalone();
    public final DataSlot gaussBuilt = DataSlot.standalone();
    public final DataSlot gaussTempo = DataSlot.standalone();
    /** Облик гаусс-пушки по прогрессу ({@link ru.adept.core.artifact.GaussLook}) двумя половинами: слот передаёт 16 бит. */
    public final DataSlot gaussLookLow = DataSlot.standalone();
    public final DataSlot gaussLookHigh = DataSlot.standalone();
    public final DataSlot accumulatorBuilt = DataSlot.standalone();
    public final DataSlot accumulatorCapacity = DataSlot.standalone();
    public final DataSlot cauldronBuilt = DataSlot.standalone();
    public final DataSlot cauldronYield = DataSlot.standalone();
    /** Итог последнего действия: 0 — нет, иначе ordinal {@link TableActions.Result} + 1. */
    public final DataSlot lastResult = DataSlot.standalone();

    private final ContainerLevelAccess access;
    private final Player player;

    public ArtifactTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public ArtifactTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.ARTIFACT_TABLE.get(), id);
        this.access = access;
        this.player = inventory.player;
        addDataSlot(choice);
        addDataSlot(bladeBuilt);
        addDataSlot(bladeEdge);
        addDataSlot(lastResult);
        addDataSlot(armorChoice);
        addDataSlot(heavyBuilt);
        addDataSlot(heavyBastion);
        addDataSlot(rangedChoice);
        addDataSlot(gaussBuilt);
        addDataSlot(gaussTempo);
        addDataSlot(gaussLookLow);
        addDataSlot(gaussLookHigh);
        addDataSlot(accumulatorBuilt);
        addDataSlot(accumulatorCapacity);
        addDataSlot(cauldronBuilt);
        addDataSlot(cauldronYield);
        refresh();
    }

    private void refresh() {
        if (player.level().isClientSide) {
            return;
        }
        ArtifactProgress p = TableActions.progress(player);
        String melee = p.choice(TableActions.MELEE).orElse("");
        choice.set(TableActions.BLADE.equals(melee) ? 1 : TableActions.HAMMER.equals(melee) ? 2 : 0);
        bladeBuilt.set(p.step(TableActions.BLADE_BUILT));
        bladeEdge.set(p.step(TableActions.BLADE_EDGE));
        String armor = p.choice(TableActions.ARMOR).orElse("");
        armorChoice.set(TableActions.LIGHT.equals(armor) ? 1 : TableActions.HEAVY.equals(armor) ? 2 : 0);
        heavyBuilt.set(p.step(TableActions.HEAVY_BUILT));
        heavyBastion.set(p.step(TableActions.HEAVY_BASTION));
        String ranged = p.choice(TableActions.RANGED).orElse("");
        rangedChoice.set(TableActions.GAUSS.equals(ranged) ? 1 : TableActions.RAIL.equals(ranged) ? 2 : 0);
        gaussBuilt.set(p.step(TableActions.GAUSS_BUILT));
        gaussTempo.set(p.step(TableActions.GAUSS_TEMPO));
        int look = ru.adept.core.artifact.GaussLook.pack(p);
        gaussLookLow.set(look & 0x7FFF);
        gaussLookHigh.set(look >> 15);
        accumulatorBuilt.set(p.step(TableActions.ACCUMULATOR_BUILT));
        accumulatorCapacity.set(p.step(TableActions.ACCUMULATOR_CAPACITY));
        cauldronBuilt.set(p.step(TableActions.CAULDRON_BUILT));
        cauldronYield.set(p.step(TableActions.CAULDRON_YIELD));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (GaussUpgrades.isButton(id)) {
            lastResult.set(GaussUpgrades.unlock(player, GaussUpgrades.node(id)).ordinal() + 1);
            refresh();
            return true;
        }
        TableActions.Result result;
        if (id == BTN_CHOOSE_BLADE) {
            result = TableActions.choosePair(player, TableActions.BLADE);
        } else if (id == BTN_CHOOSE_HAMMER) {
            result = TableActions.choosePair(player, TableActions.HAMMER);
        } else if (id >= BTN_CRUDE_FIRST && id < BTN_CRUDE_FIRST + TableActions.CRUDE.size()) {
            result = TableActions.craftCrude(player, id - BTN_CRUDE_FIRST);
        } else if (id == BTN_RESONATOR) {
            result = TableActions.craftResonator(player);
        } else if (id == BTN_CAMP) {
            result = TableActions.craftCamp(player);
        } else if (id == BTN_KEY) {
            result = TableActions.craftKey(player);
        } else if (id == BTN_BLADE_PROTOTYPE) {
            result = TableActions.craftBladePrototype(player);
        } else if (id == BTN_BLADE_EDGE) {
            result = TableActions.upgradeBladeEdge(player);
        } else if (id == BTN_CHOOSE_LIGHT) {
            result = TableActions.choosePair(player, TableActions.ARMOR, TableActions.LIGHT);
        } else if (id == BTN_CHOOSE_HEAVY) {
            result = TableActions.choosePair(player, TableActions.ARMOR, TableActions.HEAVY);
        } else if (id == BTN_CHOOSE_GAUSS) {
            result = TableActions.choosePair(player, TableActions.RANGED, TableActions.GAUSS);
        } else if (id == BTN_CHOOSE_RAIL) {
            result = TableActions.choosePair(player, TableActions.RANGED, TableActions.RAIL);
        } else if (id == BTN_HEAVY_PROTOTYPE) {
            result = TableActions.craftHeavyPrototype(player);
        } else if (id == BTN_GAUSS_PROTOTYPE) {
            result = TableActions.craftGaussPrototype(player);
        } else if (id == BTN_HEAVY_BASTION) {
            result = TableActions.upgradeHeavyBastion(player);
        } else if (id == BTN_GAUSS_TEMPO) {
            result = TableActions.upgradeGaussTempo(player);
        } else if (id == BTN_ACCUMULATOR) {
            result = TableActions.craftAccumulator(player);
        } else if (id == BTN_CAULDRON) {
            result = TableActions.craftCauldron(player);
        } else if (id == BTN_ACCUMULATOR_CAPACITY) {
            result = TableActions.upgradeAccumulatorCapacity(player);
        } else if (id == BTN_CAULDRON_YIELD) {
            result = TableActions.upgradeCauldronYield(player);
        } else {
            return false;
        }
        lastResult.set(result.ordinal() + 1);
        refresh();
        return true;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    /** Облик гаусс-пушки по прогрессу игрока, как его видит клиент. */
    public int gaussLook() {
        return gaussLookLow.get() | (gaussLookHigh.get() << 15);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ARTIFACT_TABLE.get());
    }
}
