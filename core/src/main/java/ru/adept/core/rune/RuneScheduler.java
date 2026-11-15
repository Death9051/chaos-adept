package ru.adept.core.rune;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.AdeptCore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Отложенные действия рун: повтор «Эха», импульсы зон «Разлома» и слов, снятие временной защиты «Пустоты».
 * Живут только в памяти сервера: после перезапуска незавершённые эффекты не продолжаются.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class RuneScheduler {
    private record Task(ServerLevel level, long due, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private RuneScheduler() {
    }

    public static void later(ServerLevel level, int ticks, Runnable action) {
        if (ticks <= 0) {
            action.run();
            return;
        }
        synchronized (TASKS) {
            TASKS.add(new Task(level, level.getGameTime() + ticks, action));
        }
    }

    /** Повторять {@code action} каждые {@code period} тиков {@code times} раз, первый — сразу. */
    public static void repeat(ServerLevel level, int period, int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            later(level, i * period, action);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        List<Task> due = new ArrayList<>();
        synchronized (TASKS) {
            Iterator<Task> it = TASKS.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (t.level.getGameTime() >= t.due) {
                    due.add(t);
                    it.remove();
                }
            }
        }
        for (Task t : due) {
            try {
                t.action.run();
            } catch (RuntimeException e) {
                AdeptCore.LOG.warn("Отложенный эффект руны не сработал", e);
            }
        }
    }
}
