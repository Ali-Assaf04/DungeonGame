import java.util.Random;
import java.util.Scanner;

/* ── All game states ─────────────────────────────────────── */
enum State {
    WELCOME,
    CLUE_ROOM_1,  ROPE_PUZZLE,
    CLUE_ROOM_2,  DOOR_SHAKE_PUZZLE,
    CLUE_ROOM_3,  FINAL_PUZZLE,
    COMPLETED
}

/**
 * ExtendedDungeonGame.java
 * -------------------------
 * Manages the text‑adventure rooms, clue collection, and
 * transitions through the different puzzle states.
 * Maintains player progress and displays narrative menus.
 */
public class ExtendedDungeonGame {

    // Scanner for reading menu choices from the console
    private final Scanner in = new Scanner(System.in);
    // Random generator for searching outcomes in later rooms
    private final Random  rng = new Random();

    // Current game state (which room/puzzle the player is in)
    private State  state = State.CLUE_ROOM_1;
    // Number of clues found in the current room
    private int    clues = 0;
    // Player's name for personalized messages
    private String player = "Adventurer";

    // First-visit flags to control one-time room descriptions
    private boolean firstRoom1 = true;
    private boolean firstRoom2 = true;
    private boolean firstRoom3 = true;

    /* ── Accessors & Mutators ────────────────────────────── */
    /**
     * Retrieve the current game state.
     */
    public State getState() { return state; }

    /**
     * Update the game state to a new value.
     */
    public void setState(State s) { state = s; }

    /**
     * Mark the game as completed, transitioning to the COMPLETED state.
     */
    public void complete() { state = State.COMPLETED; }

    /**
     * Set the player's name if non-blank; retains previous name otherwise.
     */
    public void setPlayerName(String n) {
        if (!n.isBlank()) player = n;
    }

    /* ── Main Menu Dispatcher ───────────────────────────── */
    /**
     * Display the appropriate menu or narrative based on the current state.
     * Called once per iteration of the main loop.
     */
    public void updateMenus() {
        switch (state) {
            case CLUE_ROOM_1 -> clueRoom1();
            case CLUE_ROOM_2 -> genericRoom(
                    2, State.DOOR_SHAKE_PUZZLE,
                    "A heavy iron gate bars your way.  Solve its riddle, then shake the sensor to lift it!",
                    firstRoom2
            );
            case CLUE_ROOM_3 -> genericRoom(
                    1, State.FINAL_PUZZLE,
                    "A rune-etched stone door crackles with energy.  Answer the riddle, stay perfectly still, then shake when prompted.",
                    firstRoom3
            );
            default -> { /* ROPE_PUZZLE, puzzles and completion handled elsewhere */ }
        }
    }

    /* ========== Level 1: Custom Clue Room ========== */
    /**
     * Handles the torch-lit antechamber where the player must find 3 clues.
     * Offers a menu of inspection options and tracks progress.
     */
    private void clueRoom1() {
        if (firstRoom1) {
            System.out.println("""
                ── Torch‑lit Antechamber ──────────────────────────────────
                Three pieces of knowledge lie hidden here.
                Collect them all (clues 3/3), then take the rope bridge.
                Type the NUMBER of the action you want and press <Enter>.
                -----------------------------------------------------------""");
            firstRoom1 = false;  // Only show this intro once
        }

        // Display clue count and action choices
        System.out.printf("(%d / 3 clues found)\n"
                + "  1) Inspect north wall\n"
                + "  2) Inspect mosaic floor\n"
                + "  3) Inspect ceiling rafters\n"
                + "  4) Rest briefly\n"
                + "  5) Proceed to the rope bridge\n"
                + "Your choice: ", clues);

        // Read user choice and dispatch to sub-menu or progression
        switch (in.nextLine().trim()) {
            case "1" -> wallMenu();
            case "2" -> floorMenu();
            case "3" -> ceilingMenu();
            case "4" -> System.out.println("You pause, steadying your breath.");
            case "5" -> {
                if (clues >= 3) {
                    clues = 0;
                    state = State.ROPE_PUZZLE;
                    System.out.println("\nYou step onto the shaky rope bridge …");
                } else {
                    System.out.println("A feeling tells you clues remain.");
                }
            }
            default -> System.out.println("Nothing happens — choose a number.");
        }
    }

    /* ---- Level 1 Sub-menus ---- */
    /**
     * North wall inspection menu; one choice yields a clue.
     */
    private void wallMenu() {
        System.out.println("""
            North wall:
              a) Loose torch bracket
              b) Crumbling fresco
              c) Ancient inscription
              d) Back""");
        switch (in.nextLine().trim().toLowerCase()) {
            case "a" -> addClue("a hidden scroll shard");
            case "b", "c" -> System.out.println("Just cold, dusty stone.");
            default -> { /* 'd' or others return to main */ }
        }
    }

    /**
     * Mosaic floor inspection menu; one choice yields a clue.
     */
    private void floorMenu() {
        System.out.println("""
            Mosaic floor:
              a) Ruby‑inlaid tile
              b) Loose flagstone
              c) Jagged crack
              d) Back""");
        switch (in.nextLine().trim().toLowerCase()) {
            case "b" -> addClue("a tarnished bronze gear");
            case "a", "c" -> System.out.println("Nothing of use.");
            default -> { /* Return */ }
        }
    }

    /**
     * Ceiling rafters inspection menu; one choice yields a clue.
     */
    private void ceilingMenu() {
        System.out.println("""
            Rafters above:
              a) Swinging chain
              b) Cobwebbed bundle
              c) Dark crevice
              d) Back""");
        switch (in.nextLine().trim().toLowerCase()) {
            case "c" -> addClue("a brittle map fragment");
            case "a", "b" -> System.out.println("Only cobwebs.");
            default -> { /* Return */ }
        }
    }

    /* ========== Levels 2 & 3: Generic Two-Alcove Rooms ========== */
    /**
     * Generic room logic for clue search in two alcoves.
     * @param needed         number of clues needed to proceed
     * @param nextState      puzzle state to enter after success
     * @param introAfterSuccess Narrative shown once before searching
     * @param firstVisitFlag controls one-time intro display
     */
    private void genericRoom(int needed,
                             State nextState,
                             String introAfterSuccess,
                             boolean firstVisitFlag) {

        // Show narrative the first time entering this room
        if (firstVisitFlag) {
            System.out.println("\n" + introAfterSuccess);
            if (nextState == State.DOOR_SHAKE_PUZZLE) firstRoom2 = false;
            if (nextState == State.FINAL_PUZZLE)      firstRoom3 = false;
        }

        // Display current clue count and options
        System.out.printf("(%d / %d clues)\n"
                + "  1) Search left alcove\n"
                + "  2) Search right alcove\n"
                + "  3) Examine loose rubble\n"
                + "  4) Proceed onward\n"
                + "Your choice: ", clues, needed);

        // Randomized clue discovery or progression
        switch (in.nextLine().trim()) {
            case "1", "2", "3" -> {
                if (rng.nextBoolean()) addClue("a time-worn relic");
                else System.out.println("Only dust fills your hands.");
            }
            case "4" -> {
                if (clues >= needed) {
                    clues = 0;
                    state = nextState;
                } else {
                    System.out.println("Instinct warns you: keep searching.");
                }
            }
            default -> System.out.println("Choose a number between 1 and 4.");
        }
    }

    /**
     * Increment clue counter and display feedback.
     * @param name description of the clue found
     */
    private void addClue(String name) {
        clues++;
        System.out.printf("You found %s!  (%d clues total)%n", name, clues);
    }
}
