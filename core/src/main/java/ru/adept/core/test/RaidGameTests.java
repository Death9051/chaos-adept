package ru.adept.core.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.raid.RaidManager;
import ru.adept.core.threat.ThreatLevel;

/** Автотесты набегов: шансы по ступеням угрозы и пределы частоты из конфига. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RaidGameTests {
    private RaidGameTests() {
    }

    @GameTest(template = "empty")
    public static void raidChancesFollowThreat(GameTestHelper helper) {
        helper.assertTrue(RaidManager.chance(ThreatLevel.CALM) == 0.0, "Набег при спокойной угрозе");
        helper.assertTrue(RaidManager.chance(ThreatLevel.SCOUTS) < RaidManager.chance(ThreatLevel.SQUADS)
                && RaidManager.chance(ThreatLevel.SQUADS) < RaidManager.chance(ThreatLevel.WARBANDS), "Шанс не растёт с угрозой");
        helper.assertTrue(BalanceConfig.RAID_COOLDOWN_MIN_TICKS.get() == 15 * 60 * 20
                && BalanceConfig.RAID_COOLDOWN_MAX_TICKS.get() == 20 * 60 * 20, "Набеги не раз в 15–20 минут");
        helper.assertTrue(BalanceConfig.RAID_CHECK_TICKS.get() == 10 * 60 * 20, "Проверка не раз в 10 минут");
        helper.assertTrue(BalanceConfig.RAID_HORN_LEAD_TICKS.get() == 60 * 20 && BalanceConfig.RAID_WAIT_TICKS.get() == 5 * 60 * 20,
                "Рог не за минуту или ожидание не 5 минут");
        helper.succeed();
    }
}
