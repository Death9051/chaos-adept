package ru.adept.core.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import ru.adept.core.AdeptCore;
import ru.adept.core.hero.HeroEntity;

/**
 * Герои рисуются моделью игрока (ванильный рендер-тип, совместим с Iris). Скины из открытой базы
 * (авторы и лицензии — docs/skins.md): по {@link #VARIANTS} на класс,
 * {@code textures/entity/hero/<класс>_<n>.png}; вариант постоянен для героя (по UUID).
 */
public class HeroRenderer extends HumanoidMobRenderer<HeroEntity, PlayerModel<HeroEntity>> {
    private static final int VARIANTS = 3;

    public HeroRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(HeroEntity hero) {
        int n = Math.floorMod(hero.getUUID().hashCode(), VARIANTS) + 1;
        return ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID,
                "textures/entity/hero/" + hero.heroClass().id() + "_" + n + ".png");
    }
}
