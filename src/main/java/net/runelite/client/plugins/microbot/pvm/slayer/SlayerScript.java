package net.runelite.client.plugins.microbot.pvm.slayer;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class SlayerScript extends Script {

    @Getter
    @Setter
    private static SlayerState state = SlayerState.GETTING_TASK;

    private static List<NPC> attackableNpcs = new ArrayList<>();

    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;

                if (Rs2Inventory.containsAll(ItemID.LOOP_HALF_OF_KEY, ItemID.TOOTH_HALF_OF_KEY))
                    Rs2Inventory.combine(ItemID.LOOP_HALF_OF_KEY, ItemID.TOOTH_HALF_OF_KEY);

                switch (state) {
                    case FIGHTING:
                        handleFighting();
                        break;
                    case GETTING_TASK:
                        getTask();
                        break;
                    case TRAVELLING:
                        handleTravel();
                        break;
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleFighting() {
        if (needsTask()) {
            Rs2Prayer.disableAllPrayers();
            state = SlayerState.GETTING_TASK;
        }

        if (Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS) == 0) {
            sleepUntil(this::isHome);
            state = SlayerState.TRAVELLING;
            return;
        }

        String taskNpc = npcNameFromTask();

        handlePrayer(taskNpc);
        handleTaskEquipment(taskNpc);
        Rs2Player.drinkPrayerPotionAt(10);

        if (Rs2Player.isInteracting() || Rs2Player.isMoving())
            return;

        attackableNpcs = Rs2Npc.getAttackableNpcs(false)
                .filter(npc -> npc.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= 30 && npc.getName().equalsIgnoreCase(taskNpc))
                .sorted(Comparator.comparing((NPC npc) -> npc.getInteracting() == Microbot.getClient().getLocalPlayer() ? 0 : 1)
                        .thenComparingInt(npc -> Rs2Player.getRs2WorldPoint().distanceToPath(npc.getWorldLocation())))
                .collect(Collectors.toList());

        if (!attackableNpcs.isEmpty()) {
            Rs2Npc.attack(attackableNpcs.stream().findFirst().orElse(null));
            sleep(2000);
            return;
        }

        if (!sleepUntil(Rs2Player::isInCombat, 30000))
            state = SlayerState.TRAVELLING;
    }

    private void handlePrayer(String npc) {
        switch (npc) {
            case "infernal mage":
            case "aberrant spectre":
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
                break;
            default:
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
                break;
        }
    }

    private void handleTaskEquipment(String npc) {
        if (npc.contains("Dragon")) {
            if (!Rs2Equipment.isWearing(1540) && Rs2Inventory.contains(1540)) {
                Rs2Inventory.equip(1540);
                Rs2Inventory.waitForInventoryChanges(1200);
                return;
            }
        }

        if (!Rs2Equipment.isWearing("defender", false) && Rs2Inventory.hasItem("defender", false)) {
            Rs2Inventory.equip(Rs2Inventory.get("defender", false).getName());
            Rs2Inventory.waitForInventoryChanges(1200);
        }
    }

    private String npcNameFromTask() {
        String task = Microbot.slayerTask;

        //handle edge cases and format task name to singular
        if (task.equalsIgnoreCase("Ethereal Beings")) task = "Dharok the Wretched";
        else if (task.equalsIgnoreCase("Jellies")) task = "Jelly";
        else if (task.equalsIgnoreCase("Giants")) task = "Cyclops";
        else if (task.equalsIgnoreCase("Spiders")) task = "Temple spider";
        else if (task.endsWith("s") && !task.equalsIgnoreCase("Cyclops")) task = task.substring(0, task.length() - 1);

        return task;
    }

    private void handleTravel() {

        String category = "Slayer";
        String task = Microbot.slayerTask;

        if (task.equalsIgnoreCase("Giants")) {
            handleCyclops();
            return;
        }

        if (task.equalsIgnoreCase("Hobgoblins") || task.equalsIgnoreCase("Black Knights")
                || task.equalsIgnoreCase("Rock crab"))
            category = "Training";

        if (task.equalsIgnoreCase("Rock crab"))
            task = "Rock Crabs";


        if (!Rs2GameObject.handleNexus(category, task)) return;

        if (task.equalsIgnoreCase("Iron dragons")) {
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            Rs2Walker.walkTo(new WorldPoint(playerLoc.getX(), playerLoc.getY() - 10, playerLoc.getPlane()));
            Rs2Player.waitForWalking();
        } else if (task.equalsIgnoreCase("Aberrant Spectres")) {
            Rs2Walker.walkTo(new WorldPoint(2455, 9791, 0));
            Rs2Player.waitForWalking();
        } else if (task.equalsIgnoreCase("Spiders")) {
            Rs2Walker.walkTo(new WorldPoint(1840, 9958, 0));
            Rs2Player.waitForWalking();
        } else if (task.equalsIgnoreCase("Green dragons")) {
            Rs2GameObject.interact(Rs2GameObject.findObjectById(36556, 2199), "Enter");
            sleep(1200);
            Rs2Walker.walkTo(new WorldPoint(2610, 9438, 0));
            Rs2Player.waitForWalking();
        }

        sleep(1000);
        state = SlayerState.FIGHTING;
    }

    private void handleCyclops() {
        if (!Rs2GameObject.handleNexus("Training", "Cyclopes"))
            return;
        sleep(1000);
        Rs2GameObject.interact(24318, "Open");
        Rs2Player.waitForWalking();
        Rs2Walker.walkTo(new WorldPoint(2845, 3541, 0));
        Rs2GameObject.interact(16671, "Climb-up");
        sleepUntil(() -> Rs2Player.getWorldLocation().getPlane() == 1);
        Rs2GameObject.interact(16672, "Climb-up");
        sleep(1500);
        Rs2GameObject.interact(24306, "Open");
        Rs2Player.waitForWalking();
        state = SlayerState.FIGHTING;
    }

    private void getTask() {
        if (!needsTask()) {
            state = SlayerState.TRAVELLING;
            return;
        }

        if (!isHome()) {
            Rs2Magic.teleportHome();
            return;
        }

        int currentPrayer = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
        int maxPrayer = Rs2Player.getRealSkillLevel(Skill.PRAYER);
        if (currentPrayer < maxPrayer * .75) {
            GameObject prayerAltar = Rs2GameObject.findObjectById(409, 2230);
            if (prayerAltar != null) {
                Rs2GameObject.interact(prayerAltar, "Pray-at");
                sleepUntil(() -> Rs2Player.getBoostedSkillLevel(Skill.PRAYER) >= maxPrayer);
            }
        }

        Rs2Npc.interact(getSlayerMasterId(), "Assignment");
        sleep(800, 1200);
        Widget randomButton = Rs2Widget.searchChildren("Random", Rs2Widget.getWidget(219, 1), true);
        if (randomButton != null) {
            Rs2Keyboard.keyPress((char) KeyEvent.VK_2);
            sleep(1200);
        }
    }

    private int getSlayerMasterId() {
        int slayerLevel = Rs2Player.getBoostedSkillLevel(Skill.SLAYER);

        int streak = Microbot.slayerStreak;
        if ((streak + 1) % 10 != 0 && (streak + 1) % 25 != 0) {
            return 13433;
        }

        if (slayerLevel >= 80) {
            return 15000;
        } else if (slayerLevel >= 65) {
            return 6797;
        } else if (slayerLevel >= 45) {
            return 403;
        } else {
            return 13433;
        }
    }

    private boolean needsTask() {
        return Microbot.slayerTask == null || Microbot.slayerTask.equalsIgnoreCase("");
    }

    private boolean isHome() {
        return Rs2Player.isNearArea(new WorldPoint(2229, 3319, 0), 20);
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