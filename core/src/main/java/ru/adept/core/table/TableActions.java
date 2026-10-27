package ru.adept.core.table;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.ArtifactEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;

import java.util.List;
import java.util.function.Supplier;

/**
 * Действия стола артефактов на сервере. Экран стола только вызывает их по кнопкам;
 * вся проверка цены и прогресса — здесь (и в геймтестах).
 */
public final class TableActions {
    public static final String MELEE = "melee";
    public static final String BLADE = "blade";
    public static final String HAMMER = "hammer";
    /** Ключ «прототип собран» в ступенях прогресса. */
    public static final String BLADE_BUILT = "blade/base";
    public static final String BLADE_EDGE = "blade/emitter/edge";

    /** Грубые компоненты, которые делаются на столе вручную. Порядок = порядок кнопок. */
    public static final List<Craft> CRUDE = List.of(
            new Craft("crude_wiring", () -> ModItems.CRUDE_WIRING.get(), BalanceConfig.TABLE_CRUDE_WIRING::get),
            new Craft("crude_coil", () -> ModItems.CRUDE_COIL.get(), BalanceConfig.TABLE_CRUDE_COIL::get),
            new Craft("crude_emitter", () -> ModItems.CRUDE_EMITTER.get(), BalanceConfig.TABLE_CRUDE_EMITTER::get),
            new Craft("crude_mana_circuit", () -> ModItems.CRUDE_MANA_CIRCUIT.get(), BalanceConfig.TABLE_CRUDE_MANA_CIRCUIT::get));

    public record Craft(String id, Supplier<Item> output, Supplier<String> cost) {
        public CostList costList() {
            return CostList.parse(cost.get());
        }
    }

    public enum Result { OK, ALREADY_CHOSEN, NOT_CHOSEN, NOT_ENOUGH, ALREADY_DONE, NOT_AVAILABLE }

    private TableActions() {
    }

    public static ArtifactProgress progress(Player player) {
        return player.getData(ModRegistries.ARTIFACT_PROGRESS);
    }

    /** Выбор пары ближнего боя. Исключающий: второй раз выбрать нельзя. */
    public static Result choosePair(Player player, String variant) {
        ArtifactProgress p = progress(player);
        if (p.choice(MELEE).isPresent()) {
            return Result.ALREADY_CHOSEN;
        }
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withChoice(MELEE, variant));
        post(player, new ArtifactEvent.PairChosen(player, MELEE, variant));
        return Result.OK;
    }

    public static Result craftCrude(Player player, int index) {
        if (index < 0 || index >= CRUDE.size()) {
            return Result.NOT_AVAILABLE;
        }
        Craft craft = CRUDE.get(index);
        CostList cost = craft.costList();
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        give(player, new ItemStack(craft.output().get()));
        return Result.OK;
    }

    /** Резонатор — одиночный артефакт, можно собрать заново, если потерян. */
    public static Result craftResonator(Player player) {
        CostList cost = CostList.parse(BalanceConfig.TABLE_RESONATOR.get());
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        give(player, new ItemStack(ModItems.RESONATOR.get()));
        post(player, new ArtifactEvent.PrototypeCrafted(player, "resonator", player.getInventory().countItem(ModItems.RESONATOR.get()) == 1));
        return Result.OK;
    }

    /** Сборка прототипа клинка. Прототип можно пересобрать, если потерян. */
    public static Result craftBladePrototype(Player player) {
        ArtifactProgress p = progress(player);
        if (!p.choice(MELEE).filter(BLADE::equals).isPresent()) {
            return Result.NOT_CHOSEN;
        }
        CostList cost = CostList.parse(BalanceConfig.TABLE_BLADE_PROTOTYPE.get());
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        give(player, new ItemStack(ModItems.ENERGY_BLADE.get()));
        boolean first = p.step(BLADE_BUILT) == 0;
        if (first) {
            player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(BLADE_BUILT, 1));
        }
        post(player, new ArtifactEvent.PrototypeCrafted(player, BLADE, first));
        return Result.OK;
    }

    /** Ступень I пути «Кромка» сегмента «Эмиттер». В срезе — единственная ступень. */
    public static Result upgradeBladeEdge(Player player) {
        ArtifactProgress p = progress(player);
        if (p.step(BLADE_BUILT) == 0) {
            return Result.NOT_CHOSEN;
        }
        if (p.step(BLADE_EDGE) >= 1) {
            return Result.ALREADY_DONE;
        }
        CostList cost = CostList.parse(BalanceConfig.TABLE_BLADE_EDGE_1.get());
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(BLADE_EDGE, 1));
        post(player, new ArtifactEvent.StepUnlocked(player, BLADE_EDGE, 1));
        return Result.OK;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void post(Player player, ArtifactEvent event) {
        if (player.getServer() != null) {
            AdeptEvents.post(event);
        }
    }
}
