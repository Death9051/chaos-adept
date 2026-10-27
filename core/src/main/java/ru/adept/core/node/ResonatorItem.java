package ru.adept.core.node;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.adept.core.config.BalanceConfig;

import java.util.Optional;

/**
 * Резонатор: ищет ближайший незачищенный узел и сообщает уровень, расстояние и направление.
 * В срезе 1 — без прокачки (антенна и анализ придут позже).
 */
public class ResonatorItem extends Item {
    private static final String[] DIRECTIONS = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};

    public ResonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Optional<NodeSearch.Found> found = NodeSearch.nearest(server, player.blockPosition(), BalanceConfig.RESONATOR_RANGE.get());
            if (found.isPresent()) {
                NodeSearch.Found f = found.get();
                BlockPos target = f.column();
                double dx = target.getX() + 0.5 - player.getX();
                double dz = target.getZ() + 0.5 - player.getZ();
                int distance = (int) Math.sqrt(dx * dx + dz * dz);
                player.displayClientMessage(Component.translatable("adept_core.resonator.found", f.level(), distance,
                        Component.translatable("adept_core.direction." + direction(dx, dz))), true);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f,
                        0.6f + 0.8f * (1.0f - Math.min(1.0f, distance / (float) BalanceConfig.RESONATOR_RANGE.get())));
            } else {
                player.displayClientMessage(Component.translatable("adept_core.resonator.none"), true);
            }
            player.getCooldowns().addCooldown(this, BalanceConfig.RESONATOR_COOLDOWN_TICKS.get());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Направление по сторонам света (в Minecraft юг — +Z). */
    public static String direction(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        int index = (int) Math.floorMod(Math.round(angle / 45.0), 8);
        return DIRECTIONS[index];
    }
}
