import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * SerialPortHandle.java
 * ----------------------
 * Wrapper for JSSC serial I/O.
 */
public class SerialPortHandle {
    private final SerialPort sp;

    /**
     * Open and configure the port at 9600 baud, 8 N 1.
     * @param port e.g. "COM8"
     */
    public SerialPortHandle(String port) throws SerialPortException {
        sp = new SerialPort(port);
        sp.openPort();
        sp.setParams(
            9600, // baud
            8,    // data bits
            1,    // stop bits
            0     // parity
        );
        sp.purgePort(SerialPort.PURGE_RXCLEAR);
    }

    /**
     * Read one byte if available, else return -1.
     */
    public int read() {
        try {
            if (sp.getInputBufferBytesCount() > 0) {
                byte[] data = sp.readBytes(1);
                return data[0] & 0xFF;
            } else {
                return -1;
            }
        } catch (SerialPortException e) {
            return -1;
        }
    }

    /**
     * **New**: Send a single byte (for the 0x01 handshake).
     */
    public void writeByte(byte b) throws SerialPortException {
        sp.writeByte(b);
    }

    /** Close if open. */
    public void close() {
        try {
            if (sp.isOpened()) sp.closePort();
        } catch (SerialPortException ignored) { }
    }
}
