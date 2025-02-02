package net.runelite.client.plugins.microbot.skilling.fishing;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Fishing",
        description = "August fishing plugin, start with tools in inventory",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class FishingPlugin extends Plugin {
    @Inject
    private FishingConfig config;

    @Provides
    FishingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FishingConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private FishingOverlay fishingOverlay;

    @Inject
    FishingScript fishingScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(fishingOverlay);
        }
        fishingScript.run(config);
    }

    protected void shutDown() {
        fishingScript.shutdown();
        overlayManager.remove(fishingOverlay);
    }

}
