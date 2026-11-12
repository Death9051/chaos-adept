package ru.adept.core.focus;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.config.FocusConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Кристалл фокуса: сила (множитель эффекта), прочность (срабатываний до износа) и модификатор по id.
 * Список — {@code focus.crystals} в конфиге.
 */
public record FocusCrystal(String item, String id, double power, int durability) {
    public static final String QUARTZ = "quartz";
    public static final String LAPIS = "lapis";
    public static final String AMETHYST = "amethyst";
    public static final String EMERALD = "emerald";
    public static final String DIAMOND = "diamond";
    public static final String NODE = "node";

    public static List<FocusCrystal> all() {
        List<FocusCrystal> out = new ArrayList<>();
        for (String raw : FocusConfig.FOCUS_CRYSTALS.get()) {
            String[] p = raw.split("\\|");
            if (p.length != 4) {
                continue;
            }
            try {
                out.add(new FocusCrystal(p[0].trim(), p[1].trim(), Double.parseDouble(p[2].trim()), Integer.parseInt(p[3].trim())));
            } catch (NumberFormatException ignored) {
                // строка с ошибкой пропускается
            }
        }
        return out;
    }

    public static Optional<FocusCrystal> byId(String id) {
        return all().stream().filter(c -> c.id.equals(id)).findFirst();
    }

    /** Кристалл по предмету (для слота ограночной машины). */
    public static Optional<FocusCrystal> of(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return all().stream().filter(c -> c.item.equals(key)).findFirst();
    }
}
