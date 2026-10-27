package ru.adept.core.registry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.PushReaction;
import ru.adept.core.table.ArtifactTableBlock;

import static ru.adept.core.registry.ModRegistries.BLOCKS;
import static ru.adept.core.registry.ModRegistries.ITEMS;

public final class ModBlocks {
    public static final DeferredBlock<ArtifactTableBlock> ARTIFACT_TABLE = BLOCKS.register("artifact_table",
            () -> new ArtifactTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f)
                    .sound(SoundType.COPPER)));

    /** Кристалл узла: не ломается и не двигается; исчезает при поглощении узла. */
    public static final DeferredBlock<Block> NODE_CRYSTAL = BLOCKS.register("node_crystal",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3_600_000.0f)
                    .noLootTable()
                    .lightLevel(state -> 12)
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK)));

    static {
        ITEMS.registerSimpleBlockItem(NODE_CRYSTAL);
        ITEMS.registerSimpleBlockItem(ARTIFACT_TABLE);
    }

    private ModBlocks() {
    }

    static void init() {
    }
}
