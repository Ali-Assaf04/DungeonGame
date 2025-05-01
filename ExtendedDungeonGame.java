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
 * Handles text‑adventure rooms & clue collection.
 * Shows a *one‑time* narrative description each time the player
 * enters a new clue room, so they always know what to do next.
 */
public class ExtendedDungeonGame {

    private final Scanner in = new Scanner(System.in);
    private final Random  rng = new Random();

    private State  state = State.CLUE_ROOM_1;
    private int    clues = 0;
    private String player = "Adventurer";

    /* first‑visit flags for room descriptions */
    private boolean firstRoom1 = true;
    private boolean firstRoom2 = true;
    private boolean firstRoom3 = true;

    /* ── accessors ── */
    public State getState()               { return state; }
    public void  setState(State s)        { state = s; }
    public void  complete()               { state = State.COMPLETED; }
    public void  setPlayerName(String n)  { player = n.isBlank() ? player : n; }

    /* ── main menu dispatcher ── */
    public void updateMenus() {
        switch (state) {
            case CLUE_ROOM_1 -> clueRoom1();
            case CLUE_ROOM_2 -> genericRoom(
                    2, State.DOOR_SHAKE_PUZZLE,
                    "A heavy **iron gate** bars your way.  "
                  + "Solve its riddle, then *shake* the sensor to lift it!",
                    firstRoom2);
            case CLUE_ROOM_3 -> genericRoom(
                    1, State.FINAL_PUZZLE,
                    "A **rune‑etched stone door** crackles with energy.  "
                  + "Answer the riddle, stay perfectly still, *then* shake "
                  + "when prompted.",
                    firstRoom3);
            default -> { } // sensor trials handled in Main
        }
    }

    /* ========== Level 1: bespoke searches ========== */
    private void clueRoom1() {
        if (firstRoom1) {
            System.out.println("""
                ── Torch‑lit Antechamber ──────────────────────────────────
                Three pieces of knowledge lie hidden here.
                Collect them all (clues 3/3), then take the rope bridge.
                Type the NUMBER of the action you want and press <Enter>.
                -----------------------------------------------------------""");
            firstRoom1 = false;
        }

        System.out.printf("""
            (%d / 3 clues found)
              1) Inspect north wall
              2) Inspect mosaic floor
              3) Inspect ceiling rafters
              4) Rest briefly
              5) Proceed to the rope bridge
              Your choice: """, clues);

        switch (in.nextLine().trim()) {
            case "1" -> wallMenu();
            case "2" -> floorMenu();
            case "3" -> ceilingMenu();
            case "4" -> System.out.println("You pause, steadying your breath.");
            case "5" -> {
                if (clues >= 3) {
                    clues = 0; state = State.ROPE_PUZZLE;
                    System.out.println("\nYou step onto the shaky rope bridge …");
                } else System.out.println("A feeling tells you clues remain.");
            }
            default -> System.out.println("Nothing happens — choose a number.");
        }
    }

    /* ---- sub‑menus for Level 1 -------------------------------------- */
    private void wallMenu() {
        System.out.println("""
            North wall:
              a) Loose torch bracket
              b) Crumbling fresco
              c) Ancient inscription
              d) Back""");
        switch (in.nextLine().trim().toLowerCase()) {
            case "a" -> addClue("a hidden scroll shard");
            case "b","c" -> System.out.println("Just cold, dusty stone.");
            default -> { }
        }
    }
    private void floorMenu() {
        System.out.println("""
            Mosaic floor:
              a) Ruby‑inlaid tile
              b) Loose flagstone
              c) Jagged crack
              d) Back""");
        switch (in.nextLine().trim().toLowerCase()) {
            case "b" -> addClue("a tarnished bronze gear");
            case "a","c" -> System.out.println("Nothing of use.");
            default -> { }
        }
    }
    private void ceilingMenu() {
        System.out.println("""
            Rafters above:
              a) Swinging chain
              b) Cobwebbed bundle
              c) Dark crevice
              d) Back""");
        switch (in.nextLine().trim().toLowerCase()) {
            case "c" -> addClue("a brittle map fragment");
            case "a","b" -> System.out.println("Only cobwebs.");
            default -> { }
        }
    }

    /* ========== Levels 2 & 3: generic two‑alcove rooms ========== */
    private void genericRoom(int needed,
                             State nextState,
                             String introAfterSuccess,
                             boolean firstVisitFlag) {

        if (firstVisitFlag) {
            System.out.println("\n" + introAfterSuccess);
            if (nextState == State.DOOR_SHAKE_PUZZLE) firstRoom2 = false;
            if (nextState == State.FINAL_PUZZLE)      firstRoom3 = false;
        }

        System.out.printf("""
            (%d / %d clues)
              1) Search left alcove
              2) Search right alcove
              3) Examine loose rubble
              4) Proceed onward
              Your choice: """, clues, needed);

        switch (in.nextLine().trim()) {
            case "1","2","3" -> {
                if (rng.nextBoolean()) addClue("a time‑worn relic");
                else System.out.println("Only dust fills your hands.");
            }
            case "4" -> {
                if (clues >= needed) {
                    clues = 0; state = nextState;
                } else System.out.println("Instinct warns you: keep searching.");
            }
            default -> System.out.println("Choose a number between 1 and 4.");
        }
    }

    private void addClue(String name) {
        clues++;
        System.out.printf("You found %s!  (%d clues total)%n", name, clues);
    }
}
