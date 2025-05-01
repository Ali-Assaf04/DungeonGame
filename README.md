# Dungeon Escape Game

This project implements a fully wireless, motion-driven “Dungeon Escape” game using an Arduino-based accelerometer, two XBee radio modules, and a Java application. Players solve riddles by holding or shaking the sensor to advance through puzzles in real time.

---

## Repository Structure

.
├── arduino/
│   └── DungeonEscape.ino
├── java/
│   ├── src/
│   │   ├── Main.java
│   │   └── SerialPortHandle.java
│   └── lib/
│   │   └── jssc.jar
├── docs/
│   └── wiring_diagram.png
└── README.md

---

## Hardware Requirements

- **Arduino Uno** (or equivalent ATmega328P board)  
- **Analog 3-axis accelerometer** (e.g., MPU-6050 wired for analog output or similar)  
- **Two XBee Series 1 modules** (configured as point-to-point, 9600 baud)  
- **USB cable** (for Arduino programming / optional debug output)  
- **Breadboard & jumper wires**

---

## Arduino Setup

1. **Wire the sensors & XBee**  
   - A3 → accelerometer X  
   - A2 → accelerometer Y  
   - A1 → accelerometer Z  
   - D10 → XBee RX  
   - D11 → XBee TX  
   - See `docs/wiring_diagram.png` for full schematic.

2. **Configure XBee modules**  
   - Use XCTU (Digi) or similar to set both modules to 9600 baud, same PAN ID, coordinator/end-device roles.

3. **Upload the sketch**  
   1. Open **DungeonEscape.ino** in Arduino IDE.  
   2. Verify that `SoftwareSerial mySerial(10, 11);` matches your wiring.  
   3. Upload to the Arduino.

4. **Verify debug output (optional)**  
   - Open the Serial Monitor at 9600 baud to see raw `x,y,z` frames.

---

## PC & Java Setup

1. **Install Java**  
   - Ensure **Java 11** or higher is installed.  
     ```bash
     java -version
     ```

2. **Obtain JSSC library**  
   - The `jssc.jar` is included in `java/lib/`. If you need a newer version, download from the [JSSC GitHub repository](https://github.com/scream3r/java-simple-serial-connector).

3. **Compile the Java code**  
   ```bash
   cd java
   javac -cp lib/jssc.jar src/*.java
