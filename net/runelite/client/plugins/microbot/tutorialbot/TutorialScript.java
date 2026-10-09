package net.runelite.client.plugins.microbot.tutorialbot;

import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class TutorialScript extends Script {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Random random = new Random();
    private long lastLogTime = 0;

    private Instant startTime;
    private TutorialConfig config;
    private TutorialPlugin plugin;

    // Target locations
    private static final WorldPoint SURVIVAL_LOCATION = new WorldPoint(3103, 3096, 0);
    private static final WorldPoint CHEF_LOCATION = new WorldPoint(3076, 3084, 0);
    private static final WorldPoint QUEST_LOCATION = new WorldPoint(3086, 3122, 0);
    private static final WorldPoint MINING_LOCATION = new WorldPoint(3081, 9504, 0);
    private static final WorldPoint COMBAT_LOCATION = new WorldPoint(3107, 9508, 0);
    private static final WorldPoint RAT_PIT_LOCATION = new WorldPoint(3105, 9517, 0);
    private static final WorldPoint BANK_LOCATION = new WorldPoint(3122, 3123, 0);
    private static final WorldPoint CHURCH_LOCATION = new WorldPoint(3124, 3106, 0);
    private static final WorldPoint WIZARD_LOCATION = new WorldPoint(3141, 3087, 0);

    public Instant getStartTime() {
        return startTime;
    }

    public boolean run(TutorialPlugin plugin, TutorialConfig config) {
        this.plugin = plugin;
        this.config = config;
        running.set(true);
        this.startTime = Instant.now();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !running.get()) {
                    return;
                }

                long now = System.currentTimeMillis();
                if (now - lastLogTime > 5000) {
                    lastLogTime = now;
                    Microbot.log("[TutorialBot] Stage=" + getCurrentStageName() + " | Varp=" + getTutorialVarp() + " | Status=" + Microbot.status);
                }

                handleTutorialLoop();
            } catch (Throwable ex) {
                Microbot.log("[TutorialBot] Error: " + ex.getMessage());
                if (ex.getStackTrace() != null && ex.getStackTrace().length > 0) {
                    for (int i = 0; i < Math.min(6, ex.getStackTrace().length); i++) {
                        Microbot.log("   at " + ex.getStackTrace()[i]);
                    }
                }
            }
        }, 0, 400, TimeUnit.MILLISECONDS);

        return true;
    }

    public int getTutorialVarp() {
        try {
            return Microbot.getVarbitPlayerValue(281);
        } catch (Exception e) {
            return Microbot.getClientThread().runOnClientThreadOptional(() -> {
                if (Microbot.getClient() == null) return -1;
                return Microbot.getClient().getVarpValue(281);
            }).orElse(-1);
        }
    }

    public String getCurrentStageName() {
        int varp = getTutorialVarp();
        if (varp <= 2) return "Character Creation / Guide";
        if (varp <= 10) return "Gielinor Guide";
        if (varp <= 130) return "Survival Expert";
        if (varp <= 190) return "Master Chef";
        if (varp <= 250) return "Quest Guide";
        if (varp <= 360) return "Mining & Smithing";
        if (varp <= 500) return "Combat Instructor";
        if (varp <= 532) return "Bank & Account Guide";
        if (varp <= 600) return "Brother Brace (Prayer)";
        if (varp < 1000) return "Magic Instructor";
        return "Tutorial Complete (Mainland)";
    }

    private void handleTutorialLoop() {
        int varp = getTutorialVarp();

        // Check if finished
        if (varp >= 1000 || (Microbot.getClient() != null && Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().getY() > 3200 && varp == 0)) {
            Microbot.status = "Tutorial Island Completed!";
            Microbot.log("[TutorialBot] Tutorial Island completed successfully!");
            shutdown();
            return;
        }

        // 1. Close poll booth interface if left open
        if (Rs2Widget.isWidgetVisible(928, 4)) {
            Rs2Widget.clickWidget(928, 4);
            sleep(400, 700);
        }

        // 2. Experience prompt modal ("How familiar are you with Old School RuneScape?")
        Widget expWidget = Rs2Widget.findWidget("experienced");
        if (expWidget == null) expWidget = Rs2Widget.findWidget("brand new");
        if (expWidget != null) {
            Microbot.status = "Selecting Experience Level";
            Rs2Widget.clickWidget(expWidget);
            sleep(1500, 2500);
            return;
        }

        // 3. Character Creation / Name Screen Handling (varp 1 or 2)
        if (handleCharacterCreationAndName()) {
            return;
        }

        // 3. Handle dialogues first if we are in interactive dialogue
        if (Rs2Dialogue.isInDialogue()) {
            Microbot.status = "Advancing Dialogue";
            if (Rs2Dialogue.hasDialogueOption("I am an experienced player.")) {
                Rs2Dialogue.keyPressForDialogueOption("I am an experienced player.", false);
            } else if (Rs2Dialogue.hasDialogueOption("No, I'm not planning to do that.")) {
                Rs2Dialogue.keyPressForDialogueOption("No, I'm not planning to do that.", false);
            } else if (Rs2Dialogue.hasDialogueOption("No, I'm ready to move on.")) {
                Rs2Dialogue.keyPressForDialogueOption("No, I'm ready to move on.", false);
            } else if (Rs2Dialogue.hasDialogueOption("I'm ready to move on.")) {
                Rs2Dialogue.keyPressForDialogueOption("I'm ready to move on.", false);
            } else if (Rs2Dialogue.hasDialogueOption("Yes, please.")) {
                Rs2Dialogue.keyPressForDialogueOption("Yes, please.", false);
            } else if (Rs2Dialogue.hasDialogueOption("Yes, send me to the mainland")) {
                Rs2Dialogue.keyPressForDialogueOption("Yes, send me to the mainland", false);
            } else if (Rs2Dialogue.hasDialogueOption("Yes.")) {
                Rs2Dialogue.keyPressForDialogueOption("Yes.", false);
            } else if (Rs2Dialogue.hasDialogueOption("Yes, I'm sure.")) {
                Rs2Dialogue.keyPressForDialogueOption("Yes, I'm sure.", false);
            } else if (Rs2Dialogue.hasDialogueOption("No thanks, I'm good.")) {
                Rs2Dialogue.keyPressForDialogueOption("No thanks, I'm good.", false);
            } else if (Rs2Dialogue.hasDialogueOption("No, I'm not interested.")) {
                Rs2Dialogue.keyPressForDialogueOption("No, I'm not interested.", false);
            } else if (Rs2Dialogue.hasDialogueOption("I've played before")) {
                Rs2Dialogue.keyPressForDialogueOption("I've played before", false);
            } else {
                Rs2Dialogue.clickContinue();
            }
            sleep(400, 700);
            return;
        }

        // 3. Stage-specific logic by varp
        switch (varp) {
            // --- GIELINOR GUIDE ---
            case 1:
            case 2:
                talkToNpc("Gielinor Guide");
                break;
            case 3:
                Microbot.status = "Clicking Settings Tab Stone";
                openTab(InterfaceTab.SETTINGS);
                sleep(400, 800);
                talkToNpc("Gielinor Guide");
                break;
            case 7:
                talkToNpc("Gielinor Guide");
                break;
            case 10:
                Microbot.status = "Leaving Gielinor Guide Room";
                Rs2GameObject.interact(new WorldPoint(3098, 3107, 0), "Open");
                sleep(1500, 2500);
                Rs2Walker.walkTo(SURVIVAL_LOCATION);
                break;

            // --- SURVIVAL EXPERT ---
            case 20:
                walkAndTalkToNpc(SURVIVAL_LOCATION, "Survival Expert");
                break;
            case 30:
                Microbot.status = "Opening Inventory Tab";
                openTab(InterfaceTab.INVENTORY);
                sleep(400, 800);
                talkToNpc("Survival Expert");
                break;
            case 40:
                Microbot.status = "Fishing Shrimp";
                if (!Rs2Inventory.hasItem("Raw shrimps")) {
                    if (!Rs2Player.isAnimating()) {
                        Rs2Npc.interact("Fishing spot", "Net");
                        waitCondition(() -> Rs2Inventory.hasItem("Raw shrimps"), 6000);
                    }
                }
                break;
            case 50:
                Microbot.status = "Opening Skills Tab";
                openTab(InterfaceTab.SKILLS);
                sleep(600, 1000);
                break;
            case 60:
                talkToNpc("Survival Expert");
                break;
            case 70:
            case 80:
                if (!Rs2Inventory.hasItem("Logs")) {
                    Microbot.status = "Chopping Tree";
                    if (!Rs2Player.isAnimating()) {
                        interactObject("Tree", "Chop down");
                        waitCondition(() -> Rs2Inventory.hasItem("Logs"), 6000);
                    }
                } else {
                    Microbot.status = "Lighting Fire";
                    if (!Rs2Player.isAnimating()) {
                        Rs2Inventory.combine("Tinderbox", "Logs");
                        waitCondition(() -> !Rs2Inventory.hasItem("Logs"), 6000);
                    }
                }
                break;
            case 90:
            case 100:
            case 110:
                if (Rs2Player.isAnimating()) {
                    return;
                }
                if (Rs2Inventory.hasItem("Raw shrimps", true)) {
                    Microbot.status = "Cooking Shrimp on Fire";
                    GameObject fire = Rs2GameObject.getGameObject("Fire");
                    if (fire == null && Rs2Inventory.hasItem("Logs", true) && Rs2Inventory.hasItem("Tinderbox", true)) {
                        Rs2Inventory.combine("Tinderbox", "Logs");
                        waitCondition(() -> Rs2GameObject.getGameObject("Fire") != null, 5000);
                        fire = Rs2GameObject.getGameObject("Fire");
                    } else if (fire == null && !Rs2Inventory.hasItem("Logs", true)) {
                        interactObject("Tree", "Chop down");
                        waitCondition(() -> Rs2Inventory.hasItem("Logs", true), 6000);
                    }
                    if (fire != null && !Rs2Player.isAnimating()) {
                        Rs2Inventory.use("Raw shrimps");
                        sleep(300, 600);
                        Rs2GameObject.interact(fire);
                        waitCondition(() -> Rs2Inventory.hasItem("Shrimps", true) || !Rs2Inventory.hasItem("Raw shrimps", true), 6000);
                        sleepUntil(() -> !Rs2Player.isAnimating(), 3000);
                    }
                }
                break;
            case 120:
            case 130:
                Microbot.status = "Walking to Master Chef";
                if (Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(CHEF_LOCATION) > 6) {
                    interactObject("Gate", "Open");
                    Rs2Walker.walkTo(CHEF_LOCATION);
                    waitCondition(() -> Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(CHEF_LOCATION) <= 4, 6000);
                } else {
                    interactObject("Door", "Open");
                }
                break;

            // --- MASTER CHEF ---
            case 140:
                talkToNpc("Master Chef");
                break;
            case 150:
                Microbot.status = "Making Dough";
                if (Rs2Inventory.hasItem("Bread dough", true)) {
                    return;
                }
                if (Rs2Inventory.hasItem("Pot of flour", true) && Rs2Inventory.hasItem("Bucket of water", true)) {
                    Rs2Inventory.combine("Pot of flour", "Bucket of water");
                    waitCondition(() -> Rs2Inventory.hasItem("Bread dough", true), 4000);
                    sleep(400, 800);
                }
                break;
            case 160:
                Microbot.status = "Baking Bread on Range";
                if (Rs2Player.isAnimating()) {
                    return;
                }
                if (Rs2Inventory.hasItem("Bread dough", true)) {
                    Rs2Inventory.use("Bread dough");
                    sleep(300, 600);
                    interactObject("Range", "Use");
                    waitCondition(() -> Rs2Inventory.hasItem("Bread", true) || !Rs2Inventory.hasItem("Bread dough", true), 6000);
                    sleepUntil(() -> !Rs2Player.isAnimating(), 3000);
                }
                break;
            case 170:
            case 180:
            case 190:
                Microbot.status = "Leaving Master Chef's House";
                WorldPoint chefExitDoor = new WorldPoint(3072, 3090, 0);
                WorldPoint playerPos = Rs2Player.getWorldLocation();
                if (playerPos != null && playerPos.getX() > 3072 && playerPos.getY() <= 3090) {
                    if (playerPos.distanceTo(chefExitDoor) > 4) {
                        Rs2Walker.walkTo(new WorldPoint(3073, 3090, 0));
                        sleep(600, 1000);
                    }
                    TileObject door = Rs2GameObject.findObjectByLocation(chefExitDoor);
                    if (door != null && Rs2GameObject.hasAction(door, "Open")) {
                        Rs2GameObject.interact(door, "Open");
                        sleep(1200, 1800);
                    } else {
                        Rs2Walker.walkTo(QUEST_LOCATION);
                    }
                } else {
                    Rs2Walker.walkTo(QUEST_LOCATION);
                }
                break;

            // --- QUEST GUIDE ---
            case 200:
            case 210:
            case 220:
                walkAndTalkToNpc(QUEST_LOCATION, "Quest Guide");
                break;
            case 230:
            case 240:
                Microbot.status = "Opening Quest Tab";
                openTab(InterfaceTab.QUESTS);
                sleep(600, 1000);
                talkToNpc("Quest Guide");
                break;
            case 250:
                Microbot.status = "Climbing Down Ladder to Mine";
                interactObject("Ladder", "Climb-down");
                waitCondition(() -> Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().getY() > 9000, 6000);
                break;

            // --- MINING & SMITHING ---
            case 260:
            case 270:
                walkAndTalkToNpc(MINING_LOCATION, "Mining Instructor");
                break;
            case 300:
                Microbot.status = "Mining Tin Ore";
                if (!Rs2Inventory.hasItem("Tin ore") && !Rs2Player.isAnimating()) {
                    TileObject tinRock = Rs2GameObject.findObjectById(10080);
                    if (tinRock != null) {
                        Rs2GameObject.interact(tinRock, "Mine");
                    } else {
                        interactObject("Rocks", "Mine");
                    }
                    waitCondition(() -> Rs2Inventory.hasItem("Tin ore"), 8000);
                }
                break;
            case 310:
                Microbot.status = "Mining Copper Ore";
                if (!Rs2Inventory.hasItem("Copper ore") && !Rs2Player.isAnimating()) {
                    TileObject copperRock = Rs2GameObject.findObjectById(10079);
                    if (copperRock != null) {
                        Rs2GameObject.interact(copperRock, "Mine");
                    } else {
                        interactObject("Rocks", "Mine");
                    }
                    waitCondition(() -> Rs2Inventory.hasItem("Copper ore"), 8000);
                }
                break;
            case 320:
                Microbot.status = "Smelting Bronze Bar";
                if (Rs2Inventory.hasItem("Copper ore") && Rs2Inventory.hasItem("Tin ore")) {
                    interactObject("Furnace", "Use");
                    waitCondition(() -> Rs2Inventory.hasItem("Bronze bar"), 6000);
                }
                break;
            case 330:
                talkToNpc("Mining Instructor");
                break;
            case 340:
            case 350:
                if (!Rs2Inventory.hasItem("Bronze dagger")) {
                    Microbot.status = "Smithing Bronze Dagger";
                    WorldPoint anvilPos = new WorldPoint(3083, 9499, 0);
                    WorldPoint playerLoc = Rs2Player.getWorldLocation();

                    boolean isSmithOpen = Rs2Widget.isWidgetVisible(312, 1) || Rs2Widget.isWidgetVisible(312, 9);

                    if (isSmithOpen) {
                        Microbot.status = "Clicking Bronze Dagger";
                        Widget daggerSlot = Rs2Widget.getWidget(312, 9);
                        if (daggerSlot != null) {
                            Rs2Widget.clickWidget(daggerSlot);
                        } else {
                            Rs2Widget.clickWidget(312, 9);
                        }
                        waitCondition(() -> Rs2Inventory.hasItem("Bronze dagger"), 5000);
                    } else {
                        Microbot.status = "Walking to Anvil";
                        if (playerLoc != null && playerLoc.distanceTo(anvilPos) > 3) {
                            Rs2Walker.walkTo(anvilPos);
                            sleep(600, 1000);
                        }
                        TileObject anvil = Rs2GameObject.findObjectByLocation(anvilPos);
                        if (anvil != null) {
                            Rs2GameObject.interact(anvil, "Smith");
                        } else {
                            Rs2GameObject.interact(anvilPos, "Smith");
                        }
                        waitCondition(() -> Rs2Widget.getWidget(312, 0) != null || Rs2Widget.getWidget(312, 9) != null, 5000);
                    }
                }
                break;
            case 360:
                Microbot.status = "Entering Combat Area";
                WorldPoint combatGate = new WorldPoint(3094, 9502, 0);
                WorldPoint pLoc = Rs2Player.getWorldLocation();
                if (pLoc != null && pLoc.getX() < 3095) {
                    if (pLoc.distanceTo(combatGate) > 4) {
                        Rs2Walker.walkTo(combatGate);
                        sleep(600, 1000);
                    }
                    TileObject gate = Rs2GameObject.findObjectByLocation(combatGate);
                    if (gate != null && Rs2GameObject.hasAction(gate, "Open")) {
                        Rs2GameObject.interact(gate, "Open");
                        sleep(1200, 1800);
                    } else {
                        Rs2Walker.walkTo(COMBAT_LOCATION);
                    }
                } else {
                    Rs2Walker.walkTo(COMBAT_LOCATION);
                }
                break;

            // --- COMBAT INSTRUCTOR ---
            case 370:
            case 380:
                walkAndTalkToNpc(COMBAT_LOCATION, "Combat Instructor");
                break;
            case 390:
                Microbot.status = "Opening Equipment Tab";
                openTab(InterfaceTab.EQUIPMENT);
                sleep(600, 1000);
                break;
            case 400:
                Microbot.status = "Viewing Equipment Stats";
                Rs2Widget.clickWidget("View equipment stats");
                sleep(600, 1000);
                break;
            case 405:
                Microbot.status = "Equipping Bronze Dagger";
                Rs2Inventory.wield("Bronze dagger");
                sleep(600, 1000);
                if (Rs2Widget.isWidgetVisible(84, 3)) {
                    Rs2Widget.clickWidget(84, 3);
                }
                break;
            case 410:
                talkToNpc("Combat Instructor");
                break;
            case 420:
                Microbot.status = "Equipping Sword & Shield";
                Rs2Inventory.wield("Bronze sword");
                Rs2Inventory.wield("Wooden shield");
                sleep(600, 1000);
                break;
            case 430:
                Microbot.status = "Opening Combat Tab";
                openTab(InterfaceTab.COMBAT);
                sleep(600, 1000);
                break;
            case 440:
            case 450:
            case 460:
                if (Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(RAT_PIT_LOCATION) > 6) {
                    Microbot.status = "Entering Rat Pit";
                    interactObject("Gate", "Open");
                    Rs2Walker.walkTo(RAT_PIT_LOCATION);
                    waitCondition(() -> Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(RAT_PIT_LOCATION) <= 4, 6000);
                } else {
                    Microbot.status = "Attacking Giant Rat (Melee)";
                    if (!Rs2Player.isInCombat()) {
                        Rs2Npc.interact("Giant rat", "Attack");
                        waitCondition(Rs2Player::isInCombat, 4000);
                    }
                }
                break;
            case 470:
                Microbot.status = "Leaving Rat Pit to Talk to Instructor";
                if (Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(COMBAT_LOCATION) > 6) {
                    interactObject("Gate", "Open");
                    Rs2Walker.walkTo(COMBAT_LOCATION);
                    waitCondition(() -> Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(COMBAT_LOCATION) <= 4, 6000);
                }
                talkToNpc("Combat Instructor");
                break;
            case 480:
            case 490:
                Microbot.status = "Attacking Giant Rat (Ranged)";
                Rs2Inventory.wield("Shortbow");
                Rs2Inventory.wield("Bronze arrow");
                if (!Rs2Player.isInCombat()) {
                    Rs2Npc.interact("Giant rat", "Attack");
                    waitCondition(Rs2Player::isInCombat, 4000);
                }
                break;
            case 500:
                Microbot.status = "Climbing Up Ladder to Surface";
                interactObject("Ladder", "Climb-up");
                waitCondition(() -> Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().getY() < 9000, 6000);
                break;

            // --- BANK & ACCOUNT GUIDE ---
            case 510:
                Microbot.status = "Walking to Bank Booth";
                if (Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(BANK_LOCATION) > 6) {
                    Rs2Walker.walkTo(BANK_LOCATION);
                    waitCondition(() -> Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(BANK_LOCATION) <= 4, 6000);
                } else {
                    interactObject("Bank booth", "Use");
                    waitCondition(Rs2Bank::isOpen, 5000);
                }
                break;
            case 520:
                if (Rs2Bank.isOpen()) {
                    Rs2Bank.closeBank();
                    sleep(400, 800);
                }
                Microbot.status = "Using Poll Booth";
                TileObject pollBooth = Rs2GameObject.findObjectById(26815);
                if (pollBooth != null) {
                    Rs2GameObject.interact(pollBooth, "Use");
                } else {
                    interactObject("Poll booth", "Use");
                }
                waitCondition(() -> Rs2Widget.isWidgetVisible(928, 4), 3000);
                if (Rs2Widget.isWidgetVisible(928, 4)) {
                    Rs2Widget.clickWidget(928, 4);
                    sleep(400, 800);
                }
                break;
            case 525:
            case 530:
                if (Rs2Widget.isWidgetVisible(928, 4)) {
                    Rs2Widget.clickWidget(928, 4);
                    sleep(400, 800);
                }
                talkToNpc("Account Guide");
                break;
            case 531:
                Microbot.status = "Opening Account Management Tab";
                openTab(InterfaceTab.ACC_MAN);
                sleep(600, 1000);
                break;
            case 532:
                talkToNpc("Account Guide");
                if (!Rs2Dialogue.isInDialogue()) {
                    interactObject("Door", "Open");
                }
                break;

            // --- BROTHER BRACE (PRAYER & FRIENDS) ---
            case 540:
            case 550:
                walkAndTalkToNpc(CHURCH_LOCATION, "Brother Brace");
                break;
            case 560:
                Microbot.status = "Opening Prayer Tab";
                openTab(InterfaceTab.PRAYER);
                sleep(600, 1000);
                break;
            case 570:
                talkToNpc("Brother Brace");
                break;
            case 580:
                Microbot.status = "Opening Friends Tab";
                openTab(InterfaceTab.FRIENDS);
                sleep(600, 1000);
                break;
            case 590:
            case 600:
                talkToNpc("Brother Brace");
                if (!Rs2Dialogue.isInDialogue()) {
                    interactObject("Door", "Open");
                }
                break;

            // --- MAGIC INSTRUCTOR & MAINLAND TELEPORT ---
            case 610:
            case 620:
                walkAndTalkToNpc(WIZARD_LOCATION, "Magic Instructor");
                break;
            case 630:
                Microbot.status = "Opening Magic Tab";
                openTab(InterfaceTab.MAGIC);
                sleep(600, 1000);
                break;
            case 640:
                talkToNpc("Magic Instructor");
                break;
            case 650:
                Microbot.status = "Casting Wind Strike on Chicken";
                NPC chicken = Rs2Npc.getNpc("Chicken");
                if (chicken != null) {
                    if (Rs2Widget.clickWidget(218, 11)) {
                        sleep(400, 800);
                        Rs2Npc.interact(chicken, "Cast");
                        sleep(2000, 3000);
                    } else {
                        Rs2Magic.castOn(MagicAction.WIND_STRIKE, chicken);
                        sleep(2000, 3000);
                    }
                }
                break;
            case 670:
            case 671:
                Microbot.status = "Talking to Magic Instructor (Accepting Teleport)";
                talkToNpc("Magic Instructor");
                break;
            case 680:
                Microbot.status = "Casting Home Teleport to Mainland";
                if (Rs2Tab.getCurrentTab() != InterfaceTab.MAGIC) {
                    Rs2Tab.switchToMagicTab();
                    sleep(400, 800);
                }
                Widget teleportWidget = Rs2Widget.getWidget(218, 6);
                if (teleportWidget != null && Rs2Widget.isWidgetVisible(218, 6)) {
                    Rs2Widget.clickWidget(teleportWidget);
                    waitCondition(() -> getTutorialVarp() >= 1000 || (Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().getY() > 3200), 20000);
                }
                break;

            default:
                Microbot.status = "Handling Step (Varp " + varp + ")";
                Rs2Dialogue.clickContinue();
                break;
        }
    }

    private boolean handleCharacterCreationAndName() {
        int varp = getTutorialVarp();
        if (varp > 2 && !Rs2Widget.isWidgetVisible(679, 0) && !Rs2Widget.isWidgetVisible(558, 0)) {
            return false;
        }

        // 1. Character Appearance Customization Screen (Widget 679)
        if (Rs2Widget.isWidgetVisible(679, 0)) {
            Microbot.status = "Randomizing Character Appearance";
            // Randomize gender (6 = Male, 7 = Female)
            boolean isFemale = random.nextBoolean();
            Rs2Widget.clickWidget(679, isFemale ? 7 : 6);
            sleep(400, 800);

            // Each pair is [prevArrow, nextArrow] for a specific feature:
            // Hair (12, 13), Beard (14, 15 - only if male), Torso (20, 21), Arms (24, 25),
            // Hands (28, 29), Legs (32, 33), Feet (36, 37),
            // Hair Color (43, 44), Torso Color (47, 48), Legs Color (51, 52), Feet Color (55, 56), Skin Color (59, 60)
            int[][] featureArrows = isFemale ? new int[][]{
                    {12, 13}, // Hair style
                    {20, 21}, // Torso style
                    {24, 25}, // Arms style
                    {28, 29}, // Wrists/Hands style
                    {32, 33}, // Legs style
                    {36, 37}, // Feet style
                    {43, 44}, // Hair color
                    {47, 48}, // Torso color
                    {51, 52}, // Legs color
                    {55, 56}, // Feet color
                    {59, 60}  // Skin color
            } : new int[][]{
                    {12, 13}, // Hair style
                    {14, 15}, // Beard style
                    {20, 21}, // Torso style
                    {24, 25}, // Arms style
                    {28, 29}, // Wrists/Hands style
                    {32, 33}, // Legs style
                    {36, 37}, // Feet style
                    {43, 44}, // Hair color
                    {47, 48}, // Torso color
                    {51, 52}, // Legs color
                    {55, 56}, // Feet color
                    {59, 60}  // Skin color
            };

            for (int[] arrowPair : featureArrows) {
                // Randomly choose left or right arrow
                int arrowId = arrowPair[random.nextInt(2)];
                // Roll 1 to 4 clicks per feature to ensure deep variety across all features
                int clicksForFeature = 1 + random.nextInt(4);
                for (int c = 0; c < clicksForFeature; c++) {
                    Rs2Widget.clickWidget(679, arrowId);
                    sleep(350, 650);
                }
                // Short human pause between adjusting different body parts
                if (random.nextInt(3) == 0) {
                    sleep(400, 750);
                }
            }

            sleep(700, 1300);
            Microbot.status = "Confirming Appearance";
            if (!Rs2Widget.clickWidget("Accept")) {
                if (!Rs2Widget.clickWidget("Confirm")) {
                    Rs2Widget.clickWidget(679, 68);
                }
            }
            sleep(2000, 3500);
            return true;
        }

        // 2. Name Selection Screen (Widget 558)
        if (Rs2Widget.isWidgetVisible(558, 0)) {
            // A. If "Set name" button is visible and active, click it
            if (Rs2Widget.hasWidget("Set name") && Rs2Widget.clickWidget("Set name")) {
                Microbot.status = "Setting Name";
                sleep(2000, 3500);
                return true;
            }

            // B. If name was taken, click suggested name
            if (Rs2Widget.hasWidget("Not available") || Rs2Widget.hasWidget("Please pick another name")) {
                Microbot.status = "Name taken, picking suggested name";
                if (Rs2Widget.clickWidget(558, 15) || Rs2Widget.clickWidget(558, 14) || Rs2Widget.clickWidget(558, 13)) {
                    sleep(1000, 1800);
                    Rs2Widget.clickWidget("Set name");
                    sleep(2000, 3500);
                    return true;
                }
            }

            // C. If name needs to be entered (input box 558, 7)
            Widget nameInputBox = Rs2Widget.getWidget(558, 7);
            if (nameInputBox != null) {
                Microbot.status = "Entering Character Name";
                Rs2Widget.clickWidget(558, 7);
                sleep(500, 900);
                String nameToUse = (config != null && config.characterName() != null && !config.characterName().isBlank())
                        ? config.characterName()
                        : generateRandomName();
                Rs2Keyboard.typeString(nameToUse);
                sleep(300, 600);
                Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);
                sleep(800, 1500);
                Rs2Widget.clickWidget("Look up name");
                sleep(2000, 3000);
                if (Rs2Widget.clickWidget("Set name")) {
                    sleep(2000, 3500);
                }
                return true;
            }

            if (Rs2Widget.clickWidget("Look up name")) {
                Microbot.status = "Looking Up Name";
                sleep(2000, 3000);
                if (Rs2Widget.clickWidget("Set name")) {
                    sleep(2000, 3500);
                }
                return true;
            }
        }

        return false;
    }

    private void talkToNpc(String npcName) {
        Microbot.status = "Talking to " + npcName;
        NPC npc = Rs2Npc.getNpc(npcName);
        if (npc != null) {
            Rs2Npc.interact(npc, "Talk-to");
            waitCondition(Rs2Dialogue::isInDialogue, 4000);
        }
    }

    private void walkAndTalkToNpc(WorldPoint location, String npcName) {
        if (Rs2Player.getWorldLocation() != null && location != null && Rs2Player.getWorldLocation().distanceTo(location) > 5) {
            Microbot.status = "Walking to " + npcName;
            Rs2Walker.walkTo(location);
            waitCondition(() -> Rs2Player.getWorldLocation() != null && Rs2Player.getWorldLocation().distanceTo(location) <= 4, 8000);
        } else {
            talkToNpc(npcName);
        }
    }

    private void interactObject(String objectName, String action) {
        Microbot.status = action + " " + objectName;
        GameObject obj = Rs2GameObject.getGameObject(objectName);
        if (obj != null) {
            Rs2GameObject.interact(obj, action);
        } else {
            Rs2GameObject.interact(objectName, action);
        }
        sleep(800, 1400);
    }

    private String generateRandomName() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        int len = 8 + random.nextInt(4);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void openTab(InterfaceTab tab) {
        Widget w = null;
        switch (tab) {
            case SETTINGS:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE11);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE11);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE11);
                if (w == null) w = Rs2Widget.getWidget(164, 41);
                if (w == null) w = Rs2Widget.getWidget(548, 41);
                if (w == null) w = Rs2Widget.getWidget(548, 52);
                break;
            case INVENTORY:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE3);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE3);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE3);
                if (w == null) w = Rs2Widget.getWidget(164, 55);
                if (w == null) w = Rs2Widget.getWidget(548, 55);
                if (w == null) w = Rs2Widget.getWidget(548, 67);
                break;
            case SKILLS:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE1);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE1);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE1);
                if (w == null) w = Rs2Widget.getWidget(164, 53);
                if (w == null) w = Rs2Widget.getWidget(548, 53);
                break;
            case QUESTS:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE2);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE2);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE2);
                if (w == null) w = Rs2Widget.getWidget(164, 54);
                if (w == null) w = Rs2Widget.getWidget(548, 54);
                break;
            case EQUIPMENT:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE4);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE4);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE4);
                if (w == null) w = Rs2Widget.getWidget(164, 56);
                if (w == null) w = Rs2Widget.getWidget(548, 56);
                break;
            case PRAYER:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE5);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE5);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE5);
                if (w == null) w = Rs2Widget.getWidget(164, 57);
                if (w == null) w = Rs2Widget.getWidget(548, 57);
                break;
            case MAGIC:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE6);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE6);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE6);
                if (w == null) w = Rs2Widget.getWidget(164, 58);
                if (w == null) w = Rs2Widget.getWidget(548, 58);
                break;
            case FRIENDS:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE9);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE9);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE9);
                if (w == null) w = Rs2Widget.getWidget(164, 40);
                if (w == null) w = Rs2Widget.getWidget(548, 40);
                break;
            case ACC_MAN:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE8);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE8);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE8);
                if (w == null) w = Rs2Widget.getWidget(164, 39);
                if (w == null) w = Rs2Widget.getWidget(548, 39);
                break;
            case EMOTES:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE12);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE12);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE12);
                if (w == null) w = Rs2Widget.getWidget(164, 42);
                if (w == null) w = Rs2Widget.getWidget(548, 42);
                break;
            case MUSIC:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE13);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE13);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE13);
                if (w == null) w = Rs2Widget.getWidget(164, 43);
                if (w == null) w = Rs2Widget.getWidget(548, 43);
                break;
            case COMBAT:
                w = Rs2Widget.getWidget(InterfaceID.Toplevel.STONE0);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelPreEoc.STONE0);
                if (w == null) w = Rs2Widget.getWidget(InterfaceID.ToplevelOsrsStretch.STONE0);
                if (w == null) w = Rs2Widget.getWidget(164, 52);
                if (w == null) w = Rs2Widget.getWidget(548, 52);
                break;
            default:
                break;
        }
        if (w != null && Rs2Widget.isWidgetVisible(w.getId()) && w.getWidth() > 0) {
            Rs2Widget.clickWidget(w);
            sleep(400, 700);
        } else {
            Rs2Tab.switchTo(tab);
            sleep(400, 700);
        }
    }

    private boolean waitCondition(BooleanSupplier condition, int timeoutMs) {
        return sleepUntil(() -> !isRunning() || condition.getAsBoolean(), timeoutMs) && isRunning();
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void shutdown() {
        running.set(false);
        super.shutdown();
    }
}
