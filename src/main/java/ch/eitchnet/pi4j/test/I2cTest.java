package ch.eitchnet.pi4j.test;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.io.i2c.I2C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I2cTest {

	private static final Logger logger = LoggerFactory.getLogger(I2cTest.class);

	private static final int PIN_LED = 22;
	private static final int PIN_BTN = 24;

	public static void main(String[] args) throws InterruptedException {
		Context pi4j = Pi4J.newAutoContext();

		DigitalOutput led = pi4j.dout().create(PIN_LED);

		var buttonConfig = DigitalInput
				.newConfigBuilder(pi4j)
				.id("button")
				.name("Press button")
				.address(PIN_BTN)
				.pull(PullResistance.PULL_DOWN)
				.debounce(3000L);
		DigitalInput btn = pi4j.din().create(buttonConfig);

		btn.addListener(e -> {
			logger.info("Button state: {}", e.state());
			switch (e.state()) {
				case LOW -> led.low();
				case HIGH -> led.high();
			}
		});

		DigitalInputConfigBuilder inputConfigBuilder = DigitalInputConfig
				.newBuilder(pi4j)
				.address(17)
				.pull(PullResistance.PULL_UP);
		DigitalInput interrupt = pi4j.din().create(inputConfigBuilder.build());
		I2C input = pi4j.i2c().create(1, 0x38);
		I2C output = pi4j.i2c().create(1, 0x20);

		logger.info("Interrupt is on {} {}", interrupt.description(), interrupt.getName());
		byte data = (byte) input.read();
		logger.info("Read input state {} for {} {}", asBinary(data), input.description(), input.getName());
		data = (byte) output.read();
		logger.info("Read output state {} for {} {}", asBinary(data), output.description(), output.getName());

		interrupt.addListener(e -> readInput(input, output));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				logger.info("Shutting down...");
				pi4j.shutdown();
			} catch (Exception e) {
				logger.error("Failed to shutdown", e);
			}
		}));

		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}

	private static void readInput(I2C input, I2C output) {
		byte data = (byte) input.read();
		logger.info("Read new state {}", asBinary(data));

		for (int j = 0; j < 8; j++) {
			boolean state = isBitSet(data, j);
			//			logger.info("Detected pin " + j + " = " + (state ? 1 : 0));
			if (j == 0 && state)
				output.write(0xfE);
			else
				output.write(0xff);
		}
	}

	public static boolean isBitSet(byte data, int position) {
		if (position > 7)
			throw new IllegalStateException("Position " + position + " is not available in a byte!");
		return ((data >> position) & 1) == 1;
	}

	public static String asBinary(byte b) {

		StringBuilder sb = new StringBuilder();

		sb.append(((b >>> 7) & 1));
		sb.append(((b >>> 6) & 1));
		sb.append(((b >>> 5) & 1));
		sb.append(((b >>> 4) & 1));
		sb.append(((b >>> 3) & 1));
		sb.append(((b >>> 2) & 1));
		sb.append(((b >>> 1) & 1));
		sb.append(((b) & 1));

		return sb.toString();
	}
}
