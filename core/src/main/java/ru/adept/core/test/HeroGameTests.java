package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.HeroEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.hero.CommanderOrderItem;
import ru.adept.core.hero.HeroClass;
import ru.adept.core.hero.HeroEntity;
import ru.adept.core.hero.HeroRank;
import ru.adept.core.hero.HeroSpawner;
import ru.adept.core.hero.HeroSquads;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.registry.ModItems;
import ru.adept.core.threat.ThreatLevel;
import ru.adept.core.threat.ThreatManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Автотесты героев: размер, состав отрядов, щит воина, отступление отряда и события, приказ командира. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HeroGameTests {
    private HeroGameTests() {
    }

    @GameTest(template = "empty")
    public static void heroSizeAndComposition(GameTestHelper helper) {
        EntityType<?> type = ModEntities.HERO.get();
        helper.assertTrue(Math.abs(type.getWidth() - 0.6f) < 1e-3 && Math.abs(type.getHeight() - 1.8f) < 1e-3, "Герой не 0,6 × 1,8");
        RandomSource r = RandomSource.create(1);
        helper.assertTrue(HeroSpawner.composition(ThreatLevel.CALM, r).isEmpty(), "Набег при спокойной угрозе");
        for (int i = 0; i < 20; i++) {
            int scouts = HeroSpawner.composition(ThreatLevel.SCOUTS, r).size();
            int squads = HeroSpawner.composition(ThreatLevel.SQUADS, r).size();
            List<HeroClass> war = HeroSpawner.composition(ThreatLevel.WARBANDS, r);
            helper.assertTrue(scouts >= 1 && scouts <= 2, "Разведка не 1–2");
            helper.assertTrue(squads >= 3 && squads <= 5, "Отряд не 3–5");
            helper.assertTrue(war.contains(HeroClass.COMMANDER) && war.size() >= 5, "Крупный отряд без командира");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void warriorShieldBlocksFront(GameTestHelper helper) {
        HeroEntity warrior = HeroSpawner.spawnHero(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), HeroClass.WARRIOR, HeroRank.APPRENTICE, null);
        warrior.setNoAi(true);
        warrior.setYBodyRot(0.0f);
        Cow front = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 5));
        Cow back = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 0));
        float health = warrior.getHealth();
        warrior.hurt(warrior.damageSources().mobAttack(front), 5.0f);
        helper.assertTrue(warrior.getHealth() == health, "Щит не принял удар спереди");
        warrior.hurt(warrior.damageSources().mobAttack(back), 5.0f);
        helper.assertTrue(warrior.getHealth() < health, "Удар в спину не прошёл");
        warrior.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void squadRetreatsAfterHalfLostAndLowersThreat(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        int oldThreat = ThreatManager.threat(server);
        AtomicInteger fell = new AtomicInteger();
        AtomicInteger retreat = new AtomicInteger();
        Object listener = new Object() {
            @SubscribeEvent
            public void fell(HeroEvent.AllyFell e) {
                fell.incrementAndGet();
            }

            @SubscribeEvent
            public void retreat(HeroEvent.SquadRetreating e) {
                retreat.incrementAndGet();
            }
        };
        NeoForge.EVENT_BUS.register(listener);
        try {
            ThreatManager.add(server, 60 - oldThreat, ThreatManager.Reason.COMMAND);
            HeroSquads squads = HeroSquads.get(server);
            HeroSquads.Squad squad = squads.create();
            List<HeroEntity> heroes = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                HeroEntity h = HeroSpawner.spawnHero(helper.getLevel(), helper.absolutePos(new BlockPos(1 + i, 2, 1)), HeroClass.WARRIOR, HeroRank.APPRENTICE, squad);
                h.setNoAi(true);
                heroes.add(h);
            }
            helper.assertTrue(squad.initial == 4, "В отряде не 4 героя");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            heroes.get(0).kill();
            helper.assertTrue(!squad.retreating, "Отступил после первой потери");
            heroes.get(1).kill();
            helper.assertTrue(squad.retreating && heroes.get(2).isRetreating(), "Не отступил, потеряв половину");
            helper.assertTrue(retreat.get() == 1 && fell.get() >= 3, "События: пал " + fell.get() + ", отступление " + retreat.get());
            heroes.get(2).kill();
            heroes.get(3).kill();
            helper.assertTrue(squads.byId(squad.id).isEmpty(), "Отряд не закончился");
            helper.assertTrue(ThreatManager.threat(server) == 60 - BalanceConfig.THREAT_RAID_DEFEATED.get(), "Угроза не упала после разгрома");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            ThreatManager.add(server, oldThreat - ThreatManager.threat(server), ThreatManager.Reason.COMMAND);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commanderOrderKeepsNames(GameTestHelper helper) {
        ItemStack order = CommanderOrderItem.create(ModItems.COMMANDER_ORDER.get(), "Ратибор", "Ольховка", 42L, 10, -20);
        var tag = CommanderOrderItem.data(order);
        helper.assertTrue(tag.getString("commander").equals("Ратибор") && tag.getString("village").equals("Ольховка")
                && tag.getLong("village_id") == 42L && tag.getInt("z") == -20, "Приказ не хранит имена и метку");
        helper.succeed();
    }
}
