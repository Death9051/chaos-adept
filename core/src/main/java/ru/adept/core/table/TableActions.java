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

    // Срез 2: броня и дальний бой
    public static final String ARMOR = "armor";
    public static final String LIGHT = "light";
    public static final String HEAVY = "heavy";
    public static final String RANGED = "ranged";
    public static final String GAUSS = "gauss";
    public static final String RAIL = "rail";
    public static final String HEAVY_BUILT = "heavy/base";
    public static final String HEAVY_BASTION = "heavy/chest/bastion";
    public static final String GAUSS_BUILT = "gauss/base";
    public static final String GAUSS_TEMPO = "gauss/barrel/tempo";

    // Срез 3: одиночные артефакты — накопитель и котёл душ
    public static final String ACCUMULATOR = "accumulator";
    public static final String ACCUMULATOR_BUILT = "accumulator/base";
    public static final String ACCUMULATOR_CAPACITY = "accumulator/core/capacity";
    public static final String CAULDRON = "cauldron";
    public static final String CAULDRON_BUILT = "cauldron/base";
    public static final String CAULDRON_YIELD = "cauldron/furnace/yield";

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
        return choosePair(player, MELEE, variant);
    }

    /** Выбор пары категории ({@code melee}, {@code armor}, {@code ranged}). Исключающий, один раз на прохождение. */
    public static Result choosePair(Player player, String category, String variant) {
        ArtifactProgress p = progress(player);
        if (p.choice(category).isPresent()) {
            return Result.ALREADY_CHOSEN;
        }
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withChoice(category, variant));
        post(player, new ArtifactEvent.PairChosen(player, category, variant));
        return Result.OK;
    }

    /** Прототип тяжёлой брони: шлем, нагрудник (с полным зарядом) и ноги, пластины ставятся отдельно. */
    public static Result craftHeavyPrototype(Player player) {
        ArtifactProgress p = progress(player);
        if (!p.choice(ARMOR).filter(HEAVY::equals).isPresent()) {
            return Result.NOT_CHOSEN;
        }
        CostList cost = CostList.parse(BalanceConfig.TABLE_HEAVY_PROTOTYPE.get());
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        give(player, new ItemStack(ModItems.HEAVY_HELMET.get()));
        ItemStack chest = new ItemStack(ModItems.HEAVY_CHESTPLATE.get());
        ru.adept.core.artifact.HeavyArmorItem.setCharge(chest, BalanceConfig.HEAVY_CHARGE_CAPACITY.get());
        give(player, chest);
        give(player, new ItemStack(ModItems.HEAVY_LEGGINGS.get()));
        boolean first = p.step(HEAVY_BUILT) == 0;
        if (first) {
            player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(HEAVY_BUILT, 1));
        }
        post(player, new ArtifactEvent.PrototypeCrafted(player, HEAVY, first));
        return Result.OK;
    }

    /** Прототип гаусс-пушки (с полным зарядом). */
    public static Result craftGaussPrototype(Player player) {
        ArtifactProgress p = progress(player);
        if (!p.choice(RANGED).filter(GAUSS::equals).isPresent()) {
            return Result.NOT_CHOSEN;
        }
        CostList cost = CostList.parse(BalanceConfig.TABLE_GAUSS_PROTOTYPE.get());
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        ItemStack gun = new ItemStack(ModItems.GAUSS_GUN.get());
        ru.adept.core.artifact.GaussGunItem.setCharge(gun, ru.adept.core.artifact.GaussGunItem.capacity());
        give(player, gun);
        boolean first = p.step(GAUSS_BUILT) == 0;
        if (first) {
            player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(GAUSS_BUILT, 1));
        }
        post(player, new ArtifactEvent.PrototypeCrafted(player, GAUSS, first));
        return Result.OK;
    }

    /** Ступень I «Бастиона» (нагрудник тяжёлой брони). */
    public static Result upgradeHeavyBastion(Player player) {
        return upgradeStep(player, HEAVY_BUILT, HEAVY_BASTION, BalanceConfig.TABLE_HEAVY_BASTION_1.get());
    }

    /** Ступень I «Темпа» (ствол гаусс-пушки). */
    public static Result upgradeGaussTempo(Player player) {
        return upgradeStep(player, GAUSS_BUILT, GAUSS_TEMPO, BalanceConfig.TABLE_GAUSS_TEMPO_1.get());
    }

    /** Накопитель — одиночный артефакт, можно собрать заново, если потерян. */
    public static Result craftAccumulator(Player player) {
        return craftSingle(player, ACCUMULATOR, ACCUMULATOR_BUILT, ru.adept.core.config.ManaConfig.TABLE_ACCUMULATOR.get(),
                () -> new ItemStack(ModItems.ACCUMULATOR.get()));
    }

    /** Котёл душ — одиночный артефакт. */
    public static Result craftCauldron(Player player) {
        return craftSingle(player, CAULDRON, CAULDRON_BUILT, ru.adept.core.config.ManaConfig.TABLE_CAULDRON.get(),
                () -> new ItemStack(ModItems.SOUL_CAULDRON.get()));
    }

    /** Ступень I «Ёмкости» (ядро накопителя). */
    public static Result upgradeAccumulatorCapacity(Player player) {
        return upgradeStep(player, ACCUMULATOR_BUILT, ACCUMULATOR_CAPACITY, ru.adept.core.config.ManaConfig.TABLE_ACCUMULATOR_CAPACITY_1.get());
    }

    /** Ступень I «Выхода» (горнило котла душ). */
    public static Result upgradeCauldronYield(Player player) {
        return upgradeStep(player, CAULDRON_BUILT, CAULDRON_YIELD, ru.adept.core.config.ManaConfig.TABLE_CAULDRON_YIELD_1.get());
    }

    private static Result craftSingle(Player player, String id, String built, String price, Supplier<ItemStack> result) {
        CostList cost = CostList.parse(price);
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        give(player, result.get());
        ArtifactProgress p = progress(player);
        boolean first = p.step(built) == 0;
        if (first) {
            player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(built, 1));
        }
        post(player, new ArtifactEvent.PrototypeCrafted(player, id, first));
        return Result.OK;
    }

    private static Result upgradeStep(Player player, String built, String step, String price) {
        ArtifactProgress p = progress(player);
        if (p.step(built) == 0) {
            return Result.NOT_CHOSEN;
        }
        if (p.step(step) >= 1) {
            return Result.ALREADY_DONE;
        }
        CostList cost = CostList.parse(price);
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(step, 1));
        post(player, new ArtifactEvent.StepUnlocked(player, step, 1));
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

    /** Лагерь — одиночный артефакт. */
    public static Result craftCamp(Player player) {
        CostList cost = CostList.parse(BalanceConfig.TABLE_CAMP.get());
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        give(player, new ItemStack(ModItems.CAMP.get()));
        post(player, new ArtifactEvent.PrototypeCrafted(player, "camp", player.getInventory().countItem(ModItems.CAMP.get()) == 1));
        return Result.OK;
    }

    /** Ключ к базе — делается на столе ещё в обычном мире. */
    public static Result craftKey(Player player) {
        CostList cost = CostList.parse(BalanceConfig.TABLE_KEY.get());
        if (!cost.canPay(player)) {
            return Result.NOT_ENOUGH;
        }
        cost.pay(player);
        give(player, new ItemStack(ModItems.KEY.get()));
        post(player, new ArtifactEvent.PrototypeCrafted(player, "key", player.getInventory().countItem(ModItems.KEY.get()) == 1));
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
