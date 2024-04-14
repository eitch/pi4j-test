package ch.eitchnet.pi4j.i2c;

import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static ch.eitchnet.pi4j.i2c.StringHelper.formatMillisecondsDuration;

public class Pi4jI2cBus implements EncodedRawI2cBus {

	private static final Logger logger = LoggerFactory.getLogger(Pi4jI2cBus.class);

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
	}

	private LoggingI2cDevice getI2cDevice(int address) {
		return this.i2cDeviceMap.computeIfAbsent(address, b -> {
			I2C i2C = this.pi4j.i2c().create(this.bus, address);
			return new LoggingI2cDevice(this, i2C, null);
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

	public <T> T execute(boolean log, Callable<T> action) throws IOException {
		long start = System.currentTimeMillis();

		try {
			lock();
			return action.call();
		} catch (InterruptedException e) {
			throw new IllegalStateException("Interrupted while waiting for lock!", e);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to execute callable " + action, e);
		} finally {
			unlock();
			if (log)
				logger.info("Took {}", formatMillisecondsDuration(System.currentTimeMillis() - start));
		}
	}

	public <T> T execute(boolean log, LoggingI2cDevice i2CDevice, Callable<T> action) throws IOException {
		long start = System.currentTimeMillis();

		try {
			lock();
			this.pi4j.getI2CProvider().
			return this.i2cBus.runBusLockedDeviceAction(i2CDevice.getI2cDevice(), action);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Interrupted while waiting for lock!", e);
		} finally {
			unlock();
			if (log)
				logger.info("Took " + formatMillisecondsDuration(System.currentTimeMillis() - start));
		}
	}

	private void unlock() {
		this.lock.unlock();
	}

	private void lock() throws InterruptedException {
		if (!this.lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
			throw new IllegalStateException(
					"Failed to acquire lock after " + LOCK_WAIT_SECONDS + " " + TimeUnit.SECONDS);
		}
	}

	public void assertLockHeldBy() {
		if (!this.lock.isHeldByCurrentThread())
			throw new IllegalStateException("I2C lock is not held by this thread!");
	}
}
