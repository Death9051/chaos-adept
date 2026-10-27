package ru.adept.story.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ru.adept.story.AdeptStory;
import ru.adept.story.journal.Journal;
import ru.adept.story.journal.JournalData;
import ru.adept.story.journal.JournalDef;
import ru.adept.story.journal.JournalDefs;
import ru.adept.story.net.EntryView;
import ru.adept.story.net.StoryNetwork;
import ru.adept.story.trader.TraderTalk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Отладочные команды «Сюжета» (права 2):
 * <pre>
 * /story journal list            — сколько записей получено и какие
 * /story journal reset           — сбросить журнал и отметки «впервые»
 * /story journal add &lt;id&gt;        — добавить запись в обход условий
 * /story trigger &lt;тип&gt; [k=v …]  — вызвать событие журнала, например: node_absorbed level=2
 * /story trader &lt;arrive|ritual&gt;   — реплики торговца при появлении (обычном или первом ритуале)
 * /story subtitle &lt;ключ&gt;         — показать строку субтитром от имени торговца
 * </pre>
 */
@EventBusSubscriber(modid = AdeptStory.MOD_ID)
public final class StoryCommands {
    private StoryCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("story")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("journal")
                        .then(Commands.literal("list").executes(StoryCommands::list))
                        .then(Commands.literal("reset").executes(StoryCommands::reset))
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(JournalDefs.ids(), builder))
                                        .executes(StoryCommands::add))))
                .then(Commands.literal("trigger")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(JournalDefs.SUPPORTED, builder))
                                .executes(ctx -> trigger(ctx, ""))
                                .then(Commands.argument("attrs", StringArgumentType.greedyString())
                                        .executes(ctx -> trigger(ctx, StringArgumentType.getString(ctx, "attrs"))))))
                .then(Commands.literal("trader")
                        .then(Commands.literal("arrive").executes(ctx -> trader(ctx, false)))
                        .then(Commands.literal("ritual").executes(ctx -> trader(ctx, true))))
                .then(Commands.literal("subtitle")
                        .then(Commands.argument("key", StringArgumentType.greedyString())
                                .executes(StoryCommands::subtitle))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        JournalData data = JournalData.get(ctx.getSource().getServer());
        StringBuilder ids = new StringBuilder();
        for (EntryView e : data.entries) {
            if (!ids.isEmpty()) {
                ids.append(", ");
            }
            ids.append(e.id());
        }
        int count = data.entries.size();
        ctx.getSource().sendSuccess(() -> Component.literal("Журнал: " + count + " записей, прочитано "
                + data.seen + ", ветка " + Journal.branch(ctx.getSource().getServer()) + ". " + ids), false);
        return count;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        JournalData.get(server).reset();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            StoryNetwork.sendSync(player);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Журнал сброшен"), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        JournalDef def = JournalDefs.get(id);
        if (def == null) {
            ctx.getSource().sendFailure(Component.literal("Нет записи " + id));
            return 0;
        }
        Journal.force(ctx.getSource().getServer(), def);
        ctx.getSource().sendSuccess(() -> Component.literal("Добавлена запись " + id), true);
        return 1;
    }

    private static int trigger(CommandContext<CommandSourceStack> ctx, String attrsText) {
        MinecraftServer server = ctx.getSource().getServer();
        String type = StringArgumentType.getString(ctx, "type");
        Map<String, String> attrs = new HashMap<>();
        for (String part : attrsText.trim().split("\\s+")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                attrs.put(part.substring(0, eq), part.substring(eq + 1));
            }
        }
        List<EntryView> added;
        if (type.equals("node_absorbed") && !attrs.containsKey("first")) {
            // Без явного first считаем «впервые» так же, как при настоящем поглощении.
            added = Journal.nodeAbsorbed(server, Integer.parseInt(attrs.getOrDefault("level", "1")));
        } else {
            added = Journal.fire(server, type, attrs);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Событие " + type + " " + attrs + ": записей " + added.size()), false);
        return added.size();
    }

    private static int trader(CommandContext<CommandSourceStack> ctx, boolean firstRitual) {
        ServerPlayer player = ctx.getSource().getPlayer();
        List<String> lines = TraderTalk.arrivalLines(ctx.getSource().getServer(), firstRitual);
        if (player != null) {
            StoryNetwork.sendSubtitle(player, TraderTalk.SPEAKER, lines);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Торговец: " + lines), false);
        return lines.size();
    }

    private static int subtitle(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Нужен игрок"));
            return 0;
        }
        StoryNetwork.sendSubtitle(player, TraderTalk.SPEAKER, List.of(StringArgumentType.getString(ctx, "key")));
        return 1;
    }
}
