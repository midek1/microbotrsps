package net.runelite.client.plugins.microbot.skilling.thieving;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.ObjectID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.concurrent.TimeUnit;


public class ThievingScript extends Script {

    private static final Integer[] THIEVING_GARBAGE = {
            ItemID.CAKE, ItemID.SILK, ItemID.FUR, ItemID.SILVER_ORE, ItemID.SILVER_BAR, ItemID.SPICE };

    private static final WorldArea THIEVING_AREA = new WorldArea(2265, 3302, 13, 10, 0);
    private static final WorldArea DONOR_THIEVING_AREA = new WorldArea(2279, 3297, 4, 12, 0);
    private static final WorldArea BEEHIVE_AREA = new WorldArea(2255, 3360, 10, 9, 0);

    public boolean reset = true;
    private Stall stall = Stall.BAKERS_STALL;

    public boolean run(ThievingConfig config) {
        reset = true;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Rs2Player.isMoving() || Microbot.pauseAllScripts) return;

                if (reset) reset();

                Rs2Inventory.dropAll(THIEVING_GARBAGE);

                NPC seren = Rs2Npc.getNpc(15171);
                if (seren != null) Rs2Npc.interact(seren);

                if (!Rs2Player.isAnimating(5000)) {
                    Rs2GameObject.interact(Rs2GameObject.findObjectById(stall.getStallObjectId(), stall.getObjectX()), "Steal-from");
                    Rs2Player.waitForAnimation();
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void reset() {
        stall = Stall.getHighestAccessibleStall();
        if (!stall.getAreaRequirement().contains(Rs2Player.getWorldLocation()))
            moveToLocation(stall.getAreaRequirement());

        Rs2GameObject.interact(Rs2GameObject.findObjectById(stall.getStallObjectId(), stall.getObjectX()), "Steal-from");
        reset = false;
    }

    private void moveToLocation(WorldArea thievingLocation) {
        Rs2Tab.switchToMagicTab();
        sleepUntil(() -> Rs2Tab.getCurrentTab().equals(InterfaceTab.MAGIC));
        Rs2Widget.clickWidget("Home Teleport");
        Rs2Player.waitForAnimation();
        Rs2GameObject.interact(Rs2GameObject.findObjectById(65527, 2236), "Teleport");
        sleepUntil(() -> Rs2Widget.hasWidget("Favourites"), 10000);

        if (!Rs2Widget.hasWidget("Skilling Teleports")) {
            Rs2Widget.clickWidget("Skilling", true);
            Rs2Widget.sleepUntilHasWidget("Skilling Teleports");
        }

        Rs2Widget.clickWidget(thievingLocation == BEEHIVE_AREA ? "Woodcutting" : "Thieving");
        Rs2Player.waitForAnimation();
        sleepUntil(() -> !Rs2Player.isWalking());

        if (thievingLocation == DONOR_THIEVING_AREA) {
            Rs2GameObject.interact(Rs2GameObject.findObject(65419, new WorldPoint(2279, 3304, 0)));
            Rs2Player.waitForWalking();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Getter
    @RequiredArgsConstructor
    public enum Stall {

        COIN_STALL_T2(85, 65522, 2281, DONOR_THIEVING_AREA),
        BEEHIVE(80, 65476, 2256, BEEHIVE_AREA),
        COIN_STALL_T1(65, 65523, 2266, THIEVING_AREA),
        SPICE_STALL(55, ObjectID.SPICE_STALL_11733, 2273, THIEVING_AREA),
        SILVER_STALL(45, ObjectID.SILVER_STALL_11734, 2275, THIEVING_AREA),
        FUR_STALL(35, ObjectID.FUR_STALL_11732, 2268, THIEVING_AREA),
        SILK_STALL(20, ObjectID.SILK_STALL_11729, 2271, THIEVING_AREA),
        BAKERS_STALL(1, ObjectID.BAKERS_STALL_11730, 2269, THIEVING_AREA);

        private final int levelRequirement;
        private final int stallObjectId;
        private final int objectX;
        private final WorldArea areaRequirement;

        public static Stall getHighestAccessibleStall() {
            int thievingLevel = Rs2Player.getBoostedSkillLevel(Skill.THIEVING);
            for (Stall stall : values()) {
                if (thievingLevel >= stall.getLevelRequirement()) {
                    return stall;
                }
            }
            return BAKERS_STALL;
        }
    }
}