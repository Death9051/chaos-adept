package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.threat.ThreatManager;
import ru.adept.core.village.ElderEntity;
import ru.adept.core.village.NamePools;
import ru.adept.core.village.ResidentEntity;
import ru.adept.core.village.RuinsManager;
import ru.adept.core.village.VillageManager;
import ru.adept.core.village.VillageRecord;
import ru.adept.core.village.VillageRegistry;

/** Автотесты деревень: замена жителей, старейшина, угроза за убитых, руины. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageGameTests {
    private VillageGameTests() {
    }

    private static VillageRecord village(GameTestHelper helper, long id) {
        BlockPos a = helper.absolutePos(new BlockPos(0, 0, 0));
        BlockPos b = helper.absolutePos(new BlockPos(4, 4, 4));
        return VillageRegistry.get(helper.getLevel().getServer()).registerManual(id, "Проба " + id,
                BoundingBox.fromCorners(a, b));
    }

    @GameTest(template = "empty")
    public static void residentBecomesElderAndKillsRaiseThreat(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        VillageRecord v = village(helper, 990_001L);
        int oldThreat = ThreatManager.threat(server);
        try {
            ResidentEntity first = helper.spawnWithNoFreeWill(ModEntities.RESIDENT.get(), new BlockPos(2, 2, 2));
            VillageManager.assign(helper.getLevel(), first);
            helper.assertTrue(v.elder != null && first.isRemoved(), "Первый житель не стал старейшиной");
            ResidentEntity second = helper.spawnWithNoFreeWill(ModEntities.RESIDENT.get(), new BlockPos(1, 2, 1));
            VillageManager.assign(helper.getLevel(), second);
            helper.assertTrue(!(second instanceof ElderEntity) && v.residents.contains(second.getUUID()), "Второй житель не записан");
            ThreatManager.add(server, -100, ThreatManager.Reason.COMMAND);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            second.hurt(second.damageSources().playerAttack(player), 1000.0f);
            helper.assertTrue(ThreatManager.threat(server) == BalanceConfig.THREAT_VILLAGER_KILLED.get(), "Угроза за жителя");
            helper.assertTrue(!v.residents.contains(second.getUUID()), "Убитый житель остался в деревне");
        } finally {
            ThreatManager.add(server, oldThreat - ThreatManager.threat(server), ThreatManager.Reason.COMMAND);
            VillageRegistry.get(server).remove(v.id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaVillagerIsReplaced(GameTestHelper helper) {
        if (!BalanceConfig.VILLAGE_REPLACE_VANILLA.get()) {
            helper.succeed();
            return;
        }
        helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        helper.runAfterDelay(3, () -> {
            AABB area = new AABB(helper.absolutePos(new BlockPos(-2, 0, -2)).getCenter(), helper.absolutePos(new BlockPos(6, 6, 6)).getCenter());
            long ours = helper.getLevel().getEntitiesOfClass(ResidentEntity.class, area).size();
            long vanilla = helper.getLevel().getEntities(EntityType.VILLAGER, area, e -> true).size();
            helper.assertTrue(ours >= 1 && vanilla == 0, "Житель не заменён: своих " + ours + ", ванильных " + vanilla);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ruinColumnStripsHouse(GameTestHelper helper) {
        VillageRecord v = village(helper, 990_002L);
        try {
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.GLASS);
            helper.setBlock(new BlockPos(2, 3, 2), Blocks.HAY_BLOCK);
            BlockPos abs = helper.absolutePos(new BlockPos(2, 2, 2));
            RuinsManager.ruinColumn(helper.getLevel(), abs.getX(), abs.getZ(), v.box);
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 2));
            helper.assertBlockPresent(ru.adept.core.registry.ModBlocks.ASH.get(), new BlockPos(2, 3, 2));
            RuinsManager.markRuined(helper.getLevel(), v);
            helper.assertTrue(v.ruined, "Деревня не отмечена руинами");
        } finally {
            VillageRegistry.get(helper.getLevel().getServer()).remove(v.id);
        }
        helper.assertTrue(!NamePools.villages().isEmpty(), "Пустой пул имён деревень");
        helper.succeed();
    }
}
