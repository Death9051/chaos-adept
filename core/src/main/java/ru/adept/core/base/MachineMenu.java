package ru.adept.core.base;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.registry.ModMenus;

/**
 * Меню машин базы по образцу ванильной печи: на клиенте — пустой контейнер и данные,
 * сервер синхронизирует слоты и {@link ContainerData}. Слот 0 — вход, слот 1 (если есть) — выход.
 */
public class MachineMenu extends AbstractContainerMenu {
    public enum Kind { GENERATOR, CRUSHER, SMELTER }

    public final Kind kind;
    private final Container container;
    private final ContainerData data;
    private final int machineSlots;

    public MachineMenu(MenuType<?> type, Kind kind, int id, Inventory inventory, Container container, ContainerData data) {
        super(type, id);
        this.kind = kind;
        this.container = container;
        this.data = data;
        this.machineSlots = container.getContainerSize();
        if (kind == Kind.GENERATOR) {
            addSlot(new Slot(container, 0, 80, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GeneratorBlockEntity.isFuel(stack);
                }
            });
        } else {
            addSlot(new Slot(container, 0, 56, 35));
            addSlot(new Slot(container, 1, 116, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public static MachineMenu generator(int id, Inventory inventory) {
        return generator(id, inventory, new SimpleContainer(1), new SimpleContainerData(MachineBlockEntity.DATA_SIZE), ContainerLevelAccess.NULL);
    }

    public static MachineMenu generator(int id, Inventory inventory, Container c, ContainerData d, ContainerLevelAccess access) {
        return new MachineMenu(ModMenus.GENERATOR.get(), Kind.GENERATOR, id, inventory, c, d);
    }

    public static MachineMenu crusher(int id, Inventory inventory) {
        return crusher(id, inventory, new SimpleContainer(2), new SimpleContainerData(MachineBlockEntity.DATA_SIZE), ContainerLevelAccess.NULL);
    }

    public static MachineMenu crusher(int id, Inventory inventory, Container c, ContainerData d, ContainerLevelAccess access) {
        return new MachineMenu(ModMenus.CRUSHER.get(), Kind.CRUSHER, id, inventory, c, d);
    }

    public static MachineMenu smelter(int id, Inventory inventory) {
        return smelter(id, inventory, new SimpleContainer(2), new SimpleContainerData(MachineBlockEntity.DATA_SIZE), ContainerLevelAccess.NULL);
    }

    public static MachineMenu smelter(int id, Inventory inventory, Container c, ContainerData d, ContainerLevelAccess access) {
        return new MachineMenu(ModMenus.SMELTER.get(), Kind.SMELTER, id, inventory, c, d);
    }

    public int data(int index) {
        return data.get(index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
