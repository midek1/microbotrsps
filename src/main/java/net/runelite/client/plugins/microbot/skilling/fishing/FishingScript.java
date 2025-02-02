package net.runelite.client.plugins.microbot.skilling.fishing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class FishingScript extends Script {

    private static final Integer[] FISHING_TOOLS = {
            ItemID.SMALL_FISHING_NET, ItemID.LOBSTER_POT, ItemID.HARPOON,
            62665, 62668 //Pearl pot/harpoon
    };

    public boolean run(FishingConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Rs2Player.isAnimating(1800) || Rs2Player.isMoving() || Microbot.pauseAllScripts) return;

                FishingSpots spot = FishingSpots.getSpot();

                if (Rs2Inventory.isFull()) {
                    Rs2Bank.openBank(Rs2GameObject.findObjectById(65466));
                    Rs2Bank.depositAllExcept(FISHING_TOOLS);
                    Rs2Bank.closeBank();
                    return;
                }

                NPC fishingSpot = Rs2Npc.getNpc(spot.getSpotNpcId());

                if (fishingSpot == null)
                    return;

                if (spot == FishingSpots.OLYMPIAN) {
                    Rs2Npc.interact(Rs2Npc.getNpcs()
                            .filter(npc -> npc.getId() == spot.getSpotNpcId() && npc.getWorldLocation().getX() == 2262).findFirst().get());
                } else {
                    Rs2Npc.interact(fishingSpot, spot.getSpotAction());
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Getter
    @RequiredArgsConstructor
    enum FishingSpots {
        OLYMPIAN(80, 15167, "Fish"),
        HARPOON(76, 1520, "Harpoon"),
        LOBSTER_POT(40, 1519, "Cage"),
        SMALL_NET(1, 1517, "Small Net");

        private final int requiredLevel;
        private final int spotNpcId;
        private final String spotAction;

        public static FishingSpots getSpot() {
            int fishingLevel = Rs2Player.getBoostedSkillLevel(Skill.FISHING);
            for (FishingSpots spot : values()) {
                if (fishingLevel >= spot.getRequiredLevel()) {
                    return spot;
                }
            }
            return SMALL_NET;
        }
    }
}