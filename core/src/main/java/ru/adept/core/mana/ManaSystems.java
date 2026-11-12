package ru.adept.core.mana;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.base.BaseData;
import ru.adept.core.base.BaseTravel;
import ru.adept.core.config.ManaConfig;

/**
 * Работа накопителя и котла душ раз в секунду у каждого игрока, и души с убитых мобов.
 * Порядок за секунду: накопитель берёт с базы (на базе) → котёл жжёт души и отдаёт ману → накопитель раздаёт артефактам.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class ManaSystems {
    private ManaSystems() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        tick(player, 20);
    }

    /** Секунда работы (вынесено для тестов). */
    public static void tick(Player player, int ticks) {
        ItemStack accumulator = AccumulatorItem.find(player);
        if (!accumulator.isEmpty()) {
            AccumulatorItem.syncCapacity(accumulator, AccumulatorItem.capacity(player));
            if (player.getServer() != null && player.level().dimension() == BaseTravel.BASE) {
                intakeFromBase(player, accumulator, BaseData.get(player.getServer()), ticks);
            }
        }
        ItemStack cauldron = SoulCauldronItem.find(player);
        if (!cauldron.isEmpty()) {
            SoulCauldronItem.process(player, cauldron, accumulator, ticks);
        }
        if (!accumulator.isEmpty()) {
            distribute(player, accumulator, ticks);
        }
    }

    /** На базе накопитель заполняется из запасов базы, по каждому ресурсу отдельно. */
    public static void intakeFromBase(Player player, ItemStack accumulator, BaseData base, int ticks) {
        int rate = ManaConfig.ACCUMULATOR_INTAKE.get() * ticks / 20;
        int cap = AccumulatorItem.capacity(player);
        int mana = Math.min(rate, Math.min(base.mana(), cap - AccumulatorItem.stored(accumulator, ArtifactPower.Resource.MANA)));
        if (mana > 0 && base.useMana(mana)) {
            AccumulatorItem.receive(accumulator, ArtifactPower.Resource.MANA, mana, cap);
        }
        int el = Math.min(rate, Math.min(base.electricity(), cap - AccumulatorItem.stored(accumulator, ArtifactPower.Resource.ELECTRICITY)));
        if (el > 0 && base.useElectricity(el)) {
            AccumulatorItem.receive(accumulator, ArtifactPower.Resource.ELECTRICITY, el, cap);
        }
    }

    /** Раздача артефактам: каждому до {@code outputPerSecond} своего ресурса, без потерь. */
    public static void distribute(Player player, ItemStack accumulator, int ticks) {
        int rate = ManaConfig.ACCUMULATOR_OUTPUT.get() * ticks / 20;
        for (ArtifactPower.Consumer c : ArtifactPower.consumers(player)) {
            int have = AccumulatorItem.stored(accumulator, c.resource());
            int give = Math.min(rate, Math.min(have, c.missing()));
            if (give > 0) {
                c.setCharge().accept(c.charge().getAsInt() + give);
                AccumulatorItem.setStored(accumulator, c.resource(), have - give);
            }
        }
    }

    /** Моб, убитый игроком с котлом, оставляет душу. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide
                || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (SoulCauldronItem.find(player).isEmpty()) {
            return;
        }
        SoulStrength strength = SoulStrength.of(mob);
        mob.level().addFreshEntity(SoulEntity.create(mob.level(), mob.position().add(0, mob.getBbHeight() * 0.5, 0),
                strength, 1, player.getUUID()));
    }
}
