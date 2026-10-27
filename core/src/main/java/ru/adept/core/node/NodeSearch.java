package ru.adept.core.node;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/** Поиск узлов: и поставленных (реестр), и ещё не поставленных кандидатов сетки. */
public final class NodeSearch {
    public record Found(BlockPos column, int level, boolean placed) {
    }

    private NodeSearch() {
    }

    /** Ближайший незачищенный узел в радиусе {@code range} по горизонтали. */
    public static Optional<Found> nearest(ServerLevel level, BlockPos from, int range) {
        NodeRegistry registry = NodeRegistry.get(level.getServer());
        Found best = null;
        double bestDist = (double) range * range;
        for (NodeRecord node : registry.all()) {
            if (node.state() == NodeRecord.State.CLEANSED) {
                continue;
            }
            double d = horizontalDistSqr(node.pos(), from);
            if (d <= bestDist) {
                bestDist = d;
                best = new Found(node.pos(), node.level(), true);
            }
        }
        int cells = range / NodeGrid.cellSize() + 1;
        int cx = NodeGrid.cellOf(from.getX());
        int cz = NodeGrid.cellOf(from.getZ());
        for (int x = cx - cells; x <= cx + cells; x++) {
            for (int z = cz - cells; z <= cz + cells; z++) {
                if (registry.isCellPlaced(NodeGrid.cellKey(x, z))) {
                    continue;
                }
                Optional<NodeGrid.Candidate> c = NodeGrid.candidate(level, x, z);
                if (c.isPresent()) {
                    double d = horizontalDistSqr(c.get().column(), from);
                    if (d <= bestDist) {
                        bestDist = d;
                        best = new Found(c.get().column(), c.get().level(), false);
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static double horizontalDistSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
