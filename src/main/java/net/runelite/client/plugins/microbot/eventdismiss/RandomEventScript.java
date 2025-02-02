package net.runelite.client.plugins.microbot.eventdismiss;

import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.concurrent.TimeUnit;

public class RandomEventScript extends Script {
    public static double version = 1.0;
    public static boolean shootingStar = false;

    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                NPC npc = Rs2Npc.getRandomEventNPC();
                if (npc != null) {
                    Microbot.pauseAllScripts = true;
                    talkToNPC(npc);
                    return;
                }

                if (shootingStar) {
                    Microbot.pauseAllScripts = true;
                    handleStar();
                    return;
                }

                Microbot.pauseAllScripts = false;

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private void handleStar() {
        GameObject star = Rs2GameObject.get("Fallen star");
        if (star == null || Rs2Player.distanceTo(star.getWorldLocation()) > 10) {
            Rs2Keyboard.typeString("::star");
            Rs2Keyboard.enter();
            return;
        }

        if (!Rs2Player.isAnimating(2000)) {
            Rs2GameObject.interact(star, "Mine");
            sleep(1000);
        }
    }

    private void talkToNPC(NPC npc) {
        // Interact with NPC to claim lamp
        Rs2Npc.interact(npc, "Talk-to");
        sleep(900);
        Rs2Dialogue.clickContinue();
        sleep(700, 900);
        Rs2Dialogue.clickContinue();
        Microbot.pauseAllScripts = false;
    }
}
