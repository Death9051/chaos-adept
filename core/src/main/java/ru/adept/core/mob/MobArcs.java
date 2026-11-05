package ru.adept.core.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Проверка «удар спереди» для мобов со щитом или панцирем. */
public final class MobArcs {
    private MobArcs() {
    }

    /** Удар пришёл с передней дуги шириной {@code arcDegrees} (по направлению тела). */
    public static boolean isFromFront(LivingEntity mob, DamageSource source, double arcDegrees) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return false;
        }
        Vec3 to = from.subtract(mob.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        if (flat.lengthSqr() < 1.0e-4) {
            return false;
        }
        Vec3 facing = Vec3.directionFromRotation(0, mob.yBodyRot);
        return facing.dot(flat.normalize()) >= Math.cos(Math.toRadians(arcDegrees / 2.0));
    }
}
