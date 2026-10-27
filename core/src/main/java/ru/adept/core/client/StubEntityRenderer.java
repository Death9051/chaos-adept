package ru.adept.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/** Заглушка рендера: блок, растянутый по размеру хитбокса сущности. Заменяется моделями GeckoLib. */
public class StubEntityRenderer<T extends Entity> extends EntityRenderer<T> {
    private final BlockState block;

    public StubEntityRenderer(EntityRendererProvider.Context context, BlockState block) {
        super(context);
        this.block = block;
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(entity.getBbWidth(), entity.getBbHeight(), entity.getBbWidth());
        pose.translate(-0.5, 0.0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
