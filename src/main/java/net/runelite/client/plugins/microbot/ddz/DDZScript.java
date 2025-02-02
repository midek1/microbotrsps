package net.runelite.client.plugins.microbot.ddz;

import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;


public class DDZScript extends Script {

    public boolean canGather = true;
    public boolean canCook = true;
    public boolean canSlay = true;

    public boolean run(DDZConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || !inDDZ() || Microbot.pauseAllScripts) return;

                NPC seren = Rs2Npc.getNpc(15171);
                if (seren != null) Rs2Npc.interact(seren);

                if (Rs2Player.isAnimating()) return;

                if (canGather)
                    gatherResources(config);
                else if (canCook)
                    cookFood();
                else
                    shutdown();

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void gatherResources(DDZConfig config) {
        if (Rs2Inventory.getUnNotedItem(config.chosenResource().getRequiredItem(), false) == null)
            return;

        if (Rs2Inventory.isFull()) {
            if (Rs2Bank.openBank(Rs2GameObject.findObjectById(65466))) {
                Rs2Bank.depositAll("Dark crab", true);
                Rs2Bank.closeBank();
            }
        }

        Rs2GameObject.interact(config.chosenResource().getGatheringObjectId());
        sleep(1000);
        Rs2Player.waitForAnimation(1200);
    }

    private void cookFood() {
        if (Rs2Inventory.use(Rs2Inventory.get("Raw dark crab", "Raw shark", "Raw Lobster"))) {
            Rs2GameObject.interact(Rs2GameObject.findObjectById(65391, 2173));
            Rs2Widget.sleepUntilHasWidget("How many would you like to create?");
            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            sleep(1000);
            Rs2Player.waitForAnimation(1200);
        } else {
            if (Rs2Bank.openBank(Rs2GameObject.findObjectById(65466, 2179))) {
                Rs2Bank.setWithdrawAsNote();
                String[] rawFish = {"Raw dark crab", "Raw shark", "Raw lobster"};
                for (String fish : rawFish) {
                    Rs2Bank.withdrawAll(fish);
                }
                Rs2Bank.closeBank();
                sleep(1200);
                if (!Rs2Inventory.contains(rawFish)) {
                    Microbot.log("no raw fish");
                    canCook = false;
                }
            }
        }

    }

    private boolean inDDZ() {
        return Rs2Player.isNearArea(new WorldPoint(2167, 3328, 0), 16);
    }

    @Override
    public void shutdown() {
        this.canCook = true;
        this.canGather = true;
        super.shutdown();
    }
}