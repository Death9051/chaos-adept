package ru.adept.core.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import ru.adept.core.camp.ExtractionManager;
import ru.adept.core.config.BalanceConfig;

import java.util.EnumSet;

/** Тлеющий дух (диверсант): бежит к установке выкачки и взрывается у неё, повреждая установку. */
public class EmberSpiritEntity extends Monster {
    private BlockPos targetPos;

    public EmberSpiritEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public void setTargetPos(BlockPos pos) {
        this.targetPos = pos;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new RunToExtractorGoal());
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetPos != null) {
            tag.put("adept_target", NbtUtils.writeBlockPos(targetPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        targetPos = NbtUtils.readBlockPos(tag, "adept_target").orElse(null);
    }

    private final class RunToExtractorGoal extends Goal {
        RunToExtractorGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return targetPos != null;
        }

        @Override
        public void tick() {
            if (targetPos == null) {
                return;
            }
            if (distanceToSqr(targetPos.getCenter()) < 4.0) {
                level().explode(EmberSpiritEntity.this, getX(), getY(), getZ(), 1.0f, Level.ExplosionInteraction.NONE);
                if (level() instanceof ServerLevel server) {
                    ExtractionManager.damageExtractor(server.getServer(), targetPos, BalanceConfig.SPIRIT_EXTRACTOR_DAMAGE.get());
                }
                discard();
                return;
            }
            if (getNavigation().isDone()) {
                getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.2);
            }
        }
    }
}
