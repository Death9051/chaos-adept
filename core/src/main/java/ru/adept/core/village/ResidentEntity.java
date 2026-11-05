package ru.adept.core.village;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.VillageEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.threat.ThreatManager;

/**
 * Житель деревни (свой вместо ванильного; поведение — ванильного жителя).
 * Реагирует на угрозу: при высокой отказывается торговать и, завидев игрока, прячется по домам.
 * Шлёт событие «NPC заметил игрока» не чаще раза в 30 секунд.
 * Размер заглушки = размер будущей модели: 0,6 × 1,95.
 */
public class ResidentEntity extends Villager {
    private long villageId;
    private boolean assigned;
    private long lastNotice = Long.MIN_VALUE;

    public ResidentEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
    }

    /** Роль для событий: {@code villager} или {@code elder}. */
    public String role() {
        return "villager";
    }

    public long villageId() {
        return villageId;
    }

    public void setVillageId(long id) {
        this.villageId = id;
        this.assigned = true;
    }

    @Override
    public Villager getBreedOffspring(ServerLevel level, AgeableMob partner) {
        ResidentEntity child = ModEntities.RESIDENT.get().create(level);
        if (child != null) {
            child.setVillagerData(child.getVillagerData().setType(VillagerType.byBiome(level.getBiome(blockPosition()))));
            child.setVillageId(villageId);
        }
        return child;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (!assigned) {
            assigned = true;
            VillageManager.assign(server, this);
        }
        if (tickCount % 20 != 0) {
            return;
        }
        int range = BalanceConfig.VILLAGE_NOTICE_RANGE.get();
        Player player = server.getNearestPlayer(this, range);
        if (player == null || player.isSpectator() || !hasLineOfSight(player)) {
            return;
        }
        int threat = ThreatManager.threat(server.getServer());
        long now = server.getGameTime();
        if (now - lastNotice >= BalanceConfig.VILLAGE_NOTICE_COOLDOWN_TICKS.get()) {
            lastNotice = now;
            String name = VillageRegistry.get(server.getServer()).byId(villageId).map(v -> v.name).orElse("");
            AdeptEvents.post(new VillageEvent.NpcNoticedPlayer(this, player, role(), villageId, name, threat));
        }
        if (threat >= BalanceConfig.VILLAGE_HIDE_AT.get()) {
            // Ванильный механизм тревоги: жители слышат «колокол» и прячутся по домам.
            getBrain().setMemory(MemoryModuleType.HEARD_BELL_TIME, now);
        }
    }

    /** Торговать готов, пока угроза ниже порога. */
    public boolean willTrade() {
        return level().getServer() == null || ThreatManager.threat(level().getServer()) < BalanceConfig.VILLAGE_TRADE_REFUSE_AT.get();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && !willTrade()) {
            setUnhappyCounter(40);
            playSound(SoundEvents.VILLAGER_NO, 1.0f, getVoicePitch());
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("adept_village", villageId);
        tag.putBoolean("adept_assigned", assigned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        villageId = tag.getLong("adept_village");
        assigned = tag.getBoolean("adept_assigned");
    }
}
