package ru.adept.core.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ru.adept.core.base.BaseTravel;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModMenus;
import ru.adept.core.trader.TraderManager;

/** Меню стационарного перехода: «На базу» и «Вызов Альянса» (после зачистки узла). */
public class TransitionMenu extends AbstractContainerMenu {
    public static final int BTN_BASE = 0;
    public static final int BTN_CALL = 1;

    public final DataSlot canCall = DataSlot.standalone();
    private final ContainerLevelAccess access;
    private final Player player;

    public TransitionMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public TransitionMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.TRANSITION.get(), id);
        this.access = access;
        this.player = inventory.player;
        addDataSlot(canCall);
        refresh();
    }

    private void refresh() {
        if (player instanceof ServerPlayer server) {
            canCall.set(TraderManager.canCall(server.getServer()) ? 1 : 0);
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        if (id == BTN_BASE) {
            access.execute((level, pos) -> {
                server.closeContainer();
                BaseTravel.enter(server, Vec3.atBottomCenterOf(pos.above()), server.getYRot());
            });
            return true;
        }
        if (id == BTN_CALL) {
            server.closeContainer();
            TraderManager.call(server);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.TRANSITION.get());
    }

}
