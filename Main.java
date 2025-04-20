import java.util.*;

/**
 *  Main.java
 *  -------------
 *  Entry point and main loop for The Crypt of Shifting Stones game.
 *  Handles initialization, serial I/O, menu updates, riddle prompting,
 *  sensor-based motion detection, and state transitions.
 */
public class Main {

    /* ---------- COM port configuration ---------- */
    // Port name for serial connection (virtual COM over USB/XBee)
    private static final String PORT = "COM8";

    /* ---------- Motion thresholds and centers ---------- */
    // Center (idle) readings for X, Y, Z axes
    private static final int CX = 500, CY = 500, CZ = 400;
    // Thresholds: within ±STILL_BAND = "still"; deviations > SHAKE_DELTA = "shake"
    private static final int STILL_BAND  = 50;
    private static final int SHAKE_DELTA = 80;

    /* ---------- Riddle questions and answers ---------- */
    // Maps each puzzle state to a two-element array: {question, answer}
    private static final Map<State, String[]> RIDDLES = Map.of(
        State.ROPE_PUZZLE, new String[]{
            "I have no wings yet I fly, no teeth yet I bite.  What am I?",
            "wind" },
        State.DOOR_SHAKE_PUZZLE, new String[]{
            "What can you break, even if you never pick it up or touch it?",
            "promise" },
        State.FINAL_PUZZLE, new String[]{
            "The more you take, the more you leave behind.  What are they?",
            "footsteps" }
    );

    /**
     *  Main entry point: sets up serial connection, game objects,
     *  and runs the main loop until the game is completed.
     */
    public static void main(String[] args) throws Exception {

        Scanner userIn = new Scanner(System.in);

        /* -- Welcome splash screen -- */
        System.out.println("""
              ╔══════════════════════════════════════╗
              ║    THE  CRYPT  OF  SHIFTING  STONES  ║
              ╚══════════════════════════════════════╝

              HOW TO PLAY
              ───────────
                • Collect clues by typing menu numbers and pressing <Enter>.
                • Each trial presents a RIDDLE you must solve to continue.
                • After a correct answer, perform the SENSOR MOTION:
                    - Hold STILL (±50)     or
                    - SHAKE vigorously (>80)
                  as instructed.

              Press <Enter> to begin…""");
        userIn.nextLine();  // Wait for user to press Enter

        // Prompt for player name; default to "Adventurer" if blank
        System.out.print("Enter your name, brave explorer: ");
        String playerName = userIn.nextLine().trim();
        if (playerName.isBlank()) playerName = "Adventurer";

        // Set up serial port and game state
        SerialPortHandle serial = new SerialPortHandle(PORT);
        ExtendedDungeonGame game        = new ExtendedDungeonGame();
        game.setPlayerName(playerName);

        // Buffer for incoming serial lines and parsed samples
        StringBuilder lineBuf = new StringBuilder();
        List<int[]>   samples = new ArrayList<>();

        long    timer = 0;
        boolean finalPhase = false;   // Tracks sub-phase in FINAL_PUZZLE
        boolean riddleSolved = false; // Ensures riddle runs once per state
        boolean bannerShown  = false; // Ensures sensor instructions show once

        // Main loop: run until game reaches COMPLETED state
        while (game.getState() != State.COMPLETED) {

            /* -- Read one byte from serial port -- */
            int b = serial.read();
            if (b >= 0) {
                char c = (char) b;
                // Accumulate until newline, then parse XYZ values
                if (c == '\n' || c == '\r') {
                    if (!lineBuf.isEmpty()) {
                        samples.add(parseXYZ(lineBuf.toString()));
                        lineBuf.setLength(0);
                    }
                } else lineBuf.append(c);
            }

            // Update text-adventure menus based on current state
            game.updateMenus();

            /* -- Riddle stage: block until puzzle answer is correct -- */
            if (isPuzzle(game.getState()) && !riddleSolved) {
                askRiddle(userIn, game.getState());
                riddleSolved = true;   // Next, show sensor instructions
            }

            /* -- Show sensor instruction banner once per puzzle -- */
            if (isPuzzle(game.getState()) && riddleSolved && !bannerShown) {
                showSensorBanner(game.getState());
                bannerShown = true;
            }

            /* -- Sensor logic: skip until we have a sample and riddle solved -- */
            if (samples.isEmpty() || !riddleSolved) continue;
            int[] v = samples.remove(0);

            // Handle motion detection depending on puzzle state
            switch (game.getState()) {

                case ROPE_PUZZLE -> {
                    // Still for 3 seconds to cross rope bridge
                    if (timer == 0) timer = now();
                    if (isStill(v) && elapsed(timer) > 3000) {
                        System.out.println("» Balanced! You cross the bridge.");
                        advanceTo(game, State.CLUE_ROOM_2);
                        timer = 0; finalPhase = false;
                        riddleSolved = false; bannerShown = false;
                    }
                    if (!isStill(v)) timer = now();
                }

                case DOOR_SHAKE_PUZZLE -> {
                    // Shake for 3 seconds to lift gate
                    if (timer == 0) timer = now();
                    if (isShake(v) && elapsed(timer) > 3000) {
                        System.out.println("» The heavy gate clanks open!");
                        advanceTo(game, State.CLUE_ROOM_3);
                        timer = 0; finalPhase = false;
                        riddleSolved = false; bannerShown = false;
                    }
                    if (!isShake(v)) timer = now();
                }

                case FINAL_PUZZLE -> {
                    // Final: still 1s then shake 3s
                    if (!finalPhase) {
                        if (timer == 0) timer = now();
                        if (isStill(v) && elapsed(timer) > 1000) {
                            System.out.println("Lock primed!  NOW SHAKE!");
                            finalPhase = true; timer = now();
                        }
                        if (!isStill(v)) timer = now();
                    } else {
                        if (isShake(v) && elapsed(timer) > 3000) {
                            System.out.printf("""
                                    ╔═══════════════════════════╗
                                    ║  Victory, %s! You escape!  ║
                                    ╚═══════════════════════════╝%n""", playerName);
                            game.complete();
                        }
                        if (!isShake(v)) timer = now();
                    }
                }

                default -> { /* No sensor logic in other states */ }
            }
        }

        // Clean up serial port and exit
        serial.close();
        System.out.println("Thank you for playing!");
    }

    // ---------- Helper methods ----------

    /**
     * Prompt and validate the riddle answer. Loops until correct.
     */
    private static void askRiddle(Scanner in, State puzzle) {
        String[] qa = RIDDLES.get(puzzle);
        if (qa == null) return;
        String question = qa[0], answer = qa[1];

        while (true) {
            System.out.println("\nRIDDLE: " + question);
            System.out.print("Your answer: ");
            String reply = in.nextLine().trim().toLowerCase(Locale.ROOT);
            if (reply.equals(answer)) {
                System.out.println("» Correct!\n");
                return;
            }
            System.out.println("Wrong answer — try again.");
        }
    }

    /**
     * Displays the motion instructions banner for the given puzzle state.
     */
    private static void showSensorBanner(State s) {
        switch (s) {
            case ROPE_PUZZLE -> System.out.println("""
                ╔═ Rope‑Bridge Sensor Challenge ═╗
                Keep the sensor ABSOLUTELY STILL for 3 seconds.
                (All three axes must stay within ±50 of their centres.)""");
            case DOOR_SHAKE_PUZZLE -> System.out.println("""
                ╔═ Gate‑Lift Sensor Challenge ═╗
                Shake the sensor so that ANY axis deviates by >80 counts,
                and keep shaking for a full 3 seconds.""");
            case FINAL_PUZZLE -> System.out.println("""
                ╔═ Final Seal Sensor Challenge ═╗
                1) Hold sensor still for 1 second
                2) Then shake vigorously for 3 seconds""");
            default -> { /* No banner for non-puzzle states */ }
        }
    }

    /**
     * Returns true if the state is one of the puzzle states needing sensor input.
     */
    private static boolean isPuzzle(State s) {
        return s == State.ROPE_PUZZLE ||
               s == State.DOOR_SHAKE_PUZZLE ||
               s == State.FINAL_PUZZLE;
    }

    /**
     * Advance the game to the next state.
     */
    private static void advanceTo(ExtendedDungeonGame g, State nextRoom) {
        g.setState(nextRoom);
    }

    // ---------- Sensor data parsers and checks ----------

    /**
     * Parse a line "x,y,z" into an integer array [x, y, z].
     */
    private static int[] parseXYZ(String s){
        String[] p = s.trim().split(",");
        return new int[]{ Integer.parseInt(p[0].trim()),
                          Integer.parseInt(p[1].trim()),
                          Integer.parseInt(p[2].trim()) };
    }

    /**
     * Returns true if all three axes are within STILL_BAND of their centres.
     */
    private static boolean isStill(int[] v){
        return Math.abs(v[0]-CX)<STILL_BAND &&
               Math.abs(v[1]-CY)<STILL_BAND &&
               Math.abs(v[2]-CZ)<STILL_BAND;
    }

    /**
     * Returns true if any axis deviates by more than SHAKE_DELTA.
     */
    private static boolean isShake(int[] v){
        return Math.abs(v[0]-CX)>SHAKE_DELTA ||
               Math.abs(v[1]-CY)>SHAKE_DELTA ||
               Math.abs(v[2]-CZ)>SHAKE_DELTA;
    }

    /**
     * Get current system time in milliseconds.
     */
    private static long now(){ return System.currentTimeMillis(); }

    /**
     * Compute elapsed time since timestamp t.
     */
    private static long elapsed(long t){ return now()-t; }
}
