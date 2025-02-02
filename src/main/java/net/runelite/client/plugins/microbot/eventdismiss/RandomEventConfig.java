package net.runelite.client.plugins.microbot.eventdismiss;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("augustrandoms")
public interface RandomEventConfig extends Config {
    @ConfigItem(
            keyName = "shootingStar",
            name = "Shooting star?",
            description = "Will break some plugins",
            position = 0
    )
    default boolean shootingStar() { return false; }
}
