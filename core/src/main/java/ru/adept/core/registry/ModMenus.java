package ru.adept.core.registry;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.adept.core.base.MachineMenu;
import ru.adept.core.table.ArtifactTableMenu;

import static ru.adept.core.registry.ModRegistries.MENUS;

public final class ModMenus {
    public static final DeferredHolder<MenuType<?>, MenuType<ArtifactTableMenu>> ARTIFACT_TABLE =
            MENUS.register("artifact_table", () -> new MenuType<>(ArtifactTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> GENERATOR =
            MENUS.register("generator", () -> new MenuType<>(MachineMenu::generator, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> CRUSHER =
            MENUS.register("crusher", () -> new MenuType<>(MachineMenu::crusher, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMELTER =
            MENUS.register("smelter", () -> new MenuType<>(MachineMenu::smelter, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }

    static void init() {
    }
}
