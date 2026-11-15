package ru.adept.core.artifact;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Предмет-показ тяжёлой силовой брони: вся модель брони целиком как предмет. Нужен окну мастерской на столе
 * артефактов (проекция с наездом на сегмент) и сценам автотеста; игроку не выдаётся. Облик — ключ {@code look}
 * в данных предмета, как у остальных артефактов ({@link GaussLook}).
 */
public class ArtifactDisplayItem extends Item implements GeoItem {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public ArtifactDisplayItem(Properties properties) {
        super(properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new ru.adept.core.client.ArtifactItemRenderer<ArtifactDisplayItem>(
                            "armor/heavy_armor", "heavy_armor", "gauss_gun", GaussLook.HEAVY);
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
