import java.util.*;
import jssc.SerialPortException;

public class Main {
    // COM port for your PC-side XBee
    private static final String PORT = "COM5";

    // Motion detection thresholds
    private static final int CX = 515, CY = 505, CZ = 404;
    private static final int STILL_BAND  = 50;
    private static final int SHAKE_DELTA = 80;

    // Riddles for each sensor puzzle
    private static final Map<State,String[]> RIDDLES = Map.of(
        State.ROPE_PUZZLE,      new String[]{
            "I have no wings yet I fly, no teeth yet I bite. What am I?",
            "wind"
        },
        State.DOOR_SHAKE_PUZZLE, new String[]{
            "What can you break, even if you never pick it up or touch it?",
            "promise"
        },
        State.FINAL_PUZZLE,     new String[]{
            "The more you take, the more you leave behind. What are they?",
            "footsteps"
        }
    );

    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System.in);

        // — Splash screen —
        System.out.println("""
            ╔══════════════════════════════════════╗
            ║       THE CRYPT OF SHIFTING STONES   ║
            ╚══════════════════════════════════════╝
            HOW TO PLAY
            ───────────
            • Solve riddles to proceed.
            • Then perform a sensor motion (still or shake).
            Press <Enter> to begin…""");
        in.nextLine();

        // — Get player name —
        System.out.print("Enter your name, brave explorer: ");
        String playerName = in.nextLine().trim();
        if (playerName.isBlank()) playerName = "Adventurer";

        // — Open serial & handshake —
        SerialPortHandle serial = new SerialPortHandle(PORT);
        System.out.println("Press <Enter> to start sensor link…");
        in.nextLine();
        serial.writeByte((byte)0x01);

        // — Initialize game —
        ExtendedDungeonGame game = new ExtendedDungeonGame();
        game.setPlayerName(playerName);

        StringBuilder lineBuf = new StringBuilder();
        List<int[]> samples = new ArrayList<>();

        long  timer       = 0;
        boolean finalPhase  = false;
        boolean riddleSolved = false;
        boolean bannerShown  = false;

        // — Main game loop —
        while (game.getState() != State.COMPLETED) {
            // 1) Read & filter serial bytes
            int b = serial.read();
            if (b >= 0) {
                char c = (char)b;
                if ((c >= '0' && c <= '9') || c == ',') {
                    lineBuf.append(c);
                } else if (c == '\n' || c == '\r') {
                    String line = lineBuf.toString();
                    lineBuf.setLength(0);
                    if (line.matches("\\d+,\\d+,\\d+")) {
                        try {
                            samples.add(parseXYZ(line));
                        } catch (NumberFormatException ignored) { }
                    }
                }
                // else: drop any other byte
            }

            // 2) Update menu & possibly pose riddle/banner
            game.updateMenus();
            if (isPuzzle(game.getState()) && !riddleSolved) {
                askRiddle(in, game.getState());
                riddleSolved = true;
            }
            if (isPuzzle(game.getState()) && riddleSolved && !bannerShown) {
                showSensorBanner(game.getState());
                bannerShown = true;
            }
            if (samples.isEmpty() || !riddleSolved) continue;

            // 3) Consume one sample & drive puzzle logic
            int[] v = samples.remove(0);
            switch (game.getState()) {
                case ROPE_PUZZLE -> {
                    if (timer == 0) timer = now();
                    if (isStill(v) && elapsed(timer) > 3000) {
                        System.out.println("» Balanced! You cross the bridge.");
                        game.setState(State.CLUE_ROOM_2);
                        timer = 0; riddleSolved = false; bannerShown = false;
                    }
                    if (!isStill(v)) timer = now();
                }
                case DOOR_SHAKE_PUZZLE -> {
                    if (timer == 0) timer = now();
                    if (isShake(v) && elapsed(timer) > 3000) {
                        System.out.println("» The heavy gate clanks open!");
                        game.setState(State.CLUE_ROOM_3);
                        timer = 0; riddleSolved = false; bannerShown = false;
                    }
                    if (!isShake(v)) timer = now();
                }
                case FINAL_PUZZLE -> {
                    if (!finalPhase) {
                        if (timer == 0) timer = now();
                        if (isStill(v) && elapsed(timer) > 1000) {
                            System.out.println("Lock primed! NOW SHAKE!");
                            finalPhase = true; timer = now();
                        }
                        if (!isStill(v)) timer = now();
                    } else {
                        if (isShake(v) && elapsed(timer) > 3000) {
                            System.out.printf("""
                                ╔═══════════════════════════╗
                                ║ Victory, %s! You escape! ║
                                ╚═══════════════════════════╝%n""",
                                playerName);
                            game.complete();
                        }
                        if (!isShake(v)) timer = now();
                    }
                }
                default -> { /* no sensor logic */ }
            }
        }

        serial.close();
        System.out.println("Thank you for playing!");
    }

    // — Helper methods below —

    private static void askRiddle(Scanner in, State s) {
        String[] qa = RIDDLES.get(s);
        String q = qa[0], a = qa[1];
        while (true) {
            System.out.println("\nRIDDLE: " + q);
            System.out.print("Your answer: ");
            String r = in.nextLine().trim().toLowerCase(Locale.ROOT);
            if (r.equals(a)) {
                System.out.println("» Correct!\n");
                return;
            }
            System.out.println("Wrong answer — try again.");
        }
    }

    private static void showSensorBanner(State s) {
        switch (s) {
            case ROPE_PUZZLE -> System.out.println("""
                ╔═ Rope-Bridge Sensor Challenge ═╗
                Keep the sensor ABSOLUTELY STILL for 3 seconds.
                (All three axes must stay within ±50 of their centres.)""");
            case DOOR_SHAKE_PUZZLE -> System.out.println("""
                ╔═ Gate-Lift Sensor Challenge ═╗
                Shake the sensor so that ANY axis deviates by >80 counts,
                and keep shaking for a full 3 seconds.""");
            case FINAL_PUZZLE -> System.out.println("""
                ╔═ Final Seal Sensor Challenge ═╗
                1) Hold sensor still for 1 second
                2) Then shake vigorously for 3 seconds""");
            default -> {}
        }
    }

    private static boolean isPuzzle(State s) {
        return s == State.ROPE_PUZZLE
            || s == State.DOOR_SHAKE_PUZZLE
            || s == State.FINAL_PUZZLE;
    }

    private static int[] parseXYZ(String line) {
        String[] p = line.split(",", 3);
        return new int[]{
            Integer.parseInt(p[0]),
            Integer.parseInt(p[1]),
            Integer.parseInt(p[2])
        };
    }

    private static boolean isStill(int[] v) {
        return Math.abs(v[0] - CX) < STILL_BAND
            && Math.abs(v[1] - CY) < STILL_BAND
            && Math.abs(v[2] - CZ) < STILL_BAND;
    }

    private static boolean isShake(int[] v) {
        return Math.abs(v[0] - CX) > SHAKE_DELTA
            || Math.abs(v[1] - CY) > SHAKE_DELTA
            || Math.abs(v[2] - CZ) > SHAKE_DELTA;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static long elapsed(long t) {
        return now() - t;
    }
}
