package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.HeroEvent;
import ru.adept.core.config.HeroConfig;
import ru.adept.core.hero.HeroClass;
import ru.adept.core.hero.HeroEntity;
import ru.adept.core.hero.HeroRank;
import ru.adept.core.hero.HeroSpawner;
import ru.adept.core.hero.HeroSpellEntity;
import ru.adept.core.hero.HeroSquads;
import ru.adept.core.threat.ThreatLevel;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Автотесты среза 3, часть 4: маг и целитель, ранг мастер, подъём павшего, стихия снаряда, состав отрядов. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MageHealerGameTests {
    private MageHealerGameTests() {
    }

    @GameTest(template = "empty")
    public static void masterRankAndNewClasses(GameTestHelper helper) {
        HeroEntity mage = HeroSpawner.spawnHero(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)), HeroClass.MAGE, HeroRank.MASTER, null);
        mage.setNoAi(true);
        helper.assertTrue(mage.has(3) && HeroRank.byId("master") == HeroRank.MASTER, "Мастер без третьего приёма");
        double health = HeroConfig.MAGE_HEALTH.get() * HeroConfig.MASTER_MULTIPLIER.get();
        helper.assertTrue(Math.abs(mage.getMaxHealth() - health) < 1e-3, "Здоровье мага-мастера: " + mage.getMaxHealth());
        helper.assertTrue(mage.getItemBySlot(EquipmentSlot.CHEST).is(Items.CHAINMAIL_CHESTPLATE), "Мастер без лучшего снаряжения");
        HeroEntity warrior = HeroSpawner.spawnHero(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 1)), HeroClass.WARRIOR, HeroRank.MASTER, null);
        warrior.setNoAi(true);
        helper.assertTrue(warrior.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "Воин-мастер без алмазного нагрудника");
        RandomSource r = RandomSource.create(7);
        helper.assertTrue(HeroSpawner.rollRank(HeroClass.ARCHER, 0f, 1f, r) == HeroRank.MASTER, "Мастер при шансе 1");
        helper.assertTrue(HeroSpawner.rollRank(HeroClass.COMMANDER, 0f, 0f, r) == HeroRank.VETERAN, "Командир ниже ветерана");
        helper.assertTrue(HeroSpawner.masterChance(50, 1) == 0f && HeroSpawner.masterChance(100, 5) > 0f, "Шанс мастера по угрозе");
        Set<HeroClass> seen = EnumSet.noneOf(HeroClass.class);
        for (int i = 0; i < 30; i++) {
            seen.addAll(HeroSpawner.composition(ThreatLevel.SQUADS, r));
            seen.addAll(HeroSpawner.composition(ThreatLevel.WARBANDS, r));
        }
        helper.assertTrue(seen.contains(HeroClass.MAGE) && seen.contains(HeroClass.HEALER), "Маг и целитель не встречаются в отрядах");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void healerHealsAndMasterRevivesOnce(GameTestHelper helper) {
        AtomicInteger healed = new AtomicInteger();
        AtomicInteger revived = new AtomicInteger();
        Object listener = new Object() {
            @SubscribeEvent
            public void heal(HeroEvent.AllyHealed e) {
                if ("healer".equals(e.getHeroClass())) {
                    healed.incrementAndGet();
                }
            }

            @SubscribeEvent
            public void revive(HeroEvent.AllyRevived e) {
                revived.incrementAndGet();
            }
        };
        NeoForge.EVENT_BUS.register(listener);
        try {
            HeroSquads.Squad squad = HeroSquads.get(helper.getLevel().getServer()).create();
            HeroEntity healer = HeroSpawner.spawnHero(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)), HeroClass.HEALER, HeroRank.MASTER, squad);
            HeroEntity warrior = HeroSpawner.spawnHero(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 1)), HeroClass.WARRIOR, HeroRank.APPRENTICE, squad);
            healer.setNoAi(true);
            warrior.setNoAi(true);
            warrior.setHealth(4f);
            healer.heal(helper.getLevel(), warrior, null);
            helper.assertTrue(Math.abs(warrior.getHealth() - (4f + HeroConfig.HEAL_AMOUNT.get())) < 1e-3, "Лечение: " + warrior.getHealth());
            helper.assertTrue(healed.get() == 1, "Нет события лечения союзника");
            helper.assertTrue(healer.canRevive(), "Мастер-целитель не может поднять павшего");
            warrior.kill();
            helper.assertTrue(warrior.isAlive() && revived.get() == 1, "Павший не поднят");
            helper.assertTrue(Math.abs(warrior.getHealth() - warrior.getMaxHealth() * HeroConfig.REVIVE_HEALTH.get()) < 1e-3,
                    "Здоровье поднятого: " + warrior.getHealth());
            helper.assertTrue(!healer.canRevive(), "Подъём не раз за бой");
            // После первого удара у бойца короткая неуязвимость: второй удар в тот же тик иначе не пройдёт.
            warrior.invulnerableTime = 0;
            warrior.kill();
            helper.assertTrue(!warrior.isAlive() && revived.get() == 1, "Второй подъём за бой");
            healer.discard();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mageElementByBiome(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        helper.assertTrue(HeroSpellEntity.Element.of(biomes.getHolderOrThrow(Biomes.DESERT)) == HeroSpellEntity.Element.FIRE, "Пустыня не огонь");
        helper.assertTrue(HeroSpellEntity.Element.of(biomes.getHolderOrThrow(Biomes.SNOWY_PLAINS)) == HeroSpellEntity.Element.FROST, "Снежная равнина не лёд");
        helper.assertTrue(HeroSpellEntity.Element.of(biomes.getHolderOrThrow(Biomes.PLAINS)) == HeroSpellEntity.Element.NATURE, "Равнина не природа");
        HeroEntity mage = HeroSpawner.spawnHero(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)), HeroClass.MAGE, HeroRank.APPRENTICE, null);
        mage.setNoAi(true);
        HeroEntity target = HeroSpawner.spawnHero(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 4)), HeroClass.WARRIOR, HeroRank.APPRENTICE, null);
        target.setNoAi(true);
        HeroSpellEntity spell = mage.castSpell(target);
        helper.assertTrue(spell.isAlive() && spell.getOwner() == mage, "Снаряд стихии не выпущен");
        spell.discard();
        helper.succeed();
    }
}
