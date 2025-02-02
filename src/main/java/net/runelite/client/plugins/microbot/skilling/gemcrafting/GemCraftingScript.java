package net.runelite.client.plugins.microbot.skilling.gemcrafting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class GemCraftingScript extends Script {

    private static final List<Integer> BANK_IDS = List.of(10060, 65466);
    public static String gem = null;

    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || Microbot.pauseAllScripts) return;

                String uncutGemName = "Uncut " + gem;
                String cutGemName = gem;
                if (gem == null || !Rs2Inventory.hasNotedItem(uncutGemName)) {
                    reset();
                    return;
                }

                //Unnote the gems
                Rs2Inventory.use(Rs2Inventory.getNotedItem(uncutGemName, true));
                Rs2GameObject.interact(BANK_IDS);
                Rs2Inventory.waitForInventoryChanges(1200);

                //Cut the gems
                Rs2Inventory.use("Chisel");
                Rs2Inventory.use(Rs2Inventory.getUnNotedItem(uncutGemName, true));
                sleep(800, 1000);
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                sleep(2000);
                sleepUntil(() -> !Rs2Player.waitForXpDrop(Skill.CRAFTING, 3000), 30000);

                //Note the processed gems
                Rs2Inventory.use(Rs2Inventory.getUnNotedItem(cutGemName, true));
                Rs2GameObject.interact(BANK_IDS);
                Rs2Inventory.waitForInventoryChanges(1200);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
        return true;
    }

    private void reset() {
        gem = Gems.getGem();

        if (gem == null) {
            Microbot.log("Out of gems");
            shutdown();
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum Gems {
        ZENYTE("Zenyte", 89),
        ONYX("Onyx", 67),
        DRAGONSTONE("Dragonstone", 55),
        DIAMOND("Diamond", 43),
        RUBY("Ruby", 34),
        EMERALD("Emerald", 27),
        SAPPHIRE("Sapphire", 20);

        private final String name;
        private final int levelRequired;

        public static String getGem() {
            int level = Rs2Player.getBoostedSkillLevel(Skill.CRAFTING);
            for (Gems gem : values()) {
                if (level >= gem.getLevelRequired() && Rs2Inventory.hasNotedItem("Uncut " + gem.getName())) {
                    return gem.getName();
                }
            }
            return null;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}