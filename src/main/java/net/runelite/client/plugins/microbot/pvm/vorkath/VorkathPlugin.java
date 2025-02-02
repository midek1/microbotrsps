package net.runelite.client.plugins.microbot.pvm.vorkath;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Vorkath",
        description = "requires inventory setup",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class VorkathPlugin extends Plugin {

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private VorkathOverlay vorkathOverlay;

    @Inject
    VorkathScript vorkathScript;

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(vorkathOverlay);
        }
        vorkathScript.run();
    }

    protected void shutDown() {
        vorkathScript.shutdown();
        overlayManager.remove(vorkathOverlay);
    }

}
