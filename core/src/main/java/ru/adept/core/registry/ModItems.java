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

    // Сплавы и литьё (срез 2). Иглы и пластины — по материалу, порядок как в base.Molten.
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> RED_GOLD_INGOT = ITEMS.registerSimpleItem("red_gold_ingot");
    public static final List<String> MOLTEN_IDS = List.of("iron", "copper", "gold", "steel", "red_gold");
    public static final List<DeferredItem<Item>> NEEDLES = MOLTEN_IDS.stream()
            .map(m -> ITEMS.registerSimpleItem(m + "_needle")).toList();
    public static final List<DeferredItem<Item>> PLATES = MOLTEN_IDS.stream()
            .map(m -> ITEMS.registerSimpleItem(m + "_plate", new Item.Properties().stacksTo(16))).toList();

    // Тяжёлая силовая броня (прототип) и гаусс-пушка (прототип)
    public static final DeferredItem<ru.adept.core.artifact.HeavyArmorItem> HEAVY_HELMET = ITEMS.register("heavy_helmet",
            () -> new ru.adept.core.artifact.HeavyArmorItem(ModRegistries.HEAVY_ARMOR, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ru.adept.core.artifact.HeavyArmorItem> HEAVY_CHESTPLATE = ITEMS.register("heavy_chestplate",
            () -> new ru.adept.core.artifact.HeavyArmorItem(ModRegistries.HEAVY_ARMOR, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ru.adept.core.artifact.HeavyArmorItem> HEAVY_LEGGINGS = ITEMS.register("heavy_leggings",
            () -> new ru.adept.core.artifact.HeavyArmorItem(ModRegistries.HEAVY_ARMOR, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ru.adept.core.artifact.GaussGunItem> GAUSS_GUN = ITEMS.register("gauss_gun",
            () -> new ru.adept.core.artifact.GaussGunItem(new Item.Properties().stacksTo(1)));

    // Трофеи героев для Альянса и приказ командира
    public static final DeferredItem<Item> HERO_EMBLEM = ITEMS.registerSimpleItem("hero_emblem");
    public static final DeferredItem<Item> HERO_BANNER = ITEMS.registerSimpleItem("hero_banner");
    public static final DeferredItem<ru.adept.core.hero.CommanderOrderItem> COMMANDER_ORDER = ITEMS.register("commander_order",
            () -> new ru.adept.core.hero.CommanderOrderItem(new Item.Properties().stacksTo(1)));

    // Срез 3: накопитель, котёл душ, сосуды, ядро души
    public static final DeferredItem<ru.adept.core.mana.AccumulatorItem> ACCUMULATOR = ITEMS.register("accumulator",
            () -> new ru.adept.core.mana.AccumulatorItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ru.adept.core.mana.SoulCauldronItem> SOUL_CAULDRON = ITEMS.register("soul_cauldron",
            () -> new ru.adept.core.mana.SoulCauldronItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ru.adept.core.mana.SoulVesselItem> GLASS_SOUL_VESSEL = ITEMS.register("glass_soul_vessel",
            () -> new ru.adept.core.mana.SoulVesselItem("glass", () -> ru.adept.core.config.ManaConfig.VESSEL_GLASS.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ru.adept.core.mana.SoulVesselItem> QUARTZ_SOUL_VESSEL = ITEMS.register("quartz_soul_vessel",
            () -> new ru.adept.core.mana.SoulVesselItem("quartz", () -> ru.adept.core.config.ManaConfig.VESSEL_QUARTZ.get(), new Item.Properties().stacksTo(1)));
    /** Ядро души — побочный продукт горнила, валюта торговца наравне со сгустками. */
    public static final DeferredItem<Item> SOUL_CORE = ITEMS.registerSimpleItem("soul_core");

    // Срез 3, часть 2: фокусы и лёгкий экзокостюм (прототип — нагрудник, генератор щита)
    public static final DeferredItem<ru.adept.core.focus.FocusItem> FOCUS = ITEMS.register("focus",
            () -> new ru.adept.core.focus.FocusItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ru.adept.core.artifact.LightArmorItem> LIGHT_CHESTPLATE = ITEMS.register("light_chestplate",
            () -> new ru.adept.core.artifact.LightArmorItem(ModRegistries.LIGHT_ARMOR, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    static void init() {
    }
}
