package ru.adept.core.api.event;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Боевые события героев для реплик сюжета. В каждом — герой, его класс ({@code tracker}, {@code warrior},
 * {@code archer}, {@code commander}; позже {@code mage}, {@code healer}), ранг ({@code apprentice}, {@code veteran})
 * и отряд. Ядро шлёт все события, частоту реплик регулирует сюжет.
 */
public abstract class HeroEvent extends AdeptEvent {
    private final Mob hero;
    private final String heroClass;
    private final String rank;
    private final UUID squad;
    private final Player player;

    protected HeroEvent(Mob hero, String heroClass, String rank, UUID squad, Player player) {
        super(hero.getServer());
        this.hero = hero;
        this.heroClass = heroClass;
        this.rank = rank;
        this.squad = squad;
        this.player = player;
    }

    public Mob getHero() {
        return hero;
    }

    public String getHeroClass() {
        return heroClass;
    }

    public String getRank() {
        return rank;
    }

    /** Отряд; {@code null} — одиночный герой (отладочная команда). */
    public UUID getSquad() {
        return squad;
    }

    /** Игрок, против которого бой (может быть {@code null} для «союзник пал»/«отступает» без цели). */
    public Player getPlayer() {
        return player;
    }

    /** Герой заметил игрока. */
    public static final class Spotted extends HeroEvent {
        public Spotted(Mob hero, String c, String r, UUID s, Player p) {
            super(hero, c, r, s, p);
        }
    }

    /** Герой впервые за бой опустился ниже половины здоровья. */
    public static final class Wounded extends HeroEvent {
        public Wounded(Mob hero, String c, String r, UUID s, Player p) {
            super(hero, c, r, s, p);
        }
    }

    /** Снаряд или заклинание героя прошли мимо игрока. */
    public static final class Missed extends HeroEvent {
        public Missed(Mob hero, String c, String r, UUID s, Player p) {
            super(hero, c, r, s, p);
        }
    }

    /** Игрок потерян из виду дольше 5 секунд. */
    public static final class PlayerLost extends HeroEvent {
        public PlayerLost(Mob hero, String c, String r, UUID s, Player p) {
            super(hero, c, r, s, p);
        }
    }

    /** Герой вылечил союзника (целители — в следующих срезах). */
    public static final class AllyHealed extends HeroEvent {
        private final Mob ally;

        public AllyHealed(Mob hero, String c, String r, UUID s, Player p, Mob ally) {
            super(hero, c, r, s, p);
            this.ally = ally;
        }

        public Mob getAlly() {
            return ally;
        }
    }

    /** Союзник пал: {@code hero} — выживший боец отряда, {@code fallen} — павший. */
    public static final class AllyFell extends HeroEvent {
        private final Mob fallen;

        public AllyFell(Mob hero, String c, String r, UUID s, Player p, Mob fallen) {
            super(hero, c, r, s, p);
            this.fallen = fallen;
        }

        public Mob getFallen() {
            return fallen;
        }
    }

    /** Отряд потерял половину бойцов и отступает; {@code hero} — командир или старший из оставшихся. */
    public static final class SquadRetreating extends HeroEvent {
        public SquadRetreating(Mob hero, String c, String r, UUID s, Player p) {
            super(hero, c, r, s, p);
        }
    }
}
