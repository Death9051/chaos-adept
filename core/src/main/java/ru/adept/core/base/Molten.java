package ru.adept.core.base;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.adept.core.registry.ModItems;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/** Расплавы среза 2: металлы плавильни и сплавы сплавщика. Иглы и пластины — по материалу. */
public enum Molten {
    IRON(() -> Items.IRON_INGOT),
    COPPER(() -> Items.COPPER_INGOT),
    GOLD(() -> Items.GOLD_INGOT),
    STEEL(() -> ModItems.STEEL_INGOT.get()),
    RED_GOLD(() -> ModItems.RED_GOLD_INGOT.get());

    private final Supplier<Item> ingot;

    Molten(Supplier<Item> ingot) {
        this.ingot = ingot;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Item ingot() {
        return ingot.get();
    }

    public Item needle() {
        return ModItems.NEEDLES.get(ordinal()).get();
    }

    public Item plate() {
        return ModItems.PLATES.get(ordinal()).get();
    }

    public static Optional<Molten> byId(String id) {
        for (Molten m : values()) {
            if (m.id().equals(id)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    public static Optional<Molten> ofIngot(ItemStack stack) {
        for (Molten m : values()) {
            if (stack.is(m.ingot())) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    /** Материал иглы или пластины по предмету. */
    public static Optional<Molten> ofPart(ItemStack stack) {
        for (Molten m : values()) {
            if (stack.is(m.needle()) || stack.is(m.plate())) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}
