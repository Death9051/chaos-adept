package ru.adept.core.rune;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.config.RuneConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Материал гравировки: множитель эффекта, шанс не стереться и модификатор (по id) для всех рун изделия.
 * Список — {@code runeMaterials.materials} в конфиге.
 */
public record RuneMaterial(String id, String item, double multiplier, double keep) {
    public static final String COPPER = "copper";
    public static final String GOLD = "gold";
    public static final String RED_GOLD = "red_gold";
    public static final String NETHERITE = "netherite";
    public static final String DIAMOND = "diamond";
    public static final String EMERALD = "emerald";
    public static final String LAPIS = "lapis";
    public static final String REDSTONE = "redstone";

    public static List<RuneMaterial> all() {
        List<RuneMaterial> out = new ArrayList<>();
        for (String raw : RuneConfig.RUNE_MATERIALS.get()) {
            String[] p = raw.split("\\|");
            if (p.length != 4) {
                continue;
            }
            try {
                out.add(new RuneMaterial(p[0].trim(), p[1].trim(), Double.parseDouble(p[2].trim()), Double.parseDouble(p[3].trim())));
            } catch (NumberFormatException ignored) {
                // строка с ошибкой пропускается
            }
        }
        return out;
    }

    public static Optional<RuneMaterial> byId(String id) {
        return all().stream().filter(m -> m.id.equals(id)).findFirst();
    }

    public static Optional<RuneMaterial> of(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return all().stream().filter(m -> m.item.equals(key)).findFirst();
    }
}
