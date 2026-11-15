package ru.adept.core.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.registry.ModMenus;

/** Клиентская регистрация ядра: экраны меню. */
@EventBusSubscriber(modid = AdeptCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CoreClient {
    private CoreClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ARTIFACT_TABLE.get(), ArtifactTableScreen::new);
        event.register(ModMenus.GENERATOR.get(), MachineScreen::new);
        event.register(ModMenus.CRUSHER.get(), MachineScreen::new);
        event.register(ModMenus.SMELTER.get(), MachineScreen::new);
        event.register(ModMenus.ALLOYER.get(), MachineScreen::new);
        event.register(ModMenus.CASTER.get(), MachineScreen::new);
        event.register(ModMenus.CUTTER.get(), MachineScreen::new);
        event.register(ModMenus.TRANSITION.get(), TransitionScreen::new);
    }

    /** Слой знака ранга героя. */
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HeroRankLayer.LAYER, HeroRankLayer::createLayer);
    }

    /** Заглушки до моделей из «Моделей и анимаций»: мобы рисуются цветными блоками своего размера. */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HORNET.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.YELLOW_CONCRETE.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.WISP.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.SHROOMLIGHT.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.EMBER_SPIRIT.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.MAGMA_BLOCK.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.HORNED_LEADER.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.MOSS_BLOCK.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.BOAR_LEADER.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.BROWN_TERRACOTTA.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.ARMADILLO.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.POLISHED_DEEPSLATE.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.SPORELING.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.SPORELING_SMALL.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.RESIDENT.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.OAK_PLANKS.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.ELDER.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.BOOKSHELF.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.HERO.get(), HeroRenderer::new);
        event.registerEntityRenderer(ModEntities.TRADER.get(), ctx -> new StubEntityRenderer<>(ctx, Blocks.CRYING_OBSIDIAN.defaultBlockState()));
        event.registerEntityRenderer(ModEntities.SOUL.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }
}
