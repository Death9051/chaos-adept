package ru.adept.core.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.EventHooks;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.mob.EmberSpiritEntity;
import ru.adept.core.mob.Possession;
import ru.adept.core.registry.ModEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Появление волн защитников вокруг узла. */
public final class WaveSpawner {
    /** Запасные звери, если в биоме нет подходящих. */
    private static final List<EntityType<?>> FALLBACK = List.of(EntityType.WOLF, EntityType.PIG, EntityType.COW, EntityType.SHEEP);

    private WaveSpawner() {
    }

    public static ExtractionMath.Composition composition(int wave, int waves) {
        return ExtractionMath.composition(wave, waves,
                BalanceConfig.WAVE_POSSESSED_BASE.get(), BalanceConfig.WAVE_POSSESSED_PER_WAVE.get(),
                BalanceConfig.WAVE_HORNETS_FROM.get(), BalanceConfig.WAVE_HORNETS_COUNT.get(),
                BalanceConfig.WAVE_WISPS_FROM.get(), BalanceConfig.WAVE_WISPS_COUNT.get(),
                BalanceConfig.WAVE_SPIRITS_FROM.get(), BalanceConfig.WAVE_SPIRITS_COUNT.get());
    }

    /** Создать волну; возвращает идентификаторы появившихся мобов. */
    public static List<UUID> spawn(ServerLevel level, ExtractionSession s, int wave, LivingEntity target) {
        ExtractionMath.Composition c = composition(wave, s.waves);
        List<UUID> spawned = new ArrayList<>();
        List<EntityType<?>> fauna = fauna(level, s.node);
        RandomSource random = level.random;
        for (int i = 0; i < c.possessed(); i++) {
            EntityType<?> type = fauna.get(random.nextInt(fauna.size()));
            Mob mob = Possession.spawnPossessed(level, type, ringPos(level, s.node, random), target);
            if (mob != null) {
                spawned.add(mob.getUUID());
            }
        }
        for (int i = 0; i < c.hornets(); i++) {
            add(level, ModEntities.HORNET.get().create(level), ringPos(level, s.node, random).above(3),
                    BalanceConfig.HORNET_HEALTH.get(), -1, target, spawned);
        }
        for (int i = 0; i < c.wisps(); i++) {
            add(level, ModEntities.WISP.get().create(level), ringPos(level, s.node, random).above(2),
                    BalanceConfig.WISP_HEALTH.get(), -1, target, spawned);
        }
        for (int i = 0; i < c.spirits(); i++) {
            EmberSpiritEntity spirit = ModEntities.EMBER_SPIRIT.get().create(level);
            if (spirit != null) {
                spirit.setTargetPos(s.extractor);
            }
            add(level, spirit, ringPos(level, s.node, random), BalanceConfig.SPIRIT_HEALTH.get(), -1, null, spawned);
        }
        if (c.guardian()) {
            add(level, ModEntities.HORNED_LEADER.get().create(level), ringPos(level, s.node, random),
                    BalanceConfig.GUARDIAN_HEALTH.get(), BalanceConfig.GUARDIAN_DAMAGE.get(), target, spawned);
        }
        return spawned;
    }

    /** Поставить отдельного моба (для отладочной команды /adept guardian и волн). */
    public static void add(ServerLevel level, Mob mob, BlockPos pos, double health, double damage, LivingEntity target, List<UUID> out) {
        if (mob == null) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        setBase(mob.getAttribute(Attributes.MAX_HEALTH), health);
        if (damage >= 0) {
            setBase(mob.getAttribute(Attributes.ATTACK_DAMAGE), damage);
        }
        mob.setHealth(mob.getMaxHealth());
        mob.setPersistenceRequired();
        if (target != null) {
            mob.setTarget(target);
        }
        level.addFreshEntity(mob);
        if (out != null) {
            out.add(mob.getUUID());
        }
    }

    private static void setBase(AttributeInstance attribute, double value) {
        if (attribute != null) {
            attribute.setBaseValue(value);
        }
    }

    /** Звери биома узла, из которых душа мира делает одержимых. */
    private static List<EntityType<?>> fauna(ServerLevel level, BlockPos node) {
        Holder<Biome> biome = level.getBiome(node);
        List<EntityType<?>> types = new ArrayList<>();
        for (MobSpawnSettings.SpawnerData data : biome.value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap()) {
            if (!types.contains(data.type)) {
                types.add(data.type);
            }
        }
        return types.isEmpty() ? FALLBACK : types;
    }

    private static BlockPos ringPos(ServerLevel level, BlockPos center, RandomSource random) {
        int min = BalanceConfig.WAVE_SPAWN_MIN_RADIUS.get();
        int max = Math.max(min, BalanceConfig.WAVE_SPAWN_MAX_RADIUS.get());
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = min + random.nextDouble() * (max - min);
        int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
        int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }
}
