package ru.adept.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ru.adept.core.AdeptCore;
import ru.adept.core.data.ArtifactProgress;

/**
 * Все регистры ядра в одном месте. Части среза добавляют свои объекты сюда
 * (блоки — BLOCKS, предметы — ITEMS и т. д.), чтобы порядок регистрации был очевиден.
 */
public final class ModRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AdeptCore.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AdeptCore.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AdeptCore.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AdeptCore.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdeptCore.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AdeptCore.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AdeptCore.MOD_ID);

    /** Прогресс артефактов игрока: выбранные пары и открытые ступени. Переживает смерть. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ArtifactProgress>> ARTIFACT_PROGRESS =
            ATTACHMENTS.register("artifact_progress", () -> AttachmentType.builder(() -> ArtifactProgress.EMPTY)
                    .serialize(ArtifactProgress.CODEC)
                    .copyOnDeath()
                    .build());

    /** Вкладка творческого режима: всё, что зарегистрировано в ITEMS. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.adept_core"))
                    .icon(() -> new ItemStack(Items.AMETHYST_SHARD))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    private ModRegistries() {
    }

    public static void register(IEventBus modBus) {
        // Классы с объектами загружаются здесь, до регистрации.
        ModBlocks.init();
        ModItems.init();
        ModMenus.init();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        TABS.register(modBus);
        MENUS.register(modBus);
        ATTACHMENTS.register(modBus);
    }
}
