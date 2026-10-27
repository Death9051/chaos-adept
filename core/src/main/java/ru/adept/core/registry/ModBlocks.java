package ru.adept.core.registry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.PushReaction;
import ru.adept.core.base.MachineBlock;
import ru.adept.core.camp.ExtractorBlock;
import ru.adept.core.camp.TransitionBlock;
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

    /** Установка выкачки: ставится лагерем, убирается сворачиванием (руками не ломается). */
    public static final DeferredBlock<ExtractorBlock> EXTRACTOR = BLOCKS.register("extractor",
            () -> new ExtractorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(-1.0f, 3_600_000.0f)
                    .noLootTable()
                    .lightLevel(state -> 7)
                    .sound(SoundType.METAL)
                    .pushReaction(PushReaction.BLOCK)));
    /** Стационарный переход лагеря: проход на базу и вызов торговца. */
    public static final DeferredBlock<TransitionBlock> TRANSITION = BLOCKS.register("transition",
            () -> new TransitionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(-1.0f, 3_600_000.0f)
                    .noLootTable()
                    .lightLevel(state -> 10)
                    .sound(SoundType.METAL)
                    .pushReaction(PushReaction.BLOCK)));

    /** Пепел — поверхность пустоши. */
    public static final DeferredBlock<Block> ASH = BLOCKS.register("ash",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f)
                    .sound(SoundType.SAND)));

    /** Генератор базы: стоит на базе с начала, не ломается. */
    public static final DeferredBlock<MachineBlock.Generator> GENERATOR = BLOCKS.register("generator",
            () -> new MachineBlock.Generator(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(-1.0f, 3_600_000.0f).sound(SoundType.METAL).lightLevel(s -> 9)));
    public static final DeferredBlock<MachineBlock.Crusher> CRUSHER = BLOCKS.register("crusher",
            () -> new MachineBlock.Crusher(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<MachineBlock.Smelter> SMELTER = BLOCKS.register("smelter",
            () -> new MachineBlock.Smelter(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).strength(3.0f).sound(SoundType.METAL).lightLevel(s -> 6)));

    static {
        ITEMS.registerSimpleBlockItem(NODE_CRYSTAL);
        ITEMS.registerSimpleBlockItem(ASH);
        ITEMS.registerSimpleBlockItem(GENERATOR);
        ITEMS.registerSimpleBlockItem(CRUSHER);
        ITEMS.registerSimpleBlockItem(SMELTER);
        ITEMS.registerSimpleBlockItem(ARTIFACT_TABLE);
    }

    private ModBlocks() {
    }

    static void init() {
    }
}
