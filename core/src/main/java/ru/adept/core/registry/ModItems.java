package ru.adept.core.registry;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import ru.adept.core.artifact.EnergyBladeItem;
import ru.adept.core.base.KeyItem;
import ru.adept.core.camp.CampItem;
import ru.adept.core.node.ResonatorItem;

import java.util.List;
import java.util.stream.IntStream;

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
    public static final DeferredItem<CampItem> CAMP = ITEMS.register("camp",
            () -> new CampItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<KeyItem> KEY = ITEMS.register("key",
            () -> new KeyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ResonatorItem> RESONATOR = ITEMS.register("resonator",
            () -> new ResonatorItem(new Item.Properties().stacksTo(1)));

    // Ресурсы узла и переработки
    /** Кристалл узла — топливо генератора базы. */
    public static final DeferredItem<Item> NODE_SHARD = ITEMS.registerSimpleItem("node_shard");
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");

    /** Сгустки узлов по уровням 1–5: валюта Альянса и ключевой материал (вершины, транспорт, врата). */
    public static final List<DeferredItem<Item>> NODE_CLUSTERS = IntStream.rangeClosed(1, 5)
            .mapToObj(level -> ITEMS.registerSimpleItem("node_cluster_" + level))
            .toList();

    public static Item cluster(int level) {
        return NODE_CLUSTERS.get(Mth.clamp(level, 1, NODE_CLUSTERS.size()) - 1).get();
    }

    private ModItems() {
    }

    static void init() {
    }
}
