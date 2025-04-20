/*
   Arduino Sketch: Crypt of Shifting Stones Sensor Node
   -----------------------------------------------------
   Streams 3-axis accelerometer readings as ASCII "x,y,z\n" over serial
   Originally intended to work over ZigBee; in practice, uses USB serial.
   Sampling period: 200 ms
*/

// Pin assignments for accelerometer axes
const int XPIN = A3;  // Analog input X-axis
const int YPIN = A2;  // Analog input Y-axis
const int ZPIN = A1;  // Analog input Z-axis

void setup() {
  // Initialize serial communication at 9600 baud
  Serial.begin(9600);
  // Wait for serial port to be ready (necessary on some boards)
  while (!Serial) {
    ; // Do nothing until Serial is available
  }
  // At this point, serial connection is open and ready to send data
}

void loop() {
  // Read raw analog value from each accelerometer axis
  int xReading = analogRead(XPIN);
  int yReading = analogRead(YPIN);
  int zReading = analogRead(ZPIN);

  // Send X value, then comma delimiter
  Serial.print(xReading);
  Serial.print(',');
  
  // Send Y value, then comma delimiter
  Serial.print(yReading);
  Serial.print(',');

  // Send Z value and newline terminator
  Serial.println(zReading);

  // Pause for 200 milliseconds before the next sample
  delay(200);
}
