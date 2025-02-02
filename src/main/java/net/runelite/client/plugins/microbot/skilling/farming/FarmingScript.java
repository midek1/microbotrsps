package net.runelite.client.plugins.microbot.skilling.farming;

import net.runelite.api.GameObject;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class FarmingScript extends Script {

    private static final List<Integer> HERB_PATCHES_MAIN = List.of(65490, 65491, 65492, 65493);
    private static final List<Integer> HERB_PATCHES_DONOR = List.of(65494, 65495, 65496, 65497);
    private static final List<Integer> ALLOTMENT_PATCHES_MAIN = List.of(65486, 65487, 65488, 65489);
    private static final List<Integer> ALLOTMENT_PATCHES_DONOR = List.of(65482, 65483, 65484, 65485);

    private static final Integer[] HERB_SEEDS = {
            ItemID.GUAM_SEED, ItemID.MARRENTILL_SEED, ItemID.TARROMIN_SEED, ItemID.HARRALANDER_SEED, ItemID.RANARR_SEED,
            ItemID.TOADFLAX_SEED, ItemID.IRIT_SEED, ItemID.AVANTOE_SEED, ItemID.KWUARM_SEED, ItemID.SNAPDRAGON_SEED,
            ItemID.CADANTINE_SEED, ItemID.LANTADYME_SEED, ItemID.DWARF_WEED_SEED, ItemID.TORSTOL_SEED };

    private static final Integer[] ALLOTMENT_SEEDS = {
            ItemID.POTATO_SEED, ItemID.ONION_SEED, ItemID.CABBAGE_SEED, ItemID.TOMATO_SEED,
            ItemID.SWEETCORN_SEED, ItemID.STRAWBERRY_SEED, ItemID.WATERMELON_SEED, ItemID.SNAPE_GRASS_SEED };

    public boolean run(FarmingConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;

                int herbSeed = Rs2Inventory.get(HERB_SEEDS).getId(); //first herb seed in inventory
                int allotmentSeed = Rs2Inventory.get(ALLOTMENT_SEEDS).getId();

                HERB_PATCHES_MAIN.forEach(patch -> {
                    GameObject patchObject = Rs2GameObject.findObjectByImposter(patch, "Pick");
                    if (patchObject != null) {
                        handlePatch(patchObject, herbSeed);
                    }
                });

                ALLOTMENT_PATCHES_MAIN.forEach(patch -> {
                    GameObject patchObject = Rs2GameObject.findObjectByImposter(patch, "Harvest");
                    if (patchObject != null) {
                        handlePatch(patchObject, allotmentSeed);
                    }
                });

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 30000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handlePatch(GameObject patch, int seedId) {
        if (Rs2Player.getWorldLocation().distanceTo(patch.getWorldLocation()) > 10)
            Rs2Walker.walkNextTo(patch);

        Rs2GameObject.interact(patch, "Pick");
        Rs2Player.waitForXpDrop(Skill.FARMING);

        if (!Rs2Inventory.useItemOnObject(seedId, patch.getId()))
            return;

        Rs2Player.waitForWalking();
        Rs2Player.waitForAnimation();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}