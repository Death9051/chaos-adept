package ru.adept.story.journal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.api.event.ArtifactEvent;
import ru.adept.core.api.event.BaseEvent;
import ru.adept.core.api.event.BiomeAbsorbedEvent;
import ru.adept.core.api.event.NodeEvent;
import ru.adept.story.AdeptStory;
import ru.adept.story.net.StoryNetwork;

import java.util.HashMap;
import java.util.Map;

/**
 * Источники событий журнала: события ядра и ванильные триггеры (первая ночь, золото, стол, биомы).
 * Реплики и вехи торговца — в {@link ru.adept.story.trader.TraderTalk}.
 */
@EventBusSubscriber(modid = AdeptStory.MOD_ID)
public final class StoryTriggers {
    private static final ResourceLocation ARTIFACT_TABLE = ResourceLocation.fromNamespaceAndPath("adept_core", "artifact_table");

    /** Уровень зачищенного узла до поглощения его биома (событие поглощения уровня не несёт). */
    private static final Map<BlockPos, Integer> CLEANSED_LEVELS = new HashMap<>();

    private StoryTriggers() {
    }

    // --- Вход в мир: журнал целиком на клиент ---

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoryNetwork.sendSync(player);
        }
    }

    // --- Ядро ---

    @SubscribeEvent
    public static void onPairChosen(ArtifactEvent.PairChosen event) {
        Journal.fire(event.getServer(), "artifact_pair_chosen",
                Map.of("category", event.getCategory(), "variant", event.getVariant()));
    }

    @SubscribeEvent
    public static void onPrototype(ArtifactEvent.PrototypeCrafted event) {
        Journal.fire(event.getServer(), "artifact_prototype_crafted", Map.of("artifact", event.getArtifact()));
    }

    @SubscribeEvent
    public static void onStep(ArtifactEvent.StepUnlocked event) {
        if (event.getKey().endsWith("/apex")) {
            Journal.fire(event.getServer(), "artifact_segment_apex", Map.of("key", event.getKey()));
        }
    }

    @SubscribeEvent
    public static void onBaseEntered(BaseEvent.Entered event) {
        Journal.fire(event.getServer(), "base_entered");
    }

    @SubscribeEvent
    public static void onWaveCleared(NodeEvent.WaveCleared event) {
        Journal.fire(event.getServer(), "node_wave_cleared", Map.of("level", Integer.toString(event.getLevel())));
    }

    @SubscribeEvent
    public static void onNodeDeath(NodeEvent.PlayerDied event) {
        Journal.fire(event.getServer(), "node_player_died", Map.of("level", Integer.toString(event.getLevel())));
    }

    @SubscribeEvent
    public static void onCleansed(NodeEvent.Cleansed event) {
        CLEANSED_LEVELS.put(event.getNode().immutable(), event.getLevel());
        Journal.fire(event.getServer(), "node_cleansed", Map.of("level", Integer.toString(event.getLevel())));
    }

    @SubscribeEvent
    public static void onAbsorbed(BiomeAbsorbedEvent event) {
        MinecraftServer server = event.getServer();
        Integer level = CLEANSED_LEVELS.remove(event.getNode());
        // Поглощение отладочной командой идёт без зачистки — тогда берём текущий уровень узлов мира.
        Journal.nodeAbsorbed(server, level != null ? level : WorldProgress.get(server).nodeTier());
    }

    // --- Ванильные триггеры ---

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().getServer() != null && isTable(event.getCrafting())) {
            Journal.fire(event.getEntity().getServer(), "artifact_table_built");
        }
    }

    @SubscribeEvent
    public static void onPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ARTIFACT_TABLE.equals(BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()))) {
            Journal.fire(player.getServer(), "artifact_table_built");
        }
    }

    /** Первая ночь позади: в мире наступил второй день (сном или ожиданием). */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 == 0 && server.overworld().getDayTime() >= 24000L) {
            Journal.fire(server, "first_dawn");
        }
    }

    /** Раз в секунду: предметы в инвентаре и биом под ногами для ещё не полученных записей. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        JournalData data = JournalData.get(server);
        for (JournalDef def : JournalDefs.all()) {
            if (data.done.contains(def.id())) {
                continue;
            }
            if (def.type().equals("item_obtained")) {
                String id = def.params().get("item");
                ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
                Item item = rl == null ? null : BuiltInRegistries.ITEM.get(rl);
                if (item != null && item != Items.AIR && player.getInventory().hasAnyMatching(stack -> stack.is(item))) {
                    Journal.fire(server, "item_obtained", Map.of("item", id));
                }
            } else if (def.type().equals("biome_entered")) {
                String tag = def.params().get("biome_tag");
                ResourceLocation rl = tag == null ? null : ResourceLocation.tryParse(tag);
                if (rl != null && player.level().getBiome(player.blockPosition()).is(TagKey.create(Registries.BIOME, rl))) {
                    Journal.fire(server, "biome_entered", Map.of("biome_tag", tag));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CLEANSED_LEVELS.clear();
    }

    private static boolean isTable(ItemStack stack) {
        return ARTIFACT_TABLE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
