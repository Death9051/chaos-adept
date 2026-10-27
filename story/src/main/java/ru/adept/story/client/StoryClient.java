package ru.adept.story.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;
import ru.adept.story.AdeptStory;

/** Клиент «Сюжета»: клавиша журнала, кнопка в инвентаре, слой с субтитрами и уведомлениями. */
public final class StoryClient {
    public static final KeyMapping JOURNAL_KEY = new KeyMapping("key.adept_story.journal",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.adept_story");

    private static Button inventoryButton;

    private StoryClient() {
    }

    /** Регистрация на шине мода. */
    @EventBusSubscriber(modid = AdeptStory.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(JOURNAL_KEY);
        }

        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(AdeptStory.MOD_ID, "story_overlay"), StoryOverlay::render);
        }
    }

    /** События игры на клиенте. */
    @EventBusSubscriber(modid = AdeptStory.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            StoryOverlay.tick();
            while (JOURNAL_KEY.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new JournalScreen());
                }
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            JournalClient.clear();
            StoryOverlay.clear();
        }

        /** Кнопка журнала над окном инвентаря, справа. */
        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof InventoryScreen screen) {
                inventoryButton = Button.builder(Component.literal("Ж"), b -> Minecraft.getInstance().setScreen(new JournalScreen()))
                        .bounds(buttonX(screen), buttonY(screen), 20, 18)
                        .tooltip(Tooltip.create(Component.translatable("adept_story.journal.button")))
                        .build();
                event.addListener(inventoryButton);
            }
        }

        /** Книга рецептов сдвигает окно инвентаря — двигаем кнопку вслед. */
        @SubscribeEvent
        public static void onScreenRender(ScreenEvent.Render.Pre event) {
            if (event.getScreen() instanceof InventoryScreen screen && inventoryButton != null) {
                inventoryButton.setX(buttonX(screen));
                inventoryButton.setY(buttonY(screen));
            }
        }

        private static int buttonX(InventoryScreen screen) {
            return screen.getGuiLeft() + screen.getXSize() - 20;
        }

        private static int buttonY(InventoryScreen screen) {
            return screen.getGuiTop() - 20;
        }
    }
}
