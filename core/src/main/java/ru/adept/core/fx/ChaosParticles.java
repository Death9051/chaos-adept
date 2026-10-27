package ru.adept.core.fx;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Общие эффекты Хаоса: бледно-оранжевые частицы со случайными переливами других цветов
 * и рваные дуги разрядов. Пока на ванильных частицах; текстуры от «Моделей» заменят их позже.
 * Пользуются ядро (ритуал, врата), «Сюжет» и модели.
 */
public final class ChaosParticles {
    private static final Vector3f CHAOS = new Vector3f(0.97f, 0.72f, 0.52f);

    private ChaosParticles() {
    }

    private static DustParticleOptions dust(RandomSource random) {
        if (random.nextFloat() < 0.2f) {
            return new DustParticleOptions(new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat()), 1.2f);
        }
        return new DustParticleOptions(CHAOS, 1.2f);
    }

    /** Разрыв: вертикальное кольцо частиц радиусом {@code radius}; {@code open} 0…1 — насколько раскрыт. */
    public static void rift(ServerLevel level, Vec3 center, double radius, double open) {
        RandomSource random = level.random;
        int points = 24;
        double r = radius * Math.max(0.1, open);
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points + random.nextDouble() * 0.2;
            double x = center.x + Math.cos(a) * r * 0.6;
            double y = center.y + Math.sin(a) * r;
            level.sendParticles(dust(random), x, y, center.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        if (random.nextFloat() < 0.4f) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 4, r * 0.4, r * 0.6, 0.1, 0.05);
        }
    }

    /** Мерцание вокруг сущности или точки. */
    public static void flicker(ServerLevel level, Vec3 center, double size) {
        RandomSource random = level.random;
        level.sendParticles(dust(random), center.x, center.y, center.z, 6, size, size, size, 0.0);
    }

    /** Рваная дуга разряда между двумя точками. */
    public static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        RandomSource random = level.random;
        int steps = Math.max(4, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, (double) i / steps)
                    .add((random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** Столб частиц вверх. */
    public static void column(ServerLevel level, Vec3 base, double height) {
        RandomSource random = level.random;
        for (double y = 0; y < height; y += 0.25) {
            level.sendParticles(dust(random), base.x, base.y + y, base.z, 1, 0.1, 0.05, 0.1, 0.0);
        }
    }
}
