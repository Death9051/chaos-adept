package ru.adept.core.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.adept.core.AdeptCore;
import ru.adept.core.mob.ArmadilloEntity;
import ru.adept.core.mob.BoarLeaderEntity;
import ru.adept.core.mob.EmberSpiritEntity;
import ru.adept.core.mob.SporelingEntity;
import ru.adept.core.mob.HornedLeaderEntity;
import ru.adept.core.mob.HornetEntity;
import ru.adept.core.mob.WispEntity;
import ru.adept.core.hero.HeroEntity;
import ru.adept.core.trader.TraderEntity;
import ru.adept.core.village.ElderEntity;
import ru.adept.core.village.ResidentEntity;

import static ru.adept.core.registry.ModRegistries.ENTITIES;

/** Сущности ядра. Модели пока заглушки (см. CoreClient). */
@EventBusSubscriber(modid = AdeptCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredHolder<EntityType<?>, EntityType<HornetEntity>> HORNET = ENTITIES.register("hornet",
            () -> EntityType.Builder.of(HornetEntity::new, MobCategory.MONSTER).sized(0.5f, 0.5f).clientTrackingRange(8).build("hornet"));
    public static final DeferredHolder<EntityType<?>, EntityType<WispEntity>> WISP = ENTITIES.register("wisp",
            () -> EntityType.Builder.of(WispEntity::new, MobCategory.MONSTER).sized(0.5f, 0.5f).clientTrackingRange(8).fireImmune().build("wisp"));
    public static final DeferredHolder<EntityType<?>, EntityType<EmberSpiritEntity>> EMBER_SPIRIT = ENTITIES.register("ember_spirit",
            () -> EntityType.Builder.of(EmberSpiritEntity::new, MobCategory.MONSTER).sized(0.6f, 1.2f).clientTrackingRange(8).fireImmune().build("ember_spirit"));
    public static final DeferredHolder<EntityType<?>, EntityType<HornedLeaderEntity>> HORNED_LEADER = ENTITIES.register("horned_leader",
            () -> EntityType.Builder.of(HornedLeaderEntity::new, MobCategory.MONSTER).sized(1.4f, 2.4f).clientTrackingRange(10).build("horned_leader"));

    // Срез 2: новые противники на узле. Размеры = размеры будущих моделей («Мобы»).
    public static final DeferredHolder<EntityType<?>, EntityType<BoarLeaderEntity>> BOAR_LEADER = ENTITIES.register("boar_leader",
            () -> EntityType.Builder.of(BoarLeaderEntity::new, MobCategory.MONSTER).sized(1.4f, 1.2f).clientTrackingRange(10).build("boar_leader"));
    public static final DeferredHolder<EntityType<?>, EntityType<ArmadilloEntity>> ARMADILLO = ENTITIES.register("armadillo",
            () -> EntityType.Builder.of(ArmadilloEntity::new, MobCategory.MONSTER).sized(1.0f, 0.8f).clientTrackingRange(8).build("armadillo"));
    public static final DeferredHolder<EntityType<?>, EntityType<SporelingEntity>> SPORELING = ENTITIES.register("sporeling",
            () -> EntityType.Builder.of(SporelingEntity::new, MobCategory.MONSTER).sized(0.8f, 1.1f).clientTrackingRange(8).build("sporeling"));
    public static final DeferredHolder<EntityType<?>, EntityType<SporelingEntity>> SPORELING_SMALL = ENTITIES.register("sporeling_small",
            () -> EntityType.Builder.of(SporelingEntity::new, MobCategory.MONSTER).sized(0.4f, 0.55f).clientTrackingRange(8).build("sporeling_small"));

    // Жители деревень (свои вместо ванильных)
    public static final DeferredHolder<EntityType<?>, EntityType<ResidentEntity>> RESIDENT = ENTITIES.register("resident",
            () -> EntityType.Builder.<ResidentEntity>of(ResidentEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).eyeHeight(1.62f)
                    .clientTrackingRange(10).build("resident"));
    public static final DeferredHolder<EntityType<?>, EntityType<ElderEntity>> ELDER = ENTITIES.register("elder",
            () -> EntityType.Builder.<ElderEntity>of(ElderEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).eyeHeight(1.62f)
                    .clientTrackingRange(10).build("elder"));

    // Герои (облик — модель игрока). 0,6 × 1,8.
    public static final DeferredHolder<EntityType<?>, EntityType<HeroEntity>> HERO = ENTITIES.register("hero",
            () -> EntityType.Builder.of(HeroEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .clientTrackingRange(10).build("hero"));

    public static final DeferredHolder<EntityType<?>, EntityType<TraderEntity>> TRADER = ENTITIES.register("trader",
            () -> EntityType.Builder.of(TraderEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("trader"));

    private ModEntities() {
    }

    static void init() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(HORNET.get(), HornetEntity.attributes().build());
        event.put(WISP.get(), WispEntity.attributes().build());
        event.put(EMBER_SPIRIT.get(), EmberSpiritEntity.attributes().build());
        event.put(HORNED_LEADER.get(), HornedLeaderEntity.attributes().build());
        event.put(BOAR_LEADER.get(), BoarLeaderEntity.attributes().build());
        event.put(ARMADILLO.get(), ArmadilloEntity.attributes().build());
        event.put(SPORELING.get(), SporelingEntity.attributes().build());
        event.put(SPORELING_SMALL.get(), SporelingEntity.smallAttributes().build());
        event.put(RESIDENT.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(ELDER.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(HERO.get(), HeroEntity.attributes().build());
        event.put(TRADER.get(), TraderEntity.attributes().build());
    }
}
