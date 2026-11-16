package ru.adept.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.GaussLook;
import ru.adept.core.artifact.GaussLook.Segment;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Рендер предмета-артефакта с моделью по сегментам (GeckoLib): гаусс-пушка, энергоклинок, показ тяжёлой брони.
 * Перед отрисовкой прячет кости, которых нет в облике предмета ({@link GaussLook}, ключ {@code look} в данных).
 * Свечение (руны, кристаллы, линзы, разрядники) — второй проход той же модели с текстурой glowmask и ванильным
 * {@code RenderType.eyes}: без своих шейдеров, совместимо с Iris (core/RENDERING.md).
 *
 * Для окна стола: если в данных предмета есть {@code focus} (номер сегмента + 1), остальные части затемняются —
 * так выбранный сегмент подсвечен.
 */
public class ArtifactItemRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {
    public static final String FOCUS_TAG = "focus";
    /** Цвет частей вне выбранного сегмента. */
    private static final int DIMMED = 0xFF5A5A66;

    private final Segment[] segments;
    /** Сегмент в фокусе текущей отрисовки: -1 — нет. */
    private int focus = -1;

    /**
     * @param geo        путь модели внутри {@code geo/}, например {@code item/gauss_gun}
     * @param texture    имя текстуры в {@code textures/artifact/} без расширения
     * @param animation  имя файла анимаций в {@code animations/item/} без расширения
     */
    public ArtifactItemRenderer(String geo, String texture, String animation, Segment[] segments) {
        super(new Model<>(id("geo/" + geo + ".geo.json"), id("textures/artifact/" + texture + ".png"),
                id("animations/item/" + animation + ".animation.json")));
        this.segments = segments;
        addRenderLayer(new GlowLayer<>(this, id("textures/artifact/" + texture + "_glowmask.png")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, path);
    }

    private static final class Model<T extends Item & GeoAnimatable> extends GeoModel<T> {
        private final ResourceLocation model;
        private final ResourceLocation texture;
        private final ResourceLocation animation;

        Model(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
            this.model = model;
            this.texture = texture;
            this.animation = animation;
        }

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return model;
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return texture;
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return animation;
        }
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        if (isReRender) {
            return;
        }
        ItemStack stack = getCurrentItemStack();
        CompoundTag tag = stack == null ? new CompoundTag() : stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        int look = tag.getInt(GaussLook.TAG);
        focus = tag.getInt(FOCUS_TAG) - 1;
        for (GeoBone bone : model.topLevelBones()) {
            applyLook(bone, look);
        }
    }

    private void applyLook(GeoBone bone, int look) {
        bone.setHidden(!GaussLook.visible(segments, look, bone.getName()));
        for (GeoBone child : bone.getChildBones()) {
            applyLook(child, look);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        int tint = colour;
        if (focus >= 0) {
            Segment segment = GaussLook.segmentOf(segments, bone.getName());
            if (segment == null || segment.ordinal() != focus) {
                // Свечение затемнённых частей почти гасим: слой eyes складывается с фоном и иначе горит пятном.
                tint = isReRender ? 0xFF16161A : DIMMED;
            } else {
                tint = 0xFFFFFFFF;
            }
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, tint);
    }

    /** Слой свечения: та же модель с маской свечения, полная яркость. */
    private static final class GlowLayer<T extends Item & GeoAnimatable> extends GeoRenderLayer<T> {
        private final ResourceLocation glowmask;

        GlowLayer(GeoRenderer<T> renderer, ResourceLocation glowmask) {
            super(renderer);
            this.glowmask = glowmask;
        }

        @Override
        public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                           MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            RenderType glow = RenderType.eyes(glowmask);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow),
                    partialTick, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
    }
}
