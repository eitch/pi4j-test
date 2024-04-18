package ch.eitchnet.pi4j.i2c;

import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Pi4jI2cBus implements EncodedRawI2cBus {

	private final Context pi4j;
	private final int bus;
	private final Map<Integer, LoggingI2cDevice> i2cDeviceMap;
	private final boolean verbose;

	public Pi4jI2cBus(Context pi4j, boolean verbose) {
		this.pi4j = pi4j;
		this.bus = 1;
		this.i2cDeviceMap = new ConcurrentHashMap<>();
		this.verbose = verbose;
	}

	@Override
	public byte[] sendI2c(byte[] i2cData) throws IOException, InterruptedException {
		return sendI2c(i2cData, 0, false);
	}

	@Override
	public byte[] sendI2c(byte[] i2cData, boolean trace) throws IOException, InterruptedException {
		return sendI2c(i2cData, 0, trace);
	}

	@Override
	public byte[] sendI2c(byte[] i2cData, int timeout, boolean trace) throws IOException, InterruptedException {
		if (i2cData.length == 0)
			throw new IllegalStateException("No data!");

		byte address = (byte) (i2cData[0] >> 1);
		boolean isRead = (i2cData[0] & 0x01) == 1;

		LoggingI2cDevice device = getI2cDevice(address);
		return device.execute(() -> {
			if (isRead) {
				if (i2cData.length < 2 || i2cData.length > 3)
					throw new IllegalStateException("Read command should have length 2 or 3");

				if (i2cData.length == 2) {
					// pure Read
					return read(device, i2cData[1]);

				} else {
					// WriteRead

					// write pointer
					device.write(this.verbose, i2cData[1]);
					// read data
					return read(device, i2cData[2]);
				}

			} else {

				if (i2cData.length == 1)
					throw new IllegalStateException("No data to write!");

				device.write(this.verbose, i2cData, 1, i2cData.length - 1);
			}

			return new byte[0];
		});
	}

	private LoggingI2cDevice getI2cDevice(int address) {
		return this.i2cDeviceMap.computeIfAbsent(address, b -> {
			I2C i2C = this.pi4j.i2c().create(this.bus, address);
			return new LoggingI2cDevice(i2C, null);
		});
	}

	private byte[] read(LoggingI2cDevice device, byte length) throws IOException {
		length = (byte) Byte.toUnsignedInt(length);
		byte[] response = new byte[length];
		int read = device.read(this.verbose, response, 0, length);
		if (read != length)
			throw new IllegalStateException("Expected to read " + length + " but read " + read);
		return response;
	}

	@Override
	public List<Byte> scanI2C() {
		List<Byte> found = new ArrayList<>();
		I2CProvider i2CProvider = this.pi4j.i2c();
		for (int i = 0x08; i < 0x78; i++) {
			try {
				I2C i2C = i2CProvider.create(this.bus, i);
				int read = i2C.read();
				if (read >= 0)
					found.add((byte) i);
				Thread.sleep(1);
				this.pi4j.shutdown(i2C.id());
				Thread.sleep(1);
			} catch (Exception e) {
				throw new RuntimeException("Scan I2C Error :" + e.getMessage());
			}
		}
		return found;
	}

	@Override
	public void open() {
		// do nothing
	}

	@Override
	public void close() {
		// do nothing
	}
}
