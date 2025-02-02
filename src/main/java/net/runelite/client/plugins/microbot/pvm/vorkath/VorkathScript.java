package net.runelite.client.plugins.microbot.pvm.vorkath;

import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.playerassist.loot.LootScript;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


public class VorkathScript extends Script {

    public static int sessionKills = 0;
    public static VorkathState state = VorkathState.FIGHTING;
    public static final String[] ITEMS_TO_LOOT = {
            "Green dragon leather", "Red dragon leather", "Black dragon leather",
            "Dragon ore", "Luminite flux", "Dragon bolts (unf)", "Onyx bolt tips",
            "Vorkath", "Onyx", "Draconic visage", "Jar of decay", "Dragonite Defender"
    };

    public boolean run() {

        if (Microbot.isLoggedIn()) Rs2Prayer.setQuickPrayers(new Rs2PrayerEnum[]{Rs2PrayerEnum.PROTECT_MAGIC, Rs2PrayerEnum.AUGURY});
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;

            switch(state) {
                case BANKING:
                    handleBanking();
                    break;
                case ENTERING:
                    handleEntering();
                    break;
                case FIGHTING:
                    handleFight();
                    break;
                case LOOTING:
                    handleLooting();
                    break;
                default:
                    state = VorkathState.BANKING;
            }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleBanking() {

    }

    private void handleEntering() {

    }

    private void handleFight() {
        NPC vorkath = Rs2Npc.getNpcsWithDead().filter(npc -> npc.getName() != null && npc.getName().equalsIgnoreCase("Vorkath")).findFirst().orElse(null);

        if (vorkath == null) return;

        Rs2Prayer.toggleQuickPrayer(true);
        Rs2Player.drinkPrayerPotionAt(20);
        Rs2Player.handleOverload();
        Rs2Player.handlePrayerEnhance();

        switch(vorkath.getId()) {
            case 8059:
                Rs2Npc.interact(vorkath, "Poke");
                sleep(800, 1200);
                break;
            case 8058:
                state = VorkathState.LOOTING;
                break;
            case 8061:
                handleFightLogic(vorkath);
                break;
        }
    }

    private void handleFightLogic(NPC vorkath) {

        for (Projectile p : Microbot.getClient().getTopLevelWorldView().getProjectiles()) {
            switch (p.getId()) {
                case 1481:
                    if (p.getTarget().equals(Rs2Player.getLocalLocation()))
                        handleNuke();
                    break;
                case 1482:
                    handleAcidWalk();
                    break;
                case 1483:
                    handleSpawn();
                    break;
            }
        }

        handleAxe(vorkath);

        if (!Rs2Player.isInteracting())
            Rs2Npc.interact(vorkath, "Attack");
    }

    private void handleAxe(NPC vorkath) {
        if (vorkath == null) return;

        if (Rs2Npc.getHealth(vorkath) < 20) {
            if (Rs2Inventory.contains(28773)) {
                Rs2Inventory.equip(28773);
                Rs2Inventory.waitForInventoryChanges(1000);
            }
        } else {
            if (Rs2Equipment.isWearing(28773)) {
                Rs2Inventory.equip(Rs2Inventory.get("wand", false).getId());
                Rs2Inventory.waitForInventoryChanges(1000);
            }
        }
    }

    private void handleNuke() {
        WorldPoint currentPos = Rs2Player.getWorldLocation();
        WorldPoint moveLoc = new WorldPoint(currentPos.getX() + (Rs2Random.between(0,2) == 1 ? 2: -2), currentPos.getY(), currentPos.getPlane());
        sleep(300, 800);
        Rs2Walker.walkFastLocal(LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), moveLoc));
        Rs2Player.waitForWalking();
    }

    private void handleAcidWalk() {
        for (int i = 0; i < 15; i++) {
            WorldPoint currentPos = Rs2Player.getWorldLocation();
            WorldPoint wantedPos = new WorldPoint(currentPos.getX() + (Rs2Random.between(0, 2) == 1 ? 1 : -1), currentPos.getY(), currentPos.getPlane());
            Rs2Walker.walkFastLocal(LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), wantedPos));
            sleepUntilTick(1);
        }
    }

    private void handleSpawn() {
        if (sleepUntil(() -> Rs2Npc.getNpc(8062) != null, 10000)) {
            NPC spawn = Rs2Npc.getNpc(8062);

            if (Rs2Equipment.isWearing(28773))
                Rs2Inventory.equip(Rs2Inventory.get("wand", false).getId());
            Rs2Npc.interact(spawn, "Attack");
            sleepUntil(spawn::isDead);
        }
    }

    private void handleLooting() {
        if (sleepUntil(() -> Rs2GroundItem.exists(ItemID.BLUE_DRAGON_LEATHER, 5), 1000)) {
            sessionKills++;
            sleep(400, 1000);
            LootingParameters loot = new LootingParameters(5, ITEMS_TO_LOOT);
            Rs2GroundItem.lootItemsBasedOnNames(loot);
            Rs2GroundItem.lootGlobalDrops();
            Rs2GroundItem.loot(2506);
        }
        state = VorkathState.FIGHTING;
    }

    @Override
    public void shutdown() {
        sessionKills = 0;
        super.shutdown();
    }
}