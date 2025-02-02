package net.runelite.client.plugins.microbot.skilling.chopper;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Chopper",
        description = "Chops trees",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ChopperPlugin extends Plugin {

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ChopperOverlay chopperOverlay;

    @Inject
    ChopperScript chopperScript;

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(chopperOverlay);
        }
        chopperScript.run();
    }

    protected void shutDown() {
        chopperScript.shutdown();
        overlayManager.remove(chopperOverlay);
    }

}
