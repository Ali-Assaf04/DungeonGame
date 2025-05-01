#include <SoftwareSerial.h>

// Pin assignments for accelerometer axes
const int XPIN = A3;       // X-axis
const int YPIN = A2;       // Y-axis
const int ZPIN = A1;       // Z-axis

// SoftwareSerial on pins 10 (RX), 11 (TX) for ZigBee link
SoftwareSerial mySerial(10, 11);

bool isRun = false;

void setup() {
  // USB Serial for debug (optional)
  Serial.begin(9600);
  while (!Serial) ;  // wait for native USB

  // ZigBee serial at 9600 baud
  mySerial.begin(9600);
}

void loop() {
  // Wait for the Java app to send 0x01
  if (mySerial.available() > 0) {
    byte cmd = mySerial.read();
    if (cmd == 0x01) {
      isRun = true;
    }
  }

  if (isRun) {
    // Read accelerometer
    int xVal = analogRead(XPIN);
    int yVal = analogRead(YPIN);
    int zVal = analogRead(ZPIN);

    // Debug output over USB
    Serial.print(xVal);
    Serial.print(',');
    Serial.print(yVal);
    Serial.print(',');
    Serial.println(zVal);

    // Send ASCII "x,y,z\n" over ZigBee
    mySerial.print(xVal);
    mySerial.print(',');
    mySerial.print(yVal);
    mySerial.print(',');
    mySerial.println(zVal);

    delay(200);
  }
}