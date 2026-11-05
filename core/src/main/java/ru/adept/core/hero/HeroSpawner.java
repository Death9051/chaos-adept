package ru.adept.core.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.EventHooks;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.threat.ThreatLevel;
import ru.adept.core.village.NamePools;
import ru.adept.core.village.VillageRecord;

import java.util.ArrayList;
import java.util.List;

/** Появление героев и отрядов. Состав отряда — по ступени угрозы («Мобы», «Герои и набеги»). */
public final class HeroSpawner {
    private HeroSpawner() {
    }

    /** Один герой (отладка и отряды). */
    public static HeroEntity spawnHero(ServerLevel level, BlockPos pos, HeroClass heroClass, HeroRank rank, HeroSquads.Squad squad) {
        HeroEntity hero = ModEntities.HERO.get().create(level);
        if (hero == null) {
            return null;
        }
        hero.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        EventHooks.finalizeMobSpawn(hero, level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        hero.setup(heroClass, rank, squad == null ? null : squad.id);
        hero.setPersistenceRequired();
        level.addFreshEntity(hero);
        if (squad != null) {
            HeroSquads.get(level.getServer()).join(squad, hero);
        }
        return hero;
    }

    /** Классы отряда для ступени угрозы. */
    public static List<HeroClass> composition(ThreatLevel threat, RandomSource random) {
        List<HeroClass> out = new ArrayList<>();
        switch (threat) {
            case CALM -> {
            }
            case SCOUTS -> {
                out.add(HeroClass.TRACKER);
                if (random.nextFloat() < 0.5f) {
                    out.add(HeroClass.TRACKER);
                }
            }
            case SQUADS -> {
                int size = 3 + random.nextInt(3);
                out.add(HeroClass.TRACKER);
                for (int i = 1; i < size; i++) {
                    out.add(i % 2 == 1 ? HeroClass.WARRIOR : HeroClass.ARCHER);
                }
            }
            case WARBANDS -> {
                int size = 5 + random.nextInt(3);
                out.add(HeroClass.COMMANDER);
                out.add(HeroClass.TRACKER);
                for (int i = 2; i < size; i++) {
                    out.add(i % 2 == 0 ? HeroClass.WARRIOR : HeroClass.ARCHER);
                }
            }
        }
        return out;
    }

    /** Шанс, что рядовой — ветеран: растёт с угрозой и уровнем узлов. */
    public static float veteranChance(int threat, int tier) {
        double base = (threat - BalanceConfig.THREAT_SQUADS_AT.get()) / 50.0 + (tier - 1) * BalanceConfig.HERO_VETERAN_PER_TIER.get();
        return (float) Mth.clamp(base, 0.0, 0.9);
    }

    /**
     * Отряд вокруг точки (в кольце 12–20 блоков). Командир всегда ветеран и именной;
     * у крупных отрядов командир и один воин — верхом.
     */
    public static HeroSquads.Squad spawnSquad(ServerLevel level, BlockPos around, Player target, ThreatLevel threat, VillageRecord village) {
        RandomSource random = level.random;
        HeroSquads squads = HeroSquads.get(level.getServer());
        HeroSquads.Squad squad = squads.create();
        if (village != null) {
            squad.villageId = village.id;
            squad.villageName = village.name;
        }
        int tier = WorldProgress.get(level.getServer()).nodeTier();
        float veteran = veteranChance(WorldProgress.get(level.getServer()).threat(), tier);
        double angle = random.nextDouble() * Math.PI * 2;
        boolean mountedWarrior = false;
        for (HeroClass c : composition(threat, random)) {
            double a = angle + (random.nextDouble() - 0.5) * 0.8;
            double r = 12 + random.nextDouble() * 8;
            int x = around.getX() + Mth.floor(Math.cos(a) * r);
            int z = around.getZ() + Mth.floor(Math.sin(a) * r);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            HeroRank rank = c == HeroClass.COMMANDER || random.nextFloat() < veteran ? HeroRank.VETERAN : HeroRank.APPRENTICE;
            HeroEntity hero = spawnHero(level, pos, c, rank, squad);
            if (hero == null) {
                continue;
            }
            hero.setVillage(squad.villageId, squad.villageName);
            if (c == HeroClass.COMMANDER) {
                squad.commander = hero.getUUID();
                squad.commanderName = NamePools.pick(NamePools.commanders(), squads.takenCommanderNames(), random);
                hero.setHeroName(squad.commanderName);
            }
            boolean mount = threat == ThreatLevel.WARBANDS && (c == HeroClass.COMMANDER || (c == HeroClass.WARRIOR && !mountedWarrior));
            if (mount) {
                mountedWarrior |= c == HeroClass.WARRIOR;
                mount(level, hero);
            }
            if (target != null) {
                hero.setTarget(target);
            }
        }
        squads.setDirty();
        return squad;
    }

    private static void mount(ServerLevel level, HeroEntity hero) {
        Horse horse = EntityType.HORSE.create(level);
        if (horse == null) {
            return;
        }
        horse.moveTo(hero.getX(), hero.getY(), hero.getZ(), hero.getYRot(), 0f);
        horse.setTamed(true);
        horse.setPersistenceRequired();
        level.addFreshEntity(horse);
        hero.startRiding(horse, true);
    }
}
