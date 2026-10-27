package ru.adept.core.node;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/** Узел в мире: место кристалла, уровень (задаётся при генерации и не меняется), биом, состояние. */
public record NodeRecord(BlockPos pos, int level, String biome, State state) {
    public enum State { DORMANT, EXTRACTING, CLEANSED }

    public NodeRecord withState(State newState) {
        return new NodeRecord(pos, level, biome, newState);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("pos", NbtUtils.writeBlockPos(pos));
        tag.putInt("level", level);
        tag.putString("biome", biome);
        tag.putString("state", state.name());
        return tag;
    }

    public static NodeRecord load(CompoundTag tag) {
        BlockPos pos = NbtUtils.readBlockPos(tag, "pos").orElse(BlockPos.ZERO);
        State state;
        try {
            state = State.valueOf(tag.getString("state"));
        } catch (IllegalArgumentException e) {
            state = State.DORMANT;
        }
        return new NodeRecord(pos, Math.max(1, tag.getInt("level")), tag.getString("biome"), state);
    }
}
