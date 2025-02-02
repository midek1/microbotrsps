package net.runelite.client.plugins.microbot.pvm.magearena2;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Mage Arena 2",
        description = "August MA2 Plugin",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class MageArena2Plugin extends Plugin {
    @Inject
    private MageArena2Config config;

    @Provides
    MageArena2Config provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MageArena2Config.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private MageArena2Overlay mageArena2Overlay;

    @Inject
    MageArena2Script mageArena2Script;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(mageArena2Overlay);
        }
        mageArena2Script.run(config);
    }

    protected void shutDown() {
        mageArena2Script.shutdown();
        overlayManager.remove(mageArena2Overlay);
    }

    @Subscribe
    private void onAnimationChanged(AnimationChanged event) {
        int animationId = event.getActor().getAnimation();

        switch (animationId) {
            case 7853: //sara melee, so pray mage
                MageArena2Script.toPray = Rs2PrayerEnum.PROTECT_MAGIC;
                break;
            case 7965: //sara mage, so pray melee
                MageArena2Script.toPray = Rs2PrayerEnum.PROTECT_MELEE;
                break;
            case 7963: //range, which we can react to
                MageArena2Script.toPray = Rs2PrayerEnum.PROTECT_RANGE;
                break;
        }
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned event) {
        if (event.getNpc().getId() == config.encounter().getNpcId()) {
            MageArena2Script.state = MageArena2Script.State.FIGHTING;
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned event) {
        if (event.getNpc().getId() == config.encounter().getNpcId()) {
            MageArena2Script.state = MageArena2Script.State.LOOTING;
            MageArena2Script.toPray = Rs2PrayerEnum.PROTECT_MELEE;
        }
    }

    @Subscribe
    private void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        if (event.getGraphicsObject().getId() == 2523) {
            MageArena2Script.needsMove = true;
        }
    }

}
