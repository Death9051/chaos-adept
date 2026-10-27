package ru.adept.story.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ru.adept.story.AdeptStory;
import ru.adept.story.client.ClientPayloads;
import ru.adept.story.journal.JournalData;

import java.util.List;

/** Сеть «Сюжета»: журнал и субтитры с сервера на клиент, отметка «прочитано» обратно. */
@EventBusSubscriber(modid = AdeptStory.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class StoryNetwork {
    private StoryNetwork() {
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AdeptStory.MOD_ID, path);
    }

    /** Весь журнал: при входе в мир и после сброса. */
    public record JournalSync(List<EntryView> entries, int seen) implements CustomPacketPayload {
        public static final Type<JournalSync> TYPE = new Type<>(id("journal_sync"));
        public static final StreamCodec<ByteBuf, JournalSync> STREAM_CODEC = StreamCodec.composite(
                EntryView.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalSync::entries,
                ByteBufCodecs.VAR_INT, JournalSync::seen,
                JournalSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Новая запись. */
    public record JournalAdded(EntryView entry) implements CustomPacketPayload {
        public static final Type<JournalAdded> TYPE = new Type<>(id("journal_added"));
        public static final StreamCodec<ByteBuf, JournalAdded> STREAM_CODEC =
                EntryView.STREAM_CODEC.map(JournalAdded::new, JournalAdded::entry);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер: игрок открыл журнал и видел записи до {@code count}. */
    public record JournalSeen(int count) implements CustomPacketPayload {
        public static final Type<JournalSeen> TYPE = new Type<>(id("journal_seen"));
        public static final StreamCodec<ByteBuf, JournalSeen> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(JournalSeen::new, JournalSeen::count);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Реплики субтитрами: ключ имени говорящего и ключи строк по порядку. */
    public record Subtitle(String speaker, List<String> lines) implements CustomPacketPayload {
        public static final Type<Subtitle> TYPE = new Type<>(id("subtitle"));
        public static final StreamCodec<ByteBuf, Subtitle> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Subtitle::speaker,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Subtitle::lines,
                Subtitle::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // Обработчики клиента вызываются только на клиенте; классы client.* грузятся при первом вызове.
        registrar.playToClient(JournalSync.TYPE, JournalSync.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.onSync(payload)));
        registrar.playToClient(JournalAdded.TYPE, JournalAdded.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.onAdded(payload)));
        registrar.playToClient(Subtitle.TYPE, Subtitle.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.onSubtitle(payload)));
        registrar.playToServer(JournalSeen.TYPE, JournalSeen.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.getServer() != null) {
                        JournalData data = JournalData.get(player.getServer());
                        int seen = Math.min(payload.count(), data.entries.size());
                        if (seen > data.seen) {
                            data.seen = seen;
                            data.setDirty();
                        }
                    }
                }));
    }

    public static void sendSync(ServerPlayer player) {
        JournalData data = JournalData.get(player.getServer());
        send(player, new JournalSync(List.copyOf(data.entries), data.seen));
    }

    public static void sendEntry(ServerPlayer player, EntryView entry) {
        send(player, new JournalAdded(entry));
    }

    public static void sendSubtitle(ServerPlayer player, String speaker, List<String> lines) {
        if (!lines.isEmpty()) {
            send(player, new Subtitle(speaker, List.copyOf(lines)));
        }
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        // В геймтестах бывают игроки без настоящего соединения — там пакет просто не нужен.
        if (player.connection == null) {
            return;
        }
        try {
            PacketDistributor.sendToPlayer(player, payload);
        } catch (RuntimeException e) {
            AdeptStory.LOG.debug("Пакет {} не отправлен: {}", payload.type().id(), e.getMessage());
        }
    }
}
