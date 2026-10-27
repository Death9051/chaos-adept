package ru.adept.core.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.adept.core.AdeptCore;
import ru.adept.core.mob.EmberSpiritEntity;
import ru.adept.core.mob.HornedLeaderEntity;
import ru.adept.core.mob.HornetEntity;
import ru.adept.core.mob.WispEntity;

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
    }
}
