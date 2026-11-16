package ru.adept.core.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.GaussLook;
import ru.adept.core.artifact.HeavyArmorItem;

/**
 * Под закрытым шлемом тяжёлой брони голова игрока не рисуется. Закрытый шлем (ступени основы I–III) скруглён
 * сильнее, чем углы кубической головы, и они торчали бы сквозь него. У прототипа шлем открытый — голова видна.
 * Скрываются голова и слой головного убора модели игрока на время отрисовки этого игрока.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID, value = Dist.CLIENT)
public final class HeavyHelmetHead {
    private HeavyHelmetHead() {
    }

    /** Надет ли на игроке закрытый шлем тяжёлой брони. */
    public static boolean closedHelmet(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof HeavyArmorItem)) {
            return false;
        }
        int look = helmet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe().getInt(GaussLook.TAG);
        return GaussLook.base(look, GaussLook.Segment.HEAVY_HELMET) >= 1;
    }

    @SubscribeEvent
    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        if (closedHelmet(event.getEntity())) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.head.visible = false;
            model.hat.visible = false;
        }
    }

    @SubscribeEvent
    public static void afterPlayer(RenderPlayerEvent.Post event) {
        if (closedHelmet(event.getEntity())) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.head.visible = true;
            model.hat.visible = true;
        }
    }
}
