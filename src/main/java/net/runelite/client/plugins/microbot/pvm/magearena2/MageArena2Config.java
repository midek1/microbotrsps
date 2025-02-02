package net.runelite.client.plugins.microbot.pvm.magearena2;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface MageArena2Config extends Config {
    @ConfigItem(
            keyName = "ma2boss",
            name = "Boss",
            description = "Select an encounter",
            position = 0
    )
    default MageArena2Bosses encounter()
    {
        return MageArena2Bosses.SARADOMIN;
    }
}
