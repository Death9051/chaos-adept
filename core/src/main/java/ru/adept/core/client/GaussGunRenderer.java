package ru.adept.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.GaussGunItem;
import ru.adept.core.artifact.GaussLook;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Рендер гаусс-пушки (GeckoLib). Перед отрисовкой прячет кости, которых нет в облике предмета ({@link GaussLook}).
 * Свечение (руны, кристаллы, линзы, разрядники) — второй проход той же модели с текстурой glowmask и ванильным
 * {@code RenderType.eyes}: без своих шейдеров, совместимо с Iris (core/RENDERING.md).
 *
 * Для окна стола: если в данных предмета есть {@code focus} (номер сегмента + 1), остальные части затемняются —
 * так выбранный сегмент подсвечен.
 */
public class GaussGunRenderer extends GeoItemRenderer<GaussGunItem> {
    public static final String FOCUS_TAG = "focus";
    private static final ResourceLocation MODEL = id("geo/item/gauss_gun.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/artifact/gauss_gun.png");
    private static final ResourceLocation GLOW = id("textures/artifact/gauss_gun_glowmask.png");
    private static final ResourceLocation ANIMATION = id("animations/item/gauss_gun.animation.json");
    /** Цвет частей вне выбранного сегмента. */
    private static final int DIMMED = 0xFF5A5A66;

    /** Сегмент в фокусе текущей отрисовки: -1 — нет. */
    private int focus = -1;

    public GaussGunRenderer() {
        super(new Model());
        addRenderLayer(new GlowLayer(this));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, path);
    }

    private static final class Model extends GeoModel<GaussGunItem> {
        @Override
        public ResourceLocation getModelResource(GaussGunItem animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(GaussGunItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(GaussGunItem animatable) {
            return ANIMATION;
        }
    }

    @Override
    public void preRender(PoseStack poseStack, GaussGunItem animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        if (isReRender) {
            return;
        }
        ItemStack stack = getCurrentItemStack();
        int look = stack == null ? 0 : GaussGunItem.look(stack);
        focus = stack == null ? -1 : stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe().getInt(FOCUS_TAG) - 1;
        for (GeoBone bone : model.topLevelBones()) {
            applyLook(bone, look);
        }
    }

    private static void applyLook(GeoBone bone, int look) {
        bone.setHidden(!GaussLook.visible(look, bone.getName()));
        for (GeoBone child : bone.getChildBones()) {
            applyLook(child, look);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, GaussGunItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        int tint = colour;
        if (focus >= 0) {
            GaussLook.Segment segment = GaussLook.segmentOf(bone.getName());
            if (segment == null || segment.ordinal() != focus) {
                tint = DIMMED;
            } else {
                tint = 0xFFFFFFFF;
            }
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, tint);
    }

    /** Слой свечения: та же модель с маской свечения, полная яркость. */
    private static final class GlowLayer extends GeoRenderLayer<GaussGunItem> {
        GlowLayer(GeoRenderer<GaussGunItem> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, GaussGunItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                           MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            RenderType glow = RenderType.eyes(GLOW);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow),
                    partialTick, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
    }
}
