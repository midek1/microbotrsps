package net.runelite.client.plugins.microbot.ddz;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GatheringAction {
    FLETCHING(15970, "Axe"),
    FISHING(65393, "Lobster pot"),
    MINING(65392, "Pickaxe");

    private final int gatheringObjectId;
    private final String requiredItem;
}
