package net.runelite.client.plugins.microbot.eventdismiss;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "August Randoms",
        description = "Interacts with randoms",
        tags = {"random", "events", "microbot"}
)
@Slf4j
public class RandomEventPlugin extends Plugin {
    @Inject
    RandomEventScript randomEventScript;

    @Inject
    RandomEventConfig config;

    @Provides
    RandomEventConfig provideConfig(ConfigManager configManager) { return configManager.getConfig(RandomEventConfig.class); }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        if (!event.getType().equals(ChatMessageType.GAMEMESSAGE)) return;

        String message = event.getMessage();
        if (message.contains("A shooting star has spawned")) {
            if (!config.shootingStar()) return;

            if (Rs2Inventory.hasItem("pickaxe", false) || Rs2Equipment.isWearing("pickaxe", false))
                RandomEventScript.shootingStar = true;
            else
                Microbot.log("No pickaxe, skipping star");
        } else if (message.contains("The shooting star has been fully depleted!")) {
            RandomEventScript.shootingStar = false;
        }
    }

    @Override
    protected void startUp() throws AWTException {
        randomEventScript.run();
    }

    protected void shutDown() {
        randomEventScript.shutdown();
    }
}
