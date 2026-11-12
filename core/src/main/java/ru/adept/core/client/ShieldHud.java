package ru.adept.core.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.LightArmorItem;

/**
 * Шкала щита из маны на экране игрока: вертикальная полоса слева от гнезда второй руки и число единиц над ней
 * (смысл не только цветом). Под полосой — тонкая синяя шкала маны костюма. Рисуется, только пока надет нагрудник
 * лёгкой брони и интерфейс не скрыт.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ShieldHud {
    private static final int BACK = 0xC0101018;
    private static final int SHIELD = 0xFF6FD8FF;
    private static final int SHIELD_EMPTY = 0xFF8A3A3A;
    private static final int MANA = 0xFF3D6BFF;
    private static final int TEXT = 0xFFE8E0D0;

    private ShieldHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "mana_shield"), ShieldHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof LightArmorItem)) {
            return;
        }
        int cap = Math.max(1, LightArmorItem.shownCapacity(chest));
        double shield = LightArmorItem.shield(chest);
        int manaCap = Math.max(1, ru.adept.core.config.FocusConfig.LIGHT_MANA_CAPACITY.get());
        int mana = LightArmorItem.mana(chest);
        int x = g.guiWidth() / 2 - 91 - 29 - 10;
        int h = 20;
        int y = g.guiHeight() - 21;
        g.fill(x - 1, y - 1, x + 6, y + h + 4, BACK);
        int filled = (int) Math.round(h * Math.min(1.0, shield / cap));
        g.fill(x, y + h - filled, x + 5, y + h, shield > 0 ? SHIELD : SHIELD_EMPTY);
        g.fill(x, y + h + 1, x + (int) Math.round(5.0 * Math.min(1.0, (double) mana / manaCap)), y + h + 3, MANA);
        String units = Integer.toString((int) Math.floor(shield));
        g.drawString(mc.font, units, x + 3 - mc.font.width(units) / 2, y - 10, shield > 0 ? TEXT : SHIELD_EMPTY, true);
    }
}
