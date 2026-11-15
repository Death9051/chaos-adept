package ru.adept.core.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.artifact.RailGunItem;
import ru.adept.core.base.BaseData;
import ru.adept.core.base.CarverBlockEntity;
import ru.adept.core.rune.Rune;
import ru.adept.core.rune.RuneMaterial;
import ru.adept.core.rune.RuneSet;
import ru.adept.core.rune.Runes;

import java.util.ArrayList;
import java.util.List;

/**
 * Отладочные команды рун и рельсы:
 * {@code /adept rune engrave <руна> [руна] [материал]} — гравировка предмета в руке без высекателя;
 * {@code /adept rune show}, {@code /adept rune erase} — бросок стирания, как при подборе;
 * {@code /adept rail}, {@code /adept rail charge <n>}; {@code /adept carver precision <0..1>} — ступень высекателя.
 */
final class RuneCommands {
    private RuneCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> rune() {
        LiteralArgumentBuilder<CommandSourceStack> engrave = Commands.literal("engrave");
        for (Rune first : Rune.values()) {
            LiteralArgumentBuilder<CommandSourceStack> a = Commands.literal(first.id()).executes(ctx -> engrave(ctx, first, null, "iron"));
            for (Rune second : Rune.values()) {
                LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal(second.id()).executes(ctx -> engrave(ctx, first, second, "iron"));
                for (String m : materials()) {
                    b.then(Commands.literal(m).executes(ctx -> engrave(ctx, first, second, m)));
                }
                a.then(b);
            }
            engrave.then(a);
        }
        return Commands.literal("rune")
                .then(engrave)
                .then(Commands.literal("show").executes(ctx -> {
                    ItemStack held = ctx.getSource().getPlayerOrException().getMainHandItem();
                    RuneSet set = RuneSet.of(held);
                    return reply(ctx, set.isEmpty() ? "На предмете в руке рун нет"
                            : "Руны: " + Runes.ids(set) + ", материал " + set.material() + ", множитель " + set.multiplier()
                            + (set.word() != null ? ", слово " + set.word().shotId + " / " + set.word().armorId : ""));
                }))
                .then(Commands.literal("erase").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack held = player.getMainHandItem();
                    if (RuneSet.of(held).isEmpty()) {
                        return reply(ctx, "На предмете в руке рун нет");
                    }
                    return reply(ctx, Runes.rollErase(player, held, player.getRandom()) ? "Гравировка стёрлась" : "Гравировка уцелела");
                }));
    }

    private static List<String> materials() {
        List<String> out = new ArrayList<>();
        for (String raw : List.of("copper", "iron", "steel", "gold", "red_gold", "netherite", "diamond", "emerald", "lapis", "redstone")) {
            out.add(raw);
        }
        return out;
    }

    private static int engrave(CommandContext<CommandSourceStack> ctx, Rune first, Rune second, String material) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (Runes.kind(held).isEmpty()) {
            return reply(ctx, "В руке должны быть иглы, болты или пластина");
        }
        if (RuneMaterial.byId(material).isEmpty()) {
            return reply(ctx, "Материала «" + material + "» нет в конфиге");
        }
        List<Rune> runes = new ArrayList<>();
        runes.add(first);
        if (second != null) {
            runes.add(second);
        }
        new RuneSet(runes, material).applyTo(held);
        return reply(ctx, "Гравировка нанесена: " + runes + " (" + material + ")");
    }

    static LiteralArgumentBuilder<CommandSourceStack> rail() {
        return Commands.literal("rail")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack gun = player.getMainHandItem();
                    if (!(gun.getItem() instanceof RailGunItem)) {
                        return reply(ctx, "Рельсы в руке нет");
                    }
                    return reply(ctx, "Рельса: заряд " + RailGunItem.charge(gun) + " из " + RailGunItem.capacity()
                            + ", «Пробой» " + RailGunItem.pierceStep(player));
                })
                .then(Commands.literal("charge").then(Commands.argument("value", IntegerArgumentType.integer(0, 100000)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack gun = player.getMainHandItem();
                    if (!(gun.getItem() instanceof RailGunItem)) {
                        return reply(ctx, "Рельсы в руке нет");
                    }
                    RailGunItem.setCharge(gun, IntegerArgumentType.getInteger(ctx, "value"));
                    return reply(ctx, "Заряд рельсы: " + RailGunItem.charge(gun));
                })));
    }

    static LiteralArgumentBuilder<CommandSourceStack> carver() {
        return Commands.literal("carver")
                .then(Commands.literal("precision").then(Commands.argument("step", IntegerArgumentType.integer(0, 3)).executes(ctx -> {
                    BaseData.get(ctx.getSource().getServer()).setMachineStep(CarverBlockEntity.PRECISION, IntegerArgumentType.getInteger(ctx, "step"));
                    return reply(ctx, "«Точность» высекателя: " + IntegerArgumentType.getInteger(ctx, "step"));
                })));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
