package net.runelite.client.plugins.microbot.pvm.slayer;

import com.google.inject.Provides;
import custom.UpdateSlayerInfoScript;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.playerassist.PlayerAssistConfig;
import net.runelite.client.plugins.microbot.playerassist.combat.BuryScatterScript;
import net.runelite.client.plugins.microbot.playerassist.loot.LootScript;
import net.runelite.client.plugins.microbot.playerassist.skill.AttackStyleScript;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "August Slayer",
        description = "Slays shit simply",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class SlayerPlugin extends Plugin {

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private SlayerOverlay slayerOverlay;
    @Inject
    private PlayerAssistConfig config;

    @Inject
    SlayerScript slayerScript;
    @Inject
    AttackStyleScript attackStyleScript;
    @Inject
    BuryScatterScript buryScatterScript;
    @Inject
    LootScript lootScript;

    @Provides
    PlayerAssistConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PlayerAssistConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(slayerOverlay);
        }
        slayerScript.run();
        lootScript.run(config);
        attackStyleScript.run(config);
        buryScatterScript.run(config);
    }

    protected void shutDown() {
        slayerScript.shutdown();
        lootScript.shutdown();
        attackStyleScript.shutdown();
        buryScatterScript.shutdown();
        overlayManager.remove(slayerOverlay);
    }

}
