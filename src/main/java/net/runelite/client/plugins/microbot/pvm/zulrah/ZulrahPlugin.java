package net.runelite.client.plugins.microbot.pvm.zulrah;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AnimationID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Zulrah",
        description = "August Zulrah Plugin",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ZulrahPlugin extends Plugin {
    @Inject
    private ZulrahConfig config;

    @Provides
    ZulrahConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ZulrahConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ZulrahOverlay zulrahOverlay;

    @Inject
    ZulrahScript zulrahScript;

    @Subscribe
    private void onAnimationChanged(AnimationChanged event) {
        String name = event.getActor().getName();
        if (name == null || !event.getActor().getName().equalsIgnoreCase("Zulrah")) return;

        //Melee phase stare attack
        if (event.getActor().getAnimation() == 5806)
            ZulrahScript.needsMove = true;
    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event) {
        int projectileId = event.getProjectile().getId();
        if (projectileId == 1044) {
            ZulrahScript.toPray = Rs2PrayerEnum.PROTECT_RANGE;
        } else if (projectileId == 1046) {
            ZulrahScript.toPray = Rs2PrayerEnum.PROTECT_MAGIC;
        }
    }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        if (!event.getType().equals(ChatMessageType.CONSOLE) && !event.getType().equals(ChatMessageType.GAMEMESSAGE)) return;

        String message = event.getMessage();
        if (message.contains("You dealt") && message.contains("of the damage to") && message.contains("Zulrah"))
            ZulrahScript.state = ZulrahState.LOOTING;
        else if (message.contains("Oh dead, you are dead!"))
            ZulrahScript.state = ZulrahState.STARTING;
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(zulrahOverlay);
        }
        zulrahScript.run(config);
    }

    protected void shutDown() {
        zulrahScript.shutdown();
        overlayManager.remove(zulrahOverlay);
    }

}
