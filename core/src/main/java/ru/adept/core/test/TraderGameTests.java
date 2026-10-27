package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.trader.TraderEntity;
import ru.adept.core.trader.TraderOffers;

/** Автотесты торговца: разбор ассортимента, разброс цен, набор позиций, неуязвимость. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TraderGameTests {
    private TraderGameTests() {
    }

    @GameTest(template = "empty")
    public static void offersParseAndRoll(GameTestHelper helper) {
        helper.assertTrue(TraderOffers.parse("minecraft:diamond*2=3;bad;minecraft:nope=1").size() == 1, "Разбор ассортимента неверен");
        RandomSource random = RandomSource.create(42);
        int fixed = TraderOffers.parse(BalanceConfig.TRADER_FIXED_OFFERS.get()).size();
        for (int i = 0; i < 20; i++) {
            MerchantOffers offers = TraderOffers.roll(random);
            int extra = offers.size() - fixed;
            helper.assertTrue(extra >= BalanceConfig.TRADER_OFFERS_MIN.get() && extra <= BalanceConfig.TRADER_OFFERS_MAX.get(),
                    "Число случайных позиций вне диапазона: " + extra);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void priceSpreadBounds(GameTestHelper helper) {
        RandomSource random = RandomSource.create(7);
        for (int i = 0; i < 200; i++) {
            int p = TraderOffers.spread(20, 0.15, random);
            helper.assertTrue(p >= 17 && p <= 23, "Цена вышла за ±15%: " + p);
        }
        helper.assertTrue(TraderOffers.spread(1, 0.15, random) == 1, "Минимальная цена не 1");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void traderIsInvulnerable(GameTestHelper helper) {
        TraderEntity trader = helper.spawn(ModEntities.TRADER.get(), new BlockPos(0, 2, 0));
        DamageSource hit = helper.getLevel().damageSources().generic();
        helper.assertTrue(trader.isInvulnerableTo(hit), "Торговец уязвим");
        helper.assertTrue(!trader.shouldBeSaved(), "Торговец сохраняется в мире");
        trader.discard();
        helper.succeed();
    }
}
