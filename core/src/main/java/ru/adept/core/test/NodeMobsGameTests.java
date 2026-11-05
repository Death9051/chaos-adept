package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.mob.ArmadilloEntity;
import ru.adept.core.mob.SporelingEntity;
import ru.adept.core.registry.ModEntities;

/** Автотесты новых противников на узле: размеры заглушек, броненосец, деление споровика. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NodeMobsGameTests {
    private NodeMobsGameTests() {
    }

    private static boolean size(EntityType<?> type, float w, float h) {
        return Math.abs(type.getWidth() - w) < 1e-3 && Math.abs(type.getHeight() - h) < 1e-3;
    }

    @GameTest(template = "empty")
    public static void stubSizesMatchModels(GameTestHelper helper) {
        helper.assertTrue(size(ModEntities.BOAR_LEADER.get(), 1.4f, 1.2f), "Кабан-вожак не 1,4 × 1,2");
        helper.assertTrue(size(ModEntities.ARMADILLO.get(), 1.0f, 0.8f), "Броненосец не 1,0 × 0,8");
        helper.assertTrue(size(ModEntities.SPORELING.get(), 0.8f, 1.1f), "Споровик не 0,8 × 1,1");
        helper.assertTrue(size(ModEntities.SPORELING_SMALL.get(), 0.4f, 0.55f), "Малый споровик не 0,4 × 0,55");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armadilloBlocksFrontAndCurls(GameTestHelper helper) {
        ArmadilloEntity armadillo = helper.spawnWithNoFreeWill(ModEntities.ARMADILLO.get(), new BlockPos(2, 2, 2));
        armadillo.setYBodyRot(0.0f);
        armadillo.setYRot(0.0f);
        // Корова прямо перед мордой (юг, +Z) и за спиной (север, −Z).
        Cow front = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 5));
        Cow back = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 0));
        float health = armadillo.getHealth();
        DamageSource fromFront = armadillo.damageSources().mobAttack(front);
        helper.assertTrue(armadillo.isFromFront(fromFront), "Удар спереди не распознан");
        armadillo.hurt(fromFront, 5.0f);
        helper.assertTrue(armadillo.getHealth() == health, "Удар спереди прошёл");
        armadillo.invulnerableTime = 0;
        armadillo.hurt(armadillo.damageSources().mobAttack(back), 4.0f);
        helper.assertTrue(armadillo.getHealth() < health, "Удар в спину не прошёл");
        helper.assertTrue(armadillo.isCurled(), "Броненосец не свернулся");
        helper.assertTrue(Math.abs(armadillo.getBbHeight() - 0.9f) < 1e-3, "Свёрнутый хитбокс не 0,9");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sporelingSplits(GameTestHelper helper) {
        SporelingEntity sporeling = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), new BlockPos(2, 2, 2));
        sporeling.kill();
        helper.runAfterDelay(2, () -> {
            int small = helper.getLevel().getEntitiesOfClass(SporelingEntity.class, sporeling.getBoundingBox().inflate(4),
                    e -> e.getType() == ModEntities.SPORELING_SMALL.get()).size();
            helper.assertTrue(small == ru.adept.core.config.BalanceConfig.SPORELING_SPLIT_COUNT.get(), "Маленьких споровиков: " + small);
            helper.succeed();
        });
    }
}
