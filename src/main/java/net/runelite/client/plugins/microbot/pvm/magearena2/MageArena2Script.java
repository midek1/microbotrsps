package net.runelite.client.plugins.microbot.pvm.magearena2;

import net.bytebuddy.asm.Advice;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.coords.Rs2LocalPoint;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldArea;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldPoint;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class MageArena2Script extends Script {

    public static Rs2PrayerEnum toPray = Rs2PrayerEnum.PROTECT_MELEE;
    public static State state = State.TRAVELING;
    public static int sessionKills = 0;
    public static boolean needsMove = false;

    public boolean run(MageArena2Config config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;
                if (Rs2GameObject.getGameObjects(config.encounter().getStatueId()) == null) state = State.TRAVELING;
                Rs2Player.drinkPrayerPotionAt(20);

                int statueId = config.encounter().getStatueId();
                int npcId = config.encounter().getNpcId();

                LocalPoint startingPoint = LocalPoint.fromScene(config.encounter().getStandX(), config.encounter().getStandY(), Microbot.getClient().getTopLevelWorldView());

                NPC treasureGoblin = Rs2Npc.getNpc("Treasure goblin");
                if (treasureGoblin != null) {
                    Rs2Npc.interact(treasureGoblin, "Attack");
                    sleepUntil(treasureGoblin::isDead);
                }

                switch (state) {
                    case TRAVELING:
                        travelToArea(startingPoint);
                        break;
                    case STARTING:
                        if (!config.encounter().equals(MageArena2Bosses.ZAMORAK))
                            Rs2Player.handleRockCake();
                        Rs2Player.handleOverload();
                        Rs2Player.handlePrayerEnhance();
                        startFight(statueId);
                        break;
                    case FIGHTING:
                        handleCombat(npcId, startingPoint);
                        break;
                    case LOOTING:
                        handleLooting();
                        break;
                    default:
                        break;
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 150, TimeUnit.MILLISECONDS);
        return true;
    }

    private void travelToArea(LocalPoint startingPoint) {
        if (!startingSpot(startingPoint)) {
            Rs2Walker.walkFastLocal(startingPoint);
            Rs2Player.waitForWalking();
        } else {
            state = State.STARTING;
        }
    }

    private void startFight(int statueId) {
        Rs2Prayer.toggle(toPray, true);

        if (Rs2GameObject.interact(statueId))
            sleep(1200);
        else
            state = State.TRAVELING;
    }

    private void handleCombat(int npcId, LocalPoint startingPoint) {
        if (toPray != null) Rs2Prayer.toggle(toPray, true);
        Rs2Prayer.toggle(Rs2PrayerEnum.AUGURY, true);

        NPC encounter = Rs2Npc.getNpc(npcId);
        if (encounter == null || encounter.isDead()) return;

        WorldPoint playerLocation = WorldPoint.fromLocalInstance(Microbot.getClient(), Rs2Player.getLocalLocation());

        switch (encounter.getId()) {
            case NpcID.DERWEN_7859:
                LocalPoint targetTile = LocalPoint.fromScene(59, 78, Microbot.getClient().getTopLevelWorldView());
                if (!Rs2Player.getLocalLocation().equals(targetTile)) {
                    Rs2Walker.walkFastLocal(targetTile);
                    Rs2Player.waitForWalking();
                    return;
                }

                Rs2Npc.getNpcs(15173).forEach(minion -> {
                    if (minion == null || minion.isDead()) return;
                    LocalPoint nullTile = LocalPoint.fromScene(59, 77, Microbot.getClient().getTopLevelWorldView());
                    if (minion.getLocalLocation().equals(nullTile)) { //unattackable it seems
                        Rs2Walker.walkFastLocal(LocalPoint.fromScene(59, 79, Microbot.getClient().getTopLevelWorldView()));
                        Rs2Player.waitForWalking();
                        Rs2Walker.walkFastLocal(targetTile);
                    }
                    Rs2Npc.interact(minion, "Attack");
                    sleepUntil(minion::isDead);
                });
                break;
            case NpcID.PORAZDIR_7860:
                for (Projectile projectile : Microbot.getClient().getTopLevelWorldView().getProjectiles()) {
                    if (projectile.getId() == 2559) {
                        Rs2Prayer.toggle(toPray, false);
                        sleepUntil(() -> (projectile.getRemainingCycles()) < 10);
                    }
                }

                if (needsMove) {
                    if (startingSpot(startingPoint)) {
                        LocalPoint newPoint = LocalPoint.fromScene(startingPoint.getSceneX(), startingPoint.getSceneY() - 1, Microbot.getClient().getTopLevelWorldView());
                        Rs2Walker.walkFastLocal(newPoint);
                    } else {
                        Rs2Walker.walkFastLocal(startingPoint);
                    }
                    needsMove = false;
                }
                break;
            case NpcID.JUSTICIAR_ZACHARIAH_7858:
                WorldPoint npcLocation = WorldPoint.fromLocalInstance(Microbot.getClient(), encounter.getLocalLocation());
                if (npcLocation.distanceTo(playerLocation) <= 2 && startingSpot(startingPoint)) {
                    LocalPoint newPoint = LocalPoint.fromScene(startingPoint.getSceneX() - 1, startingPoint.getSceneY(), Microbot.getClient().getTopLevelWorldView());
                    Rs2Walker.walkFastLocal(newPoint);
                }
                break;
        }

        if (!Rs2Combat.inCombat()) {
            Rs2Npc.attack(encounter);
            sleepUntil(Rs2Combat::inCombat);
        }

    }

    private static final String[] itemsToLoot = {"Demon's heart", "Ent's roots", "Justiciar's hand", "staff", "book",
            "garland", "blessing", "Guthixian icon", "Saradomin's light", "symbol", "Elder chaos", "halo", "lamp",
            "bond", "flask", "noxifer", "buchu", "cicely", "zenyte", "onyx", "superior"};

    private void handleLooting() {
        sessionKills++;
        LootingParameters lootingParameters = new LootingParameters(10, itemsToLoot);
        /*LootingParameters lootParams = new LootingParameters(5, 1, 1, 0, false, false,
                "Demon's heart", "Ent's roots", "Justiciar's hand",
                "staff", "book", "garland", "blessing", "Guthixian icon", "Saradomin's light", "symbol",
                "Elder chaos", "halo",
                "lamp", "bond", "flask", "noxifer", "buchu", "cicely", "zenyte", "onyx", "superior");*/
        Rs2GroundItem.lootItemsBasedOnNames(lootingParameters);
        state = State.TRAVELING;
    }

    private boolean startingSpot(LocalPoint startingPoint) {
        return Rs2Player.getLocalLocation().equals(startingPoint);
    }

    public enum State {
        TRAVELING, STARTING, FIGHTING, LOOTING
    }

    @Override
    public void shutdown() {
        state = State.TRAVELING;
        sessionKills = 0;
        super.shutdown();
    }
}