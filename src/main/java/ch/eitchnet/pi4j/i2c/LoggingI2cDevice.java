package ch.eitchnet.pi4j.i2c;

import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CDevice;
import li.strolch.utils.communication.PacketObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static ch.eitchnet.pi4j.i2c.StringHelper.toHexString;
import static ch.eitchnet.pi4j.i2c.StringHelper.toPrettyHexString;
import static li.strolch.utils.helper.StringHelper.toHexString;
import static li.strolch.utils.helper.StringHelper.toPrettyHexString;

public class LoggingI2cDevice {

	private static final Logger logger = LoggerFactory.getLogger(LoggingI2cDevice.class);

	private final Pi4jI2cBus pi4jI2cBus;
	private final I2C i2C;

	private final String i2cAddressWriteS;
	private final String i2cAddressReadS;
	private final String i2cAddressReadingS;
	private final String i2cAddressS;

	private long ioWait;
	private int ioWaitNanos;
	private long lastWriteNanos;

	private PacketObserver packetObserver;

	public LoggingI2cDevice(Pi4jI2cBus pi4jI2cBus, I2C i2C, PacketObserver packetObserver) {
		this.pi4jI2cBus = pi4jI2cBus;
		this.i2C = i2C;
		this.packetObserver = packetObserver;
		this.i2cAddressS = toHexString((byte) this.i2C.getDevice());
		this.i2cAddressWriteS = "0x" + this.i2cAddressS + ":  Writing: ";
		this.i2cAddressReadingS = "0x" + this.i2cAddressS + ":  Reading: ";
		this.i2cAddressReadS = "0x" + this.i2cAddressS + ":  Read: ";
	}

	public I2C getI2c() {
		return this.i2C;
	}

	public void setPacketObserver(PacketObserver packetObserver) {
		this.packetObserver = packetObserver;
	}

	public int getAddress() {
		return this.i2C.getDevice();
	}

	private void assertHasLock() {
		this.pi4jI2cBus.assertLockHeldBy();
	}

	public void write(boolean log, byte data) throws IOException, InterruptedException {
		assertHasLock();
		sleepIfNecessary();

		if (log)
			logger.info(this.i2cAddressWriteS + toHexString(data));

		this.i2C.write(data);

		if (this.packetObserver != null)
			this.packetObserver.notifySent(new byte[]{data});
		this.lastWriteNanos = System.nanoTime();
	}

	public void write(boolean log, byte[] buffer) throws IOException, InterruptedException {
		write(log, buffer, 0, buffer.length);
	}

	public void write(boolean log, byte[] buffer, int pos, int length) throws IOException, InterruptedException {
		assertHasLock();
		sleepIfNecessary();

		if (log)
			logger.info(this.i2cAddressWriteS + toPrettyHexString(buffer));

		this.i2C.write(buffer, pos, length);

		if (this.packetObserver != null)
			this.packetObserver.notifySent(buffer);
		this.lastWriteNanos = System.nanoTime();
	}

	public void write(boolean log, int address, byte b) throws IOException, InterruptedException {
		write(log, new byte[]{(byte) address, b});
	}

	public void write(boolean log, int address, byte[] buffer) throws IOException, InterruptedException {
		byte[] data = new byte[buffer.length + 1];
		data[0] = (byte) address;
		System.arraycopy(buffer, 0, data, 1, buffer.length);
		write(log, data);
	}

	public void writeRead(boolean log, byte[] writeBuffer, byte[] readBuffer) throws IOException, InterruptedException {
		assertHasLock();
		sleepIfNecessary();

		if (log)
			logger.info(this.i2cAddressWriteS + toPrettyHexString(writeBuffer));

		this.i2C.
		int read = this.i2C.read(writeBuffer, 0, writeBuffer.length, readBuffer, 0, readBuffer.length);
		if (read != readBuffer.length)
			throw new IllegalStateException("Expected to read " + readBuffer.length + " bytes, but read " + read);

		if (this.packetObserver != null) {
			this.packetObserver.notifySent(writeBuffer);
			this.packetObserver.notifyReceived(readBuffer);
		}
		this.lastWriteNanos = System.nanoTime();

		if (log)
			logger.info(this.i2cAddressReadS + toPrettyHexString(readBuffer));
	}

	public int read(boolean log) throws IOException {
		assertHasLock();
		int read = this.i2cDevice.read();

		if (log)
			logger.info(this.i2cAddressReadS + toHexString((byte) read));
		if (this.packetObserver != null)
			this.packetObserver.notifyReceived(new byte[]{(byte) read});

		return read;
	}

	public int read(boolean log, byte address) throws IOException {
		assertHasLock();
		if (log)
			logger.info(this.i2cAddressWriteS + toHexString(address));

		int read = this.i2cDevice.read(address);

		if (log)
			logger.info(this.i2cAddressReadS + toHexString((byte) read));
		if (this.packetObserver != null) {
			this.packetObserver.notifySent(new byte[]{address});
			this.packetObserver.notifyReceived(new byte[]{(byte) read});
		}

		return read;
	}

	public int read(boolean log, byte[] buffer) throws IOException {
		return read(log, buffer, 0, buffer.length);
	}

	public int read(boolean log, byte[] buffer, int pos, int length) throws IOException {
		assertHasLock();
		if (log)
			logger.info(this.i2cAddressReadingS + buffer.length + " bytes from last set address...");

		int read = this.i2cDevice.read(buffer, pos, length);
		if (read != buffer.length)
			throw new IllegalStateException("Expected to read " + buffer.length + " bytes, but read " + read);

		if (log)
			logger.info(this.i2cAddressReadS + toPrettyHexString(buffer, pos, length));
		if (this.packetObserver != null)
			this.packetObserver.notifyReceived(new byte[]{(byte) read});
		return read;
	}

	public void read(boolean log, byte address, byte[] buffer) throws IOException {
		assertHasLock();
		if (log)
			logger.info(this.i2cAddressReadingS + buffer.length + " bytes from address " + toHexString(address));

		int read = this.i2cDevice.read(address, buffer, 0, buffer.length);
		if (read != buffer.length)
			throw new IllegalStateException("Expected to read " + buffer.length + " bytes, but read " + read);

		if (log)
			logger.info(this.i2cAddressReadS + toPrettyHexString(buffer));
		if (this.packetObserver != null) {
			this.packetObserver.notifySent(new byte[]{address});
			this.packetObserver.notifyReceived(buffer);
		}
	}

	private void sleepIfNecessary() throws InterruptedException {
		if (this.ioWait == 0L) {
			if (this.ioWaitNanos == 0L)
				return;

			long nextWriteNanos = this.lastWriteNanos + this.ioWaitNanos;
			long now = System.nanoTime();
			if (nextWriteNanos > now) {
				Thread.sleep(0L, (int) (nextWriteNanos - now));
			}
			return;
		}

		long nextWrite = (this.lastWriteNanos / 1000000) + this.ioWait;
		long now = System.currentTimeMillis();
		if (nextWrite > now) {
			Thread.sleep(nextWrite - now);
		}
	}

	public void setIoWait(long ioWait, int ioWaitNanos) {
		this.ioWait = ioWait;
		this.ioWaitNanos = ioWaitNanos;

		if (ioWait == 0L)
			logger.info("Using " + ioWaitNanos + " ns for write sleep");
	}

	@Override
	public String toString() {
		return "I2C Device @ " + this.i2cAddressS;
	}
}
