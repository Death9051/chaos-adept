package ru.adept.core.village;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import ru.adept.core.AdeptCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Пулы имён деревень и командиров. Мод сюжета кладёт их в данные:
 * {@code data/<мод>/adept_names/villages.json} и {@code commanders.json}, формат {@code {"names": ["…", …]}}.
 * Файлы всех модов складываются. Без них — запасной пул ядра.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class NamePools extends SimpleJsonResourceReloadListener {
    public static final List<String> FALLBACK_VILLAGES = List.of(
            "Ольховка", "Заречье", "Броды", "Каменка", "Сосновый Лог", "Ветрянка", "Полынь", "Ключи",
            "Межа", "Овражье", "Тихий Плёс", "Берёзовка", "Гать", "Сухая Балка", "Мельничье", "Рябинки");
    public static final List<String> FALLBACK_COMMANDERS = List.of(
            "Ярополк", "Вышата", "Ратибор", "Добрыня", "Святослава", "Остромир", "Велимира", "Твердислав",
            "Милорад", "Горислава", "Борислав", "Ведана");

    private static List<String> villages = List.of();
    private static List<String> commanders = List.of();

    private NamePools() {
        super(new com.google.gson.Gson(), "adept_names");
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new NamePools());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<String> v = new ArrayList<>();
        List<String> c = new ArrayList<>();
        files.forEach((id, json) -> {
            List<String> target = switch (id.getPath()) {
                case "villages" -> v;
                case "commanders" -> c;
                default -> null;
            };
            if (target != null && json instanceof JsonObject obj && obj.get("names") instanceof JsonArray names) {
                names.forEach(n -> target.add(n.getAsString()));
            }
        });
        villages = List.copyOf(v);
        commanders = List.copyOf(c);
        AdeptCore.LOG.info("Пулы имён: деревень {}, командиров {}", villages.size(), commanders.size());
    }

    public static List<String> villages() {
        return villages.isEmpty() ? FALLBACK_VILLAGES : villages;
    }

    public static List<String> commanders() {
        return commanders.isEmpty() ? FALLBACK_COMMANDERS : commanders;
    }

    /** Имя из пула, по возможности не совпадающее с уже занятыми. */
    public static String pick(List<String> pool, java.util.Collection<String> taken, RandomSource random) {
        List<String> free = pool.stream().filter(n -> !taken.contains(n)).toList();
        if (!free.isEmpty()) {
            return free.get(random.nextInt(free.size()));
        }
        return pool.get(random.nextInt(pool.size())) + " " + (taken.size() + 1);
    }
}
