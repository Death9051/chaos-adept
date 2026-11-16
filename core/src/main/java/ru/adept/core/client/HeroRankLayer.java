package ru.adept.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import ru.adept.core.AdeptCore;
import ru.adept.core.hero.HeroEntity;

/**
 * Знак ранга на облике героя: повязка на правом плече и 1–3 золотых кольца под ней (ученик, ветеран, мастер).
 * Ранг читается по числу колец, а не только по цвету. Отдельная модель, раздутая поверх брони, ванильный
 * рендер-тип брони (совместим с Iris). Текстуры {@code textures/entity/hero/rank_<ранг>.png}, раскладка как у брони 64×32;
 * сейчас — заглушки «Ядра», окончательные рисуют «Модели».
 */
public class HeroRankLayer extends RenderLayer<HeroEntity, PlayerModel<HeroEntity>> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "hero_rank"), "main");

    private final HumanoidModel<HeroEntity> model;

    public HeroRankLayer(RenderLayerParent<HeroEntity, PlayerModel<HeroEntity>> parent, HumanoidModel<HeroEntity> model) {
        super(parent);
        this.model = model;
    }

    /** Раздута сильнее нагрудника (1.0), чтобы повязка была видна и поверх брони героя. */
    public static LayerDefinition createLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.15f), 0.0f), 64, 32);
    }

    public static ResourceLocation texture(HeroEntity hero) {
        return ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "textures/entity/hero/rank_" + hero.rank().id() + ".png");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, HeroEntity hero, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (hero.isInvisible()) {
            return;
        }
        getParentModel().copyPropertiesTo(model);
        model.setAllVisible(false);
        model.rightArm.visible = true;
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture(hero))), light, OverlayTexture.NO_OVERLAY);
    }
}
