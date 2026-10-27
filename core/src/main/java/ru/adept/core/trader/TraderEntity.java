package ru.adept.core.trader;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.TraderEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.fx.ChaosParticles;

/**
 * Торговец Альянса: приходит по ритуалу, неуязвим, не сохраняется в мире.
 * Уходит после закрытия окна торговли или по истечении времени ожидания.
 * Модель — заглушка до работы «Моделей и анимаций».
 */
public class TraderEntity extends AbstractVillager {
    private int lifetime;
    private boolean traded;
    private boolean leaving;

    public TraderEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return AbstractVillager.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
    }

    @Override
    protected void updateTrades() {
        getOffers().addAll(TraderOffers.roll(random));
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);
        traded = true;
        if (getTradingPlayer() instanceof ServerPlayer player) {
            AdeptEvents.post(new TraderEvent.Deal(player, BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).toString(),
                    "purchase", offer.getResult().getCount()));
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && !leaving && getTradingPlayer() == null && !getOffers().isEmpty()) {
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void setTradingPlayer(Player player) {
        boolean closing = player == null && getTradingPlayer() != null;
        super.setTradingPlayer(player);
        if (closing && traded) {
            leave();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (tickCount % 10 == 0) {
            ChaosParticles.flicker(server, position().add(0, getBbHeight() * 0.5, 0), 0.4);
        }
        if (getTradingPlayer() == null && ++lifetime > BalanceConfig.TRADER_STAY_SECONDS.get() * 20) {
            leave();
        }
    }

    /** Уйти в разрыв. */
    public void leave() {
        if (leaving || !(level() instanceof ServerLevel server)) {
            return;
        }
        leaving = true;
        ChaosParticles.rift(server, position().add(0, 1.0, 0), 1.2, 1.0);
        Player nearest = server.getNearestPlayer(this, 32.0);
        if (nearest != null) {
            AdeptEvents.post(new TraderEvent.Left(nearest));
        }
        discard();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }
}
