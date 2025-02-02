package net.runelite.client.plugins.microbot.skilling.gemcrafting;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "GemCrafter",
        description = "Start with noted gems and a chisel at GE",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class GemCraftingPlugin extends Plugin {

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private GemCraftingOverlay gemCraftingOverlay;

    @Inject
    GemCraftingScript gemCraftingScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(gemCraftingOverlay);
        }
        gemCraftingScript.run();
    }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        if (!event.getType().equals(ChatMessageType.CONSOLE)) return;

        String message = event.getMessage();
        if (message.contains("Your Crafting prestige rank is now")
                || message.contains("Congratulations, you just advanced"))
            GemCraftingScript.gem = null;
    }

    protected void shutDown() {
        gemCraftingScript.shutdown();
        overlayManager.remove(gemCraftingOverlay);
    }

}
