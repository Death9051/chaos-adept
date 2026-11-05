package ru.adept.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ru.adept.core.api.WorldFlags;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.absorb.AbsorptionData;
import ru.adept.core.absorb.AbsorptionManager;
import ru.adept.core.base.BaseData;
import ru.adept.core.base.BaseTravel;
import ru.adept.core.camp.ExtractionManager;
import ru.adept.core.camp.WaveSpawner;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.threat.ThreatManager;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.node.NodeGrid;
import ru.adept.core.node.NodePlacement;
import ru.adept.core.node.NodeRecord;
import ru.adept.core.node.NodeRegistry;
import ru.adept.core.node.NodeSearch;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.trader.TraderManager;
import ru.adept.core.registry.ModRegistries;

/**
 * Отладочные команды {@code /adept ...} (уровень прав 2, как у /give).
 * Нужны для точечных проверок без прохождения. Каждая часть среза добавляет свою ветку:
 * узлы — {@code node}, волны — {@code wave}, страж — {@code guardian} и т. д.
 */
public final class AdeptCommands {
    private AdeptCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("adept")
                .requires(src -> src.hasPermission(2))
                .then(flag())
                .then(tier())
                .then(threat())
                .then(artifact())
                .then(node())
                .then(wave())
                .then(guardian())
                .then(mob())
                .then(village())
                .then(hero())
                .then(squad())
                .then(raid())
                .then(extraction())
                .then(absorb())
                .then(base())
                .then(trader()));
    }

    // /adept flag get|set|remove <key> [value]
    private static LiteralArgumentBuilder<CommandSourceStack> flag() {
        return Commands.literal("flag")
                .then(Commands.literal("get").then(Commands.argument("key", ResourceLocationArgument.id())
                        .executes(ctx -> {
                            String key = ResourceLocationArgument.getId(ctx, "key").toString();
                            String value = WorldFlags.get(ctx.getSource().getServer()).get(key).orElse("—");
                            return reply(ctx, key + " = " + value);
                        })))
                .then(Commands.literal("set").then(Commands.argument("key", ResourceLocationArgument.id())
                        .then(Commands.argument("value", StringArgumentType.greedyString()).executes(ctx -> {
                            String key = ResourceLocationArgument.getId(ctx, "key").toString();
                            String value = StringArgumentType.getString(ctx, "value");
                            WorldFlags.get(ctx.getSource().getServer()).set(key, value);
                            return reply(ctx, key + " = " + value);
                        }))))
                .then(Commands.literal("remove").then(Commands.argument("key", ResourceLocationArgument.id())
                        .executes(ctx -> {
                            String key = ResourceLocationArgument.getId(ctx, "key").toString();
                            WorldFlags.get(ctx.getSource().getServer()).remove(key);
                            return reply(ctx, key + " удалён");
                        })));
    }

    // /adept tier [set <n>] — уровень узлов мира
    private static LiteralArgumentBuilder<CommandSourceStack> tier() {
        return Commands.literal("tier")
                .executes(ctx -> {
                    WorldProgress progress = WorldProgress.get(ctx.getSource().getServer());
                    return reply(ctx, "Уровень узлов: " + progress.nodeTier() + ", до вехи зачищено "
                            + progress.cleansedAtTier() + "/" + BalanceConfig.TIER_NODES_TO_ADVANCE.get()
                            + ", сложность Improved Mobs " + ru.adept.core.progress.TierProgression.difficultyFor(progress.nodeTier()));
                })
                .then(Commands.literal("cleanse").executes(ctx -> {
                    WorldProgress progress = WorldProgress.get(ctx.getSource().getServer());
                    progress.recordCleansed(ctx.getSource().getServer(), progress.nodeTier());
                    return reply(ctx, "Уровень узлов: " + progress.nodeTier() + ", до вехи зачищено "
                            + progress.cleansedAtTier() + "/" + BalanceConfig.TIER_NODES_TO_ADVANCE.get());
                }))
                .then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> {
                            WorldProgress progress = WorldProgress.get(ctx.getSource().getServer());
                            progress.setNodeTier(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "value"));
                            return reply(ctx, "Уровень узлов: " + progress.nodeTier());
                        })));
    }

    // /adept threat [set <0-100> | add <±n>] — угроза мира и её ступень
    private static LiteralArgumentBuilder<CommandSourceStack> threat() {
        return Commands.literal("threat")
                .executes(ctx -> threatReply(ctx))
                .then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            ThreatManager.add(server, IntegerArgumentType.getInteger(ctx, "value") - ThreatManager.threat(server),
                                    ThreatManager.Reason.COMMAND);
                            return threatReply(ctx);
                        })))
                .then(Commands.literal("add").then(Commands.argument("delta", IntegerArgumentType.integer(-100, 100))
                        .executes(ctx -> {
                            ThreatManager.add(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "delta"),
                                    ThreatManager.Reason.COMMAND);
                            return threatReply(ctx);
                        })));
    }

    private static int threatReply(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        return reply(ctx, "Угроза: " + ThreatManager.threat(server) + " (" + ThreatManager.level(server).id() + ")");
    }

    // /adept artifact show|choose|step|reset <player> ...
    private static LiteralArgumentBuilder<CommandSourceStack> artifact() {
        return Commands.literal("artifact")
                .then(Commands.literal("show").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ArtifactProgress p = progress(ctx);
                            return reply(ctx, "Пары: " + p.choices() + "; ступени: " + p.steps());
                        })))
                .then(Commands.literal("choose").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("category", StringArgumentType.word())
                                .then(Commands.argument("variant", StringArgumentType.word()).executes(ctx -> {
                                    String category = StringArgumentType.getString(ctx, "category");
                                    String variant = StringArgumentType.getString(ctx, "variant");
                                    setProgress(ctx, progress(ctx).withChoice(category, variant));
                                    return reply(ctx, category + " → " + variant);
                                })))))
                .then(Commands.literal("step").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("key", StringArgumentType.string())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 3)).executes(ctx -> {
                                    String key = StringArgumentType.getString(ctx, "key");
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    setProgress(ctx, progress(ctx).withStep(key, value));
                                    return reply(ctx, key + " = " + value);
                                })))))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            setProgress(ctx, ArtifactProgress.EMPTY);
                            return reply(ctx, "Прогресс артефактов сброшен");
                        })));
    }

    // /adept node — ближайший узел; node spawn <уровень> — поставить узел рядом; node list — сколько поставлено
    private static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("node")
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    BlockPos from = BlockPos.containing(src.getPosition());
                    return NodeSearch.nearest(src.getLevel(), from, BalanceConfig.RESONATOR_RANGE.get() * 4)
                            .map(f -> reply(ctx, "Ближайший узел: уровень " + f.level() + ", " + f.column().toShortString()
                                    + (f.placed() ? " (поставлен)" : " (ещё не загружен)")))
                            .orElseGet(() -> reply(ctx, "Узлов поблизости нет"));
                })
                .then(Commands.literal("list").executes(ctx -> {
                    NodeRegistry registry = NodeRegistry.get(ctx.getSource().getServer());
                    long cleansed = registry.all().stream().filter(n -> n.state() == NodeRecord.State.CLEANSED).count();
                    return reply(ctx, "Узлов поставлено: " + registry.all().size() + ", зачищено: " + cleansed);
                }))
                .then(Commands.literal("spawn").then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            BlockPos column = BlockPos.containing(src.getPosition()).offset(3, 0, 0);
                            BlockPos pos = NodePlacement.place(src.getLevel(), column);
                            int level = IntegerArgumentType.getInteger(ctx, "level");
                            String biome = NodeGrid.biomeAt(src.getLevel(), pos.getX(), pos.getZ()).unwrapKey()
                                    .map(k -> k.location().toString()).orElse("unknown");
                            NodeRegistry.get(src.getServer()).add(new NodeRecord(pos, level, biome, NodeRecord.State.DORMANT), null);
                            return reply(ctx, "Узел уровня " + level + " поставлен: " + pos.toShortString());
                        })));
    }

    // /adept wave — начать следующую волну; wave clear — засчитать текущую отбитой
    private static LiteralArgumentBuilder<CommandSourceStack> wave() {
        return Commands.literal("wave")
                .executes(ctx -> ExtractionManager.startNextWave(ctx.getSource().getServer())
                        ? reply(ctx, "Волна начата") : reply(ctx, "Нет выкачки или волна уже идёт"))
                .then(Commands.literal("clear").executes(ctx -> ExtractionManager.forceClearWave(ctx.getSource().getServer())
                        ? reply(ctx, "Волна убрана, засчитается на следующем тике") : reply(ctx, "Волна не идёт")));
    }

    // /adept guardian — поставить стража узла рядом (вне выкачки, для проверки)
    private static LiteralArgumentBuilder<CommandSourceStack> guardian() {
        return Commands.literal("guardian").executes(ctx -> {
            CommandSourceStack src = ctx.getSource();
            BlockPos pos = BlockPos.containing(src.getPosition()).offset(4, 0, 0);
            WaveSpawner.add(src.getLevel(), ModEntities.HORNED_LEADER.get().create(src.getLevel()), pos,
                    BalanceConfig.GUARDIAN_HEALTH.get(), BalanceConfig.GUARDIAN_DAMAGE.get(), src.getPlayer(), null);
            return reply(ctx, "Рогатый вожак поставлен");
        });
    }

    // /adept mob <boar_leader|armadillo|sporeling|sporeling_small> — поставить противника узла рядом
    private static LiteralArgumentBuilder<CommandSourceStack> mob() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mob");
        java.util.Map<String, java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>>> mobs =
                java.util.Map.of("boar_leader", ModEntities.BOAR_LEADER, "armadillo", ModEntities.ARMADILLO,
                        "sporeling", ModEntities.SPORELING, "sporeling_small", ModEntities.SPORELING_SMALL,
                        "hornet", ModEntities.HORNET, "wisp", ModEntities.WISP, "ember_spirit", ModEntities.EMBER_SPIRIT);
        mobs.forEach((name, type) -> root.then(Commands.literal(name).executes(ctx -> {
            CommandSourceStack src = ctx.getSource();
            BlockPos pos = BlockPos.containing(src.getPosition()).offset(4, 0, 0);
            WaveSpawner.add(src.getLevel(), type.get().create(src.getLevel()), pos, -1, -1, src.getPlayer(), null);
            return reply(ctx, name + " поставлен");
        })));
        return root;
    }

    // /adept village — деревня рядом; village list; village ruin — сделать руинами деревню рядом
    private static LiteralArgumentBuilder<CommandSourceStack> village() {
        return Commands.literal("village")
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    return ru.adept.core.village.VillageRegistry.get(src.getServer())
                            .findOrRegisterAt(src.getLevel(), BlockPos.containing(src.getPosition()))
                            .map(v -> reply(ctx, "Деревня «" + v.name + "» #" + v.id + ": жителей " + v.residents.size()
                                    + ", старейшина " + (v.elder != null ? "жив" : "нет")
                                    + (v.ruined ? ", руины" : "") + (v.destroyed ? ", разрушена" : "")))
                            .orElseGet(() -> reply(ctx, "Рядом нет деревни"));
                })
                .then(Commands.literal("list").executes(ctx -> {
                    var all = ru.adept.core.village.VillageRegistry.get(ctx.getSource().getServer()).all();
                    StringBuilder sb = new StringBuilder("Деревень: " + all.size());
                    all.forEach(v -> sb.append("\n«").append(v.name).append("» ").append(v.center().toShortString())
                            .append(v.ruined ? " (руины)" : ""));
                    return reply(ctx, sb.toString());
                }))
                .then(Commands.literal("ruin").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    return ru.adept.core.village.VillageRegistry.get(src.getServer())
                            .findOrRegisterAt(src.getLevel(), BlockPos.containing(src.getPosition()))
                            .map(v -> reply(ctx, "«" + v.name + "» — руины, колонок: "
                                    + ru.adept.core.village.RuinsManager.ruinWhole(src.getLevel(), v)))
                            .orElseGet(() -> reply(ctx, "Рядом нет деревни"));
                }));
    }

    // /adept hero <tracker|warrior|archer|commander> [veteran] — одиночный герой рядом (без отряда)
    private static LiteralArgumentBuilder<CommandSourceStack> hero() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("hero");
        for (ru.adept.core.hero.HeroClass c : ru.adept.core.hero.HeroClass.values()) {
            root.then(Commands.literal(c.id())
                    .executes(ctx -> spawnHero(ctx, c, ru.adept.core.hero.HeroRank.APPRENTICE))
                    .then(Commands.literal("veteran").executes(ctx -> spawnHero(ctx, c, ru.adept.core.hero.HeroRank.VETERAN))));
        }
        return root;
    }

    private static int spawnHero(CommandContext<CommandSourceStack> ctx, ru.adept.core.hero.HeroClass c, ru.adept.core.hero.HeroRank rank) {
        CommandSourceStack src = ctx.getSource();
        BlockPos pos = BlockPos.containing(src.getPosition()).offset(5, 0, 0);
        var hero = ru.adept.core.hero.HeroSpawner.spawnHero(src.getLevel(), pos, c, rank, null);
        if (hero != null && c == ru.adept.core.hero.HeroClass.COMMANDER) {
            hero.setHeroName(ru.adept.core.village.NamePools.pick(ru.adept.core.village.NamePools.commanders(), java.util.Set.of(), src.getLevel().random));
        }
        return reply(ctx, c.id() + " (" + rank.id() + ") поставлен");
    }

    // /adept squad <scouts|squads|warbands> — отряд вокруг игрока по ступени угрозы, от ближайшей деревни
    private static LiteralArgumentBuilder<CommandSourceStack> squad() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("squad");
        for (ru.adept.core.threat.ThreatLevel level : ru.adept.core.threat.ThreatLevel.values()) {
            if (level == ru.adept.core.threat.ThreatLevel.CALM) {
                continue;
            }
            root.then(Commands.literal(level.id()).executes(ctx -> {
                CommandSourceStack src = ctx.getSource();
                BlockPos at = BlockPos.containing(src.getPosition());
                var village = ru.adept.core.village.VillageRegistry.get(src.getServer()).all().stream()
                        .filter(v -> !v.ruined).min(java.util.Comparator.comparingDouble(v -> v.center().distSqr(at))).orElse(null);
                var squad = ru.adept.core.hero.HeroSpawner.spawnSquad(src.getLevel(), at, src.getPlayer(), level, village);
                return reply(ctx, "Отряд: " + squad.members.size() + " героев"
                        + (squad.commanderName.isEmpty() ? "" : ", командир " + squad.commanderName)
                        + (squad.villageName.isEmpty() ? "" : ", из деревни «" + squad.villageName + "»"));
            }));
        }
        return root;
    }

    // /adept raid — почему набега нет / состояние; raid check — проверка с шансом; raid now — рог без шанса;
    // raid spawn — отряд сразу, без рога
    private static LiteralArgumentBuilder<CommandSourceStack> raid() {
        return Commands.literal("raid")
                .executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    var st = ru.adept.core.raid.RaidManager.state(player);
                    long now = ctx.getSource().getServer().overworld().getGameTime();
                    String status = st.spawnAt > 0 ? "рог прозвучал, отряд через " + (st.spawnAt - now) / 20 + " с"
                            : st.squad != null ? (st.waitUntil > 0 ? "отряд ждёт у выхода " + (st.waitUntil - now) / 20 + " с" : "идёт бой")
                            : ru.adept.core.raid.RaidManager.blocker(player).map(b -> "набег невозможен: " + b).orElse("набег возможен");
                    return reply(ctx, "Набег: " + status);
                })
                .then(Commands.literal("check").executes(ctx -> reply(ctx,
                        ru.adept.core.raid.RaidManager.check(ctx.getSource().getPlayerOrException(), false) ? "Рог!" : "Набега нет")))
                .then(Commands.literal("now").executes(ctx -> reply(ctx,
                        ru.adept.core.raid.RaidManager.check(ctx.getSource().getPlayerOrException(), true) ? "Рог!" : "Набег невозможен")))
                .then(Commands.literal("spawn").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    var st = ru.adept.core.raid.RaidManager.state(player);
                    var level = ThreatManager.level(ctx.getSource().getServer());
                    ru.adept.core.raid.RaidManager.horn(player, level == ru.adept.core.threat.ThreatLevel.CALM ? ru.adept.core.threat.ThreatLevel.SCOUTS : level);
                    st.spawnAt = ctx.getSource().getServer().overworld().getGameTime();
                    return reply(ctx, "Отряд появится на следующей секунде");
                }));
    }

    // /adept extraction — состояние; extraction set <проценты>
    private static LiteralArgumentBuilder<CommandSourceStack> extraction() {
        return Commands.literal("extraction")
                .executes(ctx -> ExtractionManager.session(ctx.getSource().getServer())
                        .map(s -> reply(ctx, "Выкачка ур. " + s.level + ": " + s.percent() + "%, волн отбито " + s.wavesCleared
                                + "/" + s.waves + (s.waveActive ? ", идёт волна (" + s.waveMobs.size() + " мобов)" : "")
                                + ", установка " + s.extractorHealth + (s.done ? ", узел зачищен" : "")))
                        .orElseGet(() -> reply(ctx, "Лагерь не развёрнут")))
                .then(Commands.literal("set").then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> ExtractionManager.setProgress(ctx.getSource().getServer(),
                                IntegerArgumentType.getInteger(ctx, "percent"))
                                ? reply(ctx, "Прогресс задан") : reply(ctx, "Нет активной выкачки"))));
    }

    // /adept absorb — состояние; absorb here — поглотить биом в точке игрока
    private static LiteralArgumentBuilder<CommandSourceStack> absorb() {
        return Commands.literal("absorb")
                .executes(ctx -> {
                    AbsorptionData data = AbsorptionData.get(ctx.getSource().getServer());
                    return reply(ctx, "Ждут поглощения: областей " + data.pendingRegionCount() + ", чанков " + data.pendingChunkCount());
                })
                .then(Commands.literal("here").executes(ctx -> {
                    AbsorptionManager.start(ctx.getSource().getServer(), BlockPos.containing(ctx.getSource().getPosition()),
                            WorldProgress.get(ctx.getSource().getServer()).nodeTier());
                    return reply(ctx, "Поглощение биома поставлено в очередь");
                }));
    }

    // /adept base — на базу и обратно; base energy <электричество> <мана>
    private static LiteralArgumentBuilder<CommandSourceStack> base() {
        return Commands.literal("base")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (BaseTravel.isBase(player.level())) {
                        BaseTravel.leave(player);
                    } else {
                        BaseTravel.enter(player, player.position(), player.getYRot());
                    }
                    return 1;
                })
                .then(Commands.literal("energy").then(Commands.argument("electricity", IntegerArgumentType.integer(0, 30_000))
                        .then(Commands.argument("mana", IntegerArgumentType.integer(0, 30_000)).executes(ctx -> {
                            BaseData data = BaseData.get(ctx.getSource().getServer());
                            data.set(IntegerArgumentType.getInteger(ctx, "electricity"), IntegerArgumentType.getInteger(ctx, "mana"));
                            return reply(ctx, "Запас базы: электричество " + data.electricity() + ", мана " + data.mana());
                        }))));
    }

    // /adept trader — ритуал вызова у игрока (без условий, для проверки)
    private static LiteralArgumentBuilder<CommandSourceStack> trader() {
        return Commands.literal("trader").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TraderManager.startRitual(player, player.blockPosition().relative(player.getDirection(), 3));
            return reply(ctx, "Ритуал начат");
        });
    }

    private static ArtifactProgress progress(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return EntityArgument.getPlayer(ctx, "player").getData(ModRegistries.ARTIFACT_PROGRESS);
    }

    private static void setProgress(CommandContext<CommandSourceStack> ctx, ArtifactProgress value) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        player.setData(ModRegistries.ARTIFACT_PROGRESS, value);
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
