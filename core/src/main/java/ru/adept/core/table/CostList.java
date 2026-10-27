package ru.adept.core.table;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.adept.core.AdeptCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Список предметов-цены из строки конфига «id*кол-во;id*кол-во».
 * Неизвестные id пропускаются с предупреждением в логе.
 */
public record CostList(List<ItemStack> items) {
    public static CostList parse(String spec) {
        List<ItemStack> out = new ArrayList<>();
        for (String part : spec.split(";")) {
            String entry = part.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int count = 1;
            int star = entry.indexOf('*');
            if (star >= 0) {
                try {
                    count = Math.max(1, Integer.parseInt(entry.substring(star + 1).trim()));
                } catch (NumberFormatException e) {
                    AdeptCore.LOG.warn("Неверное количество в рецепте стола: {}", entry);
                }
                entry = entry.substring(0, star).trim();
            }
            ResourceLocation id = ResourceLocation.tryParse(entry);
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) {
                AdeptCore.LOG.warn("Неизвестный предмет в рецепте стола: {}", entry);
                continue;
            }
            out.add(new ItemStack(item, count));
        }
        return new CostList(List.copyOf(out));
    }

    /** Хватает ли предметов в инвентаре игрока (творческий режим — всегда хватает). */
    public boolean canPay(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        for (ItemStack need : items) {
            if (count(player.getInventory(), need.getItem()) < need.getCount()) {
                return false;
            }
        }
        return true;
    }

    /** Списать цену. Вызывать только после {@link #canPay}. */
    public void pay(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        Inventory inv = player.getInventory();
        for (ItemStack need : items) {
            int left = need.getCount();
            for (int slot = 0; slot < inv.getContainerSize() && left > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.is(need.getItem())) {
                    int take = Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                }
            }
        }
        inv.setChanged();
    }

    private static int count(Inventory inv, Item item) {
        int total = 0;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
