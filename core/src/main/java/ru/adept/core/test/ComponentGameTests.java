package ru.adept.core.test;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.component.ComponentFamily;
import ru.adept.core.component.ComponentItem;
import ru.adept.core.component.ComponentVariant;
import ru.adept.core.component.ModComponents;

/** Автотесты компонентов среза 4: набор предметов, вариации по природе семейства, выдача командой. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ComponentGameTests {
    private ComponentGameTests() {
    }

    @GameTest(template = "empty")
    public static void componentSetComplete(GameTestHelper helper) {
        helper.assertTrue(ModComponents.all().size() == 66, "Ожидалось 66 компонентов, есть " + ModComponents.all().size());
        for (ComponentFamily family : ComponentFamily.values()) {
            for (ComponentVariant variant : ComponentVariant.values()) {
                boolean expected = family.variants().contains(variant);
                for (int rank = 1; rank <= ComponentFamily.MAX_RANK; rank++) {
                    ComponentItem item = ModComponents.get(family, rank, variant);
                    helper.assertTrue((item != null) == expected,
                            "Вариация " + variant.id() + " у " + family.id() + " " + (expected ? "отсутствует" : "лишняя"));
                    if (item == null) {
                        continue;
                    }
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    helper.assertTrue(id.getPath().equals(family.itemId(rank, variant)), "Неверный id: " + id);
                    helper.assertTrue(item.rank() == rank && item.family() == family && item.variant() == variant,
                            "Предмет " + id + " хранит не те ранг, семейство или вариацию");
                    String name = item.getName(new ItemStack(item)).getString();
                    helper.assertTrue(!name.isBlank() && !name.equals(id.getPath()), "У " + id + " пустое название: " + name);
                    helper.assertTrue(item.getDescriptionId().equals("item." + AdeptCore.MOD_ID + "." + id.getPath()),
                            "У " + id + " не тот ключ названия: " + item.getDescriptionId());
                }
            }
        }
        helper.assertTrue(ModComponents.get(ComponentFamily.COIL, 4, ComponentVariant.TECH) == null, "Ранг 4 не должен существовать");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void componentCommandGives(GameTestHelper helper) {
        // FakePlayer не входит в мир: моды вроде Curios не шлют ему пакеты синхронизации (в fullpack мок-игрок в мире падал).
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("adept_component_test".getBytes()), "adept_component_test"));
        player.getInventory().clearContent();
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withEntity(player).withPermission(4).withSuppressedOutput();
        var commands = helper.getLevel().getServer().getCommands();
        commands.performPrefixedCommand(source, "adept component coil 2 magic 5");
        commands.performPrefixedCommand(source, "adept component wiring 3");
        int coils = player.getInventory().countItem(ModComponents.get(ComponentFamily.COIL, 2, ComponentVariant.MAGIC));
        int wiring = player.getInventory().countItem(ModComponents.get(ComponentFamily.WIRING, 3, ComponentVariant.TECH));
        helper.assertTrue(coils == 5, "Магических катушек II выдано " + coils + " вместо 5");
        helper.assertTrue(wiring == 16, "Проводки III выдано " + wiring + " вместо 16");
        helper.succeed();
    }
}
