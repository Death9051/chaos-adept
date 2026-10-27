package ru.adept.core.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.WorldFlags;

/**
 * Серверные автотесты ядра (CI, servertest). В обычной игре не запускаются:
 * NeoForge включает их только для пространств имён из neoforge.enabledGameTestNamespaces.
 * Шаблон «empty» — пустая площадка 1×1×1 из data/adept_core/structure/empty.nbt.
 */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {
    }

    @GameTest(template = "empty")
    public static void worldFlagsRoundTrip(GameTestHelper helper) {
        WorldFlags flags = WorldFlags.get(helper.getLevel().getServer());
        flags.set("adept_core:ci_probe", "ok");
        helper.assertTrue(flags.get("adept_core:ci_probe").filter("ok"::equals).isPresent(),
                "Флаг мира не читается после записи");
        flags.remove("adept_core:ci_probe");
        helper.assertTrue(!flags.has("adept_core:ci_probe"), "Флаг мира не удаляется");
        helper.succeed();
    }
}
