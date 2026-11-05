package ru.adept.core.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import ru.adept.core.hero.HeroEntity;

/**
 * Герои рисуются моделью игрока (ванильный рендер-тип, совместим с Iris). Скины — заглушка до выбора
 * из открытой базы скинов: ванильные Стив и Алекс по классу.
 */
public class HeroRenderer extends HumanoidMobRenderer<HeroEntity, PlayerModel<HeroEntity>> {
    private static final ResourceLocation STEVE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final ResourceLocation ALEX = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/alex.png");

    public HeroRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(HeroEntity hero) {
        return switch (hero.heroClass()) {
            case ARCHER, TRACKER -> ALEX;
            default -> STEVE;
        };
    }
}
