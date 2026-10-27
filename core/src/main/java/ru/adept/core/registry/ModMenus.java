package ru.adept.core.registry;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.adept.core.table.ArtifactTableMenu;

import static ru.adept.core.registry.ModRegistries.MENUS;

public final class ModMenus {
    public static final DeferredHolder<MenuType<?>, MenuType<ArtifactTableMenu>> ARTIFACT_TABLE =
            MENUS.register("artifact_table", () -> new MenuType<>(ArtifactTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }

    static void init() {
    }
}
