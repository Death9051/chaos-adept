package ru.adept.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.GaussLook;
import ru.adept.core.artifact.HeavyArmorItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Рендер тяжёлой силовой брони на игроке (GeckoLib). Какие части тела рисовать, решает слот предмета; какие кости
 * сегментов видны — облик в данных предмета ({@link GaussLook}, ключ {@code look}): кости вне облика пропускаются
 * при обходе. Свечение — второй проход с маской и ванильным {@code RenderType.eyes} (core/RENDERING.md).
 */
public class HeavyArmorRenderer extends GeoArmorRenderer<HeavyArmorItem> {
    private static final ResourceLocation MODEL = id("geo/armor/heavy_armor.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/artifact/heavy_armor.png");
    private static final ResourceLocation GLOW = id("textures/artifact/heavy_armor_glowmask.png");
    private static final ResourceLocation ANIMATION = id("animations/item/gauss_gun.animation.json");

    public HeavyArmorRenderer() {
        super(new Model());
        addRenderLayer(new GlowLayer(this));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, path);
    }

    private static final class Model extends GeoModel<HeavyArmorItem> {
        @Override
        public ResourceLocation getModelResource(HeavyArmorItem animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(HeavyArmorItem animatable) {
            return TEXTURE;
        }

        /** Своих анимаций у брони нет: контроллеров она не заводит, файл нужен только как заглушка. */
        @Override
        public ResourceLocation getAnimationResource(HeavyArmorItem animatable) {
            return ANIMATION;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, HeavyArmorItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        ItemStack stack = getCurrentStack();
        int look = stack == null ? 0 : stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe().getInt(GaussLook.TAG);
        if (!GaussLook.visible(GaussLook.HEAVY, look, bone.getName())) {
            return;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }

    /** Слой свечения: та же модель с маской свечения, полная яркость. */
    private static final class GlowLayer extends GeoRenderLayer<HeavyArmorItem> {
        GlowLayer(GeoRenderer<HeavyArmorItem> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, HeavyArmorItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                           MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            RenderType glow = RenderType.eyes(GLOW);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow),
                    partialTick, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
    }
}
