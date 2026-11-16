package ru.adept.core.seed;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Предмет стола артефактов. Стол один на игру (зачаток уходит в рецепт): сломанный выпадает собой и переносится,
 * поэтому, как и зачаток, не горит и не исчезает на земле.
 */
public class TableItem extends BlockItem {
    public TableItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1).fireResistant());
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }
}
