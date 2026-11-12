package ru.adept.core.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.artifact.LightArmorItem;
import ru.adept.core.artifact.LightArmorSystem;
import ru.adept.core.config.FocusConfig;
import ru.adept.core.focus.FocusCrystal;
import ru.adept.core.focus.FocusCut;
import ru.adept.core.focus.FocusItem;
import ru.adept.core.focus.Focuses;

/**
 * Отладочные команды фокусов и щита лёгкой брони:
 * {@code /adept focus give <огранка> <кристалл>}, {@code /adept focus wear <раз>}, {@code /adept focus repair};
 * {@code /adept shield}, {@code /adept shield set <единицы> <мана>}, {@code /adept shield hit <урон>}.
 * Фокус и щит — у нагрудника лёгкой брони, надетого на игрока.
 */
final class FocusCommands {
    private FocusCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> focus() {
        LiteralArgumentBuilder<CommandSourceStack> give = Commands.literal("give");
        for (FocusCut cut : FocusCut.values()) {
            LiteralArgumentBuilder<CommandSourceStack> c = Commands.literal(cut.id());
            for (String crystal : new String[]{FocusCrystal.QUARTZ, FocusCrystal.LAPIS, FocusCrystal.AMETHYST,
                    FocusCrystal.EMERALD, FocusCrystal.DIAMOND, FocusCrystal.NODE}) {
                c.then(Commands.literal(crystal).executes(ctx -> {
                    var found = FocusCrystal.byId(crystal);
                    if (found.isEmpty()) {
                        return reply(ctx, "Кристалла «" + crystal + "» нет в конфиге");
                    }
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack focus = FocusItem.create(cut, found.get());
                    if (!player.getInventory().add(focus)) {
                        player.drop(focus, false);
                    }
                    return reply(ctx, "Выдан фокус " + cut.id() + " (" + crystal + ")");
                }));
            }
            give.then(c);
        }
        return Commands.literal("focus")
                .then(give)
                .then(Commands.literal("wear").then(Commands.argument("times", IntegerArgumentType.integer(1, 1000)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack chest = LightArmorSystem.chest(player);
                    ItemStack focus = chest.isEmpty() ? ItemStack.EMPTY : LightArmorItem.focus(chest);
                    if (!Focuses.active(focus)) {
                        return reply(ctx, "В нагруднике лёгкой брони нет рабочего фокуса");
                    }
                    int times = IntegerArgumentType.getInteger(ctx, "times");
                    Focuses.Wear last = Focuses.Wear.WORN;
                    int done = 0;
                    while (done < times && Focuses.active(focus)) {
                        last = Focuses.wear(player, focus, player.getRandom());
                        done++;
                        if (last == Focuses.Wear.BROKEN) {
                            focus = ItemStack.EMPTY;
                            break;
                        }
                    }
                    LightArmorItem.setFocus(chest, focus);
                    return reply(ctx, "Износ ×" + done + ": " + switch (last) {
                        case BROKEN -> "фокус сломался";
                        case CRACKED -> "фокус треснул";
                        default -> "прочность " + FocusItem.durability(focus) + "/" + FocusItem.max(focus);
                    });
                })))
                .then(Commands.literal("repair").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack chest = LightArmorSystem.chest(player);
                    ItemStack focus = chest.isEmpty() ? ItemStack.EMPTY : LightArmorItem.focus(chest);
                    if (focus.isEmpty()) {
                        return reply(ctx, "В нагруднике лёгкой брони нет фокуса");
                    }
                    FocusItem.repair(focus);
                    LightArmorItem.setFocus(chest, focus);
                    return reply(ctx, "Фокус починен");
                }));
    }

    static LiteralArgumentBuilder<CommandSourceStack> shield() {
        return Commands.literal("shield")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack chest = LightArmorSystem.chest(player);
                    if (chest.isEmpty()) {
                        return reply(ctx, "Нагрудник лёгкой брони не надет");
                    }
                    ItemStack focus = LightArmorItem.focus(chest);
                    return reply(ctx, "Щит " + String.format(java.util.Locale.ROOT, "%.1f", LightArmorItem.shield(chest))
                            + " из " + LightArmorSystem.capacity(player) + ", мана костюма " + LightArmorItem.mana(chest)
                            + ", фокус: " + (focus.isEmpty() ? "нет" : FocusItem.cut(focus).id() + " " + FocusItem.crystalId(focus)
                            + (FocusItem.cracked(focus) ? " (треснул)" : " " + FocusItem.durability(focus) + "/" + FocusItem.max(focus))));
                })
                .then(Commands.literal("set").then(Commands.argument("units", DoubleArgumentType.doubleArg(0, 10000))
                        .then(Commands.argument("mana", IntegerArgumentType.integer(0, 100000)).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ItemStack chest = LightArmorSystem.chest(player);
                            if (chest.isEmpty()) {
                                return reply(ctx, "Нагрудник лёгкой брони не надет");
                            }
                            LightArmorItem.setMana(chest, IntegerArgumentType.getInteger(ctx, "mana"));
                            LightArmorItem.setShield(chest, DoubleArgumentType.getDouble(ctx, "units"), LightArmorSystem.capacity(player));
                            return reply(ctx, "Щит и мана костюма заданы (мана до " + FocusConfig.LIGHT_MANA_CAPACITY.get() + ")");
                        }))))
                .then(Commands.literal("hit").then(Commands.argument("damage", DoubleArgumentType.doubleArg(0, 1000)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack chest = LightArmorSystem.chest(player);
                    if (chest.isEmpty()) {
                        return reply(ctx, "Нагрудник лёгкой брони не надет");
                    }
                    float passed = LightArmorSystem.absorb(player, chest, (float) DoubleArgumentType.getDouble(ctx, "damage"), null);
                    return reply(ctx, "Прошло в здоровье: " + String.format(java.util.Locale.ROOT, "%.1f", passed)
                            + ", щит " + String.format(java.util.Locale.ROOT, "%.1f", LightArmorItem.shield(chest)));
                })));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
