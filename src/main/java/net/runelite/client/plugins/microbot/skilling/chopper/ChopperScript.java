package net.runelite.client.plugins.microbot.skilling.chopper;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


public class ChopperScript extends Script {

    private static final WorldPoint MAHOGANY_POINT = new WorldPoint(2214, 3373, 0);
    private static final WorldPoint REGULAR_POINT = new WorldPoint(2260, 3362, 0);

    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Rs2Player.isAnimating(1200) || Rs2Player.isWalking() || Microbot.pauseAllScripts)
                    return;

                if (Rs2Inventory.isFull())
                    sleepUntil((() -> !Rs2Inventory.hasUnNotedItem("logs", false)), () -> {
                        Rs2Inventory.use(Rs2Inventory.getUnNotedItem("logs", false));
                        Rs2GameObject.interact(Rs2GameObject.findObjectById(65466));
                        Rs2Inventory.waitForInventoryChanges(1000);
                    }, 5000, 1000);

                int woodcuttingLevel = Rs2Player.getBoostedSkillLevel(Skill.WOODCUTTING);

                if (woodcuttingLevel >= 50 && Microbot.donatorAmount >= 50) {
                    chopMahogany();
                } else {
                    chopNormals();
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void chopMahogany() {
        if (Rs2Player.getWorldLocation().equals(MAHOGANY_POINT)) {
            Rs2GameObject.interact(Rs2GameObject.findObjectById(36688, 2216));
            sleep(1000);
            return;
        }

        Rs2GameObject.handleNexus("Bosses", "Olympian raids");
        if (Rs2GameObject.interact(Rs2GameObject.findObjectById(65411, 2212))) {
            sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(MAHOGANY_POINT) <= 3, 20000);
            Rs2Walker.walkTo(MAHOGANY_POINT, 0);
            Rs2Player.waitForWalking();
        }
    }

    private void chopNormals() {
        if (Rs2Player.isNearArea(REGULAR_POINT, 20)) {
            List<GameObject> trees = Rs2GameObject.getGameObjectsWithinDistance(15, new WorldPoint(2260, 3362, 0));
            Optional<GameObject> tree = trees.stream().filter(gameObject -> gameObject.getId() == (Rs2Player.getBoostedSkillLevel(Skill.WOODCUTTING) < 30 ? 36684 : 10833)).findFirst();
            if (tree.isPresent()) {
                Rs2GameObject.interact(tree.get(), "Chop down");
                sleep(1000);
            }
            return;
        }

        Rs2GameObject.handleNexus("Skilling", "Woodcutting");
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}