package ru.adept.core.artifact;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModItems;
import ru.adept.core.table.TableActions;

/** Боевые бонусы клинка от ступеней игрока. */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class BladeCombat {
    private BladeCombat() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (event.getSource().getDirectEntity() != player || !player.getMainHandItem().is(ModItems.ENERGY_BLADE.get())) {
            return;
        }
        int edge = TableActions.progress(player).step(TableActions.BLADE_EDGE);
        if (edge > 0) {
            event.setAmount(event.getAmount() + (float) (edge * BalanceConfig.BLADE_EDGE_DAMAGE_PER_STEP.get()));
        }
    }
}
