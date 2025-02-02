package net.runelite.client.plugins.microbot.pvm.zulrah;

import net.bytebuddy.asm.Advice;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.World;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Optional;
import java.util.concurrent.TimeUnit;


public class ZulrahScript extends Script {

    public static int sessionKills = 0;
    public static boolean needsMove = false;
    public static Rs2PrayerEnum toPray = Rs2PrayerEnum.PROTECT_MAGIC;
    private static final String[] itemsToLoot = {
            "Serpentine visage", "Magic fang", "Tanzanite fang", "Uncut onyx", "Jar of swamp", "mutragen", "Toxic orb",
            "Dragon dart tip", "dragon leather", "Essence shards", "ore", "Luminite flux"
    };

    public static ZulrahState state = ZulrahState.STARTING;

    public boolean run(ZulrahConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;

                Rs2Player.handleOverload();
                Rs2Player.handlePrayerEnhance();
                Rs2Player.drinkPrayerPotionAt(10);

                switch(state) {
                    case STARTING:
                        handleStart();
                        break;
                    case FIGHTING:
                        handleFight();
                        break;
                    case LOOTING:
                        handleLoot();
                        break;
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 150, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleStart() {

        toPray = Rs2PrayerEnum.PROTECT_MAGIC;
        Rs2Player.handleRockCake();

        GameObject portalNexus = Rs2GameObject.findObjectById(65527, 2236);
        if (portalNexus != null) {

            if (Rs2Player.getBoostedSkillLevel(Skill.PRAYER) < 50) {
                GameObject prayerAltar = Rs2GameObject.findObjectById(409, 2230);
                if (prayerAltar == null) return;
                Rs2GameObject.interact(prayerAltar, "Pray-at");
                sleepUntil(() -> Rs2Player.getBoostedSkillLevel(Skill.PRAYER) > 50);
                return;
            }

            if (Rs2GameObject.interact(portalNexus, "Teleport"))
                sleepUntil(() -> Rs2Widget.hasWidget("Favourites"), 10000);

            if (!Rs2Widget.hasWidget("Favourite Teleports")) {
                Rs2Widget.clickWidget(2006, 16); //favourites tab
                sleepUntil(() -> Rs2Widget.hasWidget("Favourite Teleports"));
            }

            Rs2Widget.clickWidget("Zulrah", Optional.of(2005), 57, true);
            sleep(600);
            Rs2Player.waitForAnimation(1200);
            return;
        }

        GameObject boat = (GameObject) Rs2GameObject.findObjectById(10068);
        if (boat != null) {
            Rs2GameObject.interact(boat, "Board");
            sleepUntil(() -> Rs2Player.getLocalLocation().equals(LocalPoint.fromScene(52, 53, Microbot.getClient().getTopLevelWorldView())), 10000);
            state = ZulrahState.FIGHTING;
            return;
        }

        Rs2Magic.teleportHome();
    }

    private void handleFight() {
        if (!Rs2Player.isAnimating(20000))
            state = ZulrahState.LOOTING;

        LocalPoint startTile = LocalPoint.fromScene(56, 62, Microbot.getClient().getTopLevelWorldView());
        LocalPoint altTile = LocalPoint.fromScene(57, 61, Microbot.getClient().getTopLevelWorldView());

        Rs2Prayer.toggle(toPray, true);
        Rs2Prayer.toggle(Rs2PrayerEnum.AUGURY, true);

        if (!Rs2Player.getLocalLocation().equals(startTile) && !Rs2Player.getLocalLocation().equals(altTile)) {
            Rs2Walker.walkFastLocal(startTile);
            Rs2Player.waitForWalking();
        }

        NPC zulrah = Rs2Npc.getNpc("Zulrah", true);
        if (zulrah == null) return;

        if (needsMove) {
            LocalPoint destinationTile = Rs2Player.getLocalLocation().equals(startTile) ? altTile : startTile;
            Rs2Walker.walkFastLocal(destinationTile);
            Rs2Player.waitForWalking(10000);
            needsMove = false;
        }

        if (!Rs2Player.isInteracting() && !zulrah.isDead()) {
            Rs2Npc.interact(zulrah, "Attack");
            sleepUntil(Rs2Player::isInteracting, 1200);
        }
    }

    private void handleLoot() {
        sessionKills++;
        LootingParameters lootparams = new LootingParameters(5, itemsToLoot);
        Rs2GroundItem.lootItemsBasedOnNames(lootparams);
        state = ZulrahState.STARTING;
    }

    @Override
    public void shutdown() {
        sessionKills = 0;
        needsMove = false;
        state = ZulrahState.STARTING;
        super.shutdown();
    }
}