package ru.adept.core.api.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * События деревень. {@code villageId} — постоянный номер деревни (чанк начала структуры), 0 — вне деревни;
 * {@code villageName} — имя, которое ядро сгенерировало и хранит в данных мира.
 */
public abstract class VillageEvent extends AdeptEvent {
    private final long villageId;
    private final String villageName;

    protected VillageEvent(MinecraftServer server, long villageId, String villageName) {
        super(server);
        this.villageId = villageId;
        this.villageName = villageName;
    }

    public long getVillageId() {
        return villageId;
    }

    public String getVillageName() {
        return villageName;
    }

    /**
     * NPC заметил игрока. {@code role}: {@code villager}, {@code elder} (позже {@code militia}, {@code refugee});
     * {@code threat} — угроза мира в этот момент. От одного NPC не чаще раза в 30 секунд.
     */
    public static final class NpcNoticedPlayer extends VillageEvent {
        private final LivingEntity npc;
        private final Player player;
        private final String role;
        private final int threat;

        public NpcNoticedPlayer(LivingEntity npc, Player player, String role, long villageId, String villageName, int threat) {
            super(player.getServer(), villageId, villageName);
            this.npc = npc;
            this.player = player;
            this.role = role;
            this.threat = threat;
        }

        public LivingEntity getNpc() {
            return npc;
        }

        public Player getPlayer() {
            return player;
        }

        public String getRole() {
            return role;
        }

        public int getThreat() {
            return threat;
        }
    }

    /** Игрок вошёл в руины деревни (при каждом входе; первый раз отмечает сюжет). */
    public static final class RuinsEntered extends VillageEvent {
        private final Player player;

        public RuinsEntered(Player player, long villageId, String villageName) {
            super(player.getServer(), villageId, villageName);
            this.player = player;
        }

        public Player getPlayer() {
            return player;
        }
    }

    /** Деревня стала руинами (поглощение биома). */
    public static final class Ruined extends VillageEvent {
        public Ruined(MinecraftServer server, long villageId, String villageName) {
            super(server, villageId, villageName);
        }
    }

    /** Игрок убил последнего жителя деревни. */
    public static final class Destroyed extends VillageEvent {
        public Destroyed(MinecraftServer server, long villageId, String villageName) {
            super(server, villageId, villageName);
        }
    }

    /** Игрок убил старейшину: набег в этой местности откладывается, угроза растёт. */
    public static final class ElderKilled extends VillageEvent {
        public ElderKilled(MinecraftServer server, long villageId, String villageName) {
            super(server, villageId, villageName);
        }
    }
}
