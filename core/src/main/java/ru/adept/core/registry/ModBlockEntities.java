package ru.adept.core.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.adept.core.base.CrusherBlockEntity;
import ru.adept.core.base.GeneratorBlockEntity;
import ru.adept.core.base.SmelterBlockEntity;

import static ru.adept.core.registry.ModRegistries.BLOCK_ENTITIES;

public final class ModBlockEntities {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR =
            BLOCK_ENTITIES.register("generator", () -> BlockEntityType.Builder.of(GeneratorBlockEntity::new, ModBlocks.GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register("crusher", () -> BlockEntityType.Builder.of(CrusherBlockEntity::new, ModBlocks.CRUSHER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ru.adept.core.base.ReservoirBlockEntity>> RESERVOIR =
            BLOCK_ENTITIES.register("reservoir", () -> BlockEntityType.Builder.of(ru.adept.core.base.ReservoirBlockEntity::new, ModBlocks.RESERVOIR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ru.adept.core.base.AlloyerBlockEntity>> ALLOYER =
            BLOCK_ENTITIES.register("alloyer", () -> BlockEntityType.Builder.of(ru.adept.core.base.AlloyerBlockEntity::new, ModBlocks.ALLOYER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ru.adept.core.base.CasterBlockEntity>> CASTER =
            BLOCK_ENTITIES.register("caster", () -> BlockEntityType.Builder.of(ru.adept.core.base.CasterBlockEntity::new, ModBlocks.CASTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmelterBlockEntity>> SMELTER =
            BLOCK_ENTITIES.register("smelter", () -> BlockEntityType.Builder.of(SmelterBlockEntity::new, ModBlocks.SMELTER.get()).build(null));

    private ModBlockEntities() {
    }

    static void init() {
    }
}
