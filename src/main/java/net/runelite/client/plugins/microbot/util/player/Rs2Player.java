package net.runelite.client.plugins.microbot.util.player;

import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.VarbitValues;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldPoint;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.runelite.api.MenuAction.CC_OP;
import static net.runelite.client.plugins.microbot.util.Global.*;


public class Rs2Player {
    static int VENOM_VALUE_CUTOFF = -38;
    private static int antiFireTime = -1;
    private static int superAntiFireTime = -1;
    private static int divineRangedTime = -1;
    private static int divineBastionTime = -1;
    private static int divineCombatTime = -1;
    public static int antiVenomTime = -1;
    public static int staminaBuffTime = -1;
    public static int antiPoisonTime = -1;
    public static int teleBlockTime = -1;
    public static Instant lastAnimationTime = null;
    private static final long COMBAT_TIMEOUT_MS = 10000;
    private static long lastCombatTime = 0;
    public static boolean lastInteractWasPlayer = false;
    @Getter
    public static int lastAnimationID = AnimationID.IDLE;

    public static boolean hasAntiFireActive() {
        return antiFireTime > 0 || hasSuperAntiFireActive();
    }

    public static boolean hasSuperAntiFireActive() {
        return superAntiFireTime > 0;
    }

    public static boolean hasDivineRangedActive() {
        return divineRangedTime > 0 || hasDivineBastionActive();
    }

    public static boolean hasRangingPotionActive(int threshold) {
        return Microbot.getClient().getBoostedSkillLevel(Skill.RANGED) - threshold > Microbot.getClient().getRealSkillLevel(Skill.RANGED);
    }

    public static boolean hasDivineBastionActive() {
        return divineBastionTime > 0;
    }

    public static boolean hasDivineCombatActive() {
        return divineCombatTime > 0;
    }

    public static boolean hasAttackActive(int threshold) {
        return Microbot.getClient().getBoostedSkillLevel(Skill.ATTACK) - threshold > Microbot.getClient().getRealSkillLevel(Skill.ATTACK);
    }

    public static boolean hasStrengthActive(int threshold) {
        return Microbot.getClient().getBoostedSkillLevel(Skill.STRENGTH) - threshold > Microbot.getClient().getRealSkillLevel(Skill.STRENGTH);
    }

    public static boolean hasDefenseActive(int threshold) {
        return Microbot.getClient().getBoostedSkillLevel(Skill.DEFENCE) - threshold > Microbot.getClient().getRealSkillLevel(Skill.DEFENCE);
    }


    public static boolean hasAntiVenomActive() {
        if (Rs2Equipment.isWearing("serpentine helm")) {
            return true;
        } else return antiVenomTime < VENOM_VALUE_CUTOFF;
    }

    public static boolean hasAntiPoisonActive() {
        return antiPoisonTime > 0;
    }

    public static boolean hasStaminaBuffActive() {
        return staminaBuffTime > 0;
    }
    
    public static boolean isTeleBlocked() {
        return teleBlockTime > 0;
    }

    private static final Map<Player, Long> playerDetectionTimes = new ConcurrentHashMap<>();

    public static void handlePotionTimers(VarbitChanged event) {
        if (event.getVarbitId() == Varbits.ANTIFIRE) {
            antiFireTime = event.getValue();
        }
        if (event.getVarbitId() == Varbits.SUPER_ANTIFIRE) {
            superAntiFireTime = event.getValue();
        }
        if (event.getVarbitId() == Varbits.DIVINE_RANGING) {
            divineRangedTime = event.getValue();
        }
        if (event.getVarbitId() == Varbits.DIVINE_BASTION) {
            divineBastionTime = event.getValue();
        }
        if (event.getVarbitId() == Varbits.DIVINE_SUPER_COMBAT) {
            divineCombatTime = event.getValue();
        }
        if (event.getVarbitId() == Varbits.STAMINA_EFFECT) {
            staminaBuffTime = event.getValue();
        }
        if (event.getVarpId() == VarPlayer.POISON) {
            if (event.getValue() >= VENOM_VALUE_CUTOFF) {
                antiVenomTime = 0;
            } else {
                antiVenomTime = event.getValue();
            }
            final int poisonVarp = event.getValue();

            if (poisonVarp == 0) {
                antiPoisonTime = -1;
            } else {
                antiPoisonTime = poisonVarp;
            }
        }
    }

    public static void handleCustom() {
        handleOverload();
        handlePrayerEnhance();
        handleRockCake();
    }

    public static void handleOverload() {
        int ticksRemaining = Microbot.augustTimers.get("Overload");
        if (ticksRemaining == 0 && Rs2Inventory.hasUnNotedItem("Overload", false)
                && Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS) > 50) {
            Rs2Inventory.interact("Overload", "Drink", false);
        }
    }

    public static void handlePrayerEnhance() {
        int ticksRemaining = Microbot.augustTimers.get("Prayer Renewal");
        if (ticksRemaining == 0 && Rs2Inventory.hasUnNotedItem("Prayer Renewal"))
            Rs2Inventory.interact("Prayer Renewal", "Drink", false);
    }

    public static void handleRockCake() {
        int currentHp = Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS);

        //only rock cake after consuming overload and taking its damage)
        int overloadTicksLeft = Microbot.augustTimers.get("Overload");
        if (overloadTicksLeft == 0 || overloadTicksLeft > 565) return;

        if (currentHp > 30) {
            Rs2Inventory.interact("Rock cake", "Eat", false);
        }
    }
    
    /**
     * Handles updates to the teleblock timer based on changes to the {@link Varbits#TELEBLOCK} varbit.
     *
     * @see Varbits#TELEBLOCK
     */
    public static void handleTeleblockTimer(VarbitChanged event){
        if (event.getVarbitId() == Varbits.TELEBLOCK) {
            int time = event.getValue();
            
            if (time < 101) {
                teleBlockTime = -1;
            } else {
                teleBlockTime = time;
            }
        }
    }

    public static void handleAnimationChanged(AnimationChanged event) {
        if (!(event.getActor() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getActor();
        if (player != Microbot.getClient().getLocalPlayer()) {
            return;
        }

        if (player.getAnimation() != AnimationID.IDLE) {
            lastAnimationTime = Instant.now();
            lastAnimationID = player.getAnimation();
        }
    }

    /**
     * Wait for walking
     */
    public static void waitForWalking() {
        boolean result = sleepUntilTrue(Rs2Player::isWalking, 100, 5000);
        if (!result) return;
        sleepUntil(() -> !Rs2Player.isWalking());
    }

    /**
     * Wait for walking in time
     *
     * @param time
     */
    public static void waitForWalking(int time) {
        boolean result = sleepUntilTrue(Rs2Player::isWalking, 100, time);
        if (!result) return;
        sleepUntil(() -> !Rs2Player.isWalking(), time);
    }

    /**
     * Wait for XP Drop
     *
     * @param skill
     * @return
     */
    public static boolean waitForXpDrop(Skill skill) {
        return waitForXpDrop(skill, 5000, false);
    }

    /**
     * Wait for XP Drop or if inventory is full
     *
     * @param skill
     * @param time
     * @return
     */
    public static boolean waitForXpDrop(Skill skill, int time) {
        return waitForXpDrop(skill, time, false);
    }

    /**
     * Wait for XP Drop or if inventory is full
     *
     * @param skill
     * @param inventoryFullCheck
     * @return
     */
    public static boolean waitForXpDrop(Skill skill, boolean inventoryFullCheck) {
        return waitForXpDrop(skill, 5000, inventoryFullCheck);
    }

    /**
     * Wait for XP Drop in time or if inventory is full
     *
     * @param skill
     * @param time
     * @param inventoryFullCheck
     * @return
     */
    public static boolean waitForXpDrop(Skill skill, int time, boolean inventoryFullCheck) {
        final int skillExp = Microbot.getClient().getSkillExperience(skill);
        return sleepUntilTrue(() -> skillExp != Microbot.getClient().getSkillExperience(skill) || (inventoryFullCheck && Rs2Inventory.isFull()), 100, time);
    }

    /**
     * Wait for animation
     */
    public static void waitForAnimation() {
        boolean result = sleepUntilTrue(Rs2Player::isAnimating, 100, 5000);
        if (!result) return;
        sleepUntil(() -> !Rs2Player.isAnimating());
    }

    /**
     * Wait for animation
     */
    public static void waitForAnimation(int time) {
        boolean result = sleepUntilTrue(() -> Rs2Player.isAnimating(time), 100, 5000);
        if (!result) return;
        sleepUntil(() -> !Rs2Player.isAnimating(time));
    }

    /**
     * Chek if the player is animating within the past ms
     *
     * @param ms
     * @return
     */
    public static boolean isAnimating(int ms) {
        return (lastAnimationTime != null && Duration.between(lastAnimationTime, Instant.now()).toMillis() < ms) || getAnimation() != AnimationID.IDLE;
    }

    /**
     * Check if the player is animating within the past 600ms
     *
     * @return
     */
    public static boolean isAnimating() {
        return isAnimating(600);
    }

    /**
     * Check if the player is walking
     *
     * @return
     */
    public static boolean isWalking() {
        return Rs2Player.isMoving();
    }

    /**
     * Checks if the player is moving
     *
     * @return
     */
    public static boolean isMoving() {
        return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getLocalPlayer().getPoseAnimation()
                != Microbot.getClient().getLocalPlayer().getIdlePoseAnimation());
    }

    /**
     * Checks if the player is interacting
     *
     * @return
     */
    public static boolean isInteracting() {
        return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getLocalPlayer().isInteracting());
    }

    /**
     * Checks if the player is a member
     *
     * @return
     */
    public static boolean isMember() {
        return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getVarpValue(VarPlayer.MEMBERSHIP_DAYS) > 0);
    }

    /**
     * Checks if a player is in a member world
     *
     * @return true if in a member world
     */
    public static boolean isInMemberWorld() {
        WorldResult worldResult = Microbot.getWorldService().getWorlds();

        List<net.runelite.http.api.worlds.World> worlds;
        if (worldResult != null) {
            worlds = worldResult.getWorlds();
            Random r = new Random();
            return worlds.stream()
                    .anyMatch(x -> x.getId() == Microbot.getClient().getWorld() && x.getTypes().contains(WorldType.MEMBERS));
        }

        return false;
    }


    @Deprecated(since = "Use the Rs2Combat.specState method", forRemoval = true)
    public static void toggleSpecialAttack(int energyRequired) {
        int currentSpecEnergy = Microbot.getClient().getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);
        if (currentSpecEnergy >= energyRequired && (Microbot.getClient().getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0)) {
            Rs2Widget.clickWidget("special attack");
        }
    }

    /**
     * Toggles player run
     *
     * @param toggle
     * @return
     */
    public static boolean toggleRunEnergy(boolean toggle) {
        if (Microbot.getVarbitPlayerValue(173) == 0 && !toggle) return true;
        if (Microbot.getVarbitPlayerValue(173) == 1 && toggle) return true;
        Widget widget = Rs2Widget.getWidget(WidgetInfo.MINIMAP_TOGGLE_RUN_ORB.getId());
        if (widget == null) return false;
        if (toggle) {
            Microbot.getMouse().click(widget.getCanvasLocation());
            sleep(150, 300);
            return true;
        } else if (!toggle) {
            Microbot.getMouse().click(widget.getCanvasLocation());
            sleep(150, 300);
            return true;
        }
        return false;
    }

    /**
     * Checks if run is enabled
     *
     * @return
     */
    public static boolean isRunEnabled() {
        return Microbot.getVarbitPlayerValue(173) == 1;
    }

    /**
     * Logs the player out of the game
     */
    public static void logout() {
        if (Microbot.isLoggedIn()) {
            //logout from main tab
            Microbot.doInvoke(new NewMenuEntry(-1, 11927560, CC_OP.getId(), 1, -1, "Logout"), new Rectangle(1, 1, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
            //logout from world hopper
            Microbot.doInvoke(new NewMenuEntry(-1, 4522009, CC_OP.getId(), 1, -1, "Logout"), new Rectangle(1, 1, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
        }

        //Rs2Reflection.invokeMenu(-1, 11927560, CC_OP.getId(), 1, -1, "Logout", "", -1, -1);
    }

    /**
     * Logouts out the player is found in an area around the player for time
     *
     * @param amountOfPlayers to detect before triggering logout
     * @param time            in milliseconds
     * @param distance        from the player
     * @return
     */
    public static boolean logoutIfPlayerDetected(int amountOfPlayers, int time, int distance) {
        List<Player> players = getPlayers();
        long currentTime = System.currentTimeMillis();
        System.out.println(players.size());

        for (Player player : players
        ) {
            System.out.println(player.getName());
        }

        if (distance > 0) {
            players = players.stream()
                    .filter(x -> x != null && x.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= distance)
                    .collect(Collectors.toList());
        }
        if (time > 0 && players.size() > amountOfPlayers) {
            // Update detection times for currently detected players
            for (Player player : players) {
                playerDetectionTimes.putIfAbsent(player, currentTime);
            }

            // Remove players who are no longer detected
            playerDetectionTimes.keySet().retainAll(players);

            // Check if any player has been detected for longer than the specified time
            for (Player player : players) {
                long detectionTime = playerDetectionTimes.getOrDefault(player, 0L);
                var a = currentTime - detectionTime;
                if (currentTime - detectionTime >= time) { // convert time to milliseconds
                    logout();
                    playerDetectionTimes.clear();
                    return true;
                }
            }
        } else if (time <= 0 && players.size() >= amountOfPlayers) {
            logout();
            playerDetectionTimes.clear();
            return true;
        }
        return false;
    }

    /**
     * @param amountOfPlayers
     * @param time
     * @return
     */
    public static boolean logoutIfPlayerDetected(int amountOfPlayers, int time) {
        return logoutIfPlayerDetected(amountOfPlayers, time, 0);
    }

    /**
     * @param amountOfPlayers
     * @return
     */
    public static boolean logoutIfPlayerDetected(int amountOfPlayers) {
        return logoutIfPlayerDetected(amountOfPlayers, 0, 0);
    }

    /**
     * Eat food at a certain health percentage, will search inventory for first possible food item.
     *
     * @param percentage
     * @return
     */
    public static boolean eatAt(int percentage) {
        double treshHold = getHealthPercentage();
        if (treshHold <= percentage) {
            return useFood();
        }
        return false;
    }

    public static boolean useFood() {
        List<Rs2Item> foods = Rs2Inventory.getInventoryFood();
        if (!foods.isEmpty()) {
            if (foods.get(0).getName().toLowerCase().contains("jug of wine")) {
                return Rs2Inventory.interact(foods.get(0), "drink");
            } else if (foods.get(0).getName().toLowerCase().contains("blighted") && Microbot.getVarbitValue(Varbits.IN_WILDERNESS) == 1) {
                return Rs2Inventory.interact(foods.get(0), "eat");
            } else if (!foods.get(0).getName().toLowerCase().contains("blighted")) {
                return Rs2Inventory.interact(foods.get(0), "eat");
            }
        }
        return false;
    }

    /**
     * Calculates the player's current health as a percentage of their real (base) health.
     *
     * @return the health percentage as a double. For example:
     *         150.0 if boosted, 80.0 if drained, or 100.0 if unchanged.
     */
    public static double getHealthPercentage() {
        return (double) (Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) * 100) / Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS);
    }

    /**
     * Get a list of players around you
     *
     * @return
     */
    public static List<Player> getPlayers() {
        return Microbot.getClient()
                .getTopLevelWorldView()
                .players()
                .stream()
                .filter(Objects::nonNull)
                .filter(x -> x != Microbot.getClient().getLocalPlayer())
                .collect(Collectors.toList());
    }

    /**
     * Use this method to get a list of players that are in combat
     *
     * @return a list of players that are in combat
     */
    public static List<Player> getPlayersInCombat() {
        return getPlayers()
                .stream()
                .filter(x -> x != null && x.getHealthRatio() != -1)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the player's health as a percentage.
     *
     * @param player The player or actor to calculate health for.
     * @return The health percentage, or -1 if health information is unavailable.
     */
    public static int calculateHealthPercentage(Player player) {
        int healthRatio = player.getHealthRatio();
        int healthScale = player.getHealthScale();

        // Check if health information is available
        if (healthRatio == -1 || healthScale == -1 || healthScale == 0) {
            return -1; // Health information is missing or invalid
        }

        // Calculate health percentage
        return (int) ((healthRatio / (double) healthScale) * 100);
    }

    /**
     * This method retrieves the id of the equipment
     *
     * @param player
     * @return
     */
    public static Map<KitType, Integer> getPlayerEquipmentIds(Player player) {

        Map<KitType, Integer> list = new HashMap<>();

        for (KitType kitType : KitType.values()) {
            int itemId = player.getPlayerComposition().getEquipmentId(kitType);
            list.put(kitType, itemId);
        }

        return list;
    }

    /**
     * This method retrieves the names of the equipment
     *
     * @param player
     * @return
     */
    public static Map<KitType, String> getPlayerEquipmentNames(Player player) {

        Map<KitType, String> list = Microbot.getClientThread().runOnClientThread(() -> {
            Map<KitType, String> _list = new HashMap<>();
            for (KitType kitType : KitType.values()) {
                String item = Microbot.getItemManager().getItemComposition(player.getPlayerComposition().getEquipmentId(kitType)).getName();
                _list.put(kitType, item);
            }
            return _list;
        });

        return list;
    }

    /**
     * Checks if a player has a specific item equipped by ID.
     *
     * @param player The player to check.
     * @param itemId The ID of the item to look for.
     * @return True if the player has the specified item equipped, false otherwise.
     */
    public static boolean hasPlayerEquippedItem(Player player, int itemId) {
        Map<KitType, Integer> equipment = getPlayerEquipmentIds(player);

        return equipment.values().stream()
                .anyMatch(equippedItemId -> equippedItemId == itemId);
    }

    /**
     * Checks if a player has any of the specified items equipped by their IDs.
     *
     * @param player The player to check.
     * @param itemIds An array of item IDs to look for.
     * @return True if the player has any of the specified items equipped, false otherwise.
     */
    public static boolean hasPlayerEquippedItem(Player player, int[] itemIds) {
        Map<KitType, Integer> equipment = getPlayerEquipmentIds(player);

        return equipment.values().stream()
                .anyMatch(equippedItemId -> Arrays.stream(itemIds).anyMatch(id -> id == equippedItemId));
    }

    /**
     * Checks if a player has a specific item equipped.
     *
     * @param player The player to check.
     * @param itemName The name of the item to look for.
     * @return True if the player has the specified item equipped, false otherwise.
     */
    public static boolean hasPlayerEquippedItem(Player player, String itemName) {
        Map<KitType, String> equipment = getPlayerEquipmentNames(player);

        return equipment.values().stream()
                .anyMatch(equippedItem -> equippedItem.equalsIgnoreCase(itemName));
    }

    /**
     * Checks if a player has any of the specified items equipped by their names.
     *
     * @param player The player to check.
     * @param itemNames A list of item names to look for.
     * @return True if the player has any of the specified items equipped, false otherwise.
     */
    public static boolean hasPlayerEquippedItem(Player player, List<String> itemNames) {
        Map<KitType, String> equipment = getPlayerEquipmentNames(player);

        return equipment.values().stream()
                .anyMatch(equippedItem -> itemNames.stream().anyMatch(equippedItem::equalsIgnoreCase));
    }

    /**
     * Gets the local players current combat level
     *
     * @return
     */
    public static int getCombatLevel() {
        return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getLocalPlayer().getCombatLevel());
    }

    /**
     * Updates the last combat time when the player engages in or is hit during combat.
     */
    public static void updateCombatTime() {
        Microbot.getClientThread().runOnClientThread(() -> {
            Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer != null && localPlayer.getInteracting() != null) {
                lastCombatTime = System.currentTimeMillis();
            }
            return null;
        });
    }

    /**
     * Checks if the player is in combat based on recent activity.
     *
     * @return True if the player is in combat, false otherwise.
     */
    public static boolean isInCombat() {
        if (lastInteractWasPlayer) {
            return (System.currentTimeMillis() - lastCombatTime < COMBAT_TIMEOUT_MS) && Microbot.getVarbitPlayerValue(1075) != -1;
        }
        return System.currentTimeMillis() - lastCombatTime < COMBAT_TIMEOUT_MS;
    }

    /**
     * Gets a list of players around the local player within the combat level range 
     * and wilderness level where they can attack and be attacked.
     *
     * @return A list of players within the combat range and attackable wilderness levels.
     */
    public static List<Player> getPlayersInCombatLevelRange() {
        int localCombatLevel = getCombatLevel();
        int localWildernessLevel = 0;/*Rs2Pvp.getWildernessLevelFrom(Rs2Player.getWorldLocation());*/
        
        if (localWildernessLevel == 0) return Collections.emptyList();
        
        int localMinCombatLevel = Math.max(3, localCombatLevel - localWildernessLevel);
        int localMaxCombatLevel = Math.min(126, localCombatLevel + localWildernessLevel);

        // Filter players based on both combat level and wilderness level constraints
        return getPlayers().stream()
                .filter(player -> {
                    int playerCombatLevel = player.getCombatLevel();
                    int playerWildernessLevel = 0;/*Rs2Pvp.getWildernessLevelFrom(player.getWorldLocation());*/
                    
                    if (playerWildernessLevel == 0) return false;
                    
                    int playerMinCombatLevel = Math.max(3, playerCombatLevel - playerWildernessLevel);
                    int playerMaxCombatLevel = Math.min(126, playerCombatLevel + playerWildernessLevel);
                    
                    boolean localCanAttackPlayer = playerCombatLevel >= localMinCombatLevel && playerCombatLevel <= localMaxCombatLevel;
                    boolean playerCanAttackLocal = localCombatLevel >= playerMinCombatLevel && localCombatLevel <= playerMaxCombatLevel;

                    return localCanAttackPlayer && playerCanAttackLocal;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets the players current world location
     *
     * @return worldpoint
     */
    public static WorldPoint getWorldLocation() {
        /*if (Microbot.getClient().isInInstancedRegion()) {
            LocalPoint l = LocalPoint.fromWorld(Microbot.getClient(), Microbot.getClient().getLocalPlayer().getWorldLocation());
            WorldPoint playerInstancedWorldLocation = WorldPoint.fromLocalInstance(Microbot.getClient(), l);
            return playerInstancedWorldLocation;
        } else {*/
            return Microbot.getClient().getLocalPlayer().getWorldLocation();
        //}
    }

    /**
     * Gets the players current Rs2WorldPoint
     *
     * @return Rs2WorldPoint
     */
    public static Rs2WorldPoint getRs2WorldPoint() {
        return new Rs2WorldPoint(getWorldLocation());
    }

    /**
     * Checks if the player is near a worldpoint
     *
     * @return
     */
    public static boolean isNearArea(WorldPoint worldPoint, int distance) {
        WorldArea worldArea = new WorldArea(worldPoint, distance, distance);
        return worldArea.contains(getWorldLocation());
    }

    /**
     * Gets the player's local point (commonly used in instanced areas)
     *
     * @return localpoint
     */
    public static LocalPoint getLocalLocation() {
        return Microbot.getClient().getLocalPlayer().getLocalLocation();
    }

    /**
     * Checks if the player has full health
     *
     * @return
     */
    public static boolean isFullHealth() {
        return Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) >= Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS);
    }

    /**
     * Checks if the player is in multi-combat area
     *
     * @return
     */
    public static boolean isInMulti() {
        return Microbot.getVarbitValue(Varbits.MULTICOMBAT_AREA) == VarbitValues.INSIDE_MULTICOMBAT_ZONE.getValue();
    }

    /**
     * Drink prayer potion at prayer point level
     *
     * @param prayerPoints
     * @return
     */
    public static boolean drinkPrayerPotionAt(int prayerPoints) {
        // Check if current prayer level is below or equal to the threshold
        if (Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER) > prayerPoints) {
            return false;
        }

        // Attempt to drink a prayer potion
        if (usePotion(ItemID.PRAYER_POTION1, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION4)) {
            return true;
        }

        // Attempt to drink a super restore potion
        if (usePotion(ItemID.SUPER_RESTORE1, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE4)) {
            return true;
        }

        // If in wilderness, attempt to drink a blighted super restore potion
        if (Microbot.getVarbitValue(Varbits.IN_WILDERNESS) == 1) {
            return usePotion(ItemID.BLIGHTED_SUPER_RESTORE1, ItemID.BLIGHTED_SUPER_RESTORE2, ItemID.BLIGHTED_SUPER_RESTORE3, ItemID.BLIGHTED_SUPER_RESTORE4);
        }

        return false;
    }

    public static boolean drinkCombatPotionAt(Skill skill) {
        return drinkCombatPotionAt(skill, true);
    }

    /**
     * Drink combat potion for skill
     *
     * @param skill
     * @return
     */
    public static boolean drinkCombatPotionAt(Skill skill, boolean superCombat) {
        if (Microbot.getClient().getBoostedSkillLevel(skill) - Microbot.getClient().getRealSkillLevel(skill) > 5) {
            return false;
        }

        if (superCombat && (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE)) {
            if (usePotion(ItemID.SUPER_COMBAT_POTION1, ItemID.SUPER_COMBAT_POTION2, ItemID.SUPER_COMBAT_POTION3, ItemID.SUPER_COMBAT_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.DIVINE_SUPER_COMBAT_POTION1, ItemID.DIVINE_SUPER_COMBAT_POTION2, ItemID.DIVINE_SUPER_COMBAT_POTION3, ItemID.DIVINE_SUPER_COMBAT_POTION4)) {
                return true;
            }
        }

        if (skill == Skill.ATTACK) {
            if (usePotion(ItemID.ATTACK_POTION1, ItemID.ATTACK_POTION2, ItemID.ATTACK_POTION3, ItemID.ATTACK_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.SUPER_ATTACK1, ItemID.SUPER_ATTACK2, ItemID.SUPER_ATTACK3, ItemID.SUPER_ATTACK4)) {
                return true;
            }
        }

        if (skill == Skill.STRENGTH) {
            if (usePotion(ItemID.STRENGTH_POTION1, ItemID.STRENGTH_POTION2, ItemID.STRENGTH_POTION3, ItemID.STRENGTH_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.SUPER_STRENGTH1, ItemID.SUPER_STRENGTH2, ItemID.SUPER_STRENGTH3, ItemID.SUPER_STRENGTH4)) {
                return true;
            }
        }

        if (skill == Skill.DEFENCE) {
            if (usePotion(ItemID.DEFENCE_POTION1, ItemID.DEFENCE_POTION2, ItemID.DEFENCE_POTION3, ItemID.DEFENCE_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.SUPER_DEFENCE1, ItemID.SUPER_DEFENCE2, ItemID.SUPER_DEFENCE3, ItemID.SUPER_DEFENCE4)) {
                return true;
            }
        }

        if (skill == Skill.RANGED) {
            if (usePotion(ItemID.RANGING_POTION1, ItemID.RANGING_POTION2, ItemID.RANGING_POTION3, ItemID.RANGING_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.BASTION_POTION1, ItemID.BASTION_POTION2, ItemID.BASTION_POTION3, ItemID.BASTION_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.DIVINE_RANGING_POTION1, ItemID.DIVINE_RANGING_POTION2, ItemID.DIVINE_RANGING_POTION3, ItemID.DIVINE_RANGING_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.DIVINE_BASTION_POTION1, ItemID.DIVINE_BASTION_POTION2, ItemID.DIVINE_BASTION_POTION3, ItemID.DIVINE_BASTION_POTION4)) {
                return true;
            }
        }

        if (skill == Skill.MAGIC) {
            if (usePotion(ItemID.MAGIC_POTION1, ItemID.MAGIC_POTION2, ItemID.MAGIC_POTION3, ItemID.MAGIC_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.DIVINE_BATTLEMAGE_POTION1, ItemID.DIVINE_BATTLEMAGE_POTION2, ItemID.DIVINE_BATTLEMAGE_POTION3, ItemID.DIVINE_BATTLEMAGE_POTION4)) {
                return true;
            }
            if (usePotion(ItemID.DIVINE_MAGIC_POTION1, ItemID.DIVINE_MAGIC_POTION2, ItemID.DIVINE_MAGIC_POTION3, ItemID.DIVINE_MAGIC_POTION4)) {
                return true;
            }
            return usePotion(ItemID.BATTLEMAGE_POTION1, ItemID.BATTLEMAGE_POTION2, ItemID.BATTLEMAGE_POTION3, ItemID.BATTLEMAGE_POTION4);
        }

        return false;
    }

    /**
     * Helper method to check for the presence of any item in the provided IDs and interact with it.
     *
     * @param itemIds Array of item IDs to check in the inventory.
     * @return true if an item was found and interacted with; false otherwise.
     */
    private static boolean usePotion(Integer ...itemIds) {
        if (Rs2Inventory.contains(itemIds)) {
            Rs2Item potion = Rs2Inventory.get(itemIds);
            if (potion != null) {
                return Rs2Inventory.interact(potion, "drink");
            }
        }
        return false;
    }


    /**
     * Checks if the player has prayer points remaining
     *
     * @return
     */
    public static boolean hasPrayerPoints() {
        return Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER) > 0;
    }

    /**
     * Checks if the player is standing on a game object
     *
     * @return
     */
    public static boolean isStandingOnGameObject() {
        WorldPoint playerPoint = getWorldLocation();
        return Rs2GameObject.getGameObject(playerPoint) != null && Rs2GroundItem.getAllAt(getWorldLocation().getX(), getWorldLocation().getY()) != null;
    }

    /**
     * Checks if the player is standing on a ground item
     *
     * @return
     */
    public static boolean isStandingOnGroundItem() {
        WorldPoint playerPoint = getWorldLocation();
        return Arrays.stream(Rs2GroundItem.getAllAt(playerPoint.getX(), playerPoint.getY())).findAny().isPresent();
    }

    /**
     * Gets the player's current animation ID
     *
     * @return
     */
    public static int getAnimation() {
        if (Microbot.getClient().getLocalPlayer() == null) return -1;
        return Microbot.getClient().getLocalPlayer().getAnimation();
    }

    /**
     * Gets player's current pose animation ID
     *
     * @return
     */
    public static int getPoseAnimation() {
        return Microbot.getClient().getLocalPlayer().getPoseAnimation();
    }

    /**
     * Gets player's current QuestState for quest
     *
     * @param quest
     * @return queststate
     */
    public static QuestState getQuestState(Quest quest) {
        Client client = Microbot.getClient();
        return Microbot.getClientThread().runOnClientThread(() -> quest.getState(client));
    }

    /**
     * Gets player's real level for skill
     *
     * @param skill
     * @return level
     */
    public static int getRealSkillLevel(Skill skill) {
        return Microbot.getClient().getRealSkillLevel(skill);
    }

    /**
     * Gets player's boosted level for skill
     *
     * @param skill
     * @return level
     */
    public static int getBoostedSkillLevel(Skill skill) {
        return Microbot.getClient().getBoostedSkillLevel(skill);
    }

    /**
     * Check if the player meets the level requirement for skill
     *
     * @param skill
     * @param levelRequired
     * @param isBoosted
     * @return
     */
    public static boolean getSkillRequirement(Skill skill, int levelRequired, boolean isBoosted) {
        if (isBoosted) return getBoostedSkillLevel(skill) >= levelRequired;
        return getRealSkillLevel(skill) >= levelRequired;
    }

    /**
     * Check if the player meets the level requirement for skill
     *
     * @param skill
     * @param levelRequired
     * @return
     */
    public static boolean getSkillRequirement(Skill skill, int levelRequired) {
        return getSkillRequirement(skill, levelRequired, false);
    }

    /**
     * Checks if the player is ironman or hardcore ironman
     *
     * @return
     */
    public static boolean isIronman() {
        int accountType = Microbot.getVarbitValue(Varbits.ACCOUNT_TYPE);
        return accountType > 0 && accountType <= 3;
    }

    /**
     * Check if the player is group ironman
     *
     * @return
     */
    public static boolean isGroupIronman() {
        int accountType = Microbot.getVarbitValue(Varbits.ACCOUNT_TYPE);
        return accountType >= 4;
    }

    /**
     * Gets the players current world
     *
     * @return world
     */
    public static int getWorld() {
        return Microbot.getClient().getWorld();
    }

    /**
     * Gets the distance from current player location to endpoint using ShortestPath (does not work in instanced regions)
     *
     * @param endpoint
     * @return distance
     */
    public static int distanceTo(WorldPoint endpoint) {
        if (Microbot.getClient().isInInstancedRegion()) {
            return getWorldLocation().distanceTo(endpoint);
        }
        return Rs2Walker.getDistanceBetween(getWorldLocation(), endpoint);
    }

    /**
     * Checks whether a player is about to logout
     *
     * @param randomDelay
     * @return
     */
    public static boolean checkIdleLogout(long randomDelay) {
        long idleClientTicks = Long.min(Microbot.getClient().getMouseIdleTicks(), Microbot.getClient().getKeyboardIdleTicks());

        return  idleClientTicks >= Microbot.getClient().getIdleTimeout() - randomDelay;
    }

    /**
     * Checks wether a player is in a cave
     *
     * @return
     */
    public static boolean isInCave() {
        return Rs2Player.getWorldLocation().getY() >= 6400 && !Microbot.getClient().getTopLevelWorldView().isInstance();
    }

    public static boolean IsInInstance() {
        return Microbot.getClient().getTopLevelWorldView().isInstance();
    }

    /**
     * Returns run energy of a player in 100
     *
     * @return
     */
    public static int getRunEnergy() {
        return Microbot.getClient().getEnergy() / 100;
    }

    /**
     * Returns true if a player has stamina effect active
     *
     * @return
     */
    public static boolean hasStaminaActive() {
        return Microbot.getVarbitValue(Varbits.RUN_SLOWED_DEPLETION_ACTIVE) != 0;
    }

    public static boolean isStunned() {
        return Microbot.getClient().getLocalPlayer().hasSpotAnim(245);
    }

    /**
     * Invokes the "attack" action on the specified player.
     *
     * @param player the player to attack
     * @return true if the action was invoked successfully, false otherwise
     */
    public static boolean attack(Player player) {
        return invokeMenu(player, "attack");
    }

    /**
     * Invokes the "walk here" action to move to the same location as the specified player.
     *
     * @param player the player under whose position to walk
     * @return true if the action was invoked successfully, false otherwise
     */
    public static boolean walkUnder(Player player) {
        return invokeMenu(player, "walk here");
    }

    /**
     * Invokes the "trade with" action on the specified player.
     *
     * @param player the player to trade with
     * @return true if the action was invoked successfully, false otherwise
     */
    public static boolean trade(Player player) {
        return invokeMenu(player, "trade with");
    }

    /**
     * Invokes the "follow" action on the specified player.
     *
     * @param player the player to follow
     * @return true if the action was invoked successfully, false otherwise
     */
    public static boolean follow(Player player) {
        return invokeMenu(player, "follow");
    }

    /**
     * Executes a specific menu action on a given player.
     *
     * @param player the player to interact with
     * @param action the action to invoke (e.g., "attack", "walk here", "trade with", "follow")
     * @return true if the action was invoked successfully, false otherwise
     */
    private static boolean invokeMenu(Player player, String action) {
        if (player == null) return false;

        // Set the current status for the action being performed
        Microbot.status = action + " " + player.getName();

        // Determine the appropriate menu action based on the action string
        MenuAction menuAction = MenuAction.CC_OP;

        if (action.equalsIgnoreCase("attack")) {
            menuAction = MenuAction.PLAYER_SECOND_OPTION;
        } else if (action.equalsIgnoreCase("walk here")) {
            menuAction = MenuAction.WALK;
        } else if (action.equalsIgnoreCase("follow")) {
            menuAction = MenuAction.PLAYER_THIRD_OPTION;
        } else if (action.equalsIgnoreCase("trade with")) {
            menuAction = MenuAction.PLAYER_FOURTH_OPTION;
        }

        // Invoke the menu entry using the selected action
        Microbot.doInvoke(
                new NewMenuEntry(0, 0, menuAction.getId(), player.getId(), -1, player.getName(), player),
                Rs2UiHelper.getActorClickbox(player)
        );

        return true;
    }
}
