package ru.adept.core.base;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Точка возврата с базы: измерение и координаты, откуда игрок ушёл. */
public record ReturnPoint(String dimension, double x, double y, double z, float yaw) {
    public static final ReturnPoint NONE = new ReturnPoint("", 0, 0, 0, 0);

    public static final Codec<ReturnPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("dimension").forGetter(ReturnPoint::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(ReturnPoint::x),
            Codec.DOUBLE.fieldOf("y").forGetter(ReturnPoint::y),
            Codec.DOUBLE.fieldOf("z").forGetter(ReturnPoint::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(ReturnPoint::yaw)
    ).apply(i, ReturnPoint::new));

    public boolean isSet() {
        return !dimension.isEmpty();
    }
}
