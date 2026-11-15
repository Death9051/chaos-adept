package ru.adept.core.trader;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Ассортимент торговца на один вызов: постоянная часть и случайные позиции из набора,
 * цена в сгустках узла I с разбросом ±{@code priceSpread}, количество покупок ограничено.
 */
public final class TraderOffers {
    public record Entry(ItemStack goods, int price) {
    }

    private TraderOffers() {
    }

    /** Разбор «товар*кол-во=цена;...». */
    public static List<Entry> parse(String spec) {
        List<Entry> out = new ArrayList<>();
        for (String part : spec.split(";")) {
            String entry = part.trim();
            int eq = entry.indexOf('=');
            if (entry.isEmpty() || eq < 0) {
                continue;
            }
            int price;
            try {
                price = Math.max(1, Integer.parseInt(entry.substring(eq + 1).trim()));
            } catch (NumberFormatException e) {
                AdeptCore.LOG.warn("Неверная цена у торговца: {}", entry);
                continue;
            }
            String goods = entry.substring(0, eq).trim();
            int count = 1;
            int star = goods.indexOf('*');
            if (star >= 0) {
                try {
                    count = Math.max(1, Integer.parseInt(goods.substring(star + 1).trim()));
                } catch (NumberFormatException e) {
                    AdeptCore.LOG.warn("Неверное количество у торговца: {}", entry);
                }
                goods = goods.substring(0, star).trim();
            }
            ResourceLocation id = ResourceLocation.tryParse(goods);
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) {
                AdeptCore.LOG.warn("Неизвестный товар у торговца: {}", goods);
                continue;
            }
            out.add(new Entry(new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize())), price));
        }
        return out;
    }

    /** Цена с разбросом: base × (1 ± spread), не меньше 1. */
    public static int spread(int base, double spread, RandomSource random) {
        double k = 1.0 + (random.nextDouble() * 2.0 - 1.0) * spread;
        return Math.max(1, (int) Math.round(base * k));
    }

    public static MerchantOffers roll(RandomSource random) {
        MerchantOffers offers = new MerchantOffers();
        double spread = BalanceConfig.TRADER_PRICE_SPREAD.get();
        for (Entry e : parse(BalanceConfig.TRADER_FIXED_OFFERS.get())) {
            offers.add(offer(e, spread, random));
        }
        List<Entry> pool = new ArrayList<>(parse(BalanceConfig.TRADER_OFFER_POOL.get()));
        int min = BalanceConfig.TRADER_OFFERS_MIN.get();
        int max = Math.max(min, BalanceConfig.TRADER_OFFERS_MAX.get());
        int n = Math.min(pool.size(), Mth.randomBetweenInclusive(random, min, max));
        for (int i = 0; i < n; i++) {
            offers.add(offer(pool.remove(random.nextInt(pool.size())), spread, random));
        }
        // Каждая позиция — и за сгустки, и за ядра душ (срез 3): вариант за ядра идёт сразу следом.
        MerchantOffers withCores = new MerchantOffers();
        for (MerchantOffer o : offers) {
            withCores.add(o);
            MerchantOffer cores = coreOffer(o);
            if (cores != null) {
                withCores.add(cores);
            }
        }
        return withCores;
    }

    private static MerchantOffer offer(Entry e, double spread, RandomSource random) {
        int price = Math.min(64, spread(e.price(), spread, random));
        int uses = Mth.randomBetweenInclusive(random, 1, BalanceConfig.TRADER_MAX_USES.get());
        return new MerchantOffer(new ItemCost(ModItems.cluster(1), price), e.goods().copy(), uses, 0, 0.0f);
    }

    /**
     * Та же позиция за ядра душ (валюта наравне со сгустками): цена в сгустках × курс, до двух стопок.
     * {@code null}, если дороже 128 ядер.
     */
    public static MerchantOffer coreOffer(MerchantOffer clusterOffer) {
        int cores = clusterOffer.getItemCostA().count() * ru.adept.core.config.ManaConfig.SOUL_CORES_PER_CLUSTER.get();
        if (cores > 128) {
            return null;
        }
        ItemCost a = new ItemCost(ModItems.SOUL_CORE.get(), Math.min(64, cores));
        java.util.Optional<ItemCost> b = cores > 64
                ? java.util.Optional.of(new ItemCost(ModItems.SOUL_CORE.get(), cores - 64)) : java.util.Optional.empty();
        return new MerchantOffer(a, b, clusterOffer.getResult().copy(), clusterOffer.getMaxUses(), 0, 0.0f);
    }

    /** Валюта сделки для события: ядра душ или сгустки. */
    public static String currency(MerchantOffer offer) {
        return offer.getItemCostA().item().value() == ModItems.SOUL_CORE.get()
                ? ru.adept.core.api.event.TraderEvent.Deal.SOUL_CORE : ru.adept.core.api.event.TraderEvent.Deal.NODE_CLUSTER;
    }
}
