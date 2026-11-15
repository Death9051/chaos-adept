package ru.adept.core.table;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.ArtifactEvent;
import ru.adept.core.artifact.GaussLook;
import ru.adept.core.artifact.GaussLook.Segment;
import ru.adept.core.component.ComponentFamily;
import ru.adept.core.component.ComponentVariant;
import ru.adept.core.component.ModComponents;
import ru.adept.core.config.UpgradeConfig;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Прокачка гаусс-пушки по сегментам на столе артефактов: узлы (ступени двух путей и вершина), их цена
 * в компонентах и открытие. Здесь только прогресс и оплата — действие ступеней добавляет «Ядро» в срезе 4.
 *
 * Правила: ступени пути открываются по порядку; вершина — когда закрыт любой из двух путей сегмента.
 * Цена («Артефакты», «Прокачка: числа»): ступень N стоит компоненты ранга N двух семейств сегмента,
 * путь основы берёт их 3:1, путь деталей — 1:3; у семейств с двумя вариациями часть компонентов рунная,
 * доля падает со ступенью. Вершина — компоненты III ранга обоих семейств и сгусток узла.
 */
public final class GaussUpgrades {
    /** Первая кнопка меню стола; дальше по 7 на сегмент: ступени I–III пути основы, I–III пути деталей, вершина. */
    public static final int BTN_FIRST = 200;
    public static final int NODES_PER_SEGMENT = 7;
    public static final int APEX = 2;

    /** Узел прокачки: путь 0 — основа, 1 — детали, {@link #APEX} — вершина (ступень 1). */
    public record Node(Segment segment, int path, int step) {
        public boolean isApex() {
            return path == APEX;
        }

        public String key() {
            return isApex() ? segment.apexKey() : segment.key(path);
        }

        public int button() {
            return BTN_FIRST + segment.ordinal() * NODES_PER_SEGMENT + (isApex() ? 6 : path * 3 + step - 1);
        }
    }

    public enum State { DONE, AVAILABLE, LOCKED }

    private GaussUpgrades() {
    }

    public static boolean isButton(int id) {
        return id >= BTN_FIRST && id < BTN_FIRST + GaussLook.SEGMENTS.length * NODES_PER_SEGMENT;
    }

    public static Node node(int button) {
        int index = button - BTN_FIRST;
        Segment segment = GaussLook.SEGMENTS[index / NODES_PER_SEGMENT];
        int n = index % NODES_PER_SEGMENT;
        return n == 6 ? new Node(segment, APEX, 1) : new Node(segment, n / 3, n % 3 + 1);
    }

    public static Node apex(Segment segment) {
        return new Node(segment, APEX, 1);
    }

    /** Состояние узла при таком облике (облик повторяет прогресс, поэтому годится и клиенту, и серверу). */
    public static State state(int look, Node node) {
        Segment s = node.segment();
        if (node.isApex()) {
            if (GaussLook.apex(look, s)) {
                return State.DONE;
            }
            return GaussLook.base(look, s) >= 3 || GaussLook.add(look, s) >= 3 ? State.AVAILABLE : State.LOCKED;
        }
        int done = GaussLook.step(look, s, node.path());
        return node.step() <= done ? State.DONE : node.step() == done + 1 ? State.AVAILABLE : State.LOCKED;
    }

    public static CostList cost(Node node) {
        Segment s = node.segment();
        List<ItemStack> items = new ArrayList<>();
        if (node.isApex()) {
            int per = UpgradeConfig.APEX_PER_FAMILY.get();
            addFamily(items, s.main(), 3, per, 0.5);
            addFamily(items, s.second(), 3, per, 0.5);
            int clusters = UpgradeConfig.APEX_CLUSTERS.get();
            if (clusters > 0) {
                items.add(new ItemStack(ModItems.cluster(UpgradeConfig.APEX_CLUSTER_LEVEL.get()), clusters));
            }
            return new CostList(List.copyOf(items));
        }
        int rank = node.step();
        int total = (int) Math.round(UpgradeConfig.at(UpgradeConfig.STEP_COMPONENTS.get(), rank, 4 + 2 * (rank - 1)));
        double magic = UpgradeConfig.at(UpgradeConfig.STEP_MAGIC_SHARE.get(), rank, 0.0);
        int big = (int) Math.round(total * 0.75);
        int small = total - big;
        addFamily(items, s.main(), rank, node.path() == 0 ? big : small, magic);
        addFamily(items, s.second(), rank, node.path() == 0 ? small : big, magic);
        return new CostList(List.copyOf(items));
    }

    /** Компоненты семейства: у семейств с двумя вариациями доля {@code magicShare} — рунные, остальные литые. */
    private static void addFamily(List<ItemStack> items, ComponentFamily family, int rank, int count, double magicShare) {
        if (count <= 0) {
            return;
        }
        if (family.nature() == ComponentFamily.Nature.BOTH) {
            int magic = (int) Math.round(count * magicShare);
            add(items, ModComponents.get(family, rank, ComponentVariant.MAGIC), magic);
            add(items, ModComponents.get(family, rank, ComponentVariant.TECH), count - magic);
        } else {
            add(items, ModComponents.get(family, rank, family.variants().get(0)), count);
        }
    }

    private static void add(List<ItemStack> items, Item item, int count) {
        if (item == null || count <= 0) {
            return;
        }
        for (ItemStack stack : items) {
            if (stack.is(item)) {
                stack.grow(count);
                return;
            }
        }
        items.add(new ItemStack(item, count));
    }

    /** Открыть узел: проверка порядка, оплата, запись прогресса, событие для сюжета. */
    public static TableActions.Result unlock(Player player, Node node) {
        ArtifactProgress p = TableActions.progress(player);
        if (p.step(TableActions.GAUSS_BUILT) == 0) {
            return TableActions.Result.NOT_CHOSEN;
        }
        State state = state(GaussLook.pack(p), node);
        if (state == State.DONE) {
            return TableActions.Result.ALREADY_DONE;
        }
        if (state == State.LOCKED) {
            return TableActions.Result.NOT_AVAILABLE;
        }
        CostList cost = cost(node);
        if (!cost.canPay(player)) {
            return TableActions.Result.NOT_ENOUGH;
        }
        cost.pay(player);
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(node.key(), node.step()));
        if (player.getServer() != null) {
            AdeptEvents.post(new ArtifactEvent.StepUnlocked(player, node.key(), node.step()));
        }
        return TableActions.Result.OK;
    }
}
