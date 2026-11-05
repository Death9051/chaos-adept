package ru.adept.core.village;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

/**
 * Старейшина: один на деревню. При высокой угрозе вызывает героев (набеги), его смерть откладывает
 * набег в этой местности, но сильно поднимает угрозу. Размер заглушки: 0,6 × 1,95.
 */
public class ElderEntity extends ResidentEntity {
    public ElderEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
    }

    @Override
    public String role() {
        return "elder";
    }
}
