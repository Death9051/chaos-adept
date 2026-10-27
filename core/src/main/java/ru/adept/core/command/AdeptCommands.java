package ru.adept.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ru.adept.core.api.WorldFlags;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.data.ArtifactProgress;
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
                .then(artifact()));
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
                .executes(ctx -> reply(ctx, "Уровень узлов: " + WorldProgress.get(ctx.getSource().getServer()).nodeTier()))
                .then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> {
                            WorldProgress progress = WorldProgress.get(ctx.getSource().getServer());
                            progress.setNodeTier(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "value"));
                            return reply(ctx, "Уровень узлов: " + progress.nodeTier());
                        })));
    }

    // /adept threat [set <0-100>]
    private static LiteralArgumentBuilder<CommandSourceStack> threat() {
        return Commands.literal("threat")
                .executes(ctx -> reply(ctx, "Угроза: " + WorldProgress.get(ctx.getSource().getServer()).threat()))
                .then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> {
                            WorldProgress progress = WorldProgress.get(ctx.getSource().getServer());
                            progress.setThreat(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "value"));
                            return reply(ctx, "Угроза: " + progress.threat());
                        })));
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
