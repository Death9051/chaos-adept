package ru.adept.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Прогресс артефактов игрока (неизменяемый; изменения — через with*).
 *
 * @param choices выбранный вариант пары: категория → вариант, например {@code melee → blade}.
 *                Выбор исключающий и делается один раз на прохождение.
 * @param steps   открытые ступени: ключ {@code артефакт/сегмент/путь} → номер ступени (0–3),
 *                вершина — путь {@code apex} со значением 1.
 */
public record ArtifactProgress(Map<String, String> choices, Map<String, Integer> steps) {
    public static final ArtifactProgress EMPTY = new ArtifactProgress(Map.of(), Map.of());

    public static final Codec<ArtifactProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("choices", Map.of())
                    .forGetter(ArtifactProgress::choices),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("steps", Map.of())
                    .forGetter(ArtifactProgress::steps)
    ).apply(i, ArtifactProgress::new));

    public ArtifactProgress {
        choices = Map.copyOf(choices);
        steps = Map.copyOf(steps);
    }

    public Optional<String> choice(String category) {
        return Optional.ofNullable(choices.get(category));
    }

    public int step(String key) {
        return steps.getOrDefault(key, 0);
    }

    public ArtifactProgress withChoice(String category, String variant) {
        Map<String, String> copy = new HashMap<>(choices);
        copy.put(category, variant);
        return new ArtifactProgress(copy, steps);
    }

    public ArtifactProgress withStep(String key, int value) {
        Map<String, Integer> copy = new HashMap<>(steps);
        if (value <= 0) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }
        return new ArtifactProgress(choices, copy);
    }
}
