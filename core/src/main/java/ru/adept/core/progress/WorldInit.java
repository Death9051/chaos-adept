package ru.adept.core.progress;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.ManaConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Команды при создании мира: выполняются один раз на мир, при первом запуске сервера
 * (например, {@code gamerule seasonalEvents false} для Born in Chaos). Список — {@code worldInit.createCommands}.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class WorldInit {
    private static final String DATA_NAME = "adept_core_world_init";

    private WorldInit() {
    }

    /** Команды к выполнению: строки «мод|команда» — только при загруженном моде, без «мод|» — всегда. */
    public static List<String> commandsFor(List<? extends String> entries, Predicate<String> modLoaded) {
        List<String> out = new ArrayList<>();
        for (String raw : entries) {
            String entry = raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int bar = entry.indexOf('|');
            if (bar >= 0) {
                String mod = entry.substring(0, bar).trim();
                String command = entry.substring(bar + 1).trim();
                if (!command.isEmpty() && (mod.isEmpty() || modLoaded.test(mod))) {
                    out.add(command);
                }
            } else {
                out.add(entry);
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Done done = server.overworld().getDataStorage().computeIfAbsent(Done.FACTORY, DATA_NAME);
        if (done.value) {
            return;
        }
        for (String command : commandsFor(ManaConfig.WORLD_CREATE_COMMANDS.get(), ModList.get()::isLoaded)) {
            try {
                int result = server.getCommands().getDispatcher().execute(command,
                        server.createCommandSourceStack().withPermission(4).withSuppressedOutput());
                AdeptCore.LOG.info("Создание мира: «{}» (результат {})", command, result);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                AdeptCore.LOG.error("Создание мира: команда «{}» не прошла: {}", command, e.getMessage());
            }
        }
        done.value = true;
        done.setDirty();
    }

    /** Отметка «команды создания мира выполнены». */
    private static final class Done extends SavedData {
        static final SavedData.Factory<Done> FACTORY = new SavedData.Factory<>(Done::new, Done::load, null);
        boolean value;

        static Done load(CompoundTag tag, HolderLookup.Provider registries) {
            Done d = new Done();
            d.value = tag.getBoolean("done");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("done", value);
            return tag;
        }
    }
}
