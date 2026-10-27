package ru.adept.core.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import ru.adept.core.artifact.EnergyBladeItem;
import ru.adept.core.node.ResonatorItem;

import static ru.adept.core.registry.ModRegistries.ITEMS;

public final class ModItems {
    // Грубые компоненты (ранг 0): делаются вручную на столе артефактов.
    public static final DeferredItem<Item> CRUDE_WIRING = ITEMS.registerSimpleItem("crude_wiring");
    public static final DeferredItem<Item> CRUDE_COIL = ITEMS.registerSimpleItem("crude_coil");
    public static final DeferredItem<Item> CRUDE_EMITTER = ITEMS.registerSimpleItem("crude_emitter");
    public static final DeferredItem<Item> CRUDE_MANA_CIRCUIT = ITEMS.registerSimpleItem("crude_mana_circuit");

    // Артефакты
    public static final DeferredItem<EnergyBladeItem> ENERGY_BLADE = ITEMS.register("energy_blade",
            () -> new EnergyBladeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ResonatorItem> RESONATOR = ITEMS.register("resonator",
            () -> new ResonatorItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    static void init() {
    }
}
