package net.runelite.client.plugins.microbot.pvm.magearena2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.NpcID;
import net.runelite.api.coords.LocalPoint;

@Getter
@RequiredArgsConstructor
public enum MageArena2Bosses {
    SARADOMIN(2873, NpcID.JUSTICIAR_ZACHARIAH_7858, 45, 80),
    ZAMORAK(2874, NpcID.PORAZDIR_7860, 58, 81),
    GUTHIX(2875, NpcID.DERWEN_7859, 52, 83);

    private final int statueId;
    private final int NpcId;
    private final int standX;
    private final int standY;
}
