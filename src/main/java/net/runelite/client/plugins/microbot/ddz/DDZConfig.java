package net.runelite.client.plugins.microbot.ddz;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ddzconfig")
public interface DDZConfig extends Config {
    @ConfigItem(
            keyName = "gatheringAction",
            name = "Gathering Action",
            description = "Choose a resource to gather",
            position = 0
    )
    default GatheringAction chosenResource()
    {
        return GatheringAction.FLETCHING;
    }
}
