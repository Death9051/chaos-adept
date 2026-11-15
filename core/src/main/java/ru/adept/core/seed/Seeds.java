package ru.adept.core.seed;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.SeedEvent;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;

/**
 * Выдача и сохранность зачатка: кладётся в инвентарь при первом входе (один раз на игрока — отметка
 * {@code seed/given} в прогрессе, переживает смерть), при смерти не выпадает, а переходит к возрождённому игроку.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class Seeds {
    public static final String GIVEN = "seed/given";
    private static final String KEPT = "adept_kept_seeds";

    private Seeds() {
    }

    public static ItemStack stack() {
        return new ItemStack(ModItems.SEED.get());
    }

    public static boolean has(Player player) {
        return player.getInventory().countItem(ModItems.SEED.get()) > 0;
    }

    /** Первый вход: выдать зачаток, если этот игрок его ещё не получал. Вернёт true, если выдан. */
    public static boolean grantFirst(Player player) {
        ArtifactProgress p = player.getData(ModRegistries.ARTIFACT_PROGRESS);
        if (p.step(GIVEN) > 0) {
            return false;
        }
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(GIVEN, 1));
        give(player, SeedEvent.FIRST_JOIN);
        return true;
    }

    /** Положить зачаток в инвентарь (не влез — под ноги) и сообщить сюжету. */
    public static void give(Player player, String source) {
        ItemStack seed = stack();
        if (!player.getInventory().add(seed)) {
            player.drop(seed, false);
        }
        if (player.getServer() != null) {
            AdeptEvents.post(new SeedEvent.Received(player, source));
        }
    }

    /** Перед смертью: убрать зачатки из инвентаря и запомнить число (чтобы не выпали). */
    public static int stash(Player player) {
        Inventory inv = player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.SEED.get())) {
                count += s.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (count > 0) {
            CompoundTag d = player.getPersistentData();
            d.putInt(KEPT, d.getInt(KEPT) + count);
        }
        return count;
    }

    /** После возрождения: вернуть запомненные зачатки новому игроку. */
    public static int restore(Player original, Player respawned) {
        int count = original.getPersistentData().getInt(KEPT);
        original.getPersistentData().remove(KEPT);
        for (int i = 0; i < count; i++) {
            ItemStack seed = stack();
            if (!respawned.getInventory().add(seed)) {
                respawned.drop(seed, false);
            }
        }
        return count;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide) {
            grantFirst(event.getEntity());
        }
    }

    /** Раньше выпадения вещей: зачаток не должен оказаться на земле. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof Player player && !player.level().isClientSide) {
            stash(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            if (event.getEntity().getInventory().countItem(ModItems.SEED.get()) > 0) {
                // Правило «сохранять инвентарь» уже перенесло вещи: зачатки вернулись сами.
                event.getOriginal().getPersistentData().remove(KEPT);
                return;
            }
            restore(event.getOriginal(), event.getEntity());
        }
    }
}
