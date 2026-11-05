package ru.adept.core.hero;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.HeroEvent;
import ru.adept.core.threat.ThreatManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Отряды героев: состав, деревня-отправитель, имя командира. Потеряв половину бойцов, отряд отступает.
 * Отряд, который отступил или погиб целиком, считается разгромленным: угроза заметно падает.
 */
public final class HeroSquads extends SavedData {
    private static final String DATA_NAME = AdeptCore.MOD_ID + "_squads";
    private static final SavedData.Factory<HeroSquads> FACTORY = new SavedData.Factory<>(HeroSquads::new, HeroSquads::load, null);

    /** Слушатели конца отряда (набеги, PR набегов): отряд, разгромлен ли он. */
    private static final List<java.util.function.BiConsumer<Squad, Boolean>> END_LISTENERS = new ArrayList<>();

    public static final class Squad {
        public final UUID id;
        public final Set<UUID> members = new HashSet<>();
        public int initial;
        public long villageId;
        public String villageName = "";
        public String commanderName = "";
        public UUID commander;
        public UUID target;
        public boolean retreating;
        public int fallen;

        Squad(UUID id) {
            this.id = id;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id);
            ListTag list = new ListTag();
            members.forEach(u -> list.add(NbtUtils.createUUID(u)));
            t.put("members", list);
            t.putInt("initial", initial);
            t.putLong("village", villageId);
            t.putString("village_name", villageName);
            t.putString("commander_name", commanderName);
            if (commander != null) {
                t.putUUID("commander", commander);
            }
            if (target != null) {
                t.putUUID("target", target);
            }
            t.putBoolean("retreating", retreating);
            t.putInt("fallen", fallen);
            return t;
        }

        static Squad load(CompoundTag t) {
            Squad s = new Squad(t.getUUID("id"));
            for (Tag m : t.getList("members", Tag.TAG_INT_ARRAY)) {
                s.members.add(NbtUtils.loadUUID(m));
            }
            s.initial = t.getInt("initial");
            s.villageId = t.getLong("village");
            s.villageName = t.getString("village_name");
            s.commanderName = t.getString("commander_name");
            s.commander = t.hasUUID("commander") ? t.getUUID("commander") : null;
            s.target = t.hasUUID("target") ? t.getUUID("target") : null;
            s.retreating = t.getBoolean("retreating");
            s.fallen = t.getInt("fallen");
            return s;
        }
    }

    private final Map<UUID, Squad> squads = new HashMap<>();

    private HeroSquads() {
    }

    public static HeroSquads get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static void onSquadEnd(java.util.function.BiConsumer<Squad, Boolean> listener) {
        END_LISTENERS.add(listener);
    }

    public Squad create() {
        Squad s = new Squad(UUID.randomUUID());
        squads.put(s.id, s);
        setDirty();
        return s;
    }

    public Optional<Squad> byId(UUID id) {
        return id == null ? Optional.empty() : Optional.ofNullable(squads.get(id));
    }

    public Collection<Squad> all() {
        return squads.values();
    }

    public Set<String> takenCommanderNames() {
        Set<String> names = new HashSet<>();
        squads.values().forEach(s -> names.add(s.commanderName));
        return names;
    }

    public void join(Squad squad, HeroEntity hero) {
        squad.members.add(hero.getUUID());
        squad.initial = squad.members.size() + squad.fallen;
        setDirty();
    }

    void onSpotted(HeroEntity hero, Player player) {
        byId(hero.squad()).ifPresent(s -> {
            s.target = player.getUUID();
            setDirty();
        });
    }

    /** Герой погиб. Остальным — «союзник пал»; при потере половины — отступление. */
    public void onMemberDied(ServerLevel level, HeroEntity hero) {
        Squad s = squads.get(hero.squad());
        if (s == null) {
            return;
        }
        s.members.remove(hero.getUUID());
        s.fallen++;
        setDirty();
        Player target = s.target == null ? null : level.getServer().getPlayerList().getPlayer(s.target);
        forEachMember(level, s, ally -> ally.post(h -> new HeroEvent.AllyFell(h, h.heroClass().id(), h.rank().id(), s.id, target, hero)));
        if (s.members.isEmpty()) {
            end(level, s, true);
            return;
        }
        if (!s.retreating && s.members.size() * 2 <= s.initial && s.initial > 1) {
            s.retreating = true;
            HeroEntity leader = leader(level, s);
            if (leader != null) {
                leader.post(h -> new HeroEvent.SquadRetreating(h, h.heroClass().id(), h.rank().id(), s.id, target));
            }
            forEachMember(level, s, HeroEntity::startRetreat);
        }
    }

    /** Отступивший герой ушёл из мира. */
    void onLeft(HeroEntity hero) {
        Squad s = squads.get(hero.squad());
        if (s == null) {
            return;
        }
        s.members.remove(hero.getUUID());
        setDirty();
        if (s.members.isEmpty() && hero.level() instanceof ServerLevel level) {
            end(level, s, true);
        }
    }

    /** Отряд ушёл без боя (набег не случился, игрок не найден): угроза не падает. */
    public void disband(ServerLevel level, Squad s) {
        forEachMember(level, s, Entity::discard);
        end(level, s, false);
    }

    private void end(ServerLevel level, Squad s, boolean defeated) {
        squads.remove(s.id);
        setDirty();
        if (defeated) {
            ThreatManager.apply(level.getServer(), ThreatManager.Reason.RAID_DEFEATED);
        }
        END_LISTENERS.forEach(l -> l.accept(s, defeated));
    }

    private HeroEntity leader(ServerLevel level, Squad s) {
        HeroEntity best = null;
        for (UUID id : s.members) {
            if (level.getEntity(id) instanceof HeroEntity h) {
                if (h.heroClass() == HeroClass.COMMANDER) {
                    return h;
                }
                if (best == null || h.rank().ordinal() > best.rank().ordinal()) {
                    best = h;
                }
            }
        }
        return best;
    }

    private static void forEachMember(ServerLevel level, Squad s, Consumer<HeroEntity> action) {
        for (UUID id : List.copyOf(s.members)) {
            if (level.getEntity(id) instanceof HeroEntity h && h.isAlive()) {
                action.accept(h);
            }
        }
    }

    private static HeroSquads load(CompoundTag tag, HolderLookup.Provider registries) {
        HeroSquads data = new HeroSquads();
        for (Tag t : tag.getList("squads", Tag.TAG_COMPOUND)) {
            Squad s = Squad.load((CompoundTag) t);
            data.squads.put(s.id, s);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        squads.values().forEach(s -> list.add(s.save()));
        tag.put("squads", list);
        return tag;
    }
}
