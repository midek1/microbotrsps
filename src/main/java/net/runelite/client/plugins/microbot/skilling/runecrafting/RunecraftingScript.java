package net.runelite.client.plugins.microbot.skilling.runecrafting;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.concurrent.TimeUnit;

public class RunecraftingScript extends Script {

    private static final WorldPoint HOME = new WorldPoint(2239, 3323, 0);
    private static final WorldPoint ALTAR = new WorldPoint(3052, 5578, 0);

    public boolean run(RunecraftingConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;

                if (!atHome())
                    teleportHome();

                Rs2Bank.openBank();
                Rs2Bank.depositAll();
                Rs2Bank.withdrawAll("Dense essence block");
                Rs2Bank.closeBank();

                Rs2GameObject.interact(65527, "Teleport");
                sleepUntil(() -> Rs2Widget.hasWidget("Favourites"));

                if (!Rs2Widget.hasWidget("Runecrafting")) {
                    Rs2Widget.clickWidget("Favourites", true);
                    sleepUntil(() -> Rs2Widget.hasWidget("Runecrafting"));
                }

                Rs2Widget.clickWidget("Runecrafting");
                sleepUntil(this::atAltar);

                Rs2GameObject.interact(29631, "Bind-random");
                Rs2Player.waitForXpDrop(Skill.RUNECRAFT);

                teleportHome();

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void teleportHome() {
        Rs2Tab.switchToMagicTab();
        sleepUntil(() -> Rs2Tab.getCurrentTab().equals(InterfaceTab.MAGIC));
        Rs2Widget.clickWidget("Home teleport");
        sleepUntil(this::atHome);
    }

    private boolean atHome() {
        return Rs2Player.getWorldLocation().distanceTo(HOME) < 10;
    }

    private boolean atAltar() {
        return Rs2Player.getWorldLocation().distanceTo(ALTAR) < 10;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}