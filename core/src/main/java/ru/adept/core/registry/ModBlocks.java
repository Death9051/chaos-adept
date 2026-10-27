package ru.adept.core.registry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import ru.adept.core.table.ArtifactTableBlock;

import static ru.adept.core.registry.ModRegistries.BLOCKS;
import static ru.adept.core.registry.ModRegistries.ITEMS;

public final class ModBlocks {
    public static final DeferredBlock<ArtifactTableBlock> ARTIFACT_TABLE = BLOCKS.register("artifact_table",
            () -> new ArtifactTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f)
                    .sound(SoundType.COPPER)));

    static {
        ITEMS.registerSimpleBlockItem(ARTIFACT_TABLE);
    }

    private ModBlocks() {
    }

    static void init() {
    }
}
