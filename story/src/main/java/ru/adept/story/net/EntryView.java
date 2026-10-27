package ru.adept.story.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** Полученная запись журнала: id описания, игровой день, страница, ключи текста (абзацы), всплывающая ли. */
public record EntryView(String id, int day, String page, List<String> keys, boolean popup) {
    public static final StreamCodec<ByteBuf, EntryView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EntryView::id,
            ByteBufCodecs.VAR_INT, EntryView::day,
            ByteBufCodecs.STRING_UTF8, EntryView::page,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), EntryView::keys,
            ByteBufCodecs.BOOL, EntryView::popup,
            EntryView::new);
}
