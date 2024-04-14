package ch.eitchnet.pi4j.i2c;

import java.io.IOException;
import java.util.List;

public interface EncodedRawI2cBus {

	byte[] sendI2c(byte[] i2cData) throws IOException, InterruptedException;

	byte[] sendI2c(byte[] i2cData, int timeout, boolean trace) throws IOException, InterruptedException;

	byte[] sendI2c(byte[] i2cData, boolean trace) throws IOException, InterruptedException;

	List<Byte> scanI2C();

	void open();

	void close();
}
