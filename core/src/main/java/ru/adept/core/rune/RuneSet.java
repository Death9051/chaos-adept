package ru.adept.core.rune;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import ru.adept.core.config.RuneConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Гравировка изделия: руны по порядку (1–2 на литом изделии) и материал. Хранится в данных предмета
 * ({@code runes} — id через запятую, {@code rune_material}) и в данных пластины внутри брони — теми же ключами.
 */
public record RuneSet(List<Rune> runes, String material) {
    public static final RuneSet EMPTY = new RuneSet(List.of(), "");
    public static final String KEY_RUNES = "runes";
    public static final String KEY_MATERIAL = "rune_material";

    public RuneSet {
        runes = List.copyOf(runes);
    }

    public boolean isEmpty() {
        return runes.isEmpty();
    }

    public Rune first() {
        return runes.isEmpty() ? null : runes.get(0);
    }

    public Rune second() {
        return runes.size() < 2 ? null : runes.get(1);
    }

    /** Особое слово, если порядок рун точно совпал. */
    public RuneWord word() {
        return runes.size() == 2 ? RuneWord.of(runes.get(0), runes.get(1)) : null;
    }

    public RuneMaterial materialData() {
        return RuneMaterial.byId(material).orElse(new RuneMaterial(material, "", 1.0, 0.5));
    }

    public boolean modifier(String id) {
        return id.equals(material);
    }

    /** Множитель эффекта: материал, у красного золота («Созвучие») — прибавка за две одинаковые руны. */
    public double multiplier() {
        double m = materialData().multiplier();
        if (modifier(RuneMaterial.RED_GOLD) && runes.size() == 2 && runes.get(0) == runes.get(1)) {
            m += RuneConfig.MOD_RESONANCE.get();
        }
        return m;
    }

    public static RuneSet read(CompoundTag tag) {
        String raw = tag.getString(KEY_RUNES);
        if (raw.isEmpty()) {
            return EMPTY;
        }
        List<Rune> list = new ArrayList<>();
        for (String id : raw.split(",")) {
            Rune r = Rune.byId(id.trim());
            if (r != null) {
                list.add(r);
            }
        }
        return list.isEmpty() ? EMPTY : new RuneSet(list, tag.getString(KEY_MATERIAL));
    }

    public void write(CompoundTag tag) {
        if (isEmpty()) {
            tag.remove(KEY_RUNES);
            tag.remove(KEY_MATERIAL);
        } else {
            tag.putString(KEY_RUNES, runes.stream().map(Rune::id).collect(Collectors.joining(",")));
            tag.putString(KEY_MATERIAL, material);
        }
    }

    public static RuneSet of(ItemStack stack) {
        return read(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    public void applyTo(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        write(tag);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** Строка для магазина гаусс-пушки и логов: {@code current,echo@gold}. */
    public String encode() {
        return isEmpty() ? "" : runes.stream().map(Rune::id).collect(Collectors.joining(",")) + "@" + material;
    }

    public static RuneSet decode(String s) {
        int at = s.indexOf('@');
        if (s.isEmpty() || at < 0) {
            return EMPTY;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_RUNES, s.substring(0, at));
        tag.putString(KEY_MATERIAL, s.substring(at + 1));
        return read(tag);
    }
}
