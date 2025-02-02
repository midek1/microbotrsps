package net.runelite.client.plugins.microbot.ddz;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "DDZ",
        description = "Microbot DDZ plugin",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class DDZPlugin extends Plugin {
    @Inject
    private DDZConfig config;

    @Provides
    DDZConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DDZConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private DDZOverlay DDZOverlay;

    @Inject
    DDZScript DDZScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(DDZOverlay);
        }
        DDZScript.run(config);
    }

    protected void shutDown() {
        DDZScript.shutdown();
        overlayManager.remove(DDZOverlay);
    }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        if (!event.getType().equals(ChatMessageType.CONSOLE)) return;

        String message = event.getMessage();
        if (message.equalsIgnoreCase("You have reached your limit of 1000 gathers here today."))
            DDZScript.canGather = false;
        else if (message.equalsIgnoreCase("You've reached the limit of 1000 uses of the fire for the day."))
            DDZScript.canCook = false;
    }

}
