package ru.adept.core.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.table.TableActions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Описания ступеней прокачки для игрока: что делает ступень, числа, условия срабатывания.
 * Тексты лежат в языковых файлах ({@code adept_core.step.<ключ>.name} и {@code .desc}),
 * числа подставляются из конфига в момент показа, поэтому правка баланса сразу видна в описании.
 * Каждый новый текст перед pull request проходит навык humanizer.
 */
public final class StepTexts {
    /** Ступень: ключ прогресса и ступень пути (1–3, вершина — 1), аргументы описания из конфига. */
    public record Step(String key, int level, Supplier<Object[]> args) {
        public String langBase() {
            return "adept_core.step." + key.replace('/', '.') + "." + level;
        }

        public MutableComponent name() {
            return Component.translatable(langBase() + ".name");
        }

        public MutableComponent description() {
            return Component.translatable(langBase() + ".desc", args.get());
        }
    }

    private static final List<Step> STEPS = List.of(
            new Step(TableActions.BLADE_EDGE, 1, () -> new Object[]{
                    format(BalanceConfig.BLADE_EDGE_DAMAGE_PER_STEP.get()),
                    format(BalanceConfig.BLADE_BASE_DAMAGE.get() + BalanceConfig.BLADE_EDGE_DAMAGE_PER_STEP.get())})
    );

    private static final Map<String, Step> BY_ID = STEPS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(s -> s.key() + "#" + s.level(), s -> s));

    private StepTexts() {
    }

    public static List<Step> all() {
        return STEPS;
    }

    public static Optional<Step> find(String key, int level) {
        return Optional.ofNullable(BY_ID.get(key + "#" + level));
    }

    /** Название и описание одной строкой для подсказки: название золотом, описание серым. */
    public static MutableComponent tooltip(Step step) {
        return step.name().withStyle(ChatFormatting.GOLD)
                .append(Component.literal("\n"))
                .append(step.description().withStyle(ChatFormatting.GRAY));
    }

    /** Число для текста: без «.0» у целых, с запятой у дробных. */
    public static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value).replace('.', ',');
    }
}
