package ru.adept.core.api.event;

import net.minecraft.world.entity.player.Player;

/** События артефактов: выбор пары, сборка прототипа, открытие ступени. */
public abstract class ArtifactEvent extends AdeptEvent {
    private final Player player;

    protected ArtifactEvent(Player player) {
        super(player.getServer());
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    /** Игрок выбрал вариант пары (навсегда). Категория, например {@code melee}; вариант — {@code blade}. */
    public static final class PairChosen extends ArtifactEvent {
        private final String category;
        private final String variant;

        public PairChosen(Player player, String category, String variant) {
            super(player);
            this.category = category;
            this.variant = variant;
        }

        public String getCategory() {
            return category;
        }

        public String getVariant() {
            return variant;
        }
    }

    /** Собран прототип артефакта. {@code first} — впервые за игру. */
    public static final class PrototypeCrafted extends ArtifactEvent {
        private final String artifact;
        private final boolean first;

        public PrototypeCrafted(Player player, String artifact, boolean first) {
            super(player);
            this.artifact = artifact;
            this.first = first;
        }

        public String getArtifact() {
            return artifact;
        }

        public boolean isFirst() {
            return first;
        }
    }

    /** Открыта ступень: ключ {@code артефакт/сегмент/путь}, номер ступени; вершина — путь {@code apex}. */
    public static final class StepUnlocked extends ArtifactEvent {
        private final String key;
        private final int step;

        public StepUnlocked(Player player, String key, int step) {
            super(player);
            this.key = key;
            this.step = step;
        }

        public String getKey() {
            return key;
        }

        public int getStep() {
            return step;
        }
    }
}
