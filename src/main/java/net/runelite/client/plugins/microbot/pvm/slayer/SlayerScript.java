package net.runelite.client.plugins.microbot.pvm.slayer;

import lombok.Getter;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;


public class SlayerScript extends Script {

    @Getter
    private static SlayerState state = SlayerState.GETTING_TASK;

    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;

                switch (state) {
                    case FIGHTING:
                        handleFighting();
                        break;
                    case GETTING_TASK:
                        getTask();
                        break;
                    case TRAVELLING:
                        String category = "Slayer";
                        if (Microbot.slayerTask.equalsIgnoreCase("Hobgoblins") || Microbot.slayerTask.equalsIgnoreCase("Black Knights")) {
                            category = "Training";
                        }
                        if (Microbot.slayerTask.equalsIgnoreCase("Rock crab")) {
                            Rs2GameObject.handleTeleportInterface("Training", "Rock Crabs");
                        } else {
                            Rs2GameObject.handleTeleportInterface(category, Microbot.slayerTask);
                        }
                        sleep(2000);
                        state = SlayerState.FIGHTING;
                        break;
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean needsTask() {
        return Microbot.slayerTask == null || Microbot.slayerTask.equalsIgnoreCase("");
    }

    private boolean isHome() {
        return Rs2Player.isNearArea(new WorldPoint(2229, 3319, 0), 20);
    }

    private void handleFighting() {
        if (needsTask()) state = SlayerState.GETTING_TASK;

        if (Rs2Player.getHealthPercentage() == 0 || Rs2Player.isMoving()) return;

        String task = Microbot.slayerTask;
        if (task.equalsIgnoreCase("Ethereal Beings"))
            task = "Ahrim the Blighted";
        else if (task.equalsIgnoreCase("jellies"))
            task = "Jelly";

        if (task.endsWith("s")) {
            task = task.substring(0, task.length() - 1);
        }

        NPC taskNpc = Rs2Npc.getNpc(task, false);
        if (taskNpc == null) {
            state = SlayerState.TRAVELLING;
            return;
        }

        if (task.equalsIgnoreCase("Infernal mage")) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
        } else {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
        }

        Rs2Player.drinkPrayerPotionAt(10);

        if (Rs2Player.isInteracting() || Microbot.getClient().getLocalPlayer().getInteracting() != null)
            return;

        if (Rs2Npc.attack(taskNpc))
            sleep(1000);
    }

    private void getTask() {
        if (needsTask()) {
            if (!isHome())
                Rs2Magic.teleportHome();

            GameObject prayerAltar = Rs2GameObject.findObjectById(409, 2230);
            if (prayerAltar != null) {
                Rs2GameObject.interact(prayerAltar, "Pray-at");
                sleepUntil(() -> Rs2Player.getBoostedSkillLevel(Skill.PRAYER) >= Rs2Player.getRealSkillLevel(Skill.PRAYER));
            }

            Rs2Npc.interact(getSlayerMasterId(), "Assignment");
            sleep(800, 1200);
            Widget randomButton = Rs2Widget.searchChildren("Random", Rs2Widget.getWidget(219, 1), true);
            if (randomButton != null) {
                Rs2Keyboard.keyPress((char) KeyEvent.VK_2);
                sleep(1200);
            }
        }
        state = SlayerState.TRAVELLING;
    }

    private int getSlayerMasterId() {
        int slayerLevel = Rs2Player.getBoostedSkillLevel(Skill.SLAYER);
        if (slayerLevel >= 65) {
            return 6797;
        } else if (slayerLevel >= 45) {
            return 403;
        } else {
            return 13433;
        }
    }

    @Override
    public void shutdown() {
        state = SlayerState.GETTING_TASK;
        super.shutdown();
    }
}

enum SlayerState {
    FIGHTING, GETTING_TASK, TRAVELLING
}