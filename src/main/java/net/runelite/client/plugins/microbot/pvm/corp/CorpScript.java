package net.runelite.client.plugins.microbot.pvm.corp;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.playerassist.enums.State;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
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

import java.util.concurrent.TimeUnit;


public class CorpScript extends Script {

    public static NPC corp = null;
    public static CorpState state = CorpState.BANKING;
    public static int sessionKills = 0;
    private static final String[] itemsToLoot = {
            "Regen bracelet", "Essence shards", "Shadowy Necronomicon", "Bloodstained Parchment", "Wrathful Manuscript",
            "Spirit shield", "Holy elixir", "Tome", "sigil", "Onyx"
    };

    public boolean run(CorpConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;

                switch (state) {
                    case TRAVELING:
                        handleTravel();
                        break;
                    case FIGHTING:
                        handleFighting();
                        break;
                    case LOOTING:
                        handleLooting();
                        break;
                    case BANKING:
                        handleBanking();
                        break;
                    case WAITING:
                        if (Rs2Equipment.isWearing(28773)) {
                            Rs2Inventory.equip(Rs2Inventory.get("wand", false).getId());
                        }
                        if (sleepUntil(() -> Rs2Npc.getNpc(NpcID.CORPOREAL_BEAST) != null, 30000))
                            state = CorpState.FIGHTING;
                        else state = CorpState.BANKING;
                        break;
                    default:
                        break;
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean inCorpCave() {
        return WorldPoint.fromLocalInstance(Microbot.getClient(), Microbot.getClient().getLocalPlayer().getLocalLocation()).getRegionID() == 11844;
    }

    private void handleTravel() {

        if (inCorpCave()) {
            if (!Rs2Player.IsInInstance()) {
                Rs2GameObject.interact(Rs2GameObject.findObjectById(9370), "Enter");
                sleepUntil(Rs2Player::IsInInstance);
                return;
            }

            if (Rs2GameObject.interact(Rs2GameObject.findObjectById(677), "Go-Through")) {
                Rs2Player.waitForWalking();
                state = CorpState.FIGHTING;
                return;
            }
        }

        Rs2GameObject.handleTeleportInterface("Boss", "Corporeal Beast");
    }

    private void handleFighting() {
        if (corp == null) return;

        if (Rs2Player.getBoostedSkillLevel(Skill.PRAYER) < 10 && !Rs2Inventory.hasUnNotedItem("Super restore", false)) {
            state = CorpState.BANKING;
            return;
        }

        Rs2Prayer.enable(Rs2PrayerEnum.AUGURY, Rs2PrayerEnum.PROTECT_MELEE);
        handleBoosts();
        handleDarkCore();

        if (Rs2Npc.getHealth(corp) < 20 && !Rs2Equipment.isWearing(28773)) {
            Rs2Inventory.equip(28773);
            Rs2Inventory.waitForInventoryChanges(1200);
        }

        if (!Rs2Player.isInteracting()) {
            Rs2Npc.interact(corp, "Attack");
            sleepUntil(Rs2Player::isInteracting);
        }
    }

    private void handleBoosts() {
        Rs2Player.handleOverload();
        Rs2Player.handlePrayerEnhance();
        Rs2Player.drinkPrayerPotionAt(20);
    }

    private void handleDarkCore() {
        boolean needsMove = false;

        for (Projectile p : Microbot.getClient().getTopLevelWorldView().getProjectiles())
            if (p.getId() == 319 && p.getTarget().equals(Rs2Player.getLocalLocation()))
                needsMove = true;

        NPC darkEnergyCore = Rs2Npc.getNpc(NpcID.DARK_ENERGY_CORE);
        if (darkEnergyCore != null && darkEnergyCore.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= 1)
            needsMove = true;

        if (!needsMove) return;

        WorldPoint firstSpot = new WorldPoint(corp.getWorldLocation().getX() - 1, corp.getWorldLocation().getY() + 1, corp.getWorldLocation().getPlane());
        WorldPoint secondSpot = new WorldPoint(corp.getWorldLocation().getX() + 1, corp.getWorldLocation().getY() - 1, corp.getWorldLocation().getPlane());
        LocalPoint newSpot = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), Rs2Player.distanceTo(firstSpot) > 1 ? firstSpot : secondSpot);

        Rs2Walker.walkFastLocal(newSpot);
        sleepUntilTick(1);
    }

    private void handleLooting() {
        sessionKills++;
        LootingParameters lootingParameters = new LootingParameters(5, itemsToLoot);
        Rs2GroundItem.lootItemsBasedOnNames(lootingParameters);
        state = CorpState.WAITING;
    }

    private void handleBanking() {
        Rs2InventorySetup corpSetup = new Rs2InventorySetup("corp", mainScheduledFuture);

        if (corpSetup.doesInventoryMatch() && corpSetup.doesEquipmentMatch()) {
            state = CorpState.TRAVELING;
            return;
        }

        if (!Rs2Bank.openBank())
            Rs2Magic.teleportHome();

        if (!corpSetup.doesEquipmentMatch())
            corpSetup.loadEquipment();

        if (!corpSetup.doesInventoryMatch())
            corpSetup.loadInventory();

        Rs2Bank.closeBank();
        sleep(800, 1200);
    }

    @Override
    public void shutdown() {
        state = CorpState.BANKING;
        sessionKills = 0;
        corp = null;
        super.shutdown();
    }
}