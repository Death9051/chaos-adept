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
    public static final int BTN_BLADE_PROTOTYPE = 20;
    public static final int BTN_BLADE_EDGE = 30;

    /** 0 — пара не выбрана, 1 — клинок, 2 — молот. */
    public final DataSlot choice = DataSlot.standalone();
    public final DataSlot bladeBuilt = DataSlot.standalone();
    public final DataSlot bladeEdge = DataSlot.standalone();
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
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        TableActions.Result result;
        if (id == BTN_CHOOSE_BLADE) {
            result = TableActions.choosePair(player, TableActions.BLADE);
        } else if (id == BTN_CHOOSE_HAMMER) {
            result = TableActions.choosePair(player, TableActions.HAMMER);
        } else if (id >= BTN_CRUDE_FIRST && id < BTN_CRUDE_FIRST + TableActions.CRUDE.size()) {
            result = TableActions.craftCrude(player, id - BTN_CRUDE_FIRST);
        } else if (id == BTN_RESONATOR) {
            result = TableActions.craftResonator(player);
        } else if (id == BTN_BLADE_PROTOTYPE) {
            result = TableActions.craftBladePrototype(player);
        } else if (id == BTN_BLADE_EDGE) {
            result = TableActions.upgradeBladeEdge(player);
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ARTIFACT_TABLE.get());
    }
}
