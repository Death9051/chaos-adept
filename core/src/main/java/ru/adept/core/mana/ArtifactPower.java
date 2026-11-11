package ru.adept.core.mana;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.artifact.GaussGunItem;
import ru.adept.core.artifact.HeavyArmorItem;
import ru.adept.core.config.BalanceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Артефакты игрока, которые питаются от накопителя: какой ресурс берут, сколько в них сейчас и сколько влезает.
 * Новый артефакт на мане или электричестве добавляет сюда свою строку.
 */
public final class ArtifactPower {
    public enum Resource {
        MANA("mana"),
        ELECTRICITY("electricity");

        private final String id;

        Resource(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** Потребитель: запас артефакта и его предел. */
    public record Consumer(ItemStack stack, Resource resource, IntSupplier charge, IntConsumer setCharge, int capacity) {
        public int missing() {
            return Math.max(0, capacity - charge.getAsInt());
        }
    }

    private ArtifactPower() {
    }

    public static List<Consumer> consumers(Player player) {
        List<Consumer> out = new ArrayList<>();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof HeavyArmorItem) {
            out.add(new Consumer(chest, Resource.ELECTRICITY, () -> HeavyArmorItem.charge(chest),
                    v -> HeavyArmorItem.setCharge(chest, v), BalanceConfig.HEAVY_CHARGE_CAPACITY.get()));
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GaussGunItem) {
                out.add(new Consumer(stack, Resource.ELECTRICITY, () -> GaussGunItem.charge(stack),
                        v -> GaussGunItem.setCharge(stack, v), GaussGunItem.capacity()));
            }
        }
        return out;
    }
}
