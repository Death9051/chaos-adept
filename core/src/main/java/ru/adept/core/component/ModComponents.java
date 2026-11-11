package ru.adept.core.component;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static ru.adept.core.registry.ModRegistries.ITEMS;

/**
 * Регистрация компонентов: 14 семейств × ранги I–III × вариации своей природы — 66 предметов.
 * Отдельный предмет на каждое сочетание, чтобы цена ступени в конфиге оставалась строкой «id*кол-во».
 */
public final class ModComponents {
    private static final Map<ComponentFamily, Map<ComponentVariant, List<DeferredItem<ComponentItem>>>> BY_FAMILY =
            new EnumMap<>(ComponentFamily.class);
    private static final List<DeferredItem<ComponentItem>> ALL = new ArrayList<>();

    static {
        for (ComponentFamily family : ComponentFamily.values()) {
            Map<ComponentVariant, List<DeferredItem<ComponentItem>>> byVariant = new EnumMap<>(ComponentVariant.class);
            for (ComponentVariant variant : family.variants()) {
                List<DeferredItem<ComponentItem>> ranks = new ArrayList<>();
                for (int rank = 1; rank <= ComponentFamily.MAX_RANK; rank++) {
                    int r = rank;
                    DeferredItem<ComponentItem> item = ITEMS.register(family.itemId(rank, variant),
                            () -> new ComponentItem(family, r, variant, new Item.Properties()));
                    ranks.add(item);
                    ALL.add(item);
                }
                byVariant.put(variant, List.copyOf(ranks));
            }
            BY_FAMILY.put(family, byVariant);
        }
    }

    private ModComponents() {
    }

    /** Предмет компонента; {@code null}, если у семейства нет такой вариации или ранг вне 1–3. */
    public static ComponentItem get(ComponentFamily family, int rank, ComponentVariant variant) {
        List<DeferredItem<ComponentItem>> ranks = BY_FAMILY.get(family).get(variant);
        if (ranks == null || rank < 1 || rank > ranks.size()) {
            return null;
        }
        return ranks.get(rank - 1).get();
    }

    public static List<DeferredItem<ComponentItem>> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Вызывается из ModRegistries, чтобы статический блок отработал до заморозки реестра. */
    public static void init() {
    }
}
