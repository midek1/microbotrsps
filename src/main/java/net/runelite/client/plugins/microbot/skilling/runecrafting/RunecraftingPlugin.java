package net.runelite.client.plugins.microbot.skilling.runecrafting;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Runecrafting",
        description = "Requires RC as favourite teleport at nexus",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class RunecraftingPlugin extends Plugin {
    @Inject
    private RunecraftingConfig config;

    @Provides
    RunecraftingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RunecraftingConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private RunecraftingOverlay runecraftingOverlay;

    @Inject
    RunecraftingScript runecraftingScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(runecraftingOverlay);
        }
        runecraftingScript.run(config);
    }

    protected void shutDown() {
        runecraftingScript.shutdown();
        overlayManager.remove(runecraftingOverlay);
    }

}
