package net.runelite.client.plugins.microbot.pvm.corp;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.NpcID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Corp",
        description = "August Corp Plugin",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class CorpPlugin extends Plugin {
    @Inject
    private CorpConfig config;

    @Provides
    CorpConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CorpConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private CorpOverlay corpOverlay;

    @Inject
    CorpScript corpScript;

    @Subscribe
    private void onNpcSpawned(NpcSpawned event) {
        if (event.getNpc().getId() == NpcID.CORPOREAL_BEAST) {
            CorpScript.corp = event.getNpc();
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned event) {
        if (event.getNpc().getId() == NpcID.CORPOREAL_BEAST) {
            CorpScript.corp = null;
            CorpScript.state = CorpState.LOOTING;
        }
    }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        if (event.getType().equals(ChatMessageType.GAMEMESSAGE)
                && event.getMessage().contains("Oh dear, you are dead!"))
            CorpScript.state = CorpState.BANKING;
    }


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(corpOverlay);
        }
        corpScript.run(config);
    }

    protected void shutDown() {
        corpScript.shutdown();
        overlayManager.remove(corpOverlay);
    }

}
