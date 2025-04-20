import jssc.*;

/**
 * SerialPortHandle.java
 * ----------------------
 * Wrapper class for managing serial port I/O using the JSSC library.
 * Handles opening, configuring, reading from, and closing a serial port.
 */
public class SerialPortHandle {
    // Underlying JSSC SerialPort object
    private final SerialPort sp;

    /**
     * Constructor: opens and configures the serial port.
     * @param port the name of the serial port (e.g., "COM8")
     * @throws SerialPortException if the port cannot be opened or configured
     */
    public SerialPortHandle(String port) throws SerialPortException {
        sp = new SerialPort(port);
        sp.openPort();                                 // Open the connection
        sp.setParams(
            9600,     // baud rate
            8,        // data bits
            1,        // stop bits
            0         // parity
        );
        // Clear any existing data in the receive buffer
        sp.purgePort(SerialPort.PURGE_RXCLEAR);
    }

    /**
     * Reads a single byte from the serial port, if available.
     * @return the byte value (0–255) if data is available; -1 if no data or on error
     */
    public int read() {
        try {
            // Check if bytes are waiting in the input buffer
            if (sp.getInputBufferBytesCount() > 0) {
                byte[] data = sp.readBytes(1);
                return data[0] & 0xFF;  // Convert signed byte to unsigned int
            } else {
                return -1;              // No data available
            }
        } catch (Exception e) {
            // On any exception (read error, port closed, etc.), return -1
            return -1;
        }
    }

    /**
     * Closes the serial port if it is open.
     * Silently ignores any exceptions during closure.
     */
    public void close() {
        try {
            if (sp.isOpened()) {
                sp.closePort();      // Close the connection
            }
        } catch (Exception ignored) {
            // Ignore errors on close
        }
    }
}
